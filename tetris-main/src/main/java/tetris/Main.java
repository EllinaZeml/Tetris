package tetris;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
public final class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        /*Button popupButton = new Button("Открыть всплывающее окно");

        Popup popup = new Popup();
        VBox popupContent = new VBox();
        popupContent.setStyle("-fx-background-color: white; -fx-padding: 10px;");
        popupContent.getChildren().add(new Button("Закрыть окно"));

        popup.getContent().addAll(popupContent);

        popupButton.setOnAction(event -> {
            if (popup.isShowing()) {
                popup.hide();
            } else {
                popup.show(stage);
            }
        });

        VBox root = new VBox();
        root.getChildren().addAll(popupButton);*/
        stage.setTitle("T E T R I S ");
        Scene scene = new Scene(new Tetris());
        stage.setScene(scene);
        stage.show();
    }

    public static final class Launcher { public static void main(String[] args) {
            launch();
        }}
}