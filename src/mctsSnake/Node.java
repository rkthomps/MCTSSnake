package mctsSnake;

import java.util.Collections;
import java.util.ArrayList;

import NashCalc.Util;
import NashCalc.Equilibrium;
import NashCalc.NashCalculator;

/*
  By convention, we will say that the row snake winning
  corresponds to a score of 1 and the col snake winning
  corresponds to a score of -1
 */

public class Node {
    public static int NUM_OPTIONS = 3;
    
    private Node parent; // Which node is this node's parent?
    private int rowIndex; // What is the row index of this node in the parent?
    private int colIndex; // What is the col index of this node in the parent?
    private Node[][] children = new Node[NUM_OPTIONS][NUM_OPTIONS];
    private long visits = 0;
    private double score = 0;
    private boolean newlyExpanded = true; // Controller has to know. 
    private ArrayList<Integer> expandQueue = new ArrayList<>();
    

    public Node(){
	this.parent = null;
	this.rowIndex = -1;
	this.colIndex = -1;
	for (int i = 0; i < (NUM_OPTIONS * NUM_OPTIONS); i++){
	    expandQueue.add(i);
	}
	Collections.shuffle(expandQueue);

    }

    public Node(Node parent, int rowIndex, int colIndex){
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
    public Node getChild(int rowMove, int colMove){
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


      I am not using a hash map because contraint arrays will not be larger
      than size three
     */
    public Node bestChild(boolean[] rowConstraints, boolean[] colConstraints){
	
	for (int i = 0; i < expandQueue.size(); i++){
	    int toExpand = expandQueue.get(i);
	    int nodeRow = toExpand / NUM_OPTIONS;
	    int nodeCol = toExpand % NUM_OPTIONS;
	    if (!rowConstraints[nodeRow])
		continue;
	    if (!colConstraints[nodeCol])
		continue;
	    Node newNode = new Node(this, nodeRow, nodeCol);
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
	double[][] payoff = payoffVisits[0];
	double[][] visits = payoffVisits[1];
	
	Equilibrium eq = NashCalculator.getEquilibrium(payoff);
	double[] rowExpected = Util.matmul(payoff, eq.getP2Policy());
	double[] colExpected = Util.matmul(Util.transposeNegate(payoff),
					   eq.getP1Policy());
	double[] rowExplore = Util.calcExplore(visits, this.visits, 1.0);
	double[] colExplore = Util.calcExplore(Util.transpose(visits),
					       this.visits, 1.0);
	
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
    public Node getBestSuccessor(){
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
	
	Equilibrium eq = NashCalculator.getEquilibrium(payoff);
	double[] rowExpected = Util.matmul(payoff, eq.getP2Policy());
	double[] colExpected = Util.matmul(Util.transposeNegate(payoff),
					   eq.getP1Policy());
	
	int nextRow = Util.randomArgMax(rowExpected);
	int nextCol = Util.randomArgMax(colExpected);

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
	Node curChild;
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
}

    
    
