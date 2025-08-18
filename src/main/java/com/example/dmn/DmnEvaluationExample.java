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
import java.util.HashMap;
import java.util.Map;

/**
 * Example class demonstrating DMN evaluation using Drools 10.1.0
 */
public class DmnEvaluationExample {

    public static void main(String[] args) {
        DmnEvaluationExample example = new DmnEvaluationExample();
        example.runLoanApprovalExample();
    }

    public void runLoanApprovalExample() {
        try {
            System.out.println("=== DMN Evaluation Example with Drools 10.1.0 ===");
            
            // Load and build the DMN model
            DMNRuntime dmnRuntime = createDMNRuntime();
            
            // Test case 1: Eligible applicant with reasonable loan amount
            System.out.println("\n--- Test Case 1: Eligible Applicant ---");
            testLoanApproval(dmnRuntime, 
                createApplicant(25, 75000, 720), 
                createLoan(300000, 30));
            
            // Test case 2: Not eligible applicant (low income)
            System.out.println("\n--- Test Case 2: Not Eligible Applicant (Low Income) ---");
            testLoanApproval(dmnRuntime, 
                createApplicant(30, 40000, 720), 
                createLoan(200000, 30));
            
            // Test case 3: Eligible applicant but loan amount too high
            System.out.println("\n--- Test Case 3: Eligible Applicant, High Loan Amount ---");
            testLoanApproval(dmnRuntime, 
                createApplicant(35, 100000, 750), 
                createLoan(600000, 30));
            
            // Test case 4: Not eligible applicant (low credit score)
            System.out.println("\n--- Test Case 4: Not Eligible Applicant (Low Credit Score) ---");
            testLoanApproval(dmnRuntime, 
                createApplicant(28, 80000, 600), 
                createLoan(250000, 30));
                
        } catch (Exception e) {
            System.err.println("Error running DMN evaluation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private DMNRuntime createDMNRuntime() {
        try {
            KieServices kieServices = KieServices.Factory.get();
            KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
            
            // Load the DMN file from resources
            InputStream dmnStream = getClass().getClassLoader()
                .getResourceAsStream("loan-approval.dmn");
            
            if (dmnStream == null) {
                throw new RuntimeException("Could not find loan-approval.dmn in resources");
            }
            
            kieFileSystem.write("src/main/resources/loan-approval.dmn", 
                kieServices.getResources().newInputStreamResource(dmnStream));
            
            KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
            kieBuilder.buildAll();
            
            if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
                System.err.println("Build errors:");
                kieBuilder.getResults().getMessages(Message.Level.ERROR)
                    .forEach(message -> System.err.println(message.getText()));
                throw new RuntimeException("Failed to build DMN model");
            }
            
            KieContainer kieContainer = kieServices.newKieContainer(
                kieBuilder.getKieModule().getReleaseId());
            
            return kieContainer.newKieSession().getKieRuntime(DMNRuntime.class);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create DMN runtime", e);
        }
    }

    private void testLoanApproval(DMNRuntime dmnRuntime, Map<String, Object> applicant, Map<String, Object> loan) {
        try {
            DMNContext dmnContext = dmnRuntime.newContext();
            dmnContext.set("Applicant", applicant);
            dmnContext.set("Loan", loan);
            
            System.out.println("Input:");
            System.out.println("  Applicant: " + applicant);
            System.out.println("  Loan: " + loan);
            
            // Evaluate all decisions in the model
            DMNResult dmnResult = dmnRuntime.evaluateAll(
                dmnRuntime.getModels().get(0), dmnContext);
            
            if (dmnResult.hasErrors()) {
                System.err.println("DMN evaluation errors:");
                dmnResult.getMessages().forEach(message -> 
                    System.err.println("  " + message.getText()));
                return;
            }
            
            // Display results
            System.out.println("Results:");
            for (DMNDecisionResult decisionResult : dmnResult.getDecisionResults()) {
                System.out.println("  " + decisionResult.getDecisionName() + ": " + 
                    decisionResult.getResult());
            }
            
        } catch (Exception e) {
            System.err.println("Error evaluating DMN: " + e.getMessage());
            e.printStackTrace();
        }
    }

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
}
