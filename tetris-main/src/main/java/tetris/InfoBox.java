package tetris;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.Reflection;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import javax.swing.*;
import java.util.concurrent.Callable;

import static java.lang.Integer.parseInt;

//пользовательское окно
final class InfoBox extends VBox {
    // Защита клавиатуры от лишних символов
    public void digits(TextField field){
        field.setTextFormatter(new TextFormatter<Integer>(change -> {
            String newText = change.getControlNewText();
            if(newText.matches("\\d*")){
                return change;
            }
            return null;
        }));
    }
    public InfoBox(final GameController gameController) {
        Text text = new Text();
        text.setText("Menu");
        text.setStyle("Lucida Console");

        setPadding(new Insets(20, 20, 20, 20));
        //панель с интервалом по умолчанию в 10 пикселей
        setSpacing(10);
        setId("infoBox");
        final ImageView playImageView = new ImageView(new Image(getClass().getResourceAsStream("/tetris/play.png")));
        playImageView .setFitHeight(40);
        playImageView .setFitWidth(40);
        final ImageView pauseImageView = new ImageView(new Image(getClass().getResourceAsStream("/tetris/pauza_4mn5bjlyjpeg_64.png")));
        pauseImageView .setFitHeight(40);
        pauseImageView .setFitWidth(40);
        final ImageView rotateImageView = new ImageView(new Image(getClass().getResourceAsStream("/tetris/rotate.png")));
        rotateImageView .setFitHeight(40);
        rotateImageView.setFitWidth(40);
        Button btnStart =new Button("New Game",rotateImageView);
        Button btnApply =new Button("Apply");
        final ImageView stopImageView = new ImageView(new Image(getClass().getResourceAsStream("/tetris/stop.png")));
        stopImageView .setFitHeight(40);
        stopImageView.setFitWidth(40);
        Button btnStop = new Button("Stop", stopImageView);
        Label lb1=new Label("Ширина игрового поля");
        Label lb2=new Label("Высота игрового поля");
        TextField str =new TextField();
        TextField blc =new TextField();
        digits(str);
        digits(blc);
        btnStart.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                gameController.start();
            }
        });
        btnApply.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                gameController.apply((byte)parseInt(str.getText()),(byte)parseInt(blc.getText()));
                btnApply.getScene().getWindow().hide();
                Stage stage1=new Stage();
                stage1.setTitle("T E T R I S ");
                Scene scene = new Scene(new Tetris());
                stage1.setScene(scene);
                stage1.show();
                //stage1.close();
            }
        });
        btnStop.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                gameController.stop();
            }
        });

        btnStop.setMaxWidth(Double.MAX_VALUE);
        btnStop.setAlignment(Pos.CENTER_LEFT);
        btnStart.setMaxWidth(Double.MAX_VALUE);
        btnStart.setAlignment(Pos.CENTER_LEFT);
        str.setMaxWidth(Double.MAX_VALUE);
        str.setAlignment(Pos.CENTER_LEFT);
        str.setMaxWidth(Double.MAX_VALUE);
        str.setAlignment(Pos.CENTER_LEFT);
        blc.setMaxWidth(Double.MAX_VALUE);
        blc.setAlignment(Pos.CENTER_LEFT);
        lb1.setMaxWidth(Double.MAX_VALUE);
        lb1.setAlignment(Pos.CENTER_LEFT);
        lb2.setMaxWidth(Double.MAX_VALUE);
        lb2.setAlignment(Pos.CENTER_LEFT);
        btnApply.setMaxWidth(Double.MAX_VALUE);
        btnApply.setAlignment(Pos.CENTER_LEFT);
        Button btnPause = new Button("Pause");
        btnPause.graphicProperty().bind(new ObjectBinding<Node>() {
            {
                super.bind(gameController.pausedProperty());
            }

            @Override
            protected Node computeValue() {
                if (gameController.pausedProperty().get()) {
                    btnPause.setText("Play");
                    return playImageView;
                } else {
                    btnPause.setText("Pause");
                    return pauseImageView;

                }
            }
        });
        btnPause.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (gameController.pausedProperty().get()) {
                    gameController.pausedProperty().set(false);
                } else {
                    gameController.pausedProperty().set(true);

                }
            }
        });
        btnPause.setMaxWidth(Double.MAX_VALUE);
        btnPause.setAlignment(Pos.CENTER_LEFT);
        Preview preview = new Preview(gameController);
        Label lblPoints = new Label();
        lblPoints.getStyleClass().add("score");
        lblPoints.textProperty().bind(Bindings.createStringBinding(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return String.valueOf(gameController.getScoreManager().scoreProperty().get());
            }
        }, gameController.getScoreManager().scoreProperty()));
        lblPoints.setAlignment(Pos.CENTER_RIGHT);
        lblPoints.setMaxWidth(Double.MAX_VALUE);
        lblPoints.setEffect(new Reflection());
        //добавление кнопок на окно
        getChildren().add(preview);
        getChildren().add(btnStart);
        getChildren().add(btnPause);
        getChildren().add(btnStop);
        getChildren().add(lb1);
        getChildren().add(str);
        getChildren().add(lb2);
        getChildren().add(blc);
        getChildren().add(btnApply);
        Label lblInfo = new Label("Используйте клавиши со стрелками \n"+ " для перемещения и поворота\n" +
                "и пробел для\n" +
                "отбрасывания фигуры.");

        getChildren().add(lblInfo);
        getChildren().addAll(lblPoints);


    }
}
