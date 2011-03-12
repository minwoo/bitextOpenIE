/*
 * Copyright (C) 2010 Minwoo Jeong (minwoo.j@gmail.com).
 * This file is part of the "bitextOpenIE" distribution.
 * http://github.com/minwoo/bitextOpenIE/
 * This software is provided under the terms of LGPL.
 */

package openie.crf;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import openie.text.*;
import openie.util.*;

public abstract class CRF {
	
	private static final long serialVersionUID = 1L;
	private static final int CURRENT_SERIAL_VERSION = 1;

    protected Parameter param;
    protected Evaluator eval;
    
    public CRF () {}
    public CRF (Parameter param) {
    	this.param = param;
    }
    
    public abstract void train (UnitextCorpus trainSet, Configure option);
    public abstract void test (UnitextCorpus testSet, Configure option);
    public abstract int[] predict (Sequence example);
    public abstract String[] predict (ArrayList<ArrayList<String>> example);
    
    
	public void load (String filename) throws IOException, ClassNotFoundException {
		FileInputStream fis = new FileInputStream(filename);
		GZIPInputStream gzfis = new GZIPInputStream(fis);
		ObjectInputStream ois = new ObjectInputStream(gzfis);
		readObject(ois);
		ois.close(); fis.close();
		param.makeEdgeIndex(false);
	}

	public void save (String filename) throws IOException {
		FileOutputStream fos = new FileOutputStream(filename);
		GZIPOutputStream gzfos = new GZIPOutputStream(fos);
		ObjectOutputStream oos = new ObjectOutputStream(gzfos);
		writeObject(oos);
		oos.close(); fos.close();
	}    
	
	private void writeObject (ObjectOutputStream out) throws IOException {
		out.writeInt(CURRENT_SERIAL_VERSION);
		out.writeObject(getClass().getName());
		out.writeObject(param);
	}

	private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
		int version = in.readInt();
		String modelType = (String) in.readObject();
		if (!modelType.equals(getClass().getName()))
			throw new ClassNotFoundException("Doen't match the model class");
		param = (Parameter) in.readObject();
	}
    
    public Parameter getParam() {
    	return param;
    }
    
    public void setParam(Parameter param) {
    	this.param = param;
    }
    
	public SparseVector pack (ArrayList<String> tokens) {
		SparseVector ret = new SparseVector();
		String[] inputs = new String[tokens.size() - 1];
		double[] values = new double[tokens.size() - 1];
		for (int i = 1; i < tokens.size(); i++) {
			String[] insideTokens = {tokens.get(i)};
			//String[] insideTokens = tokens[i].split(elimiter, -1);
			
			if (insideTokens.length > 1) {
				inputs[i-1] = insideTokens[0];
				values[i-1] = Double.parseDouble(insideTokens[1]);
			}
			else {
				inputs[i-1] = tokens.get(i);
				values[i-1] = 1;
			}
		}
		
		int[] ids = param.indexing(tokens.get(0), inputs, values, false);
		ret.setLabel(ids[0]);
		for (int i = 1; i < ids.length; i++) {
			if (ids[i] >= 0)
				ret.addElement(ids[i], values[i-1]);
		}
		
		return ret;
	}

}

