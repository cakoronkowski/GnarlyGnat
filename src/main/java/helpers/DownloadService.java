package helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import data.*;
import db.DbManager;
import javafx.application.Platform;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;
import services.FileManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by Stephen on 12/5/16.
 */
public class DownloadService {

    private static AsyncHttpClient httpClient = new DefaultAsyncHttpClient();
    private static DbManager dbManager;

    public static ClientTorrent startDownload(File torrentFile){
        dbManager = DbManager.getInstance();
        String id = "";
        String fileName ="";
        String fileSize ="";
        String chunkSize ="";
        String trackerString ="";
        String hashListString="";
        RepoTorrentFile torrent;
        try {

            BufferedReader reader = new BufferedReader(new FileReader(torrentFile));
            id = reader.readLine();
            fileName = reader.readLine();
            fileSize = reader.readLine();
            chunkSize = reader.readLine();
            trackerString = reader.readLine();
            hashListString=reader.readLine();
            Long filesize = Long.parseLong(fileSize);
            int chunkS = Integer.parseInt(chunkSize);
            int numChunks = (int) (Math.ceil((float) filesize / (float) chunkS));
            FileInfo fileInfo = new FileInfo(fileName, filesize,numChunks , chunkS, getDownloadDirectory());
            //RepoTorrentFile(FileInfo info, String id, List<String> trackers, String[] hashList){

            List<String> trackers = Arrays.asList(trackerString.split(","));
            String[] hashes = hashListString.split(",");
            torrent = new RepoTorrentFile(fileInfo, id, trackers, hashes);
            return startDownload(torrent);
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }

        return null;
    }

    public static ClientTorrent startDownload(RepoTorrentFile torrentFile)
    {
        List<ClientTorrent> torrents =  startDownload(Arrays.asList(torrentFile));
        if(torrents == null || torrents.isEmpty())
        {
            return null;
        }

        return torrents.get(0);
    }

    public static List<ClientTorrent> startDownload(List<RepoTorrentFile> torrentFiles){
        dbManager = DbManager.getInstance();
        List<ClientTorrent> clientTorrents = new ArrayList<>();
        if(torrentFiles != null && !torrentFiles.isEmpty())
        {
            for(RepoTorrentFile torrentFile : torrentFiles)
            {
                TorrentCache.lookup = 0;

                String downloadDirectory = dbManager.getDefaultDownloadDirectory();
                try {

                if(downloadDirectory == null || downloadDirectory.isEmpty())
                    downloadDirectory = DownloadService.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath() + "downloads/";
                } catch (URISyntaxException ex)
                {
                    ex.printStackTrace();
                }

                //create download directry if does not exist
                File dDirectory = new File(downloadDirectory);
                dDirectory.mkdir();



                File torrentDownloadFile = new File(downloadDirectory + torrentFile.getFileName());
                try {
                    if(torrentDownloadFile.exists())
                        torrentDownloadFile.delete();

                    torrentDownloadFile.createNewFile();
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }

                try {


                    Long filesize = torrentFile.getFileSize();
                    int chunkS = torrentFile.getChunkSize();
                    int numChunks = (int) (Math.ceil((float) filesize / (float) chunkS));
                    FileInfo fileInfo = new FileInfo(torrentFile.getFileName(), filesize, numChunks, chunkS, torrentDownloadFile.getAbsolutePath());
                    FileManager fileManager = new FileManager(filesize, chunkS, "rw", torrentDownloadFile.getAbsolutePath(), null, false);
                    List<String> trackers = torrentFile.getTrackers();
                    String trackerAddress = trackers.get(0);
                    String[] hashes = torrentFile.getHashList();
                    long lookup = 0;
                    Future<List<TrackerPeer>> promise =httpClient.prepareGet("http://" + trackerAddress + "/api/torrents/" + torrentFile.getTorrentId() + "/peers?port=" + dbManager.getCurrentSeedingPort()).execute(new AsyncCompletionHandler<List<TrackerPeer>>(){
                        @Override
                        public List<TrackerPeer> onCompleted(Response response) throws Exception{

                            if(response.getStatusCode() == 200)
                            {


                                ObjectMapper mapper = new ObjectMapper().registerModule(new ParameterNamesModule())
                                        .registerModule(new Jdk8Module())
                                        .registerModule(new JavaTimeModule());

                                TorrentCache.lookup = Long.parseLong(response.getHeader("lookup_millis"));

                                List<TrackerPeer> payload = Arrays.asList(mapper.readValue(response.getResponseBody(),  TrackerPeer[].class));
                                if (payload != null)
                                {
                                    if(torrentFile != null)
                                    {

                                    }

                                    System.out.println("NUMBER OF PEERS: " + payload.size());
                                }
                                else
                                    System.out.println("NUMBER OF PEERS: NONE");

                                return payload;
                            }

                            if(response.getStatusCode() == 500)
                                throw new IllegalStateException("Tracker threw an exception status: 500");

                            return null;
                        }

                        @Override
                        public void onThrowable(Throwable t){
                            System.err.println("getting peers from tracker failed: ");
                        }
                    });

                    List<TrackerPeer> trackerPeers = promise.get();




                    List<PeerInfo> peers = new ArrayList<>();
                    FileStatus status;
                    try {
                        for (TrackerPeer tp : trackerPeers) {
                            peers.add(new PeerInfo(tp.getIpAddress(), tp.getPort(), PeerStatus.Seeder));
                        }


                        status = FileStatus.READY;
                    }catch(NullPointerException e)
                    {
                        status = FileStatus.STALLED;
                        System.err.println("no peers on file :(");
                    }

                    if (trackerPeers == null || trackerPeers.size() == 0 || peers.size() == 0)
                        status = FileStatus.STALLED;

                    //create clienttorrent
                    ClientTorrent clientTorrent = new ClientTorrent(fileInfo, fileManager, status, trackers, peers, false, torrentFile.getTorrentId(), LocalDateTime.now(), hashes);
                    clientTorrent.Audit.NumberOfPeers = peers.size();
                    clientTorrent.Audit.LookupTimeForPeers = TorrentCache.lookup;
                    //add to db
                    dbManager.InsertClientTorrent(clientTorrent);
                    //add to cache

                    Platform.runLater(() -> {

                        TorrentCache.addTorrent(clientTorrent);
                        //add to incomplete to start download
                        TorrentCache.addIncomplete(clientTorrent);
                        System.out.println("Adding new download to everything.");
                    });



                    clientTorrents.add( clientTorrent);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.out.println("An Error Occurred starting the torrent download: " + ex.getStackTrace());
                }
            }

        }

        return clientTorrents;
    }


    public static String getDownloadDirectory(){
        String downloadDirectory = dbManager.getDefaultDownloadDirectory();
        try {

            if(downloadDirectory == null || downloadDirectory.isEmpty())
                downloadDirectory = DownloadService.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath() + "downloads/";
        } catch (URISyntaxException ex)
        {
            ex.printStackTrace();
        }

        //create download directry if does not exist
        File dDirectory = new File(downloadDirectory);
        dDirectory.mkdir();

        return downloadDirectory;
    }



}
