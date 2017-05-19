package data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Stephen on 12/5/16.
 */
public class TorrentAudit {
    public String TorrentId;
    public long CreateEpoh;
    public int NumberOfPeers;
    public long LookupTimeForPeers;
    public List<ChunkAudit> ChunkAudits = new ArrayList<>();
}
