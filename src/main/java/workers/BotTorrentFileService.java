package workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import data.RepoTorrentFile;
import data.TrackerPeer;
import db.DbManager;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by Stephen on 12/5/16.
 */
public class BotTorrentFileService {

    private static AsyncHttpClient httpClient = new DefaultAsyncHttpClient();
    private static DbManager dbManager;

    public static List<RepoTorrentFile> getMostRecentRepoTorrentFiles(){
        dbManager = DbManager.getInstance();
        long currentSync = System.currentTimeMillis();
        long lastSync = dbManager.getLastSyncEpoch();

        Future<List<RepoTorrentFile>> promise =httpClient.prepareGet("http://" + dbManager.getDefaultRepo() + "/api/botSync/" + lastSync).execute(new AsyncCompletionHandler<List<RepoTorrentFile>>(){
            @Override
            public List<RepoTorrentFile> onCompleted(Response response) throws Exception{

                if(response.getStatusCode() == 200)
                {


                    ObjectMapper mapper = new ObjectMapper().registerModule(new ParameterNamesModule())
                            .registerModule(new Jdk8Module())
                            .registerModule(new JavaTimeModule());

                    List<RepoTorrentFile> payload = Arrays.asList(mapper.readValue(response.getResponseBody(),  RepoTorrentFile[].class));
                    if (payload != null)
                        System.out.println("NUMBER OF Torrent Files for Bot: " + payload.size());
                    else
                        System.out.println("NUMBER OF torrent files for bot: NONE");

                    return payload;
                }

                if(response.getStatusCode() == 500)
                    throw new IllegalStateException("Bot torrent file service threw an exception status: 500");

                return null;
            }

            @Override
            public void onThrowable(Throwable t){
                System.err.println("Bot torrent file servicefailed: ");
            }
        });


        List<RepoTorrentFile> trackerPeers = new ArrayList<>();

        try
        {

         trackerPeers = promise.get();
        } catch (InterruptedException ex)
        {
            ex.printStackTrace();
        } catch (ExecutionException e)
        {
            e.printStackTrace();
        }

        if(trackerPeers != null && !trackerPeers.isEmpty())
            dbManager.updateLastSyncEpoch(currentSync);

        return trackerPeers;

    }
}
