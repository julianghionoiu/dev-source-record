package tdl.record.sourcecode.snapshot.file;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SnapshotsFileWriterTest {

    @Rule
    public TemporaryFolder sourceFolder = new TemporaryFolder();

    @Rule
    public TemporaryFolder destinationFolder = new TemporaryFolder();

    @Test
    public void run() throws IOException {
        Path output = destinationFolder.newFile("snapshot.bin").toPath();
        Path dirPath = Paths.get("src/test/resources/directory_snapshot/dir1");
        Path sourceDir = sourceFolder.getRoot().toPath();
        FileUtils.copyDirectory(dirPath.toFile(), sourceDir.toFile());

        try (SnapshotsFileWriter writer = new SnapshotsFileWriter(output, sourceDir, false)) {
            writer.takeSnapshot();

            appendString(sourceDir, "file1.txt", "\nLOREM");
            writer.takeSnapshot();

            appendString(sourceDir, "file1.txt", "\nIPSUM");
            writer.takeSnapshot();

            appendString(sourceDir, "file1.txt", "\nDOLOR");
            writer.takeSnapshot();

            appendString(sourceDir, "file1.txt", "\nSIT");
            writer.takeSnapshot();
            
            appendString(sourceDir, "file2.txt", "\nLOREM");
            writer.takeSnapshot();
            
            appendString(sourceDir, "file4.txt", "\nIPSUM");
            writer.takeSnapshot();
            
            appendString(sourceDir, "file5.txt", "\nDOLOR");
            writer.takeSnapshot();
        }
    }

    private static void appendString(Path dir, String path, String data) throws IOException {
        FileUtils.writeStringToFile(dir.resolve(path).toFile(), data, Charset.defaultCharset(), true);
    }
}