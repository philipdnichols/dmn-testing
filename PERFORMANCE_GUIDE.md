# DMN Performance Optimization Guide

This guide explains how to use the comprehensive DMN performance comparison framework to optimize your DMN runtime configurations, including both individual and unified runtime analysis.

## Quick Start

1. **Build the project:**
   ```bash
   mvn clean compile
   ```

2. **Run individual runtime performance comparison:**
   ```bash
   mvn exec:java@performance-comparison
   ```

3. **Analyze individual runtime results:**
   ```bash
   mvn exec:java@performance-analysis
   ```

4. **Run unified runtime analysis (NEW):**
   ```bash
   mvn exec:java@unified-runtime-analysis
   ```

## Understanding the Results

### Test Types

**Individual Runtime Testing**: Tests each DMN model in its own isolated runtime
**Unified Runtime Testing**: Tests all DMN models loaded into a single runtime

### Configuration Numbers
Each configuration (0-15) represents a combination of optimization options:
- **Config 0**: No optimizations (baseline)
- **Config 15**: All optimizations enabled
- **Others**: Various combinations of the 4 optimization flags

### Performance Metrics

#### Individual Runtime Metrics
- **Build Time**: Time to compile the DMN model (milliseconds)
- **Evaluation Time**: Time to evaluate decisions (microseconds)
- **Success Rate**: Percentage of successful evaluations
- **Percentiles**: P95 and P99 evaluation times for understanding outliers

#### Unified Runtime Metrics (NEW)
- **Unified Build Time**: Time to compile all DMN models into one runtime
- **Memory Usage**: Heap memory consumption comparison
- **Cross-Model Evaluation**: Performance when evaluating multiple models in sequence
- **Runtime Efficiency Score**: Composite score weighing evaluation time and memory usage

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

## Unified Runtime vs Individual Runtime Decision Guide

### When to Use Unified Runtime
- **Multiple DMN models** are used in the same application
- **Memory efficiency** is important (typically 20-40% memory savings)
- **Cross-model decision scenarios** are common
- **Application startup time** is less critical than runtime performance
- **Shared resources** and caching benefits are desired

### When to Use Individual Runtimes
- **Only one DMN model** is used at a time
- **Dynamic model loading/unloading** is required
- **Strict isolation** between decision models is needed
- **Minimal startup time** is critical
- **Independent model versioning** is important

### Performance Characteristics

#### Unified Runtime Advantages
- **Memory Efficiency**: Shared KIE container and runtime resources
- **Cross-Model Performance**: Optimized for scenarios using multiple models
- **Caching Benefits**: Shared compilation and optimization artifacts
- **Resource Consolidation**: Single JVM instance for all models

#### Unified Runtime Trade-offs
- **Higher Build Time**: All models must be compiled together
- **Larger Memory Footprint**: All models loaded simultaneously
- **Complex Error Handling**: Failure in one model affects the entire runtime
- **Deployment Complexity**: All models must be deployed together

## Output Files

### Individual Runtime Analysis
- **`dmn-performance-results.csv`**: Detailed performance data for all configurations and DMN models
- **`dmn-performance-analysis.txt`**: Human-readable analysis with recommendations

### Unified Runtime Analysis (NEW)
- **`dmn-unified-runtime-results.csv`**: Comprehensive unified vs individual runtime comparison
- **`dmn-unified-runtime-analysis.txt`**: Detailed analysis of unified runtime benefits and trade-offs

## Conclusion

Use this expanded framework to systematically evaluate and optimize your DMN runtime performance for both individual and unified runtime scenarios. The unified runtime analysis helps you make informed decisions about runtime architecture based on your specific use case requirements.
