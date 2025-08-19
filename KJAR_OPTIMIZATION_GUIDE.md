# DMN KJAR Optimization Guide

## Overview

This guide demonstrates how to use Knowledge JARs (KJARs) to significantly improve DMN evaluation performance by precompiling and caching compiled DMN runtimes. This approach can provide substantial performance benefits over the traditional method of building DMN runtimes from raw DMN files at runtime.

## What is a KJAR?

A Knowledge JAR (KJAR) is a precompiled package containing your DMN models in their compiled form. Instead of parsing and compiling DMN files every time you create a runtime, you can:

1. **Build Phase**: Compile DMN files into a KJAR once
2. **Runtime Phase**: Load the precompiled KJAR to instantly restore the DMN runtime state

## Performance Benefits

Based on our implementation and testing, KJAR-based optimization can provide:

- **Faster Startup**: Eliminate DMN parsing and compilation overhead
- **Reduced Memory Usage**: Precompiled models are more memory efficient
- **Consistent Performance**: Avoid compilation-related performance variations
- **Scalability**: Better performance in high-load scenarios

## Implementation Components

### 1. DMNKjarManager

The `DMNKjarManager` class provides production-ready KJAR management with:

- **Multi-level Caching**: In-memory + disk-based KJAR caching
- **Cache Validation**: Automatic invalidation when DMN files change
- **Hot Reloading**: Support for updating DMN models without restart
- **Statistics**: Cache performance monitoring

```java
// Basic usage
DMNKjarManager kjarManager = new DMNKjarManager();

// Get optimized DMN runtime (automatically handles KJAR creation/loading)
DMNRuntime runtime = kjarManager.getDMNRuntime("loan-approval.dmn");

// Precompile multiple DMN files for faster startup
kjarManager.precompileAllDMNs(
    "loan-approval.dmn",
    "insurance-risk-assessment.dmn",
    "supply-chain-optimization.dmn"
);
```

### 2. DMNKjarPerformanceAnalyzer

Comprehensive performance comparison tool that measures:
- Traditional DMN runtime creation vs KJAR-based approach
- Build time improvements
- Evaluation time improvements
- Memory usage analysis

### 3. DMNKjarExample

Practical examples demonstrating:
- KJAR precompilation strategies
- Runtime optimization patterns
- Cache management best practices
- Multi-DMN scenarios

## Key Features

### Automatic KJAR Management

```java
// The manager handles everything automatically:
// 1. Check in-memory cache
// 2. Check disk cache (KJAR files)
// 3. Build from source if necessary
// 4. Cache results for future use

DMNRuntime runtime = kjarManager.getDMNRuntime("my-dmn.dmn");
```

### Cache Validation

```java
// Automatic invalidation when DMN files change
// - File modification detection
// - Content hash validation
// - Metadata verification

boolean isValid = kjarManager.isKjarCacheValid(kjarPath, metadataPath, dmnFiles);
```

### Multi-DMN Support

```java
// Combine multiple DMN files into a single optimized runtime
List<String> dmnFiles = Arrays.asList(
    "loan-approval.dmn",
    "risk-assessment.dmn",
    "portfolio-analysis.dmn"
);

DMNRuntime combinedRuntime = kjarManager.getDMNRuntime(dmnFiles);
```

### Performance Monitoring

```java
// Get detailed cache statistics
CacheStatistics stats = kjarManager.getCacheStatistics();
System.out.println("Memory entries: " + stats.memoryEntries);
System.out.println("Disk entries: " + stats.diskEntries);
System.out.println("Total size: " + stats.totalDiskSizeBytes + " bytes");
```

## Usage Patterns

### 1. Application Startup Optimization

```java
// Precompile all DMN files during application initialization
@PostConstruct
public void initializeDMNRuntimes() {
    kjarManager.precompileAllDMNs(
        "loan-approval.dmn",
        "insurance-risk-assessment.dmn",
        "supply-chain-optimization.dmn",
        "financial-portfolio-analysis.dmn",
        "healthcare-treatment-protocol.dmn"
    );
}
```

### 2. High-Performance Evaluation

```java
@Service
public class DMNEvaluationService {
    private final DMNKjarManager kjarManager = new DMNKjarManager();
    
    public DMNResult evaluateLoanApproval(Map<String, Object> context) {
        // Fast runtime access via KJAR cache
        DMNRuntime runtime = kjarManager.getDMNRuntime("loan-approval.dmn");
        
        DMNContext dmnContext = runtime.newContext();
        dmnContext.setAll(context);
        
        return runtime.evaluateDecisionService(
            runtime.getModels().get(0), 
            dmnContext, 
            "Loan Approval Service"
        );
    }
}
```

### 3. Development vs Production

```java
// Development: Clear cache to pick up DMN file changes
if (isDevelopmentMode()) {
    kjarManager.clearDiskCache();
}

// Production: Use aggressive caching for maximum performance
if (isProductionMode()) {
    kjarManager.precompileAllDMNs(getAllDMNFiles());
}
```

## Running the Examples

### 1. Basic KJAR Example

```bash
mvn exec:java -Dexec.mainClass="com.example.dmn.DMNKjarExample"
```

This demonstrates:
- Precompilation workflows
- Runtime optimization patterns
- Cache management
- Multi-DMN scenarios

### 2. Performance Analysis

```bash
mvn exec:java -Dexec.mainClass="com.example.dmn.DMNKjarPerformanceAnalyzer"
```

This generates comprehensive performance comparisons between traditional and KJAR approaches.

### 3. Interactive Analysis Script

```bash
./run-kjar-analysis.sh
```

This runs the full performance analysis with automated reporting and CSV generation.

## Maven Integration

The project includes Maven execution profiles for easy testing:

```xml
<!-- Run KJAR performance analysis -->
<execution>
    <id>kjar-performance-analysis</id>
    <goals>
        <goal>java</goal>
    </goals>
    <configuration>
        <mainClass>com.example.dmn.DMNKjarPerformanceAnalyzer</mainClass>
    </configuration>
</execution>

<!-- Run KJAR examples -->
<execution>
    <id>kjar-example</id>
    <goals>
        <goal>java</goal>
    </goals>
    <configuration>
        <mainClass>com.example.dmn.DMNKjarExample</mainClass>
    </configuration>
</execution>
```

## Advanced Configuration

### Custom Cache Directory

```java
DMNKjarManager kjarManager = new DMNKjarManager("/path/to/custom/cache");
```

### Cache TTL Configuration

```java
// Modify CachedKieContainer.CACHE_TTL_MINUTES for custom expiration
public static class CachedKieContainer {
    private static final long CACHE_TTL_MINUTES = 120; // 2 hours
    // ...
}
```

### Memory Management

```java
// Clear in-memory cache periodically to manage memory usage
@Scheduled(fixedRate = 3600000) // Every hour
public void clearMemoryCache() {
    kjarManager.clearMemoryCache();
}
```

## Best Practices

### 1. Precompilation Strategy

- **Build Time**: Create KJARs during application build/deployment
- **Startup Time**: Precompile critical DMN files during application initialization
- **Runtime**: Use lazy loading for less frequently used DMN files

### 2. Cache Management

- **Development**: Clear caches when DMN files change
- **Testing**: Use separate cache directories per test environment
- **Production**: Monitor cache hit rates and size

### 3. Error Handling

```java
try {
    DMNRuntime runtime = kjarManager.getDMNRuntime("my-dmn.dmn");
    // Use runtime...
} catch (RuntimeException e) {
    // Handle KJAR creation/loading errors
    log.error("Failed to create DMN runtime: {}", e.getMessage());
    // Fallback to traditional approach if needed
}
```

### 4. Monitoring

```java
// Regular cache statistics monitoring
@Scheduled(fixedRate = 300000) // Every 5 minutes
public void logCacheStatistics() {
    CacheStatistics stats = kjarManager.getCacheStatistics();
    log.info("KJAR Cache - Memory: {} entries, Disk: {} entries ({} MB)", 
        stats.memoryEntries, stats.diskEntries, 
        stats.totalDiskSizeBytes / (1024.0 * 1024.0));
}
```

## Troubleshooting

### Common Issues

1. **KJAR Creation Failures**: Check DMN file validity and paths
2. **Cache Invalidation**: Verify file modification detection is working
3. **Memory Issues**: Monitor cache size and implement appropriate TTL
4. **Performance Degradation**: Check cache hit rates and consider precompilation

### Debug Logging

Enable debug logging to troubleshoot KJAR operations:

```java
// Add logging to DMNKjarManager methods
System.out.println("Creating KJAR for: " + dmnFileName);
System.out.println("Loading from cache: " + kjarPath);
System.out.println("Cache hit: " + (cachedContainer != null));
```

## Conclusion

KJAR-based optimization provides a significant performance improvement for DMN evaluation scenarios. The implementation in this project demonstrates:

1. **Practical KJAR Management**: Production-ready caching and validation
2. **Performance Benefits**: Measurable improvements in startup and evaluation times
3. **Enterprise Features**: Hot reloading, monitoring, and error handling
4. **Best Practices**: Patterns for development, testing, and production use

Use this implementation as a foundation for optimizing DMN performance in your applications, adapting the caching strategies and configuration to your specific requirements.
