import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;


public class Q2 {

	static HashMap<String, Double> tParams;
	static HashMap<String, Double> qParams;
	static HashMap<String, HashSet<String>> possibleTranslations;
	static ArrayList<String> eLines;
	static ArrayList<String> fLines;
	public static void main (String[] args) {
		
		String eCorpus = args[0];
		String fCorpus = args[1];
		String tParamFile = args[2];
		
		ArrayList<String> unzippedE = decompress(eCorpus);
		ArrayList<String> unzippedF = decompress(fCorpus);
		
		possibleTranslations = new HashMap<String, HashSet<String>>();
		eLines = new ArrayList<String>();
		fLines = new ArrayList<String>();
		
		try {
			long startTime = System.currentTimeMillis();
			// Initialize q parameters to uniform distribution over
			// all j for each i, l, and m.
			// q (j | i, l, m) = 1 / l+1
			if (unzippedE.size() != unzippedF.size()) {
				System.out.println("ERROR: Unequal file sizes.");
				return;
			}
			System.out.println("Generating q params and possible translations...");
			qParams = generateQParams(unzippedE, unzippedF);
			
			// Initialize t(f|e) params using parameters from IBM model 1
			System.out.println("Loading tParams...");
			tParams = loadTParams(tParamFile);
			
			// Extend implementation of EM algorithm to IBM Model 2.
			System.out.println("Executing EM algorithm...");
			ArrayList<HashMap<String, Double>> tAndqParams = IBM2(eLines, fLines, 5);
			HashMap<String, Double> newTParams = tAndqParams.get(0);
			HashMap<String, Double> newQParams = tAndqParams.get(1);
			// Run 5 iterations of EM for IBM model 2
			
			// Use the model to compute alignments for sentence pairs.
			// Print alignments for the first 20 sentences as before.
			System.out.println("Aligning the first 20 sentences in training data...");
			printAlignments("First20Alignments2.txt", eLines, fLines, newTParams, newQParams);
			
			// Comment on the difference in model performance.
			long endTime = System.currentTimeMillis();
			System.out.println("Q5 time taken: " + (endTime - startTime));
			
			// Write out the parameters for use in the next part of the question.
			saveTandQParams(newTParams, newQParams);
			
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	static void saveTandQParams(HashMap<String, Double> Ts, HashMap<String, Double> Qs) throws IOException{
		FileWriter tParamWriter = new FileWriter("tParams2.txt");
		Set<Entry<String, Double>> tParamList = Ts.entrySet();
		for (Entry<String, Double> tParam : tParamList) {
			String key = tParam.getKey();
			Double value = tParam.getValue();
			tParamWriter.write(key + " " + value + "\n");
		}
		tParamWriter.close();
		
		FileWriter qParamWriter = new FileWriter("qParams2.txt");
		Set<Entry<String, Double>> qParamList = Qs.entrySet();
		for (Entry<String, Double> qParam : qParamList) {
			String key = qParam.getKey();
			Double value = qParam.getValue();
			qParamWriter.write(key + " " + value + "\n");
		}
		qParamWriter.close();
	}
	
	static void printAlignments(String destFile, ArrayList<String> eLines, ArrayList<String> fLines,
			HashMap<String, Double> tParams, HashMap<String, Double> qParams) throws IOException{
		File aligned = new File(destFile);
		FileWriter writer = new FileWriter(aligned);
		for (int i = 0; i < 20; i ++) {
			String eLine = eLines.get(i).replace("NULL", "").trim();
			System.out.println(eLine);
			writer.write(eLine + "\n");
			System.out.println(fLines.get(i));
			writer.write(fLines.get(i) + "\n");
			ArrayList<Integer> alignment = findAlignment(eLines.get(i), fLines.get(i), tParams, qParams);
			System.out.println(alignment);
			writer.write(alignment + "\n");
		}
		writer.close();
	}
	
	public static ArrayList<Integer> findAlignment(String eLine, String fLine, HashMap<String, Double> tFEParams,
			HashMap<String, Double> qParams) {
		String eLinePlusNull = "NULL " + eLine;
		String[] fWords = fLine.split(" ");
		String[] eWords = eLinePlusNull.split(" ");
		ArrayList<Integer> alignment = new ArrayList<Integer>();
		
		// For each French word, find the English word with the best score. Write the index of that word
		// to the array.
		
		// TODO: change this to work with q params too

		int l = eWords.length;
		int m = fWords.length;
		
		for (int i = 1; i <=m; i++) {
			int bestIndex = -1;
			double bestScore = 0;
			for (int j = 0; j < l; j++) {
				String fWord = fWords[i-1];
				String eWord = eWords[j];
				String tKey = eWord.concat(" ").concat(fWord);
				Double tScore = tParams.get(tKey);
				
				String JILMKey = j + " " + i + " " + l + " " + m;
				Double qScore = qParams.get(JILMKey);
				
				Double combinedScore = tScore * qScore;
				
				if (combinedScore > bestScore) {
					bestScore = combinedScore;
					bestIndex = j;
				}
			}
			alignment.add(bestIndex);
		}
		return alignment;
	}
	
	public static ArrayList<HashMap<String, Double>> IBM2 (ArrayList<String> eLines, ArrayList<String> fLines,
			int iterations) {
		HashMap<String, Double> counts = new HashMap<String, Double>();
		HashMap<Integer, HashSet<Integer>> possibleLMs = new HashMap<Integer, HashSet<Integer>>();
		String nullWord = "NULL ";
		
		for (int s = 1; s <= iterations; s++) {
			long timestampStart = System.currentTimeMillis();
			for (int k = 1; k <= eLines.size(); k++) {
				// ** k-1 to make indices line up
				String kFLine = fLines.get(k-1); // size mk
				String kELine = nullWord + eLines.get(k-1); // size lk

				String[] kEComp = kELine.split(" ");
				String[] kFComp = kFLine.split(" ");
				
				int m = kFComp.length;
				int l = kEComp.length;
				HashSet<Integer> possibleMs = new HashSet<Integer>();
				if (possibleLMs.containsKey(l)) {
					possibleMs = possibleLMs.get(l);
				}
				possibleMs.add(m);
				possibleLMs.put(l, possibleMs);
				
				for (int i = 1; i <= m; i++) {
					// Calculate just the denominator for delta
					double denom = 0;
					for (int j = 0; j < l; j++) {
						String cEFKey = kEComp[j].concat(" ").concat(kFComp[i-1]);
						String jILMKey = j + " " + i + " " + l + " " + m;
						
						if (l == 1 && m == 1) {
							System.out.println();
						}

						//System.out.println(cEFKey);
						double tParam = tParams.get(cEFKey);
						double qParam = qParams.get(jILMKey);
						double txq = tParam * qParam;
						denom += txq;
					}
					
					for (int j = 0; j < l; j++) {
						// Create keys for c(e,f), c(e), c(j|ilm), c(ilm)
						String cEFKey = kEComp[j].concat(" ").concat(kFComp[i-1]);
						String cEKey = kEComp[j];
						String cJILMKey = j + " " + i + " " + l + " " + m;
						String cILMKey = i + " " + l + " " + m;
						
						// Get old cEF val if it exists
						double cEFVal = 0;
						if (counts.containsKey(cEFKey)) {
							cEFVal = counts.get(cEFKey);
						}
						// Get old cE val if it exists
						double cEVal = 0;
						if (counts.containsKey(cEKey)) {
							cEVal = counts.get(cEKey);
						}
						// Get old cJILM val if it exists
						double cJILMVal = 0;
						if (counts.containsKey(cJILMKey)) {
							cJILMVal = counts.get(cJILMKey);
						}
						// Get old cILM val if it exists
						double cILMVal = 0;
						if (counts.containsKey(cILMKey)) {
							cILMVal = counts.get(cILMKey);
						}
						
						// calculate delta(k,i,j)
						double num = tParams.get(cEFKey) * qParams.get(cJILMKey);
						double delta = num / denom;
						
						cEFVal = cEFVal + delta;
						cEVal = cEVal + delta;
						cJILMVal = cJILMVal + delta;
						cILMVal = cILMVal + delta;
						counts.put(cEFKey, cEFVal);
						counts.put(cEKey, cEVal);
						counts.put(cJILMKey, cJILMVal);
						counts.put(cILMKey, cILMVal);
					} // end j loop
				} // end i loop
			} // end k loop
			
			// set tParams for all valid combos of f and e.
		
			for (String e : possibleTranslations.keySet()) {
				// Number of possible foreign translations for each e
				for (String f : possibleTranslations.get(e)) {
					
					String ef = e.concat(" ").concat(f);
					double cEF = counts.get(ef);
					double cE = counts.get(e);
					double tFE = cEF / cE;
					tParams.put(ef, tFE);
				}
			}
			// update q params
			for (int l : possibleLMs.keySet()) {
				for (int m : possibleLMs.get(l)) {
					for (int i = 1; i <= m; i++) {
						for (int j = 0; j < l; j++) {
							String ilm = i + " " + l + " " + m;
							String jilm = j + " " + ilm;
							double cJILM = counts.get(jilm);
							double cILM = counts.get(ilm);
							double qJILM = cJILM / cILM;
							qParams.put(jilm, qJILM);
						}
					}
				}
			}	
			long timestampEnd = System.currentTimeMillis();
			long timeTaken = timestampEnd - timestampStart;
			System.out.println("Done with iter " + s + ": " + timeTaken + " ms");
		}// end sVal
		ArrayList<HashMap<String, Double>> tAndq = new ArrayList<HashMap<String, Double>>();
		tAndq.add(tParams);
		tAndq.add(qParams);
		return tAndq;
	}
	
	public static HashMap<String, Double> generateQParams(ArrayList<String> english, ArrayList<String> foreign) {
		HashMap<String, Double> allQParams = new HashMap<String, Double>();
		String nullWord = "NULL ";
		for (int n = 0; n < english.size(); n++) {
			String eLine = english.get(n);
			eLines.add(eLine);
			
			String ePlusNull = nullWord + eLine;
			String[] eComp = ePlusNull.split(" ");
			
			int l = eComp.length;
			
			String fLine = foreign.get(n);
			fLines.add(fLine);
			String[] fComp = fLine.split(" ");
			
			// Add words to possibleTranslations (bookkeeping to use later in IBM2)
			for (String eWord : eComp) {
				HashSet<String> translations;
				if (possibleTranslations.containsKey(eWord)) {
					translations = possibleTranslations.get(eWord);
				}
				else {
					translations = new HashSet<String>();
				}
				
				for (String fString : fComp) {
					translations.add(fString);
				}
				possibleTranslations.put(eWord, translations);
			}
			
			int m = fComp.length;
			
			for (int i = 1; i <= m; i++) {
				for (int j = 0; j < l; j++) {
					String key = j + " " + i + " " + l + " " + m;
					Double value = 1 / ((double) l + 1);
					if (l == 1 && m == 1) {
						System.out.println();
					}
					allQParams.put(key, value);
				}
			}

		}
		return allQParams;
	}
	
	public static HashMap<String, Double> loadTParams(String tParamFile) throws IOException {
		BufferedReader readTParams = new BufferedReader(new FileReader(tParamFile));
		String tParam = readTParams.readLine();
		HashMap<String, Double> allTParams = new HashMap<String, Double>();
		while(tParam != null) {
			String[] tParamComp = tParam.split(" ");
			String key = tParamComp[0] + " " + tParamComp[1];
			Double value = Double.parseDouble(tParamComp[2]);
			allTParams.put(key, value);
			tParam = readTParams.readLine();
		}
		readTParams.close();
		return allTParams;
	}
	
	public static ArrayList<String> decompress(String zipFileName) {
		ArrayList<String> unzipped = new ArrayList<String>();
		try {
			FileInputStream in = new FileInputStream(zipFileName);
			GZIPInputStream gZIPIn = new GZIPInputStream(in);
			Reader decoder = new InputStreamReader(gZIPIn);
			BufferedReader buffered = new BufferedReader(decoder);
			
			String line = buffered.readLine();
			while (line != null) {
				unzipped.add(line);
				line = buffered.readLine();
			}
			
			buffered.close();
			decoder.close();
			in.close();
			gZIPIn.close();
			
			return unzipped;
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
 }
