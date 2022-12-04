

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

public class NaiveBot implements Bot {

    private final int APPLE_CHANGE_TIME = 10;
    private final double DECISION_TIME = 0.5e9;
    private final int MAX_DEPTH = 180;
    private final double APPLE_REWARD = 0.5;
    private final double GAME_REWARD = 1;
    private Random rnd = new Random();
    private BufferedWriter outBuffer;
    
    // Indices for PrevCur array
    private final int PREV = 0;
    private final int CUR = 1;

    private NaiveController controller = new NaiveController();
    private Coordinate prevAppleCoordinate = null;
    private int timeUntilAppleChange = APPLE_CHANGE_TIME;
    private int numMoves = -1;

    private int numRollouts = 0;

    public NaiveBot(){
	try {
	    outBuffer =
		new BufferedWriter(new FileWriter("Naivelog.txt"));
	} catch (IOException e){}
    }


    /*
      This version of chooseDirection uses the rolloutPolicy to impelment MCTS.
      Below is a version of chooseDirection that plays Snakes using solely the
      rollout policy. Exactly one of these versions should be commented out. 
     */
    @Override
    public Direction chooseDirection(Snake snake, Snake opponent, Coordinate mazeSize,
				     Coordinate apple){
	Direction[] rowPrevCurDir = getPrevCurDirection(snake);
	Direction[] colPrevCurDir = getPrevCurDirection(opponent);
	
	int rowSnakeMove = toRelativeDirection(rowPrevCurDir[PREV], rowPrevCurDir[CUR]);
	int colSnakeMove = toRelativeDirection(colPrevCurDir[PREV], colPrevCurDir[CUR]);
	//controller.moveRoot(rowSnakeMove, colSnakeMove);
	controller = new NaiveController();
	
	updateGameState(snake, opponent, apple);
	long startTime = System.nanoTime();
	long timeDelta;
	numRollouts = 0;
	while(true){
	    timeDelta = System.nanoTime() - startTime;
	    if (timeDelta >= DECISION_TIME)
		break;
	    rollout(snake, opponent, mazeSize, apple, controller);
	    numRollouts++;
	}
	int[] finalMoves = controller.getFinalMoves();
	Direction chosenDir = toDirection(finalMoves[NaiveController.ROW_MOVE], rowPrevCurDir[CUR]);
	try{
	    outBuffer.write(String.format("Current Direction: " + rowPrevCurDir[CUR] + "\n"));
	    outBuffer.write(String.format("Chosen Direction: " + chosenDir + "\n"));
	    outBuffer.flush();
	} catch (IOException e) {}
	return chosenDir;
    }


    public int getNumRollouts(){return numRollouts;}
    public NaiveController getController(){return controller;}


    /*
      Perform a single rollout 
     */
    private void rollout(Snake snake, Snake opponent, Coordinate mazeSize,
			 Coordinate apple, NaiveController controller){
	int curDepth = 0;
	boolean defaultPolicy = false;
	boolean[] rowConstraints = new boolean[NaiveNode.NUM_OPTIONS];
	boolean[] colConstraints = new boolean[NaiveNode.NUM_OPTIONS];
	Snake rowSnake = snake.clone();
	Snake colSnake = opponent.clone();
	Direction[] rowPrevCurDir;
	Direction[] colPrevCurDir;
	double colScore = 0;
	double rowScore = 0;
	boolean gameOver = false;
	int[] nextMove = new int[NaiveController.RETURN_SIZE];
	int appleCountDown = timeUntilAppleChange;
	boolean rowDead = false;
	boolean colDead = false;

	ArrayList<Integer> defaultMoves = new ArrayList<>();
	for (int i = 0; i < NaiveNode.NUM_OPTIONS; i++)
	    defaultMoves.add(i);
	
	while (curDepth < MAX_DEPTH) {
	    rowPrevCurDir = getPrevCurDirection(snake);
	    colPrevCurDir = getPrevCurDirection(opponent);

	    // --------- Build Move Constraints ----------
	    if (!fillConstraints(rowSnake, rowPrevCurDir[CUR], colSnake,
				 colPrevCurDir[CUR], mazeSize, apple, rowConstraints)){
		rowDead = true;
		gameOver = true;
	    }
	    
	    if (!fillConstraints(colSnake, colPrevCurDir[CUR], rowSnake,
				 rowPrevCurDir[CUR], mazeSize, apple, colConstraints)){
		colDead = true;
		gameOver = true;
	    }
	    if (gameOver)
		break;

	    //------------Compute apple distances -----------
	    double[] rowDistances = getDistances(rowSnake, rowPrevCurDir[CUR], apple);
	    double[] colDistances = getDistances(colSnake, colPrevCurDir[CUR], apple);


	    // ------------ Get next move -----------------
	    if (defaultPolicy){
		// Greedy Rollouts (TODO IMPLEMENT GREEDY POLICY)
		nextMove[NaiveController.ROW_MOVE] = greedyPolicy(rowDistances, rowConstraints);
		nextMove[NaiveController.COL_MOVE] = greedyPolicy(colDistances, colConstraints);
		nextMove[NaiveController.LEAF_NODE] = 1;


	    } else {
		nextMove = controller.nextMove(rowConstraints, colConstraints,
					       rowDistances, colDistances);
	    }

	    // ----------- Run Game Step ------------
	    Direction rowDir = toDirection(nextMove[NaiveController.ROW_MOVE],
					   rowPrevCurDir[CUR]);
	    Direction colDir = toDirection(nextMove[NaiveController.COL_MOVE],
					   colPrevCurDir[CUR]);
	    boolean rowGrow = rowSnake.getHead().moveTo(rowDir).equals(apple);
	    rowScore += (rowGrow ? APPLE_REWARD : 0);
	    boolean colGrow = colSnake.getHead().moveTo(colDir).equals(apple);
	    colScore += (colGrow ? APPLE_REWARD : 0);
	    rowDead = !rowSnake.moveTo(rowDir, rowGrow);
	    colDead = !colSnake.moveTo(colDir, colGrow);
	    if (rowGrow || colGrow || apple == null || appleCountDown == 0){
		apple = randomNonOccupiedCell(rowSnake, colSnake, mazeSize);
		appleCountDown = APPLE_CHANGE_TIME;
	    } else {
		appleCountDown--;
	    }
	    rowDead |= rowSnake.headCollidesWith(colSnake);
	    colDead |= colSnake.headCollidesWith(rowSnake);
	    if (rowDead || colDead){
		gameOver = true;
		break;
	    }
	    curDepth++;
	}
	if (rowDead ^ colDead){
	    rowScore += (colDead ? GAME_REWARD : 0);
	    colScore += (rowDead ? GAME_REWARD : 0);
	} else {
	    boolean rowBigger = (rowSnake.body.size() - colSnake.body.size()) > 0;
	    rowScore += (rowBigger ? GAME_REWARD : 0);
	    colScore += (rowBigger ? 0 : GAME_REWARD);
	}
	controller.propogateScore((rowScore - colScore) * 1 / curDepth);
    }

    
    private double[] getDistances(Snake snake, Direction snakeDir, Coordinate apple){
	double[] distances = new double[Node.NUM_OPTIONS];
	Direction nextHopDir;
	Coordinate nextHop;
	
	for (int i = 0; i < NaiveNode.NUM_OPTIONS; i++){
	    nextHopDir = toDirection(i, snakeDir);
	    nextHop = snake.getHead().moveTo(nextHopDir);
	    distances[i]  = Math.abs(nextHop.x - apple.x) + Math.abs(nextHop.y - apple.y);
	}
	return distances;
    }

    private int greedyPolicy(double[] distances, boolean[] constraints){
	int[] bestChoices = new int[NaiveNode.NUM_OPTIONS];
	double curBest = Double.MAX_VALUE;
	int numBest = 0;
	for (int i = 0; i < NaiveNode.NUM_OPTIONS; i++){
	    if (!constraints[i])
		continue;
	    if (distances[i] < curBest){
		curBest = distances[i];
		numBest = 0;
		bestChoices[numBest] = i;
		numBest++;
	    } else if (distances[i] == curBest){
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
	for (int i = 0; i < NaiveNode.NUM_OPTIONS; i++){ // Iterate through forward, right, left
	    Direction nextDir = toDirection(i, snakeDir);
	    Coordinate nextSquare = snake.getHead().moveTo(nextDir);
	    constraints[i] = squareOkayAdvanced(snake, opponent, mazeSize, apple, nextSquare);
	    if (!constraints[i])
		numConstraints++;
	}
	return numConstraints < NaiveNode.NUM_OPTIONS;
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
	    return NaiveController.FORWARD;
	
	Coordinate left = new Coordinate(prevDir.v.y, -1 * prevDir.v.x);
	if (left.equals(curDir.v))
	    return NaiveController.LEFT;

	Coordinate right = new Coordinate(-1 * prevDir.v.y, prevDir.v.x);
	if (right.equals(curDir.v))
	    return NaiveController.RIGHT;
	return -1;
    }


    /*
      Convert a current direction and a relative direction to a direction. 
     */
    private Direction toDirection(int relDirection, Direction baseDirection){
	if (relDirection == NaiveController.FORWARD)
	    return baseDirection;
	if (relDirection == NaiveController.LEFT)
	    return matchDirection(baseDirection.dy, -1 * baseDirection.dx);
	if (relDirection == NaiveController.RIGHT)
	    return matchDirection(-1 * baseDirection.dy, baseDirection.dx);
	System.exit(1); // No direction found
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
