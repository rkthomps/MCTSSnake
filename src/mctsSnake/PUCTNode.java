package mctsSnake;

import java.util.Collections;
import java.util.ArrayList;

import NashCalc.Util;
import NashCalc.Equilibrium;
import NashCalc.NashCalculator;

/*
  This node is very similar to Node.java except that it stores
  the PRIORS at every node as well. It stores the sum of the distances
  to the apple from the node. That way, the prior is the average of the
  distances from each of the games
 */

public class PUCTNode {
    public static int NUM_OPTIONS = 3;
    public static double EXPLORE = 1.0;
    public static double SOFT_STRENGTH = 1.0;
    
    private PUCTNode parent; // Which node is this node's parent?
    private int rowIndex; // What is the row index of this node in the parent?
    private int colIndex; // What is the col index of this node in the parent?
    private PUCTNode[][] children = new PUCTNode[NUM_OPTIONS][NUM_OPTIONS];
    private double rowLogits[] = new double[NUM_OPTIONS]; // Default value is zero
    private double colLogits[] = new double[NUM_OPTIONS];
    private long visits = 0;
    private double score = 0;
    private boolean newlyExpanded = true; // Controller has to know. 
    private ArrayList<Integer> expandQueue = new ArrayList<>();

    public PUCTNode(){
	this.parent = null;
	this.rowIndex = -1;
	this.colIndex = -1;
	for (int i = 0; i < (NUM_OPTIONS * NUM_OPTIONS); i++){
	    expandQueue.add(i);
	}
	Collections.shuffle(expandQueue);

    }

    public PUCTNode(PUCTNode parent, int rowIndex, int colIndex){
	this.parent = parent;
	this.rowIndex = rowIndex;
	this.colIndex = colIndex;
	for (int i = 0; i < (NUM_OPTIONS * NUM_OPTIONS); i++){
	    expandQueue.add(i);
	}
	Collections.shuffle(expandQueue);
    }


    // --------GETTERS--------
    // =======================
    public int getExpandLength(){return expandQueue.size();}
    public int getRowIndex(){return rowIndex;}
    public int getColIndex(){return colIndex;}
    public double getScore(){return score;}
    public long getVisits(){return visits;}
    public double[] getRowLogits() {return rowLogits;}
    public double[] getColLogits() {return colLogits;}
    public PUCTNode getChild(int rowMove, int colMove){
	return children[rowMove][colMove];
    }
    public void setParentNull(){
	parent = null;
    }
    public void turnOffNewlyExpanded(){newlyExpanded = false;}
    public boolean getNewlyExpanded(){return newlyExpanded;}


    /*
      Calculate UCB for the row snake and col snake
      weighting the calculation by the nash equilibrium
      policy for the other snake.

      rowConstraints is a boolean array. True means the snake can go to the row.
      False means the snake cannot go to the row.

      This function also recieves row and col distances

      I am not using a hash map because contraint arrays will not be larger
      than size three.
     */
    public PUCTNode bestChild(boolean[] rowConstraints, boolean[] colConstraints,
			  double[] rowDistances, double[] colDistances){
	rowLogits = Util.addArrs(rowLogits, rowDistances);
	colLogits = Util.addArrs(colLogits, colDistances);
	
	for (int i = 0; i < expandQueue.size(); i++){
	    int toExpand = expandQueue.get(i);
	    int nodeRow = toExpand / NUM_OPTIONS;
	    int nodeCol = toExpand % NUM_OPTIONS;
	    if (!rowConstraints[nodeRow])
		continue;
	    if (!colConstraints[nodeCol])
		continue;
	    PUCTNode newNode = new PUCTNode(this, nodeRow, nodeCol);
	    children[nodeRow][nodeCol] = newNode;
	    expandQueue.remove(i);
	    return newNode;
	}

	// Find the rows and columns that are unconstrained
	int payoffRows = 0;
	int payoffCols = 0;
	int[] validRows = new int[NUM_OPTIONS];
	int[] validCols = new int[NUM_OPTIONS];
	for (int i = 0; i < rowConstraints.length; i++){
	    if (rowConstraints[i]){
		validRows[payoffRows] = i;
		payoffRows++;
	    }
	}
	for (int i = 0; i < colConstraints.length; i++){
	    if (colConstraints[i]){
		validCols[payoffCols] = i;
		payoffCols++;
	    }
	}

	// Create payoff and visit matricies from children
	double[][][] payoffVisits = getPayoffVisit(validRows, validCols,
						   payoffRows, payoffCols);
	double[] constrainedRowLogits = getLogits(rowLogits, validRows, payoffRows);
	double[] constrainedColLogits = getLogits(colLogits, validCols, payoffCols);
	
	double[][] payoff = payoffVisits[0];
	double[][] visits = payoffVisits[1];
	
	Equilibrium eq = NashCalculator.getEquilibrium(payoff);
	double[] rowExpected = Util.matmul(payoff, eq.getP2Policy());
	double[] colExpected = Util.matmul(Util.transposeNegate(payoff),
					   eq.getP1Policy());
	double[] rowExplore = Util.calcPUCTExplore(visits, constrainedRowLogits, this.visits,
						   EXPLORE, SOFT_STRENGTH);
	double[] colExplore = Util.calcPUCTExplore(Util.transpose(visits), constrainedColLogits, this.visits,
						   EXPLORE, SOFT_STRENGTH);
	
	int nextRow = Util.randomArgMax(Util.addArrs(rowExpected, rowExplore));
	int nextCol = Util.randomArgMax(Util.addArrs(colExpected, colExplore));

	// Calculate UCB
	return children[validRows[nextRow]][validCols[nextCol]];
    }


    /*
      Propogate the given score back up to the root
      of the search tree. 
     */
    public void propogateScore(double score){
	this.score += score;
	this.visits += 1;
	if (parent == null){
	    return;
	}
	parent.propogateScore(score);
    }

    
    /*
      Gets the final best move for the rowsnake and the colsnake
    */
    public PUCTNode getBestSuccessor(){
	int[] validRows = new int[NUM_OPTIONS];
	int[] validCols = new int[NUM_OPTIONS];
	int payoffRows = 0;
	int payoffCols = 0;

	for (int i = 0; i < NUM_OPTIONS; i++){
	    boolean rowValid = false;
	    boolean colValid = false;
	    for (int j = 0; j < NUM_OPTIONS; j++){
		if (children[i][j] != null)
		    rowValid = true;
		if (children[j][i] != null)
		    colValid = true;
	    }
	    if (rowValid){
		validRows[payoffRows] = i;
		payoffRows++;
	    }
	    if (colValid){
		validCols[payoffCols] = i;
		payoffCols++;
	    }
	}

	// Create payoff and visit matricies from children
	double[][][] payoffVisits = getPayoffVisit(validRows, validCols,
						   payoffRows, payoffCols);
	double[][] payoff = payoffVisits[0];
	double[][] visits = payoffVisits[1];

	/*
	Equilibrium eq = NashCalculator.getEquilibrium(payoff);
	double[] rowExpected = Util.matmul(payoff, eq.getP2Policy());
	double[] colExpected = Util.matmul(Util.transposeNegate(payoff),
					   eq.getP1Policy());
	*/
	double[] rowVisits = Util.sumAcross(visits);
	double[] colVisits = Util.sumOver(visits);
	
	int nextRow = Util.randomArgMax(rowVisits);
	int nextCol = Util.randomArgMax(colVisits);

	return children[validRows[nextRow]][validCols[nextCol]];
    }



    /*
      ToString Method to print the search tree for debugging. 
     */
    public String toString(int depth){
	return toStringHelper("", depth);
    }


    private String toStringHelper(String indent, int depth){
	if (depth == 0){
	    return indent + "...\n";
	}
	
	String rep = "";
	rep += Controller.getMove(rowIndex);
	rep += Controller.getMove(colIndex);

	String totalRep = String.format(indent + rep + "; %5d Visits; %3.3f Score\n",
					visits, score);
	String nextIndent = indent + "|  ";

	for (int i = 0; i < NUM_OPTIONS; i++){
	    for (int j = 0; j < NUM_OPTIONS; j++){
		if (children[i][j] == null){
		    totalRep += (nextIndent + "Unvisited.\n");
		} else {
		    totalRep += (children[i][j].toStringHelper(nextIndent, depth - 1));
		}
	    }
	}
	return totalRep;
    }

    
    /*
      Gets the payoff and visit matricies for this node
     */
    private double[][][] getPayoffVisit(int[] validRows, int[] validCols,
					int payoffRows, int payoffCols){
	double[][][] payoffVisit = new double[2][payoffRows][payoffCols];
	PUCTNode curChild;
	for (int i = 0; i < payoffRows; i++){
	    for (int j = 0; j < payoffCols; j++){
		curChild = children[validRows[i]][validCols[j]];
		payoffVisit[0][i][j] = curChild.getScore() /
		    curChild.getVisits();
		payoffVisit[1][i][j] = curChild.getVisits();
	    }
	}
	return payoffVisit;
    }

    private double[] getLogits(double[] logits, int[] validMoves, int numMoves){
	double[] logitResults = new double[numMoves];
	for (int i = 0; i < numMoves; i++){
	    logitResults[i] = logits[validMoves[i]];
	}
	return logitResults;
    }
}

    
    
