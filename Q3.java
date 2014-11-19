import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;


public class Q3 {

	static HashMap<String, Double> tParams;
	static HashMap<String, Double> qParams;
	static ArrayList<String> eLines;
	static ArrayList<String> fLines;
	
	public static void main (String[] args) {
		
		String eScrambled = args[0];
		String fOriginal = args[1];

		String tParamFile = args[2];
		String qParamFile = args[3];
		
		try {
			long startTime = System.currentTimeMillis();
			
			System.out.println("Reading in t Params...");
			tParams = loadTParams(tParamFile);
			System.out.println("Reading in q Params...");
			qParams = loadQParams(qParamFile);
			eLines = readIn(eScrambled);
			fLines = readIn(fOriginal);
			System.out.println("Finding best alignments...");
			ArrayList<String> unscrambled = findBestAlignments (eLines, fLines, tParams, qParams);
			// print the unscrambled sentences to a new file
			System.out.println();
			System.out.println("Printing unscrambled English to unscrambled.en");
			printUnscrambled(unscrambled);
			
			long endTime = System.currentTimeMillis();
			System.out.println("Time taken: " + (endTime - startTime));
		}
		
		catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public static void printUnscrambled (ArrayList<String> unscrambled)  throws IOException{
		FileWriter writer = new FileWriter("unscrambled.en");
		for (String s : unscrambled) {
			writer.write(s + "\n");
		}
		writer.flush();
		writer.close();
	}
	
	public static ArrayList<String> findBestAlignments(ArrayList<String> eLines, ArrayList<String> fLines,
			HashMap<String, Double> tParams, HashMap<String, Double> qParams) {
		ArrayList<String> unscrambled = new ArrayList<String>();
		
		// loop over all German sentences
		for (String fLine : fLines) {
			String[] fComp = fLine.split(" ");
			// m = length of german sentence.
			int m = fComp.length;
			
			// initialize best English sentence. you don't have to print alignment.
			String bestEnglishSentence = "";
			double bestEnglishSentenceScore = 0.0;
			
			// loop over all English sentences.
			for (String eLine : eLines) {
				String eLinePlusNull = "NULL " + eLine;
				String[] eComp = eLinePlusNull.split(" ");
				// l = length of english sentence.
				int l = eComp.length - 1;
				
				String thisEnglishSentence = eLine;
				double thisEnglishSentenceScore = 0.0;
				
				// for each word in German sentence at index i = 1....m
				for (int i = 1; i <=m; i++) {
					//double bestEnglishWordIndex = -1.0;
					double bestEnglishWordScore = 0.0;
					for (int j = 0; j <=l; j++) {
						String qKey = j + " " + i + " " + l + " " + m;
						double qScore;
						if (qParams.get(qKey) == null) {
							qScore = 0.0;
						}
						else {
							qScore = qParams.get(qKey);
						}
				
						String tKey = eComp[j] + " " + fComp[i-1];
						System.out.println(tKey);
						double tScore;
						if (tParams.get(tKey) == null) {
							tScore = 0.0;
						}
						else {
							tScore = tParams.get(tKey);
						}
						System.out.println("Tscore: " + tScore);
						System.out.println("Qscore: " + qScore);
						
						double tq = tScore * qScore;
						if (tq == 0) {
							// Large negative constant for when inner product is 0.
							tq = -10000;
						}
						
						// Only bother if the combined tq score is not 0.
						if (tq > 0) {
							double wordScore = Math.log(tq);
							System.out.println("Log score: " + wordScore + "\n");
							if (tq > bestEnglishWordScore) {
								 bestEnglishWordScore = tq;
								 //bestEnglishWordIndex = j;
							}
						}
						else {
							System.out.println("skipping.\n");
						}
					}
					// Update the sum
					thisEnglishSentenceScore += bestEnglishWordScore;
				}
				if (thisEnglishSentenceScore > bestEnglishSentenceScore) {
					bestEnglishSentenceScore = thisEnglishSentenceScore;
					bestEnglishSentence = thisEnglishSentence;
				}
			}
			unscrambled.add(bestEnglishSentence);
		}
		return unscrambled;
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
	
	public static HashMap<String, Double> loadQParams(String qParamFile) throws IOException {
		BufferedReader readQParams = new BufferedReader(new FileReader(qParamFile));
		String qParam = readQParams.readLine();
		HashMap<String, Double> allQParams = new HashMap<String, Double>();
		while(qParam != null) {
			String[] qParamComp = qParam.split(" ");
			String key = qParamComp[0] + " " + qParamComp[1] + " " + qParamComp[2] + " " + qParamComp[3];
			Double value = Double.parseDouble(qParamComp[4]);
			allQParams.put(key, value);
			qParam = readQParams.readLine();
		}
		readQParams.close();
		return allQParams;
	}
	
	public static ArrayList<String> readIn (String fileName) throws IOException{
		ArrayList<String> lines = new ArrayList<String>();
		FileReader in = new FileReader(fileName);
		BufferedReader buffered = new BufferedReader(in);
		
		String line = buffered.readLine();
		while (line != null) {
			lines.add(line);
			line = buffered.readLine();
		}
		
		buffered.close();
		in.close();
		
		return lines;
	}
}
