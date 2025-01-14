package java1;

import com.google.common.base.Joiner;

import javax.sound.midi.Sequencer;
import javax.swing.*;
import javax.swing.text.Utilities;
import java.io.*;
import java.sql.SQLOutput;
import java.util.*;
import java.util.stream.Collectors;

public class LowUtilitySequenceMining {
    Set<int[]> preSet = new LinkedHashSet<>();
    Map<Integer, Integer[]> mapTidToItems = new LinkedHashMap<>();
    Map<Integer, Integer[]> mapTidToUtilities = new LinkedHashMap<>();
    Map<Integer, Integer[]> mapTidToItems_preprocess = new LinkedHashMap<>();
    Map<Integer, Integer[]> mapTidToUtilities_preprocess = new LinkedHashMap<>();
    Map<Integer, Integer> mapItemToUtility = new LinkedHashMap<>();
    Map<Integer, BitSet> mapItemToBitSet = new LinkedHashMap<>();
    Set<List<Integer>> maxSequenceList = new LinkedHashSet<>();
    List<List<Integer>> maxSequenceListUtility = new ArrayList<>();
    Set<List<Integer>> maxSequenceSet = new HashSet<>();
    Set<List<Integer>> hasProcessedSequenceList = new LinkedHashSet<>();
    Map<List<Integer>, Integer> lowUtilityPattern = new LinkedHashMap<>();


    Generator3 generator;
    int count = 0;
    //    记录每行序列的起始项的位置
    Set<Integer> sequenceId = new TreeSet<>();
    int max_utility;
    BufferedWriter writer = null;
    long runtime;
    long runtime2;
    long runtime3;
    long runtime4;
    long runtime5;

    long startTime;
    int patternCount;
    long candidatesCount;

    int maxLength;

    public void runAlgorithm(String input, int max_utility, int maxLength, String output) throws IOException {

        this.max_utility = max_utility;
        startTime = System.currentTimeMillis();
        System.out.println("输出最大效用:" + max_utility);
        System.out.println("startTime:" + startTime);
//
        loadFile(input);
        runtime2 = System.currentTimeMillis() - startTime;
        preprocess(maxLength);
        runtime3 = System.currentTimeMillis() - startTime;
        processCoreAlgorithm();
        runtime = System.currentTimeMillis() - startTime;
        showStates();
    }

    private void myprint(int[] itemset, int uOfRoot) throws IOException {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < itemset.length; i++) {
            if (i == itemset.length - 1) {
                // System.out.print(itemset[i]+":");
                buffer.append(itemset[i] + ":");
            } else {
                // System.out.print(itemset[i]+" ");
                buffer.append(itemset[i] + " ");
            }
        }
        //System.out.println(uOfRoot);
        buffer.append(uOfRoot);
        writer.write(buffer.toString());
        writer.newLine();
        writer.flush();
    }

public void loadFile(String path) throws IOException {
    Set<Integer> setOfItems = new TreeSet<>();
    Map<Integer, Integer[]> mapTidToItems = new LinkedHashMap<>();
    Map<Integer, Integer[]> mapTidToUtilities = new LinkedHashMap<>();
    int tidCount = 0;
    sequenceId.add(0);

    try (BufferedReader myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path))))) {
        String thisLine;

        // Read each line from the file
        while ((thisLine = myInput.readLine()) != null) {
            // Skip comments, empty lines, and metadata
            if (thisLine.isEmpty() || thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%' || thisLine.charAt(0) == '@') {
                continue;
            }

            // Split the line into parts
            String[] partitions = thisLine.split(":");
            String[] items = partitions[0].split(" ");
            String[] utilities = partitions[2].split(" ");

            // Convert items and utilities to Integer arrays
            Integer[] itemsInt = new Integer[items.length];
            Integer[] utilsInt = new Integer[utilities.length];

            for (int i = 0; i < items.length; i++) {
                itemsInt[i] = Integer.parseInt(items[i]);
                utilsInt[i] = Integer.parseInt(utilities[i]);
                count++;

                // Update item and utility values if utility exceeds max_utility
                if (utilsInt[i] >= max_utility) {
                    itemsInt[i] = -1;
                    utilsInt[i] = -1;
                    setOfItems.add(-1);
                } else {
                    setOfItems.add(itemsInt[i]);
                }
            }

            // Add the processed data to the maps
            mapTidToItems.put(tidCount, itemsInt);
            mapTidToUtilities.put(tidCount, utilsInt);
            sequenceId.add(count);
            tidCount++;
        }

        this.mapTidToItems = mapTidToItems;
        this.mapTidToUtilities = mapTidToUtilities;

        // Print mapTidToItems data
        printMapData("mapTidToItems", mapTidToItems);

        // Print mapTidToUtilities data
        printMapData("mapTidToUtilities", mapTidToUtilities);

    } catch (IOException e) {
        // Handle file reading exceptions
        e.printStackTrace();
    }
}

    private void printMapData(String mapName, Map<Integer, Integer[]> mapData) {
        System.out.println("Printing " + mapName + ":");
        for (Map.Entry<Integer, Integer[]> entry : mapData.entrySet()) {
            System.out.print("Line " + entry.getKey() + ": ");
            System.out.println(Arrays.toString(entry.getValue()));
        }
    }
    public void preprocess(int maxLength) {
        System.out.println("当前运行到 preprocess");
        int countBit = 0;

        // 创建每一项的位图
        for (Map.Entry<Integer, Integer[]> entry : mapTidToItems.entrySet()) {
            Integer[] items = entry.getValue();
            for (Integer key : items) {
                mapItemToBitSet.computeIfAbsent(key, k -> new BitSet()).set(countBit);
                countBit++;
            }
        }

        runtime4 = System.currentTimeMillis() - startTime;

        this.maxLength = maxLength;

        for (Map.Entry<Integer, Integer[]> entry : mapTidToItems.entrySet()) {
            Integer[] items = entry.getValue();

            for (int i = 0; i < items.length; i++) {
                if (items[i] == -1) {
                    continue; // 跳过分隔符
                }

                List<Integer> listItems = new ArrayList<>();
                for (int j = i; j < items.length && (j - i + 1) <= maxLength; j++) {
                    if (items[j] == -1) {
                        break; // 遇到分隔符时退出内层循环
                    }
                    listItems.add(items[j]);
                }

                candidatesCount++;

                if (!isContains(listItems)) {
                    addSequence(listItems);
                }
            }
        }

        System.out.println("输出最大序列");
        System.out.println(maxSequenceList);

        runtime5 = System.currentTimeMillis() - startTime;
    }
public void processCoreAlgorithm() {
    System.out.println("当前运行到coreAlg");
    for (List<Integer> sequence : maxSequenceList) {
        int utilityOfSequence = countUtility(sequence);
        if (utilityOfSequence <= max_utility) {
            processLowUtilitySequence(sequence, utilityOfSequence);
        } else {
            cut(sequence);
        }
    }
    printLowUtilitySequence();
}
    private void processLowUtilitySequence(List<Integer> sequence, int utility) {
        if (!hasProcessedSequenceList.contains(sequence)) {
            hasProcessedSequenceList.add(sequence);
            candidatesCount++;
            patternCount++;
            lowUtilityPattern.put(sequence, utility);
            LUSM(sequence);
        }
    }
    public void coreAlg() {
        System.out.println("当前运行到coreAlg");
        Integer utilityOfSequence = 0;
        List<Integer> sequence1 = new ArrayList<>();
//        sequence1.add(3);
//        sequence1.add(4);
//        System.out.println(countUtility(sequence1));
        for (List<Integer> sequence : maxSequenceList) {
            utilityOfSequence = countUtility(sequence);
            if (utilityOfSequence <= max_utility) {
                hasProcessedSequenceList.add(sequence);
                candidatesCount++;
                patternCount++;
                lowUtilityPattern.put(sequence, utilityOfSequence);
                LUSM(sequence);
            } else {
                cut(sequence);
            }
        }
        printLowUtilitySequence();
        System.out.println("输出已经处理的序列列表");
        System.out.println(hasProcessedSequenceList);
    }
    public void cut1(List<Integer> sequence) {
        if (sequence.size() > 1) {
            List<List<Integer>> utilities = getUtilities(sequence);
            cutSequence(sequence, utilities, true);  // 剪除首项
            cutSequence(sequence, utilities, false); // 剪除尾项
        } else {
            processSingleElementSequence(sequence);
        }
    }
    public void cut(List<Integer> sequence) {
        if (sequence.size() > 1) {
            List<List<Integer>> utilities = getUtilities(sequence);
            cutFirst(sequence, utilities);
            cutLast(sequence, utilities);
        } else {
//            System.out.println("处理一个项");
            if (hasProcessedSequenceList.contains(sequence) == false) {
                LUSMItem(sequence);
                hasProcessedSequenceList.add(sequence);
            }
        }
    }

    public void cut(List<Integer> sequence, List<List<Integer>> utilities) {
        if (sequence.size() > 1) {
            cutSequence(sequence, utilities, true);  // 剪除首项
            cutSequence(sequence, utilities, false); // 剪除尾项
        } else {
            processSingleElementSequence(sequence);

        }
    }
private void processSingleElementSequence(List<Integer> sequence){
    if (hasProcessedSequenceList.contains(sequence) == false) {
        LUSMItem(sequence);
        hasProcessedSequenceList.add(sequence);
    }
}
    private void cutSequence(List<Integer> sequence, List<List<Integer>> utilities, boolean isFirst) {
        List<List<Integer>> newUtilities = new ArrayList<>();
        List<Integer> newSequence;

        for (List<Integer> entry : utilities) {
            List<Integer> modifiedEntry = new ArrayList<>(entry);
            if (isFirst) {
                modifiedEntry.remove(0); // 删除首项
            } else {
                modifiedEntry.remove(modifiedEntry.size() - 1); // 删除尾项
            }
            newUtilities.add(modifiedEntry);
        }

        newSequence = isFirst
                ? sequence.subList(1, sequence.size())
                : sequence.subList(0, sequence.size() - 1);

        int allUtilities = countUtility_FL(newUtilities);
        if (allUtilities <= max_utility) {
            LUSM(new ArrayList<>(newSequence));
        } else {
            cut(new ArrayList<>(newSequence), newUtilities);
        }
    }

    public void LUSMItem(List<Integer> sequence) {
        int utility = countUtility(sequence);
        if (utility <= max_utility) {
            processLowUtilitySequence(sequence,utility);
        }
    }

    public void cutFirst(List<Integer> sequence) {
        List<List<Integer>> utilities = getUtilities(sequence);
        int allUtilities = 0;
        int start = 1;
        for (Iterator<List<Integer>> it = utilities.iterator(); it.hasNext(); ) {
            List<Integer> entry = it.next();
            for (int i = start; i < entry.size(); i++) {
                allUtilities += entry.get(i);
            }
        }
        List<Integer> sequenceFirst = new ArrayList<>();
        sequenceFirst.addAll(sequence.subList(1, sequence.size()));
        if (allUtilities <= max_utility) {

            LUSM(sequenceFirst);
        } else {
            cut(sequenceFirst);
        }
    }

    public void cutFirst(List<Integer> sequence, List<List<Integer>> utilities) {
        List<List<Integer>> newUtilities = new ArrayList<>();
        List<Integer> sequenceTest = new ArrayList<>();
        sequenceTest.add(4);
        sequenceTest.add(1);
        sequenceTest.add(3);
        if (sequence.equals(sequenceTest)) {
            System.out.println("输出413");
        }
        int allUtilities = 0;
        int start = 1;
        for (Iterator<List<Integer>> it = utilities.iterator(); it.hasNext(); ) {
            List<Integer> entry = it.next();
            List<Integer> newEntry = new ArrayList<>(entry);
            newEntry.remove(0);
            newUtilities.add(newEntry);
        }
        allUtilities = countUtility_FL(newUtilities);
        List<Integer> sequenceFirst = new ArrayList<>();
        sequenceFirst.addAll(sequence.subList(1, sequence.size()));
        if (allUtilities <= max_utility) {

            LUSM(sequenceFirst);
        } else {
            cut(sequenceFirst, newUtilities);
        }
    }
    public int countUtility_FL(List<List<Integer>> utilities) {
        return utilities.stream()
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .reduce(0, Integer::sum);
    }


    public void cutLast(List<Integer> sequence) {
        List<List<Integer>> utilities = getUtilities(sequence);
        int start = 1;
        int allUtilities = 0;
        for (Iterator<List<Integer>> it = utilities.iterator(); it.hasNext(); ) {
            List<Integer> entry = it.next();
            for (int i = 0; i < entry.size() - 1; i++) {
                allUtilities += entry.get(i);
            }
        }
        List<Integer> sequenceLast = new ArrayList<>();
        sequenceLast.addAll(sequence.subList(0, sequence.size() - 1));
        if (allUtilities <= max_utility) {
            LUSM(sequenceLast);
        } else {
            cut(sequenceLast);
        }

    }

    public void cutLast(List<Integer> sequence, List<List<Integer>> utilities) {
        List<List<Integer>> newUtilities = new ArrayList<>();

        int allUtilities = 0;
        for (Iterator<List<Integer>> it = utilities.iterator(); it.hasNext(); ) {
            List<Integer> entry = it.next();
            List<Integer> newEntry = new ArrayList<>(entry);
            newEntry.remove(newEntry.size() - 1);
            newUtilities.add(newEntry);
        }
        allUtilities = countUtility_FL(newUtilities);
        List<Integer> sequenceLast = new ArrayList<>();
        sequenceLast.addAll(sequence.subList(0, sequence.size() - 1));
        if (allUtilities <= max_utility) {
            LUSM(sequenceLast);
        } else {
            cut(sequenceLast, newUtilities);
        }

    }


    public void addSequence(List<Integer> sequence) {
        maxSequenceList.add(sequence);
        maxSequenceSet.add(sequence);
    }

    public int countUtility(List<Integer> sequence) {
        int allUtility = 0;
        int Utility = 0;
        boolean isExist = true;
        BitSet bitSet = mapItemToBitSet.get(sequence.get(0));
        int index = bitSet.nextSetBit(0);
        while (index != -1) {
            Utility = 0;
            Utility += getUtility(index);
            isExist = true;
            int newIndex = index;
            for (int i = 1; i < sequence.size(); i++) {
                newIndex++;
                BitSet bitSet1 = mapItemToBitSet.get(sequence.get(i));
                if (bitSet1.get(newIndex) == false) {
                    isExist = false;
                    break;
                }
                Utility += getUtility(newIndex);
            }
            if (isExist == true) {
                allUtility += Utility;
            }
            index = bitSet.nextSetBit(index + 1);
        }
        return allUtility;
    }

    public List<List<Integer>> getUtilities(List<Integer> sequence) {
        List<List<Integer>> utilities = new ArrayList<>();
        List<Integer> utility;
        boolean isExist = true;
        BitSet bitSet = mapItemToBitSet.get(sequence.get(0));
        int index = bitSet.nextSetBit(0);
        while (index != -1) {
            utility = new ArrayList<>();
            utility.add(getUtility(index));
            isExist = true;
            int newIndex = index;
            for (int i = 1; i < sequence.size(); i++) {
                newIndex++;
                BitSet bitSet1 = mapItemToBitSet.get(sequence.get(i));
                if (bitSet1.get(newIndex) == false) {
                    isExist = false;
                    break;
                }
                utility.add(getUtility(newIndex));
            }
            if (isExist == true) {
                utilities.add(utility);
            }
            index = bitSet.nextSetBit(index + 1);
        }
        return utilities;
    }

    public int getUtility(int index) {
        Integer[] utilities;
        Integer utility;
        int lastCount = 0, count = 0;
        int i = -1;
        for (Integer key : sequenceId) {
            lastCount = count;
            count = key;
            if (index < key) {
                break;
            }
            i++;
        }
        utilities = mapTidToUtilities.get(i);
        utility = utilities[index - lastCount];
        return utility;
    }
    public boolean isContains(List<Integer> sequence) {
        // 优化第一个条件
        if (maxSequenceSet.contains(sequence)) {
            return true;
        }

        // 并行化检查 containsSublist
        return maxSequenceList.parallelStream().anyMatch(maxSequence -> containsSublist(maxSequence, sequence));
    }
    private boolean containsSublist(List<Integer> maxSequence, List<Integer> sequence) {
        int n = maxSequence.size();
        int m = sequence.size();
        if (m > n) return false;

        // 滑动窗口优化
        for (int i = 0; i <= n - m; i++) {
            boolean match = true;
            for (int j = 0; j < m; j++) {
                if (!maxSequence.get(i + j).equals(sequence.get(j))) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return true;
            }
        }
        return false;
    }

    public String ListToString(List<Integer> sequence) {
        String string = "";
        for (int i = 0; i < sequence.size(); i++) {
            string += sequence.get(i);
        }
        return string;
    }

    public void printStats(List<Double> runTimelist, List<Double> memorylist, List<Long> candidateslist, List<Integer> patternlist) {

        runTimelist.add((double) runtime / 1000);
        memorylist.add(MemoryLogger.getInstance().getMaxMemory());
        candidateslist.add(candidatesCount);
        patternlist.add(patternCount);

    }

    public void showStates() {
        System.out.println("存储数据运行时间为：" + (double) runtime2 / 1000 + "s");
        System.out.println("获得项的位图运行时间为：" + (double) runtime4 / 1000 + "s");
        System.out.println("获得最长序列运行时间为：" + (double) runtime5 / 1000 + "s");
        System.out.println("预处理运行时间为：" + (double) runtime3 / 1000 + "s");
        System.out.println("运行时间为：" + (double) runtime / 1000 + "s");
        System.out.println("运行内存为：" + MemoryLogger.getInstance().getMaxMemory() + "MB");
        System.out.println("总的内存为：" + (double) Runtime.getRuntime().totalMemory() / 1024d / 1024d + "MB");
        System.out.println("空闲内存为：" + (double) Runtime.getRuntime().freeMemory() / 1024d / 1024d + "MB");
        System.out.println("候选集数目为：" + candidatesCount);
        System.out.println("模式数目为：" + patternCount);
    }
public void LUSM(List<Integer> sequence) {
    if (!hasProcessedSequenceList.contains(sequence)) {
        processLowUtilitySequence(sequence, countUtility(sequence));
    }

    if (sequence.size() == 1) {
        processSingleElementSequence(sequence);
        return;
    }

    List<Integer> subFirstSequence = new ArrayList<>(sequence);
    List<Integer> subLastSequence = new ArrayList<>(sequence);

    subFirstSequence.remove(0); // 删除首项
    subLastSequence.remove(subLastSequence.size() - 1); // 删除尾项

    processSubSequence(subFirstSequence);
    processSubSequence(subLastSequence);
}

    private void processSubSequence(List<Integer> subSequence) {
        if (subSequence.size() > 1 && !hasProcessedSequenceList.contains(subSequence)) {
            int utility = countUtility(subSequence);
            if (utility <= max_utility && lowUtilityPattern.get(subSequence) == null) {
                processLowUtilitySequence(subSequence, utility);
            }
            hasProcessedSequenceList.add(subSequence);
            cut(subSequence);
        }
    }

    public void printLowUtilitySequence() {

        System.out.println("低效用序列有:" + patternCount + "个");
        if (patternCount > 0) {
            System.out.println("输出低效用序列：");
            for (Map.Entry<List<Integer>, Integer> entry : lowUtilityPattern.entrySet()) {
                System.out.println("序列：" + entry.getKey() + "  效用：" + entry.getValue());
            }
        }
        System.out.println("输出低效用序列的数量：" + lowUtilityPattern.size());

    }

}

