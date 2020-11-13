package client;

import constants.Constants;
import gui.ProgressBarTask;
import lombok.extern.slf4j.Slf4j;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class FileSaverTask implements Runnable {
    private final DataInputStream inputStream;
    private final Socket socket;
    private final ProgressBarTask progressBarTask;
    private final ConcurrentHashMap<String, Long> data;
    private final CountDownLatch counter;

    public FileSaverTask(Socket socket, DataInputStream inputStream, ProgressBarTask progressBarTask, ConcurrentHashMap<String, Long> data, CountDownLatch counter) {
        this.socket = socket;
        this.inputStream = inputStream;
        this.progressBarTask = progressBarTask;
        this.data = data;
        this.counter = counter;
    }

    public boolean downloadFiles() throws IOException {
        String s = inputStream.readUTF();
        if (s.equals(Constants.POISON_PILL.getName())) {
            log.info("poison.pill");
            return true;
        }
        File file = new File(s);
        new File(String.valueOf(file.getParentFile())).mkdirs();
        writeBytes(file, inputStream);
        progressBarTask.updateNumberBar();
        return false;
    }

    private void writeBytes(File file, DataInputStream inputStream) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rws")) {
            long offset = inputStream.readLong();
            raf.seek(offset);
            long read = offset;
            long numRead = 0;
            long chunk = inputStream.readLong();
            byte[] fileBytes = new byte[(int) chunk];
            while (!Thread.currentThread().isInterrupted() &&
                    chunk > 0 && (numRead = inputStream.readNBytes(fileBytes, 0, (int) chunk)) > 0) {
                read = read + numRead;
                raf.write(fileBytes);
                chunk = inputStream.readLong();
                fileBytes = new byte[(int) chunk];
                progressBarTask.updateSizeBar(numRead);
                data.put(file.getAbsolutePath(), read);
            }
            log.info("receive " + file.getAbsolutePath());
        } catch (SocketException ex) {
            counter.countDown();
            data.put(file.getAbsolutePath(), file.length());
        }
    }

    @Override
    public void run() {
        try {
            boolean poison = false;
            do {
                poison = downloadFiles();
            } while (!poison);
        } catch (IOException e) {
            Client.saveData(data);
        } finally {
            counter.countDown();
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
