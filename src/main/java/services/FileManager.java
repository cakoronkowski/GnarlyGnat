package services;

import data.ChunkStatus;
import data.RepoTorrentFile;
import jetbrains.exodus.entitystore.Entity;
import jetbrains.exodus.entitystore.EntityId;
import lombok.Getter;
import lombok.Synchronized;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Created by cakor on 11/9/2016.
 */

public class FileManager implements Comparable<FileManager>, Serializable {
    long length;
    long chunk_size;
    String fileName;
    int number_of_chunks;
    File logfile=new File("syncTestLog.txt");
    BufferedWriter writer =new BufferedWriter(new FileWriter(logfile));
    transient RandomAccessFile file;
    public ChunkStatus[] statuslist;
    private boolean isSeedMode;
    private String fileAccessMode;

    public FileManager(long fileLength, long chunkSize, String mode, String fileInputName, ChunkStatus[] statuses, boolean seedMode) throws FileNotFoundException, IOException
    {
        writer.write("START");
        file=new RandomAccessFile(fileInputName, mode);
        chunk_size=chunkSize;
        fileName=fileInputName;
        length=fileLength;
        number_of_chunks=(int)Math.ceil(((double)length/(double)chunk_size));
        if(statuses == null) {
            statuslist = new ChunkStatus[(int) Math.ceil(((double) length / (double) chunk_size))];
            if(!seedMode)
                Arrays.fill(statuslist, ChunkStatus.EMPTY);
            else if(seedMode)
                Arrays.fill(statuslist, ChunkStatus.COMPLETE);
        }
        else
            statuslist = statuses;
        isSeedMode = seedMode;
        fileAccessMode = mode;
        if(mode.equals("rw"))
        {
            file.setLength(fileLength);
        }
    }

    @Synchronized
    public ChunkStatus[] getChunkStatusList(){return statuslist;}

    @Synchronized
    public ChunkStatus getChunkStatus(int chunkIndex)
    {
        return statuslist[chunkIndex];
    }
    @Synchronized
    public boolean hasChunk(int chunkIndex)
    {
        return statuslist != null && chunkIndex < statuslist.length && statuslist[chunkIndex] != null;
    }
    @Synchronized
    public void setChunkStatus(ChunkStatus status, int chunkIndex)
    {
        statuslist[chunkIndex]=status;
    }

    @Synchronized
    public void getChunk(byte[] b, int chunkIndex) throws IOException
    {
        if(file == null || !file.getFD().valid())
            file = new RandomAccessFile(fileName, fileAccessMode);

        if(chunkIndex >= 0 && chunkIndex < number_of_chunks-1)
        {
            file.seek(chunkIndex * chunk_size);
            file.read(b);
            //file.seek(0)
        }
        else if(chunkIndex  >= 0 && chunkIndex == number_of_chunks-1)
        {
            int length_of_data=(int)(this.length%this.chunk_size);
            byte[] data =new byte[length_of_data];

            file.seek(chunkIndex * chunk_size);
            file.read(data);
            for (int i=0;i<length_of_data;i++)
            {
                b[i]=data[i];
            }

        }
        else{
            throw new IOException("invalid chunk index: file: " + fileName + " chunk number: " + chunkIndex);
        }
    }
    @Synchronized
    public void writeChunk(byte[] b, int chunkIndex) throws IOException
    {
        //System.out.println("writing chunk: " + chunkIndex + "data:" + Arrays.toString(b));
        if(file == null || !file.getFD().valid())
            file = new RandomAccessFile(fileName, fileAccessMode);
        if(chunkIndex >= 0 && chunkIndex < number_of_chunks-1)
        {
            String str="";
            file.seek(chunkIndex * chunk_size);
            str+="expected: "+ chunkIndex * chunk_size;
            file.write(b);
            str+=" got: "+ (file.getFilePointer() - chunk_size)+"\n";
            if((chunkIndex * chunk_size)!=(file.getFilePointer() - chunk_size))
            { writer.append(str);
                writer.flush();}

            //file.seek(0)
        }
        else if(chunkIndex  >= 0 && chunkIndex == number_of_chunks-1)
        {
            int length_of_data=(int)(this.length%this.chunk_size);
            byte[] data =new byte[length_of_data];
            for (int i=0;i<length_of_data;i++)
            {
                data[i]=b[i];
            }
            file.seek(chunkIndex * chunk_size);
            file.write(data);
        }
        else{
            throw new IOException("invalid chunk index: file: " + fileName + " chunk number: " + chunkIndex);
        }

    }


    @Override
    public int compareTo(FileManager o) {
        return 0;
    }



    public long getLength() {
        return length;
    }

    public long getChunk_size() {
        return chunk_size;
    }

    public String getFileName() {
        return fileName;
    }

    public int getNumber_of_chunks() {
        return number_of_chunks;
    }

    public RandomAccessFile getFile() {
        return file;
    }

    public ChunkStatus[] getStatuslist() {
        return statuslist;
    }

    public boolean isSeedMode() {
        return isSeedMode;
    }

    public String getFileAccessMode(){return fileAccessMode;}

    public static Function<Entity, FileManager> entityToFileManager = new Function<Entity, FileManager>() {
        @Override
        public FileManager apply(Entity entity) {
            long fileLength  = Long.valueOf((String)entity.getProperty("FileLength"));
            long chunkSize = Long.valueOf((String)entity.getProperty("ChunkSize"));
            String mode = (String) entity.getProperty("Mode");
            String fileInputName = (String) entity.getProperty("FileInputName");

            boolean seedMode = Boolean.valueOf((String) entity.getProperty("SeedMode"));

            List<ChunkStatus> statuses = new ArrayList<ChunkStatus>();
            for(Entity s : entity.getLinks("StatusList"))
            {
                ChunkStatus stat = ChunkStatus.COMPLETE;
                if(s.getProperty("val").equals(ChunkStatus.COMPLETE.toString()))
                {
                    stat = ChunkStatus.COMPLETE;
                }
                else if(s.getProperty("val").equals(ChunkStatus.EMPTY.toString()))
                {
                    stat = ChunkStatus.EMPTY;
                }
                else if(s.getProperty("val").equals(ChunkStatus.IN_PROGRESS.toString()))
                {
                    stat = ChunkStatus.IN_PROGRESS;
                }
                statuses.add(stat);
            }
            try {
                return new FileManager(fileLength, chunkSize, mode, fileInputName, statuses.toArray(new ChunkStatus[0]), seedMode);
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
            return null;

        }
    };

    @Synchronized
    public void closeFile() {
        try {

            file.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }



}
