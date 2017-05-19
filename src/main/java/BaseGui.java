import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;

import java.io.IOException;

/**
 * Created by Stephen on 11/9/16.
 */
public abstract class BaseGui extends Application {
    /**
     * Returns object of resource loaded, cast to the Fx element that you want.
     * Yeah, scary,.....
     *
     * @param viewName
     * @return Object that should be cast to the gui element you want.
     */
    protected Object LoadView(String viewName){
        try {
            return FXMLLoader.load(getClass().getResource("/views/" + viewName));

        }
        catch (IOException ex)
        {
            System.err.println(ex);
        }
        return null;
    }
}
