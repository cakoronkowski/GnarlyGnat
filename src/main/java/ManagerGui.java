/**
 * Created by Stephen on 11/9/16.
 */
import data.ClientConfig;
import data.RepoTorrentFile;
import db.DbManager;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;

import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import controllers.ClientController;
import workers.Repo;
import workers.TrackerWebService;

import java.io.Closeable;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ManagerGui extends BaseGui {

    public static void main(String[] args) {

        try {

        config = new ClientConfig(args);
        } catch (IllegalArgumentException ex)
        {
            ex.printStackTrace();
            System.exit(1);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(){
            public void run() {
                try {
                    if(DbManager.getInstance() != null)
                        DbManager.getInstance().close();
                } catch (IOException ex) {
                    System.err.println("Was unable to shutdown database");
                    ex.printStackTrace();
                }

            }
        });
        launch(args);
    }

    TableView<RepoTorrentFile> table  = new TableView();
    ObservableList<RepoTorrentFile> torrentFileList;
    RepoTorrentFile currentTorrentFile = new RepoTorrentFile();
    Text fileNameText = new Text("No Torrent Selected");
    static ClientConfig config;
    Stage mainStage;


    @Override
    public void start(Stage primaryStage) {
        if(config != null)
            DbManager.getInstance().SaveClientConfig(config);
        mainStage = primaryStage;
        mainStage.setHeight(400.0);
        mainStage.setWidth(800.0);
        mainStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
        mainStage.setTitle("GnarlyGnat");
        mainStage.getIcons().add(new Image(getClass().getResourceAsStream("/imgs/gnat_icon.png")));

        if((config == null || !config.isInBotMode()) && !config.hasType()) {
            System.out.println("starting main");
            try {

                //setup main stage
                FlowPane s = (FlowPane) LoadView("manager.fxml");
                Scene scene = new Scene(s);

                mainStage.setScene(scene);



                //Client Instance Button
                Button clientBtn = (Button) scene.lookup("#clientBtn");
                Image clientIcon = new Image(getClass().getResourceAsStream("/imgs/server-client.png"));
                clientBtn.setGraphic(new ImageView(clientIcon));

                clientBtn.setOnAction(event -> {
                    startClient();
                });


                //Tracker Instance Button
                Button trackerBtn = (Button) scene.lookup("#trackerBtn");
                Image trackerIcon = new Image(getClass().getResourceAsStream("/imgs/server.png"));
                trackerBtn.setGraphic(new ImageView(trackerIcon));
                trackerBtn.setOnAction(event -> {
                    startTracker();
                });


                //Repo Instance Button
                Button repositryBtn = (Button) scene.lookup("#repositoryBtn");
                Image repositryIcon = new Image(getClass().getResourceAsStream("/imgs/database.png"));
                repositryBtn.setGraphic(new ImageView(repositryIcon));

                repositryBtn.setOnAction(event -> {
                    startRepo();
                });

                mainStage.show();

            } catch (Exception ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
        else
        {
            switch (config.getType())
            {
                case PEER:
                    startClient();
                    break;
                case REPOSITORY:
                    startRepo();
                    break;
                case TRACKER:
                    startTracker();
                    break;
            }
        }
    }

    private void startClient(){
        mainStage.hide();

        BorderPane clientLoader = (BorderPane) LoadView("client.fxml");
        assert clientLoader != null : "client loader is null";
        Scene clientScene = new Scene(clientLoader);
        assert clientScene != null : "client scene is null";

        mainStage.setScene(clientScene);
        mainStage.setTitle("GnarlyGnat Client");
        mainStage.setHeight(600);
        mainStage.setWidth(1000);
        mainStage.show();

    }

    private void startTracker(){
        mainStage.hide();
        BorderPane tracker = (BorderPane) LoadView("tracker.fxml");
//        TrackerWebService trackerWebService = new TrackerWebService();
//        trackerWebService.start();
        Scene trackerScene = new Scene(tracker);
        mainStage.setScene(trackerScene);
        mainStage.setTitle("GnarlyGnat Tracker");
        mainStage.setHeight(600);
        mainStage.setWidth(1000);
        mainStage.show();
    }

    private void startRepo(){
        mainStage.hide();
        BorderPane repository = (BorderPane) LoadView("repository.fxml");
        Scene repositoryScene = new Scene(repository);
        //Repo repoWebServer = new Repo();
        //repoWebServer.start();
        mainStage.setScene(repositoryScene);
        mainStage.setTitle("GnarlyGnat Repository");
        mainStage.setHeight(600);
        mainStage.setWidth(1000);
        mainStage.show();
    }

}
