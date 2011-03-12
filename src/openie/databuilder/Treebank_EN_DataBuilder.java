
package openie.databuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Treebank_EN_DataBuilder {
	
	private PrintStream logStream = null;
    
	public Treebank_EN_DataBuilder() {
        logStream = System.err;
	}
	
	public void setLog(PrintStream stream) {
		this.logStream = stream;
	}
	
	public class Node {
		public String word;
		public String postag;
		public String label;
		
		public String w;
		public String p;
		public String regex;
		
		public Node() {
		}
		
		public Node(String w, String p, String l) {
			word = w; postag = p; label = l;
		}
	}
	
	public void tokenizeString(String input, ArrayList<Node> seq) {
		if (input.trim().length() <= 0) return;
		String[] tokens = input.trim().split(" ");
		if (tokens.length <= 0) return;
		
		boolean isInPhrase = false;
		Node entNode = new Node();
		for (int i = 0; i < tokens.length; i++) {
			String[] units = tokens[i].split("/");
			assert (units.length == 3);
			if (units[2].endsWith("B-ENT")) {
				if (isInPhrase) 
					seq.add(entNode);
				entNode = new Node(units[0], units[1], "NP");
				isInPhrase = true;
			} else if (units[2].endsWith("I-ENT")) {
				entNode.word += "_" + units[0];
				entNode.postag += "_" + units[1];
			} else {
				if (isInPhrase) 
					seq.add(entNode);
				seq.add(new Node(units[0], units[1], units[2]));
				isInPhrase = false;
			}
		}
		if (isInPhrase)
			seq.add(entNode);
	}
	
	public void tokenizeString(String input, ArrayList<Node> seq, String arg) {
		if (input.trim().length() <= 0) return;
		String[] tokens = input.trim().split(" ");
		if (tokens.length <= 0) return;
		
		boolean isInPhrase = false;
		Node entNode = new Node();
		for (int i = 0; i < tokens.length; i++) {
			String[] units = tokens[i].split("/");
			assert (units.length == 3);
			if (units[2].endsWith("B-ENT")) {
				if (isInPhrase) 
					seq.add(entNode);
				entNode = new Node(units[0], units[1], arg);
				isInPhrase = true;
			} else if (units[2].endsWith("I-ENT")) {
				entNode.word += "_" + units[0];
				entNode.postag += "_" + units[1];
			} else {
				if (isInPhrase) 
					seq.add(entNode);
				seq.add(new Node(units[0], units[1], units[2]));
				isInPhrase = false;
			}
		}
		if (isInPhrase)
			seq.add(entNode);
	}
	
	public String[] beVerb = {"am", "are", "is", "was", "were", "be", "been", "being"};
	
	public ArrayList<ArrayList<String>> generateFeature(ArrayList<Node> sequence) {
		ArrayList<ArrayList<String>> featureForm = new ArrayList<ArrayList<String>>();
		
		for (int i = 0; i < sequence.size(); i++) {
			ArrayList<String> oneline = new ArrayList<String>();
			String label = sequence.get(i).label;
			oneline.add(label);
			
			// current
			Node cur = sequence.get(i);
			String w = ""; String p = cur.postag;
			if (label == "ENT" || label == "NP") {
				p = "NP";
			}
			
			oneline.add("p=" + p); // postag
			if (!p.startsWith("N") && !p.startsWith("VB")) {
				w = cur.word; 
				oneline.add("w=" + w); // word (if not functional, e.g. Noun, Verb, Adverb
			} else if (cur.postag.startsWith("VB")) {
				for (String v : beVerb) {
					if (v == w) {
						w = "be";
						oneline.add("w=" + w); // be verb
						break;
					}
				}
			}
			
			// lexical feature (regex)
			if (w != "") {
				for (String l : generateLexicalFeature(w)) {
					oneline.add(l);
					if (l.startsWith("regex="))
						cur.regex = l.replace("regex=", "");
				}
			}
			
			featureForm.add(oneline);
			cur.w = w; cur.p = p;		
		}
		
		// surrounding feature
		for (int i = 0; i < sequence.size(); i++) {
			ArrayList<String> oneline = featureForm.get(i);
			Node cur = sequence.get(i);
			String w = cur.w, p = cur.p;
			
			// window (prev)
			if (i > 0) {
				Node p1 = sequence.get(i-1);
				oneline.add("p-1=" + p1.p);
				oneline.add("p-1&p=" + p1.p +"&"+ p);
				if (w != "")
					oneline.add("p-1&w=" + p1.p +"&"+ w);
				if (p1.w != "") {
					oneline.add("w-1=" + p1.w);
					if (w != "")
						oneline.add("w-1&w=" + p1.w +"&"+ w);
					oneline.add("w-1&p" + p1.w +"&"+ p);
				}
				if (p1.regex != null)
					oneline.add("regex-1=" + p1.regex);
				if (i > 1) {
					Node p2 = sequence.get(i-2);
					oneline.add("p-2=" + p2.p);
					oneline.add("p-2&p-1=" + p2.p +"&"+ p1.p);
					oneline.add("p-2&p-1&p=" + p2.p +"&"+ p1.p + "&" + p);
					if (w != "")
						oneline.add("p-2&p-1&w=" + p2.p +"&"+ p1.p + "&" + w);
					if (p2.w != "") {
						oneline.add("w-2=" + p2.w);
					}
					if (p2.regex != null)
						oneline.add("regex-2=" + p2.regex);
					if (i > 2) {
						Node p3 = sequence.get(i-3);
						oneline.add("p-3=" + p3.p);
						oneline.add("p-3&p-2=" + p3.p +"&"+ p2.p);
						oneline.add("p-3&p-2&p-1=" + p3.p +"&"+ p2.p +"&"+ p1.p);
						oneline.add("p-3&p-2&p-1&p=" + p3.p +"&"+ p2.p +"&"+ p1.p +"&"+ p);
						if (w != "")
							oneline.add("p-3&p-2&p-1&w=" + p3.p +"&"+ p2.p +"&"+ p1.p +"&"+ w);
						if (p3.w != "") {
							oneline.add("w-3=" + p3.w);
						}
						if (p3.regex != null)
							oneline.add("regex-3=" + p3.regex);
					}
				}
			}
			
			// window (next)
			if (i < sequence.size() - 1) {
				Node p1 = sequence.get(i+1);
				oneline.add("p+1=" + p1.p);
				oneline.add("p&p+1=" + p +"&"+ p1.p);
				if (w != "")
					oneline.add("w&p+1=" + w + "&"+ p1.p);
				if (p1.w != "") {
					oneline.add("w+1=" + p1.w);
					if (w != "")
						oneline.add("w&w+1=" + w +"&"+ p1.w);
					oneline.add("p&w+1" + p +"&"+ p1.w);
				}
				if (p1.regex != null)
					oneline.add("regex+1=" + p1.regex);
				if (i < sequence.size() - 2) {
					Node p2 = sequence.get(i+2);
					oneline.add("p+2=" + p2.p);
					oneline.add("p+1&p+2=" + p1.p +"&"+ p2.p);
					oneline.add("p&p+1&p+2=" + p +"&"+ p1.p +"&"+ p2.p);
					if (w != "")
						oneline.add("w&p+1&p+2" + w +"&"+ p1.p +"&"+ p2.p);
					if (p2.w != "") {
						oneline.add("w+2=" + p2.w);
					}
					if (p2.regex != null)
						oneline.add("regex+2=" + p2.regex);
					if (i < sequence.size() - 3) {
						Node p3 = sequence.get(i+3);
						oneline.add("p+3=" + p3.p);
						oneline.add("p+2&p+3=" + p2.p +"&"+ p3.p);
						oneline.add("p+1&p+2&p+3=" + p1.p +"&"+ p2.p +"&"+ p3.p);
						oneline.add("p&p+1&p+2&p+3=" + p +"&"+ p1.p +"&"+ p2.p +"&"+ p3.p);
						if (w != "")
							oneline.add("w&p+1&p2&p+3" + w +"&"+ p1.p +"&"+ p2.p +"&"+ p3.p);
						if (p3.w != "") {
							oneline.add("w+3=" + p3.w);
						}
						if (p3.regex != null)
							oneline.add("regex+3=" + p3.regex);
					}
				}
			}
		}
		
		return featureForm;
	}
	
	public ArrayList<String> generateLexicalFeature(String w) {
		ArrayList<String> feature = new ArrayList<String>();
		
        Pattern upperCasePattern = Pattern.compile("[A-Z][A-Z]*");
        Matcher upperCaseMatcher = upperCasePattern.matcher(w);
        String output1 = upperCaseMatcher.replaceAll("A");
        
        Pattern lowerCasePattern = Pattern.compile("[a-z][a-z]*");
        Matcher lowerCaseMatcher = lowerCasePattern.matcher(output1);
        String output2 = lowerCaseMatcher.replaceAll("a");
        
        Pattern digitPattern = Pattern.compile("[0-9][0-9]*");
        Matcher digitMatcher = digitPattern.matcher(output2);
        String output3 = digitMatcher.replaceAll("0");
        
        Pattern puncPattern = Pattern.compile("\\p{P}\\p{P}*");
        Matcher puncMatcher = puncPattern.matcher(output3);
        String output4 = puncMatcher.replaceAll(".");
        
        
        feature.add("regex=" + output4);
        if (output4.equals("A"))
        	feature.add("lex:allCapital=true");
        else if (output4.startsWith("A")) 
        	feature.add("lex:beginCapital=true");
        else if (output4.indexOf('A') >= 0)
        	feature.add("lex:containCapital=true");
        
        if (output4.equals("0"))
        	feature.add("lex:allDigit=true");
        else if (output4.startsWith("0")) 
        	feature.add("lex:beginDigit=true");
        else if (output4.indexOf('0') >= 0)
        	feature.add("lex:containDigit=true");

        if (output4.equals("."))
        	feature.add("lex:allPunc=true");
        else if (output4.startsWith(".")) 
        	feature.add("lex:beginPunc=true");
        else if (output4.indexOf('.') >= 0)
        	feature.add("lex:containPunc=true");
        
        if (output4.replace('A', ' ').replace('a', ' ').replace('0', ' ').replace('.', ' ').trim().length() > 0)
        	feature.add("lex:containOtherChar=true");
        
		return feature;
	}
	
	static public int maxDistance = 0;
	
	public boolean printInstance(String line, BufferedWriter outputWriter) throws IOException {
		ArrayList<Node> sequence = new ArrayList<Node>();
		
		line = line.replace("\\/", "\\\\");
		
		// left context of ARG1
		String leftContextOfARG1 = line.substring(0, line.indexOf("<ARG1"));
		tokenizeString(leftContextOfARG1, sequence);
		// ARG1
		String e1String = line.substring(line.indexOf("<ARG1"), line.indexOf("</ARG1>"));
		e1String = e1String.substring(e1String.indexOf(">")+1);
		tokenizeString(e1String, sequence, "ENT");
		// context between ARG1 and ARG2
		String context = line.substring(line.indexOf("</ARG1>"), line.indexOf("<ARG2"));
		context = context.substring(context.indexOf(">")+1);
		tokenizeString(context, sequence);
		// ARG2
		String e2String = line.substring(line.indexOf("<ARG2"), line.indexOf("</ARG2>"));
		e2String = e2String.substring(e2String.indexOf(">")+1);
		tokenizeString(e2String, sequence, "ENT");
		// right context of ARG2
		String rightContextOfARG2 = line.substring(line.indexOf("</ARG2>"));
		rightContextOfARG2 = rightContextOfARG2.substring(rightContextOfARG2.indexOf(">")+1);
		tokenizeString(rightContextOfARG2, sequence);
		
		// filtering
		boolean containVerb = false;
		boolean containREL = false;
		boolean containNNP = false;
		int distance = 0;
		int entNumber = 0;
		int npNumber = 0;
		for (Node n : sequence) {
			if (n.postag.startsWith("VB"))
				containVerb = true;
			if (n.label.endsWith("REL"))
				containREL = true;
			if (n.label == "ENT") {
				entNumber++;
				if (entNumber == 1 && n.postag.indexOf("NNP") >= 0)
					containNNP = true;
			}
			if (n.label == "NP") {
				if (entNumber > 0)
					npNumber++;
			}
			if (entNumber > 0)
				distance++;
			if (entNumber > 1 && n.label == "ENT")
				break;
		}
		if (!containVerb)
			return false;
		if (distance > 20) 
			return false;
		if (containREL && distance > maxDistance)
			maxDistance = distance;
		//if (npNumber > 1)
		//	return false;
		
		if (!containNNP)
			return false;
		
		// feature generation
		ArrayList<ArrayList<String>> featureForm = generateFeature(sequence);
		
		// print out
		entNumber = 0;
		for (ArrayList<String> oneline : featureForm) {
			if (oneline.get(0).equals("ENT")) {
				entNumber++;
			}
			
			if (entNumber > 0) {
				StringBuffer buf = new StringBuffer();
				for (String t : oneline)
					buf.append(t + " ");
				buf.append("\n");
				outputWriter.write(buf.toString());
				logStream.print(buf.toString());
			}
			
			if (entNumber > 1 && oneline.get(0).equals("ENT")) {
				break;
			}
		}
		outputWriter.write("\n");
		logStream.println();
		
		return true;
	}

	public void run(String inputPath, String outputPath) throws IOException {
		run(new File(inputPath), new File(outputPath));
	}
	
	public void run(File inputDir, File outputDir) throws IOException {
        String[] filelist = inputDir.list();
		if (!outputDir.exists()) outputDir.mkdir();
        
        for (String filename : filelist) {
        	File inFile = new File(inputDir.getPath() + "/" + filename);
        	
        	if (inFile.isDirectory()) {
        		run(inFile, new File(outputDir.getPath() + "/" + filename));
        	} 
        	else if (inFile.isFile()) {
        		if (!filename.endsWith(".instance"))
        			continue;
        		
        		int instanceNumber = 0, positiveNumber = 0;
        		System.err.print("Parsing file: " + inFile.getPath() + " ... ");
        		
    			BufferedReader inputReader = new BufferedReader(new FileReader(inFile.getPath()));
        		final String filenamePrefix = filename.substring(0, filename.lastIndexOf('.'));
        		BufferedWriter outputWriter = new BufferedWriter(new FileWriter(outputDir.getPath() + "/" + filenamePrefix + ".feature"));
    			
    			String line = null;
    			while ((line = inputReader.readLine()) != null) {
    				if (line.length() <= 0 || line.startsWith("# ")) {
				    	continue;
    				}
    				if (printInstance(line, outputWriter)) {
    					instanceNumber++;
    					if (line.contains("REL"))
    						positiveNumber++;
    				}
    			}
    			System.err.println("done (" + instanceNumber + "=" + positiveNumber + "+" + (instanceNumber-positiveNumber) +")");
    			totalInstanceNumber += instanceNumber;
    			totalPositiveNumber += positiveNumber;
    			inputReader.close();
    			outputWriter.close();
        	}
        }
	}
	
	static public int totalInstanceNumber = 0, totalPositiveNumber = 0;
	
	static public void main(String args[]) throws IOException {
		
		int argIndex = 0;
		if (args.length < 1) {
			System.err.println("usage: java " + Treebank_EN_DataBuilder.class.getName() + " [options] \n\t -inputDir inputDataPath \n\t -outputDir outputDirPath \n\t -logfile logFile");
			return;
		}
			
		String inputDirPath = null, outputDirPath = null, logFileName = null;
		PrintStream logStream = System.err;
		
	    while (argIndex < args.length && args[argIndex].charAt(0) == '-') {
	    	
			if (args[argIndex].equalsIgnoreCase("-inputDir")) {
				inputDirPath = args[argIndex + 1];
				argIndex += 2;
			} else if (args[argIndex].equalsIgnoreCase("-outputDir")) {
				outputDirPath = args[argIndex + 1];
				argIndex += 2;
			} else if (args[argIndex].equalsIgnoreCase("-logFile")) {
				logFileName = args[argIndex + 1];
				argIndex += 2;
			}
	    }
	    
	    Treebank_EN_DataBuilder builder = new Treebank_EN_DataBuilder();
		if (logFileName != null) 
			builder.setLog(new PrintStream(new FileOutputStream(logFileName)));
		
		builder.run(inputDirPath, outputDirPath);
		
		System.err.println("Done (" + totalInstanceNumber + "=" + totalPositiveNumber + "+" + (totalInstanceNumber-totalPositiveNumber) +")");
		System.err.println(maxDistance);
		logStream.close();

		
		
	}
}
