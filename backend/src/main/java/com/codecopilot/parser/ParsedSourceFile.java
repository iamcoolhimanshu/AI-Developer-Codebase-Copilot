package com.codecopilot.parser;

import java.util.ArrayList;
import java.util.List;

public record ParsedSourceFile(
        String path,
        String language,
        String packageName,
        List<String> imports,
        List<ParsedType> types,
        List<ParsedApiEndpoint> endpoints) {

    public record ParsedType(
            Kind kind,
            String name,
            String fqName,
            int startLine,
            int endLine,
            List<String> annotations,
            List<String> modifiers,
            String parentClass,
            List<String> interfaces,
            List<ParsedField> fields,
            List<ParsedMethod> methods) {

        public enum Kind {
            CLASS, INTERFACE, ENUM, RECORD, ANNOTATION
        }
    }

    public record ParsedField(
            String name,
            String type,
            List<String> modifiers,
            List<String> annotations,
            int startLine,
            int endLine) {
    }

    public record Parameter(String name, String type) {
    }

    public record ParsedMethod(
            String name,
            String returnType,
            boolean constructor,
            List<String> modifiers,
            List<String> annotations,
            List<Parameter> parameters,
            List<String> methodCalls,
            int startLine,
            int endLine,
            String body) {
    }

    public record ParsedApiEndpoint(
            String httpMethod,
            String path,
            String className,
            String methodName,
            String filePath,
            int startLine,
            int endLine) {

        public ParsedApiEndpoint withResolved(String fullPath) {
            return new ParsedApiEndpoint(httpMethod, path, className, methodName, fullPath, startLine, endLine);
        }
    }

    public static ParsedSourceFile empty(String path, String language) {
        return new ParsedSourceFile(path, language, "", new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }
}