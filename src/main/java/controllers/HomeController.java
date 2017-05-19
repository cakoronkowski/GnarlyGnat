package controllers;

import data.ChunkAudit;
import data.ClientTorrent;
import data.TorrentCache;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by Stephen on 12/5/16.
 */
public class HomeController implements Initializable {


    @FXML
    public FlowPane parentFlowPane;
    @FXML
    public VBox overallVbox;
    @FXML
    public VBox torrentVBox;
    @FXML
    public ChoiceBox torrentChoiceBox;
    @FXML
    private Button refreshBtn;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        init();
    }

    private void init(){
        initSections();
        initGraphs();
        refreshBtn.setOnAction(event -> {
            torrentChoiceBox.setItems(FXCollections.observableArrayList(TorrentCache.getHashTorrents().keySet()));
        });

        torrentChoiceBox.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            loadTorrent((String)torrentChoiceBox.getItems().get(newValue.intValue()));
        });
    }

    private void initSections(){

    }

    private void initGraphs(){

    }

    private void initTables(){

    }

    private void loadTorrent(String id)
    {
        ClientTorrent torrent = TorrentCache.getTorrentById(id);
        for(ChunkAudit audit  : torrent.Audit.ChunkAudits)
        {
            System.out.println(audit.ChunkNum + "," + audit.StartReqTime + "," + audit.EndReqTime + "," + audit.getTransferTime() + "," + audit.ChunkFailed);
        }

        System.out.println("Torrent Audit: peers: " + torrent.Audit.NumberOfPeers + " lookup" + torrent.Audit.LookupTimeForPeers);

    }
}
