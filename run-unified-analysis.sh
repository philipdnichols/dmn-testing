#!/bin/bash

# DMN Unified Runtime Analysis Script
# This script runs the unified runtime performance analysis and generates comprehensive reports

echo "=== DMN Unified Runtime Performance Analysis ==="
echo "This will analyze performance of unified runtime vs individual runtimes"
echo "Testing all 5 DMN models with 16 optimization configurations"
echo ""

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed or not in PATH"
    exit 1
fi

# Build the project
echo "Building project..."
mvn clean compile -q
if [ $? -ne 0 ]; then
    echo "Error: Failed to build project"
    exit 1
fi

echo "Build successful!"
echo ""

# Run unified runtime analysis
echo "Running unified runtime analysis..."
echo "This may take several minutes as it tests 16 configurations with extensive iterations"
echo ""

start_time=$(date +%s)
mvn exec:java@unified-runtime-analysis -q

if [ $? -eq 0 ]; then
    end_time=$(date +%s)
    duration=$((end_time - start_time))
    echo ""
    echo "=== Analysis Complete ==="
    echo "Duration: ${duration} seconds"
    echo ""
    echo "Generated files:"
    if [ -f "dmn-unified-runtime-results.csv" ]; then
        echo "✓ dmn-unified-runtime-results.csv - Raw performance data"
    fi
    if [ -f "dmn-unified-runtime-analysis.txt" ]; then
        echo "✓ dmn-unified-runtime-analysis.txt - Detailed analysis report"
        echo ""
        echo "=== Key Findings Preview ==="
        echo "Top 3 configurations by efficiency score:"
        grep -A 15 "TOP UNIFIED RUNTIME CONFIGURATIONS:" dmn-unified-runtime-analysis.txt | head -20
        echo ""
        echo "Memory efficiency summary:"
        grep -A 5 "Average memory savings" dmn-unified-runtime-analysis.txt
    fi
    echo ""
    echo "For complete analysis, review: dmn-unified-runtime-analysis.txt"
    echo "For data analysis, use: dmn-unified-runtime-results.csv"
else
    echo "Error: Unified runtime analysis failed"
    exit 1
fi
