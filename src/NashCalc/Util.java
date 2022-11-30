

package NashCalc;

public class Util {


    public static int[] ordered(int size){
	int[] arr = new int[size];
	for (int i = 0; i < size; i++)
	    arr[i] = i;
	return arr;
    }


    public static double[][] copy(double[][] arr){
	double[][] copy = new double[arr.length][arr[0].length];
	for (int i = 0; i < arr.length; i++){
	    for (int j = 0; j < arr[0].length; j++){
		copy[i][j] = arr[i][j];
	    }
	}
	return copy;
    }

    public static boolean arrEquals(double[][] arr1, double[][] arr2, double delta){
	if (arr1.length != arr2.length) return false;
	if (arr1.length == 0) return true;
	if (arr1[0].length != arr2[0].length) return false;

	for (int i = 0; i < arr1.length; i++){
	    for (int j = 0; j < arr1[0].length; j++){
		if (Math.abs(arr1[i][j] - arr2[i][j]) > delta) return false;
	    }
	}
	return true;
    }

     public static double[][] transpose(double[][] arr){
	double[][] transposed = new double[arr[0].length][arr.length];
	for (int i = 0; i < arr.length; i++)
	    for (int j = 0; j < arr[0].length; j++)
		transposed[j][i] = arr[i][j];
	return transposed;

    }

    public static long[][] transpose(long[][] arr){
	long[][] transposed = new long[arr[0].length][arr.length];
	for (int i = 0; i < arr.length; i++)
	    for (int j = 0; j < arr[0].length; j++)
		transposed[j][i] = arr[i][j];
	return transposed;
    }


    public static double[][] transposeNegate(double[][] arr){
	double[][] transposed = new double[arr[0].length][arr.length];
	for (int i = 0; i < arr.length; i++)
	    for (int j = 0; j < arr[0].length; j++)
		transposed[j][i] = -1 * arr[i][j];
	return transposed; 
    }

    public static double[] matmul(double[][] arr1, double[] arr2){
	double[] result = new double[arr1.length];
	for (int i = 0; i < arr1.length; i++){
	    result[i] = 0;
	    for (int j = 0; j < arr2.length; j++){
		result[i] += (arr1[i][j] * arr2[j]);
	    }
	}
	return result;
    }

    public static int randomArgMax(double[] arr){
	int[] candidates = new int[arr.length];
	double curMax = -1e10;
	int numEq = 0;
	for (int i = 0; i < arr.length; i++){
	    if (arr[i] > curMax){
		curMax = arr[i];
		candidates[0] = i;
		numEq = 1;
	    } else if (arr[i] == curMax){
		candidates[numEq] = i;
		numEq += 1;
	    } 
	}
	return candidates[(int)(Math.random() * numEq)];
    }

    public static double[] calcExplore(double[][] visits, long parentVisits, double exploreConst){
	double[] exploreTerms = new double[visits.length];
	double childVisits;
	for (int i = 0; i < visits.length; i++){
	    childVisits = 0;
	    for (int j = 0; j < visits[0].length; j++){
		childVisits += visits[i][j];
	    }
	    exploreTerms[i] = exploreConst * Math.sqrt(2 * Math.log(parentVisits) / childVisits);
	}
	return exploreTerms;
    }


    /*
      This is not actually softmax, there is some modification to the logits
      # TODO : -1 times logit
     */
    public static double[] softmax(double[] logits, long parentVisits, double softStrength){
	double eulerSum = 0;
	for (int i = 0; i < logits.length; i++){
	    eulerSum += Math.exp(-1 * softStrength * logits[i] / parentVisits);
	}
	double[] weights = new double[logits.length];
	for (int i = 0; i < logits.length; i++)
	    weights[i] = Math.exp(-1 * softStrength * logits[i] / parentVisits) / eulerSum;
	return weights;
    }

    
    public static double[] calcPUCTExplore(double[][] visits, double[] logits, long parentVisits,
					   double exploreConst, double softStrength){
	double[] weights = softmax(logits, parentVisits, softStrength);
	double[] exploreTerms = new double[visits.length];
	double childVisits;
	for (int i = 0; i < visits.length; i++){
	    childVisits = 0;
	    for (int j = 0; j < visits[0].length; j++){
		childVisits += visits[i][j];
	    }
	    exploreTerms[i] = exploreConst * weights[i] * Math.sqrt(parentVisits) / (childVisits + 1);
	}
	return exploreTerms;
    }

    
    public static double[] addArrs(double[] arr1, double[] arr2){
	double[] returnArr = new double[arr1.length];
	for (int i = 0; i < arr1.length; i++){
	    returnArr[i] = arr1[i] + arr2[i];
	}
	return returnArr;
    }

    /*
      Sum along the first axis
     */
    public static double[] sumAcross(double[][] arr){
	double[] sum = new double[arr.length];
	for (int i = 0; i < arr.length; i++){
	    for (int j = 0; j < arr[0].length; j++){
		sum[i] += arr[i][j];
	    }
	}
	return sum;
    }

    /*
      Sum along the zeroth axis
     */
    public static double[] sumOver(double[][] arr){
	double[] sum = new double[arr[0].length];
	for (int i = 0; i < arr.length; i++){
	    for (int j = 0; j < arr[0].length; j++){
		sum[j] += arr[i][j];
	    }
	}
	return sum;
    }

}
