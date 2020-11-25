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
    private final DataInputStream dis;
    private final Socket socket;
    private final ProgressBarTask progressBarTask;
    private final ConcurrentHashMap<String, Long> data;
    private final CountDownLatch gate;

    public FileSaverTask(Socket socket, DataInputStream dis,
                         ProgressBarTask progressBarTask,
                         ConcurrentHashMap<String, Long> data,
                         CountDownLatch gate) {
        this.socket = socket;
        this.dis = dis;
        this.progressBarTask = progressBarTask;
        this.data = data;
        this.gate = gate;
    }

    public boolean downloadFiles() throws IOException {
        String fileName = dis.readUTF();
        if (fileName.equals(Constants.POISON_PILL.getName())) {
            log.info("poison.pill");
            return true;
        }
        File file = new File(fileName);
        new File(String.valueOf(file.getParentFile())).mkdirs();
        writeBytes(file, dis);
        progressBarTask.updateNumberBar();
        return false;
    }

    private void writeBytes(File file, DataInputStream dis) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rws")) {
            long offset = dis.readLong();
            raf.seek(offset);
            long totalRead = offset;
            long chunk = dis.readLong();
            byte[] fileBytes = new byte[(int) chunk];
            long read;
            while (!Thread.currentThread().isInterrupted() &&
                    chunk > 0 && (read = dis.readNBytes(fileBytes, 0, (int) chunk)) > 0) {
                totalRead = totalRead + read;
                raf.write(fileBytes);
                chunk = dis.readLong();
                fileBytes = new byte[(int) chunk];
                progressBarTask.updateSizeBar(read);
                data.put(file.getAbsolutePath(), totalRead);
            }
            log.info("receive " + file.getAbsolutePath());
        } catch (SocketException ex) {
            gate.countDown();
            data.put(file.getAbsolutePath(), file.length());
        }
    }

    @Override
    public void run() {
        try {
            boolean poison;
            do {
                poison = downloadFiles();
            } while (!poison);
        } catch (IOException e) {
            Client.saveData(data);
        } finally {
            gate.countDown();
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
