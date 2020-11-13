package server;

import constants.Constants;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RecursiveTask;

@Slf4j
public class FileSearcherTask extends RecursiveTask<Long> {
    private final File dir;
    private final BlockingQueue<File> fileBlockingQueue;
    private CopyOnWriteArrayList<FileSearcherTask> listTasks;

    public FileSearcherTask(CopyOnWriteArrayList<FileSearcherTask> listTasks, BlockingQueue<File> fileBlockingQueue, File dir) {
        this.fileBlockingQueue = fileBlockingQueue;
        this.listTasks = listTasks;
        this.dir = dir;
    }

    private long getFiles() {
        long fileSize = 0;
        File[] files = dir.listFiles();
        assert files != null;
        if (files.length == 0) {
            fileBlockingQueue.add(dir);
        }
        listTasks = new CopyOnWriteArrayList<>();
        for (File file : files) {
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
        if (dir == Constants.POISON_PILL) {
            try {
                fileBlockingQueue.put(dir);
                return 0L;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return getFiles();
    }
}
