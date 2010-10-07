/*
 * Copyright (C) 2010 Minwoo Jeong (minwoo.j@gmail.com).
 * This file is part of the "bitextOpenIE" distribution.
 * http://github.com/minwoo/bitextOpenIE/
 * This software is provided under the terms of LGPL.
 */

package openie.text;

import java.util.ArrayList;

public class Sequence {
	
	protected ArrayList<SparseVector> seq;
	protected int seq_id;
	
	public Sequence() {
		init();
	}
	
	public Sequence(int id) {
		init();
		setId(id);
	}
	
	public void init() {
		seq = new ArrayList<SparseVector>();
		seq_id = -1;
	}
	
	public Sequence(ArrayList<SparseVector> seq, int id) {
		setSeq(seq);
		setId(id);
	}
	
	public ArrayList<SparseVector> getSeq() {
		return seq;
	}
	
	public int getId() {
		return seq_id;
	}
	
	public void setSeq(ArrayList<SparseVector> seq) {
		this.seq = seq;
	}
	
	public void setId(int id) {
		seq_id = id;
	}
	
	public void addElement(SparseVector elem) {
		seq.add(elem);
	}
	
	public SparseVector at(int i) {
		return seq.get(i);
	}
	
	public int size() {
		return seq.size();		
	}
	
}
