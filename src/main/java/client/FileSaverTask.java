package client;

import constants.Constants;
import gui.ProgressBarTask;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
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
    private CountDownLatch counter;

    public FileSaverTask(Socket socket, DataInputStream inputStream, ProgressBarTask progressBarTask, ConcurrentHashMap<String, Long> data, CountDownLatch counter) {
        this.socket = socket;
        this.inputStream = inputStream;
        this.progressBarTask = progressBarTask;
        this.data = data;
        this.counter = counter;
    }

    public boolean downloadFiles() {
        try {
            String s = inputStream.readUTF();
            log.info(s, socket.getLocalPort());
            if (s.equals(Constants.POISON_PILL.getName())) {
                return true;
            }
            File file = new File(s);
            new File(String.valueOf(file.getParentFile())).mkdirs();
            writeBytes(file, inputStream);
            progressBarTask.updateNumberBar();
        } catch (EOFException e) {
//            log.warn("eoef", socket.getLocalPort());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void writeBytes(File file, DataInputStream inputStream) {
        RandomAccessFile raf;
        try {
            raf = new RandomAccessFile(file, "rws");
            raf.seek(inputStream.readLong());
            long read = 0;
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
            log.info("recieve " + file.getAbsolutePath());
        } catch (SocketException ex) {
            counter.countDown();
            data.put(file.getAbsolutePath(), file.length());
            ex.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            boolean poison = false;
            do {
                poison = downloadFiles();
            } while (!poison);
            counter.countDown();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
