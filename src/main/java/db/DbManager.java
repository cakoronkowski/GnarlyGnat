package db;

import data.*;
import helpers.EnumHelper;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.bindings.ComparableBinding;
import jetbrains.exodus.bindings.StringBinding;
import jetbrains.exodus.entitystore.PersistentEntityStore;
import jetbrains.exodus.entitystore.*;
import jetbrains.exodus.env.*;
import jetbrains.exodus.util.LightOutputStream;
import lombok.Synchronized;
import lombok.extern.java.Log;
import org.jetbrains.annotations.NotNull;
import services.FileManager;

import javax.xml.stream.events.EndElement;
import java.io.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static jetbrains.exodus.bindings.StringBinding.entryToString;
import static jetbrains.exodus.bindings.StringBinding.stringToEntry;

/**
 * Created by Stephen on 11/4/16.
 */

public class DbManager implements Closeable {

    private PersistentEntityStore store;
    private Environment environment;

    private static DbManager instance;

    private DbManager(){
        construct();
    }

    private static class DbManagerHelper{
        private static final DbManager INSTANCE = new DbManager();
    }

    public static DbManager getInstance() {
        return DbManagerHelper.INSTANCE;
    }


    //props
    private static final String PERSISTENT_ENTITY_STORE_LOCATION = "data";
    private static final String ENVIROMENT_LOCATION = "envdata";
    private static final String TORRENTFILE_TYPE = "RepoTorrentFile";
    private static final String CLIENTORRENT_TYPE = "ClientTorrent";
    private static final String TRACKERTORRENT_TYPE = "TrackerTorrent";
    private static final String TRACKERPEER_TYPE = "TrackerPeer";
    private static final String CLIENTCONFIG_TYPE = "ClientConfig";
    private static final String TORRENTAUDIT_TYPE = "TorrentAudit";
    private static final String CHUNKAUDIT_TYPE = "ChunkAudit";


    private void construct(){
        store = PersistentEntityStores.newInstance(PERSISTENT_ENTITY_STORE_LOCATION);
        environment = Environments.newInstance(ENVIROMENT_LOCATION);
    }


    /**
     * Return a list of all torrent files in database.
     *
     * @see RepoTorrentFile
     * @see List
     * @return List of data.RepoTorrentFile
     */
    @Synchronized
    public List<RepoTorrentFile> GetAllTorrentFiles(){
        List<RepoTorrentFile> results = computeInReadOnlyStore( txn ->
             StreamSupport.stream(txn.getAll(TORRENTFILE_TYPE).spliterator(), false).map(entityToTorrentFileFunction).collect(Collectors.toList()));

        return results;
    }


    /**
     * Returns the data.RepoTorrentFile with the id, null if not found
     *
     * @see RepoTorrentFile
     * @param id TorrentId of the file
     * @return data.RepoTorrentFile where TorrentId == id if none, returns null
     */
    @Synchronized
    public RepoTorrentFile GetTorrentFileById(String id){

        return computeInReadOnlyStore(txn -> {
            Entity file = txn.find(TORRENTFILE_TYPE, "TorrentId", id).getFirst();
            if(file != null)
                return entityToTorrentFileFunction.apply(file);
            return null;
        });
    }

    /**
     * Returns List of data.RepoTorrentFile where data.RepoTorrentFile.FileName starts with @name
     *
     * @see RepoTorrentFile
     * @see List
     * @param name Name of the data.RepoTorrentFile
     * @return List of TorrentFiles where FileName starts with @name
     */
    @Synchronized
    public List<RepoTorrentFile> GetTorrentFilesWithName(String name)
    {

        return computeInReadOnlyStore(txn -> {
            return StreamSupport.stream(txn.findStartingWith(TORRENTFILE_TYPE, "FileName", name).spliterator(), false).map(entityToTorrentFileFunction).collect(Collectors.toList());
        });

    }


    /**
     * Function that maps one Entity to data.RepoTorrentFile
     * @see RepoTorrentFile
     * @see Entity
     */
    private static Function<Entity, RepoTorrentFile> entityToTorrentFileFunction = new Function<Entity, RepoTorrentFile>() {
        @Override
        public RepoTorrentFile apply(Entity entity) {
            RepoTorrentFile torrent = new RepoTorrentFile();
            if(entity.getProperty("ChunkSize") != null)
                torrent.setChunkSize((int)entity.getProperty("ChunkSize"));
            if(entity.getProperty("FileName") != null)
                torrent.setFileName((String)entity.getProperty("FileName"));
            if(entity.getProperty("FileSize") != null)
                torrent.setFileSize((long)entity.getProperty("FileSize"));
            if(entity.getProperty("TorrentId") != null)
                torrent.setTorrentId((String)entity.getProperty("TorrentId"));
            if(entity.getProperty("Id") != null)
                torrent.setId((EntityId) entity.getProperty("Id"));
            else
                torrent.setId(entity.getId());
            if(entity.getProperty("epoch") != null)
            {
                torrent.setCreateDateEpoch((long)entity.getProperty("epoch"));
            }
            List<String> trackers = new ArrayList<>();
            for(Entity tracker : entity.getLinks("Trackers"))
            {
                trackers.add((String)tracker.getProperty("Address"));
            }
            torrent.setTrackers(trackers);

            List<String> hashes = new ArrayList<>();
            for(Entity hash : entity.getLinks("Hashes"))
            {
                hashes.add((String) hash.getProperty("val"));
            }
            torrent.setHashList(hashes.toArray(new String[0]));
            return torrent;
        }
    };


    /**
     * Inserts data.RepoTorrentFile and returns the EntityId for the inserted file
     * @see RepoTorrentFile
     * @see Entity
     * @param file data.RepoTorrentFile to be inserted into the database
     * @return EntityId of the file, doesn't really do anything because all searching is done with TorrentId
     */
    @Synchronized
    public EntityId InsertTorrentFile(RepoTorrentFile file){

        return computeInStore(txn -> {
            final Entity torrentFile = txn.newEntity(TORRENTFILE_TYPE);

            torrentFile.setProperty("FileName", file.getFileName());
            torrentFile.setProperty("FileSize", file.getFileSize());
            torrentFile.setProperty("ChunkSize", file.getChunkSize());
            torrentFile.setProperty("TorrentId", file.getTorrentId());
            torrentFile.setProperty("epoch", file.getCreateDateTimeEpoch());

            for(String t : file.getTrackers())
            {
                Entity tracker = txn.newEntity("Tracker");
                tracker.setProperty("Address", t);
                tracker.setLink("ParentRepoFile", torrentFile);
                torrentFile.addLink("Trackers", tracker);
            }

            for(String h : file.getHashList())
            {
                Entity hash = txn.newEntity("Hash");
                hash.setProperty("val", h);
                hash.setLink("ParentRepoFile", torrentFile);
                torrentFile.addLink("Hashes", hash);
            }

            return torrentFile.getId();
        });
    }

    /**
     * Finds all torrent files that have been created after the @epochDate
     * @param epochDate
     * @return List of torrent files
     */

    @Synchronized
    public List<RepoTorrentFile> GetAllRepoTorrentsAfterEpochDate(long epochDate)
    {
        return computeInReadOnlyStore( txn -> {
            //find all torrents that have a create date after @epochDate
            return StreamSupport.stream(txn.find(TORRENTFILE_TYPE, "epoch", epochDate, Long.MAX_VALUE).spliterator(), false).map(entityToTorrentFileFunction).collect(Collectors.toList());
        });
    }


    /**
     * Updates the data.RepoTorrentFile with file where TorrentId == file.TorrentId
     * @see RepoTorrentFile
     * @param file The data.RepoTorrentFile to be updated
     */
    @Synchronized
    public void UpdateTorrentFile(RepoTorrentFile file){

        executeInStore(txn -> {
            final Entity torrentFile = txn.find(TORRENTFILE_TYPE, "TorrentId", file.getTorrentId()).getFirst();

            torrentFile.setProperty("FileName", file.getFileName());
            torrentFile.setProperty("FileSize", file.getFileSize());
            torrentFile.setProperty("ChunkSize", file.getChunkSize());
            torrentFile.setProperty("epoch", file.getCreateDateTimeEpoch());

            for(Entity tracker : torrentFile.getLinks("Trackers"))
            {
                tracker.delete();
            }

            for(Entity hash : torrentFile.getLinks("Hashes"))
            {
                hash.delete();
            }
            for(String t : file.getTrackers())
            {
                Entity tracker = txn.newEntity("Tracker");
                tracker.setProperty("Address", t);
                tracker.setLink("ParentRepoFile", torrentFile);
                torrentFile.addLink("Trackers", tracker);
            }

            for(String h : file.getHashList())
            {
                Entity hash = txn.newEntity("Hash");
                hash.setProperty("val", h);
                hash.setLink("ParentRepoFile", torrentFile);
                torrentFile.addLink("Hashes", hash);
            }
        });
    }

    @Synchronized
    public void UpsertTorrentFile(RepoTorrentFile file)
    {
        RepoTorrentFile t = GetTorrentFileById(file.getTorrentId());
        if(t == null)
        {
            InsertTorrentFile(file);
        }
        else
        {
            UpdateTorrentFile(file);
        }
    }




    //Client Torrent Files
    @Synchronized
    public List<ClientTorrent> GetAllClientTorrents(){

        return computeInReadOnlyStore(txn -> {
            return StreamSupport.stream(txn.getAll(CLIENTORRENT_TYPE).spliterator(), false).map(entityToClientTorrentFunction).collect(Collectors.toList());

        });
    }
    @Synchronized
    public ClientTorrent GetClientTorrentById(String torrentId)
    {
        return computeInReadOnlyStore(txn -> {
            Entity file = txn.find(CLIENTORRENT_TYPE, "TorrentId", torrentId).getFirst();
            if(file != null)
                return entityToClientTorrentFunction.apply(file);
            return null;
        });
    }
    @Synchronized
    public void InsertClientTorrent(ClientTorrent torrent)
    {
        executeInStore( txn -> {
            Entity t = txn.newEntity(CLIENTORRENT_TYPE);

            createFileInfoForTorrentEntity(txn, t, torrent.getFileInfo());
            createFileManagerForTorrentEntity(txn, t, torrent.getFileManager());

            t.setProperty("FileStatus", torrent.getStatus().toString());

            t.setProperty("isComplete", torrent.getIsComplete());

            t.setProperty("TorrentId", torrent.getTorrentId());

            t.setProperty("CreateDateTime", torrent.getCreateDateTime().toString());
            if(torrent.getTrackers() != null)
                for(String tracker : torrent.getTrackers())
                {
                    Entity trackerIp = txn.newEntity("Tracker");
                    trackerIp.setProperty("name", tracker);
                    trackerIp.setLink("parentTorrent", t);
                    t.addLink("Trackers", trackerIp);
                }

            if(torrent.getPeers() != null)
                for(PeerInfo peer : torrent.getPeers())
                {
                    Entity p = txn.newEntity("PeerInfo");
                    p.setProperty("IpAddress", peer.IpAddress);
                    p.setProperty("Port", peer.Port);
                    p.setProperty("Status", peer.Status.toString());
                    p.setLink("parentTorrent", t);
                    t.addLink("Peers", p);
                }
        });
    }

    @Synchronized
    public void UpdateClientTorrent(ClientTorrent torrent){

        executeInStore( txn -> {
            final Entity t = txn.find(CLIENTORRENT_TYPE, "TorrentId", torrent.getTorrentId()).getFirst();

            for(Entity tracker : t.getLinks("Trackers"))
            {
                tracker.delete();
            }

            for (Entity tracker : t.getLinks("Peers")) {
                tracker.delete();
            }

            updateFileInfoForTorrentEntity(txn, t, torrent.getFileInfo());
            updateFileManagerForTorrentEntity(txn, t, torrent.getFileManager());

            t.setProperty("FileStatus", torrent.getStatus().toString());

            t.setProperty("isComplete", torrent.getIsComplete());

            t.setProperty("TorrentId", torrent.getTorrentId());

            t.setProperty("CreateDateTime", torrent.getCreateDateTime().toString());

            for(String tracker : torrent.getTrackers())
            {
                Entity trackerIp = txn.newEntity("Tracker");
                trackerIp.setProperty("name", tracker);
                trackerIp.setLink("parentTorrent", t);
                t.addLink("Trackers", trackerIp);
            }

            for(PeerInfo peer : torrent.getPeers())
            {
                Entity p = txn.newEntity("PeerInfo");
                p.setProperty("IpAddress", peer.IpAddress);
                p.setProperty("Port", peer.Port);
                p.setProperty("Status", peer.Status.toString());
                p.setLink("parentTorrent", t);
                t.addLink("Peers", p);
            }

        });
    }

    private static void createFileInfoForTorrentEntity(StoreTransaction txn, Entity torrent, FileInfo info)
    {
        Entity fileInfo = txn.newEntity("FileInfo");

        fileInfo.setProperty("FileName", info.getFileName());

        fileInfo.setProperty("FileLength", info.getLength());

        fileInfo.setProperty("ChunkCount", info.getNumberOfChunks());

        fileInfo.setProperty("SizeOfChunks", info.getChunkSize());
        fileInfo.setProperty("AbsolutePath", info.getAbsolutePath());


        torrent.setLink("FileInfo", fileInfo);
        fileInfo.setLink("ParentTorrent", torrent);
    }

    private static void updateFileInfoForTorrentEntity(StoreTransaction txn, Entity torrent, FileInfo info)
    {
        torrent.getLink("FileInfo").delete();
        createFileInfoForTorrentEntity(txn, torrent, info);
    }

    private static void createFileManagerForTorrentEntity(StoreTransaction txn, Entity torrent, FileManager manager)
    {
        Entity fileManager = txn.newEntity("FileManager");

        fileManager.setProperty("FileLength", manager.getLength());
        fileManager.setProperty("ChunkSize", manager.getChunk_size());

        fileManager.setProperty("Mode", manager.getFileAccessMode());

        fileManager.setProperty("FileInputName", manager.getFileName());
        fileManager.setProperty("SeedMode", manager.isSeedMode());


        for(ChunkStatus status : manager.getChunkStatusList())
        {
            Entity s = txn.newEntity("ChunkStatus");
            s.setProperty("val", status.toString());
            fileManager.addLink("StatusList", s);
        }

        torrent.setLink("FileManager", fileManager);
        fileManager.setLink("ParentTorrent", torrent);
    }
    private static void updateFileManagerForTorrentEntity(StoreTransaction txn, Entity torrent, FileManager manager) {
        torrent.getLink("FileManager").delete();
        createFileManagerForTorrentEntity(txn, torrent, manager);
    }


    /**
     * Function that maps one Entity to data.ClientTorrent
     * @see ClientTorrent
     * @see Entity
     */
    private static Function<Entity, ClientTorrent> entityToClientTorrentFunction = new Function<Entity, ClientTorrent>() {
        @Override
        public ClientTorrent apply(Entity entity) {
            List<String> trackers = new ArrayList<>();
            for(Entity tracker : entity.getLinks("Trackers"))
            {
                trackers.add((String)tracker.getProperty("name"));
            }

            List<PeerInfo> peers = new ArrayList<>();
            for(Entity tracker : entity.getLinks("Peers"))
            {
                peers.add(new PeerInfo((String)tracker.getProperty("IpAddress"), (int)tracker.getProperty("IpAddress"), (PeerStatus.fromString((String) tracker.getProperty("IpAddress")))));
            }

            return new ClientTorrent(
                    FileInfo.entityToFileManager.apply(entity.getLink("FileInfo")),
                    FileManager.entityToFileManager.apply(entity.getLink("FileManager")),
                   // (FileInfo) entity.getProperty("FileInfo"),
                    //(FileManager) entity.getProperty("FileManager"),
                    EnumHelper.getFileStatusFromString((String)entity.getProperty("FileStatus")),
                    trackers,
                    peers,
                    (boolean)entity.getProperty("isComplete"),
                    (String) entity.getProperty("TorrentId"),
                    LocalDateTime.parse((String)  entity.getProperty("CreateDateTime")));
        }
    };

    //settings
    @Synchronized
    public String getDefaultTracker(){

        return computeInReadOnlyEnviroment(txn -> {
            final Store store = environment.openStore("Settings", StoreConfig.WITHOUT_DUPLICATES, txn);

            ByteIterable tracker = store.get(txn, stringToEntry("def_tracker"));
            if(tracker == null)
            {
                return "192.168.1.24:7000";
            }
            return entryToString(tracker);
        });
    }
    @Synchronized
    public void updateDefaultTracker(String tracker){


        executeInEnviroment(txn -> {
            final Store store = environment.openStore("Settings", StoreConfig.WITHOUT_DUPLICATES, txn);
            store.put(txn, StringBinding.stringToEntry("def_tracker"), StringBinding.stringToEntry(tracker));
        });
    }
    @Synchronized
    public String getDefaultDownloadDirectory(){


        return computeInReadOnlyEnviroment(txn -> {
            final Store store = environment.openStore("Settings", StoreConfig.WITHOUT_DUPLICATES, txn);
            ByteIterable repo = store.get(txn, stringToEntry("def_down_dir"));

            if(repo == null)
            {
                return "";
            }
            return entryToString(repo);
        });
    }
    @Synchronized
    public void updateDefaultDownloadDirectory(String tracker){


        executeInEnviroment(txn -> {
            final Store store = environment.openStore("Settings", StoreConfig.WITHOUT_DUPLICATES, txn);
            store.put(txn, StringBinding.stringToEntry("def_down_dir"), StringBinding.stringToEntry(tracker));
        });
    }

    @Synchronized
    public String getDefaultRepo(){

        return computeInReadOnlyEnviroment(txn -> {
            final Store store = environment.openStore("Settings", StoreConfig.WITHOUT_DUPLICATES, txn);
            ByteIterable repo = store.get(txn, stringToEntry("def_repo"));

            if(repo == null)
            {
                return "192.168.1.23:8080";
            }
            return entryToString(repo);
        });
    }
    @Synchronized
    public void updateDefaultRepo(String repo){


        executeInEnviroment(txn -> {
            final Store store = environment.openStore("Settings", StoreConfig.WITHOUT_DUPLICATES, txn);
            store.put(txn, StringBinding.stringToEntry("def_repo"), StringBinding.stringToEntry(repo));
        });

    }



    @Synchronized
    public long getLastSyncEpoch(){

        String e = computeInReadOnlyEnviroment(txn -> {
            final Store store = environment.openStore("Settings", StoreConfig.WITHOUT_DUPLICATES, txn);
            ByteIterable repo = store.get(txn, stringToEntry("last_sync_epoch"));

            if(repo == null)
            {
                return "";
            }
            return entryToString(repo);
        });
        if(e.isEmpty())
            return 0;
        return Long.parseLong(e);
    }
    @Synchronized
    public void updateLastSyncEpoch(long epoch){


        executeInEnviroment(txn -> {
            final Store store = environment.openStore("Settings", StoreConfig.WITHOUT_DUPLICATES, txn);
            store.put(txn, StringBinding.stringToEntry("last_sync_epoch"), StringBinding.stringToEntry(String.valueOf(epoch)));
        });

    }


    @Synchronized
    public String getCurrentSeedingPort(){
        return computeInReadOnlyEnviroment(txn -> {
            final Store store = environment.openStore("Settings", StoreConfig.WITHOUT_DUPLICATES, txn);
            ByteIterable repo = store.get(txn, stringToEntry("current_port"));

            if(repo == null)
            {
                System.err.println("repo null");
                return "";
            }
            return entryToString(repo);
        });
    }
    @Synchronized
    public void updateCurrentSeedingPort(String port){
        executeInEnviroment(txn -> {
            final Store store = environment.openStore("Settings", StoreConfig.WITHOUT_DUPLICATES, txn);
            store.put(txn, StringBinding.stringToEntry("current_port"), StringBinding.stringToEntry(port));
        });
    }

    private void executeInEnviroment(Consumer<Transaction> execute){
        environment.executeInTransaction(txn -> {execute.accept(txn);});
    }

    private <T> T computeInEnviroment(Function<Transaction, T> compute)
    {
        return environment.computeInTransaction(txn -> {return compute.apply(txn);});
    }

    private <T> T computeInReadOnlyEnviroment(Function<Transaction, T> compute)
    {
        return environment.computeInReadonlyTransaction(txn -> {return compute.apply(txn);});
    }


    //Tracker stuff


    public void UpsertTrackerPeerForTorrent(String torrentId, TrackerPeer peer)
    {
        TrackerPeer p = GetTrackerPeerByTorrentIdAndPeerId(torrentId, peer.getIpAddress(), peer.getPort());
        if(p == null || peer.getIpAddress() == null || peer.getIpAddress().isEmpty())
        {
            System.out.println("inserting peer");
            InsertTrackerPeer(torrentId, peer);
        }
        else
        {
            System.out.println("updating peer");
            UpdateTrackerPeer(torrentId, peer);
        }
    }

    private void InsertTrackerPeer(String torrentId, TrackerPeer p){

        executeInStore(txn -> {
            Entity torrent = txn.find(TRACKERTORRENT_TYPE, "TorrentId", torrentId).getFirst();
            if(torrent != null) {
                Entity peer = txn.newEntity(TRACKERPEER_TYPE);
                peer.setProperty("IpAddress", p.getIpAddress());
                peer.setProperty("CreateDateTime", p.getCreateDateTime().toString());
                peer.setProperty("Port", p.getPort());
                peer.setProperty("LastKeepAliveDateTime", p.getLastKeepAliveDateTime().toString());
                peer.setProperty("LastKeepAliveEpoch", p.getLastKeepAliveEpoch());
                peer.setLink("ParentTorrent", torrent);
                torrent.addLink("Peers", peer);

            }
        });
    }

    private void UpdateTrackerPeer(String torrentId, TrackerPeer p){

        executeInStore(txn -> {
            Entity torrent = txn.find(TRACKERTORRENT_TYPE, "TorrentId", torrentId).getFirst();
            if(torrent != null) {
                for(Entity peer : torrent.getLinks("Peers"))
                {
                    if (peer.getProperty("IpAddress").equals(p.getIpAddress()))
                    {
                        peer.setProperty("Port", p.getPort());
                        peer.setProperty("LastKeepAliveDateTime", p.getLastKeepAliveDateTime().toString());
                        peer.setProperty("LastKeepAliveEpoch", p.getLastKeepAliveEpoch());
                        return;
                    }
                    else if((long)peer.getProperty("LastKeepAliveEpoch") <= p.getLastKeepAliveEpoch())
                    {
                        torrent.deleteLink("Peers", peer);
                        peer.delete();
                    }
                }
            }
        });
    }

    private TrackerPeer GetTrackerPeerByTorrentIdAndPeerId(String torrentId, String ip, int port)
    {
        return computeInReadOnlyStore( txn -> {
            Entity torrent = txn.find(TRACKERTORRENT_TYPE, "TorrentId", torrentId).getFirst();
            if(torrent != null) {
                for (Entity peer : torrent.getLinks("Peers")) {
                    if (peer.getProperty("IpAddress").equals(ip) && (int)peer.getProperty("Port") == port)
                    {
                     System.out.println("got matching peer");
                        return entityToTrackerPeerFunction.apply(peer);
                    }
                }
            }
            return null;
        });
    }

    public List<TrackerPeer> GetPeersForTorrentByTorrentIdAndFilterRequestingIp(String torrentId, String requestingIp, int requestingPort, long startingTime)
    {

        return computeInReadOnlyStore( txn -> {
            Entity torrent = txn.find(TRACKERTORRENT_TYPE, "TorrentId", torrentId).getFirst();
            if (torrent != null) {
                if(requestingPort > 0)
                    return StreamSupport.stream(torrent.getLinks("Peers").spliterator(), false).map(entityToTrackerPeerFunction).filter(trackerPeer -> (trackerPeer.getLastKeepAliveEpoch() >= startingTime && !(trackerPeer.getIpAddress() + trackerPeer.getPort()).equals(requestingIp + requestingPort))).collect(Collectors.toList());
                else
                    return StreamSupport.stream(torrent.getLinks("Peers").spliterator(), false).map(entityToTrackerPeerFunction).collect(Collectors.toList());

            }
            return null;
        });
    }

    public void InsertTrackerTorrent(TrackerTorrent t){

        executeInStore( txn -> {
            Entity newTorrent = txn.newEntity(TRACKERTORRENT_TYPE);
            newTorrent.setProperty("TorrentId", t.getTorrentId());
            for(TrackerPeer peer : t.getPeers())
            {
                Entity p = txn.newEntity(TRACKERPEER_TYPE);
                p.setProperty("IpAddress", peer.getIpAddress());
                p.setProperty("CreateDateTime", peer.getCreateDateTime().toString());
                p.setProperty("Port", peer.getPort());
                p.setProperty("LastKeepAliveDateTime", peer.getLastKeepAliveDateTime().toString());
                p.setLink("ParentTorrent", newTorrent);
                newTorrent.addLink("Peers", p);
            }
        });
    }

    //Audit

    public List<TorrentAudit> GetAllTorrentAudits(String torrentid)
    {
        List<TorrentAudit> audits = computeInStore( txn -> {
            return StreamSupport.stream(txn.getAll(TORRENTAUDIT_TYPE).spliterator(), false).map(entityToTorrentAuditFunction).collect(Collectors.toList());

        });
        return audits != null && !audits.isEmpty() ? audits : new ArrayList<>();
    }

    public void UpdateChunkAudit(ChunkAudit audit)
    {
//        executeInStore(txn -> {
//            Entity existing = txn.find(TORRENTAUDIT_TYPE, "", torrentAudit.TorrentId).intersect(txn.find(TORRENTAUDIT_TYPE, "Peers", torrentAudit.NumberOfPeers)).getFirst();
//
//        });
    }

    public void UpdateTorrentAudit(TorrentAudit torrentAudit)
    {
        executeInStore( txn -> {
            Entity existing = txn.find(TORRENTAUDIT_TYPE, "Id", torrentAudit.TorrentId).intersect(txn.find(TORRENTAUDIT_TYPE, "Peers", torrentAudit.NumberOfPeers)).getFirst();
            if(existing == null || (String)existing.getProperty("Id") == null) {
                Entity newAudit = txn.newEntity(TORRENTAUDIT_TYPE);
                newAudit.setProperty("Id", torrentAudit.TorrentId);
                newAudit.setProperty("Epoch", torrentAudit.CreateEpoh);
                newAudit.setProperty("Lookup", torrentAudit.LookupTimeForPeers);
                newAudit.setProperty("Peers", torrentAudit.NumberOfPeers);

                if (torrentAudit.ChunkAudits != null && !torrentAudit.ChunkAudits.isEmpty()) {
                    for (ChunkAudit c : torrentAudit.ChunkAudits) {
                        Entity cAudit = txn.newEntity(CHUNKAUDIT_TYPE);
                        cAudit.setProperty("Start", c.StartReqTime);
                        cAudit.setProperty("End", c.EndReqTime);
                        cAudit.setProperty("Failed", c.ChunkFailed);
                        cAudit.setProperty("Epoch", c.CreateEpoch);
                        cAudit.setProperty("Id", c.TorrentId);
                        newAudit.addLink("ChunkAudits", cAudit);
                        cAudit.addLink("Parent", newAudit);
                    }
                }
            } else {
                existing.setProperty("Epoch", torrentAudit.CreateEpoh);
                existing.setProperty("Lookup", torrentAudit.LookupTimeForPeers);
                if (torrentAudit.ChunkAudits != null && !torrentAudit.ChunkAudits.isEmpty()) {
                    for (Entity oldc : existing.getLinks("ChunkAudits"))
                    {
                        existing.deleteLink("ChunkAudits", oldc);
                        oldc.delete();
                    }

                    for (ChunkAudit c : torrentAudit.ChunkAudits) {
                        Entity cAudit = txn.newEntity(CHUNKAUDIT_TYPE);
                        cAudit.setProperty("Start", c.StartReqTime);
                        cAudit.setProperty("End", c.EndReqTime);
                        cAudit.setProperty("Failed", c.ChunkFailed);
                        cAudit.setProperty("Epoch", c.CreateEpoch);
                        cAudit.setProperty("Id", c.TorrentId);
                        existing.addLink("ChunkAudits", cAudit);
                        cAudit.addLink("Parent", existing);
                    }
                }


            }

        });

    }


    private static Function<Entity, TorrentAudit> entityToTorrentAuditFunction = new Function<Entity, TorrentAudit>() {
        @Override
        public TorrentAudit apply(Entity entity) {

            if (entity == null || entity.getProperty("TorrentId") == null)
                return null;

            TorrentAudit audit = new TorrentAudit();

            List<ChunkAudit> chunkAudits = new ArrayList<>();
            if (entity.getLinks("ChunkAudits") != null)
                for (Entity chunkAudit : entity.getLinks("ChunkAudits")) {
                    ChunkAudit cAudit = new ChunkAudit();
                    cAudit.StartReqTime = (long) chunkAudit.getProperty("Start");
                    cAudit.EndReqTime = (long) chunkAudit.getProperty("End");
                    cAudit.ChunkFailed = (boolean) chunkAudit.getProperty("Failed");
                    cAudit.CreateEpoch = (long) chunkAudit.getProperty("Epoch");
                    cAudit.TorrentId = (String) chunkAudit.getProperty("Id");

                    chunkAudits.add(cAudit);
                }

            audit.ChunkAudits = chunkAudits;
            audit.CreateEpoh = (long) entity.getProperty("Epoch");
            audit.LookupTimeForPeers = (long) entity.getProperty("Lookup");
            audit.NumberOfPeers = (int) entity.getProperty("Peers");
            audit.TorrentId = (String) entity.getProperty("Id");

            return audit;
        }
    };

    //End Audit section


    public ClientConfig GetClientConfig(){
        return computeInReadOnlyStore( txn -> {
           EntityIterable it = txn.getAll(CLIENTCONFIG_TYPE);
           if(it.isEmpty())
               return null;
           Entity c = it.getFirst();

           return new ClientConfig(
                  c.getProperty("botMode") != null ?(boolean) c.getProperty("botMode") : false,
                   c.getProperty("sleep") != null ?(int) c.getProperty("sleep") : 0,
                   c.getProperty("port") != null ?(int) c.getProperty("port") : 0,
                   c.getProperty("type") != null ?ClientConfig.ClientInstanceType.getTypeFromString((String) c.getProperty("type")) : ClientConfig.ClientInstanceType.PEER
           );



        });
    }

    public void SaveClientConfig(ClientConfig config){
        executeInStore( txn -> {
            Entity c;
            if(txn.getAll(CLIENTCONFIG_TYPE).isEmpty())
                c = txn.newEntity(CLIENTCONFIG_TYPE);
            else
                c = txn.getAll(CLIENTCONFIG_TYPE).getFirst();
            c.setProperty("botMode", config.isInBotMode());
            c.setProperty("sleep", config.getBotSleepInSeconds());
            c.setProperty("port", config.getPreferredPort());
            c.setProperty("type", config.getType().toString());
        });
    }


    private static Function<Entity, TrackerPeer> entityToTrackerPeerFunction = new Function<Entity, TrackerPeer>() {
        @Override
        public TrackerPeer apply(Entity entity) {
            return new TrackerPeer(
                    (String) entity.getProperty("IpAddress"),
                    (int) entity.getProperty("Port"),
                    LocalDateTime.parse((String) entity.getProperty("CreateDateTime")),
                    LocalDateTime.parse((String) entity.getProperty("LastKeepAliveDateTime")),
                    (long) entity.getProperty("LastKeepAliveEpoch"));
        }
    };

    private static Function<Entity, TrackerTorrent> entityToTrackerTorrentFunction = new Function<Entity, TrackerTorrent>() {
        @Override
        public TrackerTorrent apply(Entity entity) {
            return new TrackerTorrent(
                    (String) entity.getProperty("TorrentId"),
                    StreamSupport.stream(entity.getLinks(TRACKERPEER_TYPE).spliterator(), false)
                            .map(entityToTrackerPeerFunction).collect(Collectors.toList())
            );
        }
    };


    //helpers

    /**
     * Clears the entire database
     */
    public void ClearAllData(){
        store.close();
    }



    private <T> T computeInStore(Function<StoreTransaction, T> compute)
    {
        return store.computeInTransaction(new StoreTransactionalComputable<T>() {
            @Override
            public T compute(StoreTransaction txn) {
                return compute.apply(txn);
            }
        });
    }

    private <T> T computeInReadOnlyStore(Function<StoreTransaction, T> compute)
    {
        return store.computeInReadonlyTransaction(new StoreTransactionalComputable<T>() {
            @Override
            public T compute(StoreTransaction txn) {
                return compute.apply(txn);
            }
        });
    }

    private void executeInStore(Consumer<StoreTransaction> execute)
    {
        store.executeInTransaction(new StoreTransactionalExecutable() {
            @Override
            public void execute(@NotNull StoreTransaction txn) {
                execute.accept(txn);
            }
        });
    }



    /**
     * Returns the PersistentEntityStore
     * @see PersistentEntityStore
     * @see PersistentEntityStores
     * @return PersistentEntityStore of database located at PERSISTENT_ENTITY_STORE_LOCATION
     */
    private static PersistentEntityStore GetPersistentEntityStore(){
        return PersistentEntityStores.newInstance(PERSISTENT_ENTITY_STORE_LOCATION);
    }


    /**
     * Used to close the entitystore and environment
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        boolean proceed = true;
        int count = 0;
        while (proceed && count < 10) {
            try {
                System.out.println("trying to close persistent store. attempt " + count);
                if(store != null)
                    store.close();
                if(environment != null)
                    environment.close();
                proceed = false;
                store = null;
                environment = null;
                instance = null;
                System.err.println("persistent store closed");
            } catch (RuntimeException e) {
                System.err.println("error closing persistent store" + e.toString());
                count++;
            }
        }
    }
}
