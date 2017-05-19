package data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import data.IValidable;
import lombok.Data;

import java.util.List;

/**
 * Created by Stephen on 11/14/16.
 */




@Data
public class NewTorrentFilePayload implements IValidable {

    public NewTorrentFilePayload(){}

    public NewTorrentFilePayload(data.FileInfo fileInfo, String trackerIp, String torrentId, String[] hashList) {
        FileInfo = fileInfo;
        TrackerIp = trackerIp;
        TorrentId = torrentId;
        HashList=hashList;
    }

    public data.FileInfo getFileInfo() {
        return FileInfo;
    }

    public void setFileInfo(data.FileInfo fileInfo) {
        FileInfo = fileInfo;
    }

    public String getTrackerIp() {
        return TrackerIp;
    }

    public void setTrackerIp(String trackerIp) {
        TrackerIp = trackerIp;
    }

    private FileInfo FileInfo;
    private String TrackerIp;
    private String[] HashList;

    public String[] getHashList()
    {
        return HashList;
    }
    public String getTorrentId() {
        return TorrentId;
    }

    public void setTorrentId(String torrentId) {
        TorrentId = torrentId;
    }

    private String TorrentId;


    @Override
    @JsonIgnore
    public boolean isValid() {
        return TrackerIp != null && !TrackerIp.isEmpty() && FileInfo != null
                && !FileInfo.getFileName().isEmpty();
    }
}
