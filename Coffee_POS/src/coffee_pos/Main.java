package coffee_pos;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.awt.print.PrinterException;
import javafx.scene.image.Image;
import javafx.stage.StageStyle;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        
        // Load the icon into the stage
        primaryStage.getIcons().add(
            new Image(getClass().getResourceAsStream("/coffee_pos/logo.png"))
        );
        
        // Existing FXML loading logic...
        primaryStage.setTitle("Coffee POS");
        primaryStage.show();

        Parent root = FXMLLoader.load(getClass().getResource("view/LoginView.fxml"));
        primaryStage.setTitle("Coffee POS - Login");
        primaryStage.getIcons().add(new Image("coffee_pos/icons/logo.png"));
        // primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
