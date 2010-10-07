/*
 * Copyright (C) 2010 Minwoo Jeong (minwoo.j@gmail.com).
 * This file is part of the "bitextOpenIE" distribution.
 * http://github.com/minwoo/bitextOpenIE/
 * This software is provided under the terms of LGPL.
 */

package openie.util;

import java.util.Arrays;

public class Stat {

	public static void fillMatrix (double[][] matrix, double val) {
		for (int i = 0; i < matrix.length; i++) 
			Arrays.fill(matrix[i], val);
	}
	
	public static double[][] createMatrix (int n, int m, double val) {
		double[][] matrix = new double[n][m];
		fillMatrix(matrix, val);
		return matrix;
	}
	
	public static double[] createVector (int n, double val) {
		double[] vector = new double[n];
		Arrays.fill(vector, val);
		return vector;
	}
}
