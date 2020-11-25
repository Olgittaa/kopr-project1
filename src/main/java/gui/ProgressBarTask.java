package gui;

import javafx.scene.control.ProgressBar;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class ProgressBarTask {
    private final ProgressBar numberBar;
    private final ProgressBar sizeBar;
    private final AtomicInteger numberOfFiles;
    private final AtomicInteger sentFiles = new AtomicInteger(0);
    private final AtomicLong sentSize = new AtomicLong(0);
    private final ConcurrentHashMap<String, Long> data;
    private final AtomicLong totalSize;

    public ProgressBarTask(ProgressBar numberBar, ProgressBar sizeBar,
                           AtomicInteger numberOfFiles, AtomicLong totalSize,
                           ConcurrentHashMap<String, Long> data) {
        this.numberOfFiles = numberOfFiles;
        this.totalSize = totalSize;
        this.sizeBar = sizeBar;
        this.numberBar = numberBar;
        this.data = data;
        getSentSize();
    }

    public synchronized void updateNumberBar() {
        int file = sentFiles.incrementAndGet();
        numberBar.setProgress((double) file / numberOfFiles.get());
    }

    public void getSentSize() {
        sentSize.set(0);
        if (data != null && data.size() > 0) {
            for (Long value : data.values()) {
                sentSize.addAndGet(value);
            }
        }
    }

    public boolean isDone() {
        return sentSize.get() == totalSize.get();
    }

    public synchronized void updateSizeBar(long size) {
        sentSize.getAndAdd(size);
        sizeBar.setProgress((double) sentSize.get() / totalSize.get());
    }

}