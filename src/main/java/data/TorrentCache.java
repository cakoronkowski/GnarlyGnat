package data;

import lombok.Synchronized;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by cakor on 11/27/2016.
 */
public class TorrentCache {
    static HashMap<String, ClientTorrent> hashTorrents= new HashMap<String, ClientTorrent>();
    static List<ClientTorrent> incomplete =new ArrayList<ClientTorrent>();
    @Synchronized
    public static void addIncomplete(ClientTorrent t)
    {
        incomplete.add(t);
    }
    @Synchronized
    public static void addTorrent(ClientTorrent t)
    {
        hashTorrents.put(t.getTorrentId(),t);
    }
    @Synchronized
    public static ClientTorrent getTorrentById(String id)
    {
        return hashTorrents.get(id);
    }
    @Synchronized
    public static ClientTorrent getIncompleteById(String id)
    {
        for(int i=0; i<incomplete.size();i++)
        {
            if(incomplete.get(i).equals(id))
            {
                return incomplete.get(i);
            }
        }
        return null;
    }
    @Synchronized
    public static List<ClientTorrent> getIncompleteList()
    {
        return incomplete;
    }
    @Synchronized
    public static HashMap<String, ClientTorrent> getHashTorrents()
    {
        return hashTorrents;
    }

    public static long lookup;
}
