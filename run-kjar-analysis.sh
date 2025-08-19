#!/bin/bash

# DMN KJAR Performance Analysis Script
# This script runs comprehensive KJAR performance testing and analysis

echo "=== DMN KJAR Performance Analysis ==="
echo "This script will:"
echo "1. Run KJAR performance comparison tests"
echo "2. Generate performance reports"
echo "3. Create visualization-ready CSV files"
echo ""

# Ensure target directory exists
mkdir -p target/kjar-cache

# Clean previous results
echo "Cleaning previous results..."
rm -f dmn-kjar-performance-results.csv
rm -rf target/kjar-cache/*

# Run KJAR performance analysis
echo "Running KJAR performance analysis..."
mvn exec:java -Dexec.mainClass="com.example.dmn.DMNKjarPerformanceAnalyzer" -q

# Check if results were generated
if [ -f "dmn-kjar-performance-results.csv" ]; then
    echo ""
    echo "✅ KJAR performance analysis completed successfully!"
    echo ""
    echo "Results saved to:"
    echo "  - dmn-kjar-performance-results.csv"
    echo "  - target/kjar-cache/ (KJAR cache files)"
    echo ""
    
    # Show a summary of the results
    echo "=== QUICK SUMMARY ==="
    if command -v python3 >/dev/null 2>&1; then
        python3 -c "
import csv
import statistics

try:
    with open('dmn-kjar-performance-results.csv', 'r') as f:
        reader = csv.DictReader(f)
        data = list(reader)
    
    if data:
        build_improvements = [float(row['Build_Improvement_Percent']) for row in data if row['Build_Improvement_Percent']]
        eval_improvements = [float(row['Eval_Improvement_Percent']) for row in data if row['Eval_Improvement_Percent']]
        
        if build_improvements:
            print(f'Average Build Time Improvement: {statistics.mean(build_improvements):.1f}%')
            print(f'Best Build Improvement: {max(build_improvements):.1f}%')
        
        if eval_improvements:
            print(f'Average Evaluation Time Improvement: {statistics.mean(eval_improvements):.1f}%')
            print(f'Best Evaluation Improvement: {max(eval_improvements):.1f}%')
            
        print(f'Total DMN files tested: {len(data)}')
    else:
        print('No performance data found in results file.')
        
except Exception as e:
    print(f'Could not parse results: {e}')
"
    else
        echo "Python3 not available for summary generation."
        echo "Check dmn-kjar-performance-results.csv for detailed results."
    fi
    
else
    echo "❌ KJAR performance analysis failed!"
    echo "Check the Maven output above for errors."
    exit 1
fi

echo ""
echo "=== KJAR Cache Information ==="
if [ -d "target/kjar-cache" ]; then
    cache_files=$(find target/kjar-cache -name "*.kjar" | wc -l)
    cache_size=$(du -sh target/kjar-cache 2>/dev/null | cut -f1)
    echo "KJAR cache files: $cache_files"
    echo "Cache directory size: $cache_size"
    
    echo ""
    echo "KJAR files created:"
    find target/kjar-cache -name "*.kjar" -exec basename {} \; | sort
fi

echo ""
echo "=== NEXT STEPS ==="
echo "1. Review dmn-kjar-performance-results.csv for detailed performance metrics"
echo "2. Consider implementing KJAR caching in your production DMN applications"
echo "3. Use the DMNKjarManager class for optimized DMN runtime management"
echo "4. Run 'mvn exec:java -Dexec.mainClass=\"com.example.dmn.DMNKjarExample\"' to see usage examples"

echo ""
echo "Analysis complete!"
