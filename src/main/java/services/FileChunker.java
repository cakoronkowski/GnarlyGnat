package services;

import java.io.File;
import java.io.RandomAccessFile;

/**
 * Created by cakor on 11/8/2016.
 */
public class FileChunker {
    public static void main(String[] args)
    {
        try {
            RandomAccessFile file = new RandomAccessFile("testfile.txt", "r");
            System.out.println(file.length());
            byte[] array =new byte[(int)file.length()];
            RandomAccessFile outfile = new RandomAccessFile("testoutfile.txt", "rw");
            outfile.setLength(file.length());
            file.read(array);
            byte[] firstchunk = new byte[15];
            byte[] secondchunk= new byte[(int)file.length()-15];
            for(int i =0;i<15;i++)
            {
                firstchunk[i]=array[i];
            }
            int j=0;
            for (int i =15;i<array.length; i++)
            {
                secondchunk[j]=array[i];
                j++;
            }
            System.out.println(outfile.length());
            outfile.seek(15);
            outfile.write(secondchunk);
            outfile.seek(0);
            outfile.write(firstchunk);
        }
        catch(Exception e)
        {
            System.err.println(e);
        }
    }
}
