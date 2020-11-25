package client;

import constants.Constants;
import gui.ProgressBarTask;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressBar;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class Client extends Service<Boolean> {
    private final ExecutorService executor;
    private final ProgressBar numberProgressBar;
    private final ProgressBar sizeProgressBar;
    private final ConcurrentHashMap<String, Long> data;
    private final CountDownLatch gate;
    private final int count;
    private ProgressBarTask progressBarTask;
    private DataInputStream dis;
    private ObjectOutputStream oos;
    private int filesCount;
    private long filesSize;

    public Client(ProgressBar numberProgressBar, ProgressBar sizeProgressBar, int count) {
        this.count = count;
        this.numberProgressBar = numberProgressBar;
        this.sizeProgressBar = sizeProgressBar;

        this.data = readData();
        this.executor = Executors.newFixedThreadPool(count);
        this.gate = new CountDownLatch(count);
    }

    public static void saveData(ConcurrentHashMap<String, Long> data) {
        try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream("map.data"))) {
            os.writeObject(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Task<Boolean> createTask() {
        return new Task<>() {
            @Override
            protected Boolean call(){
                doConnect();

                try {
                    if (data == null || data.size() == 0) {
                        oos.writeUTF("START");
                    } else {
                        oos.writeUTF("CONTINUE");
                        oos.writeObject(data);
                    }
                    oos.writeInt(count);
                    oos.flush();

                    filesCount = dis.readInt();
                    filesSize = dis.readLong();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                progressBarTask = new ProgressBarTask(numberProgressBar, sizeProgressBar,
                        new AtomicInteger(filesCount), new AtomicLong(filesSize), data);
                connect();
                return true;
            }
        };
    }

    public void doConnect() {
        try {
            Socket managerSocket = new Socket(Constants.SERVER_HOST, Constants.MANAGER_PORT);
            oos = new ObjectOutputStream(managerSocket.getOutputStream());
            dis = new DataInputStream(managerSocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void connect() {
        for (int i = 0; i < count; i++) {
            try {
                Socket socket = new Socket(Constants.SERVER_HOST, Constants.SERVER_PORT);
                FileSaverTask task = new FileSaverTask(socket,
                        new DataInputStream(socket.getInputStream()), progressBarTask, data, gate);
                executor.execute(task);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        executor.shutdown();
    }

    public void stop() {
        if (progressBarTask.isDone()) {
            new File("map.data").delete();
        } else {
            try {
                executor.shutdownNow();
                gate.await();
                saveData(data);
                cancel();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private ConcurrentHashMap<String, Long> readData() {
        ConcurrentHashMap<String, Long> data = new ConcurrentHashMap<>();
        File file = new File("map.data");
        if (file.exists()) {
            try {
                ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream("map.data"));
                data = (ConcurrentHashMap<String, Long>) objectInputStream.readObject();
                objectInputStream.close();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        file.delete();
        return data;
    }
}