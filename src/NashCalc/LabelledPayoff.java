

package NashCalc;

public class LabelledPayoff {

    private double[][] payoffMatrix;
    private int[] rowLabels;
    private int[] colLabels;

    public LabelledPayoff(double[][] payoffMatrix, int[] rowLabels, int[] colLabels){
	this.payoffMatrix = payoffMatrix;
	this.rowLabels = rowLabels;
	this.colLabels = colLabels;
    }

    public double[][] getPayoff(){return payoffMatrix;}
    public int[] getRowLabels(){return rowLabels;}
    public int[] getColLabels(){return colLabels;}
    public int numRows(){return payoffMatrix.length;}
    public int numCols(){return payoffMatrix[0].length;}
    

    public void setPayoff(double[][] payoffMatrix){
	this.payoffMatrix = payoffMatrix;
    }

    public void setRowLabels(int[] rowLabels){
	this.rowLabels = rowLabels;
    }

    public void setColLabels(int[] colLabels){
	this.colLabels = colLabels;
    }
}

    
