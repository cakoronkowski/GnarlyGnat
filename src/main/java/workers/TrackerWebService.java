package workers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import data.*;
import db.DbManager;
import helpers.JsonConverter;
import jetbrains.exodus.entitystore.EntityId;
import services.NetworkHelper;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.template.velocity.VelocityTemplateEngine;
import spark.utils.IOUtils;

import javax.servlet.ServletOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

import static spark.Spark.*;
import static spark.Spark.awaitInitialization;

/**
 * Created by Stephen on 11/28/16.
 */
public class TrackerWebService {
    public TrackerWebService(int p)
    {
        if(p < 100)
            port = 8080;
        else
            port = p;
    }

    public void start(){
        startWebService();
    }

    private static DbManager dbManager;
    private static ClientConfig clientConfig;
    private static int port;

    private static void startWebService()
    {
        dbManager = DbManager.getInstance();
        clientConfig = dbManager.GetClientConfig();


        try {
            port = getAvailablePort();
            System.out.println("Tracker WebServer Running at: localhost:" + port);
        }
        catch (NoSuchElementException ex)
        {
            System.err.println(ex);
        }

        exception(Exception.class, (e, req, res) -> e.printStackTrace()); // print all exceptions
        if(clientConfig.isInBotMode() && clientConfig.hasPreferredPort())
            port = clientConfig.getPreferredPort();

        port(port);
        //api endpoints
        post("/api/torrents/:torrentId/register",  (req, res) -> registerTorrent(req, res));
        get("api/torrents/:torrentId/peers", (req, res) -> getPeersForTorrentByTorrentId(req, res));
        get("api/torrents/:torrentId/keepAlive", (req, res) -> keepClientAlive(req, res));
        get("/api/ping", (req, res) -> "pong");

        awaitInitialization();
    }

    private static String getPeersForTorrentByTorrentId(Request req, Response res){
        String torrentId = req.params("torrentId");

        int requestingPort = 0;
        try{
            if(req.queryParams() != null && req.queryParams("port") != null)
            requestingPort = Integer.valueOf(req.queryParams("port"));
        } catch (NumberFormatException ex)
        {
            ex.printStackTrace();
        }
        String requestingIp = req.ip();
        if(torrentId == null || torrentId.equals(""))
        {
            logErr("get peers 422");
            res.status(422);
            return "";
        }

        System.out.println(req.ip() + " is requesting peers for " + torrentId);
        long startReqTime  =System.currentTimeMillis();
        List<TrackerPeer> peers = dbManager.GetPeersForTorrentByTorrentIdAndFilterRequestingIp(torrentId, requestingIp, requestingPort, System.currentTimeMillis() - 120000);
        long endReqTime = System.currentTimeMillis();
        if(peers == null)
        {
            logErr("No peers: 404");
            res.status(404);
            return "";
        }

        log("getPeersForTorrentByTorrentId");
        log(JsonConverter.dataToJson(peers));
        res.header("lookup_millis", String.valueOf(endReqTime - startReqTime));
        return JsonConverter.dataToJson(peers);
    }

    private static String keepClientAlive(Request req, Response res){
        try {
            log("Start keep alive");
            String torrentId = req.params("torrentId");
            String ip = req.ip();
            int port = Integer.valueOf(req.queryParams("port"));
            if(ip == null || port < 100)
            {
                logErr("ip == null || port < 100: return 422");
                res.status(422);
                return "";
            }
            log("keep client alive: " + ip + " for torrent: " + torrentId);
            dbManager.UpsertTrackerPeerForTorrent(torrentId, new TrackerPeer(ip, port));
        }
        catch (Exception ex)
        {
            logErr(ex.getStackTrace().toString());
            res.status(500);
            return "";
        }
        res.status(200);

        log("keepClientAlive");
        return "";
    }


    private static String registerTorrent(Request req, Response res){

        String torrentId = req.params("torrentId");
        if(torrentId == null || torrentId.equals(""))
        {
            log("registerTorrent:FAILED:422");
            res.status(422);
            return "";
        }

        try {
        log("registerTorrent");
            dbManager.InsertTrackerTorrent(new TrackerTorrent(torrentId, new ArrayList<TrackerPeer>()));
            res.status(200);
            res.type("application/json");
            return "";
        }
        catch (Exception ex)
        {
            res.status(500);
            logErr(ex.toString());
            return "";
        }
    }

    public static void stopServer(){
        stop();
    }

    @FunctionalInterface
    private interface ICRoute extends Route {
        default Object handle(Request request, Response response) throws Exception {
            handle(request);
            return "";
        }
        void handle(Request request) throws Exception;
    }

    private static int getAvailablePort() throws NoSuchElementException{
        if(NetworkHelper.isLocalPortAvailable(port))
            return port;
        int port = NetworkHelper.MIN_PORT_NUMBER;
        for (int i = NetworkHelper.MIN_PORT_NUMBER; i <= NetworkHelper.MAX_PORT_NUMBER; i++)
        {
            if(NetworkHelper.isLocalPortAvailable(i))
                return i;
        }

        throw new NoSuchElementException("no ports are available");

    }

    private static void logErr(String line)
    {
        System.err.println(line);
    }
    private static void log(String line)
    {
        System.out.println(line);
    }

}
