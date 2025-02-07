package java1;

import com.google.common.base.Joiner;
import org.jetbrains.annotations.NotNull;

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
        startTime = System.currentTimeMillis();
        System.out.println("输出最大效用:" + max_utility);
        System.out.println("startTime:" + startTime);
//
        loadFile(input);
        runtime2 = System.currentTimeMillis() - startTime;
        preprocess(maxLength);
        runtime3 = System.currentTimeMillis() - startTime;
//        coreAlg();
        processCoreAlgorithm();
        runtime = System.currentTimeMillis() - startTime;
        showStates();
//        //sortTrans(input);
//        String delPrex = null;
//        List<int[]> res = genContain(input);
//
//        writer = new BufferedWriter(new FileWriter(output));
//
//
//        for (int i = 0; i < res.size(); i++) {
////            System.out.print("输出res["+i+"]");
////            for(int j=0;j<res.get(i).length;j++){
////                System.out.print(res.get(i)[j]);
////            }
////            System.out.println(" ");
//            LUM(delPrex, res.get(i));
//            preSet.add(res.get(i));
//        }
//        runtime = System.currentTimeMillis() - startTime;
//
//        writer.close();
    }

    public void LUM(String delPre, int[] itemset) throws IOException {

        for (int[] maxItemset : preSet) {
            List<Integer> l1 = Arrays.stream(maxItemset).boxed().collect(Collectors.toList());
//            System.out.println("输出l1："+l1);
            List<Integer> l2 = Arrays.stream(itemset).boxed().collect(Collectors.toList());
//            System.out.println("输出l2："+l2);
            if (l1.containsAll(l2)) {
                return;
            }
        }
//        System.out.println(" ");
//        System.out.print("输出itemset:");
//        for(int i=0;i<itemset.length;i++){
//            System.out.print(itemset[i]);
//        }
//        System.out.println(" ");
        candidatesCount++;
//        System.out.println("输出candidatesCount:"+candidatesCount);
        ULB ulb = generator.getUBofItemset(itemset);
        //获取当前项集的utility
        int uOfRoot = ulb.getUofItemset();
//        System.out.print("输出itemset:");
//        for(int i=0;i<itemset.length;i++){
//            System.out.print(itemset[i]);
//        }
//        System.out.println(" ");
//        System.out.println("uOfRoot:"+uOfRoot);
        if (uOfRoot <= max_utility) {
            myprint(itemset, uOfRoot);
            candidatesCount++;
            patternCount++;
        }
        //不需要考虑一项集的扩展
        if (itemset.length == 1) {
            return;
        }

        int lbOfExD;
        if (delPre != null) {
            String[] delPreArr2 = delPre.split("&");
            int lastItem = Integer.valueOf(delPreArr2[delPreArr2.length - 1]);
            //获取深度剪枝的下届，若>maxutil,则不考虑扩展
//            得到该项集的叶子收缩
            lbOfExD = ulb.getLBofExD(lastItem);
        } else {
            //获取深度剪枝的下届，若>maxutil,则不考虑扩展
            lbOfExD = ulb.getMinUOfItem();
//            System.out.println("输出最小效用："+lbOfExD);
        }
        MemoryLogger.getInstance().checkMemory();
        //要不要放到上方的里面(不放到里面因为如果本身不满足条件的话，扩展有可能满足条件，放到上面会忽略扩展)
        //考虑其扩展

        //深度剪枝
        if (lbOfExD <= max_utility) {
            if (delPre != null) {
                //只能删除大于lastitem的元素
                String[] delPreArr = delPre.split("&");
                int lastItem = Integer.valueOf(delPreArr[delPreArr.length - 1]);
                for (int i = 0; i < itemset.length; i++) {
                    if (itemset[i] < lastItem) {
                        continue;
                    }
                    //获取宽度剪枝下届，若下届>maxutil,本身及扩展都剪去
                    int lbOfExB = ulb.getLBofExW(itemset[i]);
                    int[] extendsion = delOneOfItemset(itemset, i, itemset[i]);
                    if (lbOfExB <= max_utility) {
                        if (extendsion.length > 0) {
                            // mycheck(delPre+"&"+itemset[i],extendsion,exteFile);
                            LUM(delPre + "&" + itemset[i], extendsion);
                        }

                    }//else{
                    //myprintExtension(extendsion,lbOfExB,false);
                    //}
                }
            } else //delpre==null
            {
                for (int i = 0; i < itemset.length; i++) {
                    //获取宽度剪枝下届，若下届>maxutil,本身及扩展都剪去
                    int lbOfExB = ulb.getLBofExW(itemset[i]);
                    int[] extendsion = delOneOfItemset(itemset, i, itemset[i]);
                    if (lbOfExB <= max_utility) {
                        if (extendsion.length > 0) {
                            //  mycheck(String.valueOf(itemset[i]),extendsion,exteFile);
                            LUM(String.valueOf(itemset[i]), extendsion);
                        }
                    }
//                    else {
//                        myprintExtension(extendsion,lbOfExB,false);
//                    }
                }
            }


        }
//        else{
//            myprintExtension(itemset,lbOfExD,true);
//        }

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


    private void myprintExtension(int[] itemset, int Lowbound, boolean isDepthPrune) {
        for (int i = 0; i < itemset.length; i++) {
            if (i == itemset.length - 1) {
                System.out.print(itemset[i] + ": lower bound = ");
            } else {
                System.out.print(itemset[i] + " ");
            }
        }
        if (isDepthPrune) {
            System.out.println(Lowbound + " 深度剪枝，扩展被过滤");
        } else {
            System.out.println(Lowbound + " 宽度剪枝，扩展被过滤");
        }

    }

    private int[] delOneOfItemset(int[] itemset, int index, int item) {
//        if (itemset.length==1){
//            return null;
//        }
        int[] res = new int[itemset.length - 1];
        if (itemset[index] == item) {
            //int i = 0, j = 0;
            for (int i = 0, j = 0; i < itemset.length && j < res.length; i++) {
                if (i == index) {
                    continue;
                } else {
                    res[j++] = itemset[i];
                }
            }
        } else {
            System.out.println("del item error");
        }
        return res;
    }

    //    public void loadFile(String path) throws IOException {
//        Set<Integer> setOfItems = new TreeSet<>();
//        Map<Integer, Integer[]> mapTidToItems = new LinkedHashMap<>();
//        Map<Integer, Integer[]> mapTidToUtilities = new LinkedHashMap<>();
//        int tidCount = 0;
//        sequenceId.add(0);
//        String thisLine;
//        BufferedReader myInput = null;
//        try {
//            myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path))));
//            // for each transaction (line) in the input file
//            while ((thisLine = myInput.readLine()) != null) {
//                // if the line is  a comment, is  empty or is a
//                // kind of metadata
//                if (thisLine.isEmpty() == true ||
//                        thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%'
//                        || thisLine.charAt(0) == '@') {
//                    continue;
//                }
//
//                String[] partions = thisLine.split(":");
//                // int transactionUtility = Integer.parseInt(partions[1]);
//                String[] items = partions[0].split(" ");
//                String[] utilities = partions[2].split(" ");
//                Integer[] itemsInt = new Integer[items.length];
//                Integer[] utilsInt = new Integer[utilities.length];
//                for (int i = 0; i < items.length; i++) {
//                    itemsInt[i] = Integer.valueOf(items[i]);
//                    utilsInt[i] = Integer.valueOf(utilities[i]);
//                    count++;
//                    if (utilsInt[i] >= max_utility) {
//                        itemsInt[i] = -1;
//                        setOfItems.add(-1);
//                        utilsInt[i] = -1;
//                    } else {
//                        setOfItems.add(itemsInt[i]);
//                    }
//                }
//                mapTidToItems.put(tidCount, itemsInt);
//                mapTidToUtilities.put(tidCount, utilsInt);
//                sequenceId.add(count);
//                tidCount++;
//            }
//            this.mapTidToItems = mapTidToItems;
//            this.mapTidToUtilities = mapTidToUtilities;
////            输出mapTidToItems的数据
//            for (Map.Entry<Integer, Integer[]> entry : mapTidToItems.entrySet()) {
////                System.out.println(entry);
//                System.out.println("第" + entry.getKey() + "行：");
//                for (int i = 0; i < entry.getValue().length; i++) {
//                    System.out.print(entry.getValue()[i] + " ");
//                }
//                System.out.println(" ");
//            }
////            输出mapTidToUtilities的数据
//            for (Map.Entry<Integer, Integer[]> entry : mapTidToUtilities.entrySet()) {
//                System.out.println("第" + entry.getKey() + "行：");
//                for (int i = 0; i < entry.getValue().length; i++) {
//                    System.out.print(entry.getValue()[i] + " ");
//                }
//                System.out.println(" ");
//            }
////            System.out.println("sequenceId"+sequenceId);
////            System.out.println(mapTidToItems);
////            System.out.println(mapTidToUtilities);
//        } catch (Exception e) {
//            // catch exceptions
//            e.printStackTrace();
//        } finally {
//            if (myInput != null) {
//                // close the file
//                myInput.close();
//            }
//        }
//
////        generator = new Generator3(setOfItems, tidCount);
////        generator.setGenerator(mapTidToItems, mapTidToUtilities);
//    }
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

    /*preprocess()
     * 使用策略1，裁剪掉每个序列中，效用大于最低效用门槛的项，如果某个序列中所有的项都被裁剪，则裁剪掉整个序列。
     *
     * */
//    public void preprocess(int maxLength) {
//        System.out.println("当前运行到preprocess");
//        Integer key;
//        int countBit = 0;
////        创建每一项的位图
//        for (Map.Entry<Integer, Integer[]> entry : this.mapTidToItems.entrySet()) {
//            Integer[] items = entry.getValue();
//            for (int i = 0; i < items.length; i++) {
//                key = items[i];
//                BitSet bitSet = mapItemToBitSet.get(key);
//                if (bitSet == null) {
//                    BitSet bitSet1 = new BitSet();
//                    mapItemToBitSet.put(key, bitSet1);
////                    System.out.println("输出"+key+"的bitSet为空");
//                }
//                bitSet = mapItemToBitSet.get(key);
//                bitSet.set(countBit);
//                countBit++;
//            }
//        }
//        runtime4 = System.currentTimeMillis() - startTime;
//        List<Integer> listItems;
//        List<Integer> listUtility;
//        boolean key1 = false;
//        this.maxLength = maxLength;
//        for (Map.Entry<Integer, Integer[]> entry : this.mapTidToItems.entrySet()) {
//            Integer key2 = entry.getKey();
////            Integer[] utilitis=this.mapTidToUtilities.get(key2);
//            Integer[] items = entry.getValue();
//            for (int i = 0; i < items.length; i++) {
////                if (key1==true){
////                    key=false;
////                    break;
////                }
////                listUtility=new ArrayList<>();
//                listItems = new ArrayList<>();
//                if (items[i] == -1) {
//                    continue;
//                }
//                for (int j = i; j < items.length && ((j - i + 1) <= maxLength); j++) {
//                    if (items[j] == -1) {
//                        key1 = true;
//                        break;
//                    }
//                    listItems.add(items[j]);
////                    listUtility.add(utilitis[j]);
//                    if (j == items.length - 1) {
//                        key1 = true;
//                    }
//                }
//                candidatesCount++;
////                System.out.println(list);
//                if (!isContains(listItems)) {
////                    maxSequenceList.add(list);
//                    addSequence(listItems);
////                    maxSequenceListUtility.add(listUtility);
//                }
////                System.out.println("输出maxSequenceList"+maxSequenceList);
//            }
//        }
//        System.out.println("输出最大序列");
//        System.out.println(maxSequenceList);
////        System.out.println(maxSequenceListUtility);
//        runtime5 = System.currentTimeMillis() - startTime;
//
////        List<Integer> x1 = Arrays.asList(1, 3, 4);
////        List<Integer> x2 = Arrays.asList(3, 4);
////
////        if (containsSublist(x1, x2)) {
////            System.out.println("x1 按顺序包含 x2");
////        } else {
////            System.out.println("x1 不按顺序包含 x2");
////        }
////        System.out.println("输出x1"+x1);
////        String string1=ListToString(x1);
////        System.out.println("输出string1"+string1);
////        String string1="11";
////        String string2="1";
////        if(string1.contains(string2))
////        {
////            System.out.println("1包含于11中");
////        }
////        for(Map.Entry<Integer,BitSet> entry:this.mapItemToBitSet.entrySet()){
////            System.out.println("输出entry:"+entry);
////            System.out.println(entry.getKey());
////            System.out.println(entry.getValue());
////            BitSet bitSets=mapItemToBitSet.get(entry);
////            System.out.println("输出项的位图：");
////            System.out.println(bitSets.size());
////        }
////            Integer[] utilities = entry.getValue();
////            Integer[] items = this.mapTidToItems.get(key);
////            for(int i=0;i<utilities.length;i++)
////            {
////                Integer utility = Integer.valueOf(utilities[i]);
////                Integer item=Integer.valueOf(items[i]);
////                Integer itemToUtility=this.mapItemToUtility.get(item);
//////                System.out.println(itemToUtility);
////                if(itemToUtility==null){
////                    this.mapItemToUtility.put(item,utility);
////                }
////                else{
////                    this.mapItemToUtility.put(item,itemToUtility+utility);
////                }
////            }
////        }
////         Set<Integer> highUtility = new LinkedHashSet<Integer>();
//////        输出mapItemToUtility中的项
////        for(Map.Entry<Integer,Integer>entry:this.mapItemToUtility.entrySet()){
//////            System.out.println("输出mapItemToUtility中的项");
//////            System.out.println("Key:"+entry.getKey()+"  Value:"+entry.getValue());
////            if(entry.getValue()>this.max_utility){
////                highUtility.add(entry.getKey());
////            }
////        }
////
////        for (Map.Entry<Integer, Integer[]> entry : this.mapTidToUtilities.entrySet()) {
//////            Set<Integer> highUtility = new HashSet<>();
////            Integer key = entry.getKey();
////            Integer item = -1;
////            Integer[] utilities = entry.getValue();
////            Integer[] items = this.mapTidToItems.get(key);
//////            System.out.println("第" + entry.getKey() + "行：");
//////            for(int j=0;j< items.length;j++){
//////                System.out.print(items[j] + " ");
//////            }
////            Integer item_new_length = items.length;
////            Integer num_item_contains=0;
////            for (int i = 0; i < items.length; i++) {
////                item = Integer.valueOf(items[i]);
////                if(highUtility.contains(item)){
////                    num_item_contains++;
////                }
////            }
//////            System.out.println("输出num_item_contains:"+num_item_contains);
////            item_new_length=item_new_length-num_item_contains;
//////            System.out.println("输出item_new_length:"+item_new_length);
//////            System.out.println("输出highUtility:" + highUtility);
////            Integer[] utilities_new = new Integer[item_new_length];
////            Integer[] items_new = new Integer[item_new_length];
////                for (int i = 0, j = 0; i < items.length; i++) {
////                    item = Integer.valueOf(items[i]);
////                    if (!highUtility.contains(item)) {
////                        items_new[j] = items[i];
////                        utilities_new[j] = utilities[i];
////                        j++;
////                    }
////                }
////
//////                System.out.println("输出item_new_length:"+item_new_length);
////                if(item_new_length==0){
////                    continue;
////                }
////                this.mapTidToItems_preprocess.put(key, items_new);
////                this.mapTidToUtilities_preprocess.put(key, utilities_new);
//
////    }
//////  输出mapTidToItems的数据
////            for(Map.Entry<Integer, String[]>entry:this.mapTidToItems_preprocess.entrySet()){
//////                System.out.println(entry);
////                System.out.println("第"+entry.getKey()+"行：");
////                for(int i=0;i<entry.getValue().length;i++){
////                    System.out.print(entry.getValue()[i]+" ");
////                }
////                System.out.println(" ");
////            }
//////  输出mapTidToUtilities的数据
////            for(Map.Entry<Integer,String[]>entry:this.mapTidToUtilities_preprocess.entrySet()){
////                System.out.println("第"+entry.getKey()+"行：");
////                for(int i=0;i<entry.getValue().length;i++){
////                    System.out.print(entry.getValue()[i]+" ");
////                }
////                System.out.println(" ");
////            }
//    }
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

    //    public void coreAlg() {
//        System.out.println("当前运行到coreAlg");
//        Integer utilityOfSequence = 0;
//        List<Integer> sequence1 = new ArrayList<>();
////        sequence1.add(3);
////        sequence1.add(4);
////        System.out.println(countUtility(sequence1));
//        for (List<Integer> sequence : maxSequenceList) {
//            utilityOfSequence = countUtility(sequence);
//            if (utilityOfSequence <= max_utility) {
//                candidatesCount++;
//                patternCount++;
//                lowUtilityPattern.put(sequence, utilityOfSequence);
//            }
//            LUSM(sequence);
//        }
//        printLowUtilitySequence();
////        List<Integer> sequenceTest=new ArrayList<>();
////        sequenceTest.add(1);
////        sequenceTest.add(3);
////        sequenceTest.add(4);
////        List<List<Integer>> UtilitiesList=getUtilities(sequenceTest);
////        System.out.println("输出：");
////        System.out.println(UtilitiesList);
//    }
    public void processCoreAlgorithm() {
        System.out.println("当前运行到coreAlg");
//        List<Integer> sequenceTest = new ArrayList<>();
//        sequenceTest.add(195);
//        sequenceTest.add(385);
//        int count=countUtility(sequenceTest);
////        List<List<Integer>> UtilitiesList = getUtilities(sequenceTest);
//        System.out.println("输出：");
//        System.out.println(count);
        for (List<Integer> sequence : maxSequenceList) {
            int utilityOfSequence = countUtility(sequence);
//            if (utilityOfSequence <= max_utility && (lowUtilityPattern.get(sequence) == null))
            if (utilityOfSequence <= max_utility && hasProcessedSequenceList.contains(sequence) == false) {
                processLowUtilitySequence(sequence, utilityOfSequence);
            } else {
//                cut2(sequence);
                cut(sequence);
            }
        }
        printLowUtilitySequence();
    }

    private void processLowUtilitySequence(List<Integer> sequence, int utility) {
//        if (utility <= max_utility) {

        if (!hasProcessedSequenceList.contains(sequence)) {
            hasProcessedSequenceList.add(sequence);
            candidatesCount++;
            patternCount++;
            lowUtilityPattern.put(sequence, utility);
//            lowUtilityPattern.put(sequence, utility);
            LUSM(sequence);
        }
//        }
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
//        List<List<Integer>> sequenceTest1=new ArrayList<>();
//        List<Integer> sequenceTest2=new ArrayList<>();
//        sequenceTest2.add(1);
//        sequenceTest2.add(2);
//        sequenceTest2.add(3);
//        sequenceTest1.add(sequenceTest2);
//        System.out.println("测试：");
//        System.out.println(countUtility_FL(sequenceTest1));

//        List<Integer> sequenceTest1 = new ArrayList<>();
//        List<Integer> sequenceTest2 = new ArrayList<>();
//        sequenceTest2.add(1);
//        sequenceTest1.add(3);
//        System.out.println("输出3的效用："+countUtility(sequenceTest1));
//        System.out.println("输出1的效用："+countUtility(sequenceTest2));
        System.out.println("输出已经处理的序列列表");
        System.out.println(hasProcessedSequenceList);
//        List<Integer> sequenceTest=new ArrayList<>();
//        sequenceTest.add(1);
//        sequenceTest.add(3);
//        sequenceTest.add(4);
//        List<List<Integer>> UtilitiesList=getUtilities(sequenceTest);
//        System.out.println("输出：");
//        System.out.println(UtilitiesList);
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

    public void cut2(List<Integer> sequence) {
        if (sequence.size() > 1) {
//            List<List<Integer>> utilities = getUtilities(sequence);
            List<Integer> subFirstSequence = new ArrayList<>(sequence);
            List<Integer> subLastSequence = new ArrayList<>(sequence);
            subFirstSequence.remove(0); // 删除首项
            subLastSequence.remove(subLastSequence.size() - 1); // 删除尾项
            int firstUtility = countUtility(subFirstSequence);
            int lastUtility = countUtility(subLastSequence);
            if (firstUtility <= max_utility && hasProcessedSequenceList.contains(subFirstSequence) == false) {
                processLowUtilitySequence(subFirstSequence, firstUtility);
                hasProcessedSequenceList.add(subFirstSequence);
            } else {
                cut2(subFirstSequence);
            }
            if (lastUtility <= max_utility && hasProcessedSequenceList.contains(subLastSequence)==false) {
                processLowUtilitySequence(subLastSequence, lastUtility);
                hasProcessedSequenceList.add(subLastSequence);
            } else {
                cut2(subLastSequence);
            }
        } else {
//            System.out.println("处理一个项");
            if (hasProcessedSequenceList.contains(sequence) == false) {
                LUSMItem(sequence);
                hasProcessedSequenceList.add(sequence);
            }
        }
    }

    public void cut(List<Integer> sequence) {
//        List<Integer> sequenceTest = new ArrayList<>();
//        sequenceTest.add(195);
//        sequenceTest.add(385);
//        if(sequence.equals(sequenceTest)){
//            System.out.println("输出195 385");
//        }
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
//            cutFirst(sequence, utilities);
//            cutLast(sequence, utilities);
//            List<Integer> sequenceTest = new ArrayList<>();
//            sequenceTest.add(195);
//            sequenceTest.add(385);
//            if(sequence.equals(sequenceTest)){
//                System.out.println("输出195 385");
//            }
            cutSequence(sequence, utilities, true);  // 剪除首项
            cutSequence(sequence, utilities, false); // 剪除尾项
        } else {
//            System.out.println("处理一个项");
            processSingleElementSequence(sequence);

        }
    }

    private void processSingleElementSequence(List<Integer> sequence) {
        if (hasProcessedSequenceList.contains(sequence) == false) {
            LUSMItem(sequence);
            hasProcessedSequenceList.add(sequence);
        }
    }

    private void cutSequence(List<Integer> sequence, List<List<Integer>> utilities, boolean isFirst) {
        List<List<Integer>> newUtilities = new ArrayList<>();
        List<Integer> newSequence;
//        List<Integer> sequenceTest = new ArrayList<>();
//        sequenceTest.add(195);
//        sequenceTest.add(385);
//        if(sequence.equals(sequenceTest)){
//            System.out.println("输出195 385");
//        }
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
            processLowUtilitySequence(sequence, utility);
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
//        List<Integer> sequenceTest = new ArrayList<>();
//        sequenceTest.add(4);
//        sequenceTest.add(1);
//        sequenceTest.add(3);
//        if (sequence.equals(sequenceTest)) {
//            System.out.println("输出413");
//        }
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

    //    public int countUtility_FL(List<List<Integer>> utilities) {
//        int utility = 0;
//        List<Integer> utilityList;
//        for (int i = 0; i < utilities.size(); i++) {
//            utilityList = utilities.get(i);
//            for (int j = 0; j < utilityList.size(); j++) {
//                utility += utilityList.get(j);
//            }
//        }
//        return utility;
//    }
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
//        System.out.println("序列和它的效用");
//        System.out.println(sequence);
//        System.out.println(allUtility);
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
//        System.out.println(sequenceId);
        int i = -1;
        for (Integer key : sequenceId) {
            lastCount = count;
            count = key;
            if (index < key) {
                break;
            }
            i++;
        }
//        System.out.println(i);
        utilities = mapTidToUtilities.get(i);
        utility = utilities[index - lastCount];
//        for (Integer element : utilities) {
//            System.out.println(element);
//        }
//        System.out.println(utilities);
        return utility;
    }

    //        public boolean isContains(List<Integer> sequence) {
//        for (List<Integer> maxSequence : maxSequenceList) {
//            if (maxSequence.equals(sequence) || containsSublist(maxSequence, sequence)) {
//                return true;
//            }
//        }
//        return false;
//    }
    public boolean isContains(List<Integer> sequence) {
        // 优化第一个条件
        if (maxSequenceSet.contains(sequence)) {
            return true;
        }

        // 并行化检查 containsSublist
        return maxSequenceList.parallelStream().anyMatch(maxSequence -> containsSublist(maxSequence, sequence));
    }

    //    public boolean containsSublist(List<Integer> x1, List<Integer> x2) {
//        String x1Str = ListToString(x1);
//        String x2Str = ListToString(x2);
////        System.out.println("输出x1:" + x1Str);
////        System.out.println("输出x2:" + x2Str);
////        if (x1Str.contains(x2Str)) {
////            System.out.println(x1Str + "包含" + x2Str);
////        }
//        return x1Str.contains(x2Str);
//    }
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

    //    public void LUSM(List<Integer> sequence) {
//        List<Integer> subFirstSequence = new ArrayList<>(sequence);
//        List<Integer> subLastSequence = new ArrayList<>(sequence);
//        if (subFirstSequence.size() > 1 && hasProcessedSequenceList.contains(subFirstSequence.remove(0)) == false)
////            if (subFirstSequence.size()>1)
//        {
//            int utilityOfFirst = countUtility(subFirstSequence);
//            if (utilityOfFirst <= max_utility && lowUtilityPattern.get(subFirstSequence) == null) {
//                candidatesCount++;
//                patternCount++;
//                lowUtilityPattern.put(subFirstSequence, utilityOfFirst);
//            }
//            hasProcessedSequenceList.add(subFirstSequence);
//            LUSM(subFirstSequence);
//        }
//        if (subLastSequence.size() > 1 && hasProcessedSequenceList.contains(subLastSequence.remove(subLastSequence.size() - 1)) == false)
////            if(subLastSequence.size()>1)
//        {
////            subLastSequence.remove(subLastSequence.size()-1);
//            int utilityOfLast = countUtility(subLastSequence);
//            if (utilityOfLast <= max_utility && lowUtilityPattern.get(subLastSequence) == null) {
//                candidatesCount++;
//                patternCount++;
//                lowUtilityPattern.put(subLastSequence, utilityOfLast);
//            }
//            hasProcessedSequenceList.add(subLastSequence);
//            LUSM(subLastSequence);
//        }
//    }
//    public void LUSM(List<Integer> sequence) {
//        int utility=countUtility(sequence);
//        if(hasProcessedSequenceList.contains(sequence)==false){
//            hasProcessedSequenceList.add(sequence);
//            if(utility<=max_utility)
//            {candidatesCount++;
//                patternCount++;
//                lowUtilityPattern.put(sequence, utility);
//
//            }
//        }
//        if (sequence.size() == 1) {
//            if (hasProcessedSequenceList.contains(sequence) == false) {
//                LUSMItem(sequence);
//                hasProcessedSequenceList.add(sequence);
//            }
//        }
//        List<Integer> subFirstSequence = new ArrayList<>(sequence);
//        List<Integer> subLastSequence = new ArrayList<>(sequence);
//        if (subFirstSequence.size() > 1 && hasProcessedSequenceList.contains(subFirstSequence.remove(0)) == false)
////            if (subFirstSequence.size()>1)
//        {
//            int utilityOfFirst = countUtility(subFirstSequence);
//            if (utilityOfFirst <= max_utility && lowUtilityPattern.get(subFirstSequence) == null) {
//                candidatesCount++;
//                patternCount++;
//                lowUtilityPattern.put(subFirstSequence, utilityOfFirst);
//            }
//            hasProcessedSequenceList.add(subFirstSequence);
//            cut(subFirstSequence);
//        }
//        if (subLastSequence.size() > 1 && hasProcessedSequenceList.contains(subLastSequence.remove(subLastSequence.size() - 1)) == false)
////            if(subLastSequence.size()>1)
//        {
////            subLastSequence.remove(subLastSequence.size()-1);
//            int utilityOfLast = countUtility(subLastSequence);
//            if (utilityOfLast <= max_utility && lowUtilityPattern.get(subLastSequence) == null) {
//                candidatesCount++;
//                patternCount++;
//                lowUtilityPattern.put(subLastSequence, utilityOfLast);
//            }
//            hasProcessedSequenceList.add(subLastSequence);
//            cut(subLastSequence);
//        }
//    }
    public void LUSM(List<Integer> sequence) {
        if (!hasProcessedSequenceList.contains(sequence)) {
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

//    private void processSubSequence(@NotNull List<Integer> subSequence) {
//        if (subSequence.size() > 1 && !hasProcessedSequenceList.contains(subSequence)) {
//            int utility = countUtility(subSequence);
//            if (utility <= max_utility && lowUtilityPattern.get(subSequence) == null) {
//                processLowUtilitySequence(subSequence, utility);
//            }
//            hasProcessedSequenceList.add(subSequence);
//            cut(subSequence);
//        }
//        if (subSequence.size() == 1 && !hasProcessedSequenceList.contains(subSequence)) {
//            processSingleElementSequence(subSequence);
//        }
//    }

    private void processSubSequence(@NotNull List<Integer> subSequence) {
        if (subSequence.size() > 1 && !hasProcessedSequenceList.contains(subSequence)) {
            int utility = countUtility(subSequence);
//            hasProcessedSequenceList.add(subSequence);
            if (utility <= max_utility && lowUtilityPattern.get(subSequence) == null) {
                processLowUtilitySequence(subSequence, utility);
//                LUSM(subSequence);
            } else {
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
//                int[] itemsInt=new int[items.length];
//                for (int i = 0; i < items.length; i++) {
//                    itemsInt[i]=Integer.valueOf(items[i]);
//                }
                trans.add(tranOne);
            }
//            for (List<Integer> value : trans) {
//                System.out.println("value的值");
//                System.out.println(value);
//            }
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
//        System.out.println("----------------------");
//        for (List<Integer> value : trans) {
//                System.out.println("value的值");
//                System.out.println(value);
//            }
        //去重
        Set<String> stringSet = new LinkedHashSet<>();
        for (int i = 0; i < trans.size(); i++) {
            List<Integer> temp = trans.get(i);
            stringSet.add(Joiner.on("&").join(temp));
        }
//        System.out.println(stringSet);
        trans.clear();

        List<int[]> res = new ArrayList<>();
        List<String> stringList = new ArrayList<>(stringSet);

        while (!stringList.isEmpty()) {
            //lastSize=trans.size();
            for (int i = stringList.size() - 2; i >= 0; i--) {

                String[] items = stringList.get(stringList.size() - 1).split("&");
//                System.out.println("输出items:");
//                for(int j=0;j<items.length;j++){
//                    System.out.print(items[j]);
//                }
//                System.out.println("");
                List<Integer> itemsList1 = Arrays.stream(Arrays.stream(items).mapToInt(Integer::parseInt).toArray()).boxed().collect(Collectors.toList());
                String[] items2 = stringList.get(i).split("&");
                List<Integer> itemsList2 = Arrays.stream(Arrays.stream(items2).mapToInt(Integer::parseInt).toArray()).boxed().collect(Collectors.toList());
//                System.out.println("输出itemList1");
//                System.out.println(itemsList1);
//                System.out.println("输出itemList2");
//                System.out.println(itemsList2);
                if (itemsList1.containsAll(itemsList2)) {
                    stringList.remove(i);
                    //i++;
                }
            }
            res.add(Arrays.stream(stringList.get(stringList.size() - 1).split("&")).mapToInt(Integer::parseInt).toArray());
            stringList.remove(stringList.size() - 1);
        }
//        for(int i=0;i<res.size();i++){
//            System.out.println("输出res:");
//            for(int j=0;j<res.get(i).length;j++){
//                System.out.print(res.get(i)[j]);
//            }
//            System.out.println("");
//        }
        return res;
    }


}

