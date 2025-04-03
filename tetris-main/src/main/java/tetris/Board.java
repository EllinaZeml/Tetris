package tetris;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HorizontalDirection;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.effect.Light;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.EventListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

//Данный класс представляет доску, на которой размещен игровой процесс

final class Board extends StackPane {
    //Количество скрытых рядов, которые расположены невидимо над доской.
    private static final byte HIDDEN_ROWS = 2;
    //Количество блоков в строке. По умолчанию это значение равно 10.
    protected static  byte BLOCKS_PER_ROW;
    //Количество блоков в столбце. По умолчанию это значение равно 20.
    protected static  byte  BLOCKS_PER_COLUMN;
    //Максимальное количество предварительных просмотров.
    private static final byte MAX_PREVIEWS = 1;
    //Переход "Двигаться вниз".
    private final TranslateTransition moveDownTransition;
    //Переход "Повернуть".
    private final RotateTransition rotateTransition;
    private final SequentialTransition moveTransition;
    //Переход, который позволяет фрагменту быстро перемещаться вниз.
    private final TranslateTransition moveDownFastTransition;
     //Переход для перемещения влево/вправо.
    private final TranslateTransition translateTransition;
    //Набор выполняющихся переходов.
    //Все запущенные переходы приостанавливаются, когда игра приостанавливается
    private final Set<Animation> runningAnimations = new HashSet<>();
    //Двумерный массив, который определяет доску.
    //Если элемент в матрице равен null, то он пуст, в противном случае он занят.
    private final Rectangle[][] matrix = new Rectangle[BLOCKS_PER_COLUMN + HIDDEN_ROWS][BLOCKS_PER_ROW];
    //список фигур(тетромино), которые будут следущими
    private final ObservableList<Tetromino> waitingTetrominos = FXCollections.observableArrayList();
    //Очень быстрый переход по выпадающему списку.
    private final TranslateTransition dropDownTransition;
     //Сохраняет, если нажата клавиша "Вниз".
     //Пока это так, воспроизводится moveDownFastTransition
    private boolean moving = false;
    //Текущее положение x и y с матрицей текущего тетромино.
    private int x = 0, y = 0;
    //Верно, в то время как тетромино отбрасывается (с помощью клавиши пробела).
    private boolean isDropping = false;
    //Текущий тетромино, который падает.
    private Tetromino currentTetromino;
    private List<BoardListener> boardListeners = new CopyOnWriteArrayList<>();
    private DoubleProperty squareSize = new SimpleDoubleProperty();
    //Создание доски
    public Board() {
        setFocusTraversable(true);
        setId("board");
        setMinWidth(35*BLOCKS_PER_ROW);
        setMinHeight(35*BLOCKS_PER_COLUMN);
        maxWidthProperty().bind(minWidthProperty());
        maxHeightProperty().bind(minHeightProperty());
        clipProperty().bind(new ObjectBinding<Node>() {
            {
                super.bind(widthProperty(), heightProperty());
            }
            @Override
            protected Node computeValue() {
                return new Rectangle(getWidth(), getHeight());
            }
        });
        setAlignment(Pos.TOP_LEFT);

        // Инициализировать переход перемещения вниз
        moveDownTransition = new TranslateTransition(Duration.seconds(0.3));
        moveDownTransition.setOnFinished(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                moving = false;
                y++;
            }
        });
        // После того как фигура переместится вниз,
        //ожидать некоторое время, пока она не переместится снова.
        PauseTransition pauseTransition = new PauseTransition();
        pauseTransition.durationProperty().bind(moveDownTransition.durationProperty());

        moveTransition = new SequentialTransition();
        moveTransition.getChildren().addAll(moveDownTransition, pauseTransition);
        moveTransition.setOnFinished(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                moveDown();
            }
        });

        // Движение должно быть приостановлено.
        registerPausableAnimation(moveTransition);

        // Быстрое перемещение фигуры
        moveDownFastTransition = new TranslateTransition(Duration.seconds(0.08));
        // Линейный интерполятор для плавного движения фигур
        moveDownFastTransition.setInterpolator(Interpolator.LINEAR);
        moveDownFastTransition.setOnFinished(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent actionEvent) {
                y++;
                moveDownFast();
            }
        });
        registerPausableAnimation(moveDownFastTransition);

        // Перемещает фигуру влево и вправо.
        translateTransition = new TranslateTransition(Duration.seconds(0.1));
        registerPausableAnimation(translateTransition);

        // Поворачивает
        rotateTransition = new RotateTransition(Duration.seconds(0.1));
        dropDownTransition = new TranslateTransition(Duration.seconds(0.1));
        dropDownTransition.setInterpolator(Interpolator.EASE_IN);

        squareSize.bind(new DoubleBinding() {
            {
                super.bind(widthProperty());
            }

            @Override
            protected double computeValue() {
                return getWidth() / BLOCKS_PER_ROW;
            }
        });
    }
    //Регистрирует анимацию, которая добавляется в список запущенных анимаций
    //Когда игра приостанавливается, все запущенные анимации приостанавливаются.
    private void registerPausableAnimation(final Animation animation) {
        animation.statusProperty().addListener(new ChangeListener<Animation.Status>() {
            @Override
            public void changed(ObservableValue<? extends Animation.Status> observableValue, Animation.Status status, Animation.Status status2) {
                if (status2 == Animation.Status.STOPPED) {
                    runningAnimations.remove(animation);
                } else {
                    runningAnimations.add(animation);
                }
            }
        });
    }

  //Порождение новой фигуры (тетромино)
    private void spawnTetromino() {

        // Заполните очередь ожидающих тетромино, если она пуста.
        while (waitingTetrominos.size() <= MAX_PREVIEWS) {
            waitingTetrominos.add(Tetromino.random(squareSize));
        }

        // Удаляется первый из очереди и создается заново.
        currentTetromino = waitingTetrominos.remove(0);

        // Сбрасывание всех переходов
        rotateTransition.setNode(currentTetromino);
        rotateTransition.setToAngle(0);

        translateTransition.setNode(currentTetromino);
        moveDownTransition.setNode(currentTetromino);
        moveDownFastTransition.setNode(currentTetromino);

        // Добавление текущей фигуры на доску
        getChildren().add(currentTetromino);

        // Перемещение фигуы в привильное положение
        // Запуск тетромино посередине (I, O) или слева посередине (J, L, S, T, Z).
        x = (matrix[0].length - currentTetromino.getMatrix().length) / 2;
        y = 0;
        // Перевод тетромино в исходное положение
        currentTetromino.setTranslateY((y - Board.HIDDEN_ROWS) * getSquareSize());
        currentTetromino.setTranslateX(x * getSquareSize());
        //translateTransition.setToX(currentTetromino.getTranslateX());

        // Начало движения фигур
        moveDown();
    }
//Уведомление тетромино о том, что он не может двигаться дальше вниз.

    private void tetrominoDropped() {
        if (y == 0) {
            //Если фигура не смогла сдвинуться с места и мы все еще находимся в исходной позиции y, то игра окончена.
            currentTetromino = null;
            waitingTetrominos.clear();
            notifyGameOver();
        } else {
            mergeTetrominoWithBoard();
        }
    }
    //Уведомляет слушателя о том, что фрагмент выпал.
    private void notifyOnDropped() {
        for (BoardListener boardListener : boardListeners) {
            boardListener.onDropped();
        }
    }
     //Уведомляет слушателя о том, что игра окончена.

    private void notifyGameOver() {
        for (BoardListener boardListener : boardListeners) {
            boardListener.onGameOver();
        }
    }

    private void notifyOnMove(HorizontalDirection horizontalDirection) {

        for (BoardListener boardListener : boardListeners) {
            boardListener.onMove(horizontalDirection);
        }
    }
    //Уведомляет слушателей о том, что строки были удалены.
    private void notifyOnRowsEliminated(int rows) {
        for (BoardListener boardListener : boardListeners) {
            boardListener.onRowsEliminated(rows);
        }
    }
    //Уведомляет слушателей о том, что был предпринят недопустимый ход.
    private void notifyInvalidMove() {
        for (BoardListener boardListener : boardListeners) {
            boardListener.onInvalidMove();
        }
    }
    //Уведомляет слушателей о том, что был предпринят недопустимый ход.
    private void notifyRotate(HorizontalDirection horizontalDirection) {
        for (BoardListener boardListener : boardListeners) {
            boardListener.onRotate(horizontalDirection);
        }
    }
    //Объединяет тетромино с доской.
    //Для каждой плитки создан прямоугольник на доске.
    //В конце концов тетромино убирается с доски и появляется новый.
    private void mergeTetrominoWithBoard() {
        int[][] tetrominoMatrix = currentTetromino.getMatrix();

        for (int i = 0; i < tetrominoMatrix.length; i++) {
            for (int j = 0; j < tetrominoMatrix[i].length; j++) {

                final int x = this.x + j;
                final int y = this.y + i;

                if (tetrominoMatrix[i][j] == 1 && y < BLOCKS_PER_COLUMN + HIDDEN_ROWS && x < BLOCKS_PER_ROW) {
                    final Rectangle rectangle = new Rectangle();

                    ChangeListener<Number> changeListener = new ChangeListener<Number>() {
                        @Override
                        public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
                            rectangle.setWidth(number2.doubleValue());
                            rectangle.setHeight(number2.doubleValue());
                            rectangle.setTranslateX(number2.doubleValue() * x);
                            rectangle.setTranslateY(number2.doubleValue() * ((Integer) rectangle.getProperties().get("y")));
                        }
                    };
                    squareSize.addListener(new WeakChangeListener<>(changeListener));
                    rectangle.setUserData(changeListener);
                    rectangle.getProperties().put("y", y - HIDDEN_ROWS);
                    rectangle.setWidth(squareSize.doubleValue());
                    rectangle.setHeight(squareSize.doubleValue());
                    rectangle.setTranslateX(squareSize.doubleValue() * x);
                    rectangle.setTranslateY(squareSize.doubleValue() * ((Integer) rectangle.getProperties().get("y")));

                    rectangle.setFill(currentTetromino.getFill());
                    ((Light.Distant) currentTetromino.getLighting().getLight()).azimuthProperty().set(225);
                    rectangle.setEffect(currentTetromino.getLighting());

                    rectangle.setArcHeight(7);
                    rectangle.setArcWidth(7);
                    // Назначение прямоугольника матрице доски
                    matrix[y][x] = rectangle;
                    getChildren().add(rectangle);
                }
            }
        }
        ParallelTransition fallRowsTransition = new ParallelTransition();
        ParallelTransition deleteRowTransition = new ParallelTransition();
        int fall = 0;
        for (int i = y + currentTetromino.getMatrix().length - 1; i >= 0; i--) {
            if (i < matrix.length) {
                boolean rowComplete = i >= y;

                // Assume the row is complete. Let's prove the opposite.
                if (rowComplete) {
                    for (int j = 0; j < matrix[i].length; j++) {
                        if (matrix[i][j] == null) {
                            rowComplete = false;
                            break;
                        }
                    }
                }
                if (rowComplete) {
                    deleteRowTransition.getChildren().add(deleteRow(i));
                    fall++;
                } else if (fall > 0) {
                    fallRowsTransition.getChildren().add(fallRow(i, fall));
                }
            }
        }
        final int f = fall;
        fallRowsTransition.setOnFinished(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent actionEvent) {
                notifyOnDropped();
            }
        });

        //Если хотя бы одна строка была удалена
        if (f > 0) {
            notifyOnRowsEliminated(f);
        }
        final SequentialTransition sequentialTransition = new SequentialTransition();
        sequentialTransition.getChildren().add(deleteRowTransition);
        sequentialTransition.getChildren().add(fallRowsTransition);
        sequentialTransition.setOnFinished(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                //sequentialTransition.getChildren().clear();
                spawnTetromino();
            }
        });
        //Кэшированные узлы приводят к утечке памяти
        //currentTetromino.setCache(false);
        getChildren().remove(currentTetromino);
        currentTetromino = null;
        registerPausableAnimation(sequentialTransition);
        sequentialTransition.playFromStart();
        notifyOnDropped();
    }

   //возвращает переход, который анимирует выпадающую строку
    private Transition fallRow(final int i, final int by) {// i-индекс строки.
        ParallelTransition parallelTransition = new ParallelTransition();

        if (by > 0) {
            for (int j = 0; j < matrix[i].length; j++) {//параметр по количеству строк
                final Rectangle rectangle = matrix[i][j];

                if (rectangle != null) {
                    // Отмена привязки к исходному положению y, чтобы позволить прямоугольнику переместиться в новое положение
                    //rectangle.translateYProperty().unbind();
                    final TranslateTransition translateTransition = new TranslateTransition(Duration.seconds(0.1), rectangle);
                    rectangle.getProperties().put("y", i - HIDDEN_ROWS + by);

                    translateTransition.toYProperty().bind(squareSize.multiply(i - HIDDEN_ROWS + by));
                    translateTransition.setOnFinished(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent actionEvent) {
                            translateTransition.toYProperty().unbind();

                            //rectangle.translateYProperty().bind(squareSize.multiply(i - HIDDEN_ROWS + by));
                        }
                    });
                    parallelTransition.getChildren().add(translateTransition);
                }
                matrix[i + by][j] = rectangle;
            }
        }
        return parallelTransition;
    }

  //Удаляет строку на доске.
    private Transition deleteRow(int rowIndex) {//RowIndex - индекс строки.

        ParallelTransition parallelTransition = new ParallelTransition();

        for (int i = rowIndex; i >= 0; i--) {
            for (int j = 0; j < BLOCKS_PER_ROW; j++) {
                if (i > 1) {
                    final Rectangle rectangle = matrix[i][j];

                    if (i == rowIndex && rectangle != null) {
                        FadeTransition fadeTransition = new FadeTransition(Duration.seconds(0.27), rectangle);
                        fadeTransition.setToValue(0);
                        fadeTransition.setCycleCount(3);
                        fadeTransition.setAutoReverse(true);
                        fadeTransition.setOnFinished(new EventHandler<ActionEvent>() {
                            public void handle(ActionEvent actionEvent) {
                                getChildren().remove(rectangle);
                            }
                        });
                        parallelTransition.getChildren().add(fadeTransition);
                    }

                }
            }
        }
        return parallelTransition;//возвращает переход, который анимирует удаляемую строку.
    }
    //Очищает игровое поле и ожидающие тетромино.
    public void clear() {
        for (int i = 0; i < BLOCKS_PER_COLUMN + HIDDEN_ROWS; i++) {
            for (int j = 0; j < BLOCKS_PER_ROW; j++) {
                matrix[i][j] = null;
            }
        }
        getChildren().clear();
        getChildren().remove(currentTetromino);
        currentTetromino = null;
        waitingTetrominos.clear();
    }
    //Вычисляет, будет ли тетромино пересекаться с доской, передавая матрицу, которая будет у тетромино.
    //Он пересекается либо в том случае, если попадает в другой тетромино, либо в том случае,
    // если он превышает левую, правую или нижнюю границу.
    //*
    //* @* @ param target Matrix - параметр целевой матрицы - матрица тетромино.
    //* @* @param target параметр нацелен на целевую позицию X.
    //* @* @param target параметр нацелен на целевую позицию Y.
    //* @возвращает значение True, если оно действительно пересекается с доской, в противном случае значение false.
    //*/

    private boolean intersectsWithBoard(final int[][] targetMatrix, int targetX, int targetY) {
        Rectangle[][] boardMatrix = matrix;

        for (int i = 0; i < targetMatrix.length; i++) {
            for (int j = 0; j < targetMatrix[i].length; j++) {

                boolean boardBlocks = false;
                int x = targetX + j;
                int y = targetY + i;

                if (x < 0 || x >= boardMatrix[i].length || y >= boardMatrix.length) {
                    boardBlocks = true;
                } else if (boardMatrix[y][x] != null) {
                    boardBlocks = true;
                }

                if (boardBlocks && targetMatrix[i][j] == 1) {
                    return true;
                }
            }
        }
        return false;
    }

   //Запускает игровое поле, создавая нового тетромино.
    public void start() {
        clear();
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                requestFocus();
            }
        });
        spawnTetromino();
    }
    //Опускает тетромино в следующее возможное положение.
    public void dropDown() {
        if (currentTetromino == null) {
            return;
        }

        moveTransition.stop();
        moveDownFastTransition.stop();
        dropDownTransition.stop();

        do {
            y++;
        }
        while (!intersectsWithBoard(currentTetromino.getMatrix(), x, y));
        y--;
        isDropping = true;
        dropDownTransition.setNode(currentTetromino);
        dropDownTransition.toYProperty().bind(squareSize.multiply(y - Board.HIDDEN_ROWS));
        dropDownTransition.setOnFinished(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent actionEvent) {
                isDropping = false;
                tetrominoDropped();
            }
        });
        registerPausableAnimation(dropDownTransition);
        dropDownTransition.playFromStart();

    }

    /**
     * Пытается повернуть тетромино.
     *
     * @параметр direction - горизонтальное направление.
     * @возвращает значение True, если вращение прошло успешно, в противном случае значение false.
     */
    public boolean rotate(final HorizontalDirection direction) {
        boolean result = false;
        if (currentTetromino == null) {
            result = false;
        } else {
            int[][] matrix = currentTetromino.getMatrix();

            int[][] newMatrix = new int[matrix.length][matrix.length];


            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[i].length; j++) {
                    if (direction == HorizontalDirection.RIGHT) {
                        newMatrix[j][matrix.length - 1 - i] = matrix[i][j];
                    } else {
                        newMatrix[matrix[i].length - 1 - j][i] = matrix[i][j];
                    }
                }
            }

            if (!intersectsWithBoard(newMatrix, x, y)) {
                currentTetromino.setMatrix(newMatrix);

                int f = direction == HorizontalDirection.RIGHT ? 1 : -1;

                rotateTransition.setFromAngle(rotateTransition.getToAngle());
                rotateTransition.setToAngle(rotateTransition.getToAngle() + f * 90);

                KeyValue kv = new KeyValue(((Light.Distant) currentTetromino.getLighting().getLight()).azimuthProperty(), 360 - 225 + 90 - rotateTransition.getToAngle());
                KeyFrame keyFrame = new KeyFrame(rotateTransition.getDuration(), kv);
                Timeline lightingAnimation = new Timeline(keyFrame);

                final ParallelTransition parallelTransition = new ParallelTransition(rotateTransition, lightingAnimation);
                registerPausableAnimation(parallelTransition);
                parallelTransition.setOnFinished(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent actionEvent) {
                        // clear, because otherwise parallelTransition won't be gc'ed because it has reference to rotateTransition.
                        parallelTransition.getChildren().clear();
                    }
                });
                parallelTransition.playFromStart();
                result = true;
            }
        }

        if (!result) {
            notifyInvalidMove();
        } else {
            notifyRotate(direction);
        }
        return result;
    }

    /**
     * Перемещает тетромино влево или вправо.
     * @параметр direction - горизонтальное направление.
     * @возвращает значение True, если перемещение прошло успешно. Ложно, если движение было заблокировано доской.
     */
    public boolean move(final HorizontalDirection direction) {
        boolean result;
        if (currentTetromino == null || isDropping) {
            result = false;
        } else {
            int i = direction == HorizontalDirection.RIGHT ? 1 : -1;
            x += i;
            // Если он не движется, проверьте только текущее положение y.
            // Если он движется, также проверьте целевое положение y.
            if (!moving && !intersectsWithBoard(currentTetromino.getMatrix(), x, y) || moving && !intersectsWithBoard(currentTetromino.getMatrix(), x, y) && !intersectsWithBoard(currentTetromino.getMatrix(), x, y + 1)) {
                translateTransition.toXProperty().unbind();
                translateTransition.toXProperty().bind(squareSize.multiply(x));
                translateTransition.playFromStart();
                result = true;
            } else {
                x -= i;
                result = false;
            }
        }
        if (!result) {
            notifyInvalidMove();
        } else {
            notifyOnMove(direction);
        }
        return result;
    }
    //Перемещает тетромино на одно поле вниз.
    public void moveDown() {
        if (!isDropping && currentTetromino != null) {
            moveDownFastTransition.stop();
            moving = true;
            /// Если он может переместиться в следующую позицию y, сделайте это!
            if (!intersectsWithBoard(currentTetromino.getMatrix(), x, y + 1) && !isDropping) {
                //moveDownTransition.setFromY(moveDownTransition.getNode().getTranslateY());
                moveDownTransition.toYProperty().unbind();
                moveDownTransition.toYProperty().bind(squareSize.multiply(y + 1 - Board.HIDDEN_ROWS));
                moveTransition.playFromStart();
            } else {
                tetrominoDropped();
            }
        }
    }
    //Быстро перемещает текущий тетромино вниз, если он еще не падает.
    public void moveDownFast() {
        if (!isDropping) {
            // Остановка обычного перехода перемещения.
            moveTransition.stop();
           //проверка, не будет ли следующая позиция пересекаться с доской.
            if (!intersectsWithBoard(currentTetromino.getMatrix(), x, y + 1)) {
                //Если может двигаться, то двигай
                moveDownFastTransition.toYProperty().unbind();
                moveDownFastTransition.toYProperty().bind(squareSize.multiply(y + 1 - Board.HIDDEN_ROWS));
                moveDownFastTransition.playFromStart();
            } else {
            // В противном случае он достиг земли.
                tetrominoDropped();
            }
        }
    }

//Приостанавливает работу доски.
    public void pause() {
        for (Animation animation : runningAnimations) {
            if (animation.getStatus() == Animation.Status.RUNNING) {
                animation.pause();
            }
        }
    }

  //Воспроизведение доски происходит снова после того, как она была приостановлена.
    public void play() {
        for (Animation animation : runningAnimations) {
            if (animation.getStatus() == Animation.Status.PAUSED) {
                animation.play();
            }
        }
        requestFocus();
    }

    /**
     * * Получает ожидающих тетромино, которые вот-вот появятся на свет.
     * Следующим будет создан первый элемент.
     * @возвращает список тетромино, находящихся в очереди.
     */
    public ObservableList<Tetromino> getWaitingTetrominos() {
        return waitingTetrominos;
    }

    public double getSquareSize() {
        return squareSize.get();
    }
    /**
     * Добавляет слушателя на доску, который получает уведомления
     * об определенных событиях.
     *
     * @* @параметр board Listener - слушатель.
     */

    public void addBoardListener(BoardListener boardListener) {
        boardListeners.add(boardListener);
    }
    /**
     * Удаляет прослушиватель, который ранее был добавлен с помощью addBoardListener()}
     *
     * @* @параметр board Listener - слушатель.
     */
    public void removeBoardListener(BoardListener boardListener) {
        boardListeners.remove(boardListener);
    }

  //Позволяет прослушивать определенные события на доске
    public static interface BoardListener extends EventListener {



       //Вызывается, когда удаляется тетромино или полная строка
       // удаляется после того, как были удалены некоторые строки.
        void onDropped();

      /**
       * Вызывается, когда одна или несколько строк заполнены и, следовательно, удаляются.
       *
       * @param rows - количество строк.
       */
        void onRowsEliminated(int rows);

       //Вызывается, когда игра окончена.
        void onGameOver();

      //Вызывается, когда было сделано недопустимое значение.
        void onInvalidMove();

      /**
       * Вызывается, когда фигура была перемещена.
       *
       * @* @параметр horizontalDirection - Указывает направление.
       */
        void onMove(HorizontalDirection horizontalDirection);


       //Вызывается при повороте фрагмента.
        void onRotate(HorizontalDirection horizontalDirection);
    }
}
