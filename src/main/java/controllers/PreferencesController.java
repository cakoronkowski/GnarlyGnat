package controllers;

import com.sun.javafx.geom.AreaOp;
import db.DbManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.asynchttpclient.*;

import javax.swing.*;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by Stephen on 11/30/16.
 */
public class PreferencesController implements Initializable {

    @FXML
    public TextField downloadDirectoryField;
    @FXML
    public TextField defTrackerField;
    @FXML
    public TextField defRepoField;
    @FXML
    public Button cancelBtn;
    @FXML
    public Button okBtn;

    @FXML
    public Button repoTestBtn;

    @FXML
    public Button trackerTestBtn;

    private DbManager dbManager;
    private ImageView unlockedIconR;
    private ImageView lockedIconR;
    private ImageView alertIconR;
    private ImageView unlockedIconT;
    private ImageView lockedIconT;
    private ImageView alertIconT;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        init();
    }


    private void init(){
        dbManager = DbManager.getInstance();
        initCancelBtn();
        initOkBtn();
        initFields();
        initTestButtons();


    }

    private void initCancelBtn(){
        cancelBtn.setOnAction(event -> {
            closeModal();
        });
    }

    private void initFields(){
        downloadDirectoryField.setText(dbManager.getDefaultDownloadDirectory());
        defRepoField.setText(dbManager.getDefaultRepo());
        defTrackerField.setText(dbManager.getDefaultTracker());
    }

    private void initOkBtn(){
        okBtn.setOnAction(event -> {
            savePreferences();
        });

    }

    private void initTestButtons(){
        unlockedIconR =  new ImageView(new Image(getClass().getResourceAsStream("/imgs/unlocked.png")));
        lockedIconR=  new ImageView(new Image(getClass().getResourceAsStream("/imgs/locked.png")));
        alertIconR =  new ImageView(new Image(getClass().getResourceAsStream("/imgs/denied.png")));
        unlockedIconT =  new ImageView(new Image(getClass().getResourceAsStream("/imgs/unlocked.png")));
        lockedIconT =  new ImageView(new Image(getClass().getResourceAsStream("/imgs/locked.png")));
        alertIconT =  new ImageView(new Image(getClass().getResourceAsStream("/imgs/denied.png")));

        int width = 20;

        unlockedIconR.setFitWidth(width);
        unlockedIconR.setPreserveRatio(true);
        unlockedIconR.setSmooth(true);
        unlockedIconT.setFitWidth(width);
        unlockedIconT.setPreserveRatio(true);
        unlockedIconT.setSmooth(true);

        lockedIconT.setFitWidth(width);
        lockedIconT.setPreserveRatio(true);
        lockedIconT.setSmooth(true);
        lockedIconR.setFitWidth(width);
        lockedIconR.setPreserveRatio(true);
        lockedIconR.setSmooth(true);

        alertIconT.setFitWidth(width);
        alertIconT.setPreserveRatio(true);
        alertIconT.setSmooth(true);
        alertIconR.setFitWidth(width);
        alertIconR.setPreserveRatio(true);
        alertIconR.setSmooth(true);

        trackerTestBtn.setGraphic(unlockedIconT);
        repoTestBtn.setGraphic(unlockedIconR);

        trackerTestBtn.setOnAction(event -> {
            testTracker();
        });

        repoTestBtn.setOnAction(event -> {
            testRepo();
        });
    }

    private void closeModal(){
        okBtn.getScene().getWindow().hide();
    }

    private void savePreferences(){
        dbManager.updateDefaultDownloadDirectory(downloadDirectoryField.getText());
        dbManager.updateDefaultRepo(defRepoField.getText());
        dbManager.updateDefaultTracker(defTrackerField.getText());
        closeModal();

    }

    private void testRepo() {
        Platform.runLater(() -> {
            repoTestBtn.setGraphic(unlockedIconR);
            if (testUrl(defRepoField.getText())) {
                repoTestBtn.setGraphic(lockedIconR);
                dbManager.updateDefaultRepo(defRepoField.getText());
            } else {
                repoTestBtn.setGraphic(alertIconR);
            }
        });
    }

    private void testTracker() {
        Platform.runLater(() -> {
            trackerTestBtn.setGraphic(unlockedIconT);
            if (testUrl(defTrackerField.getText())) {
                trackerTestBtn.setGraphic(lockedIconT);
                dbManager.updateDefaultTracker(defTrackerField.getText());
            } else {
                trackerTestBtn.setGraphic(alertIconT);
            }
        });

    }

    private boolean testUrl(String address) {
        boolean success;
        AsyncHttpClient c = new DefaultAsyncHttpClient();
        try {


            Future<Boolean> f = c.prepareGet("http://" + address + "/api/ping").execute(
                    new AsyncCompletionHandler<Boolean>() {

                        @Override
                        public Boolean onCompleted(Response response) throws Exception {
                            // Do something with the Response
                            return response.getStatusCode() == 200;
                        }

                        @Override
                        public void onThrowable(Throwable t) {
                            // Something wrong happened.
                        }
                    });


            success = f.get();
            System.out.println("suc" + success);
        } catch (InterruptedException ex) {
            success = false;

        } catch (ExecutionException ec) {
            success = false;
        } catch (NumberFormatException nex) {
            success = false;
        }

        return success;
    }
}
