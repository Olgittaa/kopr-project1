package gui;

import client.Client;
import constants.Constants;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;

@Slf4j
public class MainWindowController {
    private Client clientManager;
    private CountDownLatch counter;
    @FXML
    private Button startButton;
    @FXML
    private Button finishButton;
    @FXML
    private Button pauseButton;
    @FXML
    private ProgressBar weigthProgressBar;
    @FXML
    private ProgressBar numberProgressBar;
    @FXML
    private Label fromDirLabel;
    @FXML
    private Label toDirLabel;
    @FXML
    private Label portLabel;
    @FXML
    private Spinner<Integer> tcpSpinner;

    public MainWindowController() {
    }

    @FXML
    void initialize() {

        fromDirLabel.setText(Constants.SOURCE_DIRECTORY);
        toDirLabel.setText(Constants.DESTINATION_DIRECTORY);
        portLabel.setText(String.valueOf(Constants.SERVER_PORT));
        SpinnerValueFactory<Integer> spinner = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 4);
        tcpSpinner.setValueFactory(spinner);
    }

    public void onStartButtonClick(MouseEvent mouseEvent) {
        Constants.setSocketsCount(tcpSpinner.getValue());
        counter = new CountDownLatch(Constants.SOCKETS_COUNT);
        clientManager = new Client(numberProgressBar, weigthProgressBar, counter);
        clientManager.start();
    }

    public void onFinishButtonClick(MouseEvent mouseEvent) {
        System.exit(0);
    }

    public void onPauseButtonClick(MouseEvent mouseEvent) {
        log.info("pause");
        clientManager.stop();

    }
}