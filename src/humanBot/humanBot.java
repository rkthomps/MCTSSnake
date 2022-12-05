package humanBot;

import snakes.Bot;
import snakes.Coordinate;
import snakes.Direction;
import snakes.Snake;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Iterator;

public class humanBot implements Bot {
    private Direction move = Direction.UP;

    /**
     * Choose the direction for a human player
     * @param snake    Your snake's body with coordinates for each segment
     * @param opponent Opponent snake's body with coordinates for each segment
     * @param mazeSize Size of the board
     * @param apple    Coordinate of an apple
     * @return Direction of bot's move (as a human)
     */
    @Override
    public Direction chooseDirection(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
        Coordinate head = snake.getHead();

        /* Get the coordinate of the second element of the snake's body
         * to prevent going backwards */
        Coordinate afterHeadNotFinal = null;
        if (snake.body.size() >= 2) {
            Iterator<Coordinate> it = snake.body.iterator();
            it.next();
            afterHeadNotFinal = it.next();
        }

        final Coordinate afterHead = afterHeadNotFinal;

        JFrame myJFrame = new JFrame();
        myJFrame.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                if (keyCode == KeyEvent.VK_UP) {
                    if (!head.moveTo(Direction.DOWN).equals(afterHead)) {
                        move = Direction.DOWN;
                    }
                }
                else if (keyCode == KeyEvent.VK_DOWN) {
                    if (!head.moveTo(Direction.UP).equals(afterHead)) {
                        move = Direction.UP;
                    }
                }
                else if (keyCode == KeyEvent.VK_LEFT) {
                    if (!head.moveTo(Direction.LEFT).equals(afterHead)) {
                        move = Direction.LEFT;
                    }
                }
                else if (keyCode == KeyEvent.VK_RIGHT) {
                    if (!head.moveTo(Direction.RIGHT).equals(afterHead)) {
                        move = Direction.RIGHT;
                    }
                }
            }
        });

        final double DECISION_TIME = 0.7e9;
        long startTime = System.nanoTime();
        long timeDelta;
         while(true){
             timeDelta = System.nanoTime() - startTime;
             if (timeDelta >= DECISION_TIME)
                 break;
         }

        myJFrame.setVisible(true);

        return move;
    }
}