package tdl.record.sourcecode.snapshot;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import tdl.record.sourcecode.content.SourceCodeProvider;
import tdl.record.sourcecode.snapshot.helpers.DirectoryDiffUtils;

public class SnapshotRecorder implements AutoCloseable {

    protected final SourceCodeProvider sourceCodeProvider;

    private Git git;

    private Path directory;

    private Path gitDirectory;

    private int counter = 0;

    private final int keySnapshotPacing;

    public SnapshotRecorder(SourceCodeProvider sourceCodeProvider, int keySnapshotPacing) {
        this.sourceCodeProvider = sourceCodeProvider;
        this.keySnapshotPacing = keySnapshotPacing;
        initTempDirectory();
        initGitDirectory();
    }

    private void initTempDirectory() {
        try {
            File sysTmpDir = FileUtils.getTempDirectory();
            directory = Files.createTempDirectory(
                    sysTmpDir.toPath(),
                    getClass().getSimpleName()
            );
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void initGitDirectory() {
        try {
            File sysTmpDir = FileUtils.getTempDirectory();
            gitDirectory = Files.createTempDirectory(
                    sysTmpDir.toPath(),
                    getClass().getSimpleName()
            );
            git = Git.init().setDirectory(gitDirectory.toFile()).call();
            commitAllChanges();
        } catch (IOException | GitAPIException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Git getGit() {
        return git;
    }

    public Path getGitDirectory() {
        return gitDirectory;
    }

    public void syncToGitDirectory() throws IOException {
        FileUtils.cleanDirectory(directory.toFile());
        sourceCodeProvider.retrieveAndSaveTo(directory);
        copyToGitDirectory();
        removeDeletedFileInOriginal();
    }

    private void copyToGitDirectory() throws IOException {
        FileFilter filter = (file) -> {
            Path relative = directory.relativize(file.toPath());
            return !((file.isDirectory() && relative.equals(".git"))
                    || relative.startsWith(".git/"));
        };
        FileUtils.copyDirectory(directory.toFile(), gitDirectory.toFile(), filter);
    }

    private void removeDeletedFileInOriginal() {
        List<String> files = DirectoryDiffUtils.getRelativeFilePathList(gitDirectory);
        files.stream().forEach((path) -> {
            if (path.startsWith(".git")) {
                return;
            }
            File file = gitDirectory.resolve(path).toFile();
            boolean isExists = file.exists()
                    && !directory.resolve(path).toFile().exists();
            if (isExists) {
                file.delete();
            }
        });
    }

    public void commitAllChanges() {
        try {
            String message = new Date().toString();
            git.add()
                    .addFilepattern(".")
                    .call();
            git.commit()
                    .setMessage(message)
                    .setAll(true)
                    .call();
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

    public Snapshot takeSnapshot() throws IOException {
        Snapshot snapshot;

        syncToGitDirectory();
        commitAllChanges();
        if (shouldTakeSnapshot()) {
            counter = 0;
            snapshot = takeKeySnapshot();
        } else {
            snapshot = takePatchSnapshot();
        }
        counter++;
        return snapshot;
    }

    private boolean shouldTakeSnapshot() {
        return counter % keySnapshotPacing == 0;
    }

    private KeySnapshot takeKeySnapshot() throws IOException {
        return KeySnapshot.takeSnapshotFromGit(git);
    }

    private PatchSnapshot takePatchSnapshot() throws IOException {
        return PatchSnapshot.takeSnapshotFromGit(git);
    }

    @Override
    public void close() {
        directory.toFile().deleteOnExit();
        git.close();
        gitDirectory.toFile().deleteOnExit();
    }
}
