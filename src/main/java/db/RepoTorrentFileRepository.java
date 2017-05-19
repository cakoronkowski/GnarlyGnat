package db;

import jetbrains.exodus.entitystore.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Created by Stephen on 11/18/16.
 */
public class RepoTorrentFileRepository {

    //props
//    private static final String PERSISTENT_ENTITY_STORE_LOCATION = "data";
//    private static final String TORRENTFILE_TYPE = "data.RepoTorrentFile";
//
//    /**
//     * Return a list of all torrent files in database.
//     *
//     * @see data.RepoTorrentFile
//     * @see List
//     * @return List of data.RepoTorrentFile
//     */
//    public static List<data.RepoTorrentFile> GetAllTorrentFiles(){
//        PersistentEntityStore entityStore = GetPersistentEntityStore();
//
//        List<data.RepoTorrentFile> results = entityStore.computeInReadonlyTransaction(new StoreTransactionalComputable<List<data.RepoTorrentFile>>() {
//            @Override
//            public List<data.RepoTorrentFile> compute(@NotNull StoreTransaction txn) {
//                return StreamSupport.stream(txn.getAll(TORRENTFILE_TYPE).spliterator(), false).map(entityToTorrentFileFunction).collect(Collectors.toList());
//            }
//        });
//        entityStore.close();
//        return results;
//    }
//
//
//    /**
//     * Returns the data.RepoTorrentFile with the id, null if not found
//     *
//     * @see data.RepoTorrentFile
//     * @param id TorrentId of the file
//     * @return data.RepoTorrentFile where TorrentId == id if none, returns null
//     */
//    public static data.RepoTorrentFile GetTorrentFileById(String id){
//        PersistentEntityStore entityStore = GetPersistentEntityStore();
//
//        data.RepoTorrentFile result = entityStore.computeInReadonlyTransaction(new StoreTransactionalComputable<data.RepoTorrentFile>() {
//            @Override
//            public data.RepoTorrentFile compute(@NotNull StoreTransaction txn) {
//                Entity file = txn.find(TORRENTFILE_TYPE, "TorrentId", id).getFirst();
//                if(file != null)
//                    return entityToTorrentFileFunction.apply(file);
//                return null;
//            }
//        });
//        entityStore.close();
//        return result;
//    }
//
//    /**
//     * Returns List of data.RepoTorrentFile where data.RepoTorrentFile.FileName starts with @name
//     *
//     * @see data.RepoTorrentFile
//     * @see List
//     * @param name Name of the data.RepoTorrentFile
//     * @return List of TorrentFiles where FileName starts with @name
//     */
//    public static List<data.RepoTorrentFile> GetTorrentFilesWithName(String name)
//    {
//        PersistentEntityStore entityStore = GetPersistentEntityStore();
//        List<data.RepoTorrentFile> result = entityStore.computeInReadonlyTransaction(new StoreTransactionalComputable<List<data.RepoTorrentFile>>() {
//            @Override
//            public List<data.RepoTorrentFile> compute(@NotNull StoreTransaction txn) {
//                return StreamSupport.stream(txn.findStartingWith(TORRENTFILE_TYPE, "FileName", name).spliterator(), false).map(entityToTorrentFileFunction).collect(Collectors.toList());
//            }
//        });
//        entityStore.close();
//        return result;
//    }
//
//
//    /**
//     * Function that maps one Entity to data.RepoTorrentFile
//     * @see data.RepoTorrentFile
//     * @see Entity
//     */
//    private static Function<Entity, data.RepoTorrentFile> entityToTorrentFileFunction = new Function<Entity, data.RepoTorrentFile>() {
//        @Override
//        public data.RepoTorrentFile apply(Entity entity) {
//            data.RepoTorrentFile torrent = new data.RepoTorrentFile();
//            if(entity.getProperty("ChunkSize") != null)
//                torrent.setChunkSize((int)entity.getProperty("ChunkSize"));
//            if(entity.getProperty("FileName") != null)
//                torrent.setFileName((String)entity.getProperty("FileName"));
//            if(entity.getProperty("FileSize") != null)
//                torrent.setFileSize((long)entity.getProperty("FileSize"));
//            if(entity.getProperty("TorrentId") != null)
//                torrent.setTorrentId((String)entity.getProperty("TorrentId"));
//            if(entity.getProperty("Id") != null)
//                torrent.setId((EntityId) entity.getProperty("Id"));
//            else
//                torrent.setId(entity.getId());
//
//            return torrent;
//        }
//    };
//
//
//    /**
//     * Inserts data.RepoTorrentFile and returns the EntityId for the inserted file
//     * @see data.RepoTorrentFile
//     * @see Entity
//     * @param file data.RepoTorrentFile to be inserted into the database
//     * @return EntityId of the file, doesn't really do anything because all searching is done with TorrentId
//     */
//    public static EntityId InsertTorrentFile(data.RepoTorrentFile file){
//
//        PersistentEntityStore entityStore = GetPersistentEntityStore();
//
//        EntityId id = entityStore.computeInTransaction(new StoreTransactionalComputable<EntityId>() {
//            @Override
//            public EntityId compute(@NotNull StoreTransaction txn) {
//                final Entity torrentFile = txn.newEntity(TORRENTFILE_TYPE);
//
//                torrentFile.setProperty("FileName", file.getFileName());
//                torrentFile.setProperty("FileSize", file.getFileSize());
//                torrentFile.setProperty("ChunkSize", file.getChunkSize());
//                torrentFile.setProperty("TorrentId", file.getTorrentId());
//                return torrentFile.getId();
//            }
//        });
//
//        entityStore.close();
//
//        return id;
//    }
//
//
//    /**
//     * Updates the data.RepoTorrentFile with file where TorrentId == file.TorrentId
//     * @see data.RepoTorrentFile
//     * @param file The data.RepoTorrentFile to be updated
//     */
//    public static void UpdateTorrentFile(data.RepoTorrentFile file){
//
//        PersistentEntityStore entityStore = GetPersistentEntityStore();
//
//        entityStore.executeInTransaction(new StoreTransactionalExecutable() {
//            @Override
//            public void execute(@NotNull StoreTransaction txn) {
//                final Entity torrentFile = txn.find(TORRENTFILE_TYPE, "TorrentId", file.getTorrentId()).getFirst();
//
//                torrentFile.setProperty("FileName", file.getFileName());
//                torrentFile.setProperty("FileSize", file.getFileSize());
//                torrentFile.setProperty("ChunkSize", file.getChunkSize());
//            }
//        });
//
//        entityStore.close();
//    }
//
//
//    public static void UpsertTorrentFile(data.RepoTorrentFile file)
//    {
//        data.RepoTorrentFile t = GetTorrentFileById(file.getTorrentId());
//        if(t == null)
//        {
//            InsertTorrentFile(file);
//        }
//        else
//        {
//            UpdateTorrentFile(file);
//        }
//    }
//    //helpers
//
//    /**
//     * Clears the entire database
//     */
//    public static void ClearAllData(){
//        PersistentEntityStore entityStore = GetPersistentEntityStore();
//        entityStore.clear();
//        entityStore.close();
//    }
//
//    /**
//     * Returns the PersistentEntityStore
//     * @see PersistentEntityStore
//     * @see PersistentEntityStores
//     * @return PersistentEntityStore of database located at PERSISTENT_ENTITY_STORE_LOCATION
//     */
//    private static PersistentEntityStore GetPersistentEntityStore(){
//        return PersistentEntityStores.newInstance(PERSISTENT_ENTITY_STORE_LOCATION);
//    }
//


}
