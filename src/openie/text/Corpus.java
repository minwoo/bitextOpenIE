/*
 * Copyright (C) 2010 Minwoo Jeong (minwoo.j@gmail.com).
 * This file is part of the "bitextOpenIE" distribution.
 * http://github.com/minwoo/bitextOpenIE/
 * This software is provided under the terms of LGPL.
 */

package openie.text;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/**
 * mj.ml4nlp.type::ClassifyCorpus.java
 *
 * @author minwoo
 * @date 2010. 2. 24.
 */
public class Corpus implements Serializable {

	private static final long serialVersionUID = 1L;
	protected ArrayList<SparseVector> data;
	protected Parameter param;
	protected Alphabet inputDict;
	protected Alphabet labelDict;
	protected InvertedIndex paramIndex;
	
	public Corpus () {
		data = new ArrayList<SparseVector>();
		param = new Parameter();
		inputDict = param.getInputAlphabet();
		labelDict = param.getLabelAlphabet();
		paramIndex = param.getParamIndex();
	}

	public Corpus (Parameter param) {
		data = new ArrayList<SparseVector>();
		this.param = param;
		inputDict = this.param.getInputAlphabet();
		labelDict = this.param.getLabelAlphabet();
		paramIndex = this.param.getParamIndex();
	}
	
	public boolean readFile (String filename, boolean isUpdate) throws IOException {
		FileReader fr = new FileReader(filename);
		BufferedReader br = new BufferedReader(fr);
		String line = null;
		
		while ((line = br.readLine()) != null) {
			String[] tokens = line.split(" ", -1);
			if (tokens.length < 2)
				continue;
			append(pack(tokens, isUpdate));
		}
		br.close(); fr.close();
		
		return true;
	}
	
	public void append (SparseVector instance) {
		data.add(instance);
	}
	
	private SparseVector pack (String[] tokens, boolean isUpdate) {
		SparseVector ret = new SparseVector();
		
		String[] inputs = new String[tokens.length - 1];
		double[] values = new double[tokens.length - 1];
		for (int i = 1; i < tokens.length; i++) {
			String[] insideTokens = tokens[i].split(":", -1);
			
			int id = -1; double val = 1;
			if (insideTokens.length > 1) {
				inputs[i-1] = insideTokens[0];
				values[i-1] = Double.parseDouble(insideTokens[1]);
			}
			else {
				inputs[i-1] = tokens[i];
				values[i-1] = 1;
			}
		}
		
		int[] ids = param.indexing(tokens[0], inputs, values, isUpdate);
		ret.label = ids[0];
		for (int i = 1; i < ids.length; i++) {
			ret.addElement(ids[i], values[i-1]);
		}
		
		return ret;
	}
	
	public void shuffle (java.util.Random r) {
		Collections.shuffle(this.data, r);
	}
	
	public int size () {
		return data.size();
	}
	
	public Iterator<SparseVector> iterator () {
		return data.iterator();
	}
}
