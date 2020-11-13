package gui;

import javafx.scene.control.ProgressBar;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ProgressBarTask {
    private final ProgressBar numberBar;
    private final ProgressBar sizeBar;
    private final AtomicInteger files;
    private final AtomicLong fileSize;
    private final AtomicInteger sendedFiles = new AtomicInteger(0);
    private final AtomicLong sendedSize = new AtomicLong(0);

    public ProgressBarTask(ProgressBar numberBar, ProgressBar sizeBar,
                           AtomicInteger files, AtomicLong fileSize) {
        this.files = files;
        this.fileSize = fileSize;
        this.sizeBar = sizeBar;
        this.numberBar = numberBar;
    }

    public synchronized void updateNumberBar() {
        numberBar.setProgress((double)sendedFiles.incrementAndGet() / files.get());
    }

    public AtomicInteger getSendedFiles() {
        return sendedFiles;
    }

    public AtomicLong getSendedSize() {
        return sendedSize;
    }

    public synchronized void updateSizeBar(long size) {
        sendedSize.getAndAdd(size);
        sizeBar.setProgress((double) sendedSize.get() / fileSize.get());
    }
}
