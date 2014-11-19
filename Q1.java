import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.*;
import java.io.*;
import java.util.PriorityQueue;

public class Q1 {

	static HashMap<String, HashSet<String>> possibleTranslations;
	static HashMap<String, Double> tParams;
	static HashSet<String> eWords;
	static HashSet<String> fWords;
	static ArrayList<String> eLines;
	static ArrayList<String> fLines;
	
	public static void main (String[] args) {
		
		String eCorpus = args[0];
		String fCorpus = args[1];
		
		ArrayList<String> unzippedE = decompress(eCorpus);
		ArrayList<String> unzippedF = decompress(fCorpus);
		eWords = new HashSet<String>();
		fWords = new HashSet<String>();
		// Contents of file
		eLines = new ArrayList<>();
		fLines = new ArrayList<>();
		
		try {
			long startTime = System.currentTimeMillis();
			if (unzippedE.size() != unzippedF.size()) {
				System.out.println("ERROR: Unequal file sizes.");
				return;
			}
			
			possibleTranslations = storeTranslations(unzippedE, unzippedF);
			
			System.out.println("Generating tParams...");
			long tParamStart = System.currentTimeMillis();
			// Create another hash table for t(f|e) parameters.
			// key: e f. value: 1 / number of items in the hashset of e.
			tParams = countTranslations(possibleTranslations);
			long tParamEnd = System.currentTimeMillis();
			long tParamTime = tParamEnd - tParamStart;
			System.out.println("Done. Took " + tParamTime + " ms");
			
			
			// Run EM algorithm 5 times 
			System.out.println("Running EM algorithm...");
			HashMap<String, Double> EMoutput = EM(tParams, eLines, fLines, 5);
			
			// Read in devwords.
			System.out.println("\nPrinting top 10 German translations for every word in devwords...");
			File devwords = new File("devwords.txt");
			ArrayList<String> words = readFile(devwords);
			
			// Print the top 10 items in that list (the words and the parameter values).
			printTopTen(words, EMoutput);
			
			// Print the alignments of the top 20 sentences.
			System.out.println("Aligning first 20 sentences in training data...");
			File aligned = new File("First20Alignments1.txt");
			FileWriter writer = new FileWriter(aligned);
			for (int i = 0; i < 20; i ++) {
				String eLine = eLines.get(i).replace("NULL", "").trim();
				System.out.println(eLine);
				writer.write(eLine + "\n");
				System.out.println(fLines.get(i));
				writer.write(fLines.get(i) + "\n");
				ArrayList<Integer> alignment = findAlignment(eLines.get(i), fLines.get(i), EMoutput);
				System.out.println(alignment);
				writer.write(alignment + "\n");
			}
			writer.close();
			
			FileWriter tParamWriter = new FileWriter("tParams.txt");
			Set<Entry<String, Double>> tParamList = tParams.entrySet();
			for (Entry<String, Double> tParam : tParamList) {
				String key = tParam.getKey();
				Double value = tParam.getValue();
				tParamWriter.write(key + " " + value + "\n");
			}
			tParamWriter.close();
			
			long endTime = System.currentTimeMillis();
			System.out.println("Total time taken: " + (endTime - startTime));
			
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public static ArrayList<Integer> findAlignment(String eLine, String fLine, HashMap<String, Double> tFEParams) {
		String[] fWords = fLine.split(" ");
		String[] eWords = eLine.split(" ");
		ArrayList<Integer> alignment = new ArrayList<Integer>();
		
		// For each French word, find the English word with the best score. Write the index of that word
		// to the array.

		for (int i = 0; i < fWords.length; i++) {
			int bestIndex = -1;
			double bestScore = 0;
			for (int j = 0; j < eWords.length; j++) {
				String fWord = fWords[i];
				String eWord = eWords[j];
				String key = eWord.concat(" ").concat(fWord);
				if (tParams.get(key) > bestScore) {
					bestScore = tParams.get(key);
					bestIndex = j;
				}
			}
			alignment.add(bestIndex);
		}
		return alignment;
	}
	
	public static void printTopTen(ArrayList<String> words, HashMap<String, Double> tFEParams) throws IOException {
		
		File topTen = new File("Top10Translations.txt");
		FileWriter writer = new FileWriter(topTen);
		
		for (String word : words) {
			HashSet<String> allFs = possibleTranslations.get(word);
			PriorityQueue<TEFParam> bestTranslations = new PriorityQueue<TEFParam>();
			for (String f : allFs) {
				// Find the 10 fs with highest tFE params.
				String key = word.concat(" ").concat(f);
				TEFParam tParam = new TEFParam(f, word, tFEParams.get(key));
				bestTranslations.add(tParam);
			}
			
			System.out.println("E: " + word);
			writer.write("E: " + word + "\n");
			
			ArrayList<String> bestTrans = new ArrayList<String>();
			for (int i = 0; i < 10; i++) {
				TEFParam best = bestTranslations.poll();
				bestTrans.add("F: " + best.f + ": " + best.tEF);
				System.out.println("F: " + best.f + ": " + best.tEF);
			}
			writer.write(bestTrans.toString() + "\n");
			System.out.println("\n");
		}
		writer.close();
	}
	
	public static HashMap<String, Double> EM(HashMap<String, Double> tParams,
			ArrayList<String> eLines, ArrayList<String> fLines, int iterations) {
		HashMap<String, Double> counts = new HashMap<String, Double>();
		
		for (int s = 1; s <= iterations; s++) {
			long timestampStart = System.currentTimeMillis();
			for (int k = 1; k <= eLines.size(); k++) {
				// ** k-1 to make indices line up
				String kFLine = fLines.get(k-1); // size mk
				String kELine = eLines.get(k-1); // size lk

				String[] kEComp = kELine.split(" ");
				String[] kFComp = kFLine.split(" ");
				
				for (int i = 0; i < kFComp.length; i++) {
					// Calculate just the denominator for delta
					double denom = 0;
					for (int j = 0; j < kEComp.length; j++) {
						String cEFKey = kEComp[j].concat(" ").concat(kFComp[i]);
						//System.out.println(cEFKey);
						double tParam = tParams.get(cEFKey);
						denom += tParam;
					}
					
					for (int j = 0; j < kEComp.length; j++) {
						// Set c(e, f)
						String cEFKey = kEComp[j].concat(" ").concat(kFComp[i]);
						double cEFVal = 0;
						if (counts.containsKey(cEFKey)) {
							cEFVal = counts.get(cEFKey);
						}
						
						// Set c(e)
						String cEKey = kEComp[j];
						double cEVal = 0;
						if (counts.containsKey(cEKey)) {
							cEVal = counts.get(cEKey);
						}
						
						// calculate delta(k,i,j)
						double num = tParams.get(cEFKey);
						double delta = num / denom;
						
						cEFVal = cEFVal + delta;
						cEVal = cEVal + delta;
						counts.put(cEFKey, cEFVal);
						counts.put(cEKey, cEVal);
					} // end j loop
				} // end i loop
			} // end k loop
			
			// set tParams for all valid combos of f and e.
			for (String e : eWords) {
				// Number of possible foreign translations for each e
				for (String f : possibleTranslations.get(e)) {
					
					String ef = e.concat(" ").concat(f);
					double cEF = counts.get(ef);
					double cE = counts.get(e);
					double tFE = cEF / cE;
					tParams.put(ef, tFE);
				}
			}
			long timestampEnd = System.currentTimeMillis();
			long timeTaken = timestampEnd - timestampStart;
			System.out.println("Done with iter " + s + ": " + timeTaken + " ms");
		}// end sVal
		return tParams;
	}
	
	public static ArrayList<String> readFile (File devwords) throws IOException{
		ArrayList<String> lines = new ArrayList<String>();
		BufferedReader devReader = new BufferedReader(new FileReader(devwords));
		String line = devReader.readLine();
		while(line != null) {
			lines.add(line);
			line = devReader.readLine();
		}
		devReader.close();
		return lines;
	}
	
	public static HashMap<String, Double> countTranslations(HashMap<String, HashSet<String>> possibleTranslations) {
		HashMap<String, Double> translCounts = new HashMap<String, Double>();
		for (String e : eWords) {
			// Number of possible foreign translations for each e
			HashSet<String> allPossFs = possibleTranslations.get(e);
			for (String f : allPossFs) {
				String key = e.intern().concat(" ").concat(f.intern());
				Double numTrans = (double) allPossFs.size();
				Double tParam = 1 / numTrans;
				translCounts.put(key, tParam);
			}
		}
		return translCounts;
	}
	
	// Third part of question 1
	
	public static HashMap<String, HashSet<String>> storeTranslations(ArrayList<String> unzippedE, ArrayList<String> unzippedF) {
		HashMap<String, HashSet<String>> posTransl = new HashMap<String, HashSet<String>>();
		String nullWord = "NULL ";
		eWords.add(nullWord.trim());
		
		String eLine;
		String fLine;
		for (int i = 0; i < unzippedE.size(); i++) {
			eLine = nullWord.concat(unzippedE.get(i));
			fLine = unzippedF.get(i);
			eLines.add(eLine);
			fLines.add(fLine);
			
			String[] eComp = eLine.split(" ");
			String[] fComp = fLine.split(" ");
			for (String eString : eComp) {
				eWords.add(eString);
				HashSet<String> translations;
				if (posTransl.containsKey(eString)) {
					translations = posTransl.get(eString);
				}
				else {
					translations = new HashSet<String>();
				}
				
				for (String fString : fComp) {
					fWords.add(fString);
					translations.add(fString);
				}
				posTransl.put(eString, translations);
			}
		}
		return posTransl;
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
