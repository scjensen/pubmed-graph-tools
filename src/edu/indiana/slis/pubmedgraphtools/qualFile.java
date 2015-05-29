package edu.indiana.slis.pubmedgraphtools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;


public class qualFile {

	public static void main(String[] args) throws IOException {
		BufferedReader reader1 = new BufferedReader (new FileReader ("src/resources/QD/qualifier.txt"));
		BufferedReader reader2 = new BufferedReader (new FileReader ("src/resources/QD/top3000QualifiedDescriptorsUsed.tsv"));
		BufferedWriter writer = new BufferedWriter (new FileWriter ("src/resources/QD/output2.csv"));
		String line = "";
		String line2 = "";
		String descID;
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("0", "");
		
		while((line=reader1.readLine())!=null) {
			map.put(line.split("\\t")[0], line.split("\\t")[1]);
		}
		reader1.close();
		System.out.println(map.size());
		
		while((line2=reader2.readLine())!=null) {
			descID = line2.split("\\t")[1];
			
			for(String x : map.keySet()) {
				if(x.equals(descID)) {
					writer.append(descID + "," + map.get(x));
					writer.newLine();
				}
			}			
		}
		reader2.close();
		writer.close();
	}
}
