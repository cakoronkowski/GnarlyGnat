package workers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import data.*;
import db.DbManager;
import helpers.DownloadService;
import helpers.JsonConverter;
import javafx.application.Platform;
import javafx.concurrent.Task;
import jetbrains.exodus.entitystore.EntityId;
import services.KeepAliveService;
import services.LeecherService;
import services.NetworkHelper;
import services.PeerListUpdaterService;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static spark.Spark.*;
import static spark.Spark.awaitInitialization;
import static spark.Spark.get;

/**
 * Created by cakor on 11/27/2016.
 */
public class ClientWorkerTask extends Task
{

    public ClientWorkerTask(ClientViewModel vm) {
        viewModel = vm;
    }

    ClientViewModel viewModel;
    private static DbManager dbManager;
    private static ClientConfig clientConfig;
    private boolean inBotMode;
    private int sleepMillis = 1000;

    @Override
    protected Object call() throws Exception {
        dbManager = DbManager.getInstance();
        clientConfig = dbManager.GetClientConfig();
        inBotMode = clientConfig.isInBotMode();
        sleepMillis = clientConfig.getBotSleepInMillis();

        KeepAliveService.start();
        PeerListUpdaterService.start();
        System.out.println("Starting SEED FROM WORKER");
        startSeedInterface();
        System.out.println("Starting Leecher Service FROM WORKER");
        startLeechService();


        return null;
    }


    private static void startSeedInterface()
    {
        int port = NetworkHelper.MIN_PORT_NUMBER;
        try {
            port = getAvailablePort();
            System.out.println("Client WebServer Running at: localhost:" + port);
        }
        catch (NoSuchElementException ex)
        {
            System.err.println(ex);
        }
        if(clientConfig.isInBotMode() && clientConfig.hasPreferredPort() && NetworkHelper.isLocalPortAvailable( clientConfig.getPreferredPort()))
            port = clientConfig.getPreferredPort();

        exception(Exception.class, (e, req, res) -> e.printStackTrace()); // print all exceptions
        port(port);

        //api endpoints
        spark.Spark.get("api/dbtorrents", (req, res) -> {
            return dataToJson(dbManager.GetAllClientTorrents());
        });
        spark.Spark.get("api/cachetorrents", (req, res) -> {
            return dataToJson(TorrentCache.getHashTorrents().values());
        });
        spark.Spark.get("/api/torrents/:torrentId/chunks/:chunkNum",  (req, res) -> getChunk(req, res));
        spark.Spark.get("/api/ping", (req, res) -> "pong");
        dbManager.updateCurrentSeedingPort(String.valueOf(port));
        awaitInitialization();
    }
    private void startLeechService()
    {
        //load torrents form DB
        System.out.println("startLeechService()");
        List<ClientTorrent> torrents = dbManager.GetAllClientTorrents();
        //add torrents to hashmap and viewmodel
        //push incomplete torrents to list


        Platform.runLater(() -> {
            for (int i = 0; i < torrents.size(); i++) {
                TorrentCache.addTorrent(torrents.get(i));
                viewModel.getTorrents().add(torrents.get(i));
                if (torrents.get(i).getStatus() != FileStatus.COMPLETED) {
                    TorrentCache.addIncomplete(torrents.get(i));
                }
            }
        });
        while (!isCancelled() && !isDone()) {


            LeecherService.startLeeching();


            if(!clientConfig.isInBotMode()) {


                try {

                    Thread.sleep(3000);
                } catch (InterruptedException ex) {
                    System.err.println("Thread was inturrupted: " + ex.getStackTrace());
                }
            }
            else {

                List<RepoTorrentFile> newTorrents = BotTorrentFileService.getMostRecentRepoTorrentFiles();
                if(newTorrents != null && !newTorrents.isEmpty())
                    DownloadService.startDownload(newTorrents );

                try {

                    Thread.sleep(sleepMillis);
                } catch (InterruptedException ex) {
                    System.err.println("Thread was inturrupted: " + ex.getStackTrace());
                }
            }
        }
        System.err.println("CLIENT WORKER DONE!!!: cancelled:" + isCancelled() + " done:" + isDone() );
        //start loop of requesting chunks of incomplete torrents
    }


    /**
     * Gets the chunk requested. Returns 500 for any exceptions, 404 if chunk not found
     * @see GetChunkResponse
     * @param req
     * @param res
     * @return JSON of GetChunkResponse
     */
    private static String getChunk(Request req, Response res){

        res.header("start_chunk_req", req.headers("start_chunk_req"));


        if (req.params("torrentId") == null || req.params("chunkNum") == null) {
            res.status(422);
            return JsonConverter.dataToJson(new GetChunkResponse(null, "Invalid Params", false, null, -1));
        }

        String id = req.params("torrentId");
        int chunk = Integer.parseInt(req.params("chunkNum"));
        System.out.println("Peer requesting chunk: " + chunk + " from: " + id);

        try {




            byte[] data = null;
            ClientTorrent torrent;
            //get torrent from hashmap
            if (TorrentCache.getHashTorrents().containsKey(id)) {
                //in hashmap
                torrent = TorrentCache.getTorrentById(id);
            } else {
                torrent = dbManager.GetClientTorrentById(id);
                //not currently in mem, add it to the hastable
                if(torrent != null)
                    TorrentCache.addTorrent(torrent);
            }

            if(torrent == null || !torrent.getFileManager().hasChunk(chunk))
            {
                System.err.println("We don't have that chunk: " + chunk + " of torrent: " + id);
                res.header("torrentId", id);
                res.header("chunk", String.valueOf(chunk));
                res.status(404);
                return JsonConverter.dataToJson(new GetChunkResponse(null, "Chunk not found", false, id, chunk));
            }

            //get chunk byte array
            System.err.println("We have that chunk: " + chunk + " of torrent: " + id);
            data = new byte[torrent.getFileInfo().getChunkSize()];
            torrent.getFileManager().getChunk(data, chunk);
            res.header("torrentId", id);
            res.header("chunk", String.valueOf(chunk));
            res.status(200);
            res.type("application/json");
            return dataToJson(new GetChunkResponse(data, "Here ya go", true,torrent.getTorrentId(), chunk));
        } catch (Exception ex) {
            res.header("torrentId", id);
            res.header("chunk", String.valueOf(chunk));
            res.status(500);
            return JsonConverter.dataToJson(new GetChunkResponse(null, ex.toString(), false, req.params("torrentId"), Integer.parseInt(req.params("chunkNUm"))));
        }
    }

    private static int getAvailablePort() throws NoSuchElementException{
        int port = NetworkHelper.MIN_PORT_NUMBER;
        for (int i = NetworkHelper.MIN_PORT_NUMBER; i <= NetworkHelper.MAX_PORT_NUMBER; i++)
        {
            if(NetworkHelper.isLocalPortAvailable(i))
                return i;
        }

        throw new NoSuchElementException("no ports are available");

    }

    public static String dataToJson(Object data) {
        return JsonConverter.dataToJson(data);
    }


}
