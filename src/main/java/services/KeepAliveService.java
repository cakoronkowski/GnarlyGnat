package services;

import data.ClientTorrent;
import data.TorrentCache;
import db.DbManager;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;

import static java.lang.Thread.sleep;

/**
 * Created by cakor on 12/4/2016.
 */
public class KeepAliveService{
    public static void start()
    {
        System.out.println("Starting Keep Alive Service");
        Thread t =new Thread(new Runnable() {

            @Override
            public void run() {
                System.out.println("Running Keep Alive Service");
                AsyncHttpClient httpClient = new DefaultAsyncHttpClient();
                DbManager db = DbManager.getInstance();
                while (true)
                {
                    for (String key : TorrentCache.getHashTorrents().keySet()) {
                        ClientTorrent value = TorrentCache.getHashTorrents().get(key);

                        System.out.println("Sending Keep Alive");
                        httpClient
                                .prepareGet("http://" + value.getTrackers().get(0) + "/api/torrents/" + value.getTorrentId() + "/keepAlive?port=" + db.getCurrentSeedingPort())
                                .execute(new AsyncCompletionHandler<Response>() {
                                    @Override
                                    public Response onCompleted(Response response) throws Exception {
                                        System.out.println("keep alive success " + value.getTorrentId());
                                        return response;
                                    }

                                    @Override
                                    public void onThrowable(Throwable t) {
                                        System.err.println("KeepAlive Service failed");
                                        System.err.println("keepalive failed: " + t.getStackTrace());
                                        //failWithText(t.getLocalizedMessage());
                                    }
                                });
                        //use key and value
                    }
                    try {
                        sleep(60000);
                    }catch (Exception e)
                    {
                        System.err.println(e);
                    }

            }
            }
        });
        t.start();
    }

}
