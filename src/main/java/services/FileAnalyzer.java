package services;

import data.FileInfo;

import java.io.File;

/**
 * Created by cakor on 11/14/2016.
 */
public class FileAnalyzer {

    public static FileInfo getInfo(File f)
    {
        FileInfo info;
        long length=f.length();
        String name=f.getName();
        String path=f.getAbsolutePath();
        boolean isChunkTooSmall=true;
        int chunkSize=0;
        int chunks=0;
        while(isChunkTooSmall)
        {
           chunkSize +=16384;
            if((Math.ceil((float)length/(float)chunkSize))< 50000)
            {
                chunks=(int)(Math.ceil((float)length/(float)chunkSize));
                isChunkTooSmall=false;
            }
        }
        info=new FileInfo(name,length,chunks,chunkSize, path);
        return info;
    }

}
