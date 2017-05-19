package data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Created by Stephen on 11/27/16.
 */
@Data
//@JsonAutoDetect etect(fieldVisibility=JsonAutoDetect.Visibility.ANY, getterVisibility=JsonAutoDetect.Visibility.NONE,setterVisibility=JsonAutoDetect.Visibility.NONE, creatorVisibility=JsonAutoDetect.Visibility.NONE)
public class GetChunkResponse {
    public GetChunkResponse (){}
    public GetChunkResponse(byte[] data, String msg, boolean success, String torrentId, int chunkNo){
        Data = data;
        Message = msg;
        Success = success;
        TorrentId =torrentId;
        ChunkNumber =chunkNo;

    }
    @JsonProperty("Data")
    private byte[] Data;
    @JsonProperty("Message")
    private String Message;
    @JsonProperty("Success")
    private boolean Success;
    @JsonProperty("TorrentId")
    private String TorrentId;
    @JsonProperty("ChunkNumber")
    private int ChunkNumber;
}
