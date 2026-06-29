package ua.demo.agentlab.ai.rag.intelligence.parser;

import ua.demo.agentlab.ai.rag.model.SourceDocument;

import ua.demo.agentlab.ai.rag.intelligence.model.ControllerRouteDefinition;
import ua.demo.agentlab.ai.rag.intelligence.model.JavaAstMethod;
import ua.demo.agentlab.ai.rag.intelligence.model.JavaAstParseResult;
import ua.demo.agentlab.ai.rag.intelligence.model.JavaAstType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ControllerRouteParser {

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([a-zA-Z0-9_.]+)\\s*;");
    private static final Pattern CLASS_PATTERN = Pattern.compile("class\\s+([A-Za-z0-9_]+)");
    private static final Pattern METHOD_SIGNATURE_PATTERN =
            Pattern.compile("(public|protected|private)\\s+[A-Za-z0-9_<>\\[\\], ?]+\\s+([A-Za-z0-9_]+)\\s*\\(");
    private static final Pattern REQUEST_MAPPING_PATTERN = Pattern.compile("@RequestMapping\\(([^)]*)\\)");
    private static final Pattern HTTP_MAPPING_PATTERN = Pattern.compile("@(Get|Post|Put|Delete|Patch)Mapping\\(([^)]*)\\)");

    public List<ControllerRouteDefinition> parse(List<SourceDocument> documents) {
        return parse(documents, List.of());
    }

    public List<ControllerRouteDefinition> parse(List<SourceDocument> documents, List<JavaAstParseResult> astResults) {
        List<ControllerRouteDefinition> routes = new ArrayList<>();
        Map<String, JavaAstParseResult> astByPath = new HashMap<>();
        for (JavaAstParseResult astResult : astResults) {
            astByPath.put(astResult.relativePath(), astResult);
        }
        for (SourceDocument document : documents) {
            if (!"java".equalsIgnoreCase(document.language())) {
                continue;
            }
            String content = document.content();
            JavaAstParseResult astResult = astByPath.get(document.relativePath());
            boolean astController = astResult != null && astResult.types().stream()
                    .anyMatch(type -> hasControllerAnnotation(type.annotations()));
            if (!astController && !content.contains("@RestController") && !content.contains("@Controller")) {
                continue;
            }
            if (astController) {
                routes.addAll(parseFromAst(document, astResult));
                continue;
            }
            String packageName = extract(PACKAGE_PATTERN, content);
            String className = extract(CLASS_PATTERN, content);
            String basePath = extractClassBasePath(content);

            String[] lines = content.split("\\R");
            String pendingHttpMethod = null;
            String pendingPath = null;
            boolean pendingFromRequestMapping = false;

            for (String rawLine : lines) {
                String line = rawLine.trim();
                Matcher httpMapping = HTTP_MAPPING_PATTERN.matcher(line);
                if (httpMapping.find()) {
                    pendingHttpMethod = httpMapping.group(1).toUpperCase(Locale.ROOT);
                    pendingPath = extractPathArgument(httpMapping.group(2));
                    pendingFromRequestMapping = false;
                    continue;
                }
                Matcher requestMapping = REQUEST_MAPPING_PATTERN.matcher(line);
                if (requestMapping.find()) {
                    pendingHttpMethod = extractRequestMethod(requestMapping.group(1));
                    pendingPath = extractPathArgument(requestMapping.group(1));
                    pendingFromRequestMapping = true;
                    continue;
                }
                Matcher methodSignature = METHOD_SIGNATURE_PATTERN.matcher(line);
                if (methodSignature.find() && pendingHttpMethod != null) {
                    String routePath = pendingPath == null || pendingPath.isBlank() ? "/" : pendingPath;
                    routes.add(new ControllerRouteDefinition(
                            className,
                            packageName,
                            document.relativePath(),
                            methodSignature.group(2),
                            pendingHttpMethod == null ? "GET" : pendingHttpMethod,
                            basePath,
                            routePath,
                            normalizePath(basePath, routePath)
                    ));
                    if (!pendingFromRequestMapping || methodSignature.group(2) != null) {
                        pendingHttpMethod = null;
                        pendingPath = null;
                        pendingFromRequestMapping = false;
                    }
                }
            }
        }
        return routes;
    }

    private List<ControllerRouteDefinition> parseFromAst(SourceDocument document, JavaAstParseResult astResult) {
        List<ControllerRouteDefinition> routes = new ArrayList<>();
        for (JavaAstType type : astResult.types()) {
            if (!hasControllerAnnotation(type.annotations())) {
                continue;
            }
            String basePath = extractBasePath(type.annotations());
            for (JavaAstMethod method : type.methods()) {
                HttpRouteMapping mapping = extractMethodMapping(method.annotations());
                if (mapping == null) {
                    continue;
                }
                routes.add(new ControllerRouteDefinition(
                        type.name(),
                        astResult.packageName(),
                        document.relativePath(),
                        method.name(),
                        mapping.httpMethod(),
                        basePath,
                        mapping.routePath(),
                        normalizePath(basePath, mapping.routePath())
                ));
            }
        }
        return routes;
    }

    private boolean hasControllerAnnotation(List<String> annotations) {
        return annotations.stream().anyMatch(annotation ->
                annotation.endsWith("RestController")
                        || annotation.endsWith("Controller"));
    }

    private String extractBasePath(List<String> annotations) {
        return annotations.stream()
                .filter(annotation -> annotation.contains("RequestMapping"))
                .findFirst()
                .map(this::extractPathArgument)
                .orElse("/");
    }

    private HttpRouteMapping extractMethodMapping(List<String> annotations) {
        for (String annotation : annotations) {
            String normalized = annotation.startsWith("@") ? annotation : "@" + annotation;
            Matcher httpMapping = HTTP_MAPPING_PATTERN.matcher(normalized);
            if (httpMapping.find()) {
                return new HttpRouteMapping(
                        httpMapping.group(1).toUpperCase(Locale.ROOT),
                        extractPathArgument(httpMapping.group(2))
                );
            }
            Matcher requestMapping = REQUEST_MAPPING_PATTERN.matcher(normalized);
            if (requestMapping.find()) {
                return new HttpRouteMapping(
                        extractRequestMethod(requestMapping.group(1)),
                        extractPathArgument(requestMapping.group(1))
                );
            }
        }
        return null;
    }

    private String extractClassBasePath(String content) {
        Matcher classRequestMapping = REQUEST_MAPPING_PATTERN.matcher(content);
        return classRequestMapping.find() ? extractPathArgument(classRequestMapping.group(1)) : "/";
    }

    private String extractPathArgument(String args) {
        if (args == null || args.isBlank()) {
            return "/";
        }
        Matcher quoted = Pattern.compile("\"([^\"]+)\"").matcher(args);
        if (quoted.find()) {
            return quoted.group(1);
        }
        Matcher pathNamed = Pattern.compile("(path|value)\\s*=\\s*\"([^\"]+)\"").matcher(args);
        return pathNamed.find() ? pathNamed.group(2) : "/";
    }

    private String extractRequestMethod(String args) {
        Matcher matcher = Pattern.compile("RequestMethod\\.([A-Z]+)").matcher(args);
        return matcher.find() ? matcher.group(1) : "GET";
    }

    private String extract(Pattern pattern, String content) {
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String normalizePath(String basePath, String routePath) {
        String base = basePath == null || basePath.isBlank() ? "/" : basePath;
        String route = routePath == null || routePath.isBlank() ? "/" : routePath;
        String combined = (base.endsWith("/") ? base.substring(0, base.length() - 1) : base)
                + "/"
                + (route.startsWith("/") ? route.substring(1) : route);
        return combined.replaceAll("//+", "/");
    }

    private record HttpRouteMapping(String httpMethod, String routePath) {
    }
}
