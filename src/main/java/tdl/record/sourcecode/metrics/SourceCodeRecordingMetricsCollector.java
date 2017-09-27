package tdl.record.sourcecode.metrics;

import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SourceCodeRecordingMetricsCollector implements SourceCodeRecordingListener {
    private boolean isCurrentlyRecording;
    private int totalSnapshots;
    private long timestampBeforeProcessingNanos;
    private long lastSnapshotProcessingTimeNano;


    public SourceCodeRecordingMetricsCollector() {
        timestampBeforeProcessingNanos = 0;
        totalSnapshots = 0;
    }

    //~~~~~~~~~~ Collectors

    @Override
    public void notifyRecordingStart(Path destinationPath) {
        this.isCurrentlyRecording = true;
        log.info("Start recording to \"" + destinationPath.getFileName());
    }

    @Override
    public void notifySnapshotStart(long timestamp, TimeUnit unit) {
        log.debug("Source Code snap !");
        timestampBeforeProcessingNanos = unit.toNanos(timestamp);
    }

    @Override
    public void notifySnapshotEnd(long timestamp, TimeUnit unit) {
        lastSnapshotProcessingTimeNano = unit.toNanos(timestamp) - timestampBeforeProcessingNanos;
        log.debug("lastSnapshotProcessingTimeNano: {}", lastSnapshotProcessingTimeNano);
        totalSnapshots++;
    }

    @Override
    public void notifyRecordingEnd() {
        this.isCurrentlyRecording = false;
        log.info("Recording stopped");
    }


    //~~~~~~~~~~ Getters


    public boolean isCurrentlyRecording() {
        return isCurrentlyRecording;
    }

    /**
     * @return total number of captured snapshots
     */
    public int getTotalSnapshots() {
        return totalSnapshots;
    }

    /**
     * @return total time it took to capture last snapshot
     */
    public long getLastSnapshotProcessingTimeNano() {
        return lastSnapshotProcessingTimeNano;
    }
}