package edu.indiana.slis.pubmedgraphtools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.Buffer;
import java.util.HashMap;


public class coAuthor {
	static String firstLine = "";	
	static BufferedWriter writer;
	
	public static void main(String[] args) throws IOException {
		//input file - tab separated file provided by Scott
		BufferedReader reader = new BufferedReader(new FileReader("src/Author-CoAuthor-3paper-threshold.tsv"));		
		String firstTerm="";
		String line = "";
		HashMap<String, Integer> map = new HashMap<String, Integer>();

		firstLine = reader.readLine();
		firstTerm = firstLine.split("\\t")[0];
		map.put(firstLine.split("\\t")[1], Integer.parseInt(firstLine.split("\\t")[2]));
				
		while((line=reader.readLine())!=null) {
			if(firstTerm.equals(line.split("\\t")[0])) {
				map.put(line.split("\\t")[1], Integer.parseInt(line.split("\\t")[2]));
			}
			else {
				calculateWeight(firstTerm,map);
				map.clear();
				firstTerm = line.split("\\t")[0];
				map.put(line.split("\\t")[1], Integer.parseInt(line.split("\\t")[2]));
			}
		}
		reader.close();
	}
		

	private static void calculateWeight(String firstTerm, HashMap<String, Integer> map) throws IOException {
		//output file
		BufferedWriter writer = new BufferedWriter(new FileWriter("src/CoAuthor.csv",true));
		float sum = 0;
		for(String x : map.keySet()) {
			sum = sum + map.get(x);
		}
		sum = 1/sum;
		
		for(String x : map.keySet()) {
			//System.out.println(firstTerm + "," + x + "," + (map.get(x)*sum));
			writer.append(firstTerm + "," + x + "," + (map.get(x)*sum));
			writer.newLine();
		}
		writer.close();
	}
}
