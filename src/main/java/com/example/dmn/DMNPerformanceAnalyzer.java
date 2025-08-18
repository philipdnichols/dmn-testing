package com.example.dmn;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Advanced statistical analysis utility for DMN performance comparison results.
 * Analyzes the impact of individual optimization options and their combinations.
 */
public class DMNPerformanceAnalyzer {

    public static void main(String[] args) {
        if (args.length > 0) {
            analyzeFromCsv(args[0]);
        } else {
            System.out.println("Usage: java DMNPerformanceAnalyzer <csv-file>");
            System.out.println("Or run after DMNPerformanceComparison to analyze dmn-performance-results.csv");
        }
    }

    public static void analyzeFromCsv(String csvFilePath) {
        try {
            List<PerformanceData> data = loadDataFromCsv(csvFilePath);
            if (data.isEmpty()) {
                System.err.println("No data found in CSV file: " + csvFilePath);
                return;
            }
            
            DMNPerformanceAnalyzer analyzer = new DMNPerformanceAnalyzer();
            analyzer.performComprehensiveAnalysis(data);
            
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
        }
    }

    public void performComprehensiveAnalysis(List<PerformanceData> data) {
        // Generate comprehensive analysis report file only
        exportDetailedAnalysis(data);
        
        // Simple confirmation message
        System.out.println("Comprehensive analysis complete. Results written to dmn-performance-analysis.txt");
    }









    private void exportDetailedAnalysis(List<PerformanceData> allData) {
        try (FileWriter writer = new FileWriter("dmn-performance-analysis.txt")) {
            writeComprehensiveReport(writer, allData);
        } catch (IOException e) {
            System.err.println("Error writing comprehensive analysis report: " + e.getMessage());
        }
    }

    private void writeComprehensiveReport(FileWriter writer, List<PerformanceData> allData) throws IOException {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        writer.write("=== COMPREHENSIVE DMN PERFORMANCE ANALYSIS REPORT ===\n");
        writer.write("Generated: " + timestamp + "\n");
        writer.write("Total data points: " + allData.size() + "\n\n");
        
        writeConfigurationMapping(writer);
        writeIndividualModelAnalysis(writer, allData);
        writeOverallOptimizationImpact(writer, allData);
        writeConfigurationRankings(writer, allData);
        writeEvidenceBasedRecommendations(writer, allData);
    }

    private void writeConfigurationMapping(FileWriter writer) throws IOException {
        writer.write("CONFIGURATION MAPPING:\n");
        writer.write("=======================\n");
        for (int i = 0; i < 16; i++) {
            boolean alpha = (i & 1) != 0;
            boolean typeCheck = (i & 2) != 0;
            boolean lenient = (i & 4) != 0;
            boolean feel = (i & 8) != 0;
            
            writer.write(String.format("Config %2d: Alpha=%s, TypeCheck=%s, Lenient=%s, FEEL=%s\n",
                i, alpha ? "ON" : "OFF", typeCheck ? "ON" : "OFF", 
                lenient ? "ON" : "OFF", feel ? "ON" : "OFF"));
        }
    }

    private void writeIndividualModelAnalysis(FileWriter writer, List<PerformanceData> allData) throws IOException {
        Map<String, List<PerformanceData>> dataByFile = allData.stream()
            .collect(Collectors.groupingBy(d -> d.dmnFile));

        writer.write("\n\nINDIVIDUAL DMN MODEL ANALYSIS:\n");
        writer.write("===============================\n");
        
        for (String dmnFile : dataByFile.keySet()) {
            List<PerformanceData> fileData = dataByFile.get(dmnFile);
            if (fileData.isEmpty()) continue;
            
            writer.write("\n" + dmnFile.toUpperCase() + ":\n");
            writer.write("-".repeat(dmnFile.length() + 1) + "\n");
            
            Optional<PerformanceData> baseline = fileData.stream()
                .filter(d -> d.configId == 0)
                .findFirst();
            
            if (baseline.isPresent()) {
                PerformanceData baselineData = baseline.get();
                
                writer.write("BASELINE PERFORMANCE (Config 0):\n");
                writer.write(String.format("  Build Time: avg=%.2fms, median=%.2fms, p95=%.2fms, p99=%.2fms\n", 
                    baselineData.avgBuildTimeMs, baselineData.medianBuildTimeMs, 
                    baselineData.p95BuildTimeMs, baselineData.p99BuildTimeMs));
                writer.write(String.format("  Eval Time:  avg=%.2fμs, median=%.2fμs, p95=%.2fμs, p99=%.2fμs\n", 
                    baselineData.avgEvalTimeMicros, baselineData.medianEvalTimeMicros, 
                    baselineData.p95EvalTimeMicros, baselineData.p99EvalTimeMicros));
                
                PerformanceData best = fileData.stream()
                    .filter(d -> d.avgEvalTimeMicros > 0)
                    .min(Comparator.comparing(d -> d.avgEvalTimeMicros))
                    .orElse(null);
                
                PerformanceData worst = fileData.stream()
                    .filter(d -> d.avgEvalTimeMicros > 0)
                    .max(Comparator.comparing(d -> d.avgEvalTimeMicros))
                    .orElse(null);
                
                if (best != null && worst != null) {
                    double maxImprovement = ((baselineData.avgEvalTimeMicros - best.avgEvalTimeMicros) / baselineData.avgEvalTimeMicros) * 100;
                    double worstDegradation = ((worst.avgEvalTimeMicros - baselineData.avgEvalTimeMicros) / baselineData.avgEvalTimeMicros) * 100;
                    
                    writer.write("\nBEST PERFORMING CONFIG:\n");
                    writer.write(String.format("  Config %d: %.1f%% improvement from baseline\n", best.configId, maxImprovement));
                    writer.write(String.format("  Build Time: avg=%.2fms, median=%.2fms, p95=%.2fms, p99=%.2fms\n", 
                        best.avgBuildTimeMs, best.medianBuildTimeMs, best.p95BuildTimeMs, best.p99BuildTimeMs));
                    writer.write(String.format("  Eval Time:  avg=%.2fμs, median=%.2fμs, p95=%.2fμs, p99=%.2fμs\n", 
                        best.avgEvalTimeMicros, best.medianEvalTimeMicros, best.p95EvalTimeMicros, best.p99EvalTimeMicros));
                    
                    writer.write("\nWORST PERFORMING CONFIG:\n");
                    writer.write(String.format("  Config %d: %.1f%% change from baseline\n", worst.configId, worstDegradation));
                    writer.write(String.format("  Build Time: avg=%.2fms, median=%.2fms, p95=%.2fms, p99=%.2fms\n", 
                        worst.avgBuildTimeMs, worst.medianBuildTimeMs, worst.p95BuildTimeMs, worst.p99BuildTimeMs));
                    writer.write(String.format("  Eval Time:  avg=%.2fμs, median=%.2fμs, p95=%.2fμs, p99=%.2fμs\n", 
                        worst.avgEvalTimeMicros, worst.medianEvalTimeMicros, worst.p95EvalTimeMicros, worst.p99EvalTimeMicros));
                }
            }
        }
    }

    private void writeOverallOptimizationImpact(FileWriter writer, List<PerformanceData> allData) throws IOException {
        writer.write("\n\nOVERALL OPTIMIZATION IMPACT:\n");
        writer.write("=============================\n");
        
        Map<String, List<PerformanceData>> dataByFile = allData.stream()
            .collect(Collectors.groupingBy(d -> d.dmnFile));
        
        String[] optNames = {"Alpha Network", "Runtime Type Check", "Lenient Mode", "FEEL Compilation"};
        double[] totalImpacts = new double[4];
        int[] validFiles = new int[4];
        
        for (List<PerformanceData> fileData : dataByFile.values()) {
            for (int opt = 0; opt < 4; opt++) {
                final int currentOpt = opt;
                List<PerformanceData> withOpt = fileData.stream()
                    .filter(d -> (d.configId & (1 << currentOpt)) != 0)
                    .collect(Collectors.toList());
                List<PerformanceData> withoutOpt = fileData.stream()
                    .filter(d -> (d.configId & (1 << currentOpt)) == 0)
                    .collect(Collectors.toList());
                
                if (!withOpt.isEmpty() && !withoutOpt.isEmpty()) {
                    double avgWith = withOpt.stream().mapToDouble(d -> d.avgEvalTimeMicros).average().orElse(0);
                    double avgWithout = withoutOpt.stream().mapToDouble(d -> d.avgEvalTimeMicros).average().orElse(0);
                    double impact = ((avgWithout - avgWith) / avgWithout) * 100;
                    
                    totalImpacts[currentOpt] += impact;
                    validFiles[currentOpt]++;
                }
            }
        }
        
        writer.write("Average optimization impact across all DMN files:\n");
        for (int opt = 0; opt < 4; opt++) {
            if (validFiles[opt] > 0) {
                double avgImpact = totalImpacts[opt] / validFiles[opt];
                writer.write(String.format("  %s: %.1f%% %s\n", 
                    optNames[opt], Math.abs(avgImpact), 
                    avgImpact > 0 ? "improvement" : "degradation"));
            }
        }
    }

    private void writeConfigurationRankings(FileWriter writer, List<PerformanceData> allData) throws IOException {
        writer.write("\n\nCONFIGURATION RANKINGS:\n");
        writer.write("=======================\n");
        
        Map<Integer, List<PerformanceData>> dataByConfig = allData.stream()
            .collect(Collectors.groupingBy(d -> d.configId));
        
        List<ConfigPerformance> rankings = new ArrayList<>();
        
        for (int configId : dataByConfig.keySet()) {
            List<PerformanceData> configData = dataByConfig.get(configId);
            
            double avgEvalTime = configData.stream()
                .filter(d -> d.avgEvalTimeMicros > 0)
                .mapToDouble(d -> d.avgEvalTimeMicros)
                .average()
                .orElse(Double.MAX_VALUE);
            
            double medianEvalTime = configData.stream()
                .filter(d -> d.medianEvalTimeMicros > 0)
                .mapToDouble(d -> d.medianEvalTimeMicros)
                .average()
                .orElse(Double.MAX_VALUE);
            
            double p95EvalTime = configData.stream()
                .filter(d -> d.p95EvalTimeMicros > 0)
                .mapToDouble(d -> d.p95EvalTimeMicros)
                .average()
                .orElse(Double.MAX_VALUE);
            
            double avgBuildTime = configData.stream()
                .filter(d -> d.avgBuildTimeMs > 0)
                .mapToDouble(d -> d.avgBuildTimeMs)
                .average()
                .orElse(Double.MAX_VALUE);
            
            double medianBuildTime = configData.stream()
                .filter(d -> d.medianBuildTimeMs > 0)
                .mapToDouble(d -> d.medianBuildTimeMs)
                .average()
                .orElse(Double.MAX_VALUE);
            
            double p95BuildTime = configData.stream()
                .filter(d -> d.p95BuildTimeMs > 0)
                .mapToDouble(d -> d.p95BuildTimeMs)
                .average()
                .orElse(Double.MAX_VALUE);
            
            double avgSuccessRate = configData.stream()
                .mapToDouble(d -> d.successRate)
                .average()
                .orElse(0);
            
            if (avgEvalTime < Double.MAX_VALUE) {
                rankings.add(new ConfigPerformance(configId, avgEvalTime, medianEvalTime, p95EvalTime,
                    avgBuildTime, medianBuildTime, p95BuildTime, avgSuccessRate, configData.size()));
            }
        }
        
        rankings.sort(Comparator.comparing(cp -> cp.avgEvalTime));
        
        writer.write("Top 10 Configurations (ranked by average evaluation time):\n\n");
        
        for (int i = 0; i < Math.min(10, rankings.size()); i++) {
            ConfigPerformance cp = rankings.get(i);
            boolean alpha = (cp.configId & 1) != 0;
            boolean typeCheck = (cp.configId & 2) != 0;
            boolean lenient = (cp.configId & 4) != 0;
            boolean feel = (cp.configId & 8) != 0;
            
            writer.write(String.format("RANK %d - CONFIG %d: Alpha=%s, TypeCheck=%s, Lenient=%s, FEEL=%s\n",
                i + 1, cp.configId,
                alpha ? "ON" : "OFF", typeCheck ? "ON" : "OFF", 
                lenient ? "ON" : "OFF", feel ? "ON" : "OFF"));
            writer.write(String.format("  Build Time: avg=%.2fms, median=%.2fms, p95=%.2fms\n",
                cp.avgBuildTime, cp.medianBuildTime, cp.p95BuildTime));
            writer.write(String.format("  Eval Time:  avg=%.2fμs, median=%.2fμs, p95=%.2fμs\n",
                cp.avgEvalTime, cp.medianEvalTime, cp.p95EvalTime));
            writer.write(String.format("  Success Rate: %.1f%% (%d data points)\n\n",
                cp.avgSuccessRate, cp.dataPoints));
        }
    }

    private void writeEvidenceBasedRecommendations(FileWriter writer, List<PerformanceData> allData) throws IOException {
        writer.write("\n\nEVIDENCE-BASED RECOMMENDATIONS:\n");
        writer.write("================================\n");
        
        Map<Integer, List<PerformanceData>> dataByConfig = allData.stream()
            .collect(Collectors.groupingBy(d -> d.configId));
        
        List<ConfigPerformance> rankings = new ArrayList<>();
        
        for (int configId : dataByConfig.keySet()) {
            List<PerformanceData> configData = dataByConfig.get(configId);
            
            double avgEvalTime = configData.stream()
                .filter(d -> d.avgEvalTimeMicros > 0)
                .mapToDouble(d -> d.avgEvalTimeMicros)
                .average()
                .orElse(Double.MAX_VALUE);
            
            double medianEvalTime = configData.stream()
                .filter(d -> d.medianEvalTimeMicros > 0)
                .mapToDouble(d -> d.medianEvalTimeMicros)
                .average()
                .orElse(Double.MAX_VALUE);
            
            double p95EvalTime = configData.stream()
                .filter(d -> d.p95EvalTimeMicros > 0)
                .mapToDouble(d -> d.p95EvalTimeMicros)
                .average()
                .orElse(Double.MAX_VALUE);
            
            double avgBuildTime = configData.stream()
                .filter(d -> d.avgBuildTimeMs > 0)
                .mapToDouble(d -> d.avgBuildTimeMs)
                .average()
                .orElse(Double.MAX_VALUE);
            
            double medianBuildTime = configData.stream()
                .filter(d -> d.medianBuildTimeMs > 0)
                .mapToDouble(d -> d.medianBuildTimeMs)
                .average()
                .orElse(Double.MAX_VALUE);
            
            double p95BuildTime = configData.stream()
                .filter(d -> d.p95BuildTimeMs > 0)
                .mapToDouble(d -> d.p95BuildTimeMs)
                .average()
                .orElse(Double.MAX_VALUE);
            
            double avgSuccessRate = configData.stream()
                .mapToDouble(d -> d.successRate)
                .average()
                .orElse(0);
            
            if (avgEvalTime < Double.MAX_VALUE) {
                rankings.add(new ConfigPerformance(configId, avgEvalTime, medianEvalTime, p95EvalTime,
                    avgBuildTime, medianBuildTime, p95BuildTime, avgSuccessRate, configData.size()));
            }
        }
        
        rankings.sort(Comparator.comparing(cp -> cp.avgEvalTime));
        
        if (!rankings.isEmpty()) {
            List<ConfigPerformance> reliableConfigs = rankings.stream()
                .filter(cp -> cp.avgSuccessRate >= 100.0)
                .limit(3)
                .collect(Collectors.toList());
            
            writer.write("TOP PRODUCTION-READY CONFIGURATIONS (100% success rate):\n\n");
            for (int i = 0; i < reliableConfigs.size(); i++) {
                ConfigPerformance cp = reliableConfigs.get(i);
                boolean alpha = (cp.configId & 1) != 0;
                boolean typeCheck = (cp.configId & 2) != 0;
                boolean lenient = (cp.configId & 4) != 0;
                boolean feel = (cp.configId & 8) != 0;
                
                writer.write(String.format("%d. CONFIG %d: Alpha=%s, TypeCheck=%s, Lenient=%s, FEEL=%s\n",
                    i + 1, cp.configId,
                    alpha ? "ON" : "OFF", typeCheck ? "ON" : "OFF",
                    lenient ? "ON" : "OFF", feel ? "ON" : "OFF"));
                writer.write(String.format("   Build Time: avg=%.2fms, median=%.2fms, p95=%.2fms\n",
                    cp.avgBuildTime, cp.medianBuildTime, cp.p95BuildTime));
                writer.write(String.format("   Eval Time:  avg=%.2fμs, median=%.2fμs, p95=%.2fμs\n",
                    cp.avgEvalTime, cp.medianEvalTime, cp.p95EvalTime));
                writer.write(String.format("   Success Rate: %.1f%%\n\n", cp.avgSuccessRate));
            }
        }
        
        writer.write("\nGENERAL GUIDELINES:\n");
        writer.write("• Test with your specific DMN models and data patterns\n");
        writer.write("• Start with baseline (Config 0) to establish performance expectations\n");
        writer.write("• Prioritize configurations with 100% success rates for production\n");
        writer.write("• Monitor build time vs evaluation time trade-offs\n");
        writer.write("• Validate performance improvements under realistic load conditions\n");
    }

    private static List<PerformanceData> loadDataFromCsv(String csvFilePath) throws IOException {
        List<PerformanceData> data = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFilePath))) {
            String headerLine = reader.readLine(); // Skip header
            if (headerLine == null) return data;
            
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 18) {
                    try {
                        PerformanceData pd = new PerformanceData(
                            parts[1], // dmnFile
                            Integer.parseInt(parts[2]), // configId
                            Boolean.parseBoolean(parts[3]), // alphaNetwork
                            Boolean.parseBoolean(parts[4]), // runtimeTypeCheck
                            Boolean.parseBoolean(parts[5]), // lenientMode
                            Boolean.parseBoolean(parts[6]), // feelCompilation
                            Double.parseDouble(parts[7]), // avgBuildTimeMs
                            Double.parseDouble(parts[8]), // medianBuildTimeMs
                            Double.parseDouble(parts[9]), // p95BuildTimeMs
                            Double.parseDouble(parts[10]), // p99BuildTimeMs
                            Double.parseDouble(parts[11]), // avgEvalTimeMicros
                            Double.parseDouble(parts[12]), // medianEvalTimeMicros
                            Double.parseDouble(parts[13]), // p95EvalTimeMicros
                            Double.parseDouble(parts[14]), // p99EvalTimeMicros
                            Double.parseDouble(parts[17]) // successRate
                        );
                        data.add(pd);
                    } catch (NumberFormatException e) {
                        System.err.println("Skipping invalid line: " + line);
                    }
                }
            }
        }
        
        return data;
    }

    // Data classes
    public static class PerformanceData {
        public final String dmnFile;
        public final int configId;
        public final boolean alphaNetwork;
        public final boolean runtimeTypeCheck;
        public final boolean lenientMode;
        public final boolean feelCompilation;
        public final double avgBuildTimeMs;
        public final double medianBuildTimeMs;
        public final double p95BuildTimeMs;
        public final double p99BuildTimeMs;
        public final double avgEvalTimeMicros;
        public final double medianEvalTimeMicros;
        public final double p95EvalTimeMicros;
        public final double p99EvalTimeMicros;
        public final double successRate;

        public PerformanceData(String dmnFile, int configId, boolean alphaNetwork, boolean runtimeTypeCheck,
                             boolean lenientMode, boolean feelCompilation, 
                             double avgBuildTimeMs, double medianBuildTimeMs, double p95BuildTimeMs, double p99BuildTimeMs,
                             double avgEvalTimeMicros, double medianEvalTimeMicros, double p95EvalTimeMicros, double p99EvalTimeMicros,
                             double successRate) {
            this.dmnFile = dmnFile;
            this.configId = configId;
            this.alphaNetwork = alphaNetwork;
            this.runtimeTypeCheck = runtimeTypeCheck;
            this.lenientMode = lenientMode;
            this.feelCompilation = feelCompilation;
            this.avgBuildTimeMs = avgBuildTimeMs;
            this.medianBuildTimeMs = medianBuildTimeMs;
            this.p95BuildTimeMs = p95BuildTimeMs;
            this.p99BuildTimeMs = p99BuildTimeMs;
            this.avgEvalTimeMicros = avgEvalTimeMicros;
            this.medianEvalTimeMicros = medianEvalTimeMicros;
            this.p95EvalTimeMicros = p95EvalTimeMicros;
            this.p99EvalTimeMicros = p99EvalTimeMicros;
            this.successRate = successRate;
        }
    }

    public static class ConfigPerformance {
        public final int configId;
        public final double avgEvalTime;
        public final double medianEvalTime;
        public final double p95EvalTime;
        public final double avgBuildTime;
        public final double medianBuildTime;
        public final double p95BuildTime;
        public final double avgSuccessRate;
        public final int dataPoints;

        public ConfigPerformance(int configId, double avgEvalTime, double medianEvalTime, double p95EvalTime,
                               double avgBuildTime, double medianBuildTime, double p95BuildTime,
                               double avgSuccessRate, int dataPoints) {
            this.configId = configId;
            this.avgEvalTime = avgEvalTime;
            this.medianEvalTime = medianEvalTime;
            this.p95EvalTime = p95EvalTime;
            this.avgBuildTime = avgBuildTime;
            this.medianBuildTime = medianBuildTime;
            this.p95BuildTime = p95BuildTime;
            this.avgSuccessRate = avgSuccessRate;
            this.dataPoints = dataPoints;
        }
    }
}
