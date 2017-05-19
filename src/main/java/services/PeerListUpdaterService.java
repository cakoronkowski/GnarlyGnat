package services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import data.*;
import db.DbManager;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by cakor on 12/4/2016.
 */
public class PeerListUpdaterService {
    public static void start()
    {
        Thread t =new Thread(){
            @Override
            public void run()
            {
                System.out.println("Starting Peer List Updater");
                AsyncHttpClient httpClient = new DefaultAsyncHttpClient();
                DbManager db = DbManager.getInstance();
                while(true)
                {

                    for (String key : TorrentCache.getHashTorrents().keySet()) {

                        ClientTorrent torrent = TorrentCache.getTorrentById(key);
                        if(!torrent.getIsComplete()) {


                            httpClient.prepareGet("http://" + torrent.getTrackers().get(0) + "/api/torrents/" + torrent.getTorrentId() + "/peers?port=" + db.getCurrentSeedingPort()).execute(new AsyncCompletionHandler<Response>() {

                                @Override
                                public Response onCompleted(Response response) throws Exception {
                                    ObjectMapper mapper = new ObjectMapper().registerModule(new ParameterNamesModule())
                                            .registerModule(new Jdk8Module())
                                            .registerModule(new JavaTimeModule());
                                    List<TrackerPeer> payload = Arrays.asList(mapper.readValue(response.getResponseBody(), TrackerPeer[].class));
                                    List<PeerInfo> peers = new ArrayList<>();
                                    FileStatus status;
                                    try {
                                        for (TrackerPeer tp : payload) {
                                            peers.add(new PeerInfo(tp.getIpAddress(), tp.getPort(), PeerStatus.Seeder));
                                        }


                                        torrent.setStatus(FileStatus.DOWNLOADING);
                                    } catch (NullPointerException e) {
                                        //status = FileStatus.STALLED;
                                        System.err.println("no peers on file :(");
                                        torrent.setStatus(FileStatus.STALLED);
                                    }
                                    torrent.setPeers(peers);

                                    return response;
                                }

                                @Override
                                public void onThrowable(Throwable t) {


                                    System.err.println(t);
                                    // Something wrong happened.
                                }
                            });
                        }


                    }




                    try {
                        sleep(60000);
                    }catch (Exception e)
                    {
                        System.err.println(e);
                    }
                }

            }
        };
        t.start();
    }

}
