package data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jetbrains.exodus.entitystore.Entity;
import jetbrains.exodus.entitystore.EntityId;
import org.apache.commons.lang.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Created by Stephen on 11/4/16.
 */
public class RepoTorrentFile {

    public RepoTorrentFile(){
        TorrentId = UUID.randomUUID().toString();
        ChunkSize = 4000;

    }

    public RepoTorrentFile(FileInfo info, String id, List<String> trackers, String[] hashList){
        Trackers = trackers;
        TorrentId = id;
        ChunkSize = info.getChunkSize();
        FileName = info.getFileName();
        FileSize = info.getLength();
        Date date=new Date();
        regdate=date.getTime();
        HashList=hashList;
    }



    private String TorrentId;
    private String FileName;
    private int ChunkSize;
    private long FileSize;
    private List<String> Trackers;
    private EntityId Id;
    private long regdate;
    private String[] HashList;


//    public LocalDateTime getCreateDateTime(){
//        return LocalDateTime.from(Instant.ofEpochMilli(regdate));
//    }

    public void setCreateDateEpoch(long epoch)
    {
        regdate = epoch;
    }
    @JsonIgnore
    public long getCreateDateTimeEpoch(){
        return regdate;
    }


    public List<String> getTrackers() {
        return Trackers;
    }

    public void setTrackers(List<String> trackers) {
        Trackers = trackers;
    }


    @JsonIgnore
    public EntityId getId() {
        return Id;
    }

    @JsonIgnore
    public void setId(EntityId id) {
        Id = id;
    }

    public String getTorrentId() {
        return TorrentId;
    }

    public void setTorrentId(String torrentId) {
        TorrentId = torrentId;
    }

    public String getFileName() {
        return FileName;
    }

    public void setFileName(String fileName)
    {
        FileName = fileName;
    }

    public int getChunkSize() {
        return ChunkSize;
    }

    public void setChunkSize(int chunkSize) {
        ChunkSize = chunkSize;
    }

    public long getFileSize() {
        return FileSize;
    }

    public void setFileSize(long fileSize) {
        FileSize = fileSize;
    }

    public void setHashList(String[] s)
    {
        HashList=s;
    }

    public String[] getHashList(){return HashList;}

    @JsonIgnore
    public List<String> getFileList(){

        ArrayList<String> list = new ArrayList<String>();

        list.add(getTorrentId());
        list.add(getFileName());
        list.add(String.valueOf(getFileSize()));
        list.add(String.valueOf(getChunkSize()));
        list.add(String.valueOf(Trackers != null ? StringUtils.join(Trackers, ",") : ""));
        String str="";
        for(int i=0;i<HashList.length;i++)
        {
            str+=HashList[i];
            if(i<HashList.length-1)
            {
                str+=",";
            }
        }
        list.add(str);
        return list;
    }

}
