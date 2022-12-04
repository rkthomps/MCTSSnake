package mctsSnake;


/*
  This code is very similar to Controller.java
  except it uses Naive to make its decisions.


  TODO: HAVENT LOOKED AT THIS FILE
 */
public class NaiveController {
    public static final int FORWARD = 0;
    public static final int LEFT = 1;
    public static final int RIGHT = 2;

    public static final int ROW_MOVE = 0;
    public static final int COL_MOVE = 1;
    public static final int LEAF_NODE = 2;
    public static final int RETURN_SIZE = 3;

    private NaiveNode rootNode;
    private NaiveNode curNode;

    public NaiveController() {
	this.rootNode = new NaiveNode();
	this.curNode = rootNode;
    }

    public NaiveController(NaiveNode rootNode){
	this.rootNode = rootNode;
	this.curNode = rootNode;
    }

    public NaiveNode getRoot(){return rootNode;}

    /*
      Change the root of the controller to be one of the root's children
     */
    public void moveRoot(int rowMove, int colMove){
	NaiveNode targetNode = rootNode.getChild(rowMove, colMove);
	if (targetNode == null)
	    targetNode = new NaiveNode();
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

      rowDistances and colDistances are used to calculate the "prior" for each
      node i.e.) should the snake favor moving right, left, or foward from the jump?
     */
    public int[] nextMove(boolean[] rowConstraints, boolean[] colConstraints,
			  double[] rowDistances, double[] colDistances) {
	curNode = curNode.bestChild(rowConstraints, colConstraints,
				    rowDistances, colDistances);
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
	NaiveNode toReturn = rootNode.getBestSuccessor();
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
