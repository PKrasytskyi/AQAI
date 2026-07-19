package unit.tests.ui.testcontract;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.testcontract.assembly.UiTestContractAssembler;
import ua.demo.agentlab.ui.testcontract.assembly.UiTestContractAssemblyInput;
import ua.demo.agentlab.ui.testcontract.writer.DeterministicTestNgWriter;
import ua.demo.agentlab.persistence.GeneratedUiSources;
import ua.demo.agentlab.validation.SimpleGeneratedUiContractValidator;
import ua.demo.agentlab.validation.ValidationStatus;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DeterministicTestNgWriterTest {

    @Test
    public void rendersDeterministicTestNgSourcesWithoutRawSelenium() {
        var writer = new DeterministicTestNgWriter("generated.pages", "generated.tests");
        var bundle = new UiTestContractAssembler().assemble(new UiTestContractAssemblyInput(
                UiTestContractFixtures.canonicalBundle(),
                UiTestContractFixtures.assertionContracts(),
                UiTestContractFixtures.pomContracts()
        ));

        var first = writer.write(bundle);
        var second = writer.write(bundle);

        Assert.assertEquals(first.files(), second.files());
        Assert.assertEquals(first.sourceMap(), second.sourceMap());
        Assert.assertEquals(first.files().size(), 4);
        String source = first.files().stream().map(file -> file.content()).reduce("", String::concat);
        Assert.assertFalse(source.contains("WebDriver"));
        Assert.assertFalse(source.contains("driver.findElement"));
        Assert.assertFalse(source.contains("By."));
        Assert.assertTrue(source.contains("credentials(\"valid-user\")"));
        Assert.assertTrue(source.contains("loginPage.enterUsername(validCredentials.username())"));
        Assert.assertTrue(source.contains("dashboardPage.logout()"));
        Assert.assertTrue(source.contains("UiAssertions.assertTrue"));
    }

    @Test
    public void createsRequirementToGeneratedLineSourceMap() {
        var writer = new DeterministicTestNgWriter("generated.pages", "generated.tests");
        var result = writer.write(new UiTestContractAssembler().assemble(new UiTestContractAssemblyInput(
                UiTestContractFixtures.canonicalBundle(),
                UiTestContractFixtures.assertionContracts(),
                UiTestContractFixtures.pomContracts()
        )));

        Assert.assertEquals(result.sourceMap().entries().size(), 4);
        Assert.assertTrue(result.sourceMap().entries().stream().allMatch(entry -> entry.testMethodLine() > 0));
        Assert.assertTrue(result.sourceMap().entries().stream()
                .flatMap(entry -> entry.invocations().stream())
                .allMatch(invocation -> invocation.line() > 0));
    }

    @Test
    public void generatedTestsCompileTogetherWithDeterministicPageObjects() throws Exception {
        var bundle = new UiTestContractAssembler().assemble(new UiTestContractAssemblyInput(
                UiTestContractFixtures.canonicalBundle(),
                UiTestContractFixtures.assertionContracts(),
                UiTestContractFixtures.pomContracts()
        ));
        List<ua.demo.agentlab.ui.writer.GeneratedSourceFile> sources = new ArrayList<>();
        sources.addAll(new ua.demo.agentlab.ai.ui.contract.DeterministicPomJavaWriter("generated.pages")
                .write(UiTestContractFixtures.pomContracts()));
        sources.addAll(new DeterministicTestNgWriter("generated.pages", "generated.tests").write(bundle).files());

        Path root = Files.createTempDirectory("deterministic-testng-compile-");
        Path classes = Files.createDirectories(root.resolve("classes"));
        List<File> sourceFiles = new ArrayList<>();
        for (var source : sources) {
            Path path = root.resolve("src")
                    .resolve(source.packageName().replace('.', File.separatorChar))
                    .resolve(source.className() + ".java");
            Files.createDirectories(path.getParent());
            Files.writeString(path, source.content(), StandardCharsets.UTF_8);
            sourceFiles.add(path.toFile());
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(
                diagnostics,
                null,
                StandardCharsets.UTF_8
        )) {
            Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromFiles(sourceFiles);
            boolean compiled = compiler.getTask(
                    null,
                    fileManager,
                    diagnostics,
                    List.of("-classpath", System.getProperty("java.class.path"), "-d", classes.toString()),
                    null,
                    units
            ).call();
            Assert.assertTrue(compiled, diagnostics.getDiagnostics().toString());
        }
    }

    @Test
    public void generatedTestsPassPublicPomApiContractValidation() {
        var bundle = new UiTestContractAssembler().assemble(new UiTestContractAssemblyInput(
                UiTestContractFixtures.canonicalBundle(),
                UiTestContractFixtures.assertionContracts(),
                UiTestContractFixtures.pomContracts()
        ));
        var pages = new ua.demo.agentlab.ai.ui.contract.DeterministicPomJavaWriter("generated.pages")
                .write(UiTestContractFixtures.pomContracts());
        var tests = new DeterministicTestNgWriter("generated.pages", "generated.tests").write(bundle).files();

        var result = new SimpleGeneratedUiContractValidator().validate(new GeneratedUiSources(pages, tests));

        Assert.assertEquals(result.status(), ValidationStatus.PASSED, result.violations().toString());
    }
}
