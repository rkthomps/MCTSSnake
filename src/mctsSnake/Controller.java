package mctsSnake;


/*
  The controller class manages an MCTS search tree
  and responds to queries by the GameSimulator Class.

  The controller gives the best possible moves based on
  UCB to the GameSimulator. 
 */
public class Controller {
    public static final int FORWARD = 0;
    public static final int LEFT = 1;
    public static final int RIGHT = 2;

    public static final int ROW_MOVE = 0;
    public static final int COL_MOVE = 1;
    public static final int LEAF_NODE = 2;
    public static final int RETURN_SIZE = 3;

    private Node rootNode;
    private Node curNode;

    public Controller() {
	this.rootNode = new Node();
	this.curNode = rootNode;
    }

    public Controller(Node rootNode){
	this.rootNode = rootNode;
	this.curNode = rootNode;
    }

    public Node getRoot(){return rootNode;}

    /*
      Change the root of the controller to be one of the root's children
     */
    public void moveRoot(int rowMove, int colMove){
	Node targetNode = rootNode.getChild(rowMove, colMove);
	if (targetNode == null)
	    targetNode = new Node();
	rootNode = targetNode;
	curNode = rootNode;
	rootNode.setParentNull();
    }

    /*
      Return a length 3 array of integers.
      The first element of the array indicates row snake's next move.
      The second element of the array indicates the col snake's next move.
      The third element of the array indicates if the node is a leaf node. 1 if it is a
      leaf node. 0 if it is not a leaf node.

      rowConstraints is an array of three booleans. The first, second, and third
      elements correspond whether or not the rowSnake can mode forward, left, or
      right respectively. colConstraints is the same. 
     */
    public int[] nextMove(boolean[] rowConstraints, boolean[] colConstraints) {
	curNode = curNode.bestChild(rowConstraints, colConstraints);
	int[] returnArr = new int[RETURN_SIZE];
	returnArr[ROW_MOVE] = curNode.getRowIndex();
	returnArr[COL_MOVE] = curNode.getColIndex();
	if (curNode.getNewlyExpanded()){
	    returnArr[LEAF_NODE] = 1;
	    curNode.turnOffNewlyExpanded();
	} else {
	    returnArr[LEAF_NODE] = 0;
	}
	return returnArr;
    }

    public void propogateScore(double score){
	curNode.propogateScore(score);
	curNode = rootNode;
    }

    /*
      For consistancy, returns an array of size ReturnSize. However, the
      last element of the triple is meaningless. The first two elements have
      the same semantics as the array returned by nextMove.
     */
    public int[] getFinalMoves(){
	Node toReturn = rootNode.getBestSuccessor();
	int[] returnArr = new int[RETURN_SIZE];
	returnArr[ROW_MOVE] = toReturn.getRowIndex();
	returnArr[COL_MOVE] = toReturn.getColIndex();
	returnArr[LEAF_NODE] = -1;
	return returnArr;
    }

    /*
      Get String representation of a move given a move index.
     */
    public static String getMove(int index){
	switch(index){
	case FORWARD:
	    return "F";
	case LEFT:
	    return "L";
	case RIGHT:
	    return "R";
	default:
	    return " ";
	}
    }
}
