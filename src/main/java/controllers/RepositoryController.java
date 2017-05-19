package controllers;

import console.ConsoleConstructor;
import data.ClientConfig;
import db.DbManager;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import workers.Repo;
import workers.TrackerWebService;

import javax.swing.*;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by cakor on 12/4/2016.
 */
public class RepositoryController implements Initializable{
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        assert startBtn != null : "Tracker Gui, startBtn is null";
        init();
    }

    @FXML
    private Button startBtn;

    @FXML
    private Button stopBtn;

    @FXML
    private TextField portText;

    @FXML
    private BorderPane parentBorderPane;

    private DbManager dbManager;
    private ClientConfig clientConfig;
    private Repo repo;

    private void init(){

        final SwingNode consoleNode = new SwingNode();
        createSwingContent(consoleNode);
        parentBorderPane.setCenter(consoleNode);

        initButtons();
        initPortTextField();

        dbManager = DbManager.getInstance();
        clientConfig = dbManager.GetClientConfig();

        if(clientConfig.isInBotMode())
            startRepo();

    }

    private void createSwingContent(final SwingNode swingNode) {
        SwingUtilities.invokeLater(() -> {
            swingNode.setContent(new ConsoleConstructor());
        });
    }


    private void initButtons(){
        startBtn.setOnAction(event -> {
            startRepo();
        });

        stopBtn.setOnAction(event -> {
            stopRepo();
        });
    }

    private void initPortTextField(){

    }

    private void startRepo(){

        startBtn.setText("Started...");
        startBtn.setDisable(true);
        if(clientConfig.hasPreferredPort())
            portText.setText(String.valueOf(clientConfig.getPreferredPort()));
        portText.setDisable(true);
        stopBtn.setDisable(false);

        repo = new Repo(Integer.parseInt(portText.getText()));
        repo.start();
    }

    private void stopRepo(){
        startBtn.setText("Start");
        startBtn.setDisable(false);
        portText.setDisable(false);
        stopBtn.setDisable(true);

        repo.stopServer();

    }

}
