package tetris;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.HorizontalDirection;
//Оценка ПО подсчету очков
final class ScoreManager implements Board.BoardListener {
    private final IntegerProperty score = new SimpleIntegerProperty();
    private final GameController gameController;
    public ScoreManager(GameController gameController) {
        this.gameController = gameController;
        gameController.getBoard().addBoardListener(this);}
    public IntegerProperty scoreProperty() {
        return score;
    }
    private void addScore(int score) {
        this.score.set(this.score.get() + score);
    }
    //количество баллов за устраненные строки
    @Override
    public void onRowsEliminated(int rows) {
        /*switch (rows) {
            case 1:
                addScore(40);
                break;
            case 2:
                addScore(100);
                break;
            case 3:
                addScore(300);
                break;
            case 4:
                addScore(1200);
                break;
        }*/
        if (rows == 1) {
            addScore(40);
        } else if (rows == 2) {
            addScore(100);
        } else if (rows == 3) {
            addScore(300);
        } else if (rows == 4) {
            addScore(1200);
        }
    }
    @Override
    public void onGameOver() {
    }
    @Override
    public void onDropped() {
    }
    @Override
    public void onInvalidMove() {
    }
    @Override
    public void onMove(HorizontalDirection horizontalDirection) {
    }
    @Override
    public void onRotate(HorizontalDirection horizontalDirection) {
    }
}
