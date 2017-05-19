import data.RepoTorrentFile;
import javafx.collections.ObservableList;

/**
 * Created by Stephen on 11/18/16.
 */
public class TestObservable {

    public static void doSomething(ObservableList<RepoTorrentFile> list){

        try
        {
            Thread.sleep(1000);
            RepoTorrentFile file = new RepoTorrentFile();
            file.setFileName("some file");
            list.add(file);

            RepoTorrentFile f = list.get(0);
            f.setFileName("TESTING");
            list.get(0).setFileName("HELLO");
            System.out.println(f.getFileName());
        }
        catch (InterruptedException ex)
        {

        }


    }
    public static void doThis(ObservableList<RepoTorrentFile> list, int i){


          list.get(i).setFileName("Updated");


    }
}
