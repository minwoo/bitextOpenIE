
package openie.text;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.WordTag;
import edu.stanford.nlp.process.Morphology;


public class FeatureFactory {

	public static class Node {
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
	
	public static void tokenizeString(String input, ArrayList<Node> seq) {
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
	
	public static void tokenizeString(String input, ArrayList<Node> seq, String arg) {
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
	
	public static ArrayList<Node> tokenizeString(String[] words, String[] tags, String[] chunks, int startIdx, int endIdx) {
		ArrayList<Node> seq = new ArrayList<Node>();
		if (startIdx > endIdx) return seq;
		if (endIdx < 0) return seq;
		if (startIdx < 0) startIdx = 0;
		if (startIdx >= words.length) return seq;
		if (endIdx > words.length) endIdx = words.length;
		
		boolean isInPhrase = false;
		Node entNode = new Node();
		for (int i = startIdx; i < endIdx; i++) {
			if (chunks[i].endsWith("B-NP")) {
				if (isInPhrase) 
					seq.add(entNode);
				entNode = new Node(words[i], tags[i], "NP");
				isInPhrase = true;
			} else if (chunks[i].endsWith("I-NP")) {
				if (entNode.word == null) {
					entNode.word = words[i];
					entNode.postag = tags[i];
					entNode.label = "NP";
				} else {
					entNode.word += "_" + words[i];
					entNode.postag += "_" + tags[i];
				}
				isInPhrase = true;
			} else {
				if (isInPhrase) 
					seq.add(entNode);
				seq.add(new Node(words[i], tags[i], "O"));
				isInPhrase = false;
			}
		}
		if (isInPhrase)
			seq.add(entNode);
		return seq;
	}
	
	public static ArrayList<String> generateLexicalFeature(String w) {
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
	
	public static String[] beVerb = {"am", "are", "is", "was", "were", "be", "been", "being", "have", "had", "has", "having"};
	
	@SuppressWarnings("unchecked")
	public static ArrayList<ArrayList<String>> generateFeature(ArrayList<Node> sequence) {
		ArrayList<ArrayList<String>> featureForm = new ArrayList<ArrayList<String>>();
		ArrayList<ArrayList<String>> featureForm2 = new ArrayList<ArrayList<String>>();
		
		/*
		 * Lexical features of current position
		 */
		for (int i = 0; i < sequence.size(); i++) {
			ArrayList<String> features = new ArrayList<String>();
			String label = sequence.get(i).label;
			features.add(label);
			
			// current
			Node cur = sequence.get(i);
			String w = ""; String p = cur.postag;
			if (label == "ENT" || label == "NP") {
				p = "NP";
//				if (cur.postag.indexOf('_') >= 0) {
//					p = "NP";
//				}
//				if (cur.postag.indexOf("NNP") >= 0)
//					oneline.add("containNNP");
//				oneline.add("isARG");
				if (label == "ENT")
					p = "ENT";
			}	
			features.add("p=" + p); // postag
			
			if (!p.startsWith("N") && !p.startsWith("VB") && !p.startsWith("ENT")) {
				w = cur.word; 
				features.add("w=" + w); // word (if not functional, e.g. Noun, Verb, Adverb
			} else if (cur.postag.startsWith("VB")) {
				WordTag wt = Morphology.stemStatic(cur.word, cur.postag);
				w = wt.word();
				features.add("w=" + w); 
//				for (String v : beVerb) {
//					if (v.equals(cur.word)) {
//						//w = "be";
//						w = cur.word;
//						oneline.add("w=" + w); // be verb
//						break;
//					}
//				}
			} else if (cur.postag.equals("WDT")) {
				w = cur.word;
				features.add("w=" + w); 
			} 
						
			// lexical feature (regex)
//			if (cur.label == "ENT" || cur.label == "NP") {
//				if (w != "") {
//					for (String l : generateLexicalFeature(w)) {
//						features.add(l);
//						if (l.startsWith("regex="))
//							cur.regex = l.replace("regex=", "");
//					}
//				}
//			}
							
			featureForm.add(features);
			featureForm2.add((ArrayList<String>)features.clone());
			cur.w = w; cur.p = p;	
		}
		// end of lexical features
		
		/*
		 * Context features of current position
		 */
		for (int i = 0; i < sequence.size(); i++) {
			Node cur = sequence.get(i);
			String w = cur.w, p = cur.p;
		
			ArrayList<String> features = null;
			if (cur.label == "ENT" || cur.label == "NP") 
				features = featureForm2.get(i);
			else if (cur.postag.startsWith("VB") || cur.postag.equals("IN") || cur.postag.equals("TO"))
				features = featureForm.get(i);
			else
				continue;
			
			// window (prev)
			if (i > 0) {
				Node p1 = sequence.get(i-1);
				features.add("p-1=" + p1.p);
				features.add("p-1&p=" + p1.p +"&"+ p);
				if (w != "")
					features.add("p-1&w=" + p1.p +"&"+ w);
				if (p1.w != "") {
					features.add("w-1=" + p1.w);
					if (w != "")
						features.add("w-1&w=" + p1.w +"&"+ w);
					features.add("w-1&p=" + p1.w +"&"+ p);
				}
				if (p1.regex != null)
					features.add("regex-1=" + p1.regex);
				if (i > 1) {
					Node p2 = sequence.get(i-2);
					features.add("p-2=" + p2.p);
					features.add("p-2&p-1=" + p2.p +"&"+ p1.p);
					features.add("p-2&p-1&p=" + p2.p +"&"+ p1.p + "&" + p);
					if (w != "") 
						features.add("p-2&p-1&w=" + p2.p +"&"+ p1.p + "&" + w);
					if (p2.w != "") {
						features.add("w-2=" + p2.w);
						features.add("w-2&p-1=" + p2.w + "&" + p1.p);
						features.add("w-2&p-1&p=" + p2.w + "&" + p1.p + "&" + p);
						if (p1.w != "") 
							features.add("w-2&w-1=" + p2.w + "&" + p1.w);
					}
					if (p2.regex != null)
						features.add("regex-2=" + p2.regex);
					if (i > 2) {
						Node p3 = sequence.get(i-3);
						features.add("p-3=" + p3.p);
						features.add("p-3&p-2=" + p3.p +"&"+ p2.p);
						features.add("p-3&p-2&p-1=" + p3.p +"&"+ p2.p +"&"+ p1.p);
						//features.add("p-3&p-2&p-1&p=" + p3.p +"&"+ p2.p +"&"+ p1.p +"&"+ p);
						//if (w != "") 
						//	oneline.add("p-3&p-2&p-1&w=" + p3.p +"&"+ p2.p +"&"+ p1.p +"&"+ w);
						if (p3.w != "") {
							features.add("w-3=" + p3.w);
							//if (p2.w != "")
							//	oneline.add("w-3&w-2&p-1&p" + p3.w +"&"+ p2.w +"&"+ p1.p +"&"+ p);

						}
						if (p3.regex != null)
							features.add("regex-3=" + p3.regex);
					}
				}
			}
			
			// window (next)
			if (i < sequence.size() - 1) {
				Node p1 = sequence.get(i+1);
				features.add("p+1=" + p1.p);
				features.add("p&p+1=" + p +"&"+ p1.p);
				if (w != "")
					features.add("w&p+1=" + w + "&"+ p1.p);
				if (p1.w != "") {
					features.add("w+1=" + p1.w);
					if (w != "")
						features.add("w&w+1=" + w +"&"+ p1.w);
					features.add("p&w+1" + p +"&"+ p1.w);
				}
				if (p1.regex != null)
					features.add("regex+1=" + p1.regex);
				if (i < sequence.size() - 2) {
					Node p2 = sequence.get(i+2);
					features.add("p+2=" + p2.p);
					features.add("p+1&p+2=" + p1.p +"&"+ p2.p);
					features.add("p&p+1&p+2=" + p +"&"+ p1.p +"&"+ p2.p);
					if (w != "")
						features.add("w&p+1&p+2" + w +"&"+ p1.p +"&"+ p2.p);
					if (p2.w != "") {
						features.add("w+2=" + p2.w);
						features.add("p+1&w+2=" + p1.p + "&" + p2.w);
						if (p1.w != "") 
							features.add("w+1&w+2=" + p1.w + "&" + p2.w);
						if (w != "")
							features.add("w&p+1&w+2=" + w + "&" + p1.p + "&" + p2.w);
						
					}
					if (p2.regex != null)
						features.add("regex+2=" + p2.regex);
					if (i < sequence.size() - 3) {
						Node p3 = sequence.get(i+3);
						features.add("p+3=" + p3.p);
						features.add("p+2&p+3=" + p2.p +"&"+ p3.p);
						features.add("p+1&p+2&p+3=" + p1.p +"&"+ p2.p +"&"+ p3.p);
						//features.add("p&p+1&p+2&p+3=" + p +"&"+ p1.p +"&"+ p2.p +"&"+ p3.p);
						//if (w != "") {
							//features.add("w&p+1&p2&p+3" + w +"&"+ p1.p +"&"+ p2.p +"&"+ p3.p);
							//if (p1.w != "") {
							//	features.add("w&w+1&p2&p+3" + w +"&"+ p1.w +"&"+ p2.p +"&"+ p3.p);
							//}
						//}
						if (p3.w != "") {
							features.add("w+3=" + p3.w);
						}
						//if (p3.regex != null)
						//	features.add("regex+3=" + p3.regex);
					}
				}
			}
		}
		// end of context features
		
		/*
		 * Context features of ENT1 & ENT2 
		 */
		ArrayList<String> entityFeat = new ArrayList<String>();
		int nEnt = 0;
		ArrayList<Integer> nVerb = new ArrayList<Integer>();
		ArrayList<Integer> nNP = new ArrayList<Integer>();
		int numVerb = 0; boolean inVP = false;
		int numNP = 0;
		for (int i = 0; i < sequence.size(); i++) {
			ArrayList<String> features = featureForm2.get(i);
			Node cur = sequence.get(i);
			if (cur.label == "ENT") {
				nEnt++;
				
				if (nEnt == 1) {
					for (String f : features) 
						if (f != "ENT" && f != "NP")
							entityFeat.add("ent1_"+f);
				}
				if (nEnt == 2) {
					for (String f : features) 
						if (f != "ENT" && f != "NP")
							entityFeat.add("ent2_"+f);	
				}
			}
			
			if (nEnt > 0 && nEnt < 2) {
				if (cur.postag.startsWith("VB")) {
					if (!inVP) {
						numVerb++;
					}
					inVP = true;
				}
				else 
					inVP = false;
				
				if (cur.label == "NP") {
					numNP++;
				}
			}
			nVerb.add(numVerb);	nNP.add(numNP);
		}
		
		int idNearestVB = 0;
		for (int i = 0; i < sequence.size(); i++) {
			ArrayList<String> features = featureForm.get(i);
			Node cur = sequence.get(i);
			if (cur.label == "ENT" || cur.label == "NP")
				continue;
			if (!cur.postag.startsWith("VB") && !cur.postag.equals("IN") && !cur.postag.equals("TO"))
				continue;
			
			//for (String f : entityFeat) {
			//	features.add(cur.w + "|" + f);
				//features.add("V" + nVerb.get(i) + "|" + f);
			//}
			if (numVerb == 0) 
				features.add("noVerb");
			else {
				if (cur.postag.startsWith("VB"))
					features.add("nPrevV="+(nVerb.get(i)-1));
				else
					features.add("nPrevV="+nVerb.get(i));
				features.add("nNextV="+(numVerb - nVerb.get(i)));
			}
			if (numNP == 0) 
				features.add("noNP");
			else {
				features.add("nPrevNP="+nNP.get(i));
				features.add("nNextNP="+(numNP - nNP.get(i)));
			}
			
			if (cur.postag.startsWith("VB"))
				idNearestVB = i;
			
			// nearest verb and ARG
			if (cur.postag.equals("IN") || cur.postag.equals("TO")) {
				if (idNearestVB == 0) {
					features.add("nearestVerb=NONE");
				} else {
					features.add("nearestVerb="+sequence.get(idNearestVB).w);
					features.add("nearestVerbPOS="+sequence.get(idNearestVB).p);
				}
				if (nNP.get(i) == numNP)
					features.add("isNearestPP=true");
			}
		}
		// end of ENT1 & ENT2
		
		return featureForm;
	}
	
	public static ArrayList<ArrayList<String>> generateFeature(ArrayList<Node> sequence, ArrayList<Node> wordForm) {
		ArrayList<ArrayList<String>> featureForm = generateFeature(sequence);
		
		// filter
		ArrayList<ArrayList<String>> ret = new ArrayList<ArrayList<String>>();
		//ArrayList<Node> newSequence = new ArrayList<Node>();
		int entNumber = 0;
		for (int i = 0; i < featureForm.size(); i++) {
			ArrayList<String> oneline = featureForm.get(i);
			if (oneline.get(0).equals("ENT")) {
				entNumber++;
			}
			if (entNumber > 0) {
				ret.add(oneline);
				wordForm.add(sequence.get(i));
			}
			if (entNumber > 1 && oneline.get(0).equals("ENT")) {
				break;
			}
		}
		
		///sequence = newSequence;
		return ret;
	}


}
