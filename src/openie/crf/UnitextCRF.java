/*
 * Copyright (C) 2010 Minwoo Jeong (minwoo.j@gmail.com).
 * This file is part of the "bitextOpenIE" distribution.
 * http://github.com/minwoo/bitextOpenIE/
 * This software is provided under the terms of LGPL.
 */

package openie.crf;

import gnu.trove.map.hash.TIntIntHashMap;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;

import openie.text.Parameter;
import openie.text.Sequence;
import openie.text.UnitextCorpus;
import openie.text.SparseVector;
import openie.text.SparseVector.IntElement;
import openie.util.Configure;

public class UnitextCRF extends CRF {

	private transient Logger logger = Logger.getLogger(UnitextCRF.class);
    
	// learning options
	private double opt_l1prior = 1;
	private double opt_initLearnRate = 0.5;
	private double opt_maxiter = 100;
	private double opt_eta = 1E-05;

	private double[] weight;	// weight vector
	private double[] count;		// empirical count
	private double[] gradient; 	// used for batch learner
	private double[] penalty;	// used for SGD-L1 

	@Override
	public int[] predict (Sequence example) {
		double[] prob = new double[example.size()];
		return argmax(example, prob);
	}
	
	@Override
	public void test(UnitextCorpus testSet, Configure option) {
		int nCorrect = 0;
		Iterator<Sequence> iter = testSet.iterator();
		while (iter.hasNext()) {
			Sequence instance = iter.next();
			int[] outcome = predict(instance);
			for (int i = 0; i < outcome.length; i++) {
				if (outcome[i] == instance.at(i).getLabel()) 
					nCorrect ++;
			}
		}
		logger.info(String.format("[TEST] %.4f", (double) nCorrect / testSet.sizeElement() ));
	}

	@Override
	public void train(UnitextCorpus trainSet, Configure option) {
		if (option.contains("maxiter"))
			this.opt_maxiter = option.getInteger("maxiter");
		if (option.contains("l1prior"))
			this.opt_l1prior = option.getDouble("l1prior");
		if (option.contains("init_learning_rate"))
			this.opt_initLearnRate = option.getDouble("init_learning_rate");
		if (option.contains("eta"))
			this.opt_eta = option.getDouble("eta");
		
		runSGDL1(trainSet);		
	}
	
	private final double[][] computeNode (Sequence instance) {
		double[][] nodeScore = new double[instance.size()+1][param.sizeLabel()];
		for (int i = 0; i < nodeScore.length; i++) 
			Arrays.fill(nodeScore[i], 1.0);
		
		double[] lambda = param.getWeight();
		for (int i = 0; i < nodeScore.length; i++) {
			for (IntElement point : instance.at(i).getElement()) {
				TIntIntHashMap index = param.getIndex(point.getId());
				for (int y : index.keys())
					 nodeScore[i][y] *= Math.exp(lambda[index.get(y)] * point.getVal());
			}
		}
		return nodeScore;
	}
	
	private final double[][] computeEdge (Sequence instance) {
		int N = param.sizeLabel();
		double[][] edgeScore = new double[N][N];
		for (int i = 0; i < edgeScore.length; i++) 
			Arrays.fill(edgeScore[i], 1.0);
		
		double[] lambda = param.getWeight();
		for (int i = 0; i < N; i++) {
			TIntIntHashMap index = param.getEdgeIndex(i);
			for (int j : index.keys()) 
				 edgeScore[i][j] *= Math.exp(lambda[index.get(j)]);
		}
		return edgeScore;
	}
	
	private final void forward (double[][] nodeScore, double[][] edgeScore) {
		
	}
	
	private final void backward (double[][] nodeScore, double[][] edgeScore) {
	
	}
	
	private final void inference (Sequence instance) {
		
	}
		
	private final double likelihood (Sequence instance) {
		double prob = 0.0;
		
		return prob;
	}

	private final void runSGDL1 (UnitextCorpus trainSet) {
		weight = param.getWeight();
		count = param.getCount();
		gradient = new double[weight.length];
		penalty = new double[weight.length];
		double[] prob = new double[param.sizeLabel()];
		
		Arrays.fill(weight, 0);
		Arrays.fill(penalty, 0);
		param.clear();
		
		double cumulativeRate = 0;
		double prevLoglikeli = 0;
		int N = trainSet.sizeElement();
		
		// iteration
		for (int niter = 0; niter < opt_maxiter; niter++) {
			int nCorrect = 0;
			double learningRate = opt_initLearnRate / ( 1.0 + niter / N );
			cumulativeRate += learningRate * opt_l1prior / N;
			double currentLoglikeli = 0;
			
			trainSet.shuffle(new java.util.Random());
			Iterator<Sequence> iter = trainSet.iterator();
			while (iter.hasNext()) {
				Sequence instance = iter.next();
				
				/*
				for (SparseVector elem : instance.getSeq()) {
					int outcome = argmax(elem, prob);
					if (outcome == elem.getLabel()) 
						nCorrect ++;
					
					// gradient update
					update(elem, prob, learningRate, cumulativeRate);
					// pseudo loglikelihood  
					currentLoglikeli -= Math.log(prob[elem.getLabel()]);
				}
				
				*/
			}
			
			for (int i = 0; i < weight.length; i++)
				currentLoglikeli += opt_l1prior * Math.abs(weight[i]);
			
			logger.info(String.format("[%d] %e %.4f", niter+1, currentLoglikeli, (double) nCorrect / N ));
			if ( Math.abs(currentLoglikeli - prevLoglikeli) / (Math.abs(currentLoglikeli) + Math.abs(prevLoglikeli)) < opt_eta)
				break;
			prevLoglikeli = currentLoglikeli;
		}
		
		// finally
		int nCorrect = 0;
		double currentLogLikeli = 0;
		Iterator<Sequence> iter = trainSet.iterator();
		while (iter.hasNext()) {
			Sequence instance = iter.next();
			
			int[] outcome = predict(instance);
			for (int i = 0; i < outcome.length; i++) {
				if (outcome[i] == instance.at(i).getLabel()) 
					nCorrect ++;
			}
			// true loglikelihood  
			currentLogLikeli -= Math.log(likelihood(instance));
		}
		for (int i = 0; i < weight.length; i++)
			currentLogLikeli += opt_l1prior * Math.abs(weight[i]);

		logger.info(String.format("[FINAL] %e %.4f", currentLogLikeli, (double) nCorrect / N ));
		param.setWeight(weight);		
	}
	
	private final void update (SparseVector instance, double[] prob, double l, double u) {
		for (IntElement point : instance.getElement()) {
			TIntIntHashMap index = param.getIndex(point.getId());
			for (int y : index.keys()) {
				
				int fid = index.get(y);
				if (y == instance.getLabel())
					weight[fid] += l * (1 - prob[y]);
				else
					weight[fid] -= l * prob[y];
				
				// clipping + lazy update
				double z = weight[fid];
				if (z > 0) 
					weight[fid] = Math.max(0, weight[fid] - (u + penalty[fid]));
				else if (z < 0)
					weight[fid] = Math.min(0, weight[fid] + (u - penalty[fid]));
				penalty[fid] += weight[fid] - z;
			}
		}		
	}

	private final int[] argmax (Sequence instance, double[] prob) {
		int[] arg = new int[instance.size()];
		
		return arg;
	}

}
