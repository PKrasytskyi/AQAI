package unit.tests.persistence;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.persistence.LocalGeneratedFileWriter;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.nio.file.Files;
import java.nio.file.Path;

public class LocalGeneratedFileWriterTest {

    @Test
    public void skipsPhysicalWriteWhenGeneratedContentIsUnchanged() throws Exception {
        Path path = Path.of("target/test-generated/idempotent/Sample.java");
        Files.createDirectories(path.getParent());
        Files.writeString(path, "class Sample {}\n");
        long firstModified = Files.getLastModifiedTime(path).toMillis();

        Thread.sleep(5L);
        new LocalGeneratedFileWriter().write(new GeneratedSourceFile(
                "unit.tests.generated",
                "Sample",
                path.toString(),
                "class Sample {}\n"
        ));

        long secondModified = Files.getLastModifiedTime(path).toMillis();
        Assert.assertEquals(secondModified, firstModified);
    }
}
