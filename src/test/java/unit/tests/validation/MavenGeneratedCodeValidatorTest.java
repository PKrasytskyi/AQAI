package unit.tests.validation;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.validation.MavenGeneratedCodeValidator;

import java.nio.file.Path;
import java.util.List;

public class MavenGeneratedCodeValidatorTest {

    @Test
    public void resolvesCompileRootToCurrentGenerationNamespace() {
        Path project = Path.of(".").toAbsolutePath().normalize();
        MavenGeneratedCodeValidator validator = new MavenGeneratedCodeValidator(project.toString());
        String namespace = "ua/demo/agentlab/ui/generated/the_internet_12345678";
        List<String> paths = List.of(
                project.resolve("src/test/java/" + namespace + "/pages/LoginPage.java").toString(),
                project.resolve("src/test/java/" + namespace + "/tests/REQ001Test.java").toString()
        );

        Assert.assertEquals(
                validator.commonSourceRoot(paths),
                project.resolve("src/test/java/" + namespace)
        );
    }

    @Test
    public void rejectsSharedTestSourceRootWithoutProjectNamespace() {
        Path project = Path.of(".").toAbsolutePath().normalize();
        MavenGeneratedCodeValidator validator = new MavenGeneratedCodeValidator(project.toString());

        Assert.expectThrows(IllegalArgumentException.class, () -> validator.commonSourceRoot(List.of(
                project.resolve("src/test/java/First.java").toString(),
                project.resolve("src/test/java/Second.java").toString()
        )));
    }
}
