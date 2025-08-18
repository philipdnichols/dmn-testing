package com.example.dmn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit tests for DMN evaluation functionality
 */
@DisplayName("DMN Evaluation Tests")
public class DmnEvaluationTest {

    private DMNRuntime dmnRuntime;

    @BeforeEach
    void setUp() {
        dmnRuntime = createDMNRuntime();
    }

    @Test
    @DisplayName("Should approve loan for eligible applicant with reasonable amount")
    void testEligibleApplicantReasonableAmount() {
        // Given
        Map<String, Object> applicant = createApplicant(25, 75000, 720);
        Map<String, Object> loan = createLoan(300000, 30);

        // When
        DMNResult result = evaluateDecision(applicant, loan);

        // Then
        assertFalse(result.hasErrors(), "DMN evaluation should not have errors");
        
        // With decision service, only output decisions are returned
        String approvalResult = getDecisionResult(result, "Loan Approval");
        
        assertEquals("APPROVED", approvalResult);
    }

    @Test
    @DisplayName("Should reject loan for applicant with low income")
    void testNotEligibleApplicantLowIncome() {
        // Given
        Map<String, Object> applicant = createApplicant(30, 40000, 720);
        Map<String, Object> loan = createLoan(200000, 30);

        // When
        DMNResult result = evaluateDecision(applicant, loan);

        // Then
        assertFalse(result.hasErrors(), "DMN evaluation should not have errors");
        
        // With decision service, only output decisions are returned
        String approvalResult = getDecisionResult(result, "Loan Approval");
        
        assertEquals("REJECTED", approvalResult);
    }

    @Test
    @DisplayName("Should reject loan for eligible applicant with high loan amount")
    void testEligibleApplicantHighLoanAmount() {
        // Given
        Map<String, Object> applicant = createApplicant(35, 100000, 750);
        Map<String, Object> loan = createLoan(600000, 30);

        // When
        DMNResult result = evaluateDecision(applicant, loan);

        // Then
        assertFalse(result.hasErrors(), "DMN evaluation should not have errors");
        
        // With decision service, only output decisions are returned
        String approvalResult = getDecisionResult(result, "Loan Approval");
        
        assertEquals("REJECTED", approvalResult);
    }

    @Test
    @DisplayName("Should reject loan for applicant with low credit score")
    void testNotEligibleApplicantLowCreditScore() {
        // Given
        Map<String, Object> applicant = createApplicant(28, 80000, 600);
        Map<String, Object> loan = createLoan(250000, 30);

        // When
        DMNResult result = evaluateDecision(applicant, loan);

        // Then
        assertFalse(result.hasErrors(), "DMN evaluation should not have errors");
        
        // With decision service, only output decisions are returned
        String approvalResult = getDecisionResult(result, "Loan Approval");
        
        assertEquals("REJECTED", approvalResult);
    }

    @Test
    @DisplayName("Should reject loan for underage applicant")
    void testUnderageApplicant() {
        // Given
        Map<String, Object> applicant = createApplicant(17, 80000, 750);
        Map<String, Object> loan = createLoan(200000, 30);

        // When
        DMNResult result = evaluateDecision(applicant, loan);

        // Then
        assertFalse(result.hasErrors(), "DMN evaluation should not have errors");
        
        // With decision service, only output decisions are returned
        String approvalResult = getDecisionResult(result, "Loan Approval");
        
        assertEquals("REJECTED", approvalResult);
    }

    private DMNResult evaluateDecision(Map<String, Object> applicant, Map<String, Object> loan) {
        DMNContext dmnContext = dmnRuntime.newContext();
        dmnContext.set("Applicant", applicant);
        dmnContext.set("Loan", loan);
        
        return dmnRuntime.evaluateDecisionService(dmnRuntime.getModels().get(0), dmnContext, "Loan Approval Service");
    }

    private String getDecisionResult(DMNResult result, String decisionName) {
        DMNDecisionResult decisionResult = result.getDecisionResultByName(decisionName);
        assertNotNull(decisionResult, "Decision result should not be null for: " + decisionName);
        return (String) decisionResult.getResult();
    }

    private DMNRuntime createDMNRuntime() {
        try {
            KieServices kieServices = KieServices.Factory.get();
            KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
            
            InputStream dmnStream = getClass().getClassLoader()
                .getResourceAsStream("loan-approval.dmn");
            
            assertNotNull(dmnStream, "Could not find loan-approval.dmn in resources");
            
            kieFileSystem.write("src/main/resources/loan-approval.dmn", 
                kieServices.getResources().newInputStreamResource(dmnStream));
            
            KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
            kieBuilder.buildAll();
            
            if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
                fail("Build errors: " + 
                    kieBuilder.getResults().getMessages(Message.Level.ERROR));
            }
            
            KieContainer kieContainer = kieServices.newKieContainer(
                kieBuilder.getKieModule().getReleaseId());
            
            return kieContainer.newKieSession().getKieRuntime(DMNRuntime.class);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create DMN runtime", e);
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
