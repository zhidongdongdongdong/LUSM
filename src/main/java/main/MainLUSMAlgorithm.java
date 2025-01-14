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
        String input1="example2.txt";
        String finalInput=input1;
        String input = fileToPath("/"+finalInput);
        String output = ".//outputNew.txt";
        int[] max_utility = new int[]{1000};
        int maxLength=5;
        for (int i = 0; i < max_utility.length; i++) {
            MemoryLogger.getInstance().reset();
            LowUtilitySequenceMining lowUtilitySequenceMining = new LowUtilitySequenceMining();
            MemoryLogger.getInstance().checkMemory();
            lowUtilitySequenceMining.runAlgorithm(input, max_utility[i],maxLength, output);
            MemoryLogger.getInstance().checkMemory();
            lowUtilitySequenceMining.printStats(runTime,memory,candidates,pattern);
        }
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