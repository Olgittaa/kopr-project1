package gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientGUI extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("Total Memory (in bytes): " + Runtime.getRuntime().totalMemory());
        System.out.println("Free Memory (in bytes): " + Runtime.getRuntime().freeMemory());
        System.out.println("Max Memory (in bytes): " + Runtime.getRuntime().maxMemory());
        Parent root = FXMLLoader.load(getClass().getResource("mainWindow.fxml"));
        primaryStage.setTitle("DirCopy");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}