package com.codecopilot.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class JavaSourceParser {

    private static final Logger log = LoggerFactory.getLogger(JavaSourceParser.class);

    private final JavaParser parser;

    public JavaSourceParser() {
        StaticJavaParser.getConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17)
                .setAttributeComments(false);
        this.parser = new JavaParser(
                new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17)
                        .setAttributeComments(false));
    }

    /**
     * Parses a single Java file into a structural model. Never throws: syntax
     * errors produce a partial model and a warning.
     */
    public ParsedSourceFile parse(Path filePath, String relativePath) {
        try {
            String source = Files.readString(filePath, StandardCharsets.UTF_8);
            return parse(source, relativePath);
        } catch (IOException e) {
            log.warn("Cannot read {}: {}", relativePath, e.getMessage());
            return ParsedSourceFile.empty(relativePath, "java");
        }
    }

    public ParsedSourceFile parse(String source, String relativePath) {
        return parser.parse(source)
                .getResult()
                .map(cu -> extract(cu, relativePath))
                .orElseGet(() -> {
                    log.warn("Failed to parse {}", relativePath);
                    return ParsedSourceFile.empty(relativePath, "java");
                });
    }

    private ParsedSourceFile extract(CompilationUnit cu, String relativePath) {
        String packageName = cu.getPackageDeclaration()
                .map(pd -> pd.getNameAsString()).orElse("");
        List<String> imports = cu.getImports().stream().map(i -> i.getNameAsString()).toList();

        List<ParsedSourceFile.ParsedType> types = new ArrayList<>();
        for (TypeDeclaration<?> top : cu.getTypes()) {
            collectType(top, top, packageName, relativePath, types);
        }

        List<ParsedSourceFile.ParsedApiEndpoint> endpoints = new ArrayList<>();
        for (TypeDeclaration<?> top : cu.getTypes()) {
            String base = mappingAnnotationValue(top, "RequestMapping");
            for (var member : top.getMembers()) {
                if (member instanceof MethodDeclaration md) {
                    String[] pair = mappingAnnotationValuePair(md);
                    if (pair != null) {
                        String fullPath = joinPath(base, pair[1]);
                        endpoints.add(new ParsedSourceFile.ParsedApiEndpoint(
                                pair[0], fullPath, top.getNameAsString(), md.getNameAsString(),
                                relativePath, md.getBegin().map(p -> p.line).orElse(0),
                                md.getEnd().map(p -> p.line).orElse(0)));
                    }
                }
            }
        }

        return new ParsedSourceFile(relativePath, "java", packageName, imports, types, endpoints);
    }

    private void collectType(TypeDeclaration<?> declared, TypeDeclaration<?> top,
                             String packageName, String path, List<ParsedSourceFile.ParsedType> out) {
        TypeDeclaration<?> type;
        if (declared.isClassOrInterfaceDeclaration()) {
            type = declared.asClassOrInterfaceDeclaration();
        } else if (declared.isEnumDeclaration()) {
            type = declared.asEnumDeclaration();
        } else if (declared.isRecordDeclaration()) {
            type = declared.asRecordDeclaration();
        } else {
            return;
        }

        List<String> annotations = type.getAnnotations().stream().map(a -> a.getNameAsString()).toList();
        List<String> modifiers = type.getModifiers().stream().map(Modifier::getKeyword).map(Enum::name).toList();

        String name = type.getNameAsString();
        ParsedSourceFile.ParsedType.Kind kind = kindOf(type);
        String fqName = packageName.isEmpty() ? name : packageName + "." + name;
        String parent = null;
        List<String> interfaces = new ArrayList<>();
        if (type.isClassOrInterfaceDeclaration()) {
            ClassOrInterfaceDeclaration cid = type.asClassOrInterfaceDeclaration();
            if (!cid.isInterface()) {
                if (!cid.getExtendedTypes().isEmpty()) {
                    parent = cid.getExtendedTypes().get(0).getNameAsString();
                }
                for (var impl : cid.getImplementedTypes()) {
                    interfaces.add(impl.getNameAsString());
                }
            } else {
                for (var ext : cid.getExtendedTypes()) {
                    interfaces.add(ext.getNameAsString());
                }
            }
        }

        List<ParsedSourceFile.ParsedField> fields = new ArrayList<>();
        List<ParsedSourceFile.ParsedMethod> methods = new ArrayList<>();
        for (var member : type.getMembers()) {
            if (member instanceof FieldDeclaration fd) {
                int start = fd.getBegin().map(p -> p.line).orElse(0);
                int end = fd.getEnd().map(p -> p.line).orElse(0);
                for (VariableDeclarator v : fd.getVariables()) {
                    fields.add(new ParsedSourceFile.ParsedField(
                            v.getNameAsString(),
                            v.getType().asString(),
                            fd.getModifiers().stream().map(m -> m.getKeyword().name()).toList(),
                            fd.getAnnotations().stream().map(a -> a.getNameAsString()).toList(),
                            start, end));
                }
            } else if (member instanceof MethodDeclaration md) {
                methods.add(toMethod(md, name, false, path));
            } else if (member instanceof ConstructorDeclaration cd) {
                methods.add(toConstructor(cd, name, path));
            } else if (member instanceof TypeDeclaration<?> nested) {
                if (nested != top) {
                    collectType(nested, top, packageName, path, out);
                }
            }
        }

        out.add(new ParsedSourceFile.ParsedType(
                kind, name, fqName, type.getBegin().map(p -> p.line).orElse(0),
                type.getEnd().map(p -> p.line).orElse(0),
                annotations, modifiers, parent, interfaces, fields, methods));
    }

    private ParsedSourceFile.ParsedMethod toMethod(MethodDeclaration md, String ownerClass, boolean ignore, String path) {
        List<String> annotations = md.getAnnotations().stream().map(a -> a.getNameAsString()).toList();
        List<String> modifiers = md.getModifiers().stream().map(m -> m.getKeyword().name()).toList();
        List<ParsedSourceFile.Parameter> params = md.getParameters().stream()
                .map(p -> new ParsedSourceFile.Parameter(p.getNameAsString(), p.getType().asString()))
                .toList();
        List<String> calls = md.findAll(MethodCallExpr.class).stream()
                .map(MethodCallExpr::getNameAsString)
                .distinct()
                .toList();
        String body = md.getBody().map(Node::toString).orElse("");
        return new ParsedSourceFile.ParsedMethod(
                md.getNameAsString(), md.getType().asString(), false, modifiers, annotations,
                params, calls, md.getBegin().map(p -> p.line).orElse(0),
                md.getEnd().map(p -> p.line).orElse(0), body);
    }

    private ParsedSourceFile.ParsedMethod toConstructor(ConstructorDeclaration cd, String ownerClass, String path) {
        List<String> annotations = cd.getAnnotations().stream().map(a -> a.getNameAsString()).toList();
        List<String> modifiers = cd.getModifiers().stream().map(m -> m.getKeyword().name()).toList();
        List<ParsedSourceFile.Parameter> params = cd.getParameters().stream()
                .map(p -> new ParsedSourceFile.Parameter(p.getNameAsString(), p.getType().asString()))
                .toList();
        List<String> calls = cd.findAll(MethodCallExpr.class).stream()
                .map(MethodCallExpr::getNameAsString)
                .distinct()
                .toList();
        String body = cd.getBody().toString();
        return new ParsedSourceFile.ParsedMethod(
                "<init>", ownerClass, true, modifiers, annotations, params, calls,
                cd.getBegin().map(p -> p.line).orElse(0), cd.getEnd().map(p -> p.line).orElse(0), body);
    }

    private ParsedSourceFile.ParsedType.Kind kindOf(TypeDeclaration<?> type) {
        if (type.isClassOrInterfaceDeclaration()) {
            return type.asClassOrInterfaceDeclaration().isInterface()
                    ? ParsedSourceFile.ParsedType.Kind.INTERFACE : ParsedSourceFile.ParsedType.Kind.CLASS;
        }
        if (type.isEnumDeclaration()) {
            return ParsedSourceFile.ParsedType.Kind.ENUM;
        }
        if (type.isRecordDeclaration()) {
            return ParsedSourceFile.ParsedType.Kind.RECORD;
        }
        if (type instanceof AnnotationDeclaration) {
            return ParsedSourceFile.ParsedType.Kind.ANNOTATION;
        }
        return ParsedSourceFile.ParsedType.Kind.CLASS;
    }

    /**
     * Extracts the "value" string of a mapping-style annotation (e.g. @RequestMapping("/orders")).
     */
    private String mappingAnnotationValue(TypeDeclaration<?> node, String annotationName) {
        for (AnnotationExpr ann : node.getAnnotations()) {
            if (ann.getNameAsString().equals(annotationName)) {
                return annotationValue(ann);
            }
        }
        return null;
    }

    private String[] mappingAnnotationValuePair(MethodDeclaration md) {
        Map<String, String> verbs = new LinkedHashMap<>();
        verbs.put("GetMapping", "GET");
        verbs.put("PostMapping", "POST");
        verbs.put("PutMapping", "PUT");
        verbs.put("DeleteMapping", "DELETE");
        verbs.put("PatchMapping", "PATCH");
        for (AnnotationExpr ann : md.getAnnotations()) {
            String verb = verbs.get(ann.getNameAsString());
            if (verb != null) {
                String value = annotationValue(ann);
                if (value == null || value.isEmpty()) {
                    value = "";
                }
                return new String[]{verb, value};
            }
            if (ann.getNameAsString().equals("RequestMapping")) {
                String value = annotationValue(ann);
                if (value == null || value.isEmpty()) {
                    value = "";
                }
                return new String[]{null, value}; // class-level RequestMapping handled separately
            }
        }
        return null;
    }

    private String annotationValue(AnnotationExpr ann) {
        if (ann instanceof com.github.javaparser.ast.expr.SingleMemberAnnotationExpr sm) {
            return sm.getMemberValue().toString().replace("\"", "");
        }
        if (ann instanceof com.github.javaparser.ast.expr.NormalAnnotationExpr na) {
            for (var pair : na.getPairs()) {
                if (pair.getNameAsString().equals("value")) {
                    return pair.getValue().toString().replace("\"", "");
                }
            }
        }
        var lit = ann.findAll(com.github.javaparser.ast.expr.StringLiteralExpr.class).stream()
                .findFirst();
        return lit.map(l -> l.getValue()).orElse("");
    }

    public static String joinPath(String base, String extra) {
        String a = base == null ? "" : base;
        String b = extra == null ? "" : extra;
        if (a.isEmpty() && b.isEmpty()) {
            return "";
        }
        if (a.isEmpty()) {
            return "/" + b.replaceFirst("^/", "");
        }
        String joined = "/" + a.replaceAll("^/+", "").replaceAll("/+$", "")
                + (b.isEmpty() ? "" : "/" + b.replaceFirst("^/", ""));
        // clear trailing slashes except root
        if (joined.length() > 1 && joined.endsWith("/")) {
            joined = joined.substring(0, joined.length() - 1);
        }
        return joined;
    }
}