package services;
import javafx.application.Platform;
import lombok.Synchronized;
import org.asynchttpclient.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by cakor on 11/27/2016.
 */
public class ClientRequestSynchronizer {

    private static int numberOfConnections=0;
    private static final int MAX_CONNECTIONS=5000;
    private static Object connectionLimiter=new Object();

    private final static AsyncHttpClientConfig clienConfig = new DefaultAsyncHttpClientConfig
            .Builder()
            .setMaxConnections(MAX_CONNECTIONS).build();

    private static AsyncHttpClient client = new DefaultAsyncHttpClient(clienConfig);

    public static void request(String ip, String portNo, String fileId, int chunkNo)
    {
        //AsyncHttpClientConfig config = new DefaultAsyncHttpClientConfig.Builder().setMaxConnections()
       // AtomicInteger connections = new AtomicInteger(0);

       // incrementConnections();
        client.prepareGet("http://"+ip+":"+portNo+"/api/torrents/"+fileId+"/chunks/"+chunkNo)
                .setHeader("start_chunk_req",String.valueOf(System.currentTimeMillis())).execute(new AsyncCompletionHandler<Response>(){

            @Override
            public Response onCompleted(Response response) throws Exception{
               // decrementConnections();
                // Do something with the Response
                // ...
               // System.out.println("SUCCESS:file:" + fileId + " chunk:" + chunkNo);

                ChunkResposeHandler.handle(response);
                return response;
            }

            @Override
            public void onThrowable(Throwable t) {
                System.err.println("ERROR: file:" + fileId + " chunk:" + chunkNo);
                try {
                    throw t;
                } catch (Exception ex)
                {

                } catch (Throwable throwable)
                {
                    System.err.println("another one.");

                }
               // System.err.println("error " + t.getMessage() + t.getStackTrace() + t.getCause().toString());
                //System.err.println(t.getLocalizedMessage());
                //decrementConnections();
                //t.printStackTrace();

                ChunkResposeHandler.updateFailedChunk(fileId, chunkNo);

                //System.err.println(t);
                // Something wrong happened.
            }
        });
    }

    public static boolean pingPeer(String address, String port){
        try {

        Future<Response> req = client.prepareGet("http://"+address+":"+port+"/api/ping").execute();
        Response response = req.get();
        return response.getStatusCode() == 200 && response.getResponseBody().equals("pong");
        } catch (ExecutionException ex)
        {
            System.err.println("Execution exception with pingPeer");
            return false;
        }
        catch (InterruptedException ex)
        {
            System.err.println("InterruptedException with pingPeer");
            return false;
        }
    }


    @Synchronized("connectionLimiter")
private static void incrementConnections()
{
    numberOfConnections++;
    if(numberOfConnections>MAX_CONNECTIONS)
    {
        try {
            synchronized (connectionLimiter)
            {
            connectionLimiter.wait();

            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
    @Synchronized("connectionLimiter")
    private static void decrementConnections()
    {
        numberOfConnections--;
        if (numberOfConnections<=MAX_CONNECTIONS) {
            synchronized (connectionLimiter)
            {
            connectionLimiter.notify();

            }
        }
    }


}
