package server;

import constants.Constants;
import lombok.extern.slf4j.Slf4j;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Slf4j
public class FileSenderTask implements Runnable {

    private final BlockingQueue<File> fileBlockingQueue;
    private final ConcurrentHashMap<String, Long> data;
    private final DataOutputStream dos;
    private final Executor executor;
    private final Socket socket;
    private File file;

    public FileSenderTask(Executor executor, Socket socket, DataOutputStream dos, BlockingQueue<File> fileBlockingQueue, ConcurrentHashMap<String, Long> data) {
        this.dos = dos;
        this.socket = socket;
        this.executor = executor;
        this.fileBlockingQueue = fileBlockingQueue;
        this.data = data;
        try {
            this.file = fileBlockingQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        sendFile();
        if (fileBlockingQueue.size() > 0) {
            executor.execute(new FileSenderTask(executor, socket, dos, fileBlockingQueue, data));
        } else {
            sendPoison();
        }
    }

    private String getDestination() {
        return Constants.DESTINATION_DIRECTORY +
                file.getAbsolutePath().substring(Constants.SOURCE_DIRECTORY.length());
    }

    public void sendPoison() {
        try {
            log.info("sendpoison " + socket.getPort() + " " + Constants.POISON_PILL);
            dos.writeUTF(Constants.POISON_PILL.getName());
            socket.close();
        } catch (SocketException e1) {
            System.err.println(e1.getMessage() + " " + socket.getPort());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void sendFile() {
        try {
            String destination = getDestination();
            dos.writeUTF(destination);
            Long offset;
            long len = (int) file.length();
            if (data == null || !data.containsKey(destination)) {
                offset = 0L;
            } else {
                offset = data.get(destination);
            }
            writeBytes(len, offset);
        } catch (IOException ignored) {

        }
    }

    private void writeBytes(long len, long offset) throws IOException {
        log.info(fileBlockingQueue.size() + " sendfile " + socket.getPort() + " " + file.getAbsolutePath() + " " + (len - offset));
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            dos.writeLong(offset);
            raf.seek(offset);

            long totalRead = offset;
            int read;
            long chunk;
            byte[] fileBytes;
            if (len - offset < Constants.CHUNK_SIZE) {
                chunk = len - offset;
                fileBytes = new byte[(int) chunk];
            } else {
                chunk = Constants.CHUNK_SIZE;
                fileBytes = new byte[Constants.CHUNK_SIZE];
            }

            dos.writeLong(chunk);

            while (totalRead < len &&
                    (read = raf.read(fileBytes, 0, (int) chunk)) >= 0) {
                totalRead += read;
                dos.write(fileBytes);
                if (len - totalRead < Constants.CHUNK_SIZE) {
                    chunk = len - totalRead;
                    fileBytes = new byte[(int) chunk];
                } else {
                    chunk = Constants.CHUNK_SIZE;
                    fileBytes = new byte[Constants.CHUNK_SIZE];
                }
                dos.writeLong(chunk);
                dos.flush();
            }
        }
    }
}
