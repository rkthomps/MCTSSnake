
package test;

import snakes.Coordinate;
import snakes.Direction;
import snakes.Snake;

import mctsSnake.TheBot;

public class TestTheBot{

    public static void main(String[] args){
	Coordinate mazeSize = new Coordinate(14, 14);
	Coordinate snakeHead = new Coordinate(2, 13);
	Direction tailDirection = Direction.DOWN;
	int snakeSize = 3;
	Snake snake = new Snake(snakeHead, tailDirection,
				snakeSize, mazeSize);

	Coordinate opSnakeHead = new Coordinate(2, 2);
	Direction opTailDirection = Direction.DOWN;
	Snake opponent = new Snake(opSnakeHead, opTailDirection,
				   snakeSize, mazeSize);
	Coordinate appleCoordinate = new Coordinate(1, 13);

	TheBot theBot = new TheBot();
	long startTime = System.nanoTime();
	System.out.println("here");
	Direction firstMove = theBot.chooseDirection(snake, opponent, mazeSize,
						     appleCoordinate);
	System.out.println(theBot.getController().getRoot().toString(2));
	double duration = (double)(System.nanoTime() - startTime) / 1e9;
	System.out.println(String.format("%5.5f Seconds", duration));
	System.out.println("First Move: " + firstMove);
	System.out.println(String.format("Number of Rollouts: %d",
					 theBot.getNumRollouts()));
	
    }
}
