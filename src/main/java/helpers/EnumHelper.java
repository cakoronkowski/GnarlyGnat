package helpers;

import data.FileStatus;

/**
 * Created by Stephen on 11/30/16.
 */
public class EnumHelper {

    public static FileStatus getFileStatusFromString(String s)
    {
        if(s.equals(FileStatus.COMPLETED.toString()))
        {
            return FileStatus.COMPLETED;
        }
        else if(s.equals(FileStatus.READY.toString()))
        {
            return FileStatus.READY;
        }
        else if(s.equals(FileStatus.DOWNLOADING.toString()))
        {
            return FileStatus.DOWNLOADING;
        }
        else
        {
            return FileStatus.STALLED;
        }

    }
}
