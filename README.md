# DMN Testing with Drools 10.1.0

This project demonstrates DMN (Decision Model and Notation) evaluation functionality using Drools 10.1.0. It includes a sample loan approval decision model and Java code to evaluate decisions programmatically.

## Project Structure

```
dmn-testing/
├── pom.xml                                    # Maven configuration
├── README.md                                  # This file
├── src/
│   ├── main/
│   │   ├── java/com/example/dmn/
│   │   │   └── DmnEvaluationExample.java     # Main example class
│   │   └── resources/
│   │       └── loan-approval.dmn             # Sample DMN model
│   └── test/
│       └── java/com/example/dmn/
│           └── DmnEvaluationTest.java        # JUnit test cases
```

## Prerequisites

- Java 17 or higher
- Maven 3.6.0 or higher

## Version Notes

This project uses Drools 10.1.0, which provides the latest DMN evaluation capabilities and excellent performance for decision model execution.

## Getting Started

### 1. Build the Project

```bash
mvn clean compile
```

### 2. Run the Example

Execute the main example class to see DMN evaluation in action:

```bash
mvn exec:java -Dexec.mainClass="com.example.dmn.DmnEvaluationExample"
```

### 3. Run the Tests

Execute the JUnit tests to verify DMN functionality:

```bash
mvn test
```

## DMN Model Overview

The included `loan-approval.dmn` model demonstrates a simple loan approval process with the following components:

### Input Data
- **Applicant**: Contains age, income, and credit score
- **Loan**: Contains amount and term

### Decisions
1. **Eligibility Check**: Determines if an applicant is eligible based on:
   - Age >= 18
   - Income >= 50,000
   - Credit Score >= 650

2. **Loan Approval**: Final approval decision based on:
   - Eligibility status
   - Loan amount <= 500,000

### Decision Logic

| Eligibility | Loan Amount | Result |
|-------------|-------------|---------|
| ELIGIBLE | <= 500,000 | APPROVED |
| ELIGIBLE | > 500,000 | REJECTED |
| NOT_ELIGIBLE | Any | REJECTED |

## Example Usage

The project includes several test cases demonstrating different scenarios:

### Test Case 1: Eligible Applicant with Reasonable Loan
```java
// Applicant: age=25, income=75000, creditScore=720
// Loan: amount=300000, term=30
// Expected: ELIGIBLE -> APPROVED
```

### Test Case 2: Low Income Applicant
```java
// Applicant: age=30, income=40000, creditScore=720
// Loan: amount=200000, term=30
// Expected: NOT_ELIGIBLE -> REJECTED
```

### Test Case 3: High Loan Amount
```java
// Applicant: age=35, income=100000, creditScore=750
// Loan: amount=600000, term=30
// Expected: ELIGIBLE -> REJECTED (amount too high)
```

## Key Dependencies

- **Drools Core** (10.1.0): Core rule engine
- **Drools Compiler** (10.1.0): Rule compilation engine
- **KIE DMN API** (10.1.0): DMN API for decision management
- **KIE DMN Core** (10.1.0): Core DMN implementation
- **KIE DMN FEEL** (10.1.0): FEEL expression language support
- **JUnit Jupiter** (5.10.0): Unit testing framework

## DMN Evaluation Process

The code demonstrates the typical DMN evaluation workflow:

1. **Load DMN Model**: Read the DMN file and build the model
2. **Create Context**: Set up input data (Applicant and Loan information)
3. **Evaluate Decisions**: Execute the decision logic
4. **Extract Results**: Retrieve decision outcomes

```java
// Create DMN runtime
DMNRuntime dmnRuntime = createDMNRuntime();

// Set up context with input data
DMNContext context = dmnRuntime.newContext();
context.set("Applicant", applicantData);
context.set("Loan", loanData);

// Evaluate all decisions
DMNResult result = dmnRuntime.evaluateAll(model, context);

// Get specific decision results
String approval = result.getDecisionResultByName("Loan Approval").getResult();
```

## Extending the Model

To extend or modify the DMN model:

1. Edit `src/main/resources/loan-approval.dmn` or create new DMN files
2. Update the Java code to load your new model
3. Modify input data structures as needed
4. Add corresponding test cases

## Common DMN Patterns

This project demonstrates several common DMN patterns:

- **Decision Tables**: Rule-based decision logic
- **Information Requirements**: Dependencies between decisions
- **Input Data**: External data inputs to decisions
- **FEEL Expressions**: Functional expression language usage
- **Hit Policies**: UNIQUE hit policy for decision tables

## Troubleshooting

### Build Issues
- Ensure Java 11+ is installed and configured
- Verify Maven is properly installed
- Check internet connectivity for dependency downloads

### Runtime Issues
- Verify DMN file is in the classpath (`src/main/resources`)
- Check for DMN model validation errors in console output
- Ensure input data structure matches DMN model expectations

## Further Reading

- [DMN Specification](https://www.omg.org/dmn/)
- [Drools Documentation](https://docs.drools.org/)
- [KIE DMN Documentation](https://docs.drools.org/latest/index.html#dmn-con_dmn-models)
