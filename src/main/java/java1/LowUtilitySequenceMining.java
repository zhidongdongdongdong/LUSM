package java1;

import com.google.common.base.Joiner;

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
    Generator3 generator;
    int count = 0;
    //    记录每行序列的起始项的位置
    Set<Integer> sequenceId = new TreeSet<>();
    int max_utility;
    BufferedWriter writer = null;
    //String exteFile="src/main/resources/extension.txt";
    long runtime;

    int patternCount;
    long candidatesCount;

    int maxLength;

    public void runAlgorithm(String input, int max_utility, int maxLength, String output) throws IOException {

        this.max_utility = max_utility;
//
        long startTime = System.currentTimeMillis();
        System.out.println("输出最大效用:" + max_utility);
        System.out.println("startTime:" + startTime);
//
        loadFile(input);
        preprocess(maxLength);
        coreAlg();
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

    public void loadFile(String path) throws IOException {
        Set<Integer> setOfItems = new TreeSet<>();
        Map<Integer, Integer[]> mapTidToItems = new LinkedHashMap<>();
        Map<Integer, Integer[]> mapTidToUtilities = new LinkedHashMap<>();
        int tidCount = 0;

        String thisLine;
        BufferedReader myInput = null;
        try {
            myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path))));
            // for each transaction (line) in the input file
            while ((thisLine = myInput.readLine()) != null) {
                // if the line is  a comment, is  empty or is a
                // kind of metadata
                if (thisLine.isEmpty() == true ||
                        thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%'
                        || thisLine.charAt(0) == '@') {
                    continue;
                }
                tidCount++;
                String[] partions = thisLine.split(":");
                // int transactionUtility = Integer.parseInt(partions[1]);
                String[] items = partions[0].split(" ");
                String[] utilities = partions[2].split(" ");
                Integer[] itemsInt = new Integer[items.length];
                Integer[] utilsInt = new Integer[utilities.length];
                for (int i = 0; i < items.length; i++) {
                    itemsInt[i] = Integer.valueOf(items[i]);
                    utilsInt[i] = Integer.valueOf(utilities[i]);
                    count++;
                    if (utilsInt[i] >= max_utility) {
                        itemsInt[i] = -1;
                        setOfItems.add(-1);
                        utilsInt[i] = -1;
                    } else {
                        setOfItems.add(itemsInt[i]);
                    }
                }
                mapTidToItems.put(tidCount, itemsInt);
                mapTidToUtilities.put(tidCount, utilsInt);
                sequenceId.add(count);
            }
            this.mapTidToItems = mapTidToItems;
            this.mapTidToUtilities = mapTidToUtilities;
//            输出mapTidToItems的数据
            for (Map.Entry<Integer, Integer[]> entry : mapTidToItems.entrySet()) {
//                System.out.println(entry);
                System.out.println("第" + entry.getKey() + "行：");
                for (int i = 0; i < entry.getValue().length; i++) {
                    System.out.print(entry.getValue()[i] + " ");
                }
                System.out.println(" ");
            }
//            输出mapTidToUtilities的数据
            for (Map.Entry<Integer, Integer[]> entry : mapTidToUtilities.entrySet()) {
                System.out.println("第" + entry.getKey() + "行：");
                for (int i = 0; i < entry.getValue().length; i++) {
                    System.out.print(entry.getValue()[i] + " ");
                }
                System.out.println(" ");
            }
//            System.out.println("sequenceId"+sequenceId);
//            System.out.println(mapTidToItems);
//            System.out.println(mapTidToUtilities);
        } catch (Exception e) {
            // catch exceptions
            e.printStackTrace();
        } finally {
            if (myInput != null) {
                // close the file
                myInput.close();
            }
        }

//        generator = new Generator3(setOfItems, tidCount);
//        generator.setGenerator(mapTidToItems, mapTidToUtilities);
    }

    /*preprocess()
     * 使用策略1，裁剪掉每个序列中，效用大于最低效用门槛的项，如果某个序列中所有的项都被裁剪，则裁剪掉整个序列。
     *
     * */
    public void preprocess(int maxLength) {
        System.out.println("当前运行到preprocess");
        Integer key;
        int countBit = 0;
        for (Map.Entry<Integer, Integer[]> entry : this.mapTidToItems.entrySet()) {
            Integer[] items = entry.getValue();
            for (int i = 0; i < items.length; i++) {
                key = items[i];
                BitSet bitSet = mapItemToBitSet.get(key);
                if (bitSet == null) {
                    BitSet bitSet1 = new BitSet();
                    mapItemToBitSet.put(key, bitSet1);
//                    System.out.println("输出"+key+"的bitSet为空");
                }
                bitSet = mapItemToBitSet.get(key);
                bitSet.set(countBit);
                countBit++;
            }
        }
        List<Integer> list;
        boolean key1=false;
        this.maxLength = maxLength;
        for (Map.Entry<Integer, Integer[]> entry : this.mapTidToItems.entrySet()) {
            Integer[] items = entry.getValue();
            for (int i = 0; i < items.length; i++) {
//                if (key1==true){
//                    key=false;
//                    break;
//                }
                list = new ArrayList<>();
                if (items[i] == -1) {
                    continue;
                }
                for (int j = i; j < items.length && ((j - i + 1) <= maxLength); j++) {
                    if(items[j] == -1) {
                        key1=true;
                        break;
                    }
                    list.add(items[j]);
                    if (j == items.length - 1) {
                        key1=true;
                    }
                }
//                System.out.println(list);
                if(!isContains(list)){
                    maxSequenceList.add(list);
                }
//                System.out.println("输出maxSequenceList"+maxSequenceList);
            }
        }
        System.out.println(maxSequenceList);

//        for(Map.Entry<Integer,BitSet> entry:this.mapItemToBitSet.entrySet()){
//            System.out.println("输出entry:"+entry);
//            System.out.println(entry.getKey());
//            System.out.println(entry.getValue());
//            BitSet bitSets=mapItemToBitSet.get(entry);
//            System.out.println("输出项的位图：");
//            System.out.println(bitSets.size());
//        }
//            Integer[] utilities = entry.getValue();
//            Integer[] items = this.mapTidToItems.get(key);
//            for(int i=0;i<utilities.length;i++)
//            {
//                Integer utility = Integer.valueOf(utilities[i]);
//                Integer item=Integer.valueOf(items[i]);
//                Integer itemToUtility=this.mapItemToUtility.get(item);
////                System.out.println(itemToUtility);
//                if(itemToUtility==null){
//                    this.mapItemToUtility.put(item,utility);
//                }
//                else{
//                    this.mapItemToUtility.put(item,itemToUtility+utility);
//                }
//            }
//        }
//         Set<Integer> highUtility = new LinkedHashSet<Integer>();
////        输出mapItemToUtility中的项
//        for(Map.Entry<Integer,Integer>entry:this.mapItemToUtility.entrySet()){
////            System.out.println("输出mapItemToUtility中的项");
////            System.out.println("Key:"+entry.getKey()+"  Value:"+entry.getValue());
//            if(entry.getValue()>this.max_utility){
//                highUtility.add(entry.getKey());
//            }
//        }
//
//        for (Map.Entry<Integer, Integer[]> entry : this.mapTidToUtilities.entrySet()) {
////            Set<Integer> highUtility = new HashSet<>();
//            Integer key = entry.getKey();
//            Integer item = -1;
//            Integer[] utilities = entry.getValue();
//            Integer[] items = this.mapTidToItems.get(key);
////            System.out.println("第" + entry.getKey() + "行：");
////            for(int j=0;j< items.length;j++){
////                System.out.print(items[j] + " ");
////            }
//            Integer item_new_length = items.length;
//            Integer num_item_contains=0;
//            for (int i = 0; i < items.length; i++) {
//                item = Integer.valueOf(items[i]);
//                if(highUtility.contains(item)){
//                    num_item_contains++;
//                }
//            }
////            System.out.println("输出num_item_contains:"+num_item_contains);
//            item_new_length=item_new_length-num_item_contains;
////            System.out.println("输出item_new_length:"+item_new_length);
////            System.out.println("输出highUtility:" + highUtility);
//            Integer[] utilities_new = new Integer[item_new_length];
//            Integer[] items_new = new Integer[item_new_length];
//                for (int i = 0, j = 0; i < items.length; i++) {
//                    item = Integer.valueOf(items[i]);
//                    if (!highUtility.contains(item)) {
//                        items_new[j] = items[i];
//                        utilities_new[j] = utilities[i];
//                        j++;
//                    }
//                }
//
////                System.out.println("输出item_new_length:"+item_new_length);
//                if(item_new_length==0){
//                    continue;
//                }
//                this.mapTidToItems_preprocess.put(key, items_new);
//                this.mapTidToUtilities_preprocess.put(key, utilities_new);

//    }
////  输出mapTidToItems的数据
//            for(Map.Entry<Integer, String[]>entry:this.mapTidToItems_preprocess.entrySet()){
////                System.out.println(entry);
//                System.out.println("第"+entry.getKey()+"行：");
//                for(int i=0;i<entry.getValue().length;i++){
//                    System.out.print(entry.getValue()[i]+" ");
//                }
//                System.out.println(" ");
//            }
////  输出mapTidToUtilities的数据
//            for(Map.Entry<Integer,String[]>entry:this.mapTidToUtilities_preprocess.entrySet()){
//                System.out.println("第"+entry.getKey()+"行：");
//                for(int i=0;i<entry.getValue().length;i++){
//                    System.out.print(entry.getValue()[i]+" ");
//                }
//                System.out.println(" ");
//            }
    }

    public void coreAlg() {
        System.out.println("当前运行到coreAlg");
        for (List<Integer> sequence : maxSequenceList) {


        }

    }
    public boolean isContains(List<Integer> sequence){
        for(List<Integer> maxSequence:maxSequenceList){
            if(maxSequence.containsAll(sequence)){
                return true;
            }
        }
        return false;
    }

    public void printStats(List<Double> runTimelist, List<Double> memorylist, List<Long> candidateslist, List<Integer> patternlist) {

//        runTimelist.add((double) runtime / 1000);
//        memorylist.add(MemoryLogger.getInstance().getMaxMemory());
//        candidateslist.add(candidatesCount);
//        patternlist.add(patternCount);

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
