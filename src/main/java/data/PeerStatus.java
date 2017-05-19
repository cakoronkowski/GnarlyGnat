package data;

/**
 * Created by Stephen on 11/27/16.
 */
public enum PeerStatus {
    Seeder, Leecher;
    public static PeerStatus fromString(String s)
    {
        return Seeder;
    }
}
