package java1;

import com.google.common.base.Joiner;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class LowUtilitySequenceMining {
    Set<int[]> preSet = new LinkedHashSet<>();
    Map<Integer, Integer[]> mapTidToItems = new LinkedHashMap<>();
    Map<Integer, Integer[]> mapTidToUtilities = new LinkedHashMap<>();
    Map<Integer, BitSet> mapItemToBitSet = new LinkedHashMap<>();
    Set<List<Integer>> maxSequenceList = new LinkedHashSet<>();
    Set<List<Integer>> maxSequenceSet = new HashSet<>();

    Set<List<Integer>> hasKnownHighUtilitySequence = new LinkedHashSet<>();
    Set<List<Integer>> hasProcessedSequenceList = new LinkedHashSet<>();
    Set<List<Integer>> hasDeleteSequenceList = new LinkedHashSet<>();
    Map<List<Integer>, Integer> lowUtilityPattern = new LinkedHashMap<>();


    Generator3 generator;
    int count = 0;
    int increment = 0;
    //    记录每行序列的起始项的位置
    Set<Integer> sequenceId = new TreeSet<>();
    int max_utility;
    BufferedWriter writer = null;
    //String exteFile="src/main/resources/extension.txt";
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
        this.maxLength=maxLength;
        startTime = System.currentTimeMillis();
        System.out.println("输出最大效用:" + max_utility);
        System.out.println("startTime:" + startTime);
        loadFile_sequence(input);
//        loadFile_sequence_delete(input);
//        loadFile(input);
        runtime2 = System.currentTimeMillis() - startTime;
        System.out.println("111111");
        getMaxSequence(maxLength);
//        getMaxSequence_delete(maxLength);
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

    public void loadFile_sequence(String path) throws IOException {
        final int[] tidCount = {0};
        final int[] item1={0};
        final int[] indexOfItem={0};
        Set<Integer> setOfItems = new TreeSet<>();
        Map<Integer, Integer[]> mapTidToItems = new LinkedHashMap<>();
        Map<Integer, Integer[]> mapTidToUtilities = new LinkedHashMap<>();
        sequenceId.add(0);
        List<Integer> tidCountList=new ArrayList<>();
        List<Integer> itemList=new ArrayList<>();
        List<Integer> indexOfItemList=new ArrayList<>();
        try (BufferedReader myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path))))) {
            myInput.lines()
                    .filter(line -> !line.isEmpty() && line.charAt(0) != '#' && line.charAt(0) != '%' && line.charAt(0) != '@')
                    .forEach(thisLine -> {
                        String[] partitions = thisLine.split("SUtility:"); // 按 "SUtility:" 分割数据
                        if (partitions.length < 2) return;  // 如果没有有效的分割部分则跳过
                        indexOfItem[0]=0;
                        boolean key = false;
                        String[] itemUtilityPairs = partitions[0].trim().split(" ");  // 按空格分割项效用对

                        List<Integer> itemsIntList = new ArrayList<>();
                        List<Integer> utilsIntList = new ArrayList<>();
                        boolean containsMaxUtility = false;

                        for (String pair : itemUtilityPairs) {
                            if (pair.isEmpty()) continue;

                            // 解析 "item[utility]" 格式
                            String[] itemUtility = pair.split("\\[|\\]"); // 按 "[" 和 "]" 分割
                            if (itemUtility.length != 2) continue;

                            try {
                                int item = Integer.parseInt(itemUtility[0].trim());
                                int utility = Integer.parseInt(itemUtility[1].trim());
                                item1[0]=item;
                                if (utility >= max_utility) {
                                    utility = -1;
                                    containsMaxUtility = true;
                                    tidCountList.add(tidCount[0]);
                                    itemList.add(item1[0]);
                                    indexOfItemList.add(indexOfItem[0]);
                                } else {
                                    key = true;
                                }
                                indexOfItem[0]++;
                                setOfItems.add(item);
                                itemsIntList.add(item);
                                utilsIntList.add(utility);
                            } catch (NumberFormatException e) {
                                // 处理解析错误的情况
                                System.out.println("解析项或效用错误: " + pair);
                            }
                        }

                        // 如果包含最大效用项，则添加 -1 到集合中
                        if (containsMaxUtility) {
                            setOfItems.add(-1);
                        }

                        // 如果存在有效的项和效用，加入到 map 中
                        if (key) {
                            count += itemsIntList.size();
                            mapTidToItems.put(tidCount[0], itemsIntList.toArray(new Integer[0]));
                            mapTidToUtilities.put(tidCount[0], utilsIntList.toArray(new Integer[0]));
                            sequenceId.add(count);
                            tidCount[0]++;
                        }
                    });

            this.mapTidToItems = mapTidToItems;
            this.mapTidToUtilities = mapTidToUtilities;
            for(int i=0;i<tidCountList.size();i++){
                addHasKnownHS(tidCountList.get(i),indexOfItemList.get(i),itemList.get(i));
            }
            System.out.println("输出高效用序列");
            System.out.println(hasKnownHighUtilitySequence);
            printMapData("mapTidToItems", mapTidToItems);
            printMapData("mapTidToUtilities", mapTidToUtilities);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadFile_sequence_delete(String path) throws IOException {
        final int[] tidCount = {0};
        Set<Integer> setOfItems = new TreeSet<>();
        Map<Integer, Integer[]> mapTidToItems = new LinkedHashMap<>();
        Map<Integer, Integer[]> mapTidToUtilities = new LinkedHashMap<>();
        sequenceId.add(0);

        try (BufferedReader myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path))))) {
            myInput.lines()
                    .filter(line -> !line.isEmpty() && line.charAt(0) != '#' && line.charAt(0) != '%' && line.charAt(0) != '@')
                    .forEach(thisLine -> {
                        String[] partitions = thisLine.split("SUtility:"); // 按 "SUtility:" 分割数据
                        if (partitions.length < 2) return;  // 如果没有有效的分割部分则跳过

                        boolean key = false;
                        String[] itemUtilityPairs = partitions[0].trim().split(" ");  // 按空格分割项效用对
                        List<Integer> itemsIntList = new ArrayList<>();
                        List<Integer> utilsIntList = new ArrayList<>();
                        boolean containsMaxUtility = false;

                        for (String pair : itemUtilityPairs) {
                            if (pair.isEmpty()) continue;

                            // 解析 "item[utility]" 格式
                            String[] itemUtility = pair.split("\\[|\\]"); // 按 "[" 和 "]" 分割
                            if (itemUtility.length != 2) continue;

                            try {
                                int item = Integer.parseInt(itemUtility[0].trim());
                                int utility = Integer.parseInt(itemUtility[1].trim());

                                // 如果效用大于等于 max_utility，则跳过该项和效用
                                if (utility >= max_utility) {
                                    containsMaxUtility = true;
                                    continue; // 跳过该项和效用，不存储
                                } else {
                                    key = true;
                                }

                                setOfItems.add(item);
                                itemsIntList.add(item);
                                utilsIntList.add(utility);
                            } catch (NumberFormatException e) {
                                // 处理解析错误的情况
                                System.out.println("解析项或效用错误: " + pair);
                            }
                        }

                        // 如果包含最大效用项，则添加 -1 到集合中
                        if (containsMaxUtility) {
                            setOfItems.add(-1);
                        }

                        // 如果存在有效的项和效用，加入到 map 中
                        if (key) {
                            count += itemsIntList.size();
                            mapTidToItems.put(tidCount[0], itemsIntList.toArray(new Integer[0]));
                            mapTidToUtilities.put(tidCount[0], utilsIntList.toArray(new Integer[0]));
                            sequenceId.add(count);
                            tidCount[0]++;
                        }
                    });

            this.mapTidToItems = mapTidToItems;
            this.mapTidToUtilities = mapTidToUtilities;

            printMapData("mapTidToItems", mapTidToItems);
            printMapData("mapTidToUtilities", mapTidToUtilities);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadFile(String path) throws IOException {
        final int[] tidCount = {0};
        Set<Integer> setOfItems = new TreeSet<>();
        Map<Integer, Integer[]> mapTidToItems = new LinkedHashMap<>();
        Map<Integer, Integer[]> mapTidToUtilities = new LinkedHashMap<>();
        sequenceId.add(0);

        try (BufferedReader myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path))))) {
            myInput.lines()
                    .filter(line -> !line.isEmpty() && line.charAt(0) != '#' && line.charAt(0) != '%' && line.charAt(0) != '@')
                    .forEach(thisLine -> {
                        String[] partitions = thisLine.split(":");
                        if (partitions.length < 3) return;
                        boolean key = false;
                        String[] items = partitions[0].trim().split("\\s+");
                        String[] utilities = partitions[2].trim().split("\\s+");
                        increment = 0;
                        List<Integer> itemsIntList = new ArrayList<>();
                        List<Integer> utilsIntList = new ArrayList<>();
                        boolean containsMaxUtility = false;

                        for (int i = 0; i < items.length; i++) {
                            int item = Integer.parseInt(items[i]);
                            int utility = Integer.parseInt(utilities[i]);
//                            count++;
                            increment++;
                            if (utility >= max_utility) {
                                utility = -1;
                                containsMaxUtility = true;
                            } else {
                                key = true;
                            }
                            setOfItems.add(item);
                            itemsIntList.add(item);
                            utilsIntList.add(utility);
                        }

                        if (containsMaxUtility) {
                            setOfItems.add(-1);
                        }
                        if (key == true) {
                            count += increment;
                            mapTidToItems.put(tidCount[0], itemsIntList.toArray(new Integer[0]));
                            mapTidToUtilities.put(tidCount[0], utilsIntList.toArray(new Integer[0]));
                            sequenceId.add(count);
                            tidCount[0]++;
                        }

                    });

            this.mapTidToItems = mapTidToItems;
            this.mapTidToUtilities = mapTidToUtilities;

            printMapData("mapTidToItems", mapTidToItems);
            printMapData("mapTidToUtilities", mapTidToUtilities);
        } catch (IOException e) {
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

    public void addHasKnownHS(int indexOfSequence,int indexOfItem,int item) {

        List<Integer> highUSequence=new ArrayList<>();
        Integer[] sequenceAll=this.mapTidToItems.get(indexOfSequence);
        if(indexOfItem-this.maxLength+1<0){
            for(int i=0;i<=indexOfItem+this.maxLength&&i< sequenceAll.length;i++){
                highUSequence=new ArrayList<>();
                for(int j=0;j<this.maxLength;j++)
                {
                    highUSequence.add(sequenceAll[i+j]);
                    if(!hasKnownHighUtilitySequence.contains(highUSequence)&&i<=indexOfItem&&j>=indexOfItem)
                    {
                        this.candidatesCount++;
                        hasKnownHighUtilitySequence.add(new ArrayList<>(highUSequence));
                    }
                }
            }
        }
        else{
            for(int i=indexOfItem-this.maxLength+1;i<=indexOfItem+this.maxLength&&i<sequenceAll.length;i++){
                highUSequence=new ArrayList<>();
                for(int j=0;j<this.maxLength&&(i+j)<sequenceAll.length;j++)
                {
                    highUSequence.add(sequenceAll[i+j]);
                    if(!hasKnownHighUtilitySequence.contains(highUSequence)&&highUSequence.contains(item))
                    {
                        this.candidatesCount++;
                        hasKnownHighUtilitySequence.add(new ArrayList<>(highUSequence));
                    }
                }
            }
        }
    }

    public void getMaxSequence_delete(int maxLength) {
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
        System.out.println("2222");
        runtime4 = System.currentTimeMillis() - startTime;

        // **优化序列处理**
        List<Integer> listItems = new ArrayList<>();  // 复用 List，减少创建对象
        for (Map.Entry<Integer, Integer[]> entry : mapTidToItems.entrySet()) {
            Integer[] items = entry.getValue();
            int length = items.length;
            System.out.println("AAA");

            for (int i = 0; i < length; i++) {
                if (items[i] == -1) {
                    continue; // 跳过分隔符
                }

                listItems.clear();  // 复用 List
                for (int j = i; j < length && (j - i + 1) <= maxLength; j++) {
                    if (items[j] == -1) {
                        break;
                    }
                    listItems.add(items[j]);
                }

                candidatesCount++;

                if (!listItems.isEmpty() && !isContains(listItems)) {
                    addSequence(new ArrayList<>(listItems));  // 传递副本，确保数据一致
                }
            }
        }
        System.out.println("333");
        System.out.println("输出最大序列");
        System.out.println(maxSequenceList);
        runtime5 = System.currentTimeMillis() - startTime;
    }

    public void getMaxSequence(int maxLength) {
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
        System.out.println("2222");
        runtime4 = System.currentTimeMillis() - startTime;

        // **优化序列处理**
        List<Integer> listItems = new ArrayList<>();  // 复用 List，减少创建对象
        for (Map.Entry<Integer, Integer[]> entry : mapTidToItems.entrySet()) {
            Integer[] items = entry.getValue();
            int length = items.length;
            System.out.println("AAA");

            for (int i = 0; i < length; i++) {
                if (items[i] == -1) {
                    continue; // 跳过分隔符
                }

                listItems.clear();  // 复用 List
                for (int j = i; j < length && (j - i + 1) <= maxLength; j++) {
                    if (items[j] == -1) {
                        break;
                    }
                    listItems.add(items[j]);
                }

                candidatesCount++;

                if (!listItems.isEmpty() && !isContains(listItems)) {
                    addSequence(new ArrayList<>(listItems));  // 传递副本，确保数据一致
                }
            }
        }
        System.out.println("333");
        System.out.println("输出最大序列");
        System.out.println(maxSequenceList);
        runtime5 = System.currentTimeMillis() - startTime;
    }

    public void processCoreAlgorithm() {
        System.out.println("当前运行到coreAlg");
        int size = maxSequenceList.size();
        int num_now = 0;
        for (List<Integer> sequence : maxSequenceList) {
            System.out.println(num_now + "/" + size);
            num_now++;
            int utilityOfSequence = countUtility(sequence);
            if (utilityOfSequence <= max_utility && hasProcessedSequenceList.contains(sequence) == false&&hasKnownHighUtilitySequence.contains(sequence)==false) {
                processLowUtilitySequence(sequence, utilityOfSequence);
            } else {
//                cut2(sequence);
                hasDeleteSequenceList.add(sequence);
                cut(sequence);
//                LUSM(sequence);
            }
        }
        printLowUtilitySequence();
    }

    private void processLowUtilitySequence(List<Integer> sequence, int utility) {
        if (!hasProcessedSequenceList.contains(sequence)&&hasKnownHighUtilitySequence.contains(sequence)==false) {
            System.out.println("获得低效用序列：" + sequence);
            hasProcessedSequenceList.add(sequence);
            candidatesCount++;
            patternCount++;
            lowUtilityPattern.put(sequence, utility);
//            lowUtilityPattern.put(sequence, utility);
            LUSM(sequence);
//            cut(sequence);

        }
    }

//    public void cut2(List<Integer> sequence) {
//        if (sequence.size() > 1) {
//            List<Integer> subFirstSequence = new ArrayList<>(sequence);
//            List<Integer> subLastSequence = new ArrayList<>(sequence);
//            subFirstSequence.remove(0); // 删除首项
//            subLastSequence.remove(subLastSequence.size() - 1); // 删除尾项
//            int firstUtility = countUtility(subFirstSequence);
//            int lastUtility = countUtility(subLastSequence);
//            if (firstUtility <= max_utility && hasProcessedSequenceList.contains(subFirstSequence) == false) {
//                processLowUtilitySequence(subFirstSequence, firstUtility);
//                hasProcessedSequenceList.add(subFirstSequence);
//            } else {
//                cut2(subFirstSequence);
//            }
//            if (lastUtility <= max_utility && hasProcessedSequenceList.contains(subLastSequence) == false) {
//                processLowUtilitySequence(subLastSequence, lastUtility);
//                hasProcessedSequenceList.add(subLastSequence);
//            } else {
//                cut2(subLastSequence);
//            }
//        } else {
////            System.out.println("处理一个项");
//            if (hasProcessedSequenceList.contains(sequence) == false) {
//                LUSMItem(sequence);
//                hasProcessedSequenceList.add(sequence);
//            }
//        }
//    }

    public void cut(List<Integer> sequence) {
        if (sequence.size() > 1) {
            List<List<Integer>> utilities = getUtilities(sequence);
            cutFirst(sequence, utilities);
            cutLast(sequence, utilities);
        } else {
//            System.out.println("处理一个项");
            if (hasProcessedSequenceList.contains(sequence) == false&&hasKnownHighUtilitySequence.contains(sequence)==false) {
                LUSMItem(sequence);
                hasProcessedSequenceList.add(sequence);
            }
        }
    }

    public void cutSequenceAll(List<Integer> sequence, List<List<Integer>> utilities) {
        if (sequence.size() > 1) {
            cutSequence(sequence, utilities, true);  // 剪除首项
            cutSequence(sequence, utilities, false); // 剪除尾项
        } else {
//            System.out.println("处理一个项");
            processSingleElementSequence(sequence);

        }
    }

    private void processSingleElementSequence(List<Integer> sequence) {
        if (hasProcessedSequenceList.contains(sequence) == false&&hasKnownHighUtilitySequence.contains(sequence)==false) {
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
//            LUSM(new ArrayList<>(newSequence));
            processSubSequence(new ArrayList<>(newSequence));
        } else {
            hasDeleteSequenceList.add(newSequence);
            cutSequenceAll(new ArrayList<>(newSequence), newUtilities);
        }
    }

    public void LUSMItem(List<Integer> sequence) {
        int utility = countUtility(sequence);
        if (utility <= max_utility) {
            processLowUtilitySequence(sequence, utility);
        }
    }

    public void cutFirst(List<Integer> sequence, List<List<Integer>> utilities) {
        List<List<Integer>> newUtilities = new ArrayList<>();
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
//            processSubSequence(new ArrayList<>(sequenceFirst));
            LUSM(sequenceFirst);
        } else {
            hasDeleteSequenceList.add(sequenceFirst);
            cutSequenceAll(sequenceFirst, newUtilities);
        }
    }

    public int countUtility_FL(List<List<Integer>> utilities) {
        return utilities.stream()
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .reduce(0, Integer::sum);
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
            hasDeleteSequenceList.add(sequenceLast);
            cutSequenceAll(sequenceLast, newUtilities);
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
        if (!hasProcessedSequenceList.contains(sequence) && !hasDeleteSequenceList.contains(sequence)&&hasKnownHighUtilitySequence.contains(sequence)==false) {
            if (countUtility(sequence) <= max_utility)
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
        if (subSequence.size() > 1 && !hasProcessedSequenceList.contains(subSequence)&&hasKnownHighUtilitySequence.contains(subSequence)==false) {
            int utility = countUtility(subSequence);
//            hasProcessedSequenceList.add(subSequence);
            if (utility <= max_utility && lowUtilityPattern.get(subSequence) == null) {
                processLowUtilitySequence(subSequence, utility);
//                LUSM(subSequence);
            } else {
//                LUSM(subSequence);
                cut(subSequence);
            }
        }
        if (subSequence.size() == 1) {
            processSingleElementSequence(subSequence);
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

    private void sortTrans(String input) throws IOException {

        Set<String> trans = new HashSet<>();
        String thisLine;
        BufferedReader myInput = null;
        try {
            myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(input))));
            // for each transaction (line) in the input file
            while ((thisLine = myInput.readLine()) != null) {
                // if the line is  a comment, is  empty or is a
                // kind of metadata
                if (thisLine.isEmpty() == true ||
                        thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%'
                        || thisLine.charAt(0) == '@') {
                    continue;
                }
                String[] partions = thisLine.split(":");
                // int transactionUtility = Integer.parseInt(partions[1]);

                String[] items = partions[0].split(" ");
                String itemsStr = Joiner.on("&").join(items);
                trans.add(itemsStr);
            }
        } catch (Exception e) {
            // catch exceptions
            e.printStackTrace();
        } finally {
            if (myInput != null) {
                // close the file
                myInput.close();
            }
        }

    }

    List<int[]> genContain(String input) throws IOException {
        List<List<Integer>> trans = new LinkedList<>();
        String thisLine;
        BufferedReader myInput = null;
        try {
            myInput = new BufferedReader(new InputStreamReader(new FileInputStream(input)));
            // for each transaction (line) in the input file
            while ((thisLine = myInput.readLine()) != null) {
                // if the line is  a comment, is  empty or is a
                // kind of metadata
                if (thisLine.isEmpty() == true ||
                        thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%'
                        || thisLine.charAt(0) == '@') {
                    continue;
                }
                String[] partions = thisLine.split(":");
                // int transactionUtility = Integer.parseInt(partions[1]);

                String[] items = partions[0].split(" ");
                List<Integer> tranOne = new ArrayList<>();
                for (int i = 0; i < items.length; i++) {
                    tranOne.add(Integer.valueOf(items[i]));
                }
                trans.add(tranOne);
            }
        } catch (Exception e) {
            // catch exceptions
            e.printStackTrace();
        } finally {
            if (myInput != null) {
                // close the file
                myInput.close();
            }
        }
        Collections.sort(trans, new Comparator<List<Integer>>() {
            @Override
            public int compare(List<Integer> o1, List<Integer> o2) {
                return o1.size() - o2.size();
            }
        });
        //去重
        Set<String> stringSet = new LinkedHashSet<>();
        for (int i = 0; i < trans.size(); i++) {
            List<Integer> temp = trans.get(i);
            stringSet.add(Joiner.on("&").join(temp));
        }
        trans.clear();

        List<int[]> res = new ArrayList<>();
        List<String> stringList = new ArrayList<>(stringSet);

        while (!stringList.isEmpty()) {
            //lastSize=trans.size();
            for (int i = stringList.size() - 2; i >= 0; i--) {

                String[] items = stringList.get(stringList.size() - 1).split("&");
                List<Integer> itemsList1 = Arrays.stream(Arrays.stream(items).mapToInt(Integer::parseInt).toArray()).boxed().collect(Collectors.toList());
                String[] items2 = stringList.get(i).split("&");
                List<Integer> itemsList2 = Arrays.stream(Arrays.stream(items2).mapToInt(Integer::parseInt).toArray()).boxed().collect(Collectors.toList());
                if (itemsList1.containsAll(itemsList2)) {
                    stringList.remove(i);
                    //i++;
                }
            }
            res.add(Arrays.stream(stringList.get(stringList.size() - 1).split("&")).mapToInt(Integer::parseInt).toArray());
            stringList.remove(stringList.size() - 1);
        }
        return res;
    }
}

