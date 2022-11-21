
package NashCalc;

// Streams
import java.io.PrintStream;
import java.io.OutputStream;

// Data Structures 
import java.util.HashSet;
import java.util.Arrays;
import java.util.ArrayList;

//Simplex Solvers
import it.ssc.pl.milp.GoalType;
import it.ssc.pl.milp.Constraint;
import it.ssc.pl.milp.LinearObjectiveFunction;
import it.ssc.pl.milp.ConsType;
import it.ssc.pl.milp.LP;
import it.ssc.pl.milp.SolutionType;
import it.ssc.pl.milp.Solution;
import it.ssc.pl.milp.Variable;

// Google Simplex Solver


public class NashCalculator{

    /**
       Finds the Nash Equilibrium of the given payoff matrix. Assumes
       the payoff matrix is for a zero-sum game, and the rewards are for
       player-one whose actions are on the first axis. 
     */
    public static Equilibrium getEquilibrium(double[][] payoff){
	// We do not need to consider any actions that are dominated.
	PrintStream originalOut = surpressOut();
	PrintStream originalErr = surpressErr();
	LabelledPayoff prunedPayoff = pruneDominatedStrategies(payoff);
	double[] rowPlayerResult;
	double[] colPlayerResult;
	try {
	    rowPlayerResult = computeEquilibrium(prunedPayoff.getPayoff());
	    colPlayerResult = computeEquilibrium(Util.transposeNegate(prunedPayoff.getPayoff()));
	} catch (Exception e){
	    System.out.println("Coult not compute NASH EQ");
	    double[] defaultRowPolicy = new double[payoff.length];
	    double[] defaultColPolicy = new double[payoff[0].length];
	    return new Equilibrium(defaultRowPolicy, defaultColPolicy, 0, 0);
	}
	// Parse results for row player
	double[] rowPolicy = new double[payoff.length];
	double rowReward = rowPlayerResult[rowPlayerResult.length - 1];
	for (int i = 0; i < rowPolicy.length; i++)
	    rowPolicy[i] = 0;
	for (int i = 0; i < rowPlayerResult.length - 1; i++)
	    rowPolicy[prunedPayoff.getRowLabels()[i]] = rowPlayerResult[i];

	//Parse results for col player
	double[] colPolicy = new double[payoff[0].length];
	double colReward = colPlayerResult[colPlayerResult.length - 1];
	for (int i = 0; i < colPolicy.length; i++)
	    colPolicy[i] = 0;
	for (int i = 0; i < colPlayerResult.length - 1; i++)
	    colPolicy[prunedPayoff.getColLabels()[i]] = colPlayerResult[i];

	enableOut(originalOut);
	enableErr(originalErr);
	return new Equilibrium(rowPolicy, colPolicy, rowReward, colReward);
    }


    /**
       Computes the equallibrium with respect to the row player
     */
    public static double[] computeEquilibrium(double[][] payoff) throws Exception{
	double posConstant = 10e5;
	// Set Optimization Vector
	double[] c = new double[payoff.length + 1];
	for (int i = 0; i < c.length; i++)
	    c[i] = 0;
	c[c.length - 1] = 1;
	System.out.println("Coeffs: " + Arrays.toString(c));

	// Set Constraint Matrix
	double[][] a = new double[payoff[0].length + 1][payoff.length + 1];
	for (int i = 0; i < payoff.length; i++)
	    for (int j = 0; j < payoff[0].length; j++)
		a[j][i] = -1 * (payoff[i][j] + posConstant);
	for (int i = 0; i < a.length; i++)
	    a[i][a[0].length - 1] = 1;
	for (int i = 0; i < a[0].length; i++)
	    a[a.length - 1][i] = 1;
	a[a.length - 1][a[0].length - 1] = 0;
	System.out.println("Constraints: " + Arrays.deepToString(a));


	// Set Constraint Vector
	double[] b = new double[payoff[0].length + 1];
	for (int i = 0; i < b.length; i++)
	    b[i] = 0;
	b[b.length - 1] = 1;
	System.out.println("Constraint vec: " + Arrays.toString(b));

	// Set up optimization
	LinearObjectiveFunction goal = new LinearObjectiveFunction(c, GoalType.MAX);
	   
	ArrayList<Constraint> constraints = new ArrayList<Constraint>();
	for (int i = 0; i < a.length; i++)
	    constraints.add(new Constraint(a[i], ConsType.LE, b[i]));
	LP lp = new LP(goal, constraints);

	// Solve optimization
	SolutionType solutionType = lp.resolve();
	double[] result = new double[payoff.length + 1];
	int resultIndex = 0;

	if (solutionType==SolutionType.OPTIMUM) {
	    Solution solution = lp.getSolution();
	    for (Variable var:solution.getVariables()){
		result[resultIndex] = var.getValue();
		resultIndex++;
	    }
	} else {
	    System.out.println("OPTIMIZER COULD NOT FIND NASH EQ");
	}
	result[result.length - 1] -= posConstant;
	System.out.println("Result:" + Arrays.toString(result));
	return result;
    }

    /**
       Prune the dominated row and column strategies from the payoff matrix. 
     */
    public static LabelledPayoff pruneDominatedStrategies(double[][] payoff){
	boolean prunedRows = true;
	boolean prunedCols = true;
	LabelledPayoff curPayoff = new LabelledPayoff(Util.copy(payoff),
						      Util.ordered(payoff.length),
						      Util.ordered(payoff[0].length));
	while (prunedRows || prunedCols){
	    prunedRows = false;
	    prunedCols = false;
	    HashSet<Integer> dominatingRows = getDominatingRows(curPayoff.getPayoff());
	    if (dominatingRows.size() < curPayoff.numRows()){
		double[][] newPayoff = new double[dominatingRows.size()][curPayoff.numCols()];
		int[] rowLabels = new int[newPayoff.length];
		int curRow = 0;
		for (int row: dominatingRows){
		    rowLabels[curRow] = curPayoff.getRowLabels()[row];
		    for (int j = 0; j < newPayoff[0].length; j++)
			newPayoff[curRow][j] = curPayoff.getPayoff()[row][j];
		    curRow++; 
		}
		curPayoff.setPayoff(newPayoff);
		curPayoff.setRowLabels(rowLabels);
		prunedRows = true;
	    }

	    HashSet<Integer> dominatingCols = getDominatingCols(curPayoff.getPayoff());
	    if (dominatingCols.size() < curPayoff.numCols()){
		double[][] newPayoff = new double[curPayoff.numRows()][dominatingCols.size()];
		int[] colLabels = new int[newPayoff[0].length];
		int curCol = 0;
		for (int col: dominatingCols){
		    colLabels[curCol] = curPayoff.getColLabels()[col];
		    for (int j = 0; j < newPayoff.length; j++){
			newPayoff[j][curCol] = curPayoff.getPayoff()[j][col];
		    }
		    curCol++; 
		}
		curPayoff.setPayoff(newPayoff);
		curPayoff.setColLabels(colLabels);
		prunedCols = true;
	    }
	}
	return curPayoff;
    }

    
    /**
       Finds and returns rows in the payoff matrix that are dominated. 
     */
    public static HashSet<Integer> getDominatingRows(double[][] payoff){
	int[][] colIndices = new int[payoff[0].length][payoff.length];
	
	for (int i = 0; i < payoff[0].length; i++){
	    // Get array to sort
	    double[] toSort = new double[payoff.length];
	    for (int j = 0; j < payoff.length; j++)
		toSort[j] = payoff[j][i];
	    // Fill sorted column indices
	    int[] sortedIndices = ArgSorter.argSort(toSort);
	    for (int j = 0; j < payoff.length; j++)
		colIndices[i][j] = sortedIndices[j];
	}

	HashSet<Integer> dominators = new HashSet<>();
	int curRow = colIndices[0].length - 1;
	do {
	    for (int i = 0; i < colIndices.length; i++)
		dominators.add(colIndices[i][curRow]);
	    curRow++;
	} while (dominators.size() > curRow);
	return dominators;
    }

    
    /**
       Finds and returns columns in the payoff matrix that are dominated.
       This just transposes the matrix and calls getDominatedRows.
    */
    public static HashSet<Integer> getDominatingCols(double[][] payoff){
	return getDominatingRows(Util.transposeNegate(payoff));
    }

    private static PrintStream surpressOut(){
	PrintStream original = System.out;
	OutputStream newOut = new OutputStream(){public void write(int b){}};
	PrintStream newPrint = new PrintStream(newOut);
	System.setOut(newPrint);
	return original;
    }

    private static PrintStream surpressErr(){
	PrintStream original = System.err;
	OutputStream newOut = new OutputStream(){public void write(int b){}};
	PrintStream newPrint = new PrintStream(newOut);
	System.setErr(newPrint);
	return original;
    }


    private static void enableOut(PrintStream original){
	System.setOut(original);
    }

    private static void enableErr(PrintStream original){
	System.setErr(original);
    }
}
