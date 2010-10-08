/*
 * Copyright (C) 2010 Minwoo Jeong (minwoo.j@gmail.com).
 * This file is part of the "bitextOpenIE" distribution.
 * http://github.com/minwoo/bitextOpenIE/
 * This software is provided under the terms of LGPL.
 */

package openie.text;

import gnu.trove.list.array.*;
import gnu.trove.map.hash.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.Logger;

public class Parameter implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private static final int CURRENT_SERIAL_VERSION = 1;

	private transient Logger logger = Logger.getLogger(Parameter.class);

	protected Alphabet inputDict;	// dictionary (i.e. hashmap) for observed features (of both node and edge) 
	protected Alphabet labelDict;	// dictionary (i.e. hashmap) for labeling 
	protected InvertedIndex paramIndex;	// index for (y, x) pair where y is a label and x an input feature; see @InvertedIndex
	protected int[][] edgeIndex;	// index for edge transition (of unitext CRF model), i.e. 1-order chain
	
	protected TDoubleArrayList weight = new TDoubleArrayList();	// weight (parameter) vector
	protected TDoubleArrayList count = new TDoubleArrayList();	// empirical count vector; this would be moved outside of class

	public Parameter () {
		inputDict = new Alphabet();
		labelDict = new Alphabet();
		paramIndex = new InvertedIndex(inputDict, labelDict);
	}
	
	public Parameter (Alphabet inputDict, Alphabet labelDict, InvertedIndex paramIndex) {
		this.inputDict = inputDict;
		this.labelDict = labelDict;
		this.paramIndex = paramIndex;
	}
	
	public void clear () {
		weight.fill(0);
	}
	
	public void reset () {
		inputDict.clear();
		labelDict.clear();
		paramIndex.clear();
		weight.clear();
		count.clear();
	}
	
	public int[] indexing (String label, String[] input, double[] value, boolean isUpdate) {
		int[] ret = new int[input.length + 1];
		
		ret[0] = labelDict.lookup(label, isUpdate);
		for (int i = 0; i < input.length; i++) {
			ret[i+1] = inputDict.lookup(input[i], isUpdate);
			lookup(ret[0], ret[i+1], value[i], isUpdate);
		}
		
		return ret;
	}
		
	public int lookup (int labelId, int inputId, double value, boolean isUpdate) {
		int fid = paramIndex.lookup(labelId, inputId, isUpdate);
		
		if (fid < weight.size()) {
			double newValue = count.get(fid) + value;
			count.set(fid, newValue);
		}
		else if (isUpdate) {
			assert(fid == weight.size());
			weight.add(0);
			count.add(value);
		}
		
		assert(paramIndex.size() == weight.size());
		return fid;
	}
	
	public int indexingEdge (String label, String prevLabel, double value, boolean isUpdate) {
		int labelId = labelDict.lookup(label, isUpdate);
		int featId = inputDict.lookup("@" + prevLabel, isUpdate);
		return lookup(labelId, featId, value, isUpdate);
	}
	
	public void makeEdgeIndex () {
		int N = sizeLabel();
		edgeIndex = new int[N][N];
		for (int i = 0; i < N; i++)
			Arrays.fill(edgeIndex[i], -1); // `-1' indicates null parameter
		
		for (int i = 0; i < N; i++) {
			String labelStr = "@" + labelDict.getObject(i);
			int featId = inputDict.lookup(labelStr, false);
			if (featId >= 0) {
				TIntIntHashMap index = getIndex(featId);
				for (int y : index.keys())
					edgeIndex[y][i] = index.get(y);
			}
		}
	}
	
	public int[] getEdgeIndex (int i) {
		return edgeIndex[i];
	}
	
	public int[][] getEdgeIndex () {
		return edgeIndex;
	}

	public TIntIntHashMap getIndex (int i) {
		return paramIndex.getIndex(i);
	}
	
	public void setWeight(double[] weightArray) {
		assert(weightArray.length == weight.size());
		for (int i = 0; i < weight.size(); i++)
			weight.set(i, weightArray[i]);
	}
	
	public double[] getWeight () {
		return weight.toArray();
	}
	
	public double[] getCount () {
		return count.toArray();
	}
	
	public int[] size () {
		return new int[] { inputDict.size(), labelDict.size(), paramIndex.size() };
	}
	
	public int sizeLabel () {
		return labelDict.size();		
	}
	
	public int sizeInput () {
		return inputDict.size();
	}
	
	public int sizeParam () {
		return paramIndex.size();
	}
	
	public Alphabet[] getAlphabet () {
		return new Alphabet[] {inputDict, labelDict};
	}
	public Alphabet getInputAlphabet () {
		return inputDict;
	}
	
	public Alphabet getLabelAlphabet () {
		return labelDict;
	}

	public InvertedIndex getParamIndex () {
		return paramIndex;
	}
	
	private void writeObject (ObjectOutputStream out) throws IOException {
		out.writeInt(CURRENT_SERIAL_VERSION);
		out.writeObject(inputDict);
		out.writeObject(labelDict);
		out.writeObject(paramIndex);
		
		out.writeInt(weight.size());
		for (int i = 0; i < weight.size(); i++)
			out.writeDouble(weight.get(i));
	}

	private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
		int version = in.readInt();
		inputDict = (Alphabet) in.readObject();
		labelDict = (Alphabet) in.readObject();
		paramIndex = (InvertedIndex) in.readObject();
		
		int size = in.readInt();
		weight = new TDoubleArrayList();
		count = new TDoubleArrayList();
		for (int i = 0; i < size; i++) {
			weight.add(in.readDouble());
			count.add(0);
		}
		
		assert(weight.size() == paramIndex.size());
	}


}
