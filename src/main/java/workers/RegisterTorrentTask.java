package workers;

import data.*;
import db.DbManager;
import helpers.CryptoHelper;
import helpers.JsonConverter;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import org.asynchttpclient.*;
import org.asynchttpclient.Response;
import services.FileAnalyzer;
import services.FileManager;
import services.LeecherService;
import spark.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by cakor on 11/27/2016.
 */
public class RegisterTorrentTask extends Task<ClientTorrent> {

    public RegisterTorrentTask(File f, ClientViewModel vm){

        file = f;
        viewModel = vm;
    }

    private DbManager dbManager;

    File file;
    private AsyncHttpClient httpClient = new DefaultAsyncHttpClient();
    ClientTorrent torrent = new ClientTorrent();
    FileInfo fileInfo;
    FileManager fileManager;
    String defTrackerIp;
    ClientViewModel viewModel;

    @Override
    protected ClientTorrent call() throws Exception {
        dbManager = DbManager.getInstance();
        defTrackerIp = dbManager.getDefaultTracker();
        try {


            //validate file info
            FileInfo f = FileAnalyzer.getInfo(file);
            System.out.println(f.toString());

            if(f == null)
                failed();
            fileInfo = f;
            fileManager = new FileManager(fileInfo.getLength(), fileInfo.getChunkSize(), "r", fileInfo.getAbsolutePath(), null, true);
            registerTorrentWithTracker();


            //register with Repo


            //add file to cache
            //add to DB


            //if anything fails run failed()



        }
        catch (Exception ex)
        {
            System.err.println(ex);
            failed();

        }

        return null;
    }

    private void registerTorrentWithTracker() {
        //register with Tracker
        String url = "http://" + defTrackerIp + "/api/torrents/" + torrent.getTorrentId() + "/register";
        System.out.println("Registering with Tracker");
        System.out.println("calling:" + url);
         httpClient.preparePost(url)
                .execute(new AsyncCompletionHandler<Response>() {
                    @Override
                    public Response onCompleted(Response response) throws Exception {
                        System.out.println("tracker registration complete, calling repo");
                        registerTorrentWithRepo();
                        return response;
                    }

                    @Override
                    public void onThrowable(Throwable t) {
                        System.err.println("tracker failed");
                        failWithText(t.getLocalizedMessage());
                    }
                });

    }



    private void registerTorrentLocally(){
        try{

//             public ClientTorrent(FileInfo fileInfo, FileManager fileManager,
//                    FileStatus status, List<String> trackers,
//                    List<PeerInfo> peers, boolean is_complete,
//            String torrentId, LocalDateTime createDate
//                         )
            System.out.println("registering torrent locally");
           // FileManager fileManager =

            torrent = new ClientTorrent(
                    fileInfo,
                    fileManager,
                    FileStatus.COMPLETED,
                    Arrays.asList(defTrackerIp),
                    new ArrayList<PeerInfo>(),
                    true,
                    torrent.getTorrentId(),
                    LocalDateTime.now());
            dbManager.InsertClientTorrent(torrent);
            TorrentCache.addTorrent(torrent);
            viewModel.getTorrents().add(torrent);

            sendKeepAlive();


            //succeeded();
        }
        catch (Exception ex)
        {
           ex.printStackTrace();
        }

    }

    private void registerTorrentWithRepo(){
        System.out.println("Hashing");
        String[] validHashes =new String[fileInfo.getNumberOfChunks()];
        for(int i=0;i<fileInfo.getNumberOfChunks(); i++)
        {
            byte[] b = new byte[fileInfo.getChunkSize()];
            try {
                fileManager.getChunk(b,i);
            }
            catch (Exception e)
            {
                System.out.println("Hashing exception");
                System.err.println(e);
            }
            validHashes[i]= CryptoHelper.generate(b);
        }
        System.out.println("Hashing complete");
        NewTorrentFilePayload f = new NewTorrentFilePayload(fileInfo, defTrackerIp, torrent.getTorrentId(),validHashes) ;
        System.out.println("registering torrent with repo: "+ "http://" + dbManager.getDefaultRepo() + "/api/torrents/register");

        String data = JsonConverter.dataToJson(f);
        //register with Tracker
        httpClient.preparePost("http://" + dbManager.getDefaultRepo() + "/api/torrents/register")
                .setHeader("Content-Type", "application/json")
                .setHeader("Content-Length", data.length() + "")
                .setBody(data)
                .execute(new AsyncCompletionHandler<Response>(){

                    @Override
                    public Response onCompleted(Response response) throws Exception{
                        registerTorrentLocally();
                        return response;
                    }

                    @Override
                    public void onThrowable(Throwable t){

                        System.err.println("repo failed: " + t.getStackTrace());
                        failWithText(t.getLocalizedMessage());
                    }
                });

    }

    private void sendKeepAlive(){
        System.out.println("Sending Keep Alive");
        httpClient
                .prepareGet("http://" + dbManager.getDefaultTracker() + "/api/torrents/"+ torrent.getTorrentId()+"/keepAlive?port=" + dbManager.getCurrentSeedingPort())
                .execute(new AsyncCompletionHandler<Response>(){
                    @Override
                    public Response onCompleted(Response response) throws Exception{
                       System.out.println("keep alive success");
                        return response;
                    }

                    @Override
                    public void onThrowable(Throwable t){

                        System.err.println("keepalive failed: " + t.getStackTrace());
                       failWithText(t.getLocalizedMessage());
                    }
                });
    }

    private void failWithText(String failText){
//        setOnFailed(event -> {
//            Alert alert = new Alert(Alert.AlertType.ERROR);
//            alert.setTitle("ERROR: Registering Torrent: " + file.getName());
//            alert.setHeaderText(null);
//            alert.setContentText(failText);
//            alert.showAndWait();
//        });
        throw new IllegalStateException(failText);
        ///failed();
    }
}
