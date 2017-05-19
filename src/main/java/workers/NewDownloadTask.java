package workers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import data.*;
import db.DbManager;
import helpers.DownloadService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.scene.control.Alert;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;
import services.FileManager;
import services.LeecherService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by cakor on 11/27/2016.
 */
public class NewDownloadTask extends Task<ClientTorrent> {
    public NewDownloadTask(File f){

        GnarlyGnatFile = f;
    }
    private AsyncHttpClient httpClient = new DefaultAsyncHttpClient();
    private DbManager dbManager;
    private String trackerAddress;
    private String repoAddress;

    File GnarlyGnatFile;
    @Override
    protected ClientTorrent call() throws Exception {
        //parse file
        dbManager = DbManager.getInstance();
        trackerAddress = dbManager.getDefaultTracker();
        repoAddress = dbManager.getDefaultRepo();
        if(trackerAddress == null || repoAddress == null || trackerAddress.isEmpty() || repoAddress.isEmpty())
            throw new IllegalArgumentException("Tracker or Repo is not set.");

        return DownloadService.startDownload(GnarlyGnatFile);
//
//        String id = "";
//                String fileName ="";
//                String fileSize ="";
//                String chunkSize ="";
//                String trackerString ="";
//                String hashListString="";
//        try {
//
//        BufferedReader reader = new BufferedReader(new FileReader(GnarlyGnatFile));
//        id = reader.readLine();
//        fileName = reader.readLine();
//        fileSize = reader.readLine();
//        chunkSize = reader.readLine();
//        trackerString = reader.readLine();
//        hashListString=reader.readLine();
//        } catch (Exception ex)
//        {
//            ex.printStackTrace();
//            failWithText("Corrupt Torrent File: " + ex.getMessage() + ex.getStackTrace());
//        }
//
//
//        String downloadDirectory = dbManager.getDefaultDownloadDirectory();
//        if(downloadDirectory == null || downloadDirectory.isEmpty())
//            downloadDirectory = getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath() + "downloads/";
//
//        //create download directry if does not exist
//        File dDirectory = new File(downloadDirectory);
//        dDirectory.mkdir();
//
//
//
//        File torrentDownloadFile = new File(downloadDirectory + fileName);
//        try {
//            torrentDownloadFile.createNewFile();
//        }
//        catch (Exception ex) {
//            ex.printStackTrace();
//            failWithText("Unable to Create File");
//        }
//
//        try {
//
//
//            Long filesize = Long.parseLong(fileSize);
//            int chunkS = Integer.parseInt(chunkSize);
//            int numChunks = (int) (Math.ceil((float) filesize / (float) chunkS));
//            FileInfo fileInfo = new FileInfo(fileName, filesize, numChunks, chunkS, torrentDownloadFile.getAbsolutePath());
//            FileManager fileManager = new FileManager(filesize, chunkS, "rw", torrentDownloadFile.getAbsolutePath(), null, false);
//            List<String> trackers = Arrays.asList(trackerString.split(","));
//            String[] hashes = hashListString.split(",");
//            Future<List<TrackerPeer>> promise =httpClient.prepareGet("http://" + trackers.get(0) + "/api/torrents/" + id + "/peers?port=" + dbManager.getCurrentSeedingPort()).execute(new AsyncCompletionHandler<List<TrackerPeer>>(){
//                @Override
//                public List<TrackerPeer> onCompleted(Response response) throws Exception{
//
//                    if(response.getStatusCode() == 200)
//                    {
//
//
//                        ObjectMapper mapper = new ObjectMapper().registerModule(new ParameterNamesModule())
//                                .registerModule(new Jdk8Module())
//                                .registerModule(new JavaTimeModule());
//
//                        List<TrackerPeer> payload = Arrays.asList(mapper.readValue(response.getResponseBody(),  TrackerPeer[].class));
//                        if (payload != null)
//                            System.out.println("NUMBER OF PEERS: " + payload.size());
//                        else
//                            System.out.println("NUMBER OF PEERS: NONE");
//
//                        return payload;
//                    }
//
//                    if(response.getStatusCode() == 500)
//                        throw new IllegalStateException("Tracker threw an exception status: 500");
//
//                    return null;
//                }
//
//                @Override
//                public void onThrowable(Throwable t){
//                    System.err.println("tracker failed: " + t.toString());
//                    failWithText(t.toString());
//                }
//            });
//
//            List<TrackerPeer> trackerPeers = promise.get();
//
//
//
//
//            List<PeerInfo> peers = new ArrayList<>();
//            FileStatus status;
//            try {
//                for (TrackerPeer tp : trackerPeers) {
//                    peers.add(new PeerInfo(tp.getIpAddress(), tp.getPort(), PeerStatus.Seeder));
//                }
//
//
//            status = FileStatus.READY;
//            }catch(NullPointerException e)
//            {
//                status = FileStatus.STALLED;
//                System.err.println("no peers on file :(");
//            }
//
//            if (trackerPeers == null || trackerPeers.size() == 0 || peers.size() == 0)
//                status = FileStatus.STALLED;
//
//            //create clienttorrent
//            ClientTorrent clientTorrent = new ClientTorrent(fileInfo, fileManager, status, trackers, peers, false, id, LocalDateTime.now(), hashes);
//
//            //add to db
//            dbManager.InsertClientTorrent(clientTorrent);
//            //add to cache
//            TorrentCache.addTorrent(clientTorrent);
//            //add to incomplete to start download
//            TorrentCache.addIncomplete(clientTorrent);
//
////            Thread downThread = new Thread(() -> {
////                if(!LeecherService.isLeeching())
////                    LeecherService.startLeeching();
////            });
////            downThread.setDaemon(true);
////
////            downThread.start();
//
//            return clientTorrent;
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            failWithText("An Error Occurred downloading torrent: " + ex.getStackTrace());
//        }
//        return null;
    }


    private void failWithText(String failText){
        setOnFailed(event -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("ERROR: Downloading Torrent: " + GnarlyGnatFile.getName());
            alert.setHeaderText(null);
            alert.setContentText(failText);
            alert.showAndWait();
        });
        throw new IllegalStateException(failText);
        ///failed();
    }
}
