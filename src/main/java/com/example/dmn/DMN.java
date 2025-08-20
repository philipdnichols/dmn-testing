package com.example.dmn;

/**
 * Class representing a DMN file configuration
 */
public class DMN {
    public final String fileName;
    public final String name;
    public final String namespace;
    public final String decisionServiceName;
    
    public DMN(String fileName, String name, String namespace, String decisionServiceName) {
        this.fileName = fileName;
        this.name = name;
        this.namespace = namespace;
        this.decisionServiceName = decisionServiceName;
    }
}
