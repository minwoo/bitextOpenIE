/*
 * Copyright (C) 2010 Minwoo Jeong (minwoo.j@gmail.com).
 * This file is part of the "bitextOpenIE" distribution.
 * http://github.com/minwoo/bitextOpenIE/
 * This software is provided under the terms of LGPL.
 */

package openie.text;

import java.util.ArrayList;

/**
 * mj.ml4nlp.type::ClassifyVector.java
 *
 * @author minwoo
 * @date 2010. 2. 24.
 */
public class SparseVector {
	
	protected ArrayList<IntElement> input;
	protected int label;
	protected int count;
	
	public SparseVector () {
		this.label = -1;
		this.input = new ArrayList<IntElement>();
		this.count = 0;
	}
	
	public SparseVector (int label) {
		this.label = label;
		this.input = new ArrayList<IntElement>();
		this.count = 1;
	}
	
	public SparseVector (int label, ArrayList<IntElement> obs) {
		this.label = label;
		this.input = obs;
		this.count = 1;
	}
	
	public int getLabel () {
		return label;
	}
	
	public ArrayList<IntElement> getElement () {
		return input;
	}
	
	public IntElement getElement (int i) {
		return input.get(i);
	}
	
	public int getCount () {
		return count;
	}
	
	public void addElement (IntElement p) {
		input.add(p);
	}
	
	public void addElement (int id, double val) {
		IntElement p = new IntElement(id, val);
		input.add(p);
	}
	
	public void addCount (int c) {
		count += c;
	}
	
	public void addCount () {
		count ++;
	}
	
	public class IntElement {
	    public int id;
	    public double val;

	    public IntElement () {
	        id = -1;
	        val = -1;
	    }

	    public IntElement (int id, double val) {
	    	set(id, val);
	    }
	    
	    public int getId () {
	    	return id;
	    }
	    
	    public double getVal () {
	    	return val;
	    }
	    
	    public void set (int id, double val) {
	        this.id = id;
	        this.val = val;
	    }
	}
}
