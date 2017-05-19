package data;

/**
 * Created by Stephen on 11/27/16.
 */
public class PeerInfo {
    public PeerInfo(){}
    public PeerInfo(String ip, int p, PeerStatus stat){
        IpAddress = ip;
        Port = p;
        Status = stat;
    }


    public String IpAddress;
    public int Port;
    public PeerStatus Status;
}
