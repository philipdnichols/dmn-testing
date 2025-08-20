# Kie Maven Plugin Test Project

This project is designed to thoroughly test the capabilities of the Kie Maven plugin, specifically focusing on how it validates, builds, and packages Drools artifacts.

## Purpose

To investigate whether the Kie Maven plugin actually provides meaningful pre-compilation or if the generated kjars simply contain the original artifacts without optimization.

## Project Structure

```
kie-plugin-test/
├── pom.xml                          # Maven configuration with Kie plugin
├── test-plugin.sh                   # Comprehensive test script
├── README.md                        # This file
└── src/main/resources/
    ├── META-INF/
    │   └── kmodule.xml             # Kie module configuration
    ├── rules/
    │   └── simple-rules.drl        # Drools Rule Language file
    ├── dmn/
    │   └── simple-decision.dmn     # Decision Model and Notation file
    ├── bpmn/
    │   └── simple-process.bpmn     # Business Process Model file
    ├── pmml/
    │   └── simple-scorecard.pmml   # Predictive Model Markup Language file
    └── dtables/
        └── decision-table.xls      # Decision table (CSV format for testing)
```

## Artifact Types Included

1. **DRL (Drools Rule Language)**: Traditional rule files with when-then logic
2. **DMN (Decision Model and Notation)**: Standard decision modeling files
3. **BPMN (Business Process Model and Notation)**: Process workflow definitions
4. **PMML (Predictive Model Markup Language)**: Machine learning model definitions
5. **Decision Tables**: Spreadsheet-based rule definitions

## Testing Strategy

The project includes multiple Maven profiles and configurations to test different aspects of the Kie plugin:

### Profiles

1. **Default**: Standard kjar packaging with full plugin features
2. **debug-build**: Verbose output to see detailed plugin behavior
3. **no-precompile**: Disables model generation for comparison

### Test Goals

1. **Validation**: Does the plugin properly validate all artifact types?
2. **Compilation**: What happens during the "compilation" phase?
3. **Packaging**: What gets included in the final kjar?
4. **Pre-compilation**: Are artifacts actually pre-compiled or just validated?
5. **Metadata**: What metadata files are generated?

## Running Tests

Execute the comprehensive test script:

```bash
./test-plugin.sh
```

This script will:
- Run various Maven goals with different configurations
- Inspect the generated kjar contents
- Compare build outputs
- Analyze what the plugin actually does

## Key Questions to Answer

1. **Does the kjar contain pre-compiled rule models?**
   - Look for `.class` files or compiled rule representations
   - Check if original `.drl`, `.dmn` files are still present

2. **Is there actual performance benefit?**
   - Compare loading times between raw files and kjar
   - Analyze memory usage differences

3. **What validation occurs?**
   - Do syntax errors get caught?
   - Are cross-references validated?

4. **Standalone usability?**
   - Can the kjar be used without original source files?
   - Does it contain everything needed for execution?

## Expected Findings

Based on the investigation hypothesis, we expect to find:
- Original artifact files included as-is in the kjar
- Minimal or no actual pre-compilation
- Validation occurs but doesn't produce optimized artifacts
- The "kjar" is primarily a packaging format rather than a compilation target

## Plugin Configuration

The pom.xml includes extensive Kie Maven plugin configuration:
- Verbose output enabled
- Validation enabled
- Model generation enabled
- Multiple execution phases
- Different profiles for testing various scenarios

## Analysis Tools

The project includes:
- Dependency plugin to unpack and inspect kjar contents
- Assembly plugin for creating analysis archives
- Multiple execution profiles for different testing scenarios
- Shell script for automated testing and analysis
