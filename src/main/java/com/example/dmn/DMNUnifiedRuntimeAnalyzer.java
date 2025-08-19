package com.example.dmn;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.dmn.api.core.DMNContext;
import org.kie.dmn.api.core.DMNModel;
import org.kie.dmn.api.core.DMNResult;
import org.kie.dmn.api.core.DMNRuntime;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Comprehensive performance analyzer for unified DMN runtime containing all DMN models.
 * This class builds a single runtime with all DMN models and measures:
 * - Build time for unified runtime vs individual runtimes
 * - Memory usage comparison
 * - Cross-model evaluation performance
 * - Runtime optimization effectiveness at scale
 */
public class DMNUnifiedRuntimeAnalyzer {

    private static final int WARMUP_ITERATIONS = 100;
    private static final int PERFORMANCE_ITERATIONS = 1000;
    private static final int BUILD_TIME_ITERATIONS = 10;
    private static final int MEMORY_SAMPLE_ITERATIONS = 5;
    
    private static final String[] DMN_FILES = {
        "loan-approval.dmn",
        "insurance-risk-assessment.dmn",
        "supply-chain-optimization.dmn",
        "financial-portfolio-analysis.dmn",
        "healthcare-treatment-protocol.dmn"
    };

    private static final String[] SERVICE_IDS = {
        "Loan Approval Service",
        "Insurance Risk Assessment Service", 
        "Supply Chain Optimization Service",
        "Portfolio Analysis Service",
        "Healthcare Treatment Protocol Service"
    };

    public static void main(String[] args) {
        DMNUnifiedRuntimeAnalyzer analyzer = new DMNUnifiedRuntimeAnalyzer();
        
        System.out.println("=== DMN Unified Runtime Performance Analysis ===");
        System.out.println("Comparing unified runtime (all DMNs) vs individual runtimes");
        System.out.println("Testing " + DMN_FILES.length + " DMN models with " + PERFORMANCE_ITERATIONS + " iterations each");
        System.out.println();
        
        List<UnifiedRuntimeResult> allResults = new ArrayList<>();
        
        // Test all 16 optimization configurations
        for (int configId = 0; configId < 16; configId++) {
            DMNOptimizationConfig config = createOptimizationConfig(configId);
            System.out.println("Testing Configuration " + configId + ": " + config);
            
            UnifiedRuntimeResult result = analyzer.measureUnifiedRuntimePerformance(config, configId);
            allResults.add(result);
            
            System.out.printf("  Unified Build: %.2fms (individual sum: %.2fms, overhead: %.1f%%)%n",
                result.unifiedBuildTimeMs, result.individualBuildTimeSumMs, 
                ((result.unifiedBuildTimeMs - result.individualBuildTimeSumMs) / result.individualBuildTimeSumMs) * 100);
            System.out.printf("  Memory: Unified=%.2fMB, Individual=%.2fMB, Savings=%.1f%%\n",
                result.unifiedMemoryUsageMB, result.individualMemoryUsageMB,
                ((result.individualMemoryUsageMB - result.unifiedMemoryUsageMB) / result.individualMemoryUsageMB) * 100);
            System.out.printf("  Avg Eval: Unified=%.2fμs, Individual=%.2fμs\n",
                result.avgUnifiedEvaluationTimeMicros, result.avgIndividualEvaluationTimeMicros);
            System.out.println();
        }
        
        // Generate comprehensive reports
        analyzer.generateUnifiedRuntimeReport(allResults);
        analyzer.exportUnifiedResultsToCsv(allResults);
        
        System.out.println("=== Unified Runtime Analysis Complete ===");
        System.out.println("Results exported to dmn-unified-runtime-results.csv");
        System.out.println("Detailed analysis written to dmn-unified-runtime-analysis.txt");
    }

    public UnifiedRuntimeResult measureUnifiedRuntimePerformance(DMNOptimizationConfig config, int configId) {
        try {
            // Measure unified runtime build time
            List<Long> unifiedBuildTimes = new ArrayList<>();
            List<Long> individualBuildTimes = new ArrayList<>();
            DMNRuntime unifiedRuntime = null;
            
            for (int i = 0; i < BUILD_TIME_ITERATIONS; i++) {
                // Build unified runtime
                long unifiedStartTime = System.nanoTime();
                unifiedRuntime = createUnifiedDMNRuntime(config);
                long unifiedEndTime = System.nanoTime();
                unifiedBuildTimes.add(unifiedEndTime - unifiedStartTime);
                
                // Build individual runtimes for comparison
                long individualStartTime = System.nanoTime();
                List<DMNRuntime> individualRuntimes = createIndividualDMNRuntimes(config);
                long individualEndTime = System.nanoTime();
                individualBuildTimes.add(individualEndTime - individualStartTime);
                
                // Clean up individual runtimes
                individualRuntimes.clear();
            }
            
            double avgUnifiedBuildTimeMs = unifiedBuildTimes.stream()
                .mapToLong(Long::longValue).average().orElse(0.0) / 1_000_000.0;
            double avgIndividualBuildTimeMs = individualBuildTimes.stream()
                .mapToLong(Long::longValue).average().orElse(0.0) / 1_000_000.0;
            
            // Measure memory usage
            MemoryMetrics unifiedMemory = measureUnifiedRuntimeMemory(config);
            MemoryMetrics individualMemory = measureIndividualRuntimesMemory(config);
            
            // Measure evaluation performance
            EvaluationMetrics unifiedEvalMetrics = measureUnifiedEvaluationPerformance(unifiedRuntime);
            EvaluationMetrics individualEvalMetrics = measureIndividualEvaluationPerformance(config);
            
            // Measure cross-model evaluation scenarios
            CrossModelMetrics crossModelMetrics = measureCrossModelEvaluationPerformance(unifiedRuntime);
            
            return new UnifiedRuntimeResult(
                configId, config,
                avgUnifiedBuildTimeMs, avgIndividualBuildTimeMs,
                unifiedMemory.totalMemoryMB, individualMemory.totalMemoryMB,
                unifiedEvalMetrics.avgEvaluationTimeMicros, individualEvalMetrics.avgEvaluationTimeMicros,
                unifiedEvalMetrics.medianEvaluationTimeMicros, individualEvalMetrics.medianEvaluationTimeMicros,
                unifiedEvalMetrics.p95EvaluationTimeMicros, individualEvalMetrics.p95EvaluationTimeMicros,
                unifiedEvalMetrics.successRate, individualEvalMetrics.successRate,
                crossModelMetrics.avgCrossModelEvaluationTimeMicros,
                crossModelMetrics.crossModelSuccessRate,
                unifiedRuntime.getModels().size(),
                calculateRuntimeEfficiencyScore(unifiedEvalMetrics, individualEvalMetrics, unifiedMemory, individualMemory)
            );

        } catch (Exception e) {
            System.err.println("Error measuring unified runtime performance for config " + configId + ": " + e.getMessage());
            return new UnifiedRuntimeResult(configId, config, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0, 0.0);
        }
    }

    private DMNRuntime createUnifiedDMNRuntime(DMNOptimizationConfig config) {
        return createDMNRuntimeWithFiles(Arrays.asList(DMN_FILES), config);
    }

    private List<DMNRuntime> createIndividualDMNRuntimes(DMNOptimizationConfig config) {
        List<DMNRuntime> runtimes = new ArrayList<>();
        for (String dmnFile : DMN_FILES) {
            runtimes.add(createDMNRuntimeWithFiles(Arrays.asList(dmnFile), config));
        }
        return runtimes;
    }

    private DMNRuntime createDMNRuntimeWithFiles(List<String> dmnFiles, DMNOptimizationConfig config) {
        try {
            // Set system properties for optimization configuration
            Properties originalProps = new Properties();
            
            try {
                // Store and set optimization properties
                setOptimizationProperties(config, originalProps);
                
                KieServices kieServices = KieServices.Factory.get();
                KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
                
                // Load all specified DMN files
                for (String dmnFile : dmnFiles) {
                    InputStream dmnStream = getClass().getClassLoader().getResourceAsStream(dmnFile);
                    if (dmnStream == null) {
                        throw new RuntimeException("Could not find " + dmnFile + " in resources");
                    }
                    
                    kieFileSystem.write("src/main/resources/" + dmnFile, 
                        kieServices.getResources().newInputStreamResource(dmnStream));
                }
                
                KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
                kieBuilder.buildAll();
                
                if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
                    System.err.println("Build errors for DMN files " + dmnFiles + ":");
                    kieBuilder.getResults().getMessages(Message.Level.ERROR)
                        .forEach(message -> System.err.println("  " + message.getText()));
                    throw new RuntimeException("Failed to build DMN models: " + dmnFiles);
                }
                
                KieContainer kieContainer = kieServices.newKieContainer(
                    kieBuilder.getKieModule().getReleaseId());
                
                return kieContainer.newKieSession().getKieRuntime(DMNRuntime.class);
                
            } finally {
                // Restore original system properties
                restoreOriginalProperties(originalProps);
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create DMN runtime for files " + dmnFiles + " with config " + config, e);
        }
    }

    private MemoryMetrics measureUnifiedRuntimeMemory(DMNOptimizationConfig config) {
        List<Double> memoryMeasurements = new ArrayList<>();
        
        for (int i = 0; i < MEMORY_SAMPLE_ITERATIONS; i++) {
            System.gc(); // Suggest garbage collection
            Thread.yield(); // Allow GC to run
            
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage beforeMemory = memoryBean.getHeapMemoryUsage();
            
            DMNRuntime runtime = createUnifiedDMNRuntime(config);
            
            MemoryUsage afterMemory = memoryBean.getHeapMemoryUsage();
            double memoryUsedMB = (afterMemory.getUsed() - beforeMemory.getUsed()) / (1024.0 * 1024.0);
            memoryMeasurements.add(Math.max(0, memoryUsedMB)); // Ensure non-negative
            
            // Allow GC by clearing reference
        }
        
        double avgMemoryMB = memoryMeasurements.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return new MemoryMetrics(avgMemoryMB);
    }

    private MemoryMetrics measureIndividualRuntimesMemory(DMNOptimizationConfig config) {
        List<Double> totalMemoryMeasurements = new ArrayList<>();
        
        for (int i = 0; i < MEMORY_SAMPLE_ITERATIONS; i++) {
            System.gc();
            Thread.yield();
            
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage beforeMemory = memoryBean.getHeapMemoryUsage();
            
            List<DMNRuntime> runtimes = createIndividualDMNRuntimes(config);
            
            MemoryUsage afterMemory = memoryBean.getHeapMemoryUsage();
            double memoryUsedMB = (afterMemory.getUsed() - beforeMemory.getUsed()) / (1024.0 * 1024.0);
            totalMemoryMeasurements.add(Math.max(0, memoryUsedMB));
            
            runtimes.clear(); // Allow GC
        }
        
        double avgMemoryMB = totalMemoryMeasurements.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return new MemoryMetrics(avgMemoryMB);
    }

    private EvaluationMetrics measureUnifiedEvaluationPerformance(DMNRuntime unifiedRuntime) {
        List<Long> allEvaluationTimes = new ArrayList<>();
        int totalSuccessfulEvaluations = 0;
        int totalEvaluations = 0;
        
        // Test each DMN model in the unified runtime
        for (int modelIndex = 0; modelIndex < DMN_FILES.length; modelIndex++) {
            String dmnFile = DMN_FILES[modelIndex];
            String serviceId = SERVICE_IDS[modelIndex];
            DMNModel model = findModelByFileName(unifiedRuntime, dmnFile);
            
            if (model == null) {
                System.err.println("Could not find model for " + dmnFile + " in unified runtime");
                continue;
            }
            
            DMNContext testContext = createTestContext(unifiedRuntime, dmnFile);
            
            // Warmup
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                try {
                    unifiedRuntime.evaluateDecisionService(model, testContext, serviceId);
                } catch (Exception e) {
                    // Continue warmup
                }
            }
            
            // Performance measurement
            for (int i = 0; i < PERFORMANCE_ITERATIONS; i++) {
                long startTime = System.nanoTime();
                try {
                    DMNResult result = unifiedRuntime.evaluateDecisionService(model, testContext, serviceId);
                    if (!result.hasErrors()) {
                        totalSuccessfulEvaluations++;
                    }
                } catch (Exception e) {
                    // Count failed evaluations
                }
                long endTime = System.nanoTime();
                allEvaluationTimes.add(endTime - startTime);
                totalEvaluations++;
            }
        }
        
        return calculateEvaluationMetrics(allEvaluationTimes, totalSuccessfulEvaluations, totalEvaluations);
    }

    private EvaluationMetrics measureIndividualEvaluationPerformance(DMNOptimizationConfig config) {
        List<Long> allEvaluationTimes = new ArrayList<>();
        int totalSuccessfulEvaluations = 0;
        int totalEvaluations = 0;
        
        for (int modelIndex = 0; modelIndex < DMN_FILES.length; modelIndex++) {
            String dmnFile = DMN_FILES[modelIndex];
            String serviceId = SERVICE_IDS[modelIndex];
            
            DMNRuntime runtime = createDMNRuntimeWithFiles(Arrays.asList(dmnFile), config);
            DMNContext testContext = createTestContext(runtime, dmnFile);
            
            // Warmup
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                try {
                    runtime.evaluateDecisionService(runtime.getModels().get(0), testContext, serviceId);
                } catch (Exception e) {
                    // Continue warmup
                }
            }
            
            // Performance measurement
            for (int i = 0; i < PERFORMANCE_ITERATIONS; i++) {
                long startTime = System.nanoTime();
                try {
                    DMNResult result = runtime.evaluateDecisionService(runtime.getModels().get(0), testContext, serviceId);
                    if (!result.hasErrors()) {
                        totalSuccessfulEvaluations++;
                    }
                } catch (Exception e) {
                    // Count failed evaluations
                }
                long endTime = System.nanoTime();
                allEvaluationTimes.add(endTime - startTime);
                totalEvaluations++;
            }
        }
        
        return calculateEvaluationMetrics(allEvaluationTimes, totalSuccessfulEvaluations, totalEvaluations);
    }

    private CrossModelMetrics measureCrossModelEvaluationPerformance(DMNRuntime unifiedRuntime) {
        List<Long> crossModelEvaluationTimes = new ArrayList<>();
        int successfulCrossModelEvaluations = 0;
        int totalCrossModelEvaluations = 0;
        
        // Create test scenarios that evaluate multiple models in sequence
        for (int scenario = 0; scenario < 100; scenario++) { // 100 cross-model scenarios
            long scenarioStartTime = System.nanoTime();
            boolean scenarioSuccess = true;
            
            try {
                // Evaluate loan approval first
                DMNModel loanModel = findModelByFileName(unifiedRuntime, "loan-approval.dmn");
                if (loanModel != null) {
                    DMNContext loanContext = createTestContext(unifiedRuntime, "loan-approval.dmn");
                    DMNResult loanResult = unifiedRuntime.evaluateDecisionService(loanModel, loanContext, "Loan Approval Service");
                    if (loanResult.hasErrors()) scenarioSuccess = false;
                }
                
                // Then evaluate insurance based on loan result
                DMNModel insuranceModel = findModelByFileName(unifiedRuntime, "insurance-risk-assessment.dmn");
                if (insuranceModel != null) {
                    DMNContext insuranceContext = createTestContext(unifiedRuntime, "insurance-risk-assessment.dmn");
                    DMNResult insuranceResult = unifiedRuntime.evaluateDecisionService(insuranceModel, insuranceContext, "Insurance Risk Assessment Service");
                    if (insuranceResult.hasErrors()) scenarioSuccess = false;
                }
                
                // Finally evaluate portfolio optimization
                DMNModel portfolioModel = findModelByFileName(unifiedRuntime, "financial-portfolio-analysis.dmn");
                if (portfolioModel != null) {
                    DMNContext portfolioContext = createTestContext(unifiedRuntime, "financial-portfolio-analysis.dmn");
                    DMNResult portfolioResult = unifiedRuntime.evaluateDecisionService(portfolioModel, portfolioContext, "Portfolio Analysis Service");
                    if (portfolioResult.hasErrors()) scenarioSuccess = false;
                }
                
            } catch (Exception e) {
                scenarioSuccess = false;
            }
            
            long scenarioEndTime = System.nanoTime();
            crossModelEvaluationTimes.add(scenarioEndTime - scenarioStartTime);
            
            if (scenarioSuccess) {
                successfulCrossModelEvaluations++;
            }
            totalCrossModelEvaluations++;
        }
        
        double avgCrossModelEvaluationTimeMicros = crossModelEvaluationTimes.stream()
            .mapToLong(Long::longValue).average().orElse(0.0) / 1000.0;
        double crossModelSuccessRate = (successfulCrossModelEvaluations * 100.0) / totalCrossModelEvaluations;
        
        return new CrossModelMetrics(avgCrossModelEvaluationTimeMicros, crossModelSuccessRate);
    }

    private DMNModel findModelByFileName(DMNRuntime runtime, String fileName) {
        return runtime.getModels().stream()
            .filter(model -> model.getResource() != null && 
                    model.getResource().getSourcePath() != null &&
                    model.getResource().getSourcePath().contains(fileName))
            .findFirst()
            .orElse(null);
    }

    private EvaluationMetrics calculateEvaluationMetrics(List<Long> evaluationTimes, int successfulEvaluations, int totalEvaluations) {
        if (evaluationTimes.isEmpty()) {
            return new EvaluationMetrics(0, 0, 0, 0);
        }
        
        double avgEvaluationTimeMicros = evaluationTimes.stream()
            .mapToLong(Long::longValue).average().orElse(0.0) / 1000.0;
        double medianEvaluationTimeMicros = calculateMedian(evaluationTimes) / 1000.0;
        double p95EvaluationTimeMicros = calculatePercentile(evaluationTimes, 0.95) / 1000.0;
        double successRate = (successfulEvaluations * 100.0) / totalEvaluations;
        
        return new EvaluationMetrics(avgEvaluationTimeMicros, medianEvaluationTimeMicros, p95EvaluationTimeMicros, successRate);
    }

    private double calculateRuntimeEfficiencyScore(EvaluationMetrics unified, EvaluationMetrics individual, 
                                                 MemoryMetrics unifiedMem, MemoryMetrics individualMem) {
        // Calculate efficiency score based on evaluation time and memory usage
        double evalTimeRatio = individual.avgEvaluationTimeMicros / Math.max(unified.avgEvaluationTimeMicros, 0.1);
        double memoryRatio = individualMem.totalMemoryMB / Math.max(unifiedMem.totalMemoryMB, 0.1);
        
        // Weighted score: 70% evaluation performance, 30% memory efficiency
        return (evalTimeRatio * 0.7) + (memoryRatio * 0.3);
    }

    private void setOptimizationProperties(DMNOptimizationConfig config, Properties originalProps) {
        String[] propNames = {
            "org.kie.dmn.alphanetwork.enabled",
            "org.kie.dmn.runtime.typecheck", 
            "org.kie.dmn.strict",
            "org.kie.dmn.feel.compilation"
        };
        
        boolean[] propValues = {
            config.alphaNetworkEnabled,
            config.runtimeTypeCheckingEnabled,
            !config.lenientModeEnabled, // strict is opposite of lenient
            config.feelCompilationEnabled
        };
        
        for (int i = 0; i < propNames.length; i++) {
            originalProps.setProperty(propNames[i], System.getProperty(propNames[i], ""));
            System.setProperty(propNames[i], String.valueOf(propValues[i]));
        }
    }

    private void restoreOriginalProperties(Properties originalProps) {
        for (String prop : originalProps.stringPropertyNames()) {
            String originalValue = originalProps.getProperty(prop);
            if (originalValue.isEmpty()) {
                System.clearProperty(prop);
            } else {
                System.setProperty(prop, originalValue);
            }
        }
    }

    private static DMNOptimizationConfig createOptimizationConfig(int configId) {
        boolean alphaNetwork = (configId & 1) != 0;
        boolean runtimeTypeCheck = (configId & 2) != 0;
        boolean lenientMode = (configId & 4) != 0;
        boolean feelCompilation = (configId & 8) != 0;
        
        return new DMNOptimizationConfig(alphaNetwork, runtimeTypeCheck, lenientMode, feelCompilation);
    }

    private double calculateMedian(List<Long> values) {
        List<Long> sorted = values.stream().sorted().collect(Collectors.toList());
        int size = sorted.size();
        if (size % 2 == 0) {
            return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
        } else {
            return sorted.get(size / 2);
        }
    }

    private double calculatePercentile(List<Long> values, double percentile) {
        List<Long> sorted = values.stream().sorted().collect(Collectors.toList());
        int index = (int) Math.ceil(percentile * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }

    private void generateUnifiedRuntimeReport(List<UnifiedRuntimeResult> results) {
        try (FileWriter writer = new FileWriter("dmn-unified-runtime-analysis.txt")) {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            writer.write("=== DMN UNIFIED RUNTIME PERFORMANCE ANALYSIS ===\n");
            writer.write("Generated: " + timestamp + "\n");
            writer.write("Total configurations tested: " + results.size() + "\n");
            writer.write("DMN models in unified runtime: " + DMN_FILES.length + "\n\n");
            
            writeUnifiedVsIndividualComparison(writer, results);
            writeMemoryEfficiencyAnalysis(writer, results);
            writeCrossModelPerformanceAnalysis(writer, results);
            writeOptimalUnifiedConfigurations(writer, results);
            writeUnifiedRuntimeRecommendations(writer, results);
            
        } catch (IOException e) {
            System.err.println("Error writing unified runtime analysis: " + e.getMessage());
        }
    }

    private void writeUnifiedVsIndividualComparison(FileWriter writer, List<UnifiedRuntimeResult> results) throws IOException {
        writer.write("UNIFIED vs INDIVIDUAL RUNTIME COMPARISON:\n");
        writer.write("=========================================\n\n");
        
        UnifiedRuntimeResult baseline = results.stream()
            .filter(r -> r.configId == 0)
            .findFirst()
            .orElse(null);
        
        if (baseline != null) {
            writer.write("BASELINE COMPARISON (Config 0 - No Optimizations):\n");
            writer.write(String.format("Build Time: Unified=%.2fms, Individual=%.2fms, Overhead=%.1f%%\n",
                baseline.unifiedBuildTimeMs, baseline.individualBuildTimeSumMs,
                ((baseline.unifiedBuildTimeMs - baseline.individualBuildTimeSumMs) / baseline.individualBuildTimeSumMs) * 100));
            writer.write(String.format("Memory Usage: Unified=%.2fMB, Individual=%.2fMB, Savings=%.1f%%\n",
                baseline.unifiedMemoryUsageMB, baseline.individualMemoryUsageMB,
                ((baseline.individualMemoryUsageMB - baseline.unifiedMemoryUsageMB) / baseline.individualMemoryUsageMB) * 100));
            writer.write(String.format("Evaluation Time: Unified=%.2fμs, Individual=%.2fμs, Change=%.1f%%\n\n",
                baseline.avgUnifiedEvaluationTimeMicros, baseline.avgIndividualEvaluationTimeMicros,
                ((baseline.avgUnifiedEvaluationTimeMicros - baseline.avgIndividualEvaluationTimeMicros) / baseline.avgIndividualEvaluationTimeMicros) * 100));
        }
        
        // Find best unified runtime configuration
        UnifiedRuntimeResult bestUnified = results.stream()
            .filter(r -> r.avgUnifiedEvaluationTimeMicros > 0)
            .min(Comparator.comparing(r -> r.avgUnifiedEvaluationTimeMicros))
            .orElse(null);
        
        if (bestUnified != null) {
            writer.write("BEST UNIFIED RUNTIME CONFIGURATION:\n");
            writer.write(String.format("Config %d: %s\n", bestUnified.configId, bestUnified.config));
            writer.write(String.format("Build Time: %.2fms (vs %.2fms individual)\n",
                bestUnified.unifiedBuildTimeMs, bestUnified.individualBuildTimeSumMs));
            writer.write(String.format("Memory Usage: %.2fMB (vs %.2fMB individual)\n",
                bestUnified.unifiedMemoryUsageMB, bestUnified.individualMemoryUsageMB));
            writer.write(String.format("Evaluation Time: %.2fμs (vs %.2fμs individual)\n",
                bestUnified.avgUnifiedEvaluationTimeMicros, bestUnified.avgIndividualEvaluationTimeMicros));
            writer.write(String.format("Efficiency Score: %.2f\n\n", bestUnified.runtimeEfficiencyScore));
        }
    }

    private void writeMemoryEfficiencyAnalysis(FileWriter writer, List<UnifiedRuntimeResult> results) throws IOException {
        writer.write("MEMORY EFFICIENCY ANALYSIS:\n");
        writer.write("============================\n\n");
        
        double avgMemorySavings = results.stream()
            .filter(r -> r.individualMemoryUsageMB > 0)
            .mapToDouble(r -> ((r.individualMemoryUsageMB - r.unifiedMemoryUsageMB) / r.individualMemoryUsageMB) * 100)
            .average()
            .orElse(0.0);
        
        writer.write(String.format("Average memory savings with unified runtime: %.1f%%\n", avgMemorySavings));
        
        UnifiedRuntimeResult bestMemoryEfficiency = results.stream()
            .filter(r -> r.individualMemoryUsageMB > 0)
            .max(Comparator.comparing(r -> (r.individualMemoryUsageMB - r.unifiedMemoryUsageMB) / r.individualMemoryUsageMB))
            .orElse(null);
        
        if (bestMemoryEfficiency != null) {
            double savings = ((bestMemoryEfficiency.individualMemoryUsageMB - bestMemoryEfficiency.unifiedMemoryUsageMB) / bestMemoryEfficiency.individualMemoryUsageMB) * 100;
            writer.write(String.format("Best memory efficiency: Config %d with %.1f%% savings\n", 
                bestMemoryEfficiency.configId, savings));
            writer.write(String.format("  Unified: %.2fMB, Individual: %.2fMB\n\n", 
                bestMemoryEfficiency.unifiedMemoryUsageMB, bestMemoryEfficiency.individualMemoryUsageMB));
        }
    }

    private void writeCrossModelPerformanceAnalysis(FileWriter writer, List<UnifiedRuntimeResult> results) throws IOException {
        writer.write("CROSS-MODEL EVALUATION PERFORMANCE:\n");
        writer.write("===================================\n\n");
        
        UnifiedRuntimeResult bestCrossModel = results.stream()
            .filter(r -> r.avgCrossModelEvaluationTimeMicros > 0)
            .min(Comparator.comparing(r -> r.avgCrossModelEvaluationTimeMicros))
            .orElse(null);
        
        if (bestCrossModel != null) {
            writer.write(String.format("Best cross-model performance: Config %d\n", bestCrossModel.configId));
            writer.write(String.format("Average cross-model evaluation time: %.2fμs\n", bestCrossModel.avgCrossModelEvaluationTimeMicros));
            writer.write(String.format("Cross-model success rate: %.1f%%\n\n", bestCrossModel.crossModelSuccessRate));
        }
        
        double avgCrossModelTime = results.stream()
            .filter(r -> r.avgCrossModelEvaluationTimeMicros > 0)
            .mapToDouble(r -> r.avgCrossModelEvaluationTimeMicros)
            .average()
            .orElse(0.0);
        
        writer.write(String.format("Average cross-model evaluation time across all configs: %.2fμs\n\n", avgCrossModelTime));
    }

    private void writeOptimalUnifiedConfigurations(FileWriter writer, List<UnifiedRuntimeResult> results) throws IOException {
        writer.write("TOP UNIFIED RUNTIME CONFIGURATIONS:\n");
        writer.write("====================================\n\n");
        
        List<UnifiedRuntimeResult> topConfigs = results.stream()
            .filter(r -> r.avgUnifiedEvaluationTimeMicros > 0)
            .sorted(Comparator.comparing((UnifiedRuntimeResult r) -> r.runtimeEfficiencyScore).reversed())
            .limit(5)
            .collect(Collectors.toList());
        
        for (int i = 0; i < topConfigs.size(); i++) {
            UnifiedRuntimeResult config = topConfigs.get(i);
            writer.write(String.format("RANK %d - CONFIG %d: %s\n", i + 1, config.configId, config.config));
            writer.write(String.format("  Efficiency Score: %.2f\n", config.runtimeEfficiencyScore));
            writer.write(String.format("  Build Time: %.2fms\n", config.unifiedBuildTimeMs));
            writer.write(String.format("  Memory Usage: %.2fMB\n", config.unifiedMemoryUsageMB));
            writer.write(String.format("  Evaluation Time: %.2fμs\n", config.avgUnifiedEvaluationTimeMicros));
            writer.write(String.format("  Success Rate: %.1f%%\n\n", config.unifiedSuccessRate));
        }
    }

    private void writeUnifiedRuntimeRecommendations(FileWriter writer, List<UnifiedRuntimeResult> results) throws IOException {
        writer.write("UNIFIED RUNTIME RECOMMENDATIONS:\n");
        writer.write("=================================\n\n");
        
        writer.write("WHEN TO USE UNIFIED RUNTIME:\n");
        writer.write("• Multiple DMN models need to be evaluated in the same application\n");
        writer.write("• Memory usage is a concern (average " + 
            String.format("%.1f%%", results.stream()
                .filter(r -> r.individualMemoryUsageMB > 0)
                .mapToDouble(r -> ((r.individualMemoryUsageMB - r.unifiedMemoryUsageMB) / r.individualMemoryUsageMB) * 100)
                .average().orElse(0.0)) + " savings)\n");
        writer.write("• Cross-model decision scenarios are common\n");
        writer.write("• Application startup time is less critical than runtime performance\n\n");
        
        writer.write("WHEN TO USE INDIVIDUAL RUNTIMES:\n");
        writer.write("• Only one DMN model is used at a time\n");
        writer.write("• Models are loaded/unloaded dynamically\n");
        writer.write("• Strict isolation between decision models is required\n");
        writer.write("• Minimal startup time is critical\n\n");
        
        writer.write("OPTIMIZATION RECOMMENDATIONS FOR UNIFIED RUNTIME:\n");
        
        // Analyze which optimizations work best for unified runtime
        Map<String, Double> optimizationImpacts = new HashMap<>();
        for (int opt = 0; opt < 4; opt++) {
            final int currentOpt = opt;
            List<UnifiedRuntimeResult> withOpt = results.stream()
                .filter(r -> (r.configId & (1 << currentOpt)) != 0)
                .collect(Collectors.toList());
            List<UnifiedRuntimeResult> withoutOpt = results.stream()
                .filter(r -> (r.configId & (1 << currentOpt)) == 0)
                .collect(Collectors.toList());
            
            if (!withOpt.isEmpty() && !withoutOpt.isEmpty()) {
                double avgWith = withOpt.stream().mapToDouble(r -> r.avgUnifiedEvaluationTimeMicros).average().orElse(0);
                double avgWithout = withoutOpt.stream().mapToDouble(r -> r.avgUnifiedEvaluationTimeMicros).average().orElse(0);
                double impact = ((avgWithout - avgWith) / avgWithout) * 100;
                
                String[] optNames = {"Alpha Network", "Runtime Type Check", "Lenient Mode", "FEEL Compilation"};
                optimizationImpacts.put(optNames[currentOpt], impact);
            }
        }
        
        optimizationImpacts.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .forEach(entry -> {
                try {
                    writer.write(String.format("• %s: %.1f%% %s\n", 
                        entry.getKey(), Math.abs(entry.getValue()),
                        entry.getValue() > 0 ? "improvement" : "degradation"));
                } catch (IOException e) {
                    // Handle exception
                }
            });
    }

    private void exportUnifiedResultsToCsv(List<UnifiedRuntimeResult> results) {
        try (FileWriter writer = new FileWriter("dmn-unified-runtime-results.csv")) {
            // CSV Header
            writer.append("Timestamp,Config_ID,Alpha_Network,Runtime_Type_Check,Lenient_Mode,FEEL_Compilation,")
                  .append("Unified_Build_Time_Ms,Individual_Build_Time_Sum_Ms,Build_Time_Overhead_Percent,")
                  .append("Unified_Memory_MB,Individual_Memory_MB,Memory_Savings_Percent,")
                  .append("Unified_Avg_Eval_Time_Micros,Individual_Avg_Eval_Time_Micros,")
                  .append("Unified_Median_Eval_Time_Micros,Individual_Median_Eval_Time_Micros,")
                  .append("Unified_P95_Eval_Time_Micros,Individual_P95_Eval_Time_Micros,")
                  .append("Unified_Success_Rate,Individual_Success_Rate,")
                  .append("Cross_Model_Avg_Eval_Time_Micros,Cross_Model_Success_Rate,")
                  .append("Models_Count,Runtime_Efficiency_Score\n");
            
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            
            for (UnifiedRuntimeResult result : results) {
                double buildTimeOverhead = result.individualBuildTimeSumMs > 0 ? 
                    ((result.unifiedBuildTimeMs - result.individualBuildTimeSumMs) / result.individualBuildTimeSumMs) * 100 : 0;
                double memorySavings = result.individualMemoryUsageMB > 0 ?
                    ((result.individualMemoryUsageMB - result.unifiedMemoryUsageMB) / result.individualMemoryUsageMB) * 100 : 0;
                
                writer.append(String.format("%s,%d,%s,%s,%s,%s,%.3f,%.3f,%.2f,%.3f,%.3f,%.2f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.2f,%.2f,%.3f,%.2f,%d,%.3f\n",
                    timestamp,
                    result.configId,
                    result.config.alphaNetworkEnabled,
                    result.config.runtimeTypeCheckingEnabled,
                    result.config.lenientModeEnabled,
                    result.config.feelCompilationEnabled,
                    result.unifiedBuildTimeMs,
                    result.individualBuildTimeSumMs,
                    buildTimeOverhead,
                    result.unifiedMemoryUsageMB,
                    result.individualMemoryUsageMB,
                    memorySavings,
                    result.avgUnifiedEvaluationTimeMicros,
                    result.avgIndividualEvaluationTimeMicros,
                    result.medianUnifiedEvaluationTimeMicros,
                    result.medianIndividualEvaluationTimeMicros,
                    result.p95UnifiedEvaluationTimeMicros,
                    result.p95IndividualEvaluationTimeMicros,
                    result.unifiedSuccessRate,
                    result.individualSuccessRate,
                    result.avgCrossModelEvaluationTimeMicros,
                    result.crossModelSuccessRate,
                    result.modelsCount,
                    result.runtimeEfficiencyScore));
            }
        } catch (IOException e) {
            System.err.println("Error writing unified runtime CSV file: " + e.getMessage());
        }
    }

    // Helper method to create test contexts (reusing from DMNPerformanceComparison)
    private DMNContext createTestContext(DMNRuntime runtime, String dmnFileName) {
        DMNContext context = runtime.newContext();
        
        switch (dmnFileName) {
            case "loan-approval.dmn":
                context.set("Applicant", createApplicant(25, 75000, 720));
                context.set("Loan", createLoan(300000, 30));
                break;
                
            case "insurance-risk-assessment.dmn":
                context.set("Person", createPerson(35, "FEMALE", "ENGINEER", "MARRIED", 75000, 750));
                context.set("Vehicle", createVehicle("TOYOTA", "CAMRY", 2019, 25000, 5, true));
                context.set("DrivingHistory", createDrivingHistory(15, 0, 1, 0, 150000));
                context.set("Coverage", createCoverage(500000, true, true, 1000));
                break;
                
            case "supply-chain-optimization.dmn":
                context.set("Product", createProduct("P002", "ELECTRONICS", 1.2, 0.05, 8000, "MEDIUM", false));
                context.set("Order", createOrder("O002", 50, "HIGH", "2024-12-25", "CHICAGO", "GOLD"));
                context.set("Supplier", createSupplier("S002", "TEXAS", 8.0, 45.0, 10, 500, 8.5));
                context.set("Warehouse", createWarehouse("W002", "ILLINOIS", 300, 800, 1000, 18, false));
                context.set("TransportRoute", createTransportRoute("R002", "ILLINOIS", "CHICAGO", 50, 2.0, 4, 2.0));
                break;
                
            case "financial-portfolio-analysis.dmn":
                context.set("InvestorProfile", createInvestorProfile(45, 100000, 1200000, "MEDIUM", 20, "HIGH", "EXPERIENCED"));
                context.set("MarketConditions", createMarketConditions(22, "SIDEWAYS", 3.8, 2.5, "MODERATE", "MIXED"));
                context.set("InvestmentGoals", createInvestmentGoals("BALANCED_GROWTH", 8.0, 15.0, 8000, "MEDIUM_PRIORITY", "MEDIUM_PRIORITY"));
                context.set("CurrentPortfolio", createCurrentPortfolio(1000000, 60, 30, 10, 5, 25, 20));
                break;
                
            case "healthcare-treatment-protocol.dmn":
                context.set("Patient", createPatient(35, "FEMALE", 65, 165, 23.9, "NORMAL", 75, 36.8, 98));
                context.set("MedicalHistory", createMedicalHistory(List.of(), List.of(), List.of(), List.of(), List.of(), "NEVER_SMOKER", "NONE"));
                context.set("Symptoms", createSymptoms("MILD_PAIN", 7, 4, List.of("FATIGUE"), 3, "GRADUAL", "INTERMITTENT"));
                context.set("LabResults", createLabResults("NORMAL", "NORMAL", "NORMAL", "NORMAL", "NORMAL", "NORMAL", 95, 5.2));
                context.set("ImagingResults", createImagingResults("NORMAL", "NOT_DONE", "NOT_DONE", "NOT_DONE", "NORMAL", "NOT_DONE"));
                break;
        }
        
        return context;
    }

    // Data creation helper methods (same as in DMNPerformanceComparison)
    private Map<String, Object> createApplicant(int age, int income, int creditScore) {
        Map<String, Object> applicant = new HashMap<>();
        applicant.put("age", age);
        applicant.put("income", income);
        applicant.put("creditScore", creditScore);
        return applicant;
    }

    private Map<String, Object> createLoan(int amount, int term) {
        Map<String, Object> loan = new HashMap<>();
        loan.put("amount", amount);
        loan.put("term", term);
        return loan;
    }

    private Map<String, Object> createPerson(int age, String gender, String occupation, String maritalStatus, int annualIncome, int creditScore) {
        Map<String, Object> person = new HashMap<>();
        person.put("age", age);
        person.put("gender", gender);
        person.put("occupation", occupation);
        person.put("maritalStatus", maritalStatus);
        person.put("annualIncome", annualIncome);
        person.put("creditScore", creditScore);
        return person;
    }

    private Map<String, Object> createVehicle(String make, String model, int year, int value, int safetyRating, boolean antitheftDevice) {
        Map<String, Object> vehicle = new HashMap<>();
        vehicle.put("make", make);
        vehicle.put("model", model);
        vehicle.put("year", year);
        vehicle.put("value", value);
        vehicle.put("safetyRating", safetyRating);
        vehicle.put("antiTheft", antitheftDevice);  // Fixed: was "antitheftDevice"
        return vehicle;
    }

    private Map<String, Object> createDrivingHistory(int yearsLicensed, int accidents, int violations, int claims, int totalMileage) {
        Map<String, Object> history = new HashMap<>();
        history.put("yearsLicensed", yearsLicensed);
        history.put("accidentsLastThreeYears", accidents);    // Fixed: was "accidents"
        history.put("violationsLastThreeYears", violations);  // Fixed: was "violations"
        history.put("claimsLastFiveYears", claims);           // Fixed: was "claims"
        history.put("milesPerYear", totalMileage);            // Fixed: was "totalMileage"
        return history;
    }

    private Map<String, Object> createCoverage(int liability, boolean comprehensive, boolean collision, int deductible) {
        Map<String, Object> coverage = new HashMap<>();
        coverage.put("liability", liability);
        coverage.put("comprehensive", comprehensive);
        coverage.put("collision", collision);
        coverage.put("deductible", deductible);
        return coverage;
    }

    private Map<String, Object> createProduct(String id, String category, double weight, double volume, int value, String fragility, boolean temperatureSensitive) {
        Map<String, Object> product = new HashMap<>();
        product.put("id", id);
        product.put("category", category);
        product.put("weight", weight);
        product.put("volume", volume);
        product.put("value", value);
        product.put("fragility", fragility);
        product.put("temperatureSensitive", temperatureSensitive);
        return product;
    }

    private Map<String, Object> createOrder(String id, int quantity, String priority, String deadline, String destination, String customerTier) {
        Map<String, Object> order = new HashMap<>();
        order.put("id", id);
        order.put("quantity", quantity);
        order.put("priority", priority);
        order.put("deadline", deadline);
        order.put("destination", destination);
        order.put("customerTier", customerTier);
        return order;
    }

    private Map<String, Object> createSupplier(String id, String location, double reliabilityScore, double costPerUnit, int leadTime, int capacity, double qualityRating) {
        Map<String, Object> supplier = new HashMap<>();
        supplier.put("id", id);
        supplier.put("location", location);
        supplier.put("reliabilityScore", reliabilityScore);
        supplier.put("costPerUnit", costPerUnit);
        supplier.put("leadTime", leadTime);
        supplier.put("capacity", capacity);
        supplier.put("qualityRating", qualityRating);
        return supplier;
    }

    private Map<String, Object> createWarehouse(String id, String location, int currentInventory, int maxCapacity, int operatingCost, int processingTime, boolean temperatureControlled) {
        Map<String, Object> warehouse = new HashMap<>();
        warehouse.put("id", id);
        warehouse.put("location", location);
        warehouse.put("currentInventory", currentInventory);
        warehouse.put("maxCapacity", maxCapacity);
        warehouse.put("operatingCost", operatingCost);
        warehouse.put("processingTime", processingTime);
        warehouse.put("temperatureControlled", temperatureControlled);
        return warehouse;
    }

    private Map<String, Object> createTransportRoute(String id, String from, String to, int distance, double costPerKm, int transitTime, double riskFactor) {
        Map<String, Object> route = new HashMap<>();
        route.put("id", id);
        route.put("from", from);
        route.put("to", to);
        route.put("distance", distance);
        route.put("costPerKm", costPerKm);
        route.put("transitTime", transitTime);
        route.put("riskFactor", riskFactor);
        return route;
    }

    private Map<String, Object> createInvestorProfile(int age, int annualIncome, int netWorth, String riskTolerance, int investmentHorizon, String liquidityNeeds, String investmentExperience) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("age", age);
        profile.put("annualIncome", annualIncome);
        profile.put("netWorth", netWorth);
        profile.put("riskTolerance", riskTolerance);
        profile.put("investmentHorizon", investmentHorizon);
        profile.put("liquidityNeeds", liquidityNeeds);
        profile.put("knowledgeLevel", investmentExperience);
        return profile;
    }

    private Map<String, Object> createMarketConditions(double volatility, String trend, double interestRates, double inflationRate, String economicIndicators, String sectorRotation) {
        Map<String, Object> conditions = new HashMap<>();
        conditions.put("volatilityIndex", volatility);  // Fixed: was "volatility"
        conditions.put("marketTrend", trend);           // Fixed: was "trend"
        conditions.put("interestRates", interestRates);
        conditions.put("inflationRate", inflationRate);
        conditions.put("economicOutlook", economicIndicators);  // Fixed: was "economicIndicators"
        conditions.put("sectorPerformance", sectorRotation);    // Fixed: was "sectorRotation"
        return conditions;
    }

    private Map<String, Object> createInvestmentGoals(String primaryObjective, double targetReturn, double maximumDrawdown, int incomeRequirement, String taxConsiderations, String esgPreferences) {
        Map<String, Object> goals = new HashMap<>();
        goals.put("primaryObjective", primaryObjective);
        goals.put("targetReturn", targetReturn);
        goals.put("maxDrawdown", maximumDrawdown);        // Fixed: was "maximumDrawdown"
        goals.put("investmentHorizon", incomeRequirement); // Fixed: was "incomeRequirement", using as horizon
        goals.put("incomePriority", taxConsiderations);   // Fixed: was "taxConsiderations"
        goals.put("growthPriority", esgPreferences);      // Fixed: was "esgPreferences"
        return goals;
    }

    private Map<String, Object> createCurrentPortfolio(int totalValue, int equityAllocation, int bondAllocation, int alternativeAllocation, int cashAllocation, int internationalAllocation, int sectorConcentration) {
        Map<String, Object> portfolio = new HashMap<>();
        portfolio.put("totalValue", totalValue);
        portfolio.put("equityAllocation", equityAllocation);
        portfolio.put("bondAllocation", bondAllocation);
        portfolio.put("alternativeAllocation", alternativeAllocation);
        portfolio.put("cashAllocation", cashAllocation);
        portfolio.put("sectorConcentration", sectorConcentration);
        portfolio.put("geographicConcentration", internationalAllocation); // Fixed: was "internationalAllocation"
        return portfolio;
    }

    private Map<String, Object> createPatient(int age, String gender, double weight, double height, double bmi, String bloodPressure, int heartRate, double temperature, int oxygenSaturation) {
        Map<String, Object> patient = new HashMap<>();
        patient.put("age", age);
        patient.put("gender", gender);
        patient.put("weight", weight);
        patient.put("height", height);
        patient.put("bmi", bmi);
        patient.put("bloodPressureCategory", bloodPressure); // Fixed: was "bloodPressure"
        patient.put("heartRate", heartRate);
        patient.put("temperature", temperature);
        patient.put("oxygenSaturation", oxygenSaturation);
        return patient;
    }

    private Map<String, Object> createMedicalHistory(List<String> chronicConditions, List<String> allergies, List<String> currentMedications, 
                                                   List<String> previousSurgeries, List<String> familyHistory, String smokingStatus, String alcoholConsumption) {
        Map<String, Object> history = new HashMap<>();
        history.put("chronicConditions", chronicConditions);
        history.put("allergies", allergies);
        history.put("currentMedications", currentMedications);
        history.put("recentSurgeries", previousSurgeries);  // Fixed: was "previousSurgeries"
        history.put("familyHistory", familyHistory);
        history.put("smokingStatus", smokingStatus);
        history.put("alcoholUse", alcoholConsumption);      // Fixed: was "alcoholConsumption"
        return history;
    }

    private Map<String, Object> createSymptoms(String primaryComplaint, int symptomDuration, int severityScore, List<String> associatedSymptoms, int painLevel, String onsetType, String symptomPattern) {
        Map<String, Object> symptoms = new HashMap<>();
        symptoms.put("primaryComplaint", primaryComplaint);
        symptoms.put("durationHours", symptomDuration);      // Fixed: was "symptomDuration"
        symptoms.put("severityScale", severityScore);        // Fixed: was "severityScore"
        symptoms.put("associatedSymptoms", associatedSymptoms);
        symptoms.put("painScale", painLevel);                // Fixed: was "painLevel"
        symptoms.put("onsetType", onsetType);
        symptoms.put("pattern", symptomPattern);             // Fixed: was "symptomPattern"
        return symptoms;
    }

    private Map<String, Object> createLabResults(String completeBloodCount, String basicMetabolicPanel, String liverFunction, 
                                                String kidneyFunction, String cardiacMarkers, String inflammatoryMarkers, int glucoseLevel, double hemoglobinA1c) {
        Map<String, Object> labs = new HashMap<>();
        labs.put("bloodCount", completeBloodCount);           // Fixed: was "completeBloodCount"
        labs.put("chemistryPanel", basicMetabolicPanel);     // Fixed: was "basicMetabolicPanel"
        labs.put("liverFunction", liverFunction);
        labs.put("kidneyFunction", kidneyFunction);
        labs.put("cardiacMarkers", cardiacMarkers);
        labs.put("inflammatoryMarkers", inflammatoryMarkers);
        labs.put("glucoseLevel", glucoseLevel);
        labs.put("hemoglobinA1c", hemoglobinA1c);
        return labs;
    }

    private Map<String, Object> createImagingResults(String chestXray, String ctScan, String mri, String ultrasound, String ecg, String echocardiogram) {
        Map<String, Object> imaging = new HashMap<>();
        imaging.put("chestXray", chestXray);
        imaging.put("ctScan", ctScan);
        imaging.put("mri", mri);
        imaging.put("ultrasound", ultrasound);
        imaging.put("ecg", ecg);
        imaging.put("echocardiogram", echocardiogram);
        return imaging;
    }

    // Data classes for results and metrics
    public static class DMNOptimizationConfig {
        public final boolean alphaNetworkEnabled;
        public final boolean runtimeTypeCheckingEnabled;
        public final boolean lenientModeEnabled;
        public final boolean feelCompilationEnabled;

        public DMNOptimizationConfig(boolean alphaNetwork, boolean runtimeTypeCheck, boolean lenient, boolean feelCompilation) {
            this.alphaNetworkEnabled = alphaNetwork;
            this.runtimeTypeCheckingEnabled = runtimeTypeCheck;
            this.lenientModeEnabled = lenient;
            this.feelCompilationEnabled = feelCompilation;
        }

        @Override
        public String toString() {
            return String.format("Alpha=%s, TypeCheck=%s, Lenient=%s, FEEL=%s",
                alphaNetworkEnabled ? "ON" : "OFF",
                runtimeTypeCheckingEnabled ? "ON" : "OFF",
                lenientModeEnabled ? "ON" : "OFF",
                feelCompilationEnabled ? "ON" : "OFF");
        }
    }

    public static class UnifiedRuntimeResult {
        public final int configId;
        public final DMNOptimizationConfig config;
        public final double unifiedBuildTimeMs;
        public final double individualBuildTimeSumMs;
        public final double unifiedMemoryUsageMB;
        public final double individualMemoryUsageMB;
        public final double avgUnifiedEvaluationTimeMicros;
        public final double avgIndividualEvaluationTimeMicros;
        public final double medianUnifiedEvaluationTimeMicros;
        public final double medianIndividualEvaluationTimeMicros;
        public final double p95UnifiedEvaluationTimeMicros;
        public final double p95IndividualEvaluationTimeMicros;
        public final double unifiedSuccessRate;
        public final double individualSuccessRate;
        public final double avgCrossModelEvaluationTimeMicros;
        public final double crossModelSuccessRate;
        public final int modelsCount;
        public final double runtimeEfficiencyScore;

        public UnifiedRuntimeResult(int configId, DMNOptimizationConfig config,
                                  double unifiedBuildTimeMs, double individualBuildTimeSumMs,
                                  double unifiedMemoryUsageMB, double individualMemoryUsageMB,
                                  double avgUnifiedEvaluationTimeMicros, double avgIndividualEvaluationTimeMicros,
                                  double medianUnifiedEvaluationTimeMicros, double medianIndividualEvaluationTimeMicros,
                                  double p95UnifiedEvaluationTimeMicros, double p95IndividualEvaluationTimeMicros,
                                  double unifiedSuccessRate, double individualSuccessRate,
                                  double avgCrossModelEvaluationTimeMicros, double crossModelSuccessRate,
                                  int modelsCount, double runtimeEfficiencyScore) {
            this.configId = configId;
            this.config = config;
            this.unifiedBuildTimeMs = unifiedBuildTimeMs;
            this.individualBuildTimeSumMs = individualBuildTimeSumMs;
            this.unifiedMemoryUsageMB = unifiedMemoryUsageMB;
            this.individualMemoryUsageMB = individualMemoryUsageMB;
            this.avgUnifiedEvaluationTimeMicros = avgUnifiedEvaluationTimeMicros;
            this.avgIndividualEvaluationTimeMicros = avgIndividualEvaluationTimeMicros;
            this.medianUnifiedEvaluationTimeMicros = medianUnifiedEvaluationTimeMicros;
            this.medianIndividualEvaluationTimeMicros = medianIndividualEvaluationTimeMicros;
            this.p95UnifiedEvaluationTimeMicros = p95UnifiedEvaluationTimeMicros;
            this.p95IndividualEvaluationTimeMicros = p95IndividualEvaluationTimeMicros;
            this.unifiedSuccessRate = unifiedSuccessRate;
            this.individualSuccessRate = individualSuccessRate;
            this.avgCrossModelEvaluationTimeMicros = avgCrossModelEvaluationTimeMicros;
            this.crossModelSuccessRate = crossModelSuccessRate;
            this.modelsCount = modelsCount;
            this.runtimeEfficiencyScore = runtimeEfficiencyScore;
        }
    }

    public static class MemoryMetrics {
        public final double totalMemoryMB;

        public MemoryMetrics(double totalMemoryMB) {
            this.totalMemoryMB = totalMemoryMB;
        }
    }

    public static class EvaluationMetrics {
        public final double avgEvaluationTimeMicros;
        public final double medianEvaluationTimeMicros;
        public final double p95EvaluationTimeMicros;
        public final double successRate;

        public EvaluationMetrics(double avgEvaluationTimeMicros, double medianEvaluationTimeMicros, 
                               double p95EvaluationTimeMicros, double successRate) {
            this.avgEvaluationTimeMicros = avgEvaluationTimeMicros;
            this.medianEvaluationTimeMicros = medianEvaluationTimeMicros;
            this.p95EvaluationTimeMicros = p95EvaluationTimeMicros;
            this.successRate = successRate;
        }
    }

    public static class CrossModelMetrics {
        public final double avgCrossModelEvaluationTimeMicros;
        public final double crossModelSuccessRate;

        public CrossModelMetrics(double avgCrossModelEvaluationTimeMicros, double crossModelSuccessRate) {
            this.avgCrossModelEvaluationTimeMicros = avgCrossModelEvaluationTimeMicros;
            this.crossModelSuccessRate = crossModelSuccessRate;
        }
    }
}
