package java1;

import java.util.*;

public class Generator3 {
    int itemsCount_row;
    int tidCount_column;
   // boolean[][] bitOfItem;
    BitSet[] bitOfItem;
   // int[][] uOfItem;
    Map<Integer,Map<Integer,Integer>> uMapOfItemToTidU=new HashMap<>();
    //<item,<tid,utility>>
    //Map<Integer,Map<Integer,Integer>> uMapOfTidToItemU=new HashMap<>();
    Map<Integer,Integer> mapItemtoIndex=new HashMap<>();
    //items 为按照字典序或其他顺序排列的item总数，tidcount为事务总数
    public Generator3(Set<Integer> items, int tidCount) {
        itemsCount_row = items.size();
        this.tidCount_column = tidCount;
        bitOfItem = new BitSet[itemsCount_row];
        int i = 0;
        while (i < itemsCount_row) {
            bitOfItem[i] = new BitSet(tidCount_column);
            i++;
        }
        //uOfItem=new int[itemsCount_row][tidCount_column];
        int index=0;
        for (Integer item:items) {
            mapItemtoIndex.put(item,index++);
        }
//        System.out.println("输出mapItemtoIndex的类型");
//        System.out.println(mapItemtoIndex.getClass().toString());
//        for (Map.Entry<Integer, Integer> entry : mapItemtoIndex.entrySet()) {
//            System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
//        }

    }
    //items 的顺序要按照字典序或其他固定顺序
    public void setGeneratorOne(int tid,String[] items, String[] utils){
//        Map<Integer,Integer> mapItemToU=uMapOfTidToItemU.get(tid);
//        if (mapItemToU==null){
//            mapItemToU=new HashMap<>();
//            uMapOfTidToItemU.put(tid,mapItemToU);
//        }
        for (int k = 0; k < items.length; k++) {
            int index=mapItemtoIndex.get(Integer.valueOf(items[k]));
            bitOfItem[index].set(tid-1);
           // bitOfItem[index][tid-1]=true;
           // uOfItem[index][tid-1]=Integer.valueOf(utils[k]);
//            uMapOfItemToTidU对应某一项所在的序列和效用
            Map<Integer,Integer> mapTidToU=uMapOfItemToTidU.get(index);
            if (mapTidToU==null){
                mapTidToU=new HashMap<>();
                uMapOfItemToTidU.put(index,mapTidToU);
            }
            mapTidToU.put(tid-1,Integer.valueOf(utils[k]));
//            System.out.println("输出mapTidToU:"+mapTidToU.getClass().toString());
//            System.out.println("输出index:"+index);
//            for(Map.Entry<Integer, Integer> entry : mapTidToU.entrySet()){
//                System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
//            }
           // mapItemToU.put(index,Integer.valueOf(utils[k]));
        }
    }
    //找到同时包含itemset里面所有item的tid,求每一个item在这些tid里的utility
    //map<item,totalUtility>
    public ULB getUBofItemset(int[] itemset){
//        System.out.print("输出itemset:");
//        for(int i=0;i<itemset.length;i++)
//        {
//            System.out.print(itemset[i]);
//        }
//        System.out.println(" ");
        ULB res=new ULB();
        res.setItemset(itemset);
        Map<Integer,Integer> mapItemToUB=new LinkedHashMap<>();
        res.setMapItemToLB(mapItemToUB);
        //所有位设置为1：111111111
        BitSet resBit=new BitSet(tidCount_column);
        resBit.set(0,tidCount_column);
//        boolean[] bitTidsOfItemset=new boolean[tidCount_column];
//        for (int i = 0; i < bitTidsOfItemset.length; i++) {
//            bitTidsOfItemset[i]=true;
//        }
        //每个 item
        for (int i = 0; i < itemset.length; i++) {
            //获取item的索引
            Integer index=mapItemtoIndex.get(itemset[i]);
//            System.out.println("itemset[i]:"+itemset[i]);
//            System.out.println("index:"+index);
            //generator中不含某个item(通常前部分有，后部分没有),这种情况LB通常是0，因为没有tid同时含有这个itemset
            if (index==null){
                return res;
            }
//            bitOfitem是某个项所在的序列号
//            System.out.println("输出bitOfItem:"+bitOfItem[index]);
//            System.out.println("输出resBit1:");
//            System.out.println(resBit);
            //每个tid
//          得到该模式所在的序列号
            resBit.and(bitOfItem[index]);
//            System.out.println("输出resBit2:");
//            System.out.println(resBit);
//            for (int j = 0; j < bitTidsOfItemset.length; j++) {
//                bitTidsOfItemset[j]=bitTidsOfItemset[j]&bitOfItem[index][j];
//            }
        }

        //resBit.stream().forEach(e -> System.out.println(e));
        for (int tid = resBit.nextSetBit(0); tid <= tidCount_column-1&&tid>=0; tid = resBit.nextSetBit(tid+1)) {
            // 输出tid
            //System.out.println(tid);
            for (int j = 0; j < itemset.length; j++) {
                int index=mapItemtoIndex.get(itemset[j]);
//                System.out.println("itemset[j]:"+itemset[j]);
//                System.out.println("index:"+index);
                Integer old= mapItemToUB.get(itemset[j]);
//                System.out.println("old:"+old);
//                mapItemToUB存储模式中各项的总的效用
                //正常情况下，uMapOfItemToTidU.get(index).get(tid)不会为空
                if (old!=null){
                    mapItemToUB.put(itemset[j],old+uMapOfItemToTidU.get(index).get(tid));
                }else {
                    mapItemToUB.put(itemset[j],uMapOfItemToTidU.get(index).get(tid));
//                    System.out.println(itemset[j]+":"+uMapOfItemToTidU.get(index).get(tid));
                }
            }
        }
        return res;
    }

    public void setGenerator(Map<Integer, String[]> mapTidToItems, Map<Integer, String[]> mapTidToUtilities) {
        for (Integer tid: mapTidToItems.keySet()) {
            setGeneratorOne(tid,mapTidToItems.get(tid),mapTidToUtilities.get(tid));
//            System.out.println("tid:"+tid+" items:"+mapTidToItems.get(tid));
        }
    }

}
