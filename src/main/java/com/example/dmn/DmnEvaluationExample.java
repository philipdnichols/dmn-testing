package com.example.dmn;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.dmn.api.core.DMNContext;
import org.kie.dmn.api.core.DMNDecisionResult;
import org.kie.dmn.api.core.DMNResult;
import org.kie.dmn.api.core.DMNRuntime;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Example class demonstrating DMN evaluation using Drools 10.1.0 with performance benchmarking
 */
public class DmnEvaluationExample {

    private static final int WARMUP_ITERATIONS = 10;
    private static final int PERFORMANCE_ITERATIONS = 100;

    public static void main(String[] args) {
        DmnEvaluationExample example = new DmnEvaluationExample();
        
        System.out.println("=== DMN Performance Evaluation Suite ===");
        System.out.println("Warming up JVM with " + WARMUP_ITERATIONS + " iterations...");
        System.out.println("Performance tests with " + PERFORMANCE_ITERATIONS + " iterations each");
        System.out.println();
        
        example.runLoanApprovalExample();
        example.runInsuranceRiskAssessmentExample();
        example.runSupplyChainOptimizationExample();
        example.runFinancialPortfolioAnalysisExample();
        example.runHealthcareTreatmentProtocolExample();
        
        System.out.println("\n=== Performance Summary Complete ===");
    }

    public void runLoanApprovalExample() {
        try {
            System.out.println("=== Loan Approval DMN Performance Test ===");
            
            // Load and build the DMN model
            DMNRuntime dmnRuntime = createDMNRuntime("loan-approval.dmn");
            
            // Test case: Eligible applicant with reasonable loan amount
            System.out.println("Testing loan approval scenario...");
            Map<String, Object> applicant = createApplicant(25, 75000, 720);
            Map<String, Object> loan = createLoan(300000, 30);
            
            // Performance test
            double buildTime = measureBuildTime("loan-approval.dmn");
            double evaluationTime = measureEvaluationTime(dmnRuntime, () -> {
                DMNContext dmnContext = dmnRuntime.newContext();
                dmnContext.set("Applicant", applicant);
                dmnContext.set("Loan", loan);
                return dmnRuntime.evaluateDecisionService(dmnRuntime.getModels().get(0), dmnContext, "Loan Approval Service");
            });
            
            System.out.printf("Build Time: %.2f μs, Average Evaluation Time: %.2f μs%n", buildTime, evaluationTime);
                
        } catch (Exception e) {
            System.err.println("Error running loan approval DMN: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void runInsuranceRiskAssessmentExample() {
        try {
            System.out.println("\n=== Insurance Risk Assessment DMN Performance Test ===");
            
            DMNRuntime dmnRuntime = createDMNRuntime("insurance-risk-assessment.dmn");
            
            System.out.println("Testing insurance risk assessment scenario...");
            
            // Performance test
            double buildTime = measureBuildTime("insurance-risk-assessment.dmn");
            double evaluationTime = measureEvaluationTime(dmnRuntime, () -> {
                DMNContext context = dmnRuntime.newContext();
                context.set("Person", createPerson(35, "FEMALE", "ENGINEER", "MARRIED", 75000, 750));
                context.set("Vehicle", createVehicle("TOYOTA", "CAMRY", 2019, 25000, 5, true));
                context.set("DrivingHistory", createDrivingHistory(15, 0, 1, 0, 150000));
                context.set("Coverage", createCoverage(500000, true, true, 1000));
                return dmnRuntime.evaluateDecisionService(dmnRuntime.getModels().get(0), context, "Insurance Risk Assessment Service");
            });
            
            System.out.printf("Build Time: %.2f μs, Average Evaluation Time: %.2f μs%n", buildTime, evaluationTime);
                
        } catch (Exception e) {
            System.err.println("Error running insurance risk assessment DMN: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void runSupplyChainOptimizationExample() {
        try {
            System.out.println("\n=== Supply Chain Optimization DMN Performance Test ===");
            
            DMNRuntime dmnRuntime = createDMNRuntime("supply-chain-optimization.dmn");
            
            System.out.println("Testing supply chain optimization scenario...");
            
            // Performance test
            double buildTime = measureBuildTime("supply-chain-optimization.dmn");
            double evaluationTime = measureEvaluationTime(dmnRuntime, () -> {
                DMNContext context = dmnRuntime.newContext();
                context.set("Product", createProduct("P002", "ELECTRONICS", 1.2, 0.05, 8000, "MEDIUM", false));
                context.set("Order", createOrder("O002", 50, "HIGH", "2024-12-25", "CHICAGO", "GOLD"));
                context.set("Supplier", createSupplier("S002", "TEXAS", 8.0, 45.0, 10, 500, 8.5));
                context.set("Warehouse", createWarehouse("W002", "ILLINOIS", 300, 800, 1000, 18, false));
                context.set("TransportRoute", createTransportRoute("R002", "ILLINOIS", "CHICAGO", 50, 2.0, 4, 2.0));
                return dmnRuntime.evaluateDecisionService(dmnRuntime.getModels().get(0), context, "Supply Chain Optimization Service");
            });
            
            System.out.printf("Build Time: %.2f μs, Average Evaluation Time: %.2f μs%n", buildTime, evaluationTime);
                
        } catch (Exception e) {
            System.err.println("Error running supply chain optimization DMN: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void runFinancialPortfolioAnalysisExample() {
        try {
            System.out.println("\n=== Financial Portfolio Analysis DMN Performance Test ===");
            
            DMNRuntime dmnRuntime = createDMNRuntime("financial-portfolio-analysis.dmn");
            
            System.out.println("Testing financial portfolio analysis scenario...");
            
            // Performance test
            double buildTime = measureBuildTime("financial-portfolio-analysis.dmn");
            double evaluationTime = measureEvaluationTime(dmnRuntime, () -> {
                DMNContext context = dmnRuntime.newContext();
                context.set("InvestorProfile", createInvestorProfile(45, 100000, 1200000, "MEDIUM", 20, "HIGH", "EXPERIENCED"));
                context.set("MarketConditions", createMarketConditions(22, "SIDEWAYS", 3.8, 2.5, "MODERATE", "MIXED"));
                context.set("InvestmentGoals", createInvestmentGoals("BALANCED_GROWTH", 8.0, 15.0, 8000, "MEDIUM_PRIORITY", "MEDIUM_PRIORITY"));
                context.set("CurrentPortfolio", createCurrentPortfolio(1000000, 60, 30, 10, 5, 25, 20));
                return dmnRuntime.evaluateDecisionService(dmnRuntime.getModels().get(0), context, "Portfolio Analysis Service");
            });
            
            System.out.printf("Build Time: %.2f μs, Average Evaluation Time: %.2f μs%n", buildTime, evaluationTime);
                
        } catch (Exception e) {
            System.err.println("Error running financial portfolio analysis DMN: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void runHealthcareTreatmentProtocolExample() {
        try {
            System.out.println("\n=== Healthcare Treatment Protocol DMN Performance Test ===");
            
            DMNRuntime dmnRuntime = createDMNRuntime("healthcare-treatment-protocol.dmn");
            
            System.out.println("Testing healthcare treatment protocol scenario...");
            
            // Performance test
            double buildTime = measureBuildTime("healthcare-treatment-protocol.dmn");
            double evaluationTime = measureEvaluationTime(dmnRuntime, () -> {
                DMNContext context = dmnRuntime.newContext();
                context.set("Patient", createPatient(35, "FEMALE", 65, 165, 23.9, "NORMAL", 75, 36.8, 98));
                context.set("MedicalHistory", createMedicalHistory(List.of(), List.of(), List.of(), List.of(), List.of(), "NEVER_SMOKER", "NONE"));
                context.set("Symptoms", createSymptoms("MILD_PAIN", 7, 4, List.of("FATIGUE"), 3, "GRADUAL", "INTERMITTENT"));
                context.set("LabResults", createLabResults("NORMAL", "NORMAL", "NORMAL", "NORMAL", "NORMAL", "NORMAL", 95, 5.2));
                context.set("ImagingResults", createImagingResults("NORMAL", "NOT_DONE", "NOT_DONE", "NOT_DONE", "NORMAL", "NOT_DONE"));
                return dmnRuntime.evaluateDecisionService(dmnRuntime.getModels().get(0), context, "Healthcare Treatment Protocol Service");
            });
            
            System.out.printf("Build Time: %.2f μs, Average Evaluation Time: %.2f μs%n", buildTime, evaluationTime);
                
        } catch (Exception e) {
            System.err.println("Error running healthcare treatment protocol DMN: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private DMNRuntime createDMNRuntime(String dmnFileName) {
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
                System.err.println("Build errors:");
                kieBuilder.getResults().getMessages(Message.Level.ERROR)
                    .forEach(message -> System.err.println(message.getText()));
                throw new RuntimeException("Failed to build DMN model: " + dmnFileName);
            }
            
            KieContainer kieContainer = kieServices.newKieContainer(
                kieBuilder.getKieModule().getReleaseId());
            
            return kieContainer.newKieSession().getKieRuntime(DMNRuntime.class);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create DMN runtime for " + dmnFileName, e);
        }
    }

    private double measureBuildTime(String dmnFileName) {
        long startTime = System.nanoTime();
        createDMNRuntime(dmnFileName);
        long endTime = System.nanoTime();
        return (endTime - startTime) / 1000.0; // Convert to microseconds
    }

    private double measureEvaluationTime(DMNRuntime runtime, EvaluationTask task) {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            try {
                task.evaluate();
            } catch (Exception e) {
                System.err.println("Warmup iteration failed: " + e.getMessage());
            }
        }

        // Performance measurement
        long totalTime = 0;
        for (int i = 0; i < PERFORMANCE_ITERATIONS; i++) {
            long startTime = System.nanoTime();
            try {
                task.evaluate();
            } catch (Exception e) {
                System.err.println("Performance iteration failed: " + e.getMessage());
            }
            long endTime = System.nanoTime();
            totalTime += (endTime - startTime);
        }

        return (totalTime / 1000.0) / PERFORMANCE_ITERATIONS; // Convert to microseconds
    }

    @FunctionalInterface
    private interface EvaluationTask {
        DMNResult evaluate();
    }

    // Data creation helper methods
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
        vehicle.put("antitheftDevice", antitheftDevice);
        return vehicle;
    }

    private Map<String, Object> createDrivingHistory(int yearsLicensed, int accidents, int violations, int claims, int totalMileage) {
        Map<String, Object> history = new HashMap<>();
        history.put("yearsLicensed", yearsLicensed);
        history.put("accidents", accidents);
        history.put("violations", violations);
        history.put("claims", claims);
        history.put("totalMileage", totalMileage);
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
        conditions.put("volatility", volatility);
        conditions.put("trend", trend);
        conditions.put("interestRates", interestRates);
        conditions.put("inflationRate", inflationRate);
        conditions.put("economicIndicators", economicIndicators);
        conditions.put("sectorRotation", sectorRotation);
        return conditions;
    }

    private Map<String, Object> createInvestmentGoals(String primaryObjective, double targetReturn, double maximumDrawdown, int incomeRequirement, String taxConsiderations, String esgPreferences) {
        Map<String, Object> goals = new HashMap<>();
        goals.put("primaryObjective", primaryObjective);
        goals.put("targetReturn", targetReturn);
        goals.put("maximumDrawdown", maximumDrawdown);
        goals.put("incomeRequirement", incomeRequirement);
        goals.put("taxConsiderations", taxConsiderations);
        goals.put("esgPreferences", esgPreferences);
        return goals;
    }

    private Map<String, Object> createCurrentPortfolio(int totalValue, int equityAllocation, int bondAllocation, int alternativeAllocation, int cashAllocation, int internationalAllocation, int sectorConcentration) {
        Map<String, Object> portfolio = new HashMap<>();
        portfolio.put("totalValue", totalValue);
        portfolio.put("equityAllocation", equityAllocation);
        portfolio.put("bondAllocation", bondAllocation);
        portfolio.put("alternativeAllocation", alternativeAllocation);
        portfolio.put("cashAllocation", cashAllocation);
        portfolio.put("internationalAllocation", internationalAllocation);
        portfolio.put("sectorConcentration", sectorConcentration);
        return portfolio;
    }

    private Map<String, Object> createPatient(int age, String gender, double weight, double height, double bmi, String bloodPressure, int heartRate, double temperature, int oxygenSaturation) {
        Map<String, Object> patient = new HashMap<>();
        patient.put("age", age);
        patient.put("gender", gender);
        patient.put("weight", weight);
        patient.put("height", height);
        patient.put("bmi", bmi);
        patient.put("bloodPressure", bloodPressure);
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

