package services;

import data.ClientTorrent;
import data.FileStatus;
import data.PeerInfo;
import data.TorrentCache;
import javafx.application.Platform;
import lombok.Synchronized;

import java.io.IOException;

/**
 * Created by cakor on 11/27/2016.
 */
public class LeecherService {
    private static boolean isLeeching=false;

    public static void startLeeching()
    {

        setLeeching(true);
        ClientTorrent currentTorrent;
        int chunkNumber=0;
        PeerInfo seeder;

        System.out.println("Starting Leecher Service");
        System.out.println("incomplete list size: " + TorrentCache.getIncompleteList().size());
        while(TorrentCache.getIncompleteList().size()>0)
        {
            for (int i=0; i<TorrentCache.getIncompleteList().size();i++)
            {
                currentTorrent=TorrentCache.getIncompleteList().get(i);
                chunkNumber=currentTorrent.getRandomIncompleteChunk();

                if(chunkNumber<0)
                {
                    TorrentCache.getIncompleteList().get(i).setStatus(FileStatus.COMPLETED);
                    TorrentCache.getIncompleteList().remove(i);
                }
                else {
                    seeder=currentTorrent.getRandomPeer();
                    if(ClientRequestSynchronizer.pingPeer(seeder.IpAddress, String.valueOf(seeder.Port)))
                        ClientRequestSynchronizer.request(seeder.IpAddress,String.valueOf(seeder.Port),currentTorrent.getTorrentId(),chunkNumber);
                }
                //useful for debugging
//                try {
//                    Thread.sleep(500);
//                } catch (InterruptedException ex)
//                {
//                    ex.printStackTrace();
//                }
            }
        }
        System.out.println("Leaching Complete");
        setLeeching(false);
    }
    @Synchronized
    private static void setLeeching(boolean b)
    {
        isLeeching=b;
    }
    @Synchronized
    public static boolean isLeeching()
    {
        return isLeeching;
    }
}
