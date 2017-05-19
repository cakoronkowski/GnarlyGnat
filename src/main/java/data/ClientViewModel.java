package data;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Created by Stephen on 11/19/16.
 */
public class ClientViewModel {

    public ClientViewModel() {
        torrents = FXCollections.observableArrayList();
    }

    public ObservableList<ClientTorrent> getTorrents() {
        return torrents;
    }

    public void setTorrents(ObservableList<ClientTorrent> torrents) {
        this.torrents = torrents;
    }

    //downloading
    private ObservableList<ClientTorrent> torrents;

}
