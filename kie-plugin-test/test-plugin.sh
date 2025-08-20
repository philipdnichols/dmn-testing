#!/bin/bash

# Kie Maven Plugin Test Script
# This script tests various aspects of the Kie Maven plugin to evaluate its effectiveness

echo "=========================================="
echo "Kie Maven Plugin Comprehensive Test"
echo "=========================================="

cd "$(dirname "$0")"

echo ""
echo "1. CLEAN BUILD TEST"
echo "==================="
echo "Testing basic clean..."
mvn clean -e

echo ""
echo "2. COMPILATION TEST"
echo "==================="
echo "Testing compilation of all artifacts..."
mvn clean compile -e

echo ""
echo "3. PACKAGING TEST"
echo "=================="
echo "Testing kjar packaging..."
mvn clean package -e

echo ""
echo "4. DEBUG BUILD TEST"
echo "==================="
echo "Testing with debug profile to see detailed plugin output..."
mvn clean package -Pdebug-build -e

echo ""
echo "5. NO-PRECOMPILE TEST"
echo "====================="
echo "Testing without precompilation to compare..."
mvn clean package -Pno-precompile -e

echo ""
echo "6. PLUGIN GOALS TEST"
echo "===================="
echo "Testing individual plugin goals..."

echo "Testing build goal..."
mvn clean kie:build -e

echo "Testing validateDMN goal..."
mvn clean kie:validateDMN -e

echo ""
echo "7. INSPECTING GENERATED ARTIFACTS"
echo "=================================="

echo "Contents of target directory:"
ls -la target/

echo ""
echo "Contents of generated kjar (if exists):"
if [ -f target/kie-plugin-test-1.0-SNAPSHOT.jar ]; then
    jar -tf target/kie-plugin-test-1.0-SNAPSHOT.jar | head -20
    echo "... (showing first 20 entries)"
    echo ""
    echo "Looking for pre-compiled artifacts:"
    jar -tf target/kie-plugin-test-1.0-SNAPSHOT.jar | grep -E "\.(class|model)$" || echo "No pre-compiled artifacts found"
    echo ""
    echo "Looking for rule metadata:"
    jar -tf target/kie-plugin-test-1.0-SNAPSHOT.jar | grep -E "META-INF.*\.(xml|properties)$" || echo "No metadata files found"
else
    echo "No kjar found in target directory"
fi

echo ""
echo "Contents of unpacked kjar (if exists):"
if [ -d target/unpacked-kjar ]; then
    echo "Directory structure:"
    find target/unpacked-kjar -type f | head -20
    echo "... (showing first 20 files)"
else
    echo "No unpacked kjar directory found"
fi

echo ""
echo "8. SIZE COMPARISON"
echo "=================="
echo "Comparing sizes of different build outputs:"
if [ -f target/kie-plugin-test-1.0-SNAPSHOT.jar ]; then
    echo "Main kjar size: $(du -h target/kie-plugin-test-1.0-SNAPSHOT.jar)"
fi

if [ -f target/kie-plugin-test-1.0-SNAPSHOT-project.tar.gz ]; then
    echo "Project archive size: $(du -h target/kie-plugin-test-1.0-SNAPSHOT-project.tar.gz)"
fi

echo ""
echo "=========================================="
echo "Test Complete!"
echo "=========================================="

echo ""
echo "ANALYSIS QUESTIONS TO CONSIDER:"
echo "1. Does the kjar contain pre-compiled rule models?"
echo "2. Are the original DMN/DRL/BPMN files included as-is?"
echo "3. What metadata files are generated?"
echo "4. Is there any performance benefit from 'pre-compilation'?"
echo "5. Can the kjar be used standalone without the original artifacts?"
