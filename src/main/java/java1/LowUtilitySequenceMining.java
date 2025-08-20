package java1;

import com.google.common.base.Joiner;
import java.util.Arrays;
import java.util.List;
import java.sql.SQLOutput;
import java.util.*;
import java.util.stream.Collectors;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class LowUtilitySequenceMining {
    int countOfCountUtilities=0;
    Set<int[]> preSet = new LinkedHashSet<>();
    // 1. 在 loadFile 之后调用一次即可
    private int[] flatItems;   // 所有事务的 item 扁平化
    private int[] flatUtils;   // 所有事务的 utility 扁平化
    private int[] txnStarts;   // 每个事务在扁平数组中的起始下标
    /**
     * 轻量级不可变序列 key，避免 List<Integer> 的哈希计算和装箱开销
     */
    final class SeqKey {
        private final int[] data;
        private final int hash;
        SeqKey(List<Integer> list) {
            data = new int[list.size()];
            for (int i = 0; i < data.length; i++) {
                data[i] = list.get(i);
            }
            hash = Arrays.hashCode(data);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof SeqKey)) return false;
            SeqKey other = (SeqKey) obj;
            return Arrays.equals(data, other.data);
        }

        /* 可选：用于调试 */
        @Override
        public String toString() {
            return Arrays.toString(data);
        }
    }
    Map<Integer, Integer[]> mapTidToItems = new LinkedHashMap<>();
    Map<Integer, Integer[]> mapTidToItems_deleted = new LinkedHashMap<>();
    Map<Integer, Integer[]> mapTidToUtilities = new LinkedHashMap<>();
    Map<Integer, BitSet> mapItemToBitSet = new LinkedHashMap<>();
    List<List<Integer>> maxSequenceList = new ArrayList<>();
    List<List<Integer>> maxSequenceList2 = new ArrayList<>();
    Set<List<Integer>> maxSequenceSet = new HashSet<>();
    Map<List<Integer>,Integer> lowestOfSeq =new LinkedHashMap<>();
    Set<List<Integer>> hasProcessedMaxSequenceSet = new LinkedHashSet<>();
    Set<List<Integer>> hasProcessedSequenceList = new LinkedHashSet<>();
    Map<List<Integer>, Integer> hasProcessedSequenceListAndId = new LinkedHashMap<>();
    Set<List<Integer>> hasDeleteSequenceList = new LinkedHashSet<>();
    Map<List<Integer>, Integer> lowUtilityPattern = new LinkedHashMap<>();
    int hadProcessedMaxSequence=0;

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
    public static List<Double> runTime = new ArrayList<>();
    public static List<Double> memory = new ArrayList<>();
    public static List<Long> candidates = new ArrayList<>();
    public static List<Integer> pattern = new ArrayList<>();
    long startTime;
    int patternCount;
    long candidatesCount;
//    private int[] prefixSum; // prefixSum[i] = sum(flatUtils[0..i-1])
    int maxLength;

    public void runAlgorithm(String input, int max_utility, int maxLength, BufferedWriter writer) throws IOException {
        MemoryLogger.getInstance().checkMemory();
        System.out.println("此处是低效用非连续序列挖掘算法");
        this.writer=writer;
        this.max_utility = max_utility;
        this.maxLength = maxLength;
        startTime = System.currentTimeMillis();
        StringBuilder buffer = new StringBuilder();
        buffer.append("max_utility:").append(max_utility).append("\n");
        buffer.append("maxLength:").append(maxLength).append("\n");
        writer.write(buffer.toString());
        writer.newLine();
        System.out.println("输出最大效用:" + max_utility);
        System.out.println("startTime:" + startTime);
        loadFile_sequence(input);
        buildIndex();
//        List<Integer> sequence1=new ArrayList<>();
//        List<Integer> sequence2=new ArrayList<>();
//        List<Integer> sequence3=new ArrayList<>();
//        List<Integer> sequence4=new ArrayList<>();
//        sequence1.add(2410);
//        sequence1.add(524);
//        sequence1.add(8533);
//        sequence2.add(3641);
//        sequence2.add(271);
//        sequence3.add(2410);
//        sequence3.add(397);
//        sequence3.add(5295);
//        sequence4.add(5876);
//        System.out.println(sequence1+"的效用是:"+countUtility(sequence1));
        System.out.println("输出count:" + sequenceId);
        runtime2 = System.currentTimeMillis() - startTime;
        createBitmap();
        getMaxSequence(maxLength);
        runtime3 = System.currentTimeMillis() - startTime;
        processCoreAlgorithm();
//        processCoreAlgorithm_improved();
        runtime = System.currentTimeMillis() - startTime;
        printLowUtilitySequence();
        showStates();
        MemoryLogger.getInstance().checkMemory();
        printStats(runTime, memory, candidates, pattern);
//        System.out.println(sequence1+"的效用是:"+countUtility(sequence1));
//        System.out.println(sequence2+"的效用是:"+countUtility(sequence2));
//        System.out.println(sequence3+"的效用是:"+countUtility(sequence3));
//        System.out.println(sequence4+"的效用是:"+countUtility(sequence4));


    }
    public void loadFile_sequence(String path) throws IOException {
        final int[] tidCount = {0};
//        final int[] item1 = {0};
        final int[] indexOfItem = {0};
//        Set<Integer> setOfItems = new TreeSet<>();
        Map<Integer, Integer[]> mapTidToItems = new LinkedHashMap<>();
        Map<Integer, Integer[]> mapTidToItems_delete = new LinkedHashMap<>();
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
//                        int lowerUtility=Integer.MAX_VALUE;
                        indexOfItem[0] = 0;
                        boolean key = false;
                        String[] itemUtilityPairs = partitions[0].trim().split(" ");  // 按空格分割项效用对
                        List<Integer> itemsIntList = new ArrayList<>();
                        List<Integer> itemsIntList_actual = new ArrayList<>();
                        List<Integer> utilsIntList = new ArrayList<>();
//                        boolean containsMaxUtility = false;
                        for (String pair : itemUtilityPairs) {
                            if (pair.isEmpty()) continue;

                            // 解析 "item[utility]" 格式
                            String[] itemUtility = pair.split("\\[|\\]"); // 按 "[" 和 "]" 分割
                            if (itemUtility.length != 2) continue;

                            try {
                                int item = Integer.parseInt(itemUtility[0].trim());
                                int utility = Integer.parseInt(itemUtility[1].trim());
                                /*
                                * 裁剪策略：存储效用小于最低效用的项；
                                *
                                * */
//                                if(this.maxLength==Integer.MAX_VALUE){
                                    if(utility<=this.max_utility){
                                        itemsIntList_actual.add(item);
                                    }
//                                }

                                indexOfItem[0]++;
//                                setOfItems.add(item);
                                itemsIntList.add(item);
                                utilsIntList.add(utility);
//                                if(utility<lowerUtility){lowerUtility=utility;}

                            } catch (NumberFormatException e) {
                                // 处理解析错误的情况
                                System.out.println("解析项或效用错误: " + pair);
                            }
                        }
                        count += itemsIntList.size();
                        mapTidToItems_delete.put(tidCount[0], itemsIntList_actual.toArray(new Integer[0]));
                        mapTidToItems.put(tidCount[0], itemsIntList.toArray(new Integer[0]));
                        mapTidToUtilities.put(tidCount[0], utilsIntList.toArray(new Integer[0]));
                        sequenceId.add(count);
                        tidCount[0]++;
//                        this.lowestOfSeq.put(itemsIntList,lowerUtility);
                    });
            this.mapTidToItems_deleted=mapTidToItems_delete;
            this.mapTidToItems = mapTidToItems;
            this.mapTidToUtilities = mapTidToUtilities;
            printMapData("mapTidToItems", mapTidToItems);
            printMapData("mapTidToUtilities", mapTidToUtilities);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void loadFile_item_Deleted(String path) throws IOException {
        final int[] tidCount = {0};
        final int[] item1 = {0};
        final int[] indexOfItem = {0};
        Set<Integer> setOfItems = new TreeSet<>();
        Map<Integer, Integer[]> mapTidToItems = new LinkedHashMap<>();
        Map<Integer, Integer[]> mapTidToItems_delete = new LinkedHashMap<>();
        Map<Integer, Integer[]> mapTidToUtilities = new LinkedHashMap<>();
        sequenceId.add(0);
//        List<Integer> tidCountList = new ArrayList<>();
        List<Integer> itemList = new ArrayList<>();
//        List<Integer> itemsIntList_actual = new ArrayList<>();
        List<Integer> indexOfItemList = new ArrayList<>();
        try (BufferedReader myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path))))) {
            myInput.lines()
                    .filter(line -> !line.isEmpty() && line.charAt(0) != '#' && line.charAt(0) != '%' && line.charAt(0) != '@')
                    .forEach(thisLine -> {
                        int lowerUtility=Integer.MAX_VALUE;
                        String[] partitions = thisLine.split(":"); // 按 "SUtility:" 分割数据
                        if (partitions.length < 2) return;  // 如果没有有效的分割部分则跳过
                        indexOfItem[0] = 0;
                        boolean key = false;
//                        String[] itemPairs = partitions[0].trim().split(" ");  // 按空格分割项效用对
//                        String[] utilityPairs = partitions[2].trim().split(" ");  // 按空格分割项效用对
                        String[] itemPairs = partitions[0].trim().split(" ");  // 按空格分割项效用对
                        String[] utilityPairs = partitions[2].trim().split(" ");  // 按空格分割项效用对
                        List<Integer> itemsIntList = new ArrayList<>();
                        List<Integer> itemsIntList_actual = new ArrayList<>();
                        List<Integer> utilsIntList = new ArrayList<>();
                        boolean containsMaxUtility = false;
                        for (int i=0;i<itemPairs.length;i++) {
//                            if (item.isEmpty()) continue;

                            // 解析 "item[utility]" 格式
//                            String[] itemUtility = pair.split("\\[|\\]"); // 按 "[" 和 "]" 分割
//                            if (itemUtility.length != 2) continue;

                            try {
                                int item = Integer.parseInt(itemPairs[i]);
                                int utility = Integer.parseInt(utilityPairs[i]);
                                if(utility<lowerUtility){lowerUtility=utility;}
                                if(this.maxLength==Integer.MAX_VALUE){
                                    if(utility<=this.max_utility){
                                        itemsIntList_actual.add(item);
                                    }
                                }
                                indexOfItem[0]++;
                                setOfItems.add(item);
                                itemsIntList.add(item);
                                utilsIntList.add(utility);
                            } catch (NumberFormatException e) {
                                // 处理解析错误的情况
//                                System.out.println("解析项或效用错误: " + pair);
                            }
                        }

                        count += itemsIntList.size();
                        mapTidToItems_delete.put(tidCount[0], itemsIntList_actual.toArray(new Integer[0]));
                        mapTidToItems.put(tidCount[0], itemsIntList.toArray(new Integer[0]));
                        mapTidToUtilities.put(tidCount[0], utilsIntList.toArray(new Integer[0]));
                        sequenceId.add(count);
                        tidCount[0]++;
                        this.lowestOfSeq.put(itemsIntList,lowerUtility);
                    });
            this.mapTidToItems_deleted=mapTidToItems_delete;
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
public static List<List<Integer>> deduplicateUtilities(List<List<Integer>> utilities) {
    return new ArrayList<>(new LinkedHashSet<>(utilities));
}


    public void createBitmap() throws IOException {
        int countBit = 0;
            for (Map.Entry<Integer, Integer[]> entry : mapTidToItems.entrySet()) {
                Integer[] items = entry.getValue();
                for (Integer key : items) {
                    mapItemToBitSet.computeIfAbsent(key, k -> new BitSet()).set(countBit);
                    countBit++;
                }
            }
        // 创建每一项的位图
    }
    public void getMaxSequence(int maxLength) throws IOException {
        StringBuilder buffer = new StringBuilder();
        buffer.append("当前运行到 preprocess\n");
        System.out.println("当前运行到 preprocess");
        System.out.println("2222");
        runtime4 = System.currentTimeMillis() - startTime;

        // **优化序列处理**
        List<Integer> listItems = new ArrayList<>();  // 复用 List，减少创建对象
//        if(this.maxLength==Integer.MAX_VALUE){
            for (Map.Entry<Integer, Integer[]> entry : mapTidToItems_deleted.entrySet()) {
                Integer[] items = entry.getValue();
                int length = items.length;
                    int i=0;
                    listItems.clear();  // 复用 List
                    for (int j = i; j < length ; j++) {
                        if (items[j] == -1) {
                            break;
                        }
                        listItems.add(items[j]);
                    }
                    if (!listItems.isEmpty() && !isContains(listItems,maxSequenceList2)) {
                        addSequence(new ArrayList<>(listItems));  // 传递副本，确保数据一致
                    }

            }
//        }else{
//            for (Map.Entry<Integer, Integer[]> entry : mapTidToItems.entrySet()) {
//                Integer[] items = entry.getValue();
//                int length = items.length;
//                for (int i = 0; i < length; i++) {
//                    if (items[i] == -1) {
//                        continue; // 跳过分隔符
//                    }
//                    listItems.clear();  // 复用 List
//                    for (int j = i; j < length && (j - i + 1) <= maxLength; j++) {
//                        if (items[j] == -1) {
//                            break;
//                        }
//                        listItems.add(items[j]);
//                    }
//                    if (!listItems.isEmpty() && !isContains(listItems,maxSequenceList2)) {
//                        addSequence(new ArrayList<>(listItems));  // 传递副本，确保数据一致
//                    }
//                }
//            }
//        }
        for(List<Integer> sequence:maxSequenceList2){
            System.out.println("正在处理序列:"+sequence);
            if(sequence.size()==0){
                continue;
            }
            List<List<Integer>> utilities=getUtilities(sequence);
            if(utilities.size()>1){
                processed(sequence,utilities);
            }
            else{
                if(!isContains(sequence,maxSequenceList)) maxSequenceList.add(sequence);
            }
//            processed(sequence,utilities);
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
    public void addSequence(List<Integer> sequence) {
        maxSequenceList2.add(sequence);
//        maxSequenceSet.add(sequence);
    }
    public void processCoreAlgorithm_improved() throws IOException {
        System.out.println("当前运行到LUSM+");
        int size = maxSequenceList.size();
        int utilityOfSequence = -1;
        int num_now = 0;
        maxSequenceList.sort((a, b) -> b.size() - a.size());
        for (List<Integer> sequence : maxSequenceList) {
            if(sequence.size()==0){
                continue;
            }
            StringBuilder buffer = new StringBuilder();
            buffer.append(num_now + "/" + size);
            System.out.println(num_now + "/" + size);
            writer.write(buffer.toString());
            writer.newLine();
            System.out.println(num_now + "/" + size);
            num_now++;
            List<List<Integer>> utilities = new ArrayList<>();
            utilities = getUtilities(sequence);
            if(sequence.size()==0){
                continue;
            }
            utilityOfSequence = countUtility_FL(utilities);
            if (utilityOfSequence == -1) System.out.println("此序列为空！");
//            List<List<Integer>> utilitiesNew2=deduplicateUtilities(utilities);
            List<Integer> sequenceNew=new ArrayList<>();
            LUM1(sequence, utilities,sequenceNew);
//            hasProcessedMaxSequenceSet.add(sequence);
//            hadProcessedMaxSequence.add();
        }
    }
//    public void processed(List<Integer> sequence, List<List<Integer>> utilities) throws IOException {
//        /*
//        * 裁剪策略：删除掉序列项中大于最低效用的项
//        *
//        * */
//        for (int i = 0; i < sequence.size(); i++) {
//            List<Integer> utility_address=new ArrayList<>();
//            int utilityOfItem = 0;
//            for (List<Integer> utility : utilities) {
//                if(!utility_address.contains(utility.get(i))){
//                    utilityOfItem += utility.get(i);
//                    utility_address.add(utility.get(i));
//                }
//            }
//            if (utilityOfItem > this.max_utility) {
//                sequence.remove(i);
//                for (List<Integer> utility : utilities) {
//                    utility.remove(i);
//                }
//                i--;
//            }
//        }
//        if(!isContains(sequence,maxSequenceList)) maxSequenceList.add(sequence);
//    }
public void processed(List<Integer> sequence,
                      List<List<Integer>> utilities) throws IOException {

    for (int i = 0; i < sequence.size(); i++) {
        Set<Integer> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        int utilityOfItem = 0;

        for (List<Integer> utility : utilities) {
            Integer val = utility.get(i);   // 拿到对象引用
            if (seen.add(val)) {            // 同一个引用只统计一次
                utilityOfItem += val;
            }
        }

        if (utilityOfItem > this.max_utility) {
            sequence.remove(i);
            for (List<Integer> utility : utilities) {
                utility.remove(i);
            }
            i--;
        }
    }

    if (!isContains(sequence, maxSequenceList)) {
        maxSequenceList.add(sequence);
    }
}


    public int getLowestOfUtilities(List<List<Integer>> utilities) {
        return utilities.stream()
                .flatMapToInt(list -> list.stream().mapToInt(Integer::intValue))
                .min()
                .orElse(Integer.MAX_VALUE);
    }
//    public List<List<Integer>> getUtilities(List<Integer> seq) {
//        countOfCountUtilities++;
//        final int k = seq.size();
//        if (k == 0) return Collections.emptyList();
//
//        /* 1. 预取 bitmap 及 long[] */
//        BitSet[] bs = new BitSet[k];
//        long[][] ws = new long[k][];
//        int[] lens = new int[k];
//        for (int i = 0; i < k; i++) {
//            bs[i] = mapItemToBitSet.get(seq.get(i));
//            if (bs[i] == null || bs[i].isEmpty()) return Collections.emptyList();
//            ws[i] = bs[i].toLongArray();
//            lens[i] = bs[i].length();
//        }
//
//        /* 2. 把 sequenceId 变成升序数组，用于区间划分 */
//        int[] sid = sequenceId.stream().mapToInt(Integer::intValue).toArray();
//
//        /* 3. 枚举起点 */
//        List<List<Integer>> res = new ArrayList<>();
//        long[] w0 = ws[0];
//        for (int wi = 0; wi < w0.length; wi++) {
//            for (long word = w0[wi]; word != 0; ) {
//                int tz = Long.numberOfTrailingZeros(word);
//                int idx0 = (wi << 6) + tz;
//                word &= word - 1;               // 快速抹最低 1
//                int up = upperBound(sid, idx0); // 事务右边界
//                if (up == -1) continue;
//
//                /* 4. 顺序匹配后续 k-1 项 */
//                int[] pos = new int[k];
//                pos[0] = idx0;
//                boolean ok = true;
//                for (int i = 1; i < k; i++) {
//                    int nxt = nextSetBit(ws[i], idx0 + 1, up);
//                    if (nxt == -1) { ok = false; break; }
//                    pos[i] = nxt;
//                    idx0 = nxt;
//                }
//                if(maxLength!=Integer.MAX_VALUE) {
//                    if (ok) {
//                        List<Integer> line = new ArrayList<>(k);
//                        for (int p : pos) line.add(getUtility(p));
//                        res.add(line);
//                    }
//                }else{
//                    if (ok) {
//                        int span = pos[k - 1] - pos[0]+1;  // 位置差
//                        if (span >= maxLength) {            // 超过最大允许跨度
//                            continue;                    // 跳过，不收集
//                        }
//
//                        // 跨度满足条件，才收集效用
//                        List<Integer> line = new ArrayList<>(k);
//                        for (int p : pos) {
//                            line.add(getUtility(p));
//                        }
//                        res.add(line);
//                    }
//                }
//            }
//        }
//        return res;
//    }
public List<List<Integer>> getUtilities(List<Integer> seq) {
    countOfCountUtilities++;
    final int k = seq.size();
    if (k == 0) return Collections.emptyList();

    /* 1. 预取 bitmap 及 long[] */
    BitSet[] bs = new BitSet[k];
    long[][] ws = new long[k][];
    for (int i = 0; i < k; i++) {
        bs[i] = mapItemToBitSet.get(seq.get(i));
        if (bs[i] == null || bs[i].isEmpty()) return Collections.emptyList();
        ws[i] = bs[i].toLongArray();
    }

    /* 2. 把 sequenceId 变成升序数组 */
    int[] sid = sequenceId.stream().mapToInt(Integer::intValue).toArray();

    /* 3. 枚举起点 */
    List<List<Integer>> res = new ArrayList<>();
    long[] w0 = ws[0];

    for (int wi = 0; wi < w0.length; wi++) {
        for (long word = w0[wi]; word != 0; ) {
            int tz = Long.numberOfTrailingZeros(word);
            int firstPos = (wi << 6) + tz;
            word &= word - 1;

            int up = upperBound(sid, firstPos);
            if (up == -1) continue;

            /* 4. 顺序匹配后续 k-1 项 */
            int[] pos = new int[k];
            pos[0] = firstPos;
            int cur = firstPos;
            boolean ok = true;

            // 判断是否需要做跨度剪枝
//            boolean hasMaxLengthLimit = (maxLength != Integer.MAX_VALUE);

            for (int i = 1; i < k; i++) {
//                if (hasMaxLengthLimit) {
//                    // ✅ 提前剪枝：计算第 i 项最晚允许位置
//                    int remainingItems = k - i;  // 后面还要匹配几个项
//                    int latestAllowed = firstPos + maxLength - remainingItems;
//
//                    // 如果当前已超限，或搜索起点已越界
//                    if (cur >= latestAllowed) {
//                        ok = false;
//                        break;
//                    }
//
//                    // 缩小搜索范围：[cur+1, min(up, latestAllowed + 1))
//                    int searchEnd = Math.min(up, latestAllowed + 1);
//                    int nxt = nextSetBit(ws[i], cur + 1, searchEnd);
//                    if (nxt == -1) {
//                        ok = false;
//                        break;
//                    }
//                    pos[i] = nxt;
//                    cur = nxt;
//                } else {
                    // 无限制：正常匹配
                    int nxt = nextSetBit(ws[i], cur + 1, up);
                    if (nxt == -1) {
                        ok = false;
                        break;
                    }
                    pos[i] = nxt;
                    cur = nxt;
//                }
            }

            // ✅ 匹配成功后：检查最终跨度（仅当有限制时）
//            if (ok) {
//                int span = cur - firstPos + 1; // +1 表示包含首尾的长度
//                if (span > maxLength) {        // 超过最大长度
//                    continue; // 跳过
//                }
//            }

            // ✅ 收集效用（只要 ok 且通过跨度检查）
            if (ok) {
                List<Integer> line = new ArrayList<>(k);
                for (int p : pos) {
                    line.add(getUtility(p));
                }
                res.add(line);
            }
        }
    }
    return res;
}
    /* ---------- 4. 一次处理 long 的 nextSetBit ---------- */
    private static int nextSetBit(long[] words, int from, int toExclusive) {
        if (from >= toExclusive) return -1;
        int wIdx = from >>> 6;
        if (wIdx >= words.length) return -1;
        long word = words[wIdx] & (~0L << (from & 63));
        while (true) {
            if (word != 0) {
                int tz = Long.numberOfTrailingZeros(word);
                int idx = (wIdx << 6) + tz;
                return idx < toExclusive ? idx : -1;
            }
            if (++wIdx >= words.length || (wIdx << 6) >= toExclusive) return -1;
            word = words[wIdx];
        }
    }



//    /**
//     * 返回 seqIds 中第一个严格大于 val 的值，
//     * 若不存在返回 -1。
//     */
    private int upperBound(int[] seqIds, int val) {
        for (int v : seqIds) {
            if (v > val) return v;
        }
        return -1;
    }
    // 使用二分查找快速定位大于 index 的最小 sequenceId
    private int findUpIndex(int[] seqIds, int index) {
        int left = 0, right = seqIds.length;
        while (left < right) {
            int mid = (left + right) >>> 1;
            if (seqIds[mid] > index) {
                right = mid;
            } else {
                left = mid + 1;
            }
        }
        return (left < seqIds.length) ? seqIds[left] : -1;
    }
//    public int countUtility(List<Integer> sequence) {
//
//        if (sequence == null || sequence.isEmpty()) return -1;
//        countOfCountUtilities++;
//        final int n = sequence.size();
//        // 1. 把 sequenceId 变成有序数组，便于二分
//        final int[] sid = sequenceId.stream().mapToInt(Integer::intValue).toArray();
//        // 2. 预取所有 BitSet，避免 map 反复查找
//        final BitSet[] bss = new BitSet[n];
//        for (int i = 0; i < n; i++) {
//            bss[i] = mapItemToBitSet.get(sequence.get(i));
//            if (bss[i] == null) return 0;          // 只要有一个 item 不存在，直接返回 0
//        }
//
//        int total = 0;
//
//        // 3. 遍历第一个 item 的所有出现位置
//        for (int idx = bss[0].nextSetBit(0); idx >= 0; idx = bss[0].nextSetBit(idx + 1)) {
//
//            // 3.1 用二分快速找到当前 idx 对应的 upindex
//            int up = upperBound(sid, idx);
//            if (up == -1) break;                   // 后面没有更大的 sequenceId 了
//
//            int cur = idx;
//            int firstPos = idx;
//            int util = getUtility(cur);            // 首个 item 的 utility
//            boolean ok = true;
//
//            // 3.2 匹配后续 item
//            for (int i = 1; i < n; i++) {
//                // 从 cur+1 开始找下一个 1
//                int nxt = bss[i].nextSetBit(cur + 1);
//                if (nxt < 0 || nxt >= up) {        // 没找到或在区间外
//                    ok = false;
//                    break;
//                }
//                util += getUtility(nxt);
//                cur = nxt;
//            }
//            if (ok) {
//                           int lastPos = cur;                 // 匹配完成后，cur 就是最后一个项的位置
//                          int span = lastPos - firstPos;     // 计算跨度
//                           if (span < maxLength) {               // 跨度小于 maxGap 才计入
//                    total += util;
//                               }
//            }
//        }
//        return total;
//    }
public int countUtility(List<Integer> sequence) {
    if (sequence == null || sequence.isEmpty()) return -1;
    countOfCountUtilities++;
    final int n = sequence.size();

    // 1. 把 sequenceId 变成有序数组
    final int[] sid = sequenceId.stream().mapToInt(Integer::intValue).toArray();

    // 2. 预取 BitSet
    final BitSet[] bss = new BitSet[n];
    for (int i = 0; i < n; i++) {
        bss[i] = mapItemToBitSet.get(sequence.get(i));
        if (bss[i] == null) return 0; // 不存在则无匹配
    }

    int total = 0;
//    boolean hasLengthLimit = (maxLength != Integer.MAX_VALUE); // ✅ 缓存判断，避免重复比较

    // 3. 遍历第一个 item 的所有出现位置
    for (int firstPos = bss[0].nextSetBit(0); firstPos >= 0;
         firstPos = bss[0].nextSetBit(firstPos + 1)) {

        int up = upperBound(sid, firstPos);
        if (up == -1) break;

        int cur = firstPos;
        int util = getUtility(cur);
        boolean ok = true;

        // 3.2 匹配后续项（带跨度剪枝）
        for (int i = 1; i < n; i++) {
//            if (hasLengthLimit) {
//                int remaining = n - i;                    // 后面还需匹配几项
//                int latestAllowed = firstPos + maxLength - remaining;
//
//                if (cur >= latestAllowed) {  // 当前位置已导致无法完成
//                    ok = false;
//                    break;
//                }
//
//                int searchEnd = Math.min(up, latestAllowed + 1);
//                int nxt = findNextSetBitInRange(bss[i], cur + 1, searchEnd);
//                if (nxt == -1) {
//                    ok = false;
//                    break;
//                }
//                util += getUtility(nxt);
//                cur = nxt;
//            } else {
                // 无限制：正常匹配
                int nxt = bss[i].nextSetBit(cur + 1);
                if (nxt < 0 || nxt >= up) {
                    ok = false;
                    break;
                }
                util += getUtility(nxt);
                cur = nxt;
            }
//        }

        // ✅ 匹配成功后：最终跨度检查（仅当有限制时）
//        if (ok && hasLengthLimit) {
//            int span = cur - firstPos + 1; // +1 表示窗口长度（含首尾）
//            if (span > maxLength) {        // 超出最大长度
//                continue;
//            }
//        }

        // ✅ 累加效用
        if (ok) {
            total += util;
        }
    }
    return total;
}
    private int findNextSetBitInRange(BitSet bs, int from, int toExclusive) {
        if (from >= toExclusive) return -1;
        int next = bs.nextSetBit(from);
        return (next >= 0 && next < toExclusive) ? next : -1;
    }
    /**

    /**
     * 把 mapTidToItems 与 mapTidToUtilities 转换成事务块列表，供后续 getUtilitiesFast 使用
     */
//    public static List<TxBlock> buildTxBlocks(Map<Integer, Integer[]> mapTidToItems,
//                                              Map<Integer, Integer[]> mapTidToUtilities) {
//        List<TxBlock> blocks = new ArrayList<>();
//        int global = 0;                 // 全局起始下标（仅在需要绝对位置时使用）
//        for (Map.Entry<Integer, Integer[]> entry : mapTidToItems.entrySet()) {
//            int tid = entry.getKey();
//            int[] seq  = toIntArray(entry.getValue());
//            int[] util = toIntArray(mapTidToUtilities.get(tid));
//            blocks.add(new TxBlock(seq, util, global));
//            global += seq.length;
//        }
//        return blocks;
//    }

    /* 把 Integer[] 快速转 int[] */
    private static int[] toIntArray(Integer[] src) {
        int[] dst = new int[src.length];
        for (int i = 0; i < src.length; i++) {
            dst[i] = src[i];
        }
        return dst;
    }
public void buildIndex() {
    int total = 0;
    for (Integer[] u : mapTidToUtilities.values()) total += u.length;

    flatItems = new int[total];
    flatUtils = new int[total];
    txnStarts = new int[mapTidToUtilities.size() + 1]; // 多一位方便二分

    int off = 0, tid = 0;
    for (Map.Entry<Integer, Integer[]> e : mapTidToItems.entrySet()) {
        txnStarts[tid++] = off;
        Integer[] items = e.getValue();
        Integer[] utils = mapTidToUtilities.get(e.getKey());
        for (int i = 0; i < items.length; i++) {
            flatItems[off]   = items[i];
            flatUtils[off++] = utils[i];
        }
    }
    txnStarts[tid] = off; // 哨兵
//    prefixSum = new int[flatUtils.length + 1];
//    for (int i = 0; i < flatUtils.length; i++) {
//        prefixSum[i + 1] = prefixSum[i] + flatUtils[i];
//    }
}

public int getUtility(int index) {
    if (index < 0 || index >= flatUtils.length) return 0;
    return flatUtils[index];
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
                    processLowUtilitySequence(sequenceNew, utilityOfSequence);
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
            if (utilityOfNewSequence <= max_utility) {
                utilityOfSequence = countUtility(sequenceNew);
                if (utilityOfSequence == -1) System.out.println("此序列为空！");
                if (utilityOfSequence <= max_utility) {
                    processLowUtilitySequence(sequenceNew, utilityOfSequence);
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
    }

    public void LUM1(List<Integer> sequence, List<List<Integer>> utilities,List<Integer> sequence1) throws IOException {
//        System.out.println("正在处理序列："+sequence1);
//        if(isHasProcessedSequence(sequence)){
//            return;
//        }
        candidatesCount++;
        int position = sequence1.size();
        List<Integer> sequenceNew = new ArrayList<>(sequence);
        int utilityOfSequence = 0;
        if(sequence.size()>=position+1){
            sequenceNew.remove(position);
            int utilityOfSequence_father = 0;
            List<List<Integer>> utilitiesNew = new ArrayList<>();
            for(List<Integer> utility:utilities){
                List<Integer> utilityNew=new ArrayList<>(utility);
                utilityNew.remove(position);
                utilitiesNew.add(utilityNew);
            }
//            if(sequence.size()==position+1){
//                int num=hasProcessedMaxSequenceSet.size();
//                List<Integer> maxSequence=maxSequenceList.get(num);
//                if(sequence.equals(maxSequence))
//                {
//                    hasProcessedMaxSequenceSet.add(maxSequence);
//                }
//                hasProcessedMaxSequenceSet.add(maxSequence);
//            }
            utilitiesNew=deduplicateUtilities(utilitiesNew);
            List<Integer> sequenceNew1=new ArrayList<>(sequence1);
            LUM1(sequenceNew, utilitiesNew,sequence1);
//            List<Integer> sequenceNew2 = new ArrayList<>(sequenceNew1);
            sequenceNew1.add(sequence.get(position));
            int utilityOfNewSequenceInFatherSeq = countUtility_FL(utilities, sequenceNew1.size());
            if (utilityOfNewSequenceInFatherSeq <= max_utility) {
                if(sequenceNew1.size()<=maxLength){
                    utilityOfSequence = countUtility(sequenceNew1);
                    if (utilityOfSequence == -1) System.out.println("此序列为空！");
                    if(utilityOfSequence<=max_utility){
                        processLowUtilitySequence(sequenceNew1, utilityOfSequence);
                    }
                }
//                List<Integer> sequenceNew3=new ArrayList<>(sequence);
//                List<List<Integer>> utiltiesNew=new ArrayList<>(utilities);
                    LUM1(sequence, utilities, sequenceNew1);
            }
//            else{
//                int num=hasProcessedMaxSequenceSet.size();
//                List<Integer> maxSequence=maxSequenceList.get(num);
//                hasProcessedMaxSequenceSet.add(maxSequence);
//            }
        }
    }
public boolean isHasProcessedSequence(List<Integer> sequence) {
//        return isContains(sequence,hasProcessedMaxSequenceSet);
    for (List<Integer> sequence1 : hasProcessedMaxSequenceSet) {
        // 检查当前序列是否与已处理的序列完全相同
        if (sequence1.equals(sequence)) {
            return true;
        }
        // 检查当前序列是否是已处理序列的子序列（考虑顺序）
        if (isSubsequence(sequence, sequence1)) {
            return true;
        }
    }
    // 如果没有找到匹配的情况，则返回false
    return false;
}

    // 判断sequence是否是mainSequence的子序列（考虑顺序）
    private boolean isSubsequence(List<Integer> sequence, List<Integer> mainSequence) {
        int seqIndex = 0; // sequence的索引
        for (int item : mainSequence) {
            if (seqIndex < sequence.size() && item == sequence.get(seqIndex)) {
                seqIndex++;
            }
        }
        return seqIndex == sequence.size();
    }
    private void processLowUtilitySequence(List<Integer> sequence, int utility) throws IOException {
//        if (!hasProcessedSequenceList.contains(sequence) && hasKnownHighUtilitySequence.contains(sequence) == false) {
        if (!hasProcessedSequenceList.contains(sequence)&&lowUtilityPattern.get(sequence)==null) {
            logLowUtilityInfo(sequence, utility);

        }
    }

    private void logLowUtilityInfo(List<Integer> sequence, int utility) throws IOException {
        if(sequence.size()==0){return;}
//        if (!hasProcessedSequenceList.contains(sequence) && hasKnownHighUtilitySequence.contains(sequence) == false) {
            System.out.println("获得低效用序列：" + sequence);
            StringBuilder buffer = new StringBuilder();
            buffer.append("获得低效用序列：" + sequence).append("\n");
            writer.write(buffer.toString());
            writer.newLine();
            hasProcessedSequenceList.add(sequence);
//            candidatesCount++;
            patternCount++;
            lowUtilityPattern.put(sequence, utility);
    }


    public void processCoreAlgorithm() throws IOException {
        System.out.println("当前运行到LUSM");
        int size = maxSequenceList.size();
        int num_now = 0;
        maxSequenceList.sort((a, b) -> b.size() - a.size());
        for (List<Integer> sequence : maxSequenceList) {
//            if(num_now==49){
//                System.out.println(11111);
//            }
//            else{break;}
            System.out.println(num_now + "/" + size);
            StringBuilder buffer = new StringBuilder();
            buffer.append(num_now + "/" + size);
            writer.write(buffer.toString());
            writer.newLine();
            num_now++;
            if(sequence.size()>0){
                List<List<Integer>> utilities = getUtilities(sequence);
//                cut(sequence,utilities);
//                processed(sequence,utilities);
                if(sequence.size()==0){
                    continue;
                }
//                utilities=getUtilities(sequence);
                int utilityOfSequence = countUtility_FL(utilities);
                if (utilityOfSequence == -1) System.out.println("此序列为空！");
                candidatesCount++;
//            if (utilityOfSequence <= max_utility && hasProcessedSequenceList.contains(sequence) == false&&hasProcessedSequenceListAndId.get(sequence) == null && hasKnownHighUtilitySequence.contains(sequence) == false) {
                if (utilityOfSequence <= max_utility && hasProcessedSequenceList.contains(sequence) == false&&hasProcessedSequenceListAndId.get(sequence) == null) {
                    if(sequence.size()<=maxLength){
                        List<Integer> sequenceNew=new ArrayList<>(sequence);
                        processLowUtilitySequence(sequenceNew,utilityOfSequence);
                    }
                    skrinkage(sequence,0);
                }
                else {
                    skrinkage_depth(sequence,utilities,0);
                }
//                hadProcessedMaxSequence++;
            }

//                hasDeleteSequenceList.add(sequence);
////                cut2(sequence);
//                hasDeleteSequenceList.add(sequence);
//                cut(sequence);
//                LUSM(sequence);
        }
    }
    public void cut(List<Integer> sequence,List<List<Integer>> utilities) throws IOException {
        if (sequence.size() > 1) {
            List<Integer> utility=utilities.get(0);
            int i,j;
            for(i=0;i<sequence.size();i++) {
            if(utility.get(i)<=max_utility){
                break;
            }
            for(j=sequence.size()-1;j>=0;j--){
                if(utility.get(j)>max_utility){
                    break;
                }
                if(i<j){
                    for(int k=sequence.size()-1;k>j;k--){
                        sequence.remove(k);
                        for(List<Integer> utility1:utilities){
                            utility1.remove(k);
                        }
                    }
                    for(int k=0;k<i;k++){
                        sequence.remove(k);
                        for(List<Integer> utility1:utilities){
                            utility1.remove(k);
                        }
                    }

                }
            }
            }
        }
    }
    public boolean isProcessed(List<Integer> sequence) throws IOException {
//        boolean hadProcessed=false;
        if(maxSequenceList.size()==0){
            return false;
        }
        else{
            for(int i=0;i<hadProcessedMaxSequence;i++){
                List<Integer> maxSequence=maxSequenceList.get(i);
                if(isSubsequence(sequence,maxSequence)){
                    return true;
                }
            }
        }
        return false;
    }

    public void skrinkage( List<Integer> sequence,int itemNum) throws IOException {
//        if(isProcessed(sequence)){return;}
        candidatesCount++;
//        List<List<Integer>> utilitiesOfSequence=getUtilities(sequence);
//        pruneItem(sequence,utilitiesOfSequence,itemNum);

        if(itemNum+1<sequence.size()){
            List<Integer> sequenceNew=new ArrayList<>(sequence);
            skrinkage(sequenceNew,itemNum+1);
        }

        if(itemNum+1==sequence.size()&&sequence.size()<=maxLength){
            int utility=countUtility(sequence);
            if(utility<=max_utility){
                List<Integer> sequenceNew2=new ArrayList<>(sequence);
                processLowUtilitySequence(sequenceNew2,utility);

            }
        }
        if(itemNum<sequence.size()){
            List<Integer> sequenceNew=new ArrayList<>(sequence);
            sequenceNew.remove(itemNum);
            if(sequenceNew.size()==0){
                return;
            }
//            System.out.println("正在处理序列："+sequenceNew);
            List<List<Integer>> utilities=getUtilities(sequenceNew);
            int utility=countUtility_FL(utilities);
            if(utility<=max_utility){
                if(sequenceNew.size()<maxLength){
                    List<Integer> sequenceNew3=new ArrayList<>(sequenceNew);
                    processLowUtilitySequence(sequenceNew3,utility);
                }
//                processLowUtilitySequence_improved(sequenceNew,utility);

                if(itemNum<sequenceNew.size()){
                    skrinkage(sequenceNew,itemNum);
                }
            }
            else{
                if(itemNum<sequenceNew.size()){
                    List<Integer> sequenceNew2=new ArrayList<>(sequenceNew);
//                    List<List<Integer>> utilitiesNew=new ArrayList<>(utilities);
                    skrinkage_depth(sequenceNew2,utilities,itemNum);
                }
            }
        }

    }
    public void skrinkage_depth( List<Integer> sequence,List<List<Integer>> utilities,int itemNum) throws IOException {
//        if(isProcessed(sequence)){return;}
        candidatesCount++;
//        List<List<Integer>> utilitiesOfSequence=copyUtilities(utilities);
        pruneItem(sequence,utilities,itemNum);

        if(itemNum+1<sequence.size()){
            List<Integer> sequenceNew1=new ArrayList<>(sequence);
            List<List<Integer>> utilitiesNew1=copyUtilities(utilities);
            skrinkage_depth(sequenceNew1,utilitiesNew1,itemNum+1);
        }
        if(itemNum+1==sequence.size()){
            int utility=countUtility(sequence);
            if(utility<=max_utility&&sequence.size()<=maxLength){
                List<Integer> sequenceNew=new ArrayList<>(sequence);
                processLowUtilitySequence(sequenceNew,utility);

            }
        }
//        List<List<Integer>> utilitiesNew2=copyUtilities(utilities);
        if(itemNum<sequence.size()){
//            List<Integer> sequenceNew=new ArrayList<>(sequence);
            sequence.remove(itemNum);
            if(sequence.size()==0){
                return;
            }
//            System.out.println("正在处理序列："+sequenceNew);
            List<List<Integer>> utilitiesOfFather=copyUtilities(utilities);
            for(List<Integer> utility:utilitiesOfFather){
                utility.remove(itemNum);
            }
            utilitiesOfFather=deduplicateUtilities(utilitiesOfFather);
                if (sequence.size()<=maxLength) {
                    if(countUtility_FL(utilitiesOfFather)<=max_utility){
                    List<List<Integer>> utilitiesNew = getUtilities(sequence);
                    if (countUtility(sequence) <= max_utility) {
//                    processLowUtilitySequence_improved(sequenceNew,countUtility(sequenceNew));
                        List<Integer> sequenceNew2 = new ArrayList<>(sequence);
                        processLowUtilitySequence(sequenceNew2, countUtility(sequenceNew2));
                        if (itemNum < sequence.size()) {
                            skrinkage(sequence, itemNum);
                        }
                    } else {
                        if (itemNum < sequence.size()) {
                            skrinkage_depth(sequence, utilitiesNew, itemNum);
                        }
                    }
                }
                else{
                    if (itemNum < sequence.size()) {
                        skrinkage_depth(sequence,utilitiesOfFather,itemNum);
                    }
                }
            }
            else{
                if(itemNum<sequence.size()){
                    skrinkage_depth(sequence,utilitiesOfFather,itemNum);
                }
            }
        }
    }

//    public void pruneItem(List<Integer> sequence, List<List<Integer>> utilities,int itemNum) throws IOException {
////        List<List<Integer>> utilities=getUtilities(sequence);
//        if(itemNum>=0&&itemNum<sequence.size()){
//            List<Integer> deleteId=new ArrayList<>();
//            for(int i=itemNum;i+1<sequence.size();i++){
//                int utilityOfFront;
//                if(itemNum==0){
//                    utilityOfFront=0;
//                }
//                else{
//                    utilityOfFront=countUtility_FL(utilities,itemNum);
//                }
//                for(List<Integer> utility:utilities){
//                    utilityOfFront+=utility.get(i);
//                }
//                if(utilityOfFront>max_utility){
//                    deleteId.add(i);
//                }
//            }
//            for(int i=deleteId.size()-1;i>=0;i--){
//                int deleteInt=deleteId.get(i);
//                sequence.remove(deleteInt);
//                for(List<Integer> utility:utilities){
//                    utility.remove(deleteInt);
//                }
//            }
//            utilities=deduplicateUtilities(utilities);
//        }
//    }
//public void pruneItem(List<Integer> sequence, List<List<Integer>> utilities,int itemNum) throws IOException {
////    System.out.println(sequence.size());
////    System.out.println(utilities);
////        List<List<Integer>> utilities=getUtilities(sequence);
//    if(itemNum>=0&&itemNum<sequence.size()){
//        List<Integer> deleteId=new ArrayList<>();
//        int utilityOfFrontCertain=0,utilityOfFront;
//        if(itemNum>0){
//            utilityOfFrontCertain=countUtility_FL(utilities,itemNum);
//        }
//        for(int i=itemNum;i+1<sequence.size();i++){
//            List<Integer> utility_address=new ArrayList<>();
//            utilityOfFront=utilityOfFrontCertain;
//            for(List<Integer> utility:utilities){
//                if(!utility_address.contains(utility.get(i))){
//                    utilityOfFront+=utility.get(i);
//                    utility_address.add(utility.get(i));
//                }
//            }
//            if(utilityOfFront>max_utility){
//                deleteId.add(i);
//            }
//        }
//        for(int i=deleteId.size()-1;i>=0;i--){
//            int deleteInt=deleteId.get(i);
//            sequence.remove(deleteInt);
//            for(List<Integer> utility:utilities){
//                utility.remove(deleteInt);
//            }
//        }
//        utilities=deduplicateUtilities(utilities);
//    }
//}
public void pruneItem(List<Integer> sequence,
                      List<List<Integer>> utilities,
                      int itemNum) throws IOException {

    if (itemNum < 0 || itemNum >= sequence.size()) {
        return;
    }

    List<Integer> deleteId = new ArrayList<>();
    int utilityOfFrontCertain = 0;

    /* 1. 计算已固定前缀的效用（itemNum 之前） */
    if (itemNum > 0) {
        utilityOfFrontCertain = countUtility_FL(utilities, itemNum);
    }

    /* 2. 向后扫描，决定哪些位置需要删除 */
    for (int i = itemNum; i + 1 < sequence.size(); i++) {
        // 按对象地址去重
        Set<Integer> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        int utilityOfFront = utilityOfFrontCertain;

        for (List<Integer> utility : utilities) {
            Integer val = utility.get(i);
            if (seen.add(val)) {          // 同一个对象只加一次
                utilityOfFront += val;
            }
        }

        if (utilityOfFront > max_utility) {
            deleteId.add(i);
        }
    }

    /* 3. 倒序删除，避免索引错位 */
    for (int i = deleteId.size() - 1; i >= 0; i--) {
        int idx = deleteId.get(i);
        sequence.remove(idx);
        for (List<Integer> utility : utilities) {
            utility.remove(idx);
        }
    }

    /* 4. 可选：进一步按地址去重整个 utilities（如果你需要） */
    utilities = deduplicateUtilities(utilities);
}
//public List<List<Integer>> copyUtilities(List<List<Integer>> utilities){
//        List<List<Integer>> utilitiesNew=new ArrayList<>();
//        for(List<Integer> utility:utilities){
//            List<Integer> utilityNew=new ArrayList<>();
//            for(Integer item:utility){
//                utilityNew.add(item);
//            }
//            utilitiesNew.add(utilityNew);
//        }
//        return utilitiesNew;
//}
public List<List<Integer>> copyUtilities(List<List<Integer>> utilities) {
    List<List<Integer>> utilitiesNew = new ArrayList<>();
    for (List<Integer> utility : utilities) {
        utilitiesNew.add(new ArrayList<>(utility));
    }
    return utilitiesNew;
}

    public void LUSM(List<Integer> sequence, List<List<Integer>> utilities, int itemNum) throws IOException {
        /*保留该项
         * */
        if(itemNum==sequence.size()-1){
            int utilityOfSequence=countUtility(sequence);
            if(utilityOfSequence<=max_utility){
                if(hasProcessedSequenceListAndId.get(sequence)==null){
                    logLowUtilityInfo(sequence, utilityOfSequence);
                    hasProcessedSequenceListAndId.put(sequence,itemNum);
                } else{
                    hasProcessedSequenceListAndId.put(sequence,itemNum);
                }
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
            if (utilityOfSequence > max_utility&&itemNum<sequenceNew.size()) {
                LUSM_width(sequenceNew, utilitiesNew, itemNum);
            } else {
                utilityOfSequence = countUtility(sequenceNew);
                if (sequenceNew.size() == 1) {
                    if (utilityOfSequence<= max_utility) {
                        processLowUtilitySequence_improved(sequenceNew, countUtility(sequenceNew), 0);
                    }
                }
                if (utilityOfSequence <= max_utility) {
                    processLowUtilitySequence_improved(sequenceNew, utilityOfSequence, itemNum - 1);
                } else {
                    if (itemNum < sequenceNew.size()) {
                        LUSM_width(sequenceNew, utilitiesNew, itemNum);
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
                        processLowUtilitySequence_improved(sequenceNew,utilityOfNewSequence,itemNum);
                    }
                }
                if(itemNum<sequenceNew.size()){
                    LUSM_width(sequenceNew, utilitiesNew, itemNum);
                }
            }
//            不删除该项
            if(itemNum+1<sequence.size()){
                LUSM_width(sequence, utilities, itemNum + 1);
            }
            if(itemNum+1==sequence.size()){
                int utilityOfNewSequence=countUtility(sequence);
                if(utilityOfNewSequence<=max_utility){
                    processLowUtilitySequence_improved(sequence,utilityOfNewSequence,itemNum);
                }
            }
        }
        if(hasProcessedSequenceListAndId.get(sequence)==null){
            hasProcessedSequenceListAndId.put(sequence,itemNum);
        }else{
            hasProcessedSequenceListAndId.put(sequence,itemNum);
        }
    }
    private void processLowUtilitySequence_improved(List<Integer> sequence, int utility, int itemNum) throws IOException {
        boolean key=false;
        if(hasProcessedSequenceListAndId.get(sequence)==null){
            logLowUtilityInfo(sequence, utility);
        }
        if ((hasProcessedSequenceListAndId.get(sequence)==null||hasProcessedSequenceListAndId.get(sequence) >=itemNum) ) {
            key=true;
        }
//        if ((hasProcessedSequenceListAndId.get(sequence)==null) ) {
//            key=true;
//        }
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
            processLowUtilitySequence_improved(sequence, utility, 0);
        }
    }


//    public int countUtility_FL(List<List<Integer>> utilities) {
//        return utilities.stream()
//                .filter(Objects::nonNull)
//                .flatMap(List::stream)
//                .reduce(0, Integer::sum);
//    }
    public int countUtility_FL(List<List<Integer>> utilities) {
        int sum = 0;
        for (List<Integer> list : utilities) {
            for (int v : list) sum += v;
        }
        return sum;
    }
//    public int countUtility_FL(List<List<Integer>> utilities) {
//        return utilities.parallelStream()
//                .flatMapToInt(list -> list.stream().mapToInt(Integer::intValue))
//                .sum();
//    }
//public int countUtility_FL(List<List<Integer>> utilities) {
//    return utilities.stream()
//            .flatMapToInt(list -> list.stream().mapToInt(Integer::intValue))
//            .sum();
//}

    /**
     * 统计 utilities 中每个 List 的前 num 个元素之和，
     * 若 num <= 0 或任一 List 长度不足，则按实际长度累加。
     * 支持提前剪枝：超过 max_utility 立即返回。
     *
     * @param utilities 效用矩阵
     * @param num       每个 List 要累加的元素个数
     * @return 累加结果
     */
//    public int countUtility_FL(List<List<Integer>> utilities, int num) {
//        if (utilities == null || num <= 0) {
//            return 0;
//        }
//
//        int sum = 0;
//        for (List<Integer> list : utilities) {
//            int bound = Math.min(num, list.size());
//            for (int i = 0; i < bound; i++) {
//                sum += list.get(i);
//                // 提前剪枝（可选）
//                if (sum > max_utility) {
//                    return sum;   // 或 return max_utility + 1; 视业务而定
//                }
//            }
//        }
//        return sum;
//    }
    public int countUtility_FL(List<List<Integer>> utilities, int num) {
        if (utilities == null || num <= 0) {
            return 0;
        }
        int sum = 0;
        for(int i=0;i<num;i++){
            Set<Integer> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            for (List<Integer> utility : utilities) {
                Integer val = utility.get(i);
                if (seen.add(val)) {          // 同一个对象只加一次
                    sum += val;
                }
                // 提前剪枝（可选）
                if (sum > max_utility) {
                    return sum;   // 或 return max_utility + 1; 视业务而定
                }
            }
        }
        return sum;
    }
    // 扁平数组专用
//    public int countUtilityFlat(int startInclusive, int endExclusive) {
//        if (startInclusive < 0 || endExclusive > flatUtils.length || startInclusive >= endExclusive)
//            return 0;
//        return prefixSum[endExclusive] - prefixSum[startInclusive];
//    }

public boolean isContains(List<Integer> seq, List<List<Integer>> set) {
    // 1. 一次性把查询序列转 int[]
    final int[] pat = seq.stream().mapToInt(Integer::intValue).toArray();
    final int n = pat.length;

    for (List<Integer> cand : set) {
        if (cand.size() < n) continue;          // 长度剪枝
        if (isSub(pat, cand)) return true;      // 指针版子序列判断
    }
    return false;
}

    /* 指针版子序列判断：时间 O(|main|)，空间 O(1) */
    private static boolean isSub(int[] pat, List<Integer> main) {
        int i = 0, m = pat.length;
        for (int v : main) {
            if (i < m && v == pat[i]) i++;
        }
        return i == m;
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

public void printStats(List<Double> runTime, List<Double> memory, List<Long> candidates, List<Integer> pattern) {
    runTime.add((double) runtime / 1000); // 运行时间，单位为秒
    memory.add(MemoryLogger.getInstance().getMaxMemory()); // 最大内存使用量，单位为MB
    candidates.add(candidatesCount); // 候选集数量
    pattern.add(patternCount); // 模式数量
}


    private void processSubSequence(List<Integer> subSequence) throws IOException {
//        if (subSequence.size() > 1 && !hasProcessedSequenceList.contains(subSequence) && hasKnownHighUtilitySequence.contains(subSequence) == false) {
        if (subSequence.size() > 1 && !hasProcessedSequenceList.contains(subSequence)) {
            int utility = countUtility(subSequence);
            if (utility == -1) System.out.println("该序列为空");
//            hasProcessedSequenceList.add(subSequence);
            if (utility <= max_utility && lowUtilityPattern.get(subSequence) == null) {
                processLowUtilitySequence_improved(subSequence, utility, 0);
            } else {
//                cut(subSequence);
            }
        }
        if (subSequence.size() == 1) {
            processSingleElementSequence(subSequence);
        }
    }


public void printLowUtilitySequence() throws IOException {
    writer.write("低效用非连续序列有：" + patternCount + "个\n");
    System.out.println("低效用序列有:" + patternCount + "个");

    if (patternCount > 0) {
        writer.write("输出低效用序列：\n");
        System.out.println("输出低效用序列：");

        int limit = 1000000000; // 限制输出最多 1000000000 条记录（可调整）
        int count = 0;

        for (Map.Entry<List<Integer>, Integer> entry : lowUtilityPattern.entrySet()) {
            if (count++ >= limit) break;
            String line = "序列：" + entry.getKey() + "  效用：" + entry.getValue() + "\n";
            writer.write(line);
            System.out.println(line.trim());
        }
    }

    writer.write("输出低效用序列的数量：" + lowUtilityPattern.size() + "\n");
    writer.newLine();
}


    public void showStates() throws IOException {
        StringBuilder buffer = new StringBuilder();
        buffer.append("计算效用运行次数为：" + countOfCountUtilities).append("\n");
        buffer.append("存储数据运行时间为：" + (double) runtime2 / 1000 + "s").append("\n");
        buffer.append("获得项的位图运行时间为：" + (double) runtime4 / 1000 + "s").append("\n");
        buffer.append("获得最长序列运行时间为：" + (double) runtime5 / 1000 + "s").append("\n");
        buffer.append("预处理运行时间为：" + (double) runtime3 / 1000 + "s").append("\n");
        buffer.append("运行时间为：" + (double) runtime / 1000 + "s").append("\n");
        buffer.append("运行内存为：" + MemoryLogger.getInstance().getMaxMemory() + "MB").append("\n");
        buffer.append("总的内存为：" + (double) Runtime.getRuntime().totalMemory() / 1024d / 1024d + "MB").append("\n");
        buffer.append("空闲内存为：" + (double) Runtime.getRuntime().freeMemory() / 1024d / 1024d + "MB").append("\n");
        buffer.append("候选集数目为_改进版：" + candidatesCount).append("\n");
        buffer.append("模式数目为：" + patternCount).append("\n");
        writer.write(buffer.toString());
        writer.newLine();
        System.out.println("计算效用运行次数为：" + countOfCountUtilities);
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

}

