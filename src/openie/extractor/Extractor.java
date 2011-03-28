/*
 * Copyright (C) 2010-2011 Minwoo Jeong (minwoo.j@gmail.com).
 * This file is part of the "bitextOpenIE" distribution.
 * http://github.com/minwoo/bitextOpenIE/
 * This software is provided under the terms of LGPL.
 */

package openie.extractor;


import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import openie.crf.*;
import openie.text.*;
import openie.text.FeatureFactory.Node;
import openie.util.*;
import opennlp.tools.chunker.*;
import opennlp.tools.postag.*;
import opennlp.tools.tokenize.*;
import opennlp.tools.util.Span;

import org.apache.log4j.Logger;
import jargs.gnu.CmdLineParser;

public class Extractor {

	static public int MAX_DISTANCE = 25;
	static public int MAX_NP = 10;
	
	private boolean modeKR;
	private UnitextCRF crf;
	private Parameter param;
	
	// openNLP tool 
	Tokenizer tokenizer;
	POSTaggerME tagger;
	ChunkerME chunker;
	
	public Extractor (boolean modeKR, String modelFileName, String nlpToolPath) throws IOException, ClassNotFoundException {
		this.modeKR = modeKR;
		
		if (modeKR) {
			
			
		} else {
			// tokenizer
			System.err.println("Loading tokenizer model...");
			InputStream tokenizerModelIn = new FileInputStream(nlpToolPath + '/' + "en-token.bin");
			tokenizer = new TokenizerME(new TokenizerModel(tokenizerModelIn));
			if (tokenizerModelIn != null) tokenizerModelIn.close();
			// postagger
			System.err.println("Loading pos-tagger model...");
			InputStream taggerModelIn = new FileInputStream(nlpToolPath + '/' + "en-pos-maxent.bin");
			tagger = new POSTaggerME(new POSModel(taggerModelIn));
			if (taggerModelIn != null) taggerModelIn.close();
			// chunker
			System.err.println("Loading chunker model...");
			InputStream chunkerModelIn = new FileInputStream(nlpToolPath + '/' + "en-chunker.bin");
			chunker = new ChunkerME(new ChunkerModel(chunkerModelIn), ChunkerME.DEFAULT_BEAM_SIZE, new DefaultChunkerSequenceValidator());
			if (chunkerModelIn != null) chunkerModelIn.close();
		}
		
		System.err.println("Loading CRF model...");
		crf = new UnitextCRF();
		param = new Parameter();
		crf.setParam(param);
		crf.load(modelFileName);
	}
	
	public static Span[] phrasesAsSpanList(String[] aSentence, String[] aTags, String[] aPreds) {
		if (aSentence.length != aTags.length || aTags.length != aPreds.length)
			throw new IllegalArgumentException("All arrays must have the same length!");
		
		// initialize with the list maximum size
		List<Span> phrases = new ArrayList<Span>(aSentence.length); 
		String startTag = "";
		int startIndex = 0;
		boolean foundPhrase = false;
		
		for (int ci = 0, cn = aPreds.length; ci < cn; ci++) {
			String pred = aPreds[ci];
			if (pred.startsWith("B-") || (!pred.equals("I-" + startTag) && !pred.equals("O"))) { // start
				if (foundPhrase) { // handle the last
					phrases.add(new Span(startIndex, ci, startTag));
		    }
		    startIndex = ci;
		    startTag = pred.substring(2);
		    foundPhrase = true;
			} else if (pred.equals("I-" + startTag)) { // middle
				// do nothing
			} else if (foundPhrase) {// end
				phrases.add(new Span(startIndex, ci, startTag));
				foundPhrase = false;
				startTag = "";
			}
		}
		if (foundPhrase) { // leftover
			phrases.add(new Span(startIndex, aPreds.length, startTag));
		}
		
		return phrases.toArray(new Span[phrases.size()]);
	}

	public void run(File inFile, PrintStream outStream, String refFileName, Boolean isEvaluation) throws IOException {
		// extraction
		BufferedReader inputReader = new BufferedReader(new FileReader(inFile.getPath()));
		
		String line = null; String rawSentence = null;
		while ((line = inputReader.readLine()) != null) {
			rawSentence = line.trim();

			if (line.length() <= 0 || line.startsWith("#")) {
		    	outStream.println(rawSentence);
		    	continue;
			}
			
			//String[] tokens = tokenizer.tokenize(line); // tokenize
			line = line.replace("\\/", "\\\\");
			if (line.split("\t", -1).length > 1) 
				line = line.split("\t", -1)[1];
			
			// processing argument marker
			String arg1String = line.substring(line.indexOf("<p1>"), line.indexOf("</p1>"));
			line = line.replace(arg1String+"</p1>", " ARG1 ");
			arg1String = arg1String.substring(arg1String.indexOf(">")+1).trim();
			String arg2String = line.substring(line.indexOf("<p2>"), line.indexOf("</p2>"));
			line = line.replace(arg2String+"</p2>", " ARG2 ");
			arg2String = arg2String.substring(arg2String.indexOf(">")+1).trim();
			
			// tokenizing
	        //String whitespaceTokenizerLine[] = WhitespaceTokenizer.INSTANCE.tokenize(line);
			String whitespaceTokenizerLine[] = tokenizer.tokenize(line);
	        // tagging
	        String[] tags = tagger.tag(whitespaceTokenizerLine);
	        POSSample posSample = new POSSample(whitespaceTokenizerLine, tags);
	        String[] words = posSample.getSentence();
	        //chunking
	        String[] chunks = chunker.chunk(posSample.getSentence(), posSample.getTags());
	        ChunkSample chunkSample = new ChunkSample(posSample.getSentence(), posSample.getTags(), chunks);
	        Span[] spans = phrasesAsSpanList(chunkSample.getSentence(), chunkSample.getTags(), chunkSample.getPreds());

	        boolean isReverseOrder = false;
	        int indexOfARG1 = -1, indexOfARG2 = -1;
	        for (int i = 0; i < words.length; i++) {
	        	if (words[i].equals("ARG1"))
	        		indexOfARG1 = i;
	        	else if (words[i].equals("ARG2"))
	        		indexOfARG2 = i;
	        }
	        
	        if (indexOfARG1 < 0 || indexOfARG2 < 0 || indexOfARG1 == indexOfARG2) {
		    	outStream.println("null\t" + rawSentence);
	        	continue;
	        }
	        if (indexOfARG1 > indexOfARG2) {
	        	int temp = indexOfARG1; indexOfARG2 = indexOfARG2; indexOfARG2 = temp;
	        	isReverseOrder = true;
	        }
	        
	        Node arg1Node = isReverseOrder ? new Node(arg2String, "NNP", "ENT") : new Node(arg1String, "NNP", "ENT");
	        Node arg2Node = isReverseOrder ? new Node(arg1String, "NNP", "ENT") : new Node(arg2String, "NNP", "ENT");
	        //ArrayList<Node> leftContextOfARG1 = FeatureFactory.tokenizeString(words, tags, chunks, indexOfARG1-2, indexOfARG1);
	        //ArrayList<Node> rightContextOfARG2 = FeatureFactory.tokenizeString(words, tags, chunks, indexOfARG2+1, indexOfARG2+3);
			ArrayList<Node> context = FeatureFactory.tokenizeString(words, tags, chunks, indexOfARG1+1, indexOfARG2);
			
			ArrayList<Node> instance = new ArrayList<Node>();
			//for (Node n : leftContextOfARG1) instance.add(n);
			instance.add(arg1Node);
			for (Node n : context) instance.add(n);
			instance.add(arg2Node);
			//for (Node n : rightContextOfARG2) instance.add(n);
	        
			// filtering 
			boolean unexpectedInstance = false;
			int nNP = 0; boolean inVerb = false;
			for (Node n : context) {
				if (n.postag.startsWith("VB"))
					inVerb = true;
				else if (n.label == "NP")
					nNP++;
			}
			if (context.size() > MAX_DISTANCE || nNP > MAX_NP || !inVerb) 
				unexpectedInstance = true;
			
			if (unexpectedInstance) {
				outStream.println("null\t" + rawSentence);
				continue;
			}
			
			// prediction
			ArrayList<Node> wordForm = new ArrayList<Node>();
			ArrayList<ArrayList<String>> featureForm = FeatureFactory.generateFeature(instance, wordForm);
			String[] prediction = crf.predict(featureForm);
//			for (int i = 0 ; i < prediction.length; i++)
//				System.out.print(wordForm.get(i).word+"/"+wordForm.get(i).postag+"/"+prediction[i] +" ");
//			System.out.println();
			// print out
			String output = predictionToString(prediction, wordForm);
        	outStream.println(output + "\t" + rawSentence);

//			for (int x = 0; x < featureForm.size(); x++) {
//				ArrayList<String> oneline = featureForm.get(x);
//				System.out.println(prediction[x] + " " + wordForm.get(x).word + " " + oneline.toString());
//			}
//			System.out.println();

			
//	        for (int i = 0; i < spans.length; i++) {
//	        	// ARG1
//	        	Node arg1 = null;
//	        	ArrayList<Node> leftContextOfARG1 = null;
//	        	if (spans[i].getType().equals("NP")) {
//	        		ArrayList<Node> seq = FeatureFactory.tokenizeString(words, tags, chunks, spans[i].getStart(), spans[i].getEnd());
//	        		if (seq.size() == 1 && seq.get(0).postag.indexOf("NNP") >= 0) {
//	        			arg1 = seq.get(0); arg1.label = "ENT";
//	        			leftContextOfARG1 = FeatureFactory.tokenizeString(words, tags, chunks, spans[i].getStart()-2, spans[i].getStart());
//	        			
//	    	        	for (int j = i+1; j < spans.length; j++) {
//	    	        		if (spans[j].getType().equals("NP")) {
//	    	        			// ARG2
//	    		        		ArrayList<Node> seq2 = FeatureFactory.tokenizeString(words, tags, chunks, spans[j].getStart(), spans[j].getEnd());
//	    		        		//if (seq2.get(0).postag.indexOf("NNP") < 0) // ARG2 should be NNP 
//	    		        		//	continue;
//	    		        		
//	    		        		Node arg2 = seq2.get(0); arg2.label = "ENT";
//	    		        		ArrayList<Node> rightContextOfARG2 = FeatureFactory.tokenizeString(words, tags, chunks, spans[j].getEnd(), spans[j].getEnd()+2);
//	    	        			ArrayList<Node> context = FeatureFactory.tokenizeString(words, tags, chunks, spans[i].getEnd(), spans[j].getStart());
//    	        				ArrayList<Node> instance = new ArrayList<Node>();
//    	        				for (Node n : leftContextOfARG1) instance.add(n);
//    	        				instance.add(arg1);
//    	        				for (Node n : context) instance.add(n);
//    	        				instance.add(arg2);
//    	        				for (Node n : rightContextOfARG2) instance.add(n);
//	    	        			// filter
//	    	        			int distance = context.size(); //spans[j].getStart() - spans[i].getEnd();
//	    	        			if (distance > 20) break;
//	    	        			boolean inVerb = false;
//	    	        			//for (int k = spans[i].getEnd(); k < spans[j].getStart(); k++) {
//	    	        			//	if (tags[k].startsWith("VB")) {
//	    	        			for (Node n : context) {
//	    	        				if (n.postag.startsWith("VB")) {
//	    	        					inVerb = true; break;
//	    	        				}
//	    	        			}
//	    	        			if (!inVerb) continue;
//
//    	        				
//    	        				ArrayList<Node> wordForm = new ArrayList<Node>();
//    	        				ArrayList<ArrayList<String>> featureForm = FeatureFactory.generateFeature(instance, wordForm);
//    	        				String[] prediction = crf.predict(featureForm);
//    	        				
//    	        				// print out
//    	        				StringBuilder buffer = new StringBuilder();
//    	        				StringBuilder temp;
//    	        				int entNumber = 0, relNumber = 0;
//    	        				ArrayList<String> npList = new ArrayList<String>();
//    	        				boolean inPhrase = false;
//    	        				for (int k = 0; k < prediction.length; k++) {
//    	        					String pred = prediction[k];
//    	        					if (pred.equals("ENT")) {
//    	        						entNumber++;
//    	        						if (entNumber == 1) {
//    	        							buffer.append("<" + wordForm.get(k).word + ",");
//    	        						} else if (entNumber == 2) {
////    	        							if (npList.size() > 0) {
////    	        								for (String np : npList)
////    	        									buffer.append("|" + np);
////    	        							}
//    	        							buffer.append(",");
//    	        							buffer.append(wordForm.get(k).word + "> ");
//    	        						}
//    	        						inPhrase = false;
//    	        					}
//    	        					else if (pred.indexOf("REL") >= 0) {
//    	        						if (inPhrase) {
//        	        						buffer.append(" " + wordForm.get(k).word);
//    	        						} else {
//    	        							if (relNumber > 0)
//    	        								buffer.append(" _ ");
//    	        							buffer.append(wordForm.get(k).word);
//    	        							relNumber++;
//    	        							
//    	        						}
//    	        						inPhrase = true;
//    	        					} else if (pred.indexOf("NP") >= 0) {
//	    	        					npList.add(wordForm.get(k).word);
//    	        						inPhrase = false;
//    	        					} else {
//    	        						inPhrase = false;
//    	        					}
//    	        				}
//    	        				if (relNumber > 0)
//    	        					outStream.print(buffer.toString());
////    	        				for (int x = 0; x < featureForm.size(); x++) {
////    	        					ArrayList<String> oneline = featureForm.get(x);
////    	        					System.out.println(prediction[x] + " " + wordForm.get(x).word + " " + oneline.toString());
////    	        				}
////    	        				System.out.println();
//
//	    	        		}
//	    	        		
//	    	        		
//	    	        	}
//
//	        		}
//	        	}
//	        } // end for
		}
		
		// evaluation
		if (isEvaluation) {
			
		}
	}
	
    private String predictionToString(String[] prediction, ArrayList<Node> wordForm) {
    	String ret = null;
    	
		StringBuilder buffer = new StringBuilder();
		StringBuilder relString = new StringBuilder();
		int entNumber = 0, relNumber = 0;
		ArrayList<String> npList = new ArrayList<String>();
		String lastNP = null;
		boolean inPhrase = false;
		for (int k = 0; k < prediction.length; k++) {
			String pred = prediction[k];
			if (pred.equals("ENT")) {
				entNumber++;
				
				if (entNumber == 1) {
					buffer.append("<" + wordForm.get(k).word + "; ");
				} else if (entNumber == 2) {
					if (npList.size() > 0) {
						for (String np : npList)
							relString.append("| " + np.replace("_", " "));
					}
					buffer.append(relString.toString()+"; ");
					buffer.append(wordForm.get(k).word + "> ");
				}
				inPhrase = false;
			}
			else if (pred.indexOf("REL") >= 0) {
				if (inPhrase) {
					relString.append(" " + wordForm.get(k).word);
					lastNP = null;
				} else {
					if (relNumber > 0) {
						if (lastNP == null) {
							relString.append(" _ ");
						} else {
							relString.append(" (arg"+npList.size()+") ");
							npList.add(lastNP);
						}
					}
					relString.append(wordForm.get(k).word);
					relNumber++;
				}
				inPhrase = true;
			} else if (pred.indexOf("NP") >= 0) {
				inPhrase = false;
				lastNP = wordForm.get(k).word;
			} else {
				inPhrase = false;
				lastNP = null;
			}
		}
		
		if (relNumber > 0)
			ret = buffer.toString();
		else
			ret = "null";
		return ret;
	}

	public static void main(String[] args) {

        Logger logger = Logger.getLogger(Extractor.class);

        // command line parsing
        CmdLineParser cmdParser = new CmdLineParser();
        CmdLineParser.Option language = cmdParser.addStringOption('l', "lang");
        CmdLineParser.Option model = cmdParser.addStringOption('m', "model");
        CmdLineParser.Option input = cmdParser.addStringOption('i', "input");
        CmdLineParser.Option output = cmdParser.addStringOption('o', "output");
        CmdLineParser.Option path = cmdParser.addStringOption('p', "path");
        CmdLineParser.Option evaluation = cmdParser.addBooleanOption('e', "evaluation");
        CmdLineParser.Option reference = cmdParser.addStringOption('r', "reference");
        CmdLineParser.Option input_dir = cmdParser.addStringOption('I', "inputDir");
        CmdLineParser.Option output_dir = cmdParser.addStringOption('O', "outputDir");

        try {
           cmdParser.parse(args);
        }
        catch (CmdLineParser.OptionException e) {
            logger.error(e.getMessage());
            logger.error("Usage: java -cp ${CLASSPATH} opeie.extractor.Extractor " +
                    "[-l,--lang] {EN, KR} [-m,--model] CRF model file [-i,--input] input text file [-o,--output] output text file (optional) \n" +
                    "[{-e,--evaluation}] [-r,--reference] ground truth file");
            System.exit(2);
        }

        boolean modeKR = (String)cmdParser.getOptionValue(language) == "KR" ? true : false;
        String modelFileName = (String)cmdParser.getOptionValue(model);
        String inputFileName = (String)cmdParser.getOptionValue(input);
        String outputFileName = (String)cmdParser.getOptionValue(output);
        String nlpToolPath = (String)cmdParser.getOptionValue(path);
        String refFileName = (String)cmdParser.getOptionValue(reference);
        Boolean isEvaluation = (Boolean)cmdParser.getOptionValue(evaluation, Boolean.FALSE);
        String inputDirName = (String)cmdParser.getOptionValue(input_dir);
        String outputDirName = (String)cmdParser.getOptionValue(output_dir);
        if (nlpToolPath == null)
        	nlpToolPath = modeKR ? "model/nlpkr" : "model/opennlp";
        
        // running
        try {
        	Extractor extractor = new Extractor(modeKR, modelFileName, nlpToolPath);
        	
        	if (inputDirName != "" && inputDirName != null) {
        		File inputDir = new File(inputDirName);
                String[] filelist = inputDir.list();
                for (String filename : filelist) {
                	File inFile = new File(inputDir.getPath() + "/" + filename);
                	System.err.println("Processing " + inFile.getPath() + " ... ");
    	    		PrintStream outStream = new PrintStream(new FileOutputStream(outputDirName + "/" + filename));
    	        	extractor.run(inFile, outStream, refFileName, isEvaluation);
    	        	outStream.close();
                }
        	} else {
	        	File inFile = new File(inputFileName);
	    		PrintStream outStream = System.out;
	    		if (outputFileName != null) 
	    			outStream = new PrintStream(new FileOutputStream(outputFileName));
	
	        	extractor.run(inFile, outStream, refFileName, isEvaluation);
	        	
	        	outStream.close();
        	}
        }
        catch (Exception e) {
        	logger.error("error " + e.getMessage());
        	e.printStackTrace();
            System.exit(2);
        }

    }
}
