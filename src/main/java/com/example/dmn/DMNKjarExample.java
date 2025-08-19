package com.example.dmn;

import org.kie.dmn.api.core.DMNContext;
import org.kie.dmn.api.core.DMNResult;
import org.kie.dmn.api.core.DMNRuntime;

import java.util.*;

/**
 * Example demonstrating how to use the DMNKjarManager for optimized DMN evaluation.
 * This class shows the practical benefits of KJAR-based optimization in various scenarios.
 */
public class DMNKjarExample {

    private final DMNKjarManager kjarManager;

    public DMNKjarExample() {
        this.kjarManager = new DMNKjarManager();
    }

    public static void main(String[] args) {
        DMNKjarExample example = new DMNKjarExample();
        
        System.out.println("=== DMN KJAR Usage Examples ===\n");
        
        // Example 1: Precompilation for faster startup
        example.demonstratePrecompilation();
        
        // Example 2: Runtime optimization
        example.demonstrateRuntimeOptimization();
        
        // Example 3: Multi-DMN scenarios
        example.demonstrateMultiDMNScenarios();
        
        // Example 4: Cache management
        example.demonstrateCacheManagement();
        
        System.out.println("\n=== Examples Complete ===");
    }

    /**
     * Demonstrates precompilation for faster application startup.
     * This is typically done during application initialization.
     */
    public void demonstratePrecompilation() {
        System.out.println("1. PRECOMPILATION EXAMPLE");
        System.out.println("Precompiling all DMN files to KJARs...");
        
        long startTime = System.nanoTime();
        
        // Precompile all DMN files
        kjarManager.precompileAllDMNs(
            "loan-approval.dmn",
            "insurance-risk-assessment.dmn",
            "supply-chain-optimization.dmn",
            "financial-portfolio-analysis.dmn",
            "healthcare-treatment-protocol.dmn"
        );
        
        long endTime = System.nanoTime();
        double totalTimeMs = (endTime - startTime) / 1_000_000.0;
        
        System.out.printf("Precompilation completed in %.2f ms%n", totalTimeMs);
        System.out.println("All subsequent runtime creation will be faster!\n");
    }

    /**
     * Demonstrates the runtime performance benefits of KJAR optimization.
     */
    public void demonstrateRuntimeOptimization() {
        System.out.println("2. RUNTIME OPTIMIZATION EXAMPLE");
        
        String dmnFile = "loan-approval.dmn";
        int iterations = 5;
        
        System.out.printf("Creating DMN runtime %d times for %s...%n", iterations, dmnFile);
        
        List<Long> runtimeCreationTimes = new ArrayList<>();
        
        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            
            // Get DMN runtime (uses KJAR cache after first call)
            DMNRuntime runtime = kjarManager.getDMNRuntime(dmnFile);
            
            long endTime = System.nanoTime();
            long creationTimeMs = (endTime - startTime) / 1_000_000;
            runtimeCreationTimes.add(creationTimeMs);
            
            System.out.printf("  Iteration %d: %d ms%n", i + 1, creationTimeMs);
            
            // Perform a sample evaluation to verify functionality
            performSampleLoanEvaluation(runtime);
        }
        
        // Calculate statistics
        long firstTime = runtimeCreationTimes.get(0);
        long subsequentAvg = runtimeCreationTimes.stream().skip(1).mapToLong(Long::longValue).sum() / (iterations - 1);
        
        System.out.printf("First creation: %d ms (includes KJAR compilation)%n", firstTime);
        System.out.printf("Subsequent average: %d ms (KJAR loading only)%n", subsequentAvg);
        System.out.printf("Average improvement: %.1fx faster%n", (double) firstTime / subsequentAvg);
        System.out.println();
    }

    /**
     * Demonstrates using multiple DMN files in a single runtime.
     */
    public void demonstrateMultiDMNScenarios() {
        System.out.println("3. MULTI-DMN SCENARIOS");
        
        // Scenario 1: Financial analysis requiring multiple DMN models
        List<String> financialDMNs = Arrays.asList(
            "loan-approval.dmn",
            "insurance-risk-assessment.dmn",
            "financial-portfolio-analysis.dmn"
        );
        
        System.out.println("Creating combined runtime for financial analysis...");
        long startTime = System.nanoTime();
        
        DMNRuntime combinedRuntime = kjarManager.getDMNRuntime(financialDMNs);
        
        long endTime = System.nanoTime();
        double creationTimeMs = (endTime - startTime) / 1_000_000.0;
        
        System.out.printf("Combined runtime created in %.2f ms%n", creationTimeMs);
        System.out.printf("Runtime contains %d DMN models%n", combinedRuntime.getModels().size());
        
        // Scenario 2: Subsequent access to the same combination
        System.out.println("Accessing the same combination again (should be faster)...");
        
        startTime = System.nanoTime();
        kjarManager.getDMNRuntime(financialDMNs);
        endTime = System.nanoTime();
        
        double cachedTimeMs = (endTime - startTime) / 1_000_000.0;
        System.out.printf("Cached runtime accessed in %.2f ms%n", cachedTimeMs);
        System.out.printf("Cache improvement: %.1fx faster%n", creationTimeMs / cachedTimeMs);
        System.out.println();
    }

    /**
     * Demonstrates cache management capabilities.
     */
    public void demonstrateCacheManagement() {
        System.out.println("4. CACHE MANAGEMENT");
        
        // Show initial cache statistics
        DMNKjarManager.CacheStatistics initialStats = kjarManager.getCacheStatistics();
        System.out.println("Initial cache state: " + initialStats);
        
        // Create some runtimes to populate cache
        kjarManager.getDMNRuntime("loan-approval.dmn");
        kjarManager.getDMNRuntime("insurance-risk-assessment.dmn");
        
        // Show updated statistics
        DMNKjarManager.CacheStatistics populatedStats = kjarManager.getCacheStatistics();
        System.out.println("After creating runtimes: " + populatedStats);
        
        // Demonstrate memory cache clearing
        System.out.println("Clearing memory cache...");
        kjarManager.clearMemoryCache();
        
        DMNKjarManager.CacheStatistics afterMemoryClear = kjarManager.getCacheStatistics();
        System.out.println("After memory clear: " + afterMemoryClear);
        
        // Access runtime again (should reload from disk KJAR)
        System.out.println("Accessing runtime after memory clear (loads from disk KJAR)...");
        long startTime = System.nanoTime();
        kjarManager.getDMNRuntime("loan-approval.dmn");
        long endTime = System.nanoTime();
        
        double reloadTimeMs = (endTime - startTime) / 1_000_000.0;
        System.out.printf("Reload from disk KJAR: %.2f ms%n", reloadTimeMs);
        
        System.out.println("Cache management demonstration complete.");
    }

    /**
     * Performs a sample loan evaluation to verify DMN runtime functionality.
     */
    private void performSampleLoanEvaluation(DMNRuntime runtime) {
        try {
            DMNContext context = runtime.newContext();
            
            // Set up test data
            Map<String, Object> applicant = new HashMap<>();
            applicant.put("age", 30);
            applicant.put("income", 80000);
            applicant.put("creditScore", 750);
            
            Map<String, Object> loan = new HashMap<>();
            loan.put("amount", 250000);
            loan.put("term", 30);
            
            context.set("Applicant", applicant);
            context.set("Loan", loan);
            
            // Evaluate the decision service
            DMNResult result = runtime.evaluateDecisionService(
                runtime.getModels().get(0), 
                context, 
                "Loan Approval Service"
            );
            
            if (result.hasErrors()) {
                System.err.println("DMN evaluation errors: " + result.getMessages());
            } else {
                // Evaluation successful (we don't need to print results for this demo)
            }
            
        } catch (Exception e) {
            System.err.println("Sample evaluation failed: " + e.getMessage());
        }
    }

    /**
     * Utility method to demonstrate the difference between traditional and KJAR approaches.
     */
    public void compareApproaches() {
        System.out.println("5. TRADITIONAL vs KJAR COMPARISON");
        
        String dmnFile = "supply-chain-optimization.dmn";
        int iterations = 3;
        
        // Traditional approach (for comparison)
        System.out.println("Traditional approach (building from scratch each time):");
        List<Long> traditionalTimes = new ArrayList<>();
        
        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            
            // This would use the traditional createDMNRuntime method
            // DMNRuntime runtime = createTraditionalDMNRuntime(dmnFile);
            
            long endTime = System.nanoTime();
            long timeMs = (endTime - startTime) / 1_000_000;
            traditionalTimes.add(timeMs);
            
            System.out.printf("  Traditional iteration %d: %d ms%n", i + 1, timeMs);
        }
        
        // KJAR approach
        System.out.println("KJAR approach (cached compilation):");
        List<Long> kjarTimes = new ArrayList<>();
        
        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            
            kjarManager.getDMNRuntime(dmnFile);
            
            long endTime = System.nanoTime();
            long timeMs = (endTime - startTime) / 1_000_000;
            kjarTimes.add(timeMs);
            
            System.out.printf("  KJAR iteration %d: %d ms%n", i + 1, timeMs);
        }
        
        // Calculate improvements
        double traditionalAvg = traditionalTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double kjarAvg = kjarTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        
        if (traditionalAvg > 0 && kjarAvg > 0) {
            double improvement = ((traditionalAvg - kjarAvg) / traditionalAvg) * 100;
            System.out.printf("Average improvement with KJAR: %.1f%% faster%n", improvement);
        }
        
        System.out.println();
    }
}
