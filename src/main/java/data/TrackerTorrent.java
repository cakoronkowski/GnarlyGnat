package data;

import java.util.List;

/**
 * Created by Stephen on 11/29/16.
 */
public class TrackerTorrent {
    public TrackerTorrent(String id, List<TrackerPeer> p){
        TorrentId = id;
        Peers = p;
    }
    private String TorrentId;
    private List<TrackerPeer> Peers;

    public String getTorrentId() {
        return TorrentId;
    }

    public void setTorrentId(String torrentId) {
        TorrentId = torrentId;
    }

    public List<TrackerPeer> getPeers() {
        return Peers;
    }

    public void setPeers(List<TrackerPeer> peers) {
        Peers = peers;
    }
}
