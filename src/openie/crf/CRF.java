/*
 * Copyright (C) 2010 Minwoo Jeong (minwoo.j@gmail.com).
 * This file is part of the "bitextOpenIE" distribution.
 * http://github.com/minwoo/bitextOpenIE/
 * This software is provided under the terms of LGPL.
 */

package openie.crf;

import java.io.IOException;

import openie.text.*;
import openie.util.*;

import org.apache.log4j.Logger;

public abstract class CRF {
	private static final long serialVersionUID = 1L;
	protected Logger logger = Logger.getLogger(CRF.class);
	
    protected Parameter param = new Parameter();
    protected Evaluator eval;
    
    public CRF () {}
    public abstract void train (Corpus trainSet, Configure option);
    public abstract void test (Corpus testSet, Configure option);
    public abstract int predict (SparseVector example);
    public abstract void save (String filename) throws IOException;
    public abstract void load (String filename) throws IOException, ClassNotFoundException;
    
    public Parameter getParam() {
    	return param;
    }
}

