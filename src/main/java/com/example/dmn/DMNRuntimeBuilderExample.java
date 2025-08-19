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

import java.io.InputStream;
import java.util.*;

/**
 * DMN Evaluation Example using DMNRuntimeBuilder-style configuration
 * with specified optimization settings including Alpha Network, lenient mode,
 * FEEL profile, and other performance configurations.
 */
public class DMNRuntimeBuilderExample {

    private static final String[] DMN_FILES = {
        "loan-approval.dmn",
        "insurance-risk-assessment.dmn", 
        "supply-chain-optimization.dmn",
        "financial-portfolio-analysis.dmn",
        "healthcare-treatment-protocol.dmn"
    };

    private static final int WARMUP_ITERATIONS = 5;
    private static final int MEASUREMENT_ITERATIONS = 20;
    private static final int PERFORMANCE_ITERATIONS = 10;

    public static void main(String[] args) {
        DMNRuntimeBuilderExample example = new DMNRuntimeBuilderExample();
        
        System.out.println("=== DMN Runtime Builder Configuration Example ===");
        System.out.println("Testing all 5 DMN files with optimized configuration:");
        System.out.println("- Alpha Network: ENABLED");
        System.out.println("- Runtime Mode: LENIENT");
        System.out.println("- Decision Service Coerce Singleton: TRUE");
        System.out.println("- Exec Model Compiler: TRUE");
        System.out.println("- Runtime Type Checking: FALSE");
        System.out.println("- FEEL Profile: DoCompileFEELProfile");
        System.out.println();
        
        // Test each DMN file individually
        for (String dmnFile : DMN_FILES) {
            example.evaluateDMNFile(dmnFile);
            System.out.println();
        }
        
        // Test all DMN files in a unified runtime
        example.evaluateAllDMNFilesUnified();
        
        System.out.println("=== Evaluation Complete ===");
    }

    /**
     * Evaluates a single DMN file with the specified configuration
     */
    public void evaluateDMNFile(String dmnFileName) {
        try {
            System.out.println("=== Evaluating: " + dmnFileName + " ===");
            
            DMNRuntime dmnRuntime = createOptimizedDMNRuntime(dmnFileName);
            
            // Performance measurement
            double buildTime = measureBuildTime(dmnFileName);
            double evaluationTime = measureEvaluationTime(dmnRuntime, dmnFileName);
            
            System.out.printf("File: %s%n", dmnFileName);
            System.out.printf("Build Time: %.2f μs%n", buildTime);
            System.out.printf("Average Evaluation Time: %.2f μs%n", evaluationTime);
            
            // Demonstrate actual evaluation
            demonstrateEvaluation(dmnRuntime, dmnFileName);
            
        } catch (Exception e) {
            System.err.println("Error evaluating " + dmnFileName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Evaluates all DMN files in a single unified runtime
     */
    public void evaluateAllDMNFilesUnified() {
        try {
            System.out.println("=== Unified Runtime with All DMN Files ===");
            
            DMNRuntime unifiedRuntime = createUnifiedDMNRuntime(DMN_FILES);
            
            System.out.printf("Unified runtime created with %d models%n", unifiedRuntime.getModels().size());
            
            // Test each model in the unified runtime with appropriate context
            for (String dmnFile : DMN_FILES) {
                try {
                    // Find the corresponding model for this DMN file
                    DMNModel targetModel = unifiedRuntime.getModels().stream()
                        .filter(model -> dmnFile.contains(model.getName()) || 
                                       model.getName().contains(dmnFile.replace(".dmn", "")))
                        .findFirst()
                        .orElse(null);
                    
                    if (targetModel != null) {
                        double evaluationTime = measureEvaluationTimeForModel(unifiedRuntime, targetModel, dmnFile);
                        System.out.printf("  %s: %.2f μs%n", dmnFile, evaluationTime);
                    } else {
                        System.out.printf("  %s: MODEL NOT FOUND%n", dmnFile);
                    }
                } catch (Exception e) {
                    System.err.printf("  %s: FAILED - %s%n", dmnFile, e.getMessage());
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error creating unified runtime: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Creates an optimized DMN runtime with the specified configuration settings
     */
    private DMNRuntime createOptimizedDMNRuntime(String dmnFileName) {
        // Store original system properties to restore later
        Properties originalProps = storeOriginalProperties();
        
        try {
            // Configure optimization properties
            setOptimizationProperties();
            
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
            restoreOriginalProperties(originalProps);
        }
    }

    /**
     * Creates a unified DMN runtime containing all specified DMN files
     */
    private DMNRuntime createUnifiedDMNRuntime(String[] dmnFileNames) {
        Properties originalProps = storeOriginalProperties();
        
        try {
            setOptimizationProperties();
            
            KieServices kieServices = KieServices.Factory.get();
            KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
            
            // Load all DMN files into the unified runtime
            for (String dmnFileName : dmnFileNames) {
                InputStream dmnStream = getClass().getClassLoader().getResourceAsStream(dmnFileName);
                if (dmnStream == null) {
                    System.err.println("Warning: Could not find " + dmnFileName + " in resources");
                    continue;
                }
                
                kieFileSystem.write("src/main/resources/" + dmnFileName, 
                    kieServices.getResources().newInputStreamResource(dmnStream));
            }
            
            KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
            kieBuilder.buildAll();
            
            if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
                System.err.println("Build errors in unified runtime:");
                kieBuilder.getResults().getMessages(Message.Level.ERROR)
                    .forEach(message -> System.err.println("  " + message.getText()));
                throw new RuntimeException("Failed to build unified DMN runtime");
            }
            
            KieContainer kieContainer = kieServices.newKieContainer(
                kieBuilder.getKieModule().getReleaseId());
            
            return kieContainer.newKieSession().getKieRuntime(DMNRuntime.class);
            
        } finally {
            restoreOriginalProperties(originalProps);
        }
    }

    /**
     * Sets the optimization properties as specified:
     * - Alpha Network: enabled
     * - Runtime Mode: lenient  
     * - Decision Service Coerce Singleton: true
     * - Exec Model Compiler: true
     * - Runtime Type Checking: false
     * - FEEL Profile: DoCompileFEELProfile
     */
    private void setOptimizationProperties() {
        // Alpha Network option
        System.setProperty("org.kie.dmn.alphanetwork.enabled", "true");
        
        // Lenient runtime mode (opposite of strict mode)
        System.setProperty("org.kie.dmn.strict", "false");
        
        // Decision Service Coerce Singleton
        System.setProperty("org.kie.dmn.decisionservice.coercesingleton", "true");
        
        // Executable Model Compiler
        System.setProperty("org.kie.dmn.compiler.execmodel", "true");
        
        // Runtime Type Checking (disabled)
        System.setProperty("org.kie.dmn.runtime.typecheck", "false");
        
        // FEEL Profile - DoCompileFEELProfile
        System.setProperty("org.kie.dmn.feel.profile", "DoCompileFEELProfile");
        
        // Additional optimization properties for better performance
        System.setProperty("org.kie.dmn.feel.compilation", "true");
        System.setProperty("org.kie.dmn.strictmode", "false");
    }

    /**
     * Stores original system properties for restoration
     */
    private Properties storeOriginalProperties() {
        Properties originalProps = new Properties();
        
        String[] propertiesToStore = {
            "org.kie.dmn.alphanetwork.enabled",
            "org.kie.dmn.strict", 
            "org.kie.dmn.decisionservice.coercesingleton",
            "org.kie.dmn.compiler.execmodel",
            "org.kie.dmn.runtime.typecheck",
            "org.kie.dmn.feel.profile",
            "org.kie.dmn.feel.compilation",
            "org.kie.dmn.strictmode"
        };
        
        for (String prop : propertiesToStore) {
            String value = System.getProperty(prop);
            if (value != null) {
                originalProps.setProperty(prop, value);
            }
        }
        
        return originalProps;
    }

    /**
     * Restores original system properties
     */
    private void restoreOriginalProperties(Properties originalProps) {
        String[] propertiesToRestore = {
            "org.kie.dmn.alphanetwork.enabled",
            "org.kie.dmn.strict",
            "org.kie.dmn.decisionservice.coercesingleton", 
            "org.kie.dmn.compiler.execmodel",
            "org.kie.dmn.runtime.typecheck",
            "org.kie.dmn.feel.profile",
            "org.kie.dmn.feel.compilation",
            "org.kie.dmn.strictmode"
        };
        
        for (String prop : propertiesToRestore) {
            if (originalProps.containsKey(prop)) {
                System.setProperty(prop, originalProps.getProperty(prop));
            } else {
                System.clearProperty(prop);
            }
        }
    }

    /**
     * Measures the build time for a DMN runtime
     */
    private double measureBuildTime(String dmnFileName) {
        long startTime = System.nanoTime();
        createOptimizedDMNRuntime(dmnFileName);
        long endTime = System.nanoTime();
        return (endTime - startTime) / 1000.0; // Convert to microseconds
    }

    /**
     * Measures the evaluation time for a DMN runtime
     */
    private double measureEvaluationTime(DMNRuntime runtime, String dmnFileName) {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            try {
                performSampleEvaluation(runtime, dmnFileName);
            } catch (Exception e) {
                // Ignore warmup failures
            }
        }

        // Performance measurement
        long totalTime = 0;
        int successfulRuns = 0;
        
        for (int i = 0; i < PERFORMANCE_ITERATIONS; i++) {
            long startTime = System.nanoTime();
            try {
                performSampleEvaluation(runtime, dmnFileName);
                long endTime = System.nanoTime();
                totalTime += (endTime - startTime);
                successfulRuns++;
            } catch (Exception e) {
                // Count failed runs but don't include their time
            }
        }

        if (successfulRuns == 0) {
            return -1; // Indicate failure
        }
        
        return (totalTime / 1000.0) / successfulRuns; // Convert to microseconds
    }

    /**
     * Measures evaluation time for a specific model in a unified runtime
     */
    private double measureEvaluationTimeForModel(DMNRuntime runtime, DMNModel targetModel, String dmnFileName) {
        long totalTime = 0;
        int successfulRuns = 0;
        
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            try {
                DMNContext context = runtime.newContext();
                populateContextForDMN(context, dmnFileName);
                
                long startTime = System.nanoTime();
                runtime.evaluateAll(targetModel, context);
                long endTime = System.nanoTime();
                
                totalTime += (endTime - startTime);
                successfulRuns++;
            } catch (Exception e) {
                // Skip failed runs for timing measurement
            }
        }
        
        if (successfulRuns == 0) {
            throw new RuntimeException("All evaluation attempts failed");
        }
        
        return (totalTime / 1000.0) / successfulRuns; // Convert to microseconds
    }

    /**
     * Performs a sample evaluation for performance testing
     */
    private DMNResult performSampleEvaluation(DMNRuntime runtime, String dmnFileName) {
        if (runtime.getModels().isEmpty()) {
            throw new RuntimeException("No models available in runtime");
        }
        
        DMNContext context = runtime.newContext();
        
        // Populate context with sample data based on DMN file
        populateContextForDMN(context, dmnFileName);
        
        // Find the specific model for the DMN file, or use the first available
        DMNModel targetModel = runtime.getModels().stream()
            .filter(model -> dmnFileName.contains(model.getName()) || 
                           model.getName().contains(dmnFileName.replace(".dmn", "")))
            .findFirst()
            .orElse(runtime.getModels().get(0));
        
        return runtime.evaluateAll(targetModel, context);
    }

    /**
     * Demonstrates actual evaluation with meaningful output
     */
    private void demonstrateEvaluation(DMNRuntime runtime, String dmnFileName) {
        try {
            if (runtime.getModels().isEmpty()) {
                System.out.println("  No models loaded");
                return;
            }
            
            DMNContext context = runtime.newContext();
            populateContextForDMN(context, dmnFileName);
            
            // Find the specific model for the DMN file, or use the first available
            DMNModel targetModel = runtime.getModels().stream()
                .filter(model -> dmnFileName.contains(model.getName()) || 
                               model.getName().contains(dmnFileName.replace(".dmn", "")))
                .findFirst()
                .orElse(runtime.getModels().get(0));
            
            DMNResult result = runtime.evaluateAll(targetModel, context);
            
            System.out.println("  Evaluation Status: " + 
                (result.hasErrors() ? "ERRORS" : "SUCCESS"));
            
            if (result.hasErrors()) {
                System.out.println("  Errors:");
                result.getMessages().forEach(msg -> 
                    System.out.println("    " + msg.getText()));
            }
            
            System.out.println("  Decision Results: " + result.getDecisionResults().size());
            
        } catch (Exception e) {
            System.out.println("  Evaluation failed: " + e.getMessage());
        }
    }

    /**
     * Populates DMN context with sample data appropriate for each DMN file
     */
    private void populateContextForDMN(DMNContext context, String dmnFileName) {
        switch (dmnFileName) {
            case "loan-approval.dmn":
                context.set("Applicant", createApplicant(30, 80000, 750));
                context.set("Loan", createLoan(250000, 30));
                break;
                
            case "insurance-risk-assessment.dmn":
                // Modified values to avoid overlapping rules in Driver Risk Score table
                context.set("Person", createPerson(25, "MALE", "ENGINEER", "MARRIED", 75000, 750));
                context.set("Vehicle", createVehicle("TOYOTA", "CAMRY", 2020, 25000, 5, true));
                context.set("DrivingHistory", createDrivingHistory(7, 0, 0, 0, 12000)); // Changed yearsLicensed to 7, violations to 0
                context.set("Coverage", createCoverage(500000, true, true, 1000));
                break;
                
            case "supply-chain-optimization.dmn":
                context.set("Product", createProduct("P001", "ELECTRONICS", 1.5, 0.1, 5000, "LOW", false));
                context.set("Order", createOrder("O001", 100, "HIGH", "2024-12-31", "NEW_YORK", "GOLD"));
                context.set("Supplier", createSupplier("S001", "CALIFORNIA", 8.5, 50.0, 7, 1000, 9.0));
                context.set("Warehouse", createWarehouse("W001", "NEW_YORK", 500, 1000, 2000, 24, false));
                context.set("TransportRoute", createTransportRoute("R001", "CALIFORNIA", "NEW_YORK", 2500, 1.5, 72, 2.5));
                break;
                
            case "financial-portfolio-analysis.dmn":
                context.set("InvestorProfile", createInvestorProfile(45, 120000, 1500000, "MEDIUM", 20, "MEDIUM", "EXPERIENCED"));
                context.set("MarketConditions", createMarketConditions(18, "BULLISH", 4.0, 2.0, "POSITIVE", "GROWTH"));
                context.set("InvestmentGoals", createInvestmentGoals("GROWTH", 8.0, 15.0, 10000, "LOW_PRIORITY", "MEDIUM_PRIORITY"));
                context.set("CurrentPortfolio", createCurrentPortfolio(1200000, 70, 20, 10, 0, 30, 15));
                break;
                
            case "healthcare-treatment-protocol.dmn":
                context.set("Patient", createPatient(28, "FEMALE", 60, 165, 22.0, "NORMAL", 72, 36.6, 99));
                context.set("MedicalHistory", createMedicalHistory(List.of(), List.of(), List.of(), List.of(), List.of(), "NEVER_SMOKER", "NONE"));
                context.set("Symptoms", createSymptoms("MILD_PAIN", 3, 3, List.of("HEADACHE"), 2, "GRADUAL", "INTERMITTENT"));
                context.set("LabResults", createLabResults("NORMAL", "NORMAL", "NORMAL", "NORMAL", "NORMAL", "NORMAL", 90, 5.0));
                context.set("ImagingResults", createImagingResults("NORMAL", "NOT_DONE", "NOT_DONE", "NOT_DONE", "NORMAL", "NOT_DONE"));
                break;
                
            default:
                // Default empty context
                break;
        }
    }

    // Helper methods for creating sample data structures
    // (Reused from existing DmnEvaluationExample with same signatures)
    
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

    private Map<String, Object> createDrivingHistory(int yearsLicensed, int accidents, int violations, int claims, int milesPerYear) {
        Map<String, Object> history = new HashMap<>();
        history.put("yearsLicensed", yearsLicensed);
        history.put("accidentsLastThreeYears", accidents);
        history.put("violationsLastThreeYears", violations);
        history.put("claimsLastFiveYears", claims);
        history.put("milesPerYear", milesPerYear);
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
        profile.put("investmentExperience", investmentExperience);
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
        history.put("previousSurgeries", previousSurgeries);
        history.put("familyHistory", familyHistory);
        history.put("smokingStatus", smokingStatus);
        history.put("alcoholConsumption", alcoholConsumption);
        return history;
    }

    private Map<String, Object> createSymptoms(String primaryComplaint, int symptomDuration, int severityScore, List<String> associatedSymptoms, int painLevel, String onsetType, String symptomPattern) {
        Map<String, Object> symptoms = new HashMap<>();
        symptoms.put("primaryComplaint", primaryComplaint);
        symptoms.put("symptomDuration", symptomDuration);
        symptoms.put("severityScore", severityScore);
        symptoms.put("associatedSymptoms", associatedSymptoms);
        symptoms.put("painLevel", painLevel);
        symptoms.put("onsetType", onsetType);
        symptoms.put("symptomPattern", symptomPattern);
        return symptoms;
    }

    private Map<String, Object> createLabResults(String completeBloodCount, String basicMetabolicPanel, String liverFunction, 
                                                String kidneyFunction, String cardiacMarkers, String inflammatoryMarkers, int glucoseLevel, double hemoglobinA1c) {
        Map<String, Object> labs = new HashMap<>();
        labs.put("completeBloodCount", completeBloodCount);
        labs.put("basicMetabolicPanel", basicMetabolicPanel);
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
}
