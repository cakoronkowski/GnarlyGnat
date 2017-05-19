package data;

import javafx.beans.property.*;
import jetbrains.exodus.entitystore.EntityId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Created by Stephen on 11/19/16.
 */
public class BaseTorrent {
    public BaseTorrent(){
        TorrentId = UUID.randomUUID().toString();
        //ChunkSize = 4000;
        _FileName = new SimpleStringProperty();
       // _ChunkSize = new SimpleIntegerProperty(ChunkSize);
        _TorrentId = new SimpleStringProperty(TorrentId);
        _FileSize = new SimpleLongProperty();
        CreateDateTime = LocalDateTime.now();

    }



    protected String TorrentId;
//    protected String FileName;
//    protected int ChunkSize;
//    protected long FileSize;
    protected LocalDateTime CreateDateTime;


    private StringProperty _FileName;
    private StringProperty _TorrentId;
    private LongProperty _FileSize;
    private StringProperty _CreateDateTime;


    public void updateCreateDateTime(LocalDateTime datetime){
        CreateDateTime = datetime;
        _CreateDateTime.setValue(datetime.toString());
    }

    public StringProperty getCreateDateTimeProperty(){
        return _CreateDateTime;
    }
    public LocalDateTime getCreateDateTime(){
        return CreateDateTime;
    }

    public StringProperty getFileNameProperty(){
        return _FileName;
    }

    public String getTorrentId() {
        return TorrentId;
    }
    public StringProperty getTorrentIdProperty() {
        return _TorrentId;
    }

    public void setTorrentId(String torrentId) {
        _TorrentId.setValue(torrentId);
        TorrentId = torrentId;
    }

//    public String getFileName() {
//        return FileName;
//    }
//
//    public void setFileName(String fileName)
//    {
//        _FileName.setValue(fileName);
//        FileName = fileName;
//    }
//
//    public int getChunkSize() {
//        return ChunkSize;
//    }
//    public IntegerProperty getChunkSizeProperty() {
//        return _ChunkSize;
//    }
//
//    public void setChunkSize(int chunkSize) {
//        _ChunkSize.setValue(chunkSize);
//        ChunkSize = chunkSize;
//    }
//
//    public long getFileSize() {
//        return FileSize;
//    }
//    public LongProperty getFileSizeProperty() {
//        return _FileSize;
//    }
//
//    public void setFileSize(long fileSize) {
//        _FileSize.setValue(fileSize);
//        FileSize = fileSize;
//    }
}
