import data.RepoTorrentFile;
import db.DbManager;
import workers.Repo;

/**
 * Created by Stephen on 11/3/16.
 */

public class Main {

    public static void main(String[] args) {


       // dbManager.ClearAllData();
        RepoTorrentFile file = new RepoTorrentFile();
        file.setFileName("GOT SEASION 12");
        file.setChunkSize(23);
        file.setFileSize(1024);
        //DbManager.InsertTorrentFile(file);


//        Repo repo = new Repo();
//        repo.start();


    }


}
