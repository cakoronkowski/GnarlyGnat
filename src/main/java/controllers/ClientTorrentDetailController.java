package controllers;

import data.ChunkStatus;
import data.ClientTorrent;
import data.PeerInfo;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import javafx.event.ActionEvent;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Created by Stephen on 11/26/16.
 */
public class ClientTorrentDetailController implements Initializable {

    @FXML
    private Tab infoTab;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private FlowPane chunkHBox;
    @FXML
    private Text fileNameText;
    @FXML
    private Text fileSizeText;
    @FXML
    private Text createDateText;
    @FXML
    private Text fileLocationText;
    @FXML
    private Button changeFileLocationBtn;
    @FXML
    private TableView peersTableView;
    @FXML
    private VBox parentVbox;
    @FXML
    private AnchorPane parentPane;


    List<Rectangle> chunkRects;

    private ClientTorrent currentTorrent;

    private Rectangle completeRect;
    private Rectangle errorRect;
    private Rectangle incompleteRect;

    private DoubleProperty rectWidth;
    private int numberOfChunks = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            System.out.println("DETAIL CONTROLLER");
            assert infoTab != null : "fx:id=\"navTreeView\" was not injected: check your FXML file 'client.fxml'.";
            assert fileLocationText != null : "fileLocationText is null";
            init();
        } catch (Exception ex)
        {
            System.err.println("CLIENT CONTROLLER INIT");
            ex.printStackTrace();
        }
    }

    private void init(){
        rectWidth = new SimpleDoubleProperty(10);
        chunkRects = new ArrayList<>();
        currentTorrent = new ClientTorrent();
        initPeerTable();
        initProgressBar();
        initTextBinding();
        initChunkBar();

        parentVbox.widthProperty().addListener((observable, oldValue, newValue) -> {

            System.out.println("changing val: " + newValue.doubleValue() + "to: " + (newValue.doubleValue() - 80.0)/numberOfChunks + " chunks: " + numberOfChunks);
            if(numberOfChunks > 0)
                rectWidth.setValue((newValue.doubleValue()-80)/numberOfChunks);
            else
                rectWidth.setValue(4);
        });

    }

    @FXML
    public void changeLocation(ActionEvent actionEvent) {
        System.out.println("changing location");
    }


    public void initPeerTable(){
//        TableColumn<PeerInfo, String> typeCol = new TableColumn<>("Type");
//        typeCol.setCellValueFactory(new PropertyValueFactory<PeerInfo, String>("Status"));

        if (peersTableView.getColumns() == null || peersTableView.getColumns().size() == 0) {
            TableColumn<PeerInfo, String> ipCol = new TableColumn<>("Address");
            ipCol.setCellValueFactory(new PropertyValueFactory<PeerInfo, String>("IpAddress"));
            peersTableView.getColumns().addAll(ipCol);
        }

        peersTableView.setEditable(false);
        peersTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        if(currentTorrent != null && currentTorrent.getPeers() != null && currentTorrent.getPeers().size() > 0)
            peersTableView.setItems(currentTorrent.getPeers());
    }

    public void initProgressBar(){
        progressBar.progressProperty().bind(currentTorrent.getProgressProperty());
        progressBar.prefWidthProperty().bind(parentVbox.widthProperty().subtract(20));
    }

    public void initTextBinding(){
        if(currentTorrent.getFileInfo() != null) {
            fileLocationText.setText(currentTorrent.getLocation());
            fileNameText.setText(currentTorrent.getFileInfo().getFileName());
            fileSizeText.setText(currentTorrent.getFileSizeString());
            createDateText.setText(currentTorrent.getCreateDateTime().toString());
        }
    }

    public void initChunkBar(){
        chunkHBox.getChildren().clear();
        chunkRects.clear();
        numberOfChunks = currentTorrent.getChunkStatuses().size();


        for(ChunkStatus c : currentTorrent.getChunkStatuses())
        {
           chunkRects.add(getRect(c));
        }

       // chunkRects = currentTorrent.getChunkRects();

        currentTorrent.setChunkRects(chunkRects);

       // if(chunkRects != null)
        chunkHBox.getChildren().addAll(chunkRects);
    }

    private Rectangle getRect(ChunkStatus status){
        int width = 20;
        int height = 10;
        Rectangle r = new Rectangle(width, height);
        r.setFill(getStatusPaint(status));
       // r.widthProperty().bind(chunkHBox.widthProperty().divide(numberOfChunks));
        //r.widthProperty().bind(chunkHBox.prefWidthProperty().subtract(20).divide(numberOfChunks));
        r.widthProperty().bind(rectWidth);

        return r;
    }

    public static class ChunkRectangle2 extends Rectangle{
        public ChunkRectangle2(double w, double h) {
            super(w, h);
        }

        @Override
        public boolean isResizable() {
            return true;
        }

        @Override
        public double minWidth(double height) {
            return 0.0;
        }

    }

    public static Paint getStatusPaint(ChunkStatus status){
        if(status == ChunkStatus.COMPLETE)
        {
           return Color.GREEN;
        }
        else if(status == ChunkStatus.EMPTY)
        {
            return Color.LIGHTGRAY;
        }
        else if(status == ChunkStatus.IN_PROGRESS)
        {
            return Color.LIMEGREEN;

        }

        return Color.RED;
    }

    public void updateCurrentTorrent(ClientTorrent torrent){
        //helps prevent memory leaks


        currentTorrent = torrent;
        initChunkBar();
        initProgressBar();
        initTextBinding();
        initPeerTable();
        peersTableView.setItems(currentTorrent.getPeers());
    }



    ListChangeListener chunkbarListener = new ListChangeListener<ChunkStatus>() {
        @Override
        public void onChanged(Change<? extends ChunkStatus> c) {
            //on change, update the corresponding rectangle color
            while (c.next()) {
                if(c.wasUpdated() || c.wasPermutated() || c.wasReplaced())
                {
                    System.out.println("chunks changed: from"  + c.getFrom() + " to: " + c.getTo());
                    for(int i = c.getFrom(); i < c.getTo(); i++)
                    {
                        final int index = i;
                        Platform.runLater(() -> {
                            updateChunkRect(index);

                        });
                    }
                }
            }
        }
    };

    private void updateChunkRect(int i){
        chunkRects.get(i).setFill(getStatusPaint(currentTorrent.getChunkStatuses().get(i)));
    }

}
