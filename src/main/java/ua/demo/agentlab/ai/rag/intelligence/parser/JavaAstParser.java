package ua.demo.agentlab.ai.rag.intelligence.parser;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.TreePathScanner;
import ua.demo.agentlab.ai.rag.intelligence.model.JavaAstMethod;
import ua.demo.agentlab.ai.rag.intelligence.model.JavaAstParseResult;
import ua.demo.agentlab.ai.rag.intelligence.model.JavaAstType;
import ua.demo.agentlab.ai.rag.model.SourceDocument;

import javax.tools.JavaCompiler;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class JavaAstParser {

    public List<JavaAstParseResult> parse(List<SourceDocument> documents) {
        List<JavaAstParseResult> results = new ArrayList<>();
        for (SourceDocument document : documents) {
            if ("java".equalsIgnoreCase(document.language())) {
                results.add(parse(document));
            }
        }
        return results;
    }

    public JavaAstParseResult parse(SourceDocument document) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return JavaAstParseResult.failed(document.relativePath(), "JDK compiler is not available for AST parsing.");
        }

        InMemoryJavaFileObject sourceFile = new InMemoryJavaFileObject(document.relativePath(), document.content());
        List<String> warnings = new ArrayList<>();
        try (var fileManager = compiler.getStandardFileManager(null, Locale.ROOT, java.nio.charset.StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) compiler.getTask(
                    null,
                    fileManager,
                    diagnostic -> warnings.add(diagnostic.getKind() + ": " + diagnostic.getMessage(Locale.ROOT)),
                    List.of("-proc:none", "-XDshould-stop.ifError=PARSE"),
                    null,
                    List.of(sourceFile)
            );

            Iterable<? extends CompilationUnitTree> units = task.parse();
            List<JavaAstType> types = new ArrayList<>();
            String packageName = "";
            Set<String> imports = new LinkedHashSet<>();

            for (CompilationUnitTree unit : units) {
                if (unit.getPackageName() != null) {
                    packageName = unit.getPackageName().toString();
                }
                for (ImportTree importTree : unit.getImports()) {
                    imports.add(importTree.getQualifiedIdentifier().toString());
                }
                new TypeCollector(types).scan(unit, null);
            }

            boolean parsed = warnings.stream().noneMatch(message -> message.startsWith("ERROR"));
            double confidence = parsed
                    ? (types.isEmpty() ? 0.65d : 0.95d)
                    : 0.25d;
            return new JavaAstParseResult(
                    document.relativePath(),
                    packageName,
                    List.copyOf(imports),
                    types,
                    warnings,
                    parsed,
                    confidence
            );
        } catch (IOException exception) {
            return JavaAstParseResult.failed(document.relativePath(), exception.getMessage());
        }
    }

    private static final class TypeCollector extends TreePathScanner<Void, Void> {

        private final List<JavaAstType> types;

        private TypeCollector(List<JavaAstType> types) {
            this.types = types;
        }

        @Override
        public Void visitClass(ClassTree node, Void unused) {
            List<String> annotations = extractAnnotations(node.getModifiers());
            List<String> interfaces = node.getImplementsClause().stream()
                    .map(Tree::toString)
                    .toList();
            List<JavaAstMethod> methods = new ArrayList<>();
            for (Tree member : node.getMembers()) {
                if (member instanceof MethodTree methodTree) {
                    methods.add(toMethod(methodTree));
                }
            }
            types.add(new JavaAstType(
                    node.getSimpleName().toString(),
                    node.getKind().name(),
                    annotations,
                    node.getExtendsClause() == null ? "" : node.getExtendsClause().toString(),
                    interfaces,
                    methods
            ));
            return super.visitClass(node, unused);
        }

        private JavaAstMethod toMethod(MethodTree methodTree) {
            List<String> invocationTargets = new ArrayList<>();
            if (methodTree.getBody() != null) {
                new TreeScanner<Void, Void>() {
                    @Override
                    public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
                        ExpressionTree select = node.getMethodSelect();
                        invocationTargets.add(select.toString());
                        return super.visitMethodInvocation(node, unused);
                    }
                }.scan(methodTree.getBody(), null);
            }
            List<String> annotations = extractAnnotations(methodTree.getModifiers());
            boolean testMethod = annotations.stream().anyMatch(annotation ->
                    annotation.endsWith("Test") || annotation.endsWith(".Test"));
            return new JavaAstMethod(
                    methodTree.getName().toString(),
                    methodTree.getReturnType() == null ? "" : methodTree.getReturnType().toString(),
                    annotations,
                    methodTree.getParameters().stream().map(parameter -> parameter.getType().toString()).toList(),
                    invocationTargets,
                    testMethod
            );
        }

        private List<String> extractAnnotations(ModifiersTree modifiersTree) {
            if (modifiersTree == null) {
                return List.of();
            }
            List<String> annotations = new ArrayList<>();
            for (AnnotationTree annotation : modifiersTree.getAnnotations()) {
                annotations.add(annotation.getAnnotationType().toString());
            }
            return annotations;
        }
    }

    private static final class InMemoryJavaFileObject extends SimpleJavaFileObject {

        private final String content;

        private InMemoryJavaFileObject(String path, String content) {
            super(URI.create("string:///" + path.replace('\\', '/')), Kind.SOURCE);
            this.content = content;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return content;
        }
    }
}
