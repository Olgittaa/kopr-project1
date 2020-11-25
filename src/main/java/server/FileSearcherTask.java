package server;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RecursiveTask;

@Slf4j
public class FileSearcherTask extends RecursiveTask<Long> {
    private final File dir;
    private final BlockingQueue<File> fileBlockingQueue;
    private CopyOnWriteArrayList<FileSearcherTask> listTasks;

    public FileSearcherTask(CopyOnWriteArrayList<FileSearcherTask> listTasks,
                            BlockingQueue<File> fileBlockingQueue, File dir) {
        this.fileBlockingQueue = fileBlockingQueue;
        this.listTasks = listTasks;
        this.dir = dir;
    }

    private long getFiles() {
        long fileSize = 0;
        File[] files = dir.listFiles();
        listTasks = new CopyOnWriteArrayList<>();
        for (File file : Objects.requireNonNull(files)) {
            if (file.isDirectory()) {
                FileSearcherTask task =
                        new FileSearcherTask(listTasks, fileBlockingQueue, file);
                listTasks.add(task);
                task.fork();
            } else {
                fileBlockingQueue.add(file);
                fileSize += file.length();
            }
        }
        for (FileSearcherTask task : listTasks) {
            fileSize += task.join();
        }
        return fileSize;
    }

    @Override
    protected Long compute() {
        return getFiles();
    }
}