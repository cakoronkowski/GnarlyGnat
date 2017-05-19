package data;

import helpers.DateHelper;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Created by Stephen on 11/28/16.
 */
@Data
public class TrackerPeer {

    public TrackerPeer(){}

    public TrackerPeer(String ipAddress, int port) {
        IpAddress = ipAddress;
        Port = port;
        CreateDateTime = LocalDateTime.now();
        LastKeepAliveDateTime = LocalDateTime.now();
        LastKeepAliveEpoch = LastKeepAliveDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

    }

    public TrackerPeer(String ipAddress, int port, LocalDateTime create, LocalDateTime last, long epoch){
        IpAddress = ipAddress;
        Port = port;
        CreateDateTime = create;
        LastKeepAliveDateTime = last;
        if(epoch > 0)
            LastKeepAliveEpoch = epoch;
        else
            LastKeepAliveEpoch = LastKeepAliveDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private String IpAddress;
    private int Port;
    private LocalDateTime CreateDateTime;
    private LocalDateTime LastKeepAliveDateTime;
    private long LastKeepAliveEpoch;


    public long getLastKeepAliveEpoch(){
        return LastKeepAliveEpoch;
    }

    public String getIpAddress() {
        return IpAddress;
    }

    public void setIpAddress(String ipAddress) {
        IpAddress = ipAddress;
    }

    public int getPort() {
        return Port;
    }

    public void setPort(int port) {
        Port = port;
    }

    public LocalDateTime getCreateDateTime() {
        return CreateDateTime;
    }

    public void setCreateDateTime(LocalDateTime createDateTime) {
        CreateDateTime = createDateTime;
    }

    public LocalDateTime getLastKeepAliveDateTime() {
        return LastKeepAliveDateTime;
    }

    public void setLastKeepAliveDateTime(LocalDateTime lastKeepAliveDateTime) {
        LastKeepAliveDateTime = lastKeepAliveDateTime;
    }

}
