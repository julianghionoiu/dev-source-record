package tdl.record.sourcecode.snapshot.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import org.apache.commons.io.IOUtils;

public class SnapshotsFileReader implements Iterator<SnapshotFileSegment>, AutoCloseable {

    private final File file;

    private final FileInputStream inputStream;

    public SnapshotsFileReader(File file) throws FileNotFoundException, IOException {
        this.file = file;
        this.inputStream = new FileInputStream(file);
    }

    @Override
    public boolean hasNext() {
        try {
            return inputStream.available() > 0;
        } catch (IOException ex) {
            return false;
        }
    }

    @Override
    public SnapshotFileSegment next() {
        try {
            byte[] header = readHeader();
            SnapshotFileSegment snapshot = SnapshotFileSegment.createFromHeaderBytes(header);
            byte[] data = readData((int) snapshot.size);
            snapshot.data = data;

            return snapshot;
        } catch (IOException ex) {
            return null;
        }
    }

    @Override
    public void remove() {
        Iterator.super.remove();
    }

    @Override
    public void forEachRemaining(Consumer<? super SnapshotFileSegment> action) {
        Iterator.super.forEachRemaining(action); //To change body of generated methods, choose Tools | Templates.
    }

    public void reset() throws IOException {
        inputStream.getChannel().position(0);
    }

    public void skip() throws IOException {
        byte[] header = readHeader();
        SnapshotFileSegment snapshot = SnapshotFileSegment.createFromHeaderBytes(header);
        inputStream.skip(snapshot.size);
    }

    public byte[] readHeader() throws IOException {
        return readData(SnapshotFileSegment.HEADER_SIZE);
    }

    public byte[] readData(int size) throws IOException {
        byte[] data = new byte[size];
        inputStream.read(data);
        return data;
    }

    public List<Date> getDates() {
        //TODO: need to do manual skip
        List<Date> list = new ArrayList<>();
        this.forEachRemaining((SnapshotFileSegment snapshot) -> {
            list.add(new Date(snapshot.timestamp * 1000L));
        });
        return list;
    }

    public List<SnapshotFileSegment> getSnapshots() {
        List<SnapshotFileSegment> list = new ArrayList<>();
        this.forEachRemaining(list::add);
        return list;
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(inputStream);
    }

    public File getFile() {
        return file;
    }

}
