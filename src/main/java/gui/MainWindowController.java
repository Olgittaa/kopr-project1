package gui;

import client.Client;
import constants.Constants;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MainWindowController {

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

    private Client clientManager;

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

    public void onStartButtonClick() {
        clientManager = new Client(numberProgressBar, weigthProgressBar, tcpSpinner.getValue());
        clientManager.start();
    }

    public void onFinishButtonClick() {
        clientManager.stop();
        System.exit(0);
    }

    public void onPauseButtonClick() {
        clientManager.stop();
    }
}
