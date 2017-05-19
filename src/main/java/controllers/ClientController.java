package controllers;

import data.ClientTorrent;
import data.ClientViewModel;
import data.FileStatus;
import db.DbManager;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import workers.ClientWorkerTask;
import workers.NewDownloadTask;
import workers.RegisterTorrentTask;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Created by Stephen on 11/19/16.
 * ClientController is the controller for the ClientView.
 * After init() is called, the client program is started in a separate thread.
 */
public class ClientController implements Initializable{


    public ClientController() {
    }

    @FXML
    private BorderPane parentBorderPane;

    //@FXML
   // private FlowPane centerFlowPane;

    private SplitPane centerSplitPane;

    //Navigation
    @FXML
    private TreeView navTreeView;

    @FXML
    private ToolBar topToolBar;

    //Home Controls
    private FlowPane homeFlowPane;

    //Torrent Controls
    @FXML
    private Button addNewTorrentBtn;

    @FXML
    private Button downloadTorrentBtn;

    @FXML
    private Button changeDefaultTracker;

    @FXML
    private Button changeDefaultRepo;

    @FXML
    private Button changeDefaultDown;

    @FXML
    public MenuItem downTorrentMenuItem;
    @FXML
    public MenuItem registerTorrentMenuItem;
    @FXML
    public MenuItem preferencesMenuItem;

    TableView<ClientTorrent> torrentTableView;
    ProgressBar downloadingPb;
    List<Rectangle> chunks;
    TableView<String> peersTableView;

    TabPane torrentDetailView;
    ClientTorrentDetailController torrentDetailController;
    FlowPane homeView;
    HomeController homeController;
    DbManager dbManager;




//    TableView<data.RepoTorrentFile> table  = new TableView();
//    ObservableList<data.RepoTorrentFile> torrentFileList;
//    data.RepoTorrentFile currentTorrentFile = new data.RepoTorrentFile();
//    Text fileNameText = new Text("No Torrent Selected");

    protected ClientViewModel viewModel;

    private final String DOWNLOADING_NAVLABEL = "Downloading";
    private final String SEEDING_NAVLABEL = "Seeding";
    private final String COMPLETED_NAVLABEL = "Completed";
    private final String HOME_NAVLABEL = "Home";
    private final String TORRENTS_NAVLABEL = "Torrents";



    private void updateDetailView(ClientTorrent newVal){
        torrentDetailController.updateCurrentTorrent(newVal);

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {

            assert navTreeView != null : "fx:id=\"navTreeView\" was not injected: check your FXML file 'client.fxml'.";
            init();
        }
        catch (Exception ex)
        {
            System.err.println("CLIENT CONTROLLER INIT");
            ex.printStackTrace();
        }
    }


    /**
     * inits the client view
     */
    private void init(){
        dbManager = DbManager.getInstance();
        System.out.println("CLIENTCONTROLLER:" + getClass().getResource("").getPath());
        //torrentDetailView = new FlowPane();
        viewModel = new ClientViewModel();

        //init components
        initNavTree();
        initNewTorrentBtn();
        initTorrentTableView();
        initTorrentDetailView();
        initPeerTableView();
        initTorrentSplitPane();
        //initTrackerButton();
        //initRepoButton();
        initDownloadTorrentBtn();
        initPreferences();
       // initDownloadDirectoryBtn();





        //centerFlowPane.setStyle(" -fx-border-color: #2e8b57;    -fx-border-width: 2px;");
        //finally navigate to Home
        changeToNavItem(HOME_NAVLABEL);


        ClientWorkerTask worker = new ClientWorkerTask(viewModel);
        Thread clientThread = new Thread(worker);
        clientThread.setDaemon(true);
        clientThread.start();

    }

    private void initTorrentSplitPane(){
        centerSplitPane = new SplitPane();
        centerSplitPane.getItems().addAll(torrentTableView, torrentDetailView);
        centerSplitPane.setDividerPositions(0.7f);
        centerSplitPane.setOrientation(Orientation.VERTICAL);
        centerSplitPane.minHeight(0);
        centerSplitPane.minWidth(0);
       // SplitPane.setResizableWithParent(centerFlowPane, true);
       // centerSplitPane.setStyle(" -fx-border-color: #4286f4;    -fx-border-width: 2px;");
    }

    private void initTorrentDetailView(){

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/views/torrent_detail_view.fxml"
                    )
            );

            //TabPane detailPane = FXMLLoader.load(getClass().getResource("controllers/torren_detail_view.fxml"));
            torrentDetailView = (TabPane) loader.load();
            torrentDetailController = (ClientTorrentDetailController) loader.getController();
        } catch (IOException e) {

            e.printStackTrace();
        }
    }


  private void initTorrentTableView(){
    torrentTableView = new TableView<>();

      TableColumn<ClientTorrent, String> fileNameColumn = new TableColumn<>("File Name");
      fileNameColumn.setCellValueFactory(new PropertyValueFactory<ClientTorrent, String>("FileName"));


      TableColumn<ClientTorrent, String> fileSizeColumn = new TableColumn<>("Size");
      fileSizeColumn.setCellValueFactory(new PropertyValueFactory<ClientTorrent, String>("FileSizeString"));

      TableColumn<ClientTorrent, String> fileStatusColumn = new TableColumn<>("Status");
      fileStatusColumn.setCellValueFactory(celldata -> celldata.getValue().getStatusString());

      TableColumn<ClientTorrent, String> filePeersColumn = new TableColumn<>("Peers");
      filePeersColumn.setCellValueFactory(celldata -> celldata.getValue().getPeerString());

      torrentTableView.getColumns().addAll(fileNameColumn, fileSizeColumn, fileStatusColumn, filePeersColumn);
      torrentTableView.setEditable(false);
      torrentTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

      torrentTableView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<ClientTorrent>() {
          @Override
          public void changed(ObservableValue<? extends ClientTorrent> observable, ClientTorrent oldValue, ClientTorrent newValue) {
                updateDetailView(newValue != null ? newValue : new ClientTorrent());
          }
      });



  }

  private void initPeerTableView(){
      peersTableView = new TableView<>();
  }



    /**
     * used to init the addNewTorrentBtn should get file info and pass it onto the worker thread to start seeding file.
     *
     */
    private void initNewTorrentBtn(){
        registerTorrentMenuItem.setOnAction(
                        e -> {

                            FileChooser fileChooser = new FileChooser();
                            fileChooser.setTitle("Select file to register");
                            File file =fileChooser.showOpenDialog(navTreeView.getScene().getWindow());
                            if(file != null) {
                                System.out.println("Starting Registration");
                                ClientTorrent registeredTorrent = new ClientTorrent();
                                RegisterTorrentTask registerTorrentTask = new RegisterTorrentTask(file, viewModel);
                                registerTorrentTask.setOnSucceeded(c -> {
                                    ClientTorrent t = registerTorrentTask.getValue();

                                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                    alert.setTitle("Register Torrent: " + file.getName());
                                    alert.setHeaderText(null);
                                    alert.setContentText("Torrent was successfully registered, happy seeding!");
                                    alert.showAndWait();
                                });

                                registerTorrentTask.setOnFailed(f -> {
                                    Alert alert = new Alert(Alert.AlertType.ERROR);
                                    alert.setTitle("Register Torrent");
                                    alert.setHeaderText("An Error Occurred Registering: " + file.getName());
                                    alert.setContentText("Uh oh, something went wrong registering the torrent, please try again.\n "+registerTorrentTask.getException().toString());
                                    alert.showAndWait();
                                });

                                Thread regThread = new Thread(registerTorrentTask);
                                regThread.start();


                            }
                        });

    }

    private void initPreferences(){
        preferencesMenuItem.setOnAction(event -> {

            GridPane prefs = new GridPane();
            PreferencesController preferencesController;
            try {

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource(
                                "/views/preferences.fxml"
                        )
                );

                //TabPane detailPane = FXMLLoader.load(getClass().getResource("controllers/torren_detail_view.fxml"));
                prefs = (GridPane) loader.load();
                preferencesController = (PreferencesController) loader.getController();
            } catch (IOException e) {

                e.printStackTrace();
            }

            Stage stage = new Stage();


            stage.setScene(new Scene(prefs));
            stage.setTitle("Preferences");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(navTreeView.getScene().getWindow() );
            stage.show();

        });
    }

    private void initDownloadTorrentBtn()
    {

        downTorrentMenuItem.setOnAction( event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("GnarlyGnat Files (*.gg)", "*.gg"));
            fileChooser.setTitle("Select Torrent File");
            File file =fileChooser.showOpenDialog(navTreeView.getScene().getWindow());
            if(file != null) {
                final NewDownloadTask downloadTask = new NewDownloadTask(file);

                downloadTask.setOnRunning(c -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Downloading Torrent: " + file.getName());
                    alert.setHeaderText(null);
                    alert.setContentText("Torrent is currently downloading");
                    alert.showAndWait();
                });

                downloadTask.setOnSucceeded( c -> {
                    ClientTorrent t = downloadTask.getValue();
                    if(t != null) {
                        viewModel.getTorrents().add(t);
                        //navigate to torrent to display progress to user.
                        navigateToTorrentDetail(t);
                    }
                    else {

                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("ERROR: Downloading Torrent: " + file.getName());
                        alert.setHeaderText(null);
                        //greatest help text ever
                        alert.setContentText("Something Happened After Success: " + downloadTask.getException().getMessage());
                        alert.showAndWait();
                    }
                });

               downloadTask.setOnFailed( e -> {
                   Alert alert = new Alert(Alert.AlertType.ERROR);
                   alert.setTitle("ERROR: Downloading Torrent: " + file.getName());
                   alert.setHeaderText(null);
                   //greatest help text ever
                   alert.setContentText("Something Happened and Failed: " + downloadTask.getException().getMessage());
                   alert.showAndWait();
               });

                Thread downloadThread = new Thread(downloadTask);
                downloadThread.start();
            }
        });

    }


    private void initNavTree(){
        //init nav
        TreeItem<String> root = new TreeItem<String>("Root Node");
        root.setExpanded(true);
        root.getChildren().addAll(
                new TreeItem<String>("Home")
        );


        ImageView downImgView =  new ImageView(new Image(getClass().getResourceAsStream("/imgs/download.png")));
        downImgView.setFitWidth(40);
        downImgView.setPreserveRatio(true);
        downImgView.setSmooth(true);


        ImageView upImgView =  new ImageView(new Image(getClass().getResourceAsStream("/imgs/upload.png")));

        upImgView.setFitWidth(40);
        upImgView.setPreserveRatio(true);
        upImgView.setSmooth(true);

        ImageView okImgView =  new ImageView(new Image(getClass().getResourceAsStream("/imgs/ok.png")));

        okImgView.setFitWidth(40);
        okImgView.setPreserveRatio(true);
        okImgView.setSmooth(true);


        TreeItem<String> torrents = new TreeItem<String>("Torrents");
        torrents.getChildren().add(new TreeItem<String>(DOWNLOADING_NAVLABEL, downImgView));
        torrents.getChildren().add(new TreeItem<String>(SEEDING_NAVLABEL, upImgView));
        torrents.getChildren().add(new TreeItem<String>(COMPLETED_NAVLABEL,okImgView));
        torrents.setExpanded(true);
        root.getChildren().add(torrents);
        TreeView<String> treeView = new TreeView<String>(root);
        treeView.setShowRoot(false);

        root.setExpanded(true);
        navTreeView.setRoot(root);
        navTreeView.setShowRoot(false);

        navTreeView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                TreeItem<String> selectedItem = (TreeItem<String>) newValue;
                if(oldValue != null && !oldValue.equals(newValue))
                    changeToNavItem(selectedItem.getValue());
            }
        });
    }


    /**
     * Updates the Center view according to what nav item is selected.
     * Only changes the view if the current nav item != selectedNavItem.
     *
     * @param selectedNavItem the text that corresponds to the TreeItem selected in the TreeView
     */
    private void changeToNavItem(String selectedNavItem){

        System.out.println(selectedNavItem);

//        if(parentBorderPane.getCenter() != null) {
//            parentBorderPane.s;
//        }

        switch (selectedNavItem)
        {
            case DOWNLOADING_NAVLABEL:
                navigateToDownloading();

               // viewModel.setCurrentTorrent(torrentTableView.getItems().get(0));
                break;
            case SEEDING_NAVLABEL:
                navigateToSeeding();
                break;
            case COMPLETED_NAVLABEL:
                navigateToCompleted();
                break;
            case HOME_NAVLABEL:
                navigateToHome();
                break;
            case TORRENTS_NAVLABEL:
                navigateToTorrents();
                break;
        }

    }

    private void navigateToHome(){

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/views/home.fxml"
                    )
            );

            //TabPane detailPane = FXMLLoader.load(getClass().getResource("controllers/torren_detail_view.fxml"));
            homeView = (FlowPane) loader.load();
            homeController = (HomeController) loader.getController();
        } catch (IOException e) {

            e.printStackTrace();
        }


        parentBorderPane.setCenter(homeView);
//
//        if(homeFlowPane == null) {
//            homeFlowPane = new FlowPane();
//
//
//            ObservableList<PieChart.Data> pieChartData =
//                    FXCollections.observableArrayList(
//                            new PieChart.Data("Grapefruit", 13),
//                            new PieChart.Data("Oranges", 25),
//                            new PieChart.Data("Plums", 10),
//                            new PieChart.Data("Pears", 22),
//                            new PieChart.Data("Apples", 30));
//            final PieChart chart = new PieChart(pieChartData);
//            chart.setTitle("Imported Fruits");
//
//            Text homeWelcome = new Text("Welcome to GnarlyGnat!");
//
//            homeFlowPane.getChildren().addAll(homeWelcome, chart);
//        }
//       // centerFlowPane.getChildren().add(homeFlowPane);
//        parentBorderPane.setCenter(homeFlowPane);
    }

    private void navigateToCompleted(){
        filterTable(TORRENT_FILTER.COMPLETED);
        Text text = new Text("Completed");
       // centerFlowPane.getChildren().add(text);
        parentBorderPane.setCenter(centerSplitPane);
    }

    private void navigateToDownloading(){
        filterTable(TORRENT_FILTER.DOWNLOADING);
       // centerFlowPane.getChildren().add(centerSplitPane);
        parentBorderPane.setCenter(centerSplitPane);

    }

    private void navigateToSeeding(){
        filterTable(TORRENT_FILTER.SEEDING);
      //  centerFlowPane.getChildren().add(centerSplitPane);
        parentBorderPane.setCenter(centerSplitPane);
    }

    private void navigateToTorrents(){
        filterTable(TORRENT_FILTER.ALL);
        //centerFlowPane.getChildren().add(centerSplitPane);
        parentBorderPane.setCenter(centerSplitPane);
    }

    private void navigateToTorrentDetail(ClientTorrent torrent)
    {
        String navItem = "";
        if(torrent.getStatus() == FileStatus.COMPLETED)
            navItem = COMPLETED_NAVLABEL;
        else
            navItem = DOWNLOADING_NAVLABEL;
        changeToNavItem(navItem);
        int index = 0;
        for(ClientTorrent t : torrentTableView.getItems())
        {
            if(t.getTorrentId().equals(torrent.getTorrentId()))
            {
                torrentTableView.getSelectionModel().select(index);
                torrentDetailController.updateCurrentTorrent(t);
                return;
            }
            index++;
        }

    }

    private void filterTable(TORRENT_FILTER filter)
    {
        FilteredList<ClientTorrent> filteredList = new FilteredList<ClientTorrent>(viewModel.getTorrents(), t -> true);

        filteredList.setPredicate(
                torrent -> {
                    switch (filter)
                    {
                        case ALL:
                            return true;
                        case COMPLETED:
                            return torrent.getStatus() == FileStatus.COMPLETED;
                        case SEEDING:
                            return torrent.getStatus() != FileStatus.READY;
                        case DOWNLOADING:
                           return  torrent.getStatus() == FileStatus.DOWNLOADING || torrent.getStatus() == FileStatus.READY;
                    }

                    return false;
                }

        );

        SortedList<ClientTorrent> sortedList = new SortedList<ClientTorrent>(filteredList);
        sortedList.comparatorProperty().bind(torrentTableView.comparatorProperty());

        torrentTableView.setItems(sortedList);
    }


    protected enum TORRENT_FILTER{
        ALL,
        DOWNLOADING,
        COMPLETED,
        SEEDING
    }

}
