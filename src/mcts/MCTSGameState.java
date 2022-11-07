package mcts;

import java.util.Random;

import snakes.Coordinate;
import snakes.Direction;
import snakes.Snake;

enum results {
	WIN,
	LOSS,
	TIE,
	ONGOING
}

public class MCTSGameState {
	// private static final int TIMEOUT_THRESHOLD = 1;
	public Snake snake1;
	public Snake snake0;
	public Coordinate appleCoordinate;
	public Coordinate mapSize;
	public int treeDepth;
	public Direction d0;
	public Direction d1;
	public int numIterationsAppleNotEaten;
	public results endResult = results.ONGOING;

	public MCTSGameState(Snake snake0, Snake snake1, Coordinate appleCoordinate, Coordinate mapSize, int depth, Direction d0, Direction d1, int numIterationsAppleNotEaten) {
		this.snake0 = snake0;
		this.snake1 = snake1;
		this.appleCoordinate = appleCoordinate;
		this.mapSize = mapSize;
		this.treeDepth = depth;
		this.d0 = d0;
		this.d1 = d1;
		this.numIterationsAppleNotEaten = numIterationsAppleNotEaten;
	}

	public MCTSGameState clone() {
		MCTSGameState clonedState = new MCTSGameState(snake0.clone(), snake1.clone(), new Coordinate(appleCoordinate.x, appleCoordinate.y), mapSize, treeDepth, d0, d1, numIterationsAppleNotEaten);
		return clonedState;
	}

	/**
	 * Set the end result of a terminal node based on whether snake0 won or lost
	 * 1 for snake0 won, 0 for tied, -1 for snake0 lost, -2 is set by default for ongoing
	 * @param result
	 */
	public void setEndResult(results result) {
		endResult = result;
	}

	public boolean isTerminal() {
		return endResult != results.ONGOING;
	}

	// public String toString() {
		// return 
	// }

	public static MCTSGameState runOneStep(MCTSGameState state) {
		boolean grow0 = state.snake0.getHead().moveTo(state.d0).equals(state.appleCoordinate);
		boolean grow1 = state.snake1.getHead().moveTo(state.d1).equals(state.appleCoordinate);

		boolean wasGrow = grow0 || grow1;

		boolean s0dead = !state.snake0.moveTo(state.d0, grow0);
		boolean s1dead = !state.snake1.moveTo(state.d1, grow1);

		if (wasGrow || state.appleCoordinate == null) {
			state.appleCoordinate = MCTSGameState.randomNonOccupiedCell(state.snake0, state.snake1, state.mapSize);
			state.numIterationsAppleNotEaten = 0; // reset the counter to disappear
		} else {
			// Apple must change place if not eaten after 10 iterations
			if (state.treeDepth + 1 == 10) {
				// reset counter and change apple
				state.appleCoordinate = MCTSGameState.randomNonOccupiedCell(state.snake0, state.snake1, state.mapSize);
				state.numIterationsAppleNotEaten = 0;
			} else
				state.numIterationsAppleNotEaten++;
		}
		s0dead |= state.snake0.headCollidesWith(state.snake1);
		s1dead |= state.snake1.headCollidesWith(state.snake0);
		
		/*
		 * stopping game condition - one of snakes collides with something
		 */
		boolean cont = !(s0dead || s1dead);

		if (!cont) {
			if (s0dead)
				state.setEndResult(results.LOSS);
			else if (s1dead)
				state.setEndResult(results.WIN);
			else if (s0dead && s1dead)
				state.setEndResult(results.TIE);
		}
		state.treeDepth++;
		return state;
	}

	private static Coordinate randomNonOccupiedCell(Snake snake0, Snake snake1, Coordinate mazeSize) {
		Random rnd = new Random();
		while (true) {
			Coordinate c = new Coordinate(rnd.nextInt(mazeSize.x), rnd.nextInt(mazeSize.y));
			if (snake0.elements.contains(c))
				continue;
			if (snake1.elements.contains(c))
				continue;

			return c;
		}
	}
}
