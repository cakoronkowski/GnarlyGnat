package data;

import jetbrains.exodus.entitystore.Entity;
import lombok.Data;
import lombok.Synchronized;
import services.FileManager;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Created by cakor on 11/15/2016.
 */


@Data
public class FileInfo implements Comparable<FileInfo>, Serializable {
    long fileLength;
    String fileId;
    String fileName;
    String absolutePath;
    int numberOfChunks;
    int chunkSize;

    public FileInfo(String name,long length,int chunkCount, int sizeOfChunks, String path)
    {
        fileName=name;
        absolutePath=path;
        fileLength=length;
        numberOfChunks=chunkCount;
        chunkSize=sizeOfChunks;

    }
    public FileInfo(String name,String Id, long length,int chunkCount, int sizeOfChunks)
    {
        fileName=name;
        fileId=Id;
        fileLength=length;
        numberOfChunks=chunkCount;
        chunkSize=sizeOfChunks;

    }
    public FileInfo(String name,String Id, long length,int chunkCount, int sizeOfChunks, String path)
    {
        fileName=name;
        absolutePath=path;
        fileId=Id;
        fileLength=length;
        numberOfChunks=chunkCount;
        chunkSize=sizeOfChunks;

    }
    public FileInfo()
    {

    }
    public String toString()
    {
        String string="";
        string+= "FileName: "+fileName+"\n";
        string+= "FileId: "+fileId+"\n";
        string+= "FileLength: "+fileLength+"\n";
        string+= "FilePath: "+absolutePath+"\n";
        string+= "# of chunks: "+numberOfChunks+"\n";
        string+= "ChunkSize: "+chunkSize;
        return string;

    }
    public long getLength() {
        return fileLength;
    }

    public void setLength(long fileLength) {
        this.fileLength = fileLength;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getNumberOfChunks() {
        return numberOfChunks;
    }

    public void setNumberOfChunks(int numberOfChunks) {
        this.numberOfChunks = numberOfChunks;
    }

    @Synchronized
    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }

    @Override
    public int compareTo(FileInfo o) {
        return fileName.compareTo(o.getFileName());
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    public static Function<Entity, FileInfo> entityToFileManager = new Function<Entity, FileInfo>() {
        @Override
        public FileInfo apply(Entity entity) {
            String name = (String) entity.getProperty("FileName");
            long length = Long.valueOf((String) entity.getProperty("FileLength"));
            int chunkCount = (int) entity.getProperty("ChunkCount");
            int sizeOfChunks = (int) entity.getProperty("SizeOfChunks");
            String path = (String) entity.getProperty("AbsolutePath");
            return new FileInfo(name, length, chunkCount, sizeOfChunks, path);
        }
    };


}
