package test;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.HashSet;

import NashCalc.*;

// run with java org.junit.runner.JUnitCore TestNashCalc

public class TestNashCalc{
    
    @Test
    public void testArgSorter1() {
	double[] myArray = {0.3, 30.45, 11.2, 10.1};
	int[] argSortedArray = ArgSorter.argSort(myArray);
	int[] argSortedExpected = {0, 3, 2, 1};
	assertArrayEquals(argSortedArray, argSortedExpected);
    }

    @Test
    public void testArgSorter2() {
	double[] myArray = {0.3, 30.45, 11.2, 10.1, 15.6};
	int[] argSortedArray = ArgSorter.argSort(myArray);
	int[] argSortedExpected = {0, 3, 2, 4, 1};
	assertArrayEquals(argSortedArray, argSortedExpected);
    }

    @Test
    public void testArgSorter3() {
	double[] myArray = {0.3};
	int[] argSortedArray = ArgSorter.argSort(myArray);
	int[] argSortedExpected = {0};
	assertArrayEquals(argSortedArray, argSortedExpected);
    }

    @Test
    public void testGetDominatingRows(){
	double[][] examplePayoff = {
	    {3, 3, 4, 3, 3},
	    {4, 4, 3, 4, 4},
	    {5, 6, 5, 6, 5},
	    {6, 5, 6, 5, 6}
	};
	HashSet<Integer> expected = new HashSet<>();
	expected.add(2);
	expected.add(3);
	HashSet<Integer> actual = NashCalculator.getDominatingRows(examplePayoff);
	assertTrue(expected.equals(actual));
    }

    

    @Test
    public void testDominatedStrategies() {
	double[][] examplePayoff = {
	    {3, 3, 3, 3, 3},
	    {4, 4, 4, 4, 4},
	    {5, 5, 5, 5, 5},
	    {6, 6, 6, 6, 6}
	};
	LabelledPayoff prunedPayoff = NashCalculator.pruneDominatedStrategies(examplePayoff);
	assertEquals(prunedPayoff.getPayoff().length, 1);
	assertEquals(prunedPayoff.getPayoff()[0].length, 1);
	assertEquals(prunedPayoff.getPayoff()[0][0], 6, 1e-10);
    }

    @Test
    public void testDominatedStrategies2() {
	double[][] examplePayoff = {
	    {7, 8, 7, 8, 44},
	    {8, 7, 8, 7, 33},
	    {9, 10, 9, 10, 22},
	    {10, 9, 10, 9, 11}
	};
	double[][] expected = {
	    {9, 10},
	    {10, 9}
	};
	LabelledPayoff prunedPayoff = NashCalculator.pruneDominatedStrategies(examplePayoff);
	assertTrue(Util.arrEquals(expected, prunedPayoff.getPayoff(), 1e-10));
    }


    @Test
    public void testNashCalcHeadsTails() {
	double[][] payoff = {
	    {-1, 1},
	    {1, -1}
	};
	Equilibrium eq = NashCalculator.getEquilibrium(payoff);
	System.out.println(eq.toString());
    }

    @Test
    public void testNashStress(){
	int testRows = 100;
	int testCols = 100;
	int numSims = 10000;
	long totalNanos = 0;
	for (int sim = 0; sim < numSims; sim++){
	    double[][] payoff = new double[testRows][testCols];
	    for (int i = 0; i < payoff.length; i++)
		for (int j = 0; j < payoff[0].length; j++)
		    payoff[i][j] = Math.random();
	    long startTime = System.nanoTime();
	    Equilibrium eq = NashCalculator.getEquilibrium(payoff);
	    long endTime = System.nanoTime();
	    totalNanos += (endTime - startTime);
	}
	System.out.println(String.format("Average time taken for %d by %d payoff matrix over %d trials: %f seconds",
					 testRows, testCols, numSims, (float)totalNanos / (numSims * 10e9)));
    }
}
