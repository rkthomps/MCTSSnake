
package NashCalc;

public class ArgSorter{
    /**
      Merge Method for argSort
     */
    private static int[] argMerge(double[] arr, int[] indexArr1,
				     int[] indexArr2){
	int i = 0;
	int j = 0;
	int k = 0;
	int[] indexMerged = new int[indexArr1.length + indexArr2.length];
	
	while ((i < indexArr1.length) && (j < indexArr2.length)){
	    if (arr[indexArr1[i]] < arr[indexArr2[j]]){
		indexMerged[k] = indexArr1[i];
		i++;
	    }
	    else {
		indexMerged[k] = indexArr2[j];
		j++;
	    }
	    k++;
	}
	while (i < indexArr1.length) {
	    indexMerged[k] = indexArr1[i];
	    i++;
	    k++;
	}
	while (j < indexArr2.length){
	    indexMerged[k] = indexArr2[j];
	    j++;
	    k++;
	}
	return indexMerged;
    }
    


    /**
      Helper method for argSort
     */
    private static int[] argSortHelper(double[] arr,
					  int[] indexArr){
	if (indexArr.length <= 1)
	    return indexArr;
	
	int mid = indexArr.length / 2;
	int[] left = new int[mid];
	int[] right = new int[indexArr.length - mid];
	
	for (int i = 0; i < left.length; i++)
	    left[i] = indexArr[i];
	for (int i = 0; i < right.length; i++)
	    right[i] = indexArr[i + mid];

	int[] sortedLeft = argSortHelper(arr, left);
	int[] sortedRight = argSortHelper(arr, right);
	int[] sorted = argMerge(arr, sortedLeft, sortedRight);
	return sorted;
    }
					  
    

    /**
      Returns the sorted indices of the given array
     */
    public static int[] argSort(double[] arr){
	int[] indexArr = new int[arr.length];
	for (int i = 0; i < indexArr.length; i++)
	    indexArr[i] = i;
	return argSortHelper(arr, indexArr);
    }
}
