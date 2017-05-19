package workers;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import data.ClientTorrent;
import data.PeerInfo;
import data.RepoTorrentFile;
import db.DbManager;

import static spark.Spark.*;
import spark.*;
import spark.utils.IOUtils;
import spark.Request;

import jetbrains.exodus.entitystore.EntityId;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;



/**
 * Created by Stephen on 11/3/16.
 */
public class Tracker implements IAmAGnarlyComponent{

    public static Multimap<String, String> trackedPeers = ArrayListMultimap.create();
    private int PortNumber = 5555;
    public static Socket clientSocket = null;

    public void start() throws IOException {
        startWebServer();
        startTracker();
    }
    private void startTracker() throws IOException {
        Boolean Running = true;


        //Accepting connections
        ServerSocket serverSocket = null; // server socket for accepting connections
        try {
            serverSocket = new ServerSocket(PortNumber);
            System.out.println("Tracker is Listening on port: 5555.");
        } catch (IOException e) {
            System.err.println("Could not listen on port: 5555.");
            System.err.println(e);
            System.exit(1);
        }

        // Creating threads with accepted connections
        while (Running) {
            try {
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                System.err.println("Peer failed to connect.");
                System.err.println(e);
                System.exit(1);
            }
        }//end while
    }
    private static void startWebServer(){
        exception(Exception.class, (e, req, res) -> e.printStackTrace()); // print all exceptions
        // staticFiles.location("/public");
        port(8080);
        boolean localhost = true;
        //  if (localhost) {
        String projectDir = System.getProperty("user.dir");
        String staticDir = "/src/main/resources/public";
        staticFiles.externalLocation(projectDir + staticDir);
//        } else {
//            staticFiles.location("/public");
//        }
        //app endpoints
        get("/hello", (req, res) -> "Hello World");
        get("/ping", (req, res) -> "pong");
       // post("api/torrents/:TorrentId", -> addTorrent(); );
        //get("api/torrents.:TorrentId/port/:portNum");
        //get("api/torrents/:torrentId", );



        after((req, res) -> {
            if (res.body() == null) { // if the route didn't return anything
                res.redirect("/");
            }
        });        //wait for server to start



        awaitInitialization();
    }





    public static void addTorrent(String torrentId){
        String ipAddress = clientSocket.getInetAddress().toString();
        if (!trackedPeers.containsKey(torrentId)){
                trackedPeers.put(torrentId, ipAddress);
        }
        else {
            if(!trackedPeers.containsEntry(torrentId,ipAddress)){
                    trackedPeers.put(torrentId,ipAddress);
            }
        }
    }



    public static void peerKeepAlive(String torrentId, int portNum){

    }
    public static void getPeers(String torrentId){
        trackedPeers.get(torrentId);
    }

}
