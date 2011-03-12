/*
 * Copyright (C) 2010 Minwoo Jeong (minwoo.j@gmail.com).
 * This file is part of the "bitextOpenIE" distribution.
 * http://github.com/minwoo/bitextOpenIE/
 * This software is provided under the terms of LGPL.
 */

package openie.crf;

import java.util.ArrayList;

import openie.text.Sequence;
import openie.text.UnitextCorpus;
import openie.util.Configure;

public class BitextCRF extends CRF {

	@Override
	public int[] predict(Sequence example) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void test(UnitextCorpus testSet, Configure option) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public String[] predict (ArrayList<ArrayList<String>> instance) {
		String[] output = new String[instance.size()];
		
		return output;
	}

	@Override
	public void train(UnitextCorpus trainSet, Configure option) {
		// TODO Auto-generated method stub
		
	}


}
