package edu.indiana.slis.pubmedgraphtools;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


class Contributor {
	String contributorId = null;
	double weight = 0.0;
	
	public Contributor(String contributorId, double weight) {
		this.contributorId = contributorId;
		this.weight = weight;
	} //end of constructor
	
	/**
	 * updateWeight
	 * The initial weight when constructed was based on all of the 
	 * contributors in the file totalling to 1.  Since we are using 
	 * a subset of those contributors, the denominator is less than 1,
	 * so we need to adjust the weight.  The new weight value is returned.
	 * 
	 * @param denominator  double that should be the denominator for 
	 *                     adjusting the weight.  This would be the 
	 *                     total weight for the subset of the contributors
	 *                     included for the MeSH term. 
	 * @return             double with the new weight value
	 */
	public double updateWeight(double denominator) {
		if(denominator != 0)
			weight = weight / denominator;
		return(weight);
	} //end of updateWeight
	
	/** 
	 * adjustWeight
	 * When the contributor weights are being adjusted, 
	 * they may total to something slightly less than one,
	 * so the difference is applied to one of the contributors
	 * (it should be the first and largest contributor).
	 * @param adjustment  double with the adjustment to the weight
	 * @return            double with the new weight value
	 */
	public double adjustWeight(double adjustment) {
		weight += adjustment;
		return(weight);
	} //end of adjustWeight

	public String getContributorId() {
		return contributorId;
	}

	public double getWeight() {
		return weight;
	}

} //end of class Contributor


/**
 * 
 * @author Rohit Ingle, Scott Jensen, Yingying 
 * This code was originally written by Rohit Ingle and then modified by Scott Jensen and Yingying ___
 * to be packaged, use parameters, weight the edge files, and allow for excluded IDs. 
 *
 */
//this code helps to generate the contributedBy relationship file
public class ContributedBy {
	protected final static String LOG4J_PATH_PROPERTY = "pubmed.log4j"; //name of system property to check
	// Logger object for logging in this class
	protected static Logger log = Logger.getLogger(ContributedBy.class.getName());
	private ArrayList<File> dataDirectories = null;
	private int maxContribute = 0;
	private HashSet<String> excludedIds = null; // used to create a set of IDs that should be excluded when 
	                                    // processing each file.
	private boolean exclusions = false; //default to there being no IDs to exclude
	                                      
	
	private boolean initialized = false;
	
	/**
	 * 
	 * @param dataPath             String with the full path to a directory containing the data files.
	 *                             Each data file is named using a qualified mesh descriptor in the format:<br/>
	 *                             DescriptorId_QualifierId <br/>
	 *                             If there is no qualifier used, then the qualifier ID should be 0<br/>
	 *                             Each file contains rows with two comma-separated values:  <br/>
	 *                             a) The ID of the paper, author, or venue that contributed to that 
	 *                                qualified descriptor<br/>
	 *                             b) The weight (in scientific notation) of the weight given to that ID 
	 *                                in the pagerank calculation.<br/>
	 * @param maxContribute        integer value with the maximum number of IDs to contribute to a qualified 
	 *                             MeSH descriptor in the graph.  If there should be no maximum on the number 
	 *                             of "Contributed By" edges for a qualified descriptor, then this limit 
	 *                             should be set to 0.
	 * @param excludedIDFileName   String with the full path to a file containing IDs to be skipped when 
	 *                             creating the contributed by files. If null, then there are no exclusions.
	 *                             Each ID to be skipped should be listed on its own line in the file.
	 */
	public ContributedBy(String dataPath, int maxContribute, String excludedIDFileName) {
		setupLogging();
		try {
			// put the top-level data directory into the list of data directories
			File dataDir = new File(dataPath);
			if (!dataDir.isDirectory())
				return;  
			dataDirectories = new ArrayList<File>();
			dataDirectories.add(dataDir);
			// Determine if there is a limit on the number of Contribute By 
			// edges for each qualified descriptor.  If zero, set to the maximum
			// integer value.
			this.maxContribute = (maxContribute > 0)? maxContribute : Integer.MAX_VALUE;
			// If there are IDs that should be excluded, populate that set of IDs
			if (excludedIDFileName != null)
				exclusions = loadExclusions(excludedIDFileName);
			log.info("Processed using data from the directory " + 
					dataPath + " with a maximum of " + 
					maxContribute + " contributors.");
			initialized = true;
		} catch (Exception e) {
			log.error("An exception occurred in initializing Contributed By: "
					+ e.getMessage(),e);
		}
		return;
	} //end of constructor
	
	
	/**
	 * setupLogging
	 * This method is used to setup the Log4J logging and is called by the constructor.
	 */
	public static void setupLogging() {
		//set up the logging
    	try {
    		String log4jPath = null;
   			log4jPath = System.getProperty(LOG4J_PATH_PROPERTY);
    		if (log4jPath == null)
    			log4jPath = System.getProperty("user.dir") + File.separator + "log4j.properties";
    		System.out.println("********** " + 
    				" log4jPathProperty: " + LOG4J_PATH_PROPERTY + 
    				" log4jPath: " + log4jPath);
    		PropertyConfigurator.configure(log4jPath);
    		log.debug("Utility-setuplogging: logging started");
    		System.out.println("Utility-setupLogging: logging started");
    		return;
    	} catch (Exception e) {
    		System.err.println("Utility-setupLogging: an error ocurred in starting the logging: " + e.getMessage() );
    	}
	} //end of setupLogging
	
	
	private boolean loadExclusions(String excludedIDFileName) throws IOException {
		BufferedReader reader = null;
		try {
			File exclusionsFile = new File(excludedIDFileName);
			if (!exclusionsFile.canRead() )
				return(false);
			excludedIds = new HashSet<String>(1000);
			reader = new BufferedReader(new FileReader(exclusionsFile));
			String line = reader.readLine();
			while (line != null) {
				String id = line.trim();
				if (id.length() > 0)
					excludedIds.add(id);
				line = reader.readLine();
			} //loop through the IDs to be excluded
			log.info("the excluded IDs were loaded form the file: " + excludedIDFileName);
			return(true); //exclusions were successfully loaded
		} catch (Exception e) {
			try {excludedIds.clear();}catch(Exception ex){}
			throw new IOException("The exclusion file: " + 
					excludedIDFileName + 
					" could not be loaded due to an exception: " + 
					e.getMessage(), e);
		} finally {
			try {reader.close();}catch(Exception e){}
		}
	} //end of loadExclusions
	
	
	/**
	 * @param outFileName   String containing the full path and name of the output file to be written.
	 */
	public void process(String outFileName) throws Exception {
		PrintWriter writer = null;
		File currentDir = null;
		if (!initialized)
			return;
		
		try {
			File outFile = new File(outFileName);
			outFile.createNewFile();
			if (!outFile.canWrite()) {
				String msg = "The file " + outFileName + 
						" cannot be written to, so the data could not be processed.";
				log.error(msg);
				throw new IOException(msg);
			}
			writer = new PrintWriter (outFile);
			while (!dataDirectories.isEmpty()) {
				currentDir = dataDirectories.remove(0);
				String[] dirContents = currentDir.list();
				for (int i = 0; i < dirContents.length; i++) {
					File currentFile = new File(currentDir, dirContents[i]);
					// if the content is a child directory, add it to the list of 
					// directories to process, otherwise process the file.
					if (currentFile.isDirectory())
						dataDirectories.add(currentFile);
					else {
						if (maxContribute > 0)
							processLimitedFile(writer, currentFile);
					}
				} // loop through the directory contents
				dirContents = null;
			} //loop through the data directories
		} catch(Exception e) {
			String msg = "An error occurred in processing the data files, so" + 
					" the output file is likely to be incomplete.";
			log.error(msg,e);
			throw new Exception(msg, e);
		} finally {
			try {
				writer.flush();
				writer.close();
			} catch(Exception e){}
		}
	} //end of process
	
	
	/**
	 * processLimitedFile
	 * This method processes the pagerank output file for a single qualified descriptor.
	 * Each file MUST have a title in the format" type_descriptorId_qualifierId
	 * Where the type is what is contributing to the keyword (e.g., venue, author, etc.)
	 * and both the descriptorId and qualifierId are numeric IDs.  If there is no qualified
	 * used in this particular case, then the qualifierID should be 0.
	 */
	private void processLimitedFile(PrintWriter writer, File currentFile) throws Exception {
		BufferedReader reader = null;
		int count = 0; //number of lines processed
		ArrayList<Contributor> contributors = null;
		try {
			String meshTerm = getMeshTerm(currentFile);
			//Setup an array list of contributors to track the contributors used.
			contributors = new ArrayList<Contributor>(maxContribute);
			// open the file and loop through processing records
			reader = new BufferedReader(new FileReader(currentFile));
			String contributor = reader.readLine();
			while (count < maxContribute && contributor != null) {
				count += processContributor(contributor, contributors, meshTerm);
				contributor = reader.readLine();
			} //loop through the file
			try {reader.close();}catch(Exception e){}
			// *** Adjust the weights ***
			adjustWeights(contributors);
			// Write out the edges with their new weights
			while(!contributors.isEmpty()) {
				Contributor contributorHolder = contributors.remove(0);
				writer.println(meshTerm + "," + 
				               contributorHolder.getContributorId() + "," + 
				               contributorHolder.getWeight() );
			}
			writer.flush();
			return;
		} finally{
			try {reader.close();}catch(Exception e){}
			try {contributors.clear();}catch(Exception e){}
		}
	} //end of processLimitedFile
		
		
	private String getMeshTerm(File file) throws IOException {
		// Get the qualified descriptor ID and qualifier ID
		String fileName = file.getName();
		int splitPos = fileName.indexOf("_");
		if (splitPos < 0) { //the underscore was not found
			String msg = "The file named " + fileName + 
					" could not be processed because the name of the file is not formatted correctly.";
			log.error(msg);
			throw new IOException(msg);
		}
		return(fileName.substring(splitPos+1) );
	} //end of getMeshTerm
	
	
	private int processContributor(String contributor, ArrayList<Contributor> contributors, String meshTerm) {
		int added = 0; // this contributor has not been added yet
		String[] contributorFields = contributor.split(",");
		// If either we are not making any exclusions, or the
		// ID is not in the set of excluded IDs, then add it
		// to the list of contributors.
		if (!exclusions || !excludedIds.contains(contributorFields[0])) {
			contributors.add(new Contributor(contributorFields[0], 
					Double.parseDouble(contributorFields[1]) ) );
			added = 1;
		} else {
			log.info("For the MeSH qualified descriptor " + 
					meshTerm + " the contributor " + 
					contributorFields[0] + " was excluded.");
		}
		return (added);
	} //end of processContributor
	
	private void adjustWeights(ArrayList<Contributor> contributors) {
		
		// Get the total weight of the contributors included
		// The total becomes the denominator for adjusting the weights.
		double total = 0.0;
		for (Contributor contributor: contributors)
			total += contributor.getWeight();
		// Adjust the weights
		double adjTotal = 0.0; // the total for the adjusted amounts
		for (Contributor contributor: contributors)
			adjTotal += contributor.updateWeight(total); 
		// apply any difference to the first (greatest) weight edge
		contributors.get(0).adjustWeight(1-adjTotal);
		return;
	} //end of adjustWeights
	
	/**
	 * 
	 * @param args  The following parameters are used: </br>
	 * 1) data file path: This is a string with the full path to a directory containing the data files.
	 *                    Each data file is named using a qualified mesh descriptor in the format:<br/>
	 *                    DescriptorId_QualifierId <br/>
	 *                    If there is no qualifier used, then the qualifier ID should be 0<br/>
	 *                    Each file contains rows with two comma-separated values:  <br/>
	 *                    a) The ID of the paper, author, or venue that contributed to that qualified descriptor<br/>
	 *                    b) The weight (in scientific notation) of the weight given to that ID in the pagerank calculation.<br/>
	 * 2) Output file name as a String containing the full path and name of the output file to be written.
	 * 3) An integer value with the maximum number of IDs to contribute to a qualified MeSH descriptor in the graph.
	 *    If there should be no maximum on the number of "Contributed By" edges for a qualified descriptor, then 
	 *    this limit should be set to 0.
	 * 4) Optional full path to a file containing IDs to be skipped when creating the contributed by files.
	 *    Each ID to be skipped should be listed on its own line in the file.
	 *                       
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		ContributedBy contributedBy = null;
		String dataPath = null;
		String outFileName = null;
		int maxContribute = 0; //0 indicates no limit
		String excludedIDFileName = null;
		
		if (args.length < 3) {
			usage();
			return;
		}
		dataPath = args[0]; // path to a directory containing data files - 
		outFileName = args[1];
		try {
			maxContribute = Integer.parseInt(args[2]);
			if (maxContribute < 0) {
				usage();
				return;
			}
		} catch (NumberFormatException e) {
			usage();
			return;
		}
		if (args.length > 3)
			excludedIDFileName = args[3];
		
		contributedBy = new ContributedBy(dataPath, maxContribute, excludedIDFileName);
		if (contributedBy.initialized)
			contributedBy.process(outFileName);
		else
			usage();
		System.out.println("Done");
                // The remainder of this method is the original main method and is no longer used 

//		File folder = new File("C:/Users/Rohit/Downloads/SCOTT/PageRankResultsUsed/PageRankResultsUsed/VenueSorted_01-23-15");
//		File[] listOfFiles = folder.listFiles();
//		BufferedWriter writer = new BufferedWriter (new FileWriter("C:/Users/Rohit/Downloads/SCOTT/PageRankResultsUsed/venue_contribute.csv",true));
//		BufferedReader reader;
//		String line = "";
//		for (int i = 0; i < listOfFiles.length; i++) {
//		      if (listOfFiles[i].isFile()) {
//		    	  String x = listOfFiles[i].getName();
//		    	  reader = new BufferedReader(new FileReader("C:/Users/Rohit/Downloads/SCOTT/PageRankResultsUsed/PageRankResultsUsed/VenueSorted_01-23-15/"+x));
//		    	  int count = 0;
//		    	  while((line=reader.readLine())!=null && count<50) {
//		    		//  System.out.println(line);
//		    		  
//		    		  writer.append(x.split("_")[1] + "_" + x.split("_")[2] + "," + line.split(",")[0] + "," + line.split(",")[1]);
//		    		  writer.newLine();
//		    		  count++;
//		    	  }
//		    	  
//		    	  
//		    	// System.out.println(x);
//		    	  //System.out.println(x.split("_")[1] + "_" + x.split("_")[2]); 
//		    	  //writer.append(x.split("_")[1] + "_" + x.split("_")[2] + "," + x.split("_")[1] + "," + x.split("_")[2]);
//		    	  //writer.newLine();
//		    	  //System.out.println("File " + listOfFiles[i].getName());
//		      } else if (listOfFiles[i].isDirectory()) {
//		    	  //System.out.println("Directory " + listOfFiles[i].getName());
//		      }
//		    }
//		writer.close();
	} //end of main
	
	private static void usage() {
		System.out.println("ContributedBy data directory path, output file name, max number of contributors, [optional excluded IDs file]");
	} //end of usage
	
} //end of class ContributedBy
