package workers; /**
 * Created by Stephen on 11/3/16.
 */
import data.ClientConfig;
import data.NewTorrentFilePayload;
import db.DbManager;
import data.RepoTorrentFile;
import helpers.JsonConverter;
import jetbrains.exodus.entitystore.EntityId;
import spark.Request;
import static spark.Spark.*;
import spark.*;

import spark.template.velocity.*;
import spark.utils.IOUtils;
import javax.servlet.ServletOutputStream;
import java.io.*;
import java.time.Instant;
import java.util.*;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;


public class Repo implements IAmAGnarlyComponent {
    public Repo(int p)
    {
        if(p < 100)
            port = 8080;
        else
            port = p;
    }

    private static int port;

    public void start(){
        clientConfig = dbManager.GetClientConfig();
        startWebServer();
        System.out.println("Repository has been started");
    }

    private static DbManager dbManager = DbManager.getInstance();
    private static ClientConfig clientConfig;

    private static void startWebServer(){
        exception(Exception.class, (e, req, res) -> e.printStackTrace()); // print all exceptions
       // staticFiles.location("/public");

        if(clientConfig.isInBotMode() && clientConfig.hasPreferredPort())
            port = clientConfig.getPreferredPort();

        port(port);
        System.out.println("Repo Webserver running at: localhost:" + port);
        //boolean localhost = true;
      //  if (localhost) {
           // String projectDir = System.getProperty("user.dir");
          //  String staticDir = "/src/main/resources/public";
           // staticFiles.externalLocation(projectDir + staticDir);
//        } else {
//            staticFiles.location("/public");
//        }
        //setup static files location
        staticFiles.location("/public");

        //app endpoints
        get("/hello", (req, res) -> "Hello World");

        get("/", (req, res) -> renderHome(req));
        get("/torrents/:id/details", (req, res) -> renderTorrentFileDetails(req));
        get("/torrents/:id/edit",  (req,res) -> renderTorrentFilesEditGet(req));
        post("/torrents/:id/edit", (ICRoute)(req) -> editTorrent(req));
        get("/torrents/:id/download", "application/octet-stream", (req, res) -> returnTorrentFileDownload(req, res));
        get("/search", (req, res) -> renderSearch(req));

        //api endpoints
        get("/api/ping", (req, res) -> "pong");
        get("/api/botSync/:date", (req, res) -> returnSyncList(req,res));
        post("/api/torrents/register",  (req, res) -> registerTorrent(req, res));


        after((req, res) -> {
            if (res.body() == null) { // if the route didn't return anything
                res.redirect("/");
            }
        });        //wait for server to start



        awaitInitialization();
    }



    private static String renderHome(Request req){
        Map<String, Object> model = new HashMap<>();
        model.put("name", "Stephen Strickland");
        model.put("torrentFiles", dbManager.GetAllTorrentFiles());

        return renderTemplate("/templates/index.vm", model);
    }

    private static String renderSearch(Request req){
        Map<String, Object> model = new HashMap<>();
        model.put("searchText", req.queryParams("searchText"));
        model.put("torrentFiles", dbManager.GetTorrentFilesWithName(req.queryParams("searchText")));
        return renderTemplate("/templates/searchResults.vm", model);
    }
    private static String returnSyncList(Request req, Response res)
    {
        List<RepoTorrentFile> newTorrents = dbManager.GetAllRepoTorrentsAfterEpochDate(Long.valueOf(req.params("date")));

        if(newTorrents == null)
        {
            res.status(404);
            return "";
        }

        System.out.println("bot requesting sync, new torrents:" + newTorrents.size());
       // System.out.println(newTorrents.toString());


        return JsonConverter.dataToJson(newTorrents);
    }
    private static Object returnTorrentFileDownload(Request req, Response res)
    {
        RepoTorrentFile file = dbManager.GetTorrentFileById(req.params("id"));


        try{

            res.header("Content-Disposition", "attachment; filename=" + file.getTorrentId() + ".gg");
      //  res.header("Content-Disposition", String.format("attachment; filename=\"%s.zip\"", uuid)); application/octet-stream
        res.type("application/octet-stream");


            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            for (String element : file.getFileList()) {
                out.writeUTF(element);
                out.writeUTF("\n");
            }
            byte[] bytes = baos.toByteArray();
            String response = new String(bytes, "UTF-8");
            List<String> list= file.getFileList();
            String stringresponse="";

            for(int i=0;i<list.size();i++)
            {
             stringresponse+=list.get(i)+"\n";
            }
        res.raw().setContentLength(stringresponse.getBytes().length);
        res.status(200);
        System.out.println(stringresponse);
        final ServletOutputStream os = res.raw().getOutputStream();
        //final FileInputStream in = new FileInputStream();

        IOUtils.copy(new ByteArrayInputStream(stringresponse.getBytes()), os);
        //in.close();
        os.close();
        }
        catch (IOException ex)
        {

        }
        return "file";

    }

    private static String renderTorrentFileDetails(Request req){
        Map<String, Object> model = new HashMap<>();
        model.put("torrentId", req.params("id"));
        model.put("torrent", !req.params("id").equals("0") ? dbManager.GetTorrentFileById(req.params("id")) : new RepoTorrentFile());
        return renderTemplate("/templates/file_details.vm", model);
    }

    private static String renderTorrentFilesEditGet(Request req){

        Map<String, Object> model = new HashMap<>();
        model.put("torrentId", req.params("id"));
        model.put("torrent", !req.params("id").equals("0") ? dbManager.GetTorrentFileById(req.params("id")) : new RepoTorrentFile());

        return renderTemplate("/templates/file_edit.vm", model);

    }

    private static void editTorrent(Request req){
        //create the torrentfile here
        System.out.println("UPDATING TORRENT: " + req.params("id"));
        System.out.println("UPDATING TORRENT: " + req.queryParams("FileName"));
        System.out.println("UPDATING TORRENT: " + req.params("FileName"));
        RepoTorrentFile file = new RepoTorrentFile();

        file.setFileSize(Long.parseLong(req.queryParams("FileSize")));
        file.setFileName(req.queryParams("FileName"));
        file.setChunkSize(Integer.parseInt(req.queryParams("ChunkSize")));
        if(!req.params("id").equals("0")) {
            System.out.println("Updating File");
            file.setTorrentId(req.queryParams("TorrentId"));
            dbManager.UpdateTorrentFile(file);

        }
        else
        {
            System.out.println("Updating File");
            dbManager.InsertTorrentFile(file);
        }



    }

    private static String registerTorrent(Request req, Response res){
        try {
            System.out.println("Attempting to register file");
            ObjectMapper mapper = new ObjectMapper();
            NewTorrentFilePayload payload = mapper.readValue(req.body(), NewTorrentFilePayload.class);
            if (!payload.isValid()) {
                System.err.println("register torrent: 422");
                res.status(422);
                return "";
            }


            RepoTorrentFile file = new RepoTorrentFile();
            file.setFileName(payload.getFileInfo().getFileName());
            file.setFileSize(payload.getFileInfo().getLength());
            file.setChunkSize(payload.getFileInfo().getChunkSize());
            file.setTorrentId(payload.getTorrentId());
            file.setTrackers(Arrays.asList(payload.getTrackerIp()));
            file.setCreateDateEpoch(Instant.now().toEpochMilli());
            file.setHashList(payload.getHashList());

            dbManager.UpsertTorrentFile(file);
            res.status(200);
            res.type("application/json");
            return "";
        } catch (JsonParseException jpe) {
            System.err.print(jpe.getStackTrace());
            res.status(400);
            return "";
        }
        catch (IOException ex)
        {
            System.err.print(ex.getStackTrace());
            res.status(400);
            return "";
        }
    }


    public static void stopServer(){
        stop();
    }

    private static String renderTemplate(String template, Map model) {
        return new VelocityTemplateEngine().render(new ModelAndView(model, template));
    }

    @FunctionalInterface
    private interface ICRoute extends Route {
        default Object handle(Request request, Response response) throws Exception {
            handle(request);
            return "";
        }
        void handle(Request request) throws Exception;
    }

    public static String dataToJson(Object data) {
       return JsonConverter.dataToJson(data);
    }
}
