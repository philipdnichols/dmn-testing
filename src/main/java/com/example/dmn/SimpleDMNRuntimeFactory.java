package com.example.dmn;

import org.kie.dmn.api.core.DMNRuntime;
import java.io.InputStream;

/**
 * Simplified factory for DMN Runtime creation that provides a cleaner API
 * while maintaining the benefits of KieServices underneath for performance.
 * 
 * This class provides the simplicity of DMNRuntimeBuilder-style API
 * while leveraging the optimizations and caching of the KieServices approach.
 */
public class SimpleDMNRuntimeFactory {
    
    private static final DMNKjarManager kjarManager = new DMNKjarManager();
    
    /**
     * Creates a DMN runtime for a single DMN file using optimized KJAR caching.
     * This method provides a simple interface while leveraging the full
     * performance optimizations of the KieServices approach.
     * 
     * @param dmnFileName the DMN file name in resources
     * @return optimized DMN runtime instance
     */
    public static DMNRuntime fromResource(String dmnFileName) {
        return kjarManager.getDMNRuntime(dmnFileName);
    }
    
    /**
     * Creates a DMN runtime from an input stream.
     * Note: This bypasses caching and creates a new runtime each time.
     * 
     * @param dmnStream the DMN content stream
     * @param fileName logical name for the DMN (used for identification)
     * @return DMN runtime instance
     */
    public static DMNRuntime fromStream(InputStream dmnStream, String fileName) {
        // Implementation would create a temporary file or use KieServices directly
        // This demonstrates how you could extend the simple API while maintaining
        // the underlying KieServices approach
        throw new UnsupportedOperationException("Stream-based creation not yet implemented");
    }
    
    /**
     * Creates a unified DMN runtime that combines multiple DMN files.
     * This leverages the advanced capabilities of KieServices for complex scenarios.
     * 
     * @param dmnFileNames array of DMN files to combine
     * @return unified DMN runtime
     */
    public static DMNRuntime fromMultipleResources(String... dmnFileNames) {
        return kjarManager.getDMNRuntime(java.util.Arrays.asList(dmnFileNames));
    }
    
    /**
     * Clears all cached runtimes. Useful for testing or when you want to
     * ensure fresh model loading.
     */
    public static void clearCache() {
        kjarManager.clearMemoryCache();
        kjarManager.clearDiskCache();
    }
    
    /**
     * Builder-style interface for more complex configurations
     */
    public static class Builder {
        private boolean alphaNetworkEnabled = true;
        private boolean runtimeTypeCheckingEnabled = false;
        private boolean lenientModeEnabled = true;
        private boolean feelCompilationEnabled = true;
        
        public Builder withAlphaNetwork(boolean enabled) {
            this.alphaNetworkEnabled = enabled;
            return this;
        }
        
        public Builder withTypeChecking(boolean enabled) {
            this.runtimeTypeCheckingEnabled = enabled;
            return this;
        }
        
        public Builder withLenientMode(boolean enabled) {
            this.lenientModeEnabled = enabled;
            return this;
        }
        
        public Builder withFeelCompilation(boolean enabled) {
            this.feelCompilationEnabled = enabled;
            return this;
        }
        
        public DMNRuntime build(String dmnFileName) {
            // This would create a runtime with the specified configuration
            // For now, delegate to the existing optimized approach
            return fromResource(dmnFileName);
        }
    }
    
    /**
     * Creates a builder for advanced configuration scenarios
     */
    public static Builder builder() {
        return new Builder();
    }
}
