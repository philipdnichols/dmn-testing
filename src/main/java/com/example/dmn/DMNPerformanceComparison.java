package com.example.dmn;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.dmn.api.core.DMNContext;
import org.kie.dmn.api.core.DMNResult;
import org.kie.dmn.api.core.DMNRuntime;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Comprehensive performance comparison testing for DMN runtime configurations.
 * Tests all 16 combinations of the 4 optimization options:
 * 1. Alpha Network (enabled/disabled)
 * 2. Runtime Type Checking (enabled/disabled)
 * 3. Lenient Mode (enabled/disabled)
 * 4. FEEL Compilation (enabled/disabled)
 */
public class DMNPerformanceComparison {

    private static final int WARMUP_ITERATIONS = 50;
    private static final int PERFORMANCE_ITERATIONS = 500;
    private static final int BUILD_TIME_ITERATIONS = 20;
    private static final String[] DMN_FILES = {
        "loan-approval.dmn",
        "insurance-risk-assessment.dmn",
        "supply-chain-optimization.dmn",
        "financial-portfolio-analysis.dmn",
        "healthcare-treatment-protocol.dmn"
    };

    public static void main(String[] args) {
        DMNPerformanceComparison comparison = new DMNPerformanceComparison();
        
        System.out.println("=== DMN Performance Optimization Comparison ===");
        System.out.println("Testing " + DMN_FILES.length + " DMN models with " + PERFORMANCE_ITERATIONS + " iterations each");
        System.out.println("Warmup iterations: " + WARMUP_ITERATIONS);
        System.out.println();
        
        List<PerformanceResult> allResults = new ArrayList<>();
        
        // Test all 16 combinations of optimization options
        for (int configId = 0; configId < 16; configId++) {
            DMNOptimizationConfig config = createOptimizationConfig(configId);
            System.out.println("Testing Configuration " + configId + ": " + config);
            
            for (String dmnFile : DMN_FILES) {
                PerformanceResult result = comparison.measurePerformance(dmnFile, config, configId);
                allResults.add(result);
                System.out.printf("  %s: Build=%.2fms (med=%.2f, p95=%.2f), Eval=%.2fμs (med=%.2f, p95=%.2f)%n", 
                    dmnFile, result.avgBuildTimeMs, result.medianBuildTimeMs, result.p95BuildTimeMs,
                    result.avgEvaluationTimeMicros, result.medianEvaluationTimeMicros, result.p95EvaluationTimeMicros);
            }
            System.out.println();
        }
        
        // Generate comprehensive report
        comparison.generateReport(allResults);
        comparison.exportToCsv(allResults);
        
        System.out.println("=== Performance Comparison Complete ===");
        System.out.println("Results exported to dmn-performance-results.csv");
    }

    public PerformanceResult measurePerformance(String dmnFileName, DMNOptimizationConfig config, int configId) {
        try {
            // Measure build times with multiple iterations
            List<Long> buildTimes = new ArrayList<>();
            DMNRuntime runtime = null;
            
            for (int i = 0; i < BUILD_TIME_ITERATIONS; i++) {
                long buildStartTime = System.nanoTime();
                runtime = createDMNRuntime(dmnFileName, config);
                long buildEndTime = System.nanoTime();
                buildTimes.add(buildEndTime - buildStartTime);
            }
            
            // Calculate build time statistics
            double avgBuildTimeMs = buildTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0) / 1_000_000.0;
            
            double medianBuildTimeMs = calculateMedian(buildTimes) / 1_000_000.0;
            double p95BuildTimeMs = calculatePercentile(buildTimes, 0.95) / 1_000_000.0;
            double p99BuildTimeMs = calculatePercentile(buildTimes, 0.99) / 1_000_000.0;

            // Create test context for the specific DMN (use the last runtime created)
            DMNContext testContext = createTestContext(runtime, dmnFileName);
            String serviceId = getServiceId(dmnFileName);

            // Warmup phase
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                try {
                    runtime.evaluateDecisionService(runtime.getModels().get(0), testContext, serviceId);
                } catch (Exception e) {
                    // Continue warmup even if some iterations fail
                }
            }

            // Performance measurement phase
            List<Long> evaluationTimes = new ArrayList<>();
            int successfulEvaluations = 0;
            
            for (int i = 0; i < PERFORMANCE_ITERATIONS; i++) {
                long startTime = System.nanoTime();
                try {
                    DMNResult result = runtime.evaluateDecisionService(runtime.getModels().get(0), testContext, serviceId);
                    if (!result.hasErrors()) {
                        successfulEvaluations++;
                    }
                } catch (Exception e) {
                    // Count failed evaluations but continue
                }
                long endTime = System.nanoTime();
                evaluationTimes.add(endTime - startTime);
            }

            // Calculate statistics
            double avgEvaluationTimeMicros = evaluationTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0) / 1000.0;

            double medianEvaluationTimeMicros = calculateMedian(evaluationTimes) / 1000.0;
            double p95EvaluationTimeMicros = calculatePercentile(evaluationTimes, 0.95) / 1000.0;
            double p99EvaluationTimeMicros = calculatePercentile(evaluationTimes, 0.99) / 1000.0;

            return new PerformanceResult(
                dmnFileName, config, configId, 
                avgBuildTimeMs, medianBuildTimeMs, p95BuildTimeMs, p99BuildTimeMs,
                avgEvaluationTimeMicros, medianEvaluationTimeMicros, p95EvaluationTimeMicros, p99EvaluationTimeMicros,
                successfulEvaluations, PERFORMANCE_ITERATIONS
            );

        } catch (Exception e) {
            System.err.println("Error measuring performance for " + dmnFileName + " with config " + configId + ": " + e.getMessage());
            return new PerformanceResult(dmnFileName, config, configId, -1, -1, -1, -1, -1, -1, -1, -1, 0, PERFORMANCE_ITERATIONS);
        }
    }

    private DMNRuntime createDMNRuntime(String dmnFileName, DMNOptimizationConfig config) {
        try {
            // Set system properties for DMN optimization configuration
            // Note: These properties may vary by Drools version - this approach uses common patterns
            
            // Store original system properties to restore later
            Properties originalProps = new Properties();
            
            try {
                // Alpha Network Compilation
                String alphaNetworkProp = "org.kie.dmn.alphanetwork.enabled";
                originalProps.setProperty(alphaNetworkProp, System.getProperty(alphaNetworkProp, ""));
                System.setProperty(alphaNetworkProp, String.valueOf(config.alphaNetworkEnabled));
                
                // Runtime Type Checking
                String typeCheckProp = "org.kie.dmn.runtime.typecheck";
                originalProps.setProperty(typeCheckProp, System.getProperty(typeCheckProp, ""));
                System.setProperty(typeCheckProp, String.valueOf(config.runtimeTypeCheckingEnabled));
                
                // Lenient Mode
                String lenientProp = "org.kie.dmn.strict";
                originalProps.setProperty(lenientProp, System.getProperty(lenientProp, ""));
                System.setProperty(lenientProp, String.valueOf(!config.lenientModeEnabled));
                
                // FEEL Compilation
                String feelProp = "org.kie.dmn.feel.compilation";
                originalProps.setProperty(feelProp, System.getProperty(feelProp, ""));
                System.setProperty(feelProp, String.valueOf(config.feelCompilationEnabled));
                
                KieServices kieServices = KieServices.Factory.get();
                KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
                
                // Load the DMN file from resources
                InputStream dmnStream = getClass().getClassLoader().getResourceAsStream(dmnFileName);
                if (dmnStream == null) {
                    throw new RuntimeException("Could not find " + dmnFileName + " in resources");
                }
                
                kieFileSystem.write("src/main/resources/" + dmnFileName, 
                    kieServices.getResources().newInputStreamResource(dmnStream));
                
                KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
                kieBuilder.buildAll();
                
                if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
                    System.err.println("Build errors for " + dmnFileName + ":");
                    kieBuilder.getResults().getMessages(Message.Level.ERROR)
                        .forEach(message -> System.err.println("  " + message.getText()));
                    throw new RuntimeException("Failed to build DMN model: " + dmnFileName);
                }
                
                KieContainer kieContainer = kieServices.newKieContainer(
                    kieBuilder.getKieModule().getReleaseId());
                
                return kieContainer.newKieSession().getKieRuntime(DMNRuntime.class);
                
            } finally {
                // Restore original system properties
                for (String prop : originalProps.stringPropertyNames()) {
                    String originalValue = originalProps.getProperty(prop);
                    if (originalValue.isEmpty()) {
                        System.clearProperty(prop);
                    } else {
                        System.setProperty(prop, originalValue);
                    }
                }
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create DMN runtime for " + dmnFileName + " with config " + config, e);
        }
    }

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

    private String getServiceId(String dmnFileName) {
        switch (dmnFileName) {
            case "loan-approval.dmn":
                return "Loan Approval Service";
            case "insurance-risk-assessment.dmn":
                return "Insurance Risk Assessment Service";
            case "supply-chain-optimization.dmn":
                return "Supply Chain Optimization Service";
            case "financial-portfolio-analysis.dmn":
                return "Portfolio Analysis Service";
            case "healthcare-treatment-protocol.dmn":
                return "Healthcare Treatment Protocol Service";
            default:
                throw new IllegalArgumentException("Unknown DMN file: " + dmnFileName);
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

    private void generateReport(List<PerformanceResult> results) {
        System.out.println("\n=== PERFORMANCE COMPARISON SUMMARY ===");
        
        // Group results by DMN file
        Map<String, List<PerformanceResult>> resultsByFile = results.stream()
            .collect(Collectors.groupingBy(r -> r.dmnFileName));
        
        for (String dmnFile : DMN_FILES) {
            List<PerformanceResult> fileResults = resultsByFile.get(dmnFile);
            if (fileResults == null || fileResults.isEmpty()) continue;
            
            System.out.println("\n" + dmnFile.toUpperCase() + ":");
            System.out.println("Config | AlphaNet | TypeCheck | Lenient | FEEL | Build(ms) | Build P95 | Eval(μs) | Eval P95 | Success%");
            System.out.println("-------|----------|-----------|---------|------|-----------|-----------|----------|----------|--------");
            
            for (PerformanceResult result : fileResults) {
                double successRate = (result.successfulEvaluations * 100.0) / result.totalEvaluations;
                System.out.printf("%6d | %8s | %9s | %7s | %4s | %9.2f | %9.2f | %8.2f | %8.2f | %7.1f%n",
                    result.configId,
                    result.config.alphaNetworkEnabled ? "YES" : "NO",
                    result.config.runtimeTypeCheckingEnabled ? "YES" : "NO",
                    result.config.lenientModeEnabled ? "YES" : "NO",
                    result.config.feelCompilationEnabled ? "YES" : "NO",
                    result.avgBuildTimeMs,
                    result.p95BuildTimeMs,
                    result.avgEvaluationTimeMicros,
                    result.p95EvaluationTimeMicros,
                    successRate);
            }
            
            // Find best and worst configurations
            PerformanceResult fastest = fileResults.stream()
                .filter(r -> r.avgEvaluationTimeMicros > 0)
                .min(Comparator.comparing(r -> r.avgEvaluationTimeMicros))
                .orElse(null);
            
            PerformanceResult slowest = fileResults.stream()
                .filter(r -> r.avgEvaluationTimeMicros > 0)
                .max(Comparator.comparing(r -> r.avgEvaluationTimeMicros))
                .orElse(null);
            
            if (fastest != null && slowest != null) {
                double improvement = ((slowest.avgEvaluationTimeMicros - fastest.avgEvaluationTimeMicros) 
                    / slowest.avgEvaluationTimeMicros) * 100;
                System.out.printf("Best: Config %d (%.2fμs), Worst: Config %d (%.2fμs), Improvement: %.1f%%%n",
                    fastest.configId, fastest.avgEvaluationTimeMicros,
                    slowest.configId, slowest.avgEvaluationTimeMicros,
                    improvement);
            }
        }
    }

    private void exportToCsv(List<PerformanceResult> results) {
        try (FileWriter writer = new FileWriter("dmn-performance-results.csv")) {
            // CSV Header
            writer.append("Timestamp,DMN_File,Config_ID,Alpha_Network,Runtime_Type_Check,Lenient_Mode,FEEL_Compilation,")
                  .append("Avg_Build_Time_Ms,Median_Build_Time_Ms,P95_Build_Time_Ms,P99_Build_Time_Ms,")
                  .append("Avg_Eval_Time_Micros,Median_Eval_Time_Micros,P95_Eval_Time_Micros,P99_Eval_Time_Micros,")
                  .append("Successful_Evaluations,Total_Evaluations,Success_Rate\n");
            
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            
            for (PerformanceResult result : results) {
                double successRate = (result.successfulEvaluations * 100.0) / result.totalEvaluations;
                writer.append(String.format("%s,%s,%d,%s,%s,%s,%s,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%d,%d,%.2f\n",
                    timestamp,
                    result.dmnFileName,
                    result.configId,
                    result.config.alphaNetworkEnabled,
                    result.config.runtimeTypeCheckingEnabled,
                    result.config.lenientModeEnabled,
                    result.config.feelCompilationEnabled,
                    result.avgBuildTimeMs,
                    result.medianBuildTimeMs,
                    result.p95BuildTimeMs,
                    result.p99BuildTimeMs,
                    result.avgEvaluationTimeMicros,
                    result.medianEvaluationTimeMicros,
                    result.p95EvaluationTimeMicros,
                    result.p99EvaluationTimeMicros,
                    result.successfulEvaluations,
                    result.totalEvaluations,
                    successRate));
            }
        } catch (IOException e) {
            System.err.println("Error writing CSV file: " + e.getMessage());
        }
    }

    // Data creation helper methods (same as in DmnEvaluationExample)
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

    // Inner classes for configuration and results
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

    public static class PerformanceResult {
        public final String dmnFileName;
        public final DMNOptimizationConfig config;
        public final int configId;
        public final double avgBuildTimeMs;
        public final double medianBuildTimeMs;
        public final double p95BuildTimeMs;
        public final double p99BuildTimeMs;
        public final double avgEvaluationTimeMicros;
        public final double medianEvaluationTimeMicros;
        public final double p95EvaluationTimeMicros;
        public final double p99EvaluationTimeMicros;
        public final int successfulEvaluations;
        public final int totalEvaluations;

        public PerformanceResult(String dmnFileName, DMNOptimizationConfig config, int configId,
                               double avgBuildTimeMs, double medianBuildTimeMs, double p95BuildTimeMs, double p99BuildTimeMs,
                               double avgEvaluationTimeMicros, double medianEvaluationTimeMicros, double p95EvaluationTimeMicros, double p99EvaluationTimeMicros,
                               int successfulEvaluations, int totalEvaluations) {
            this.dmnFileName = dmnFileName;
            this.config = config;
            this.configId = configId;
            this.avgBuildTimeMs = avgBuildTimeMs;
            this.medianBuildTimeMs = medianBuildTimeMs;
            this.p95BuildTimeMs = p95BuildTimeMs;
            this.p99BuildTimeMs = p99BuildTimeMs;
            this.avgEvaluationTimeMicros = avgEvaluationTimeMicros;
            this.medianEvaluationTimeMicros = medianEvaluationTimeMicros;
            this.p95EvaluationTimeMicros = p95EvaluationTimeMicros;
            this.p99EvaluationTimeMicros = p99EvaluationTimeMicros;
            this.successfulEvaluations = successfulEvaluations;
            this.totalEvaluations = totalEvaluations;
        }
    }
}
