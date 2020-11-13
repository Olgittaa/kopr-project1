package server;

import constants.Constants;
import lombok.extern.slf4j.Slf4j;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

@Slf4j
public class Server {
    private BlockingQueue<File> fileBlockingQueue;
    private long filesSize;
    private Socket socket;
    private ObjectInputStream dataInputStream;
    private DataOutputStream dataOutputStream;

    public Server() {
        initConnection();
    }

    public static void main(String[] args) {
        new Server();
    }

    public void initConnection() {
        try (ServerSocket serverSocket = new ServerSocket(Constants.MANAGER_PORT)) {
            while (true) {
                Socket managerSocket = serverSocket.accept();
                dataInputStream = new ObjectInputStream(managerSocket.getInputStream());
                dataOutputStream = new DataOutputStream(managerSocket.getOutputStream());
                ConcurrentHashMap<String, Long> data = null;

                String action = dataInputStream.readUTF();
                log.info(action);

                if (action.equals("CONTINUE")) {
                    data = (ConcurrentHashMap<String, Long>) dataInputStream.readObject();
                }
                getLists(new File(Constants.SOURCE_DIRECTORY), data);
                sendDataForBar();
                exetuteTasks(data);
                managerSocket.close();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void sendDataForBar() {
        try {
            dataOutputStream.writeInt(fileBlockingQueue.size());
            dataOutputStream.writeLong(filesSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getLists(File rootDir, ConcurrentHashMap<String, Long> data) {
        File[] files = rootDir.listFiles();
        fileBlockingQueue = new LinkedBlockingQueue<>();
        assert files != null;
        if (files.length == 0) {
            fileBlockingQueue.add(rootDir);
        }
        CopyOnWriteArrayList<FileSearcherTask> listTasks = new CopyOnWriteArrayList<>();
        FileSearcherTask fileSearcherTask = new FileSearcherTask(listTasks, fileBlockingQueue, rootDir);
        listTasks.add(fileSearcherTask);
        fileSearcherTask.fork();

        filesSize = 0;
        for (FileSearcherTask task : listTasks) {
            filesSize += task.join();
        }
    }

    public void exetuteTasks(ConcurrentHashMap<String, Long> data) {
        try {
            ServerSocket serverSocket = new ServerSocket(Constants.SERVER_PORT);
            ExecutorService executor = Executors.newFixedThreadPool(4);
            for (int i = 0; i < 4; i++) {
                Socket socket = serverSocket.accept();
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                FileSenderTask fileSenderTask = new FileSenderTask(executor, socket, dataOutputStream, fileBlockingQueue, data);
                executor.execute(fileSenderTask);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}