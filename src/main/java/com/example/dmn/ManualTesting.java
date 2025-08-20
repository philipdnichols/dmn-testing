package com.example.dmn;

import org.kie.api.KieServices;
import org.kie.api.builder.*;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.io.Resource;
import org.kie.internal.io.ResourceFactory;
import org.kie.dmn.api.core.DMNContext;
import org.kie.dmn.api.core.DMNModel;
import org.kie.dmn.api.core.DMNResult;
import org.kie.dmn.api.core.DMNRuntime;
import org.kie.dmn.core.internal.utils.DMNRuntimeBuilder;


import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.HdrHistogram.Histogram;

public class ManualTesting {
    
    /**
     * A utility class for timing method executions using lambda expressions with statistical analysis.
     * Provides both void methods (Runnable) and methods that return values (Supplier).
     * Uses HdrHistogram for accurate latency measurements and percentile calculations.
     */
    public static class TimingUtil {
        
        private static final int DEFAULT_ITERATIONS = 100;
        private static final int DEFAULT_WARMUP_ITERATIONS = 10;
        
        /**
         * Times a method that returns a value with comprehensive statistics
         * @param operation The operation to time
         * @param operationName A descriptive name for the operation
         * @param iterations Number of times to run the operation
         * @param warmupIterations Number of warmup iterations before measurement
         * @param <T> The return type of the operation
         * @return StatisticalTimedResult containing result and comprehensive timing statistics
         */
        public static <T> StatisticalTimedResult<T> timeOperationWithStats(
                Supplier<T> operation, String operationName, int iterations, int warmupIterations) {
            
            // Warmup phase
            System.out.printf("[TIMING] Warming up %s (%d iterations)...%n", operationName, warmupIterations);
            T result = null;
            for (int i = 0; i < warmupIterations; i++) {
                result = operation.get();
            }
            
            // Measurement phase
            System.out.printf("[TIMING] Measuring %s (%d iterations)...%n", operationName, iterations);
            Histogram histogram = new Histogram(TimeUnit.SECONDS.toNanos(60), 3); // Track up to 60 seconds with 3 decimal precision
            
            for (int i = 0; i < iterations; i++) {
                long startTime = System.nanoTime();
                result = operation.get();
                long endTime = System.nanoTime();
                long durationNanos = endTime - startTime;
                histogram.recordValue(durationNanos);
            }
            
            TimingStatistics stats = new TimingStatistics(histogram);
            printStatistics(operationName, stats);
            
            return new StatisticalTimedResult<>(result, stats);
        }
        
        /**
         * Times a method that returns a value with default iterations
         */
        public static <T> StatisticalTimedResult<T> timeOperationWithStats(Supplier<T> operation, String operationName) {
            return timeOperationWithStats(operation, operationName, DEFAULT_ITERATIONS, DEFAULT_WARMUP_ITERATIONS);
        }
        
        /**
         * Times a void method with comprehensive statistics
         * @param operation The operation to time
         * @param operationName A descriptive name for the operation
         * @param iterations Number of times to run the operation
         * @param warmupIterations Number of warmup iterations before measurement
         * @return TimingStatistics containing comprehensive timing information
         */
        public static TimingStatistics timeVoidOperationWithStats(
                Runnable operation, String operationName, int iterations, int warmupIterations) {
            
            // Warmup phase
            System.out.printf("[TIMING] Warming up %s (%d iterations)...%n", operationName, warmupIterations);
            for (int i = 0; i < warmupIterations; i++) {
                operation.run();
            }
            
            // Measurement phase
            System.out.printf("[TIMING] Measuring %s (%d iterations)...%n", operationName, iterations);
            Histogram histogram = new Histogram(TimeUnit.SECONDS.toNanos(60), 3);
            
            for (int i = 0; i < iterations; i++) {
                long startTime = System.nanoTime();
                operation.run();
                long endTime = System.nanoTime();
                long durationNanos = endTime - startTime;
                histogram.recordValue(durationNanos);
            }
            
            TimingStatistics stats = new TimingStatistics(histogram);
            printStatistics(operationName, stats);
            
            return stats;
        }
        
        /**
         * Times a void method with default iterations
         */
        public static TimingStatistics timeVoidOperationWithStats(Runnable operation, String operationName) {
            return timeVoidOperationWithStats(operation, operationName, DEFAULT_ITERATIONS, DEFAULT_WARMUP_ITERATIONS);
        }
        
        /**
         * Legacy method for single timing (backwards compatibility)
         */
        public static <T> TimedResult<T> timeOperation(Supplier<T> operation, String operationName) {
            long startTime = System.nanoTime();
            T result = operation.get();
            long endTime = System.nanoTime();
            long durationNanos = endTime - startTime;
            long durationMillis = TimeUnit.NANOSECONDS.toMillis(durationNanos);
            
            System.out.printf("[TIMING] %s took %d ms (%.2f seconds)%n", 
                operationName, durationMillis, durationNanos / 1_000_000_000.0);
            
            return new TimedResult<>(result, durationNanos, durationMillis);
        }
        
        /**
         * Legacy method for single timing (backwards compatibility)
         */
        public static TimingInfo timeVoidOperation(Runnable operation, String operationName) {
            long startTime = System.nanoTime();
            operation.run();
            long endTime = System.nanoTime();
            long durationNanos = endTime - startTime;
            long durationMillis = TimeUnit.NANOSECONDS.toMillis(durationNanos);
            
            System.out.printf("[TIMING] %s took %d ms (%.2f seconds)%n", 
                operationName, durationMillis, durationNanos / 1_000_000_000.0);
            
            return new TimingInfo(durationNanos, durationMillis);
        }
        
        private static void printStatistics(String operationName, TimingStatistics stats) {
            System.out.printf("=== %s Statistics ===%n", operationName);
            System.out.printf("Iterations: %d%n", stats.getTotalCount());
            System.out.printf("Average:    %.3f ms%n", stats.getMeanMs());
            System.out.printf("Median:     %.3f ms%n", stats.getMedianMs());
            System.out.printf("P95:        %.3f ms%n", stats.getP95Ms());
            System.out.printf("P99:        %.3f ms%n", stats.getP99Ms());
            System.out.printf("P99.9:      %.3f ms%n", stats.getP999Ms());
            System.out.printf("Min:        %.3f ms%n", stats.getMinMs());
            System.out.printf("Max:        %.3f ms%n", stats.getMaxMs());
            System.out.printf("Std Dev:    %.3f ms%n", stats.getStdDeviationMs());
            System.out.println("================================");
        }
    }
    
    /**
     * Container for timing information only
     */
    public static class TimingInfo {
        public final long durationNanos;
        public final long durationMillis;
        
        public TimingInfo(long durationNanos, long durationMillis) {
            this.durationNanos = durationNanos;
            this.durationMillis = durationMillis;
        }
    }
    
    /**
     * Container for a timed operation result and timing information
     */
    public static class TimedResult<T> {
        public final T result;
        public final long durationNanos;
        public final long durationMillis;
        
        public TimedResult(T result, long durationNanos, long durationMillis) {
            this.result = result;
            this.durationNanos = durationNanos;
            this.durationMillis = durationMillis;
        }
    }
    
    /**
     * Container for comprehensive timing statistics using HdrHistogram
     */
    public static class TimingStatistics {
        private final Histogram histogram;
        
        public TimingStatistics(Histogram histogram) {
            this.histogram = histogram;
        }
        
        public long getTotalCount() {
            return histogram.getTotalCount();
        }
        
        public double getMeanMs() {
            return histogram.getMean() / 1_000_000.0;
        }
        
        public double getMedianMs() {
            return histogram.getValueAtPercentile(50.0) / 1_000_000.0;
        }
        
        public double getP95Ms() {
            return histogram.getValueAtPercentile(95.0) / 1_000_000.0;
        }
        
        public double getP99Ms() {
            return histogram.getValueAtPercentile(99.0) / 1_000_000.0;
        }
        
        public double getP999Ms() {
            return histogram.getValueAtPercentile(99.9) / 1_000_000.0;
        }
        
        public double getMinMs() {
            return histogram.getMinValue() / 1_000_000.0;
        }
        
        public double getMaxMs() {
            return histogram.getMaxValue() / 1_000_000.0;
        }
        
        public double getStdDeviationMs() {
            return histogram.getStdDeviation() / 1_000_000.0;
        }
        
        public Histogram getHistogram() {
            return histogram;
        }
    }
    
    /**
     * Container for a timed operation result with comprehensive statistics
     */
    public static class StatisticalTimedResult<T> {
        public final T result;
        public final TimingStatistics statistics;
        
        public StatisticalTimedResult(T result, TimingStatistics statistics) {
            this.result = result;
            this.statistics = statistics;
        }
    }

    private static final DMN[] DMNS = {
            new DMN("loan-approval.dmn", "loan-approval", "https://example.com/dmn", "Loan Approval Service"),
            new DMN("insurance-risk-assessment.dmn", "Insurance Risk Assessment", "https://example.com/dmn/insurance", "Insurance Risk Assessment Service"),
            new DMN("supply-chain-optimization.dmn", "Supply Chain Optimization", "https://example.com/dmn/supply-chain", "Supply Chain Optimization Service"),
            new DMN("financial-portfolio-analysis.dmn", "Financial Portfolio Analysis", "https://example.com/dmn/financial", "Portfolio Analysis Service"),
            new DMN("healthcare-treatment-protocol.dmn", "Healthcare Treatment Protocol", "https://example.com/dmn/healthcare", "Healthcare Treatment Protocol Service")
    };

    public static void main(String[] args) {
        List<DMN> dmns = Arrays.stream(DMNS).toList();

        System.out.println("=== Enhanced DMN Performance Analysis with Statistical Timing ===");
        System.out.println();

        // Use the enhanced timing utility to measure DMN runtime building with comprehensive statistics
        StatisticalTimedResult<DMNRuntime> timedResultBuilder = TimingUtil.timeOperationWithStats(
            () -> {
                try {
                    return buildDMNRuntimeUsingDMNRuntimeBuilder(dmns);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            },
            "DMN Runtime Building (DMNRuntimeBuilder)",
            50,  // Run 50 iterations for statistical accuracy
            5    // 5 warmup iterations
        );
        DMNRuntime dmnRuntimeBuilder = timedResultBuilder.result;

        // Use the enhanced timing utility to measure DMN evaluation with comprehensive statistics
        TimingStatistics evaluationStats = TimingUtil.timeVoidOperationWithStats(
            () -> evaluateDmns(dmns, dmnRuntimeBuilder),
            "DMN Evaluation (DMNRuntimeBuilder Runtime)",
            100, // Run 100 iterations for statistical accuracy
            10   // 10 warmup iterations
        );

        // Optional: demonstrate with default iterations (100 measurements, 10 warmups)
        // System.out.println("\n=== Quick Test with Default Settings ===");
        // TimingStatistics quickStats = TimingUtil.timeVoidOperationWithStats(
        //     () -> evaluateDmns(dmns, dmnRuntimeBuilder),
        //     "DMN Evaluation (Quick Test)"
        // );

        // Optional: legacy single-run timing for comparison
        // System.out.println("\n=== Legacy Single-Run Timing for Comparison ===");
        // TimingInfo legacyTiming = TimingUtil.timeVoidOperation(
        //     () -> evaluateDmns(dmns, dmnRuntimeBuilder),
        //     "DMN Evaluation (Single Run)"
        // );
    }

    private static DMNRuntime buildDMNRuntimeUsingKieServices(List<DMN> dmns) {
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

        dmns.forEach(dmn -> {
            try {
                InputStream dmnStream = ManualTesting.class.getClassLoader().getResourceAsStream(dmn.fileName);
                if (dmnStream == null) {
                    throw new RuntimeException("Could not find " + dmn.fileName + " in resources");
                }

                // Read the InputStream into a byte array
                byte[] dmnBytes = dmnStream.readAllBytes();
                dmnStream.close();

                // Create a ByteArrayResource manually using KIE's ResourceFactory
                Resource resource = ResourceFactory.newByteArrayResource(dmnBytes);
                resource.setSourcePath(dmn.fileName);

                kieFileSystem.write("src/main/resources/" + dmn.fileName, resource);
            } catch (Exception e) {
                throw new RuntimeException("Failed to read DMN file: " + dmn.fileName, e);
            }
        });

        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();
        KieModule kieModule = kieBuilder.getKieModule();
        ReleaseId releaseId = kieModule.getReleaseId();

        Results results = kieBuilder.getResults();
        // System.out.println(results.toString());
        if (results.hasMessages(Message.Level.ERROR)) {
            throw new RuntimeException("Failed to build DMN Runtime");
        }

        KieContainer kieContainer = kieServices.newKieContainer(releaseId);

        try (KieSession kieSession = kieContainer.newKieSession()) {
            return kieSession.getKieRuntime(DMNRuntime.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static DMNRuntime buildDMNRuntimeUsingDMNRuntimeBuilder(List<DMN> dmns) throws Exception {
        // Build the resource collection from the DMN list
        List<Resource> resources = new ArrayList<>();
        
        for (DMN dmn : dmns) {
            InputStream dmnStream = ManualTesting.class.getClassLoader().getResourceAsStream(dmn.fileName);
            if (dmnStream == null) {
                throw new RuntimeException("Could not find " + dmn.fileName + " in resources");
            }
            
            // Read the InputStream into a byte array
            byte[] dmnBytes = dmnStream.readAllBytes();
            dmnStream.close();
            
            // Create a ByteArrayResource manually using KIE's ResourceFactory
            Resource resource = ResourceFactory.newByteArrayResource(dmnBytes);
            resource.setSourcePath(dmn.fileName);
            resources.add(resource);
        }
        
        // Build the DMN runtime using the resource collection
        return DMNRuntimeBuilder.fromDefaults()
            .buildConfiguration()
            .fromResources(resources)
            .getOrElseThrow(Exception::new);
    }

    private static void evaluateDmns(List<DMN> dmns, DMNRuntime dmnRuntime) {
        dmns.forEach(dmn -> {
            DMNModel dmnModel = dmnRuntime.getModel(dmn.namespace, dmn.name);
            DMNContext dmnContext = createTestContext(dmnRuntime, dmn.fileName);
            DMNResult dmnResult = dmnRuntime.evaluateDecisionService(dmnModel, dmnContext, dmn.decisionServiceName);
            // Suppressed verbose DMN result logging for performance testing
            // To see results, uncomment the line below:
            // printCleanResults(dmn.decisionServiceName, dmnResult);
        });
    }
    
    /**
     * Utility method to print DMN results in a clean, readable format
     * (instead of the verbose DMNDecisionResultImpl toString output)
     */
    private static void printCleanResults(String serviceName, DMNResult dmnResult) {
        System.out.println("=== " + serviceName + " Results ===");
        if (dmnResult.hasErrors()) {
            System.out.println("ERRORS:");
            dmnResult.getMessages().forEach(msg -> System.out.println("  " + msg));
        } else {
            dmnResult.getDecisionResults().forEach(result -> 
                System.out.printf("  %-30s: %s%n", result.getDecisionName(), result.getResult()));
        }
        System.out.println();
    }

    private static DMNContext createTestContext(DMNRuntime runtime, String dmnFileName) {
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

    private static Map<String, Object> createApplicant(int age, int income, int creditScore) {
        Map<String, Object> applicant = new HashMap<>();
        applicant.put("age", age);
        applicant.put("income", income);
        applicant.put("creditScore", creditScore);
        return applicant;
    }

    private static Map<String, Object> createLoan(int amount, int term) {
        Map<String, Object> loan = new HashMap<>();
        loan.put("amount", amount);
        loan.put("term", term);
        return loan;
    }

    private static Map<String, Object> createPerson(int age, String gender, String occupation, String maritalStatus, int annualIncome, int creditScore) {
        Map<String, Object> person = new HashMap<>();
        person.put("age", age);
        person.put("gender", gender);
        person.put("occupation", occupation);
        person.put("maritalStatus", maritalStatus);
        person.put("annualIncome", annualIncome);
        person.put("creditScore", creditScore);
        return person;
    }

    private static Map<String, Object> createVehicle(String make, String model, int year, int value, int safetyRating, boolean antitheftDevice) {
        Map<String, Object> vehicle = new HashMap<>();
        vehicle.put("make", make);
        vehicle.put("model", model);
        vehicle.put("year", year);
        vehicle.put("value", value);
        vehicle.put("safetyRating", safetyRating);
        vehicle.put("antiTheft", antitheftDevice);  // Fixed: was "antitheftDevice"
        return vehicle;
    }

    private static Map<String, Object> createDrivingHistory(int yearsLicensed, int accidents, int violations, int claims, int totalMileage) {
        Map<String, Object> history = new HashMap<>();
        history.put("yearsLicensed", yearsLicensed);
        history.put("accidentsLastThreeYears", accidents);    // Fixed: was "accidents"
        history.put("violationsLastThreeYears", violations);  // Fixed: was "violations"
        history.put("claimsLastFiveYears", claims);           // Fixed: was "claims"
        history.put("milesPerYear", totalMileage);            // Fixed: was "totalMileage"
        return history;
    }

    private static Map<String, Object> createCoverage(int liability, boolean comprehensive, boolean collision, int deductible) {
        Map<String, Object> coverage = new HashMap<>();
        coverage.put("liability", liability);
        coverage.put("comprehensive", comprehensive);
        coverage.put("collision", collision);
        coverage.put("deductible", deductible);
        return coverage;
    }

    private static Map<String, Object> createProduct(String id, String category, double weight, double volume, int value, String fragility, boolean temperatureSensitive) {
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

    private static Map<String, Object> createOrder(String id, int quantity, String priority, String deadline, String destination, String customerTier) {
        Map<String, Object> order = new HashMap<>();
        order.put("id", id);
        order.put("quantity", quantity);
        order.put("priority", priority);
        order.put("deadline", deadline);
        order.put("destination", destination);
        order.put("customerTier", customerTier);
        return order;
    }

    private static Map<String, Object> createSupplier(String id, String location, double reliabilityScore, double costPerUnit, int leadTime, int capacity, double qualityRating) {
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

    private static Map<String, Object> createWarehouse(String id, String location, int currentInventory, int maxCapacity, int operatingCost, int processingTime, boolean temperatureControlled) {
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

    private static Map<String, Object> createTransportRoute(String id, String from, String to, int distance, double costPerKm, int transitTime, double riskFactor) {
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

    private static Map<String, Object> createInvestorProfile(int age, int annualIncome, int netWorth, String riskTolerance, int investmentHorizon, String liquidityNeeds, String investmentExperience) {
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

    private static Map<String, Object> createMarketConditions(double volatility, String trend, double interestRates, double inflationRate, String economicIndicators, String sectorRotation) {
        Map<String, Object> conditions = new HashMap<>();
        conditions.put("volatilityIndex", volatility);  // Fixed: was "volatility"
        conditions.put("marketTrend", trend);           // Fixed: was "trend"
        conditions.put("interestRates", interestRates);
        conditions.put("inflationRate", inflationRate);
        conditions.put("economicOutlook", economicIndicators);  // Fixed: was "economicIndicators"
        conditions.put("sectorPerformance", sectorRotation);    // Fixed: was "sectorRotation"
        return conditions;
    }

    private static Map<String, Object> createInvestmentGoals(String primaryObjective, double targetReturn, double maximumDrawdown, int incomeRequirement, String taxConsiderations, String esgPreferences) {
        Map<String, Object> goals = new HashMap<>();
        goals.put("primaryObjective", primaryObjective);
        goals.put("targetReturn", targetReturn);
        goals.put("maxDrawdown", maximumDrawdown);        // Fixed: was "maximumDrawdown"
        goals.put("investmentHorizon", incomeRequirement); // Fixed: was "incomeRequirement", using as horizon
        goals.put("incomePriority", taxConsiderations);   // Fixed: was "taxConsiderations"
        goals.put("growthPriority", esgPreferences);      // Fixed: was "esgPreferences"
        return goals;
    }

    private static Map<String, Object> createCurrentPortfolio(int totalValue, int equityAllocation, int bondAllocation, int alternativeAllocation, int cashAllocation, int internationalAllocation, int sectorConcentration) {
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

    private static Map<String, Object> createPatient(int age, String gender, double weight, double height, double bmi, String bloodPressure, int heartRate, double temperature, int oxygenSaturation) {
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

    private static Map<String, Object> createMedicalHistory(List<String> chronicConditions, List<String> allergies, List<String> currentMedications,
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

    private static Map<String, Object> createSymptoms(String primaryComplaint, int symptomDuration, int severityScore, List<String> associatedSymptoms, int painLevel, String onsetType, String symptomPattern) {
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

    private static Map<String, Object> createLabResults(String completeBloodCount, String basicMetabolicPanel, String liverFunction,
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

    private static Map<String, Object> createImagingResults(String chestXray, String ctScan, String mri, String ultrasound, String ecg, String echocardiogram) {
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
