# DMN Performance Optimization Guide

This guide explains how to use the comprehensive DMN performance comparison framework to optimize your DMN runtime configurations.

## Quick Start

1. **Build the project:**
   ```bash
   mvn clean compile
   ```

2. **Run performance comparison:**
   ```bash
   mvn exec:java@performance-comparison
   ```

3. **Analyze results:**
   ```bash
   mvn exec:java@performance-analysis
   ```

## Understanding the Results

### Configuration Numbers
Each configuration (0-15) represents a combination of optimization options:
- **Config 0**: No optimizations (baseline)
- **Config 15**: All optimizations enabled
- **Others**: Various combinations of the 4 optimization flags

### Performance Metrics
- **Build Time**: Time to compile the DMN model (milliseconds)
- **Evaluation Time**: Time to evaluate decisions (microseconds)
- **Success Rate**: Percentage of successful evaluations
- **Percentiles**: P95 and P99 evaluation times for understanding outliers

### Optimization Options

| Option | Description | When to Enable |
|--------|-------------|----------------|
| Alpha Network | Compiles rule networks for faster matching | Large rule sets with many conditions |
| Runtime Type Check | Validates data types during execution | Development/testing phase |
| Lenient Mode | Relaxes strict validation rules | When model compatibility is needed |
| FEEL Compilation | Pre-compiles FEEL expressions | Complex mathematical expressions |

## Interpreting Results

### Sample Output Analysis
```
Config | AlphaNet | TypeCheck | Lenient | FEEL | Build(ms) | Eval(μs) | Success%
-------|----------|-----------|---------|------|-----------|----------|--------
     0 | NO       | NO        | NO      | NO   |     15.23 |   125.45 |   100.0
     1 | YES      | NO        | NO      | NO   |     18.67 |   98.32  |   100.0
    15 | YES      | YES       | YES     | YES  |     25.43 |   87.21  |   100.0
```

### Key Insights:
- **Config 0** provides the baseline performance
- **Config 1** shows Alpha Network impact (faster evaluation, slightly slower build)
- **Config 15** shows combined optimizations effect

## Recommended Configurations

Based on comprehensive testing across 5 complex DMN models, here are the proven optimal configurations:

### **🏆 Production Recommendations:**
- **Config 9** (Alpha=ON, TypeCheck=OFF, Lenient=OFF, FEEL=ON): **Best overall performance** - 17.35μs average
- **Config 12** (Alpha=OFF, TypeCheck=OFF, Lenient=ON, FEEL=ON): **Alternative optimum** - 17.53μs average
- **Config 8** (Alpha=OFF, TypeCheck=OFF, Lenient=OFF, FEEL=ON): **Simple optimum** - 17.64μs average

### **📊 Key Findings:**
- **FEEL Compilation**: 25.7% average improvement - **always enable**
- **Lenient Mode**: 28.1% average improvement - **highly beneficial**
- **Alpha Network**: 16.0% average improvement - **context dependent**
- **Runtime Type Check**: **1252.3% average degradation** - **avoid in production**

### **⚠️ Critical Warning:**
**Never enable Runtime Type Checking in production** - it causes massive performance degradation and often reduces success rates to 0%.

## Best Practices

### 1. Start with Baseline
Always measure Config 0 (no optimizations) first to establish baseline performance.

### 2. Test Individual Options
Enable one optimization at a time to understand individual impact:
- Config 1: Alpha Network only
- Config 2: Runtime Type Check only (⚠️ expect degradation)
- Config 4: Lenient Mode only
- Config 8: FEEL Compilation only

### 3. Consider Trade-offs
- **Build Time vs. Evaluation Time**: Some optimizations increase build time but reduce evaluation time
- **Memory vs. Speed**: Compiled networks use more memory but evaluate faster
- **Safety vs. Performance**: Disabling type checks improves speed but reduces safety

### 4. Environment-Specific Testing
Test with your actual DMN models and data patterns, as results vary significantly based on:
- Model complexity
- Data characteristics
- Hardware specifications
- JVM configuration

## Troubleshooting

### Low Success Rates
If success rates drop below 100%, investigate:
- Data type mismatches when type checking is disabled
- Model incompatibilities in lenient mode
- Complex expressions that fail compilation

### Unexpected Performance
If results don't match expectations:
- Verify JVM warmup is sufficient (increase warmup iterations)
- Check for system resource constraints
- Compare with different DMN models
- Validate test data consistency

## Advanced Usage

### Custom Analysis
Modify `DMNPerformanceAnalyzer.java` to add custom metrics or analysis:
```java
// Add custom performance thresholds
double targetEvalTime = 100.0; // microseconds
boolean meetsTarget = result.avgEvaluationTimeMicros < targetEvalTime;
```

### Configuration Selection
Use results to programmatically select optimal configurations:
```java
// Find best configuration for evaluation time
OptimalConfig best = results.stream()
    .min(Comparator.comparing(r -> r.avgEvaluationTimeMicros))
    .map(r -> r.config)
    .orElse(defaultConfig);
```

## Output Files

### `dmn-performance-results.csv`
Detailed performance data for all configurations and DMN models. Use with spreadsheet tools or data analysis frameworks.

### `dmn-performance-analysis.txt`
Human-readable analysis with:
- Configuration rankings
- Statistical significance tests
- Optimization impact summaries
- Recommendations

## Integration with CI/CD

Add performance regression testing to your build pipeline:
```bash
# Run performance tests
mvn exec:java@performance-comparison

# Check for regressions (custom script)
./check-performance-regression.sh dmn-performance-results.csv
```

## Conclusion

Use this framework to systematically evaluate and optimize your DMN runtime performance. Remember that optimal configurations depend on your specific use case, so always test with representative data and models.
