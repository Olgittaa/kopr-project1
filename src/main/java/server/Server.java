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
    private ServerSocket serverSocket;
    private DataOutputStream dos;
    private long filesSize;

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
                ObjectInputStream ois = new ObjectInputStream(managerSocket.getInputStream());
                dos = new DataOutputStream(managerSocket.getOutputStream());
                ConcurrentHashMap<String, Long> data = null;

                String action = ois.readUTF();
                log.info(action);
                if (action.equals("CONTINUE")) {
                    data = (ConcurrentHashMap<String, Long>) ois.readObject();
                }
                int count = ois.readInt();
                getFileBlockingQueue(new File(Constants.SOURCE_DIRECTORY));
                sendDataForBar();
                executeTasks(data, count);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void sendDataForBar() {
        try {
            dos.writeInt(fileBlockingQueue.size());
            dos.writeLong(filesSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getFileBlockingQueue(File rootDir) {
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

    public void executeTasks(ConcurrentHashMap<String, Long> data, int count) {
        try {
            if (serverSocket == null) {
                serverSocket = new ServerSocket(Constants.SERVER_PORT);
            }
            ExecutorService executor = Executors.newFixedThreadPool(count);
            for (int i = 0; i < count; i++) {
                Socket socket = serverSocket.accept();
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                FileSenderTask fileSenderTask = new FileSenderTask(executor, socket, dos, fileBlockingQueue, data);
                executor.execute(fileSenderTask);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}