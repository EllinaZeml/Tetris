package tetris;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

final class GameController{
    private final Board board;//доска
    private final NotificationOverlay notificationOverlay;//Наложение уведомлений
   // private Tetris tetris;//Наложение уведомлений
    private final ScoreManager scoreManager;//Подсчет балов
    private final BooleanProperty paused = new SimpleBooleanProperty();
    public GameController() {
        this.board = new Board();
        this.scoreManager = new ScoreManager(this);
        notificationOverlay = new NotificationOverlay(this);
        paused.addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean aBoolean2) {
                if (aBoolean2) {
                    pause();
                } else {
                    play();}}});}
    public BooleanProperty pausedProperty() {
        return paused;
    }

    public void start() {
        board.start();
        scoreManager.scoreProperty().set(0);
        paused.set(false);
    }
    public Board getBoard() {
        return board;
    }
    public void apply(byte str, byte blc) {
        board.BLOCKS_PER_ROW=str;
        board.BLOCKS_PER_COLUMN=blc;
    }
    private void pause() {
        board.pause();
    }
    public void stop() {
        board.clear();
        scoreManager.scoreProperty().set(0);
        paused.set(false);
    }

    public void play() {
        paused.set(false);
        board.play();
    }
    public NotificationOverlay getNotificationOverlay() {
        return notificationOverlay;
    }
    public ScoreManager getScoreManager() {
        return scoreManager;
    }

}
