package java1;

import com.google.common.base.Joiner;
import java.util.*;
import java.util.stream.Collectors;
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
    Map<List<Integer>, Integer> hasProcessedSequenceListAndId = new LinkedHashMap<>();
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

    public void runAlgorithm(String input, int max_utility, int maxLength, BufferedWriter writer) throws IOException {
        System.out.println("此处是低效用非连续序列挖掘算法");
        this.writer=writer;
        this.max_utility = max_utility;
        this.maxLength = maxLength;
        startTime = System.currentTimeMillis();
        StringBuilder buffer = new StringBuilder();
        buffer.append("max_utility:").append(max_utility).append("\n");
        buffer.append("maxLength:").append(maxLength).append("\n");
        buffer.append("processCoreAlgorithm_improved").append("\n");
        buffer.append("processCoreAlgorithm").append("\n");
        writer.write(buffer.toString());
        writer.newLine();
        System.out.println("输出最大效用:" + max_utility);
        System.out.println("startTime:" + startTime);
        loadFile_sequence(input);
        System.out.println("输出count:" + sequenceId);
//        loadFile_sequence_delete(input);
//        loadFile(input);
        runtime2 = System.currentTimeMillis() - startTime;
//        System.out.println("111111");
        getMaxSequence(maxLength);
//        getMaxSequence_delete(maxLength);
        runtime3 = System.currentTimeMillis() - startTime;
        processCoreAlgorithm();
//        processCoreAlgorithm_improved();
        runtime = System.currentTimeMillis() - startTime;
        printLowUtilitySequence();
        showStates();
    }
    public static List<List<Integer>> deduplicateUtilities(List<List<Integer>> utilities) {
        List<List<Integer>> utilitiesNew=new ArrayList<>();
        List<Integer> sequenceId=new ArrayList<>();
        for(List<Integer> utility:utilities){
            int index=utilities.indexOf(utility);
            for(int i=index+1;i<utilities.size();i++){
                boolean key=true;
                List<Integer> utilityNew=utilities.get(i);
                for(int j=0;j<utility.size();j++){
                    if(utility.get(j)!=utilityNew.get(j)){
                        key=false;
                    }
                }
                if(key==true&&utility!=utilities.get(utilities.size()-1)){
                    sequenceId.add(i);
                }
            }
//            if(key==true){
//                utilities.remove(utility);
//                continue;
//            }
        }
        if(sequenceId.size()!=0){
//            for(int i=0;i<sequenceId.size();i++){
//                utilities.remove(sequenceId.get(i));
//            }
            for(int i=0;i<utilities.size();i++){
                if(!sequenceId.contains(i)){
                    utilitiesNew.add(utilities.get(i));
                }
            }
        }
        return utilitiesNew;
//        Set<List<Integer>> uniqueUtilities = new HashSet<>();
//        for (List<Integer> list : utilities) {
//            uniqueUtilities.add(List.copyOf(list));
//        }
//        return new ArrayList<>(uniqueUtilities);
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
        final int[] item1 = {0};
        final int[] indexOfItem = {0};
        Set<Integer> setOfItems = new TreeSet<>();
        Map<Integer, Integer[]> mapTidToItems = new LinkedHashMap<>();
        Map<Integer, Integer[]> mapTidToUtilities = new LinkedHashMap<>();
        sequenceId.add(0);
//        List<Integer> tidCountList = new ArrayList<>();
        List<Integer> itemList = new ArrayList<>();
        List<Integer> indexOfItemList = new ArrayList<>();
        try (BufferedReader myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path))))) {
            myInput.lines()
                    .filter(line -> !line.isEmpty() && line.charAt(0) != '#' && line.charAt(0) != '%' && line.charAt(0) != '@')
                    .forEach(thisLine -> {
                        String[] partitions = thisLine.split("SUtility:"); // 按 "SUtility:" 分割数据
                        if (partitions.length < 2) return;  // 如果没有有效的分割部分则跳过
                        indexOfItem[0] = 0;
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
//                                item1[0] = item;
//                                if (utility >= max_utility) {
//                                    utility = -1;
//                                    containsMaxUtility = true;
//                                    tidCountList.add(tidCount[0]);
//                                    itemList.add(item1[0]);
//                                    indexOfItemList.add(indexOfItem[0]);
//                                } else {
//                                    key = true;
//                                }
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
//                        if (containsMaxUtility) {
//                            setOfItems.add(-1);
//                        }

                        // 如果存在有效的项和效用，加入到 map 中
//                        if (key) {
                        count += itemsIntList.size();
                        mapTidToItems.put(tidCount[0], itemsIntList.toArray(new Integer[0]));
                        mapTidToUtilities.put(tidCount[0], utilsIntList.toArray(new Integer[0]));
                        sequenceId.add(count);
                        tidCount[0]++;
//                        }
                    });

            this.mapTidToItems = mapTidToItems;
            this.mapTidToUtilities = mapTidToUtilities;
//            for (int i = 0; i < tidCountList.size(); i++) {
//                addHasKnownHS(tidCountList.get(i), indexOfItemList.get(i), itemList.get(i));
//            }
//            System.out.println("输出高效用序列");
//            System.out.println(hasKnownHighUtilitySequence);
//            StringBuilder buffer = new StringBuilder();
//            buffer.append("输出高效用序列：").append(hasKnownHighUtilitySequence);
//            writer.write(buffer.toString());
//            System.out.println("输出高效用序列");
//            System.out.println(hasKnownHighUtilitySequence);
//            writer.newLine();
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

    private void printMapData(String mapName, Map<Integer, Integer[]> mapData) throws IOException {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Printing:"+mapName+":\n");
        System.out.println("Printing " + mapName + ":");
        for (Map.Entry<Integer, Integer[]> entry : mapData.entrySet()) {
            buffer.append("Line"+entry.getKey()+":");
            buffer.append(Arrays.toString(entry.getValue())+"\n");
            System.out.print("Line " + entry.getKey() + ": ");
            System.out.println(Arrays.toString(entry.getValue()));
        }
        writer.write(buffer.toString());
        writer.newLine();
    }

//    public void addHasKnownHS(int indexOfSequence, int indexOfItem, int item) {
//
//        List<Integer> highUSequence = new ArrayList<>();
//        Integer[] sequenceAll = this.mapTidToItems.get(indexOfSequence);
//        if (indexOfItem - this.maxLength + 1 < 0) {
//            for (int i = 0; i <= indexOfItem + this.maxLength && i < sequenceAll.length; i++) {
//                highUSequence = new ArrayList<>();
//                for (int j = 0; j < this.maxLength && ((i + j) < sequenceAll.length); j++) {
//                    highUSequence.add(sequenceAll[i + j]);
//                    if (!hasKnownHighUtilitySequence.contains(highUSequence) && i <= indexOfItem && j >= indexOfItem) {
//                        this.candidatesCount++;
//                        hasKnownHighUtilitySequence.add(new ArrayList<>(highUSequence));
//                    }
//                }
//            }
//        } else {
//            for (int i = indexOfItem - this.maxLength + 1; i <= indexOfItem + this.maxLength && i < sequenceAll.length; i++) {
//                highUSequence = new ArrayList<>();
//                for (int j = 0; j < this.maxLength && (i + j) < sequenceAll.length; j++) {
//                    highUSequence.add(sequenceAll[i + j]);
//                    if (!hasKnownHighUtilitySequence.contains(highUSequence) && highUSequence.contains(item)) {
//                        this.candidatesCount++;
//                        hasKnownHighUtilitySequence.add(new ArrayList<>(highUSequence));
//                    }
//                }
//            }
//        }
//    }

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

    public void getMaxSequence(int maxLength) throws IOException {
        StringBuilder buffer = new StringBuilder();
        buffer.append("当前运行到 preprocess\n");
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
        buffer.append("输出最大序列");
        buffer.append(maxSequenceList);
        System.out.println("输出最大序列");
        System.out.println(maxSequenceList);
        writer.write(buffer.toString());
        writer.newLine();
        runtime5 = System.currentTimeMillis() - startTime;
    }

    public void processCoreAlgorithm() throws IOException {
        System.out.println("当前运行到coreAlg");
        int size = maxSequenceList.size();
        int num_now = 0;
        for (List<Integer> sequence : maxSequenceList) {
            System.out.println(num_now + "/" + size);
            StringBuilder buffer = new StringBuilder();
            buffer.append(num_now + "/" + size);
            writer.write(buffer.toString());
            writer.newLine();
            num_now++;
            int utilityOfSequence = countUtility(sequence);
            if (utilityOfSequence == -1) System.out.println("此序列为空！");
//            if (utilityOfSequence <= max_utility && hasProcessedSequenceList.contains(sequence) == false&&hasProcessedSequenceListAndId.get(sequence) == null && hasKnownHighUtilitySequence.contains(sequence) == false) {
                if (utilityOfSequence <= max_utility && hasProcessedSequenceList.contains(sequence) == false&&hasProcessedSequenceListAndId.get(sequence) == null) {
                processLowUtilitySequence(sequence, utilityOfSequence, -1);
            } else {
//                cut2(sequence);
                hasDeleteSequenceList.add(sequence);
                cut(sequence);
//                LUSM(sequence);
            }
        }
//        printLowUtilitySequence();
    }

    private void processLowUtilitySequence(List<Integer> sequence, int utility, int itemNum) throws IOException {
        boolean key=false;
        if(hasProcessedSequenceListAndId.get(sequence)==null){
            StringBuilder buffer = new StringBuilder();
            buffer.append("获得低效用非连续序列：" + sequence).append("\n");
            writer.write(buffer.toString());
            writer.newLine();
            System.out.println("获得低效用序列：" + sequence);
            hasProcessedSequenceList.add(sequence);
            candidatesCount++;
            patternCount++;
            lowUtilityPattern.put(sequence, utility);
        }
        if ((hasProcessedSequenceListAndId.get(sequence)==null||hasProcessedSequenceListAndId.get(sequence) >=itemNum) ) {
//            lowUtilityPattern.put(sequence, utility);
            key=true;
//            cut(sequence);
        }
        if(hasProcessedSequenceListAndId.get(sequence)==null){
            hasProcessedSequenceListAndId.put(sequence,itemNum);
        }else{
            hasProcessedSequenceListAndId.put(sequence,itemNum);
        }
        if(key){
            List<List<Integer>> utilities = getUtilities(sequence);
            if (itemNum + 1 < sequence.size()) {
                LUSM(sequence, utilities, itemNum + 1);
            }
        }
//        List<List<Integer>> utilities = getUtilities(sequence);
//        if (itemNum + 1 < sequence.size()&&sequence.size()!=0) {
//            LUSM(sequence, utilities, itemNum + 1);
//        }
//        else{
//            System.out.println("11111");
//        }
    }

    private void processLowUtilitySequence_improved(List<Integer> sequence, int utility) throws IOException {
//        if (!hasProcessedSequenceList.contains(sequence) && hasKnownHighUtilitySequence.contains(sequence) == false) {
        if (!hasProcessedSequenceList.contains(sequence)) {
            System.out.println("获得低效用序列：" + sequence);
            StringBuilder buffer = new StringBuilder();
            buffer.append("获得低效用序列：" + sequence).append("\n");
            writer.write(buffer.toString());
            writer.newLine();
            hasProcessedSequenceList.add(sequence);
            candidatesCount++;
            patternCount++;
            lowUtilityPattern.put(sequence, utility);
//            lowUtilityPattern.put(sequence, utility);
//            LUSM(sequence);
//            cut(sequence);

        }
    }

    public void processCoreAlgorithm_improved() throws IOException {
        System.out.println("当前运行到coreAlg改进版");
        int size = maxSequenceList.size();
        int utilityOfSequence = -1;
        int num_now = 0;
        for (List<Integer> sequence : maxSequenceList) {
            StringBuilder buffer = new StringBuilder();
            buffer.append(num_now + "/" + size);
            System.out.println(num_now + "/" + size);
            writer.write(buffer.toString());
            writer.newLine();
            System.out.println(num_now + "/" + size);
            num_now++;
            List<List<Integer>> utilities = new ArrayList<>();
            utilities = getUtilities(sequence);
            utilityOfSequence = countUtility(sequence);
            if (utilityOfSequence == -1) System.out.println("此序列为空！");
            if (utilityOfSequence <= max_utility) {
                processLowUtilitySequence_improved(sequence, utilityOfSequence);
            }
            List<List<Integer>> utilitiesNew2=deduplicateUtilities(utilities);
            LUM1(0, sequence, utilitiesNew2);
//            int utilityOfSequence = countUtility(sequence);
//            if (utilityOfSequence <= max_utility && hasProcessedSequenceList.contains(sequence) == false&&hasKnownHighUtilitySequence.contains(sequence)==false) {
//                processLowUtilitySequence(sequence, utilityOfSequence);
//            } else {
////                cut2(sequence);
//                hasDeleteSequenceList.add(sequence);
//                cut(sequence);
////                LUSM(sequence);
//            }
        }
//        printLowUtilitySequence();
    }

    public void LUM1(int itemNum, List<Integer> sequence, List<List<Integer>> utilities) throws IOException {
        List<Integer> sequenceNew = new ArrayList<>();
        int utilityOfSequence_father = 0;
        int utilityOfSequence = 0;
        List<List<Integer>> utilitiesNew = new ArrayList<>();
        if (itemNum == 0) {
            sequenceNew.add(sequence.get(0));
            for (List<Integer> utility : utilities) {
                utilityOfSequence_father += utility.get(0);
            }
            if (utilityOfSequence_father <= max_utility) {
                utilityOfSequence = countUtility(sequenceNew);
                if (utilityOfSequence == -1) System.out.println("此序列为空！");
                if (utilityOfSequence <= max_utility) {
                    processLowUtilitySequence_improved(sequenceNew, utilityOfSequence);
                }
                //保留该项
                if (itemNum + 1 < sequence.size()) {
                    LUM1(itemNum + 1, sequence, utilities);
                }
            }


//            不保留该项
            if (sequence.size() != 1) {
                List<Integer> sequenceNewCutFirst = new ArrayList<>(sequence);
                sequenceNewCutFirst.remove(0);
                List<List<Integer>> utilitiesNew1 = new ArrayList<>(utilities);
                for (List<Integer> utility : utilitiesNew1) {
                    utility.remove(0);
                }
                List<List<Integer>> utilitiesNew2=deduplicateUtilities(utilitiesNew1);
                LUM1(0, sequenceNewCutFirst, utilitiesNew2);
            }
        } else {
            int utilityOfNewSequence = 0;
            for (int i = 0; i <= itemNum; i++) {
                sequenceNew.add(sequence.get(i));
            }
            for (List<Integer> utility : utilities) {
                List<Integer> utilityNew = new ArrayList<>();
                for (int i = 0; i <= itemNum; i++) {
                    utilityNew.add(utility.get(i));
                    utilityOfNewSequence += utility.get(i);
                }
                utilitiesNew.add(utilityNew);
            }
            List<List<Integer>> utilitiesNew2=deduplicateUtilities(utilitiesNew);
//            deduplicateUtilities(utilitiesNew);
            if (utilityOfNewSequence <= max_utility) {
                utilityOfSequence = countUtility(sequenceNew);
                if (utilityOfSequence == -1) System.out.println("此序列为空！");
                if (utilityOfSequence <= max_utility) {
                    processLowUtilitySequence_improved(sequenceNew, utilityOfSequence);
                }
                //保留该项
                if (itemNum + 1 < sequence.size()) {
                    LUM1(itemNum + 1, sequence, utilities);
                }
            }
//                不保留该项
                if (sequence.size() != 1) {
                    List<Integer> sequenceNewCutFirst = new ArrayList<>(sequence);
                    sequenceNewCutFirst.remove(itemNum);
                    List<List<Integer>> utilitiesNew1 = new ArrayList<>();
                    for (List<Integer> utility2 : utilities) {
                        List<Integer> utility = new ArrayList<>(utility2);
                        utilitiesNew1.add(utility);
                    }
                    for (List<Integer> utility : utilitiesNew1) {
                        utility.remove(itemNum);
                    }
                    List<List<Integer>> utilitiesNew3=deduplicateUtilities(utilitiesNew1);
                    if (itemNum < sequenceNewCutFirst.size()) {
                        LUM1(itemNum, sequenceNewCutFirst, utilitiesNew3);
                    }
                }
            }

//        System.out.println("输出成功！");
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

    public void cut(List<Integer> sequence) throws IOException {
        if (sequence.size() > 1) {
            List<List<Integer>> utilities = getUtilities(sequence);
            LUSM(sequence, utilities, 0);
//            cutFirst(sequence, utilities);
//            cutLast(sequence, utilities);
        } else {
//            System.out.println("处理一个项");
//            if (hasProcessedSequenceList.contains(sequence) == false && hasKnownHighUtilitySequence.contains(sequence) == false) {
                if (hasProcessedSequenceList.contains(sequence) == false) {
                LUSMItem(sequence);
                hasProcessedSequenceList.add(sequence);
            }
        }
    }

    public void cutSequenceAll(List<Integer> sequence, List<List<Integer>> utilities) throws IOException {
        if (sequence.size() > 1) {
            cutSequence(sequence, utilities, true);  // 剪除首项
            cutSequence(sequence, utilities, false); // 剪除尾项
        } else {
//            System.out.println("处理一个项");
            processSingleElementSequence(sequence);

        }
    }

    private void processSingleElementSequence(List<Integer> sequence) throws IOException {
//        if (hasProcessedSequenceList.contains(sequence) == false && hasKnownHighUtilitySequence.contains(sequence) == false) {
        if (hasProcessedSequenceList.contains(sequence) == false) {
            LUSMItem(sequence);
            hasProcessedSequenceList.add(sequence);
        }
    }

    private void cutSequence(List<Integer> sequence, List<List<Integer>> utilities, boolean isFirst) throws IOException {
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

    public void LUSMItem(List<Integer> sequence) throws IOException {
        int utility = countUtility(sequence);
        if (utility == -1) System.out.println("此序列为空！");

        if (utility <= max_utility) {
            processLowUtilitySequence(sequence, utility, 0);
        }
    }

    public void cutFirst(List<Integer> sequence, List<List<Integer>> utilities) throws IOException {
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


    public void cutLast(List<Integer> sequence, List<List<Integer>> utilities) throws IOException {
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
        if (sequence.size() == 0) {
            return -1;
        }
        int allUtility = 0;
        int Utility = 0;
        boolean isExist = true;
        BitSet bitSet = mapItemToBitSet.get(sequence.get(0));
        int index = bitSet.nextSetBit(0);
        int upindex = -1;
        for (int indexItem : sequenceId) {
            if (indexItem > index) {
                upindex = indexItem;
                break;
            }
        }
        while (index != -1) {
            Utility = 0;
            Utility += getUtility(index);
            int newIndex = index;
            for (int i = 1; i < sequence.size(); i++) {
                isExist = false;
                while (++newIndex != -1 && (newIndex < upindex)) {
                    BitSet bitSet1 = mapItemToBitSet.get(sequence.get(i));
                    if (bitSet1.get(newIndex) == true) {
                        isExist = true;
                        break;
                    }
                }
                if (newIndex < upindex) {
                    Utility += getUtility(newIndex);
                }
            }
            if (isExist == true) {
                allUtility += Utility;
            }
            index = bitSet.nextSetBit(index + 1);
            for (int indexItem : sequenceId) {
                if (indexItem > index) {
                    upindex = indexItem;
                    break;
                }
            }
        }
        return allUtility;
    }

    public List<List<Integer>> getUtilities(List<Integer> sequence) {
        List<List<Integer>> utilities = new ArrayList<>();
        List<Integer> utility;
        int upindex=-1;
        boolean isExist = true;
        BitSet bitSet = mapItemToBitSet.get(sequence.get(0));
        int index = bitSet.nextSetBit(0);
        for(int indexItem:sequenceId){
            if(indexItem>index){
                upindex=indexItem;
                break;
            }
        }
        while (index != -1) {
            utility = new ArrayList<>();
            utility.add(getUtility(index));
            isExist = true;
            int newIndex = index;
            for (int i = 1; i < sequence.size(); i++) {
                isExist=false;
                while(++newIndex!=-1&&(newIndex<upindex)){
                    BitSet bitSet1 = mapItemToBitSet.get(sequence.get(i));
                    if (bitSet1.get(newIndex) == true) {
                        isExist = true;
                        break;
                    }
                }
                if(newIndex<upindex){
                    utility.add(getUtility(newIndex));
                }
            }
            if (isExist == true) {
                utilities.add(utility);
            }
            index = bitSet.nextSetBit(index + 1);
            for(int indexItem:sequenceId){
                if(indexItem>index){
                    upindex=indexItem;
                    break;
                }
            }
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
        if (utilities == null) {
            System.out.println("此时utilities==null");
        }
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

    public void showStates() throws IOException {
        StringBuilder buffer = new StringBuilder();
        buffer.append("存储数据运行时间为：" + (double) runtime2 / 1000 + "s").append("\n");
        buffer.append("获得项的位图运行时间为：" + (double) runtime4 / 1000 + "s").append("\n");
        buffer.append("获得最长序列运行时间为：" + (double) runtime5 / 1000 + "s").append("\n");
        buffer.append("预处理运行时间为：" + (double) runtime3 / 1000 + "s").append("\n");
        buffer.append("运行时间为：" + (double) runtime / 1000 + "s").append("\n");
        buffer.append("运行内存为：" + MemoryLogger.getInstance().getMaxMemory() + "MB").append("\n");
        buffer.append("总的内存为：" + (double) Runtime.getRuntime().totalMemory() / 1024d / 1024d + "MB").append("\n");
        buffer.append("空闲内存为：" + (double) Runtime.getRuntime().freeMemory() / 1024d / 1024d + "MB").append("\n");
        buffer.append("候选集数目为：" + candidatesCount).append("\n");
        buffer.append("模式数目为：" + patternCount).append("\n");
        writer.write(buffer.toString());
        writer.newLine();
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

    public void LUSM(List<Integer> sequence) throws IOException {
//        if (!hasProcessedSequenceList.contains(sequence) && !hasDeleteSequenceList.contains(sequence) && hasKnownHighUtilitySequence.contains(sequence) == false) {
        if (!hasProcessedSequenceList.contains(sequence) && !hasDeleteSequenceList.contains(sequence)) {
            if (countUtility(sequence) <= max_utility && countUtility(sequence) != 0)
                processLowUtilitySequence(sequence, countUtility(sequence), 0);
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

    public void LUSM(List<Integer> sequence, List<List<Integer>> utilities, int itemNum) throws IOException {
        //        if (hasKnownHighUtilitySequence.contains(sequence) == false) {
//            if(sequence.size()==1){
//                if(countUtility(sequence)<=max_utility){
//                    processLowUtilitySequence(sequence,countUtility(sequence),0);
//                }
//            }
            /*计算效用值，如果效用值小于等于门槛，则计入数据
             * */
//        int utilityOfSequence=countUtility(sequence);
//        if(utilityOfSequence<=max_utility){
//            processLowUtilitySequence(sequence,utilityOfSequence);
//        }
            /*保留该项
             * */
            if(itemNum==sequence.size()-1){
                int utilityOfSequence=countUtility(sequence);
                if(utilityOfSequence<=max_utility){
                    processLowUtilitySequence(sequence,utilityOfSequence,itemNum);
                }
            }
            if (itemNum + 1 < sequence.size()) {
                LUSM(sequence, utilities, itemNum + 1);
            }

            /*不保留该项
             * 删除该项，获取删除该项之后整个序列的宽度扩展:
             * 若宽度扩展大于门槛，则获取子序列的宽度扩展，重复该操作
             * 若宽度扩展小于等于该门槛
             * */
            List<Integer> sequenceNew = new ArrayList<>(sequence);
            sequenceNew.remove(itemNum);
            if(sequenceNew.size()!=0){
                List<List<Integer>> utilitiesNew = new ArrayList<>();
                for (List<Integer> utility : utilities) {
                    List<Integer> utilityNew = new ArrayList<>(utility);
                    utilityNew.remove(itemNum);
                    utilitiesNew.add(utilityNew);
                }
                int utilityOfSequence = countUtility_FL(utilitiesNew);
                if (sequenceNew.size() == 1) {
                    if (countUtility(sequenceNew) <= max_utility) {
                        processLowUtilitySequence(sequenceNew, countUtility(sequenceNew), 0);
                    }
                }
                if (utilityOfSequence > max_utility&&itemNum<sequenceNew.size()) {
//                    hasKnownHighUtilitySequence.add(sequenceNew);
                    LUSM_width(sequenceNew, utilitiesNew, itemNum);
                } else {
//                List<Integer> sequence111=new ArrayList<>();
//                sequence111.add(1);
//                sequence111.add(4);
//                if(sequence.equals(sequence111)){
//                    System.out.println("输出1111111");
//                }
                    utilityOfSequence = countUtility(sequenceNew);
                    if (utilityOfSequence <= max_utility) {
                        processLowUtilitySequence(sequenceNew, utilityOfSequence, itemNum - 1);
                    } else {
                        if (itemNum < sequenceNew.size()) {
                            LUSM(sequenceNew, utilitiesNew, itemNum);
                        }
                    }
                }
            }
        }


    public void LUSM_width(List<Integer> sequence, List<List<Integer>> utilities, int itemNum) throws IOException {
        if ((hasProcessedSequenceListAndId.get(sequence)==null||hasProcessedSequenceListAndId.get(sequence) >=itemNum) ) {
//            lowUtilityPattern.put(sequence, utility);
            List<Integer> sequenceNew = new ArrayList<>(sequence);
            sequenceNew.remove(itemNum);
            List<List<Integer>> utilitiesNew = new ArrayList<>();
            for (List<Integer> utility : utilities) {
                List<Integer> utilityNew = new ArrayList<>(utility);
                utilityNew.remove(itemNum);
                utilitiesNew.add(utilityNew);
            }
            int utilityOfSequence = countUtility_FL(utilitiesNew);
            if (utilityOfSequence > max_utility&&itemNum<sequenceNew.size()) {
                LUSM_width(sequenceNew, utilitiesNew, itemNum);
            } else {
                if(sequenceNew.size()==itemNum&&itemNum!=0){
                    int utilityOfNewSequence=countUtility(sequenceNew);
                    if(utilityOfNewSequence<=max_utility){
                        processLowUtilitySequence(sequenceNew,utilityOfNewSequence,itemNum);
                    }
                }
                if(itemNum<sequenceNew.size()){
                    LUSM(sequenceNew, utilitiesNew, itemNum);
                }
            }
//            不删除该项
            if(itemNum+1<sequence.size()){
                LUSM_width(sequence, utilities, itemNum + 1);
            }
            if(itemNum+1==sequence.size()){
                int utilityOfNewSequence=countUtility(sequence);
                if(utilityOfNewSequence<=max_utility){
                    processLowUtilitySequence(sequence,utilityOfNewSequence,itemNum);
                }
            }
        }
        if(hasProcessedSequenceListAndId.get(sequence)==null){
            hasProcessedSequenceListAndId.put(sequence,itemNum);
        }else{
            hasProcessedSequenceListAndId.put(sequence,itemNum);
        }
//            cut(sequence);
//        if (hasKnownHighUtilitySequence.contains(sequence) == false) {
////            删除该项
//        }
    }

    private void processSubSequence(List<Integer> subSequence) throws IOException {
//        if (subSequence.size() > 1 && !hasProcessedSequenceList.contains(subSequence) && hasKnownHighUtilitySequence.contains(subSequence) == false) {
        if (subSequence.size() > 1 && !hasProcessedSequenceList.contains(subSequence)) {
            int utility = countUtility(subSequence);
            if (utility == -1) System.out.println("该序列为空");
//            hasProcessedSequenceList.add(subSequence);
            if (utility <= max_utility && lowUtilityPattern.get(subSequence) == null) {
                processLowUtilitySequence(subSequence, utility, 0);
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

    public void printLowUtilitySequence() throws IOException {
        StringBuilder buffer = new StringBuilder();
        buffer.append("低效用非连续序列有："+patternCount+"个").append("\n");
        System.out.println("低效用序列有:" + patternCount + "个");
        if (patternCount > 0) {
            buffer.append("输出低效用序列：").append("\n");
            System.out.println("输出低效用序列：");
            for (Map.Entry<List<Integer>, Integer> entry : lowUtilityPattern.entrySet()) {
                buffer.append("序列：" + entry.getKey() + "  效用：" + entry.getValue()).append("\n");
                System.out.println("序列：" + entry.getKey() + "  效用：" + entry.getValue());
            }
        }
        System.out.println("输出低效用序列的数量：" + lowUtilityPattern.size());
        buffer.append("输出低效用序列的数量：" + lowUtilityPattern.size()).append("\n");
        writer.write(buffer.toString());
        writer.newLine();
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

