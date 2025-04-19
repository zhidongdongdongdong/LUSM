package main;
import java1.LowUtilitySequenceMining;
import java1.MemoryLogger;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainLUSMAlgorithm {
    static List<Double> runTime=new ArrayList<>();
    static List<Double> memory=new ArrayList<>();
    static List<Long> candidates=new ArrayList<>();
    static List<Integer> pattern=new ArrayList<>();
    public static void main(String[] args)throws IOException {
        //扫描文件
//        String input1="bible_short.txt";
//        String input1="bible.txt";
        String input1="example.txt";
//        String input1="SIGN_sequence_utility.txt";
//        String input1="kosrak10k.txt";
//        String input1="bible_short.txt";
        BufferedWriter writer = null;
        String finalInput=input1;
        String input = fileToPath("/"+finalInput);
        String output = ".//outputNew.txt";
        Runtime runtime=Runtime.getRuntime();
        long initialMemory=runtime.totalMemory()-runtime.freeMemory();
        System.out.println("程序开始前的内存消耗："+initialMemory/(1024*1024)+"MB");
        writer = new BufferedWriter(new FileWriter(output));
        int[] max_utility = new int[]{50};
        int maxLength=5;
        for (int i = 0; i < max_utility.length; i++) {
            MemoryLogger.getInstance().reset();
            LowUtilitySequenceMining lowUtilitySequenceMining = new LowUtilitySequenceMining();
            MemoryLogger.getInstance().checkMemory();
            lowUtilitySequenceMining.runAlgorithm(input, max_utility[i],maxLength, writer);
            MemoryLogger.getInstance().checkMemory();
            lowUtilitySequenceMining.printStats(runTime,memory,candidates,pattern);
        }
        long finalMemory=runtime.totalMemory()-runtime.freeMemory();
        StringBuilder buffer = new StringBuilder();
        buffer.append("程序结束时的内存消耗："+finalMemory/(1024*1024)+"MB\n");
        buffer.append("程序总共消耗的内存为："+(finalMemory-initialMemory)/(1024*1024)+"MB\n");
        buffer.append("真实内存消耗为： "+ MemoryLogger.getInstance().getMaxMemory()+ " MB\n");
        buffer.append("输出maxutility:"+max_utility[0]);
        writer.write(buffer.toString());
        writer.newLine();
        System.out.println("程序结束时的内存消耗："+finalMemory/(1024*1024)+"MB");
        long memoryConsumed=finalMemory-initialMemory;
        System.out.println("程序总共消耗的内存为："+memoryConsumed/(1024*1024)+"MB");
        System.out.println("真实内存消耗为： "+ MemoryLogger.getInstance().getMaxMemory()+ " MB");
        writer.close();
        //        OutputExp(max_utility,finalInput);
    }
    private static void OutputExp(int[] max_utility, String input) throws IOException {
        String experimentFile = ".//newexp"+input;
        BufferedWriter bufferedWriter=new BufferedWriter(new FileWriter(experimentFile));
        bufferedWriter.write("maxUtil: ");
        for (int i = 0; i < max_utility.length; i++) {
            if (i==max_utility.length-1){
                bufferedWriter.write(max_utility[i]+"");
            }else {
                bufferedWriter.write(max_utility[i]+",");
            }
        }
        bufferedWriter.newLine();
        bufferedWriter.write("runTime: ");
        for (int i = 0; i < max_utility.length; i++) {
            if (i==max_utility.length-1){
                bufferedWriter.write(runTime.get(i)+"");
            }else {
                bufferedWriter.write(runTime.get(i)+",");
            }
        }
        bufferedWriter.newLine();
        bufferedWriter.write("memory: ");
        for (int i = 0; i < max_utility.length; i++) {
            if (i==max_utility.length-1){
                bufferedWriter.write(memory.get(i)+"");
            }else {
                bufferedWriter.write(memory.get(i)+",");
            }

        }
        bufferedWriter.newLine();
        bufferedWriter.write("candidates: ");
        for (int i = 0; i < max_utility.length; i++) {
            if (i==max_utility.length-1){
                bufferedWriter.write(candidates.get(i)+"");
            }else {
                bufferedWriter.write(candidates.get(i)+",");
            }

        }
        bufferedWriter.newLine();
        bufferedWriter.write("patterns: ");
        for (int i = 0; i < max_utility.length; i++) {
            if (i==max_utility.length-1){
                bufferedWriter.write(pattern.get(i)+"");
            }else {
                bufferedWriter.write(pattern.get(i)+",");
            }

        }
        bufferedWriter.flush();
        bufferedWriter.close();
    }

    public static String fileToPath(String filename) throws UnsupportedEncodingException{
        URL url = MainLUSMAlgorithm.class.getResource(filename);
        if(url!=null)
        {
//            System.out.println("url不为空！");
//            System.out.println(url);
            return java.net.URLDecoder.decode(url.getPath(),"UTF-8");
        }
        else {
            System.out.println(url);
            System.out.println("url为空");
            return "111";
        }
    }
}