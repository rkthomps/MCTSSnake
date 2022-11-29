

package mctsSnake;

import java.util.Iterator;
import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import snakes.Coordinate;
import snakes.Snake;
import snakes.Bot;
import snakes.Direction;

public class RolloutBot implements Bot {

    private final int APPLE_CHANGE_TIME = 10;
    private final double DECISION_TIME = 0.88e9;
    private Random rnd = new Random();
    private BufferedWriter outBuffer;
    
    // Indices for PrevCur array
    private final int PREV = 0;
    private final int CUR = 1;

    private Controller controller = new Controller();
    private Coordinate prevAppleCoordinate = null;
    private int timeUntilAppleChange = APPLE_CHANGE_TIME;
    private int numMoves = -1;

    private int numRollouts = 0;

    public RolloutBot(){
	try {
	    outBuffer =
		new BufferedWriter(new FileWriter("TheBotLog.txt"));
	} catch (IOException e){}
    }

    /*
      Implements RolloutPolicySnake. Exactly one chooseDirection function should be
      commented out (this one or the one above)
     */
    @Override      
    public Direction chooseDirection(Snake snake, Snake opponent, Coordinate mazeSize,
				     Coordinate apple){
	boolean[] constraints = new boolean[Node.NUM_OPTIONS];
	Direction[] rowPrevCurDir = getPrevCurDirection(snake);
	Direction[] colPrevCurDir = getPrevCurDirection(opponent);
	if (!fillConstraints(snake, rowPrevCurDir[CUR], opponent, colPrevCurDir[CUR],
			     mazeSize, apple, constraints))
	    return Direction.UP;
	int nextMove = greedyPolicy(snake, rowPrevCurDir[CUR], constraints, apple);
	return toDirection(nextMove, rowPrevCurDir[CUR]);
    }


    public int getNumRollouts(){return numRollouts;}
    public Controller getController(){return controller;}


    private int greedyPolicy(Snake snake, Direction snakeDir, boolean[] constraints,
			     Coordinate apple){
	int[] bestChoices = new int[Node.NUM_OPTIONS];
	int curBest = Integer.MAX_VALUE;
	int numBest = 0;
	Direction nextHopDir;
	Coordinate nextHop;
	int score;
	for (int i = 0; i < Node.NUM_OPTIONS; i++){
	    if (!constraints[i])
		continue;
	    nextHopDir = toDirection(i, snakeDir);
	    nextHop = snake.getHead().moveTo(nextHopDir);
	    score = Math.abs(nextHop.x - apple.x) + Math.abs(nextHop.y - apple.y);
	    if (score < curBest){
		curBest = score;
		numBest = 0;
		bestChoices[numBest] = i;
		numBest++;
	    } else if (score == curBest){
		bestChoices[numBest] = i;
		numBest++;
	    }
	}
	return bestChoices[rnd.nextInt(numBest)];
    }


    private int randomPolicy(ArrayList<Integer> defaultMoves, boolean[] constraints){
	return getRandomMove(defaultMoves, constraints);
    }


    private int getRandomMove(ArrayList<Integer> defaultMoves, boolean[] constraints){
	Collections.shuffle(defaultMoves);
	for (int i = 0; i < defaultMoves.size(); i++){
	    if (constraints[i])
		return i;
	}
	return -1;
    }

    /*
      Returns false if all of the moves are constrained. Returns true otherwise
    */
    private boolean fillConstraints(Snake snake, Direction snakeDir, Snake opponent,
				 Direction opponentDir, Coordinate mazeSize,
				 Coordinate apple, boolean[] constraints){
	int numConstraints = 0;
	for (int i = 0; i < Node.NUM_OPTIONS; i++){ // Iterate through forward, right, left
	    Direction nextDir = toDirection(i, snakeDir);
	    Coordinate nextSquare = snake.getHead().moveTo(nextDir);
	    constraints[i] = squareOkayAdvanced(snake, opponent, mazeSize, apple, nextSquare);
	    if (!constraints[i])
		numConstraints++;
	}
	return numConstraints < Node.NUM_OPTIONS;
    }

    
    /*
      If we wanted to do more advanced filtering it would be here but I'm worried
      it will be too computationally intensive. 
     */
    private boolean squareOkay(Snake snake, Snake opponent, Coordinate mazeSize,
			       Coordinate apple, Coordinate nextSquare){
	return nextSquare.inBounds(mazeSize);
    }

    /*
      Here is the more advanced filtering I was talking about above
     */
    private boolean squareOkayAdvanced(Snake snake, Snake opponent,
				       Coordinate mazeSize, Coordinate apple,
				       Coordinate nextSquare){
	if (!nextSquare.inBounds(mazeSize))
	    return false;
	if (nextSquare.equals(snake.body.getLast()))
	    return true;
	if (nextSquare.equals(opponent.body.getLast()))
	    return true;
	if (snake.elements.contains(nextSquare))
	    return false;
	if (opponent.elements.contains(nextSquare))
	    return false;
	return true;
    }

    
    private void updateGameState(Snake rowSnake, Snake colSnake, Coordinate apple){
	if (apple.equals(prevAppleCoordinate)){
	    timeUntilAppleChange--;
	    if (timeUntilAppleChange < 0) {timeUntilAppleChange = 0;}
	} else {
	    timeUntilAppleChange = APPLE_CHANGE_TIME;
	    prevAppleCoordinate = apple;
	}
	numMoves++;
    }

    /*
      Gets the previous and current direction of the given snake.
      Always returns an array of size two. First element of the array
      contains the previous direction. Second element of the array
      contains the current direction.
     */
    private Direction[] getPrevCurDirection(Snake snake){
	Direction[] prevCur = new Direction[2];
	Iterator<Coordinate> bodyIterator = snake.body.iterator();
	Coordinate head = bodyIterator.next();
	Coordinate second = bodyIterator.next();
	Coordinate third = bodyIterator.next();

	Direction prevDir = third.getDirection(second);
	Direction curDir = second.getDirection(head);
	prevCur[0] = prevDir;
	prevCur[1] = curDir;
	return prevCur;
    }

    /*
      Find *Relitive* direction of the curDir with respect to prevDir
    */
    private int toRelativeDirection(Direction prevDir, Direction curDir){
	if (prevDir.v.equals(curDir.v))
	    return Controller.FORWARD;
	
	Coordinate left = new Coordinate(prevDir.v.y, -1 * prevDir.v.x);
	if (left.equals(curDir.v))
	    return Controller.LEFT;

	Coordinate right = new Coordinate(-1 * prevDir.v.y, prevDir.v.x);
	if (right.equals(curDir.v))
	    return Controller.RIGHT;
	return -1;
    }


    /*
      Convert a current direction and a relative direction to a direction. 
     */
    private Direction toDirection(int relDirection, Direction baseDirection){
	if (relDirection == Controller.FORWARD)
	    return baseDirection;
	if (relDirection == Controller.LEFT)
	    return matchDirection(baseDirection.dy, -1 * baseDirection.dx);
	if (relDirection == Controller.RIGHT)
	    return matchDirection(-1 * baseDirection.dy, baseDirection.dx);
	return Direction.UP;
    }


    private Coordinate randomNonOccupiedCell(Snake rowSnake, Snake colSnake,
					     Coordinate mazeSize) {
	while (true) {
	    Coordinate c = new Coordinate(rnd.nextInt(mazeSize.x), rnd.nextInt(mazeSize.y));
	    if (rowSnake.elements.contains(c))
		continue;
	    if (colSnake.elements.contains(c))
		continue;
	    return c;
	}
    }

    private Direction matchDirection(int dx, int dy){
	if (dx == 0 && dy == 1)
	    return Direction.UP;
	if (dx == 0 && dy == -1)
	    return Direction.DOWN;
	if (dx == 1 && dy == 0)
	    return Direction.RIGHT;
	return Direction.LEFT;
    }
}
