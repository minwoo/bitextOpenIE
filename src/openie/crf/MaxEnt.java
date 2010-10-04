/*
 * Copyright (C) 2010 Minwoo Jeong (minwoo.j@gmail.com).
 * This file is part of the "bitextOpenIE" distribution.
 * http://github.com/minwoo/bitextOpenIE/
 * This software is provided under the terms of LGPL.
 */

package openie.crf;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.*;

import java.io.*;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.log4j.Logger;

import sun.tools.tree.ThisExpression;

import openie.text.*;
import openie.text.SparseVector.IntElement;
import openie.util.Configure;

public class MaxEnt extends CRF implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final int CURRENT_SERIAL_VERSION = 1;
	private static final String CLASS_NAME = MaxEnt.class.getName();

	private transient Logger logger = Logger.getLogger(MaxEnt.class);
    
	// learning options
	private double opt_l1prior = 1;
	private double opt_initLearnRate = 0.5;
	private double opt_maxiter = 100;
	private double opt_eta = 1E-05;

	private double[] weight;	// weight vector
	private double[] count;		// empirical count
	private double[] gradient; 	// used for batch learner
	private double[] penalty;	// used for SGD-L1 
	
	public MaxEnt() {}

	/* (non-Javadoc)
	 * @see mj.ml4nlp.classifier.Classifier#classify(mj.ml4nlp.type.CategoryDatum)
	 */
	@Override
	public int predict (SparseVector example) {
		double[] prob = new double[param.sizeLabel()];
		return argmax(example, prob);
	}
	
	/* (non-Javadoc)
	 * @see mj.ml4nlp.classifier.Classifier#test(mj.ml4nlp.type.ClassifyCorpus, mj.ml4nlp.util.Configure)
	 */
	@Override
	public void test(Corpus testSet, Configure option) {
		int nCorrect = 0;
		Iterator<SparseVector> iter = testSet.iterator();
		while (iter.hasNext()) {
			SparseVector instance = iter.next();
			int outcome = predict(instance);
			if (outcome == instance.getLabel()) 
				nCorrect ++;
		}
		logger.info(String.format("[TEST] %.4f", (double) nCorrect / testSet.size() ));
	}

	/* (non-Javadoc)
	 * @see mj.ml4nlp.classifier.Classifier#learn(mj.ml4nlp.type.CategoryData, mj.ml4nlp.util.Configure)
	 */
	@Override
	public void train (Corpus trainSet, Configure option) {
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
	
	private final void runSGDL1 (Corpus trainSet) {
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
		int N = trainSet.size();
		
		// iteration
		for (int niter = 0; niter < opt_maxiter; niter++) {
			int nCorrect = 0;
			double learningRate = opt_initLearnRate / ( 1.0 + niter / N );
			cumulativeRate += learningRate * opt_l1prior / N;
			double currentLoglikeli = 0;
			
			trainSet.shuffle(new java.util.Random());
			Iterator<SparseVector> iter = trainSet.iterator();
			while (iter.hasNext()) {
				SparseVector instance = iter.next();
				
				int outcome = argmax(instance, prob);
				if (outcome == instance.getLabel()) 
					nCorrect ++;
				
				// gradient update
				update(instance, prob, learningRate, cumulativeRate);
				// pseudo loglikelihood  
				currentLoglikeli -= Math.log(prob[instance.getLabel()]);
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
		Iterator<SparseVector> iter = trainSet.iterator();
		while (iter.hasNext()) {
			SparseVector instance = iter.next();
			
			int outcome = argmax(instance, prob);
			if (outcome == instance.getLabel()) 
				nCorrect ++;
			// true loglikelihood  
			currentLogLikeli -= Math.log(prob[instance.getLabel()]);
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
	
	private final int argmax (SparseVector instance, double[] prob) {
		Arrays.fill(prob, 0);
		
		for (IntElement point : instance.getElement()) {
			TIntIntHashMap index = param.getIndex(point.getId());
			for (int y : index.keys())
				prob[y] += weight[index.get(y)];
		}
		
		double sum = 0.0, max = 0.0;
		int max_y = 0;
		for (int y = 0; y < prob.length; y++) {
			prob[y] = Math.exp(prob[y]);
			sum += prob[y];
			if (prob[y] > max) {
				max = prob[y];
				max_y = y;
			}
		}
		for (int y = 0; y < prob.length; y++)
			prob[y] /= sum;
		
		return max_y;
	}

	/* (non-Javadoc)
	 * @see mj.ml4nlp.classifier.Classifier#load(java.lang.String)
	 */
	@Override
	public void load (String filename) throws IOException, ClassNotFoundException {
		FileInputStream fis = new FileInputStream(filename);
		ObjectInputStream ois = new ObjectInputStream(fis);
		readObject(ois);
		ois.close(); fis.close();
	}

	/* (non-Javadoc)
	 * @see mj.ml4nlp.classifier.Classifier#save(java.lang.String)
	 */
	@Override
	public void save (String filename) throws IOException {
		FileOutputStream fos = new FileOutputStream(filename);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		writeObject(oos);
		oos.close(); fos.close();
	}

	private void writeObject (ObjectOutputStream out) throws IOException {
		out.writeInt(CURRENT_SERIAL_VERSION);
		out.writeObject(CLASS_NAME);
		out.writeObject(param);
	}

	private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
		int version = in.readInt();
		String modelType = (String) in.readObject();
		if (!modelType.equals(MaxEnt.class.getName()))
			throw new ClassNotFoundException("Doen't match the model class");
		param = (Parameter) in.readObject();
	}


}
