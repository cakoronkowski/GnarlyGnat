package data;


import controllers.ClientTorrentDetailController;
import helpers.CryptoHelper;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.shape.Rectangle;
import services.FileManager;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Stephen on 11/19/16.
 */
public class ClientTorrent extends BaseTorrent {
    private List<Integer> incompleteChunks =new ArrayList<Integer>();
    //new Torrent
    Random rand;
    public ClientTorrent(List<String> trackers, List<String> seeders) {

        Trackers = trackers;
        rand =new Random();

        Status = FileStatus.READY;
        _Progress = new SimpleDoubleProperty(0.0);

        ChunkStatuses = FXCollections.observableArrayList();
        _PeerString = new SimpleStringProperty();
        _StatusString = new SimpleStringProperty();
    }

    //from db
    public ClientTorrent(List<String> trackers, List<String> seeders, FileStatus status, long fileSize) {
        Trackers = trackers;
        rand =new Random();
        Status = status;
        _Progress = new SimpleDoubleProperty(0.0);

        ChunkStatuses = FXCollections.observableArrayList();
        _PeerString = new SimpleStringProperty();
        _StatusString = new SimpleStringProperty();
        //FileSize = fileSize;
    }


    public ClientTorrent(FileInfo fileInfo, FileManager fileManager,
                         FileStatus status, List<String> trackers,
                         List<PeerInfo> peers, boolean is_complete,
                         String torrentId, LocalDateTime createDate
                         )
    {
        ChunkStatuses = FXCollections.observableArrayList();
        Peers = FXCollections.observableArrayList();
        rand =new Random();
        FileInfo = fileInfo;
        FileManager = fileManager;
        HashList = new String[fileInfo.getNumberOfChunks()];
        for(int i = 0; i < fileInfo.getNumberOfChunks(); i++)
        {
            byte[] b = new byte[fileInfo.getChunkSize()];
            try {
                fileManager.getChunk(b, i);
                HashList[i] = CryptoHelper.generate(b);
            }catch (Exception e)
            {
                System.err.println(e);
            }
            if(fileManager.getChunkStatus(i)!= ChunkStatus.COMPLETE)
            {
                incompleteChunks.add(i);
            }
            ChunkStatuses.add(i,fileManager.getChunkStatus(i));
            if(fileManager.getChunkStatus(i) == ChunkStatus.COMPLETE)
                numberOfCompleteChunks++;


        }

        Trackers = trackers;
        Status = status;
//        if(chunkRects == null)
//            chunkRects = new ArrayList<>();
//
//        Platform.runLater( () -> {
//            for(int i = 0; i < ChunkStatuses.size(); i++)
//            {
//                Rectangle r = new Rectangle()
//                chunkRects.a.setFill(ClientTorrentDetailController.getStatusPaint(ChunkStatuses.get(i)));
//            }
//        });
        if (peers != null)
            Peers.setAll(peers);
        else
        {
            setStatus(FileStatus.STALLED);
        }
        isComplete = is_complete;
        TorrentId = torrentId;
        CreateDateTime = createDate;
        _PeerString = new SimpleStringProperty();
        _StatusString = new SimpleStringProperty();
        setStatus(status);
        _PeerString.setValue(String.valueOf(peers.size()));
        _Progress = new SimpleDoubleProperty(0.0);
        System.out.println("Number of chunks: " + ChunkStatuses.size());


        Peers.addListener(new ListChangeListener<PeerInfo>() {
            @Override
            public void onChanged(Change<? extends PeerInfo> c) {
                if(c.wasAdded() || c.wasRemoved())
                {
                    _PeerString.setValue(String.valueOf(Peers.size()));
                }
            }
        });

        if(isComplete)
        {
            _Progress.setValue(1);
        }
    }

    public ClientTorrent(){
        Status = FileStatus.READY;
        _Progress = new SimpleDoubleProperty(0.0);
        ChunkStatuses = FXCollections.observableArrayList();
        _PeerString = new SimpleStringProperty();
        _StatusString = new SimpleStringProperty();
        rand =new Random();
    }
    public ClientTorrent(FileInfo fileInfo, FileManager fileManager,
                         FileStatus status, List<String> trackers,
                         List<PeerInfo> peers, boolean is_complete,
                         String torrentId, LocalDateTime createDate, String[] hashList
    )
    {
        ChunkStatuses = FXCollections.observableArrayList();
        Peers = FXCollections.observableArrayList();
        HashList=hashList;
        FileInfo = fileInfo;
        FileManager = fileManager;
        rand =new Random();
        for(int i = 0; i < fileInfo.getNumberOfChunks(); i++)
        {
            if(fileManager.getChunkStatus(i)!= ChunkStatus.COMPLETE)
            {
                incompleteChunks.add(i);
            }
            ChunkStatuses.add(i,fileManager.getChunkStatus(i));

            if(fileManager.getChunkStatus(i) == ChunkStatus.COMPLETE)
                numberOfCompleteChunks++;


        }
//        if(chunkRects == null)
//            chunkRects = new ArrayList<>();
//
//        Platform.runLater( () -> {
//            for(int i = 0; i < ChunkStatuses.size(); i++)
//            {
//                chunkRects.get(i).setFill(ClientTorrentDetailController.getStatusPaint(ChunkStatuses.get(i)));
//            }
//        });


        Trackers = trackers;
        Status = status;
        if (peers != null)
            Peers.setAll(peers);
        else
        {
            setStatus(FileStatus.STALLED);
        }
        isComplete = is_complete;
        TorrentId = torrentId;
        CreateDateTime = createDate;
        _PeerString = new SimpleStringProperty();
        _StatusString = new SimpleStringProperty();
        setStatus(status);
        _PeerString.setValue(String.valueOf(peers.size()));
        _Progress = new SimpleDoubleProperty(0.0);


        Peers.addListener(new ListChangeListener<PeerInfo>() {
            @Override
            public void onChanged(Change<? extends PeerInfo> c) {
                while (c.next()) {
                    if (c.wasAdded() || c.wasRemoved() ||c.wasUpdated())

                    {
                        _PeerString.setValue(String.valueOf(Peers.size()));
                    }
                }
            }
        });

    }

    public TorrentAudit Audit = new TorrentAudit();


    /**
     * List of trackers, most likely only one will be used unless the seeders provided by the Tracker are useless.
     */
    private List<String> Trackers;

    /**
     * Observable list of Peers for the peer table
     */
    private ObservableList<PeerInfo> Peers;

    /**
     * used to shortcut the chunkbar and progress bar, can also be used for indicator
     */
    private boolean isComplete = false;

    /**
     *  status of file: ready, downloading,...
     */
    private FileStatus Status;

    /**
     * contains info like filesize, absolute location
     */
    private FileInfo FileInfo;

    /**
     * manages reading/writing of physical file
     */
    private FileManager FileManager;

    /**
     * bound to UI for the overall progresbar of the downloaded torrent
     */
    private DoubleProperty _Progress;

    /**
     * list of chunk statuses for chunk bar
     */
    private ObservableList<ChunkStatus> ChunkStatuses;
    private StringProperty _StatusString;
    private StringProperty _PeerString;
    private int numberOfCompleteChunks = 0;
    List<Rectangle> chunkRects;


    public String[] getHashList() {
        return HashList;
    }

    public void setHashList(String[] hashList) {
        HashList = hashList;
    }

    private String[] HashList;

    public void setChunkRects(List<Rectangle> rects)
    {
        chunkRects = rects;
    }

    public List<Rectangle> getChunkRects(){return chunkRects;}

    public ObservableList<PeerInfo> getPeers() {
        return Peers;
    }

    public void setPeers(ObservableList<PeerInfo> peers) {
        Peers = peers;
    }

    public void setPeers(List<PeerInfo> peers) {
        Peers.setAll(peers);
    }

    public ObservableList<ChunkStatus> getChunkStatuses() {
        return ChunkStatuses;
    }

    public void setChunkStatuses(ObservableList<ChunkStatus> chunkStatues) {
        ChunkStatuses = chunkStatues;
    }

    public int getRandomIncompleteChunk()
    {
        if(incompleteChunks.size() == 0)
            return -1;


       // return incompleteChunks.remove(0);
        //Random rand =new Random();
        int next = rand.nextInt(incompleteChunks.size());

        if(ChunkStatuses.get(next) != null && incompleteChunks.size() > 0)
            setChunkStatus(incompleteChunks.get(0), ChunkStatus.IN_PROGRESS);

        return (incompleteChunks.size()) <= 0 ? -1 : incompleteChunks.remove(next);
    }

    public void addIncompleteChunk(Integer i) {
        setChunkStatus(i, ChunkStatus.EMPTY);
        incompleteChunks.add(i);
    }

    public void setChunkStatus(int i, ChunkStatus status) {

        if (status == ChunkStatus.COMPLETE && ChunkStatuses.get(i) != ChunkStatus.COMPLETE)
            numberOfCompleteChunks++;
        else if (status != ChunkStatus.COMPLETE && ChunkStatuses.get(i) == ChunkStatus.COMPLETE)
            numberOfCompleteChunks--;

        if(i > ChunkStatuses.size())
            ChunkStatuses.add(i, status);
        else
        {
            ChunkStatuses.set(i, status);
        }

        FileManager.setChunkStatus(ChunkStatus.COMPLETE, i);

        _Progress.set((double) numberOfCompleteChunks / ChunkStatuses.size());

        Platform.runLater( () -> {
            if(chunkRects!=null &&  i<(chunkRects.size()))
            chunkRects.get(i).setFill(ClientTorrentDetailController.getStatusPaint(status));
        });
    }

    public PeerInfo getRandomPeer()
    {
       // Random rand =new Random();
        if(Peers.size() == 0)
        {
            return null;
        }
        int nextPeerIndex = rand.nextInt(Peers.size());
       // System.out.println("Next Peer for Leeching:" + nextPeerIndex);
        return Peers.get(nextPeerIndex);
    }
    public String getFileSizeString(){
        if(FileInfo == null)
        {
            return "File info does not exists";
        }

        double val = FileInfo.getLength();
        String type = "Bytes";
        DecimalFormat df = new DecimalFormat("###.##");
        if(withinRange((double)FileInfo.getLength() / 1024.0)  )
        {
            val = (double)FileInfo.getLength() / 1024.0;
            type = "KB";

        }
        else if(withinRange((double)FileInfo.getLength() / 1048576.0))
        {
            val = (double)FileInfo.getLength() / 1048576.0;
            type = "MB";
        }
        else if(withinRange((double)FileInfo.getLength() / 1073741824.0))
        {
            val = (double)FileInfo.getLength() / 1073741824.0;
            type = "GB";
        }
        else if(withinRange((double)FileInfo.getLength() / 1099511627776.0))
        {
            val = (double)FileInfo.getLength() / 1099511627776.0;
            type = "TB";
        }
        return df.format(val) + " " + type;
    }

    public String getLocation() {
        return FileInfo != null ? FileInfo.absolutePath : "";
    }

    public String getFileStatusString(){
        if(Status != null)
       switch (Status)
       {
           case COMPLETED:
               return "Completed";
           case DOWNLOADING:
               return "Downloading";
           case READY:
               return "Ready";
           case STALLED:
               return "Stalled";
       }

       return "UNKNOWN";

    }

    public DoubleProperty getProgressProperty(){
        return _Progress;
    }

    public List<String> getTrackers() {
        return Trackers;
    }

    public void setTrackers(List<String> trackers) {
        Trackers = trackers;
    }

    public FileStatus getStatus() {
        return Status;
    }

    public void setStatus(FileStatus status) {
        _StatusString.setValue(status.toString());
        Status = status;
        if(status == FileStatus.COMPLETED && !FileManager.isSeedMode())
        {
            System.out.println("Status is complete, closing file");
            FileManager.closeFile();

        }
    }

    public StringProperty getPeerString(){return _PeerString;}

    public StringProperty getStatusString(){return _StatusString;}

    public boolean getIsComplete(){return isComplete;}

    public FileManager getFileManager(){return FileManager;}

    public FileInfo getFileInfo(){return FileInfo;}

    public String getFileName(){
        return FileInfo != null ? FileInfo.getFileName() : "";
    }
    //helpers
    private boolean withinRange(double size)
    {
        return (size < 1000 && size > 1);
    }

}
