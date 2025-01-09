package java1;

import java.util.Map;

public class ULB {
    int[] itemset;
    Map<Integer,Integer> mapItemToLB;

    public int getUofItemset(){
        int utility=0;
        for (Integer key:mapItemToLB.keySet()) {
            utility+=mapItemToLB.get(key);
        }
        return utility;
    }
    //depth prune
    //获取排在lastDelItem之前的所有item的utility作为深度剪枝的下界
    public int getLBofExD(int lastDelItem){
        if (itemset[0]>lastDelItem){
            int min=Integer.MAX_VALUE;
            for (int i = 0; i < itemset.length; i++) {
                if (mapItemToLB.get(itemset[i])!=null){
                    min=Math.min(min,mapItemToLB.get(itemset[i]));
                }else {
                    min=Math.min(min,0);
                }
            }
            return min;
        }
        int LB=0;
        for (int i = 0; i < itemset.length; i++) {
            if (itemset[i]>lastDelItem){
                break;
            }else {
                if (mapItemToLB.get(itemset[i])!=null){
                    LB+=mapItemToLB.get(itemset[i]);
                }
                //                else {
                //                    LB+=0;
                //                }
            }
        }
        return LB;
    }
    public int getMinUOfItem(){
        int minUtility=0;
        for (Integer key:mapItemToLB.keySet()) {
            minUtility=Math.min(mapItemToLB.get(key),minUtility);
        }
        return minUtility;
    }
    //width prune
    public int getLBofExW(int lastDelItem){

        if (lastDelItem==itemset[0]){
            int min=Integer.MAX_VALUE;
            for (int i = 1; i < itemset.length; i++) {
                if (mapItemToLB.get(itemset[i])!=null){
                    min=Math.min(min,mapItemToLB.get(itemset[i]));
                }else {
                    min=Math.min(min,0);
                }
            }
            return min;
        }
        int LB=0;
        for (int i = 0; i < itemset.length; i++) {
            if (itemset[i]==lastDelItem){
                break;
            }else {
                if (mapItemToLB.get(itemset[i])!=null){
                    LB+=mapItemToLB.get(itemset[i]);
                }
    //                else {
    //                    LB+=0;
    //                }
            }
        }
        return LB;
    }


    public int[] getItemset() {
        return itemset;
    }

    public void setItemset(int[] itemset) {
        this.itemset = itemset;
    }

    public Map<Integer, Integer> getMapItemToUB() {
        return mapItemToLB;
    }

    public void setMapItemToLB(Map<Integer, Integer> mapItemToLB) {
        this.mapItemToLB = mapItemToLB;
    }


}
