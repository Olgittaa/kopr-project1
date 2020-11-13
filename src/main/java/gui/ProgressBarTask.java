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
    private final AtomicInteger files;
    private final AtomicInteger sendedFiles = new AtomicInteger(0);
    private final AtomicLong sendedSize = new AtomicLong(0);
    private final ConcurrentHashMap<String, Long> data;
    private final AtomicLong fileSize;

    public ProgressBarTask(ProgressBar numberBar, ProgressBar sizeBar,
                           AtomicInteger files, AtomicLong fileSize,
                           ConcurrentHashMap<String, Long> data) {
        this.files = files;
        this.fileSize = fileSize;
        this.sizeBar = sizeBar;
        this.numberBar = numberBar;
        this.data = data;
        getSendedSize();
    }

    public synchronized void updateNumberBar() {
        int file = sendedFiles.incrementAndGet();
        numberBar.setProgress((double) file / files.get());
    }

    public void getSendedSize() {
        sendedSize.set(0);
        if (data != null && data.size() > 0) {
            for (Long value : data.values()) {
                sendedSize.addAndGet(value);
            }
        }
    }

    public boolean isDone() {
        return sendedSize.get() == fileSize.get();
    }

    public synchronized void updateSizeBar(long size) {
        sendedSize.getAndAdd(size);
        sizeBar.setProgress((double) sendedSize.get() / fileSize.get());
    }

}