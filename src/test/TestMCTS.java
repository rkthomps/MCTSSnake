
package test;

import mctsSnake.*;

class TestMCTS {

    /*
      This method rewards snake 1 for going forward eight times,
      and snake 2 for going left eight times.
      Therefore, it is expected that snake 1 goes down after
      many rollouts, and snake 2 goes right after many rollouts.
     */
    public static void runPathologicalSnakeTest(){
	Node root = new Node();
	Controller controller = new Controller(root);
	int numSims = 200;
	int numForwards = 2;
	int numLefts = 2;
	int terminalDepth = 20;
	int[] nextMove = new int[Controller.RETURN_SIZE];
	boolean[] rowConstraints = {false, true, true};
	boolean[] colConstraints = {true, false, true};

	int depthLeft;
	int forwardsLeft;
	int leftsLeft;
	int rowScore;
	int colScore;
	boolean defaultPolicy;

	for(int i = 0; i < numSims; i++){
	    depthLeft = terminalDepth;
	    forwardsLeft = numForwards;
	    leftsLeft = numLefts;
	    rowScore = 0;
	    colScore = 0;
	    defaultPolicy = false;

	    while (depthLeft > 0){
		if (defaultPolicy){
		    nextMove = new int[Controller.RETURN_SIZE];
		    nextMove[Controller.ROW_MOVE] = (int)(Math.random() * 3);
		    nextMove[Controller.ROW_MOVE] = (int)(Math.random() * 3);
		    nextMove[Controller.LEAF_NODE] = 1;
		} else {
		    nextMove = controller.nextMove(rowConstraints, colConstraints);
		}

		if (nextMove[Controller.ROW_MOVE] == Controller.FORWARD){
		    forwardsLeft--;
		    if (forwardsLeft == 0)
			rowScore = 1;
		} else {
		    forwardsLeft = terminalDepth + 1;
		}

		if (nextMove[Controller.COL_MOVE] == Controller.LEFT){
		    leftsLeft--;
		    if (leftsLeft == 0)
			colScore = 1;
		} else {
		    leftsLeft = terminalDepth + 1;
		}

		if (nextMove[Controller.LEAF_NODE] == 1)
		    defaultPolicy = true;

		if ((rowScore > 0) || (colScore > 0)){
		    controller.propogateScore(rowScore - colScore);
		    break;
		}
		depthLeft--;
	    }

	    if (depthLeft == 0){
		controller.propogateScore(0);
	    }
	}
	int[] finalMoves = controller.getFinalMoves();
	System.out.println(controller.getRoot().toString(3));
	System.out.println(String.format("Row Snake Move: %d", finalMoves[Controller.ROW_MOVE]));
	System.out.println(String.format("Col Snake Move: %d", finalMoves[Controller.COL_MOVE]));
    }

    public static void main(String[] args){
	// Row snake should go forward (0)
	// Col snake should go left (1)
	runPathologicalSnakeTest();
    }
}
