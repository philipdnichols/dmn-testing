package com.example.dmn;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.KieContainer;
import org.kie.dmn.api.core.DMNContext;
import org.kie.dmn.api.core.DMNResult;
import org.kie.dmn.api.core.DMNRuntime;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Performance analyzer comparing traditional DMN runtime creation vs KJAR-based approach.
 * This class demonstrates how precompiled KJARs can significantly improve DMN evaluation performance.
 */
public class DMNKjarPerformanceAnalyzer {

    private static final int WARMUP_ITERATIONS = 50;
    private static final int PERFORMANCE_ITERATIONS = 200;
    private static final int BUILD_TIME_ITERATIONS = 10;
    private static final String KJAR_CACHE_DIR = "target/kjar-cache";
    private static final String[] DMN_FILES = {
        "loan-approval.dmn",
        "insurance-risk-assessment.dmn", 
        "supply-chain-optimization.dmn",
        "financial-portfolio-analysis.dmn",
        "healthcare-treatment-protocol.dmn"
    };

    public static void main(String[] args) {
        DMNKjarPerformanceAnalyzer analyzer = new DMNKjarPerformanceAnalyzer();
        
        System.out.println("=== DMN KJAR Performance Analysis ===");
        System.out.println("Comparing traditional DMN runtime vs KJAR-based approach");
        System.out.println("Warmup iterations: " + WARMUP_ITERATIONS);
        System.out.println("Performance iterations: " + PERFORMANCE_ITERATIONS);
        System.out.println();
        
        List<PerformanceComparison> results = new ArrayList<>();
        
        // Test each DMN file
        for (String dmnFile : DMN_FILES) {
            System.out.println("Testing: " + dmnFile);
            PerformanceComparison comparison = analyzer.compareApproaches(dmnFile);
            results.add(comparison);
            
            System.out.printf("  Traditional: Build=%.2fms, Eval=%.2fμs%n", 
                comparison.traditionalBuildTimeMs, comparison.traditionalEvalTimeMicros);
            System.out.printf("  KJAR:        Build=%.2fms, Eval=%.2fμs%n", 
                comparison.kjarBuildTimeMs, comparison.kjarEvalTimeMicros);
            System.out.printf("  Improvement: Build=%.1f%%, Eval=%.1f%%%n%n", 
                comparison.buildTimeImprovement, comparison.evalTimeImprovement);
        }
        
        // Generate comprehensive report
        analyzer.generateReport(results);
        analyzer.exportToCsv(results);
        
        System.out.println("Results exported to dmn-kjar-performance-results.csv");
    }

    public PerformanceComparison compareApproaches(String dmnFileName) {
        try {
            // Clean up any existing cache for this DMN
            cleanupKjarCache(dmnFileName);
            
            // Measure traditional approach
            TraditionalMetrics traditional = measureTraditionalApproach(dmnFileName);
            
            // Measure KJAR approach (includes initial KJAR creation + subsequent loads)
            KjarMetrics kjar = measureKjarApproach(dmnFileName);
            
            // Calculate improvements
            double buildTimeImprovement = ((traditional.avgBuildTimeMs - kjar.avgBuildTimeMs) / traditional.avgBuildTimeMs) * 100;
            double evalTimeImprovement = ((traditional.avgEvalTimeMicros - kjar.avgEvalTimeMicros) / traditional.avgEvalTimeMicros) * 100;
            
            return new PerformanceComparison(
                dmnFileName,
                traditional.avgBuildTimeMs, traditional.avgEvalTimeMicros,
                kjar.avgBuildTimeMs, kjar.avgEvalTimeMicros,
                kjar.kjarCreationTimeMs, kjar.kjarLoadTimeMs,
                buildTimeImprovement, evalTimeImprovement,
                traditional.successfulEvaluations, kjar.successfulEvaluations
            );
            
        } catch (Exception e) {
            System.err.println("Error comparing approaches for " + dmnFileName + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private TraditionalMetrics measureTraditionalApproach(String dmnFileName) {
        List<Long> buildTimes = new ArrayList<>();
        List<Long> evalTimes = new ArrayList<>();
        int successfulEvaluations = 0;
        
        // Measure build times across multiple iterations
        for (int i = 0; i < BUILD_TIME_ITERATIONS; i++) {
            long buildStart = System.nanoTime();
            DMNRuntime runtime = createTraditionalDMNRuntime(dmnFileName);
            long buildEnd = System.nanoTime();
            buildTimes.add(buildEnd - buildStart);
            
            // Use this runtime for some evaluation tests
            if (i == 0) {
                DMNContext context = createTestContext(runtime, dmnFileName);
                String serviceId = getServiceId(dmnFileName);
                
                // Warmup
                for (int w = 0; w < WARMUP_ITERATIONS; w++) {
                    try {
                        runtime.evaluateDecisionService(runtime.getModels().get(0), context, serviceId);
                    } catch (Exception e) {
                        // Continue warmup
                    }
                }
                
                // Performance measurement
                for (int p = 0; p < PERFORMANCE_ITERATIONS; p++) {
                    long evalStart = System.nanoTime();
                    try {
                        DMNResult result = runtime.evaluateDecisionService(runtime.getModels().get(0), context, serviceId);
                        if (!result.hasErrors()) {
                            successfulEvaluations++;
                        }
                    } catch (Exception e) {
                        // Count failed evaluations
                    }
                    long evalEnd = System.nanoTime();
                    evalTimes.add(evalEnd - evalStart);
                }
            }
        }
        
        double avgBuildTimeMs = buildTimes.stream().mapToLong(Long::longValue).average().orElse(0.0) / 1_000_000.0;
        double avgEvalTimeMicros = evalTimes.stream().mapToLong(Long::longValue).average().orElse(0.0) / 1000.0;
        
        return new TraditionalMetrics(avgBuildTimeMs, avgEvalTimeMicros, successfulEvaluations);
    }

    private KjarMetrics measureKjarApproach(String dmnFileName) {
        List<Long> kjarLoadTimes = new ArrayList<>();
        List<Long> evalTimes = new ArrayList<>();
        int successfulEvaluations = 0;
        
        // Measure KJAR creation time (this is done once and cached)
        Path kjarPath = getKjarPath(dmnFileName);
        long kjarCreationStart = System.nanoTime();
        createKjar(dmnFileName, kjarPath);
        long kjarCreationEnd = System.nanoTime();
        double kjarCreationTimeMs = (kjarCreationEnd - kjarCreationStart) / 1_000_000.0;
        
        // Measure KJAR loading times (this is what happens repeatedly at runtime)
        for (int i = 0; i < BUILD_TIME_ITERATIONS; i++) {
            long loadStart = System.nanoTime();
            DMNRuntime runtime = loadDMNRuntimeFromKjar(kjarPath);
            long loadEnd = System.nanoTime();
            kjarLoadTimes.add(loadEnd - loadStart);
            
            // Use this runtime for some evaluation tests
            if (i == 0) {
                DMNContext context = createTestContext(runtime, dmnFileName);
                String serviceId = getServiceId(dmnFileName);
                
                // Warmup
                for (int w = 0; w < WARMUP_ITERATIONS; w++) {
                    try {
                        runtime.evaluateDecisionService(runtime.getModels().get(0), context, serviceId);
                    } catch (Exception e) {
                        // Continue warmup
                    }
                }
                
                // Performance measurement
                for (int p = 0; p < PERFORMANCE_ITERATIONS; p++) {
                    long evalStart = System.nanoTime();
                    try {
                        DMNResult result = runtime.evaluateDecisionService(runtime.getModels().get(0), context, serviceId);
                        if (!result.hasErrors()) {
                            successfulEvaluations++;
                        }
                    } catch (Exception e) {
                        // Count failed evaluations
                    }
                    long evalEnd = System.nanoTime();
                    evalTimes.add(evalEnd - evalStart);
                }
            }
        }
        
        double avgLoadTimeMs = kjarLoadTimes.stream().mapToLong(Long::longValue).average().orElse(0.0) / 1_000_000.0;
        double avgEvalTimeMicros = evalTimes.stream().mapToLong(Long::longValue).average().orElse(0.0) / 1000.0;
        
        return new KjarMetrics(avgLoadTimeMs, avgEvalTimeMicros, kjarCreationTimeMs, avgLoadTimeMs, successfulEvaluations);
    }

    private DMNRuntime createTraditionalDMNRuntime(String dmnFileName) {
        try {
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
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create traditional DMN runtime for " + dmnFileName, e);
        }
    }

    private void createKjar(String dmnFileName, Path kjarPath) {
        try {
            KieServices kieServices = KieServices.Factory.get();
            
            // Create a unique release ID for this KJAR
            String artifactId = "dmn-kjar-" + dmnFileName.replace(".dmn", "").replace("-", "");
            ReleaseId releaseId = kieServices.newReleaseId("com.example", artifactId, "1.0.0");
            
            KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
            kieFileSystem.generateAndWritePomXML(releaseId);
            
            // Load and add the DMN file
            InputStream dmnStream = getClass().getClassLoader().getResourceAsStream(dmnFileName);
            if (dmnStream == null) {
                throw new RuntimeException("Could not find " + dmnFileName + " in resources");
            }
            
            kieFileSystem.write("src/main/resources/" + dmnFileName, 
                kieServices.getResources().newInputStreamResource(dmnStream));
            
            KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
            kieBuilder.buildAll();
            
            if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
                System.err.println("KJAR build errors for " + dmnFileName + ":");
                kieBuilder.getResults().getMessages(Message.Level.ERROR)
                    .forEach(message -> System.err.println("  " + message.getText()));
                throw new RuntimeException("Failed to build KJAR for DMN model: " + dmnFileName);
            }
            
            // Store the release ID instead of trying to serialize bytes
            String releaseIdString = releaseId.toExternalForm();
            
            // Create cache directory if it doesn't exist
            Files.createDirectories(kjarPath.getParent());
            
            // Write release ID to file
            Files.write(kjarPath, releaseIdString.getBytes());
            
            System.out.println("Created KJAR for " + dmnFileName + " (ReleaseId: " + releaseIdString + ")");
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create KJAR for " + dmnFileName, e);
        }
    }

    private DMNRuntime loadDMNRuntimeFromKjar(Path kjarPath) {
        try {
            // Load release ID from file (new approach)
            String releaseIdString = new String(Files.readAllBytes(kjarPath)).trim();
            
            KieServices kieServices = KieServices.Factory.get();
            
            // Parse the release ID string (format: groupId:artifactId:version)
            String[] parts = releaseIdString.split(":");
            if (parts.length != 3) {
                throw new RuntimeException("Invalid release ID format: " + releaseIdString);
            }
            ReleaseId releaseId = kieServices.newReleaseId(parts[0], parts[1], parts[2]);
            
            // Create container from the release ID
            KieContainer kieContainer = kieServices.newKieContainer(releaseId);
            
            return kieContainer.newKieSession().getKieRuntime(DMNRuntime.class);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to load DMN runtime from KJAR: " + kjarPath, e);
        }
    }

    private Path getKjarPath(String dmnFileName) {
        String kjarFileName = dmnFileName.replace(".dmn", ".kjar");
        return Paths.get(KJAR_CACHE_DIR, kjarFileName);
    }

    private void cleanupKjarCache(String dmnFileName) {
        try {
            Path kjarPath = getKjarPath(dmnFileName);
            Files.deleteIfExists(kjarPath);
        } catch (Exception e) {
            // Ignore cleanup errors
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

    private void generateReport(List<PerformanceComparison> results) {
        System.out.println("\n=== KJAR PERFORMANCE ANALYSIS REPORT ===");
        
        System.out.println("\nDETAILED RESULTS:");
        System.out.println("DMN File                          | Traditional (ms) | KJAR Load (ms) | KJAR Creation (ms) | Build Improvement | Eval Improvement");
        System.out.println("----------------------------------|------------------|----------------|--------------------|--------------------|------------------");
        
        for (PerformanceComparison result : results) {
            if (result != null) {
                System.out.printf("%-32s | %14.2f | %12.2f | %16.2f | %16.1f%% | %15.1f%%%n",
                    result.dmnFileName,
                    result.traditionalBuildTimeMs,
                    result.kjarBuildTimeMs,
                    result.kjarCreationTimeMs,
                    result.buildTimeImprovement,
                    result.evalTimeImprovement);
            }
        }
        
        // Calculate summary statistics
        List<PerformanceComparison> validResults = results.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        if (!validResults.isEmpty()) {
            double avgBuildImprovement = validResults.stream()
                .mapToDouble(r -> r.buildTimeImprovement)
                .average()
                .orElse(0.0);
            
            double avgEvalImprovement = validResults.stream()
                .mapToDouble(r -> r.evalTimeImprovement)
                .average()
                .orElse(0.0);
            
            System.out.println("\nSUMMARY:");
            System.out.printf("Average Build Time Improvement: %.1f%%%n", avgBuildImprovement);
            System.out.printf("Average Evaluation Time Improvement: %.1f%%%n", avgEvalImprovement);
            
            // Find best improvements
            PerformanceComparison bestBuild = validResults.stream()
                .max(Comparator.comparing(r -> r.buildTimeImprovement))
                .orElse(null);
            
            PerformanceComparison bestEval = validResults.stream()
                .max(Comparator.comparing(r -> r.evalTimeImprovement))
                .orElse(null);
            
            if (bestBuild != null) {
                System.out.printf("Best Build Improvement: %.1f%% (%s)%n", 
                    bestBuild.buildTimeImprovement, bestBuild.dmnFileName);
            }
            
            if (bestEval != null) {
                System.out.printf("Best Evaluation Improvement: %.1f%% (%s)%n", 
                    bestEval.evalTimeImprovement, bestEval.dmnFileName);
            }
        }
        
        System.out.println("\nKEY INSIGHTS:");
        System.out.println("- KJAR creation is a one-time cost that pays off with faster subsequent loads");
        System.out.println("- For production use, KJARs should be created during build/deployment phase");
        System.out.println("- KJAR loading bypasses DMN parsing and compilation, reducing startup time");
        System.out.println("- Memory usage is also typically lower with precompiled KJARs");
    }

    private void exportToCsv(List<PerformanceComparison> results) {
        try (FileWriter writer = new FileWriter("dmn-kjar-performance-results.csv")) {
            // CSV Header
            writer.append("Timestamp,DMN_File,Traditional_Build_Ms,Traditional_Eval_Micros,")
                  .append("KJAR_Load_Ms,KJAR_Eval_Micros,KJAR_Creation_Ms,")
                  .append("Build_Improvement_Percent,Eval_Improvement_Percent,")
                  .append("Traditional_Success_Count,KJAR_Success_Count\n");
            
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            
            for (PerformanceComparison result : results) {
                if (result != null) {
                    writer.append(String.format("%s,%s,%.3f,%.3f,%.3f,%.3f,%.3f,%.2f,%.2f,%d,%d\n",
                        timestamp,
                        result.dmnFileName,
                        result.traditionalBuildTimeMs,
                        result.traditionalEvalTimeMicros,
                        result.kjarBuildTimeMs,
                        result.kjarEvalTimeMicros,
                        result.kjarCreationTimeMs,
                        result.buildTimeImprovement,
                        result.evalTimeImprovement,
                        result.traditionalSuccessCount,
                        result.kjarSuccessCount));
                }
            }
        } catch (IOException e) {
            System.err.println("Error writing CSV file: " + e.getMessage());
        }
    }

    // Data creation helper methods (same as other performance classes)
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
        vehicle.put("antiTheft", antitheftDevice);
        return vehicle;
    }

    private Map<String, Object> createDrivingHistory(int yearsLicensed, int accidents, int violations, int claims, int totalMileage) {
        Map<String, Object> history = new HashMap<>();
        history.put("yearsLicensed", yearsLicensed);
        history.put("accidentsLastThreeYears", accidents);
        history.put("violationsLastThreeYears", violations);
        history.put("claimsLastFiveYears", claims);
        history.put("milesPerYear", totalMileage);
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
        conditions.put("volatilityIndex", volatility);
        conditions.put("marketTrend", trend);
        conditions.put("interestRates", interestRates);
        conditions.put("inflationRate", inflationRate);
        conditions.put("economicOutlook", economicIndicators);
        conditions.put("sectorPerformance", sectorRotation);
        return conditions;
    }

    private Map<String, Object> createInvestmentGoals(String primaryObjective, double targetReturn, double maximumDrawdown, int incomeRequirement, String taxConsiderations, String esgPreferences) {
        Map<String, Object> goals = new HashMap<>();
        goals.put("primaryObjective", primaryObjective);
        goals.put("targetReturn", targetReturn);
        goals.put("maxDrawdown", maximumDrawdown);
        goals.put("investmentHorizon", incomeRequirement);
        goals.put("incomePriority", taxConsiderations);
        goals.put("growthPriority", esgPreferences);
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
        portfolio.put("geographicConcentration", internationalAllocation);
        return portfolio;
    }

    private Map<String, Object> createPatient(int age, String gender, double weight, double height, double bmi, String bloodPressure, int heartRate, double temperature, int oxygenSaturation) {
        Map<String, Object> patient = new HashMap<>();
        patient.put("age", age);
        patient.put("gender", gender);
        patient.put("weight", weight);
        patient.put("height", height);
        patient.put("bmi", bmi);
        patient.put("bloodPressureCategory", bloodPressure);
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
        history.put("recentSurgeries", previousSurgeries);
        history.put("familyHistory", familyHistory);
        history.put("smokingStatus", smokingStatus);
        history.put("alcoholUse", alcoholConsumption);
        return history;
    }

    private Map<String, Object> createSymptoms(String primaryComplaint, int symptomDuration, int severityScore, List<String> associatedSymptoms, int painLevel, String onsetType, String symptomPattern) {
        Map<String, Object> symptoms = new HashMap<>();
        symptoms.put("primaryComplaint", primaryComplaint);
        symptoms.put("durationHours", symptomDuration);
        symptoms.put("severityScale", severityScore);
        symptoms.put("associatedSymptoms", associatedSymptoms);
        symptoms.put("painScale", painLevel);
        symptoms.put("onsetType", onsetType);
        symptoms.put("pattern", symptomPattern);
        return symptoms;
    }

    private Map<String, Object> createLabResults(String completeBloodCount, String basicMetabolicPanel, String liverFunction, 
                                                String kidneyFunction, String cardiacMarkers, String inflammatoryMarkers, int glucoseLevel, double hemoglobinA1c) {
        Map<String, Object> labs = new HashMap<>();
        labs.put("bloodCount", completeBloodCount);
        labs.put("chemistryPanel", basicMetabolicPanel);
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

    // Inner classes for metrics and results
    public static class TraditionalMetrics {
        public final double avgBuildTimeMs;
        public final double avgEvalTimeMicros;
        public final int successfulEvaluations;

        public TraditionalMetrics(double avgBuildTimeMs, double avgEvalTimeMicros, int successfulEvaluations) {
            this.avgBuildTimeMs = avgBuildTimeMs;
            this.avgEvalTimeMicros = avgEvalTimeMicros;
            this.successfulEvaluations = successfulEvaluations;
        }
    }

    public static class KjarMetrics {
        public final double avgBuildTimeMs;
        public final double avgEvalTimeMicros;
        public final double kjarCreationTimeMs;
        public final double kjarLoadTimeMs;
        public final int successfulEvaluations;

        public KjarMetrics(double avgBuildTimeMs, double avgEvalTimeMicros, double kjarCreationTimeMs, double kjarLoadTimeMs, int successfulEvaluations) {
            this.avgBuildTimeMs = avgBuildTimeMs;
            this.avgEvalTimeMicros = avgEvalTimeMicros;
            this.kjarCreationTimeMs = kjarCreationTimeMs;
            this.kjarLoadTimeMs = kjarLoadTimeMs;
            this.successfulEvaluations = successfulEvaluations;
        }
    }

    public static class PerformanceComparison {
        public final String dmnFileName;
        public final double traditionalBuildTimeMs;
        public final double traditionalEvalTimeMicros;
        public final double kjarBuildTimeMs;
        public final double kjarEvalTimeMicros;
        public final double kjarCreationTimeMs;
        public final double kjarLoadTimeMs;
        public final double buildTimeImprovement;
        public final double evalTimeImprovement;
        public final int traditionalSuccessCount;
        public final int kjarSuccessCount;

        public PerformanceComparison(String dmnFileName, 
                                   double traditionalBuildTimeMs, double traditionalEvalTimeMicros,
                                   double kjarBuildTimeMs, double kjarEvalTimeMicros,
                                   double kjarCreationTimeMs, double kjarLoadTimeMs,
                                   double buildTimeImprovement, double evalTimeImprovement,
                                   int traditionalSuccessCount, int kjarSuccessCount) {
            this.dmnFileName = dmnFileName;
            this.traditionalBuildTimeMs = traditionalBuildTimeMs;
            this.traditionalEvalTimeMicros = traditionalEvalTimeMicros;
            this.kjarBuildTimeMs = kjarBuildTimeMs;
            this.kjarEvalTimeMicros = kjarEvalTimeMicros;
            this.kjarCreationTimeMs = kjarCreationTimeMs;
            this.kjarLoadTimeMs = kjarLoadTimeMs;
            this.buildTimeImprovement = buildTimeImprovement;
            this.evalTimeImprovement = evalTimeImprovement;
            this.traditionalSuccessCount = traditionalSuccessCount;
            this.kjarSuccessCount = kjarSuccessCount;
        }
    }
}
