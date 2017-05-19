package data;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by Stephen on 12/5/16.
 */
public class ChunkAudit {
    public String TorrentId;
    public long StartReqTime;
    public long EndReqTime;
    public long CreateEpoch;
    public boolean ChunkFailed;
    public int ChunkNum;
    @JsonIgnore
    public long getTransferTime(){return EndReqTime - StartReqTime;}
}
