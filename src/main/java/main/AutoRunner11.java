package main;

import java.io.*;
import java.util.*;
import java1.LUSM_naive;
import java1.LowUtilitySequenceMining;
import java1.MemoryLogger;

public class AutoRunner11 {

    private static final int MAX_MIN_UTIL = 20;      // 1..10
    private static final int MAX_LENGTH   = Integer.MAX_VALUE;

    // 是否调用 LUSM_naive，设置为 true 调用 LUSM_naive，设置为 false 调用 LowUtilitySequenceMining
    private static final boolean USE_LUSM_NAIVE = false;

    public static void main(String[] args) throws Exception {
        // 你想跑的文件列表（与 MainLUSMAlgorithm 一样）
        String[] fileNames = {
//                "SIGN_sequence_utility_sequence1.txt",
//                "SIGN_sequence_utility_sequence2.txt",
//                "SIGN_sequence_utility_sequence3.txt",
//                "SIGN_sequence_utility_sequence4.txt",
//                "SIGN_sequence_utility_sequence5.txt",
//                "SIGN_sequence_utility_sequence5.txt",
//                "Synthetic_10k.txt",
//                "Synthetic_80k.txt",
//                "Synthetic_160k.txt",
//                "Synthetic_320k.txt",
//                "Synthetic_400k.txt",
//                "Synthetic5k.txt",
//                "Synthetic6k.txt",
//                "Synthetic7k.txt",
//                "Synthetic8k.txt",
//                "Synthetic9k.txt",
//                "Synthetic50k.txt",
//                "Synthetic60k.txt",
//                "Synthetic70k.txt",
//                "Synthetic80k.txt",
//                "Synthetic90k.txt",
//                "Synthetic100k.txt",
//                "SIGN_sequence_utility_sequence7.txt",
//                "SIGN_sequence_utility_sequence8.txt",
//                "SIGN_sequence_utility_sequence9.txt",
//                "SIGN_sequence_utility_sequence10.txt",
//                "SIGN_sequence_utility_short1.txt",
//                "SIGN_sequence_utility_short2.txt",
//                "SIGN_sequence_utility_short3.txt",
//                "SIGN_sequence_utility_short4.txt",
//                "SIGN_sequence_utility_short5.txt",
//                "SIGN_sequence_utility_short6.txt",
//                "SIGN_sequence_utility_short7.txt",
//                "SIGN_sequence_utility_short8.txt",
//                "SIGN_sequence_utility_short9.txt",
//                "SIGN_sequence_utility_short10.txt",
//                "SIGN_sequence_utility_short11.txt",
//                "SIGN_sequence_utility_short12.txt",
//                "SIGN_sequence_utility_short13.txt",
//                "SIGN_sequence_utility_short14.txt",
//                "SIGN_sequence_utility_short15.txt",
//                "SIGN_sequence_utility_short16.txt",
//                "SIGN_sequence_utility_short17.txt",
//                "SIGN_sequence_utility_short18.txt",
//                "SIGN_sequence_utility_short19.txt",
//                "SIGN_sequence_utility_short20.txt",
//                "SIGN_sequence_utility_short21.txt",
//                "SIGN_sequence_utility_short22.txt",
//                "SIGN_sequence_utility_short23.txt",
//                "SIGN_sequence_utility_short24.txt",
//                "SIGN_sequence_utility_short25.txt",
//                "SIGN_sequence_utility_short26.txt",
//                "SIGN_sequence_utility_short27.txt",
//                "SIGN_sequence_utility_short28.txt",
//                "SIGN_sequence_utility_short29.txt",
//                "SIGN_sequence_utility_short30.txt",
//                "SIGN_sequence_utility_short31.txt",
//                "SIGN_sequence_utility_short32.txt",
//                "SIGN_sequence_utility_short33.txt",
//                "SIGN_sequence_utility_short34.txt",
                "chess_synthetic_3k.txt",
//                "SIGN_sequence_utility.txt",
//                "BMS_sequence_utility.txt",
//                "kosrak10k.txt",
//                "Leviathan.txt",
//                "bible.txt",
//                "FIFA_sequence_utility.txt"
        };
        // 汇总 CSV（放在项目根目录）
        try (PrintWriter csv = new PrintWriter("summary.csv")) {
            csv.println("file,minUtil,runtimeSec,memoryMB,candidates,patterns");

            for (String fn : fileNames) {
                String inputPath = MainLUSMAlgorithm.fileToPath("/" + fn);

                for (int minUtil = 10; minUtil <= MAX_MIN_UTIL; minUtil++) {
                    String base = fn.replaceFirst("[.][^.]+$", ""); // 去掉 .txt
//                    String outName = "output_naive" + base + "_minUtil_" + minUtil + "_new" + ".txt";
                    String outName = "output_" + base + "_minUtil_" + minUtil + "_new" + ".txt";

                    System.out.printf(">>> %s  minUtil=%d%n", fn, minUtil);

                    MemoryLogger.getInstance().reset(); // 重置内存统计
                    Stats stats = runOnce(inputPath, minUtil, outName);

                    // 写入汇总 CSV
                    csv.printf("%s,%d,%.2f,%.2f,%d,%d%n",
                            base, minUtil,
                            stats.runtimeMs() / 1000.0,
                            stats.memoryMB(),
                            stats.candidates(),
                            stats.patterns());

                    // 写入详细信息到输出文件
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(outName, true))) {
                        writer.write("程序结束时的内存消耗：" + stats.finalMemory() / (1024 * 1024) + "MB\n");
                        writer.write("程序总共消耗的内存为：" + stats.memoryConsumed() / (1024 * 1024) + "MB\n");
                        writer.write("最大内存使用量： " + stats.memoryMB() + " MB\n");
                        writer.write("输出maxutility:" + minUtil + "\n");
                        writer.write("运行时间为：" + stats.runtimeMs() + "秒\n");
                        writer.write("候选集数量为：" + stats.candidates() + "\n");
                        writer.write("模式数量为：" + stats.patterns() + "\n");
                    }
                }
            }
        }

        System.out.println("All jobs finished. Check summary.csv & output_*.txt");
    }

    /* 运行一次算法并收集统计信息 */
    private static Stats runOnce(String input, int minUtil, String outName) throws Exception {
        List<Double> rt = new ArrayList<>();
        List<Double> mem = new ArrayList<>();
        List<Long> cand = new ArrayList<>();
        List<Integer> pat = new ArrayList<>();

        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outName))) {
            if (USE_LUSM_NAIVE) {
                LUSM_naive miner = new LUSM_naive(); // 使用 LUSM_naive
                MemoryLogger.getInstance().checkMemory(); // 检查初始内存
                miner.runAlgorithm(input, minUtil, MAX_LENGTH, writer); // 调用 LUSM_naive 的 runAlgorithm 方法
                MemoryLogger.getInstance().checkMemory(); // 检查最终内存
                miner.printStats(rt, mem, cand, pat); // 调用 LUSM_naive 的 printStats 方法
            } else {
                LowUtilitySequenceMining miner = new LowUtilitySequenceMining(); // 使用 LowUtilitySequenceMining
                MemoryLogger.getInstance().checkMemory(); // 检查初始内存
                miner.runAlgorithm(input, minUtil, MAX_LENGTH, writer); // 调用 LowUtilitySequenceMining 的 runAlgorithm 方法
                MemoryLogger.getInstance().checkMemory(); // 检查最终内存
                miner.printStats(rt, mem, cand, pat); // 调用 LowUtilitySequenceMining 的 printStats 方法
            }
        }

        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryConsumed = Math.max(0, finalMemory - initialMemory); // 避免负数

        return new Stats(
                rt.get(0),
                mem.get(0),
                cand.get(0),
                pat.get(0),
                finalMemory,
                memoryConsumed,
                MemoryLogger.getInstance().getMaxMemory()
        );
    }

    /* 简单 DTO */
    private static class Stats {
        private final double runtimeMs;
        private final double memoryMB;
        private final long candidates;
        private final int patterns;
        private final long finalMemory;
        private final long memoryConsumed;

        Stats(double rt, double mem, long c, int p, long finalMemory, long memoryConsumed, double maxMemoryUsage) {
            this.runtimeMs = rt;
            this.memoryMB = maxMemoryUsage; // 使用最大内存使用量
            this.candidates = c;
            this.patterns = p;
            this.finalMemory = finalMemory;
            this.memoryConsumed = memoryConsumed;
        }

        public double runtimeMs() { return runtimeMs; }
        public double memoryMB() { return memoryMB; }
        public long candidates() { return candidates; }
        public int patterns() { return patterns; }
        public long finalMemory() { return finalMemory; }
        public long memoryConsumed() { return memoryConsumed; }
    }
}