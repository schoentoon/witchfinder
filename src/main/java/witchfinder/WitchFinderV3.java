package witchfinder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.Random;

public class WitchFinderV3 {
	protected int hutcount;

	protected long startSeed, endSeed, seedsToCheck;
	protected int numberOfThreads,radius;	
	protected String[] contSeeds;
	
	protected String startMessage;
	
	private WitchFinderV3() {
		seedsToCheck = 1000000000; //default is 1 billion
		numberOfThreads = 8; // default is 8 threads
		radius = 128; // default is 128
		
		hutcount = 0;
	}
	
	public void process() throws Exception {
		// calculate startTime
		long startTime = System.currentTimeMillis();
		
		System.out.println(startMessage + (seedsToCheck/1000000) + 
			" million seeds (" + startSeed + "-" + endSeed + ") with quality setting 0 " +
				" with a radius of " + radius + " chunks on " + numberOfThreads + " threads:" + System.lineSeparator());
		
		
		//calculate number of seeds per thread
		long seedsPerThread = seedsToCheck / numberOfThreads;
		File jarFile = new File("minecraft_server.1.6.4.jar");

		/*
		 * Initialize WitchFinderThreads and start them
		 */
		WitchFinderThread[] threads = new WitchFinderThread[numberOfThreads];
		if(contSeeds == null)
			for(int i=0; i<numberOfThreads;) {
				threads[i] = new WitchFinderThread(this, startSeed, ++i<numberOfThreads? startSeed += seedsPerThread: endSeed, radius, jarFile);
			}
		else
			for(int i=0; i<numberOfThreads;) {
				threads[i] = new WitchFinderThread(this, Long.parseLong(contSeeds[i++]), Long.parseLong(contSeeds[2*i-1]), radius, jarFile);
			}
		
		for(WitchFinderThread wf: threads) wf.start();

		/*
		 * Initialize Update Thread, and start it
		 */
		UpdateThread updateThread = new UpdateThread(threads, startTime, seedsToCheck);
				
		updateThread.setDaemon(true);
		updateThread.start();
		
		/*
		 * Join to the WitchFinderThreads and wait for them to complete
		 */
		for(WitchFinderThread wf: threads) {
			try {
				wf.join();
			} 
			catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//calculate passed time
		int calcTime = (int)(System.currentTimeMillis() - startTime);
		int seconds = calcTime / 1000;
		int minutes = seconds / 60;
		int hours = minutes / 60;
		seconds -= minutes * 60;
		minutes -= hours * 60;
		
		System.out.println("found " + hutcount + " witch huts in total");
		System.out.format("calculation took %02d:%02d:%02d (%.3f ns per seed)%n%n", hours, minutes, seconds,
				((double)calcTime * 1000000 / seedsToCheck), ((double) calcTime * 1000000000 / (seedsToCheck * radius * radius * 4)));
	}
	
	public static void main(String[] args) throws Exception {
		WitchFinderV3 wf = initialize(args);
		wf.process();
	}
	
	public synchronized void outputCandidate(Candidate candidate) {
 		hutcount++;
 		
 		System.out.format("%tT: %s", new Date(), candidate);
 		
 		if(candidate.hutCount == 4 && candidate.maxDistanceToCenter < 115) 
 			System.out.println("This is a really good quad witch hut! Please send the candidate above to BBInME, JL2579 or TheCodeRaider!"); //;D
		
 		if(candidate.hutCount == 3 && candidate.totalWitchSpawningArea > 504 ) 
 			System.out.println("This is a really good triple witch hut with a temple part way in a swamp! Please send the candidate above to BBInME, JL2579 or TheCodeRaider!"); //;D
 		System.out.println();
 	}
	
	public static WitchFinderV3 initialize(String[] args) {
		
		WitchFinderV3 wf = new  WitchFinderV3();
		
		BufferedReader argsReader = null;
		try{
			argsReader = new BufferedReader(new FileReader("args.txt"));
		}
		catch(FileNotFoundException f){
			// ignore, argsReader will be null
		}
		
		/*
		 * If there is a continue.txt file then read the seeds in it
		 */
		BufferedReader contReader = null;
		
		try{
			contReader = new BufferedReader(new FileReader("continue.txt"));
			
			if(contReader != null){
				wf.contSeeds = contReader.readLine().split(" ");			
			}
		}
		catch(IOException f){
			// ignore, contSeeds will be null
		}
		finally {
			try {
				if(contReader != null)
					contReader.close();
			} 
			catch (IOException e) {
				// ignore
			}
				
		}
		
		
		/*
		 * process the input parameters (command line, args.txt or default value)
		 */
		
		// read first line, startseed
		String line = null;
		if(argsReader != null)
			try {
				line = argsReader.readLine();
			} 
			catch (IOException e1) {
				// ignore, line will be null
			}
		
		/*
		 * Initialize startSeed, check input args first, then args.text, finally use a random number
		 */
		try{
			if ( args.length > 0)
				wf.startSeed = Long.parseLong(args[0]) * 1000000;
			else 
				if ( line != null)
					wf.startSeed = Long.parseLong(line) * 1000000;
				else
					// default to a random number
					wf.startSeed = new Random().nextLong();
		}
		catch(NumberFormatException e){
			// default to a random number
			wf.startSeed = new Random().nextLong();
		}
		
		// read second line, number of seeds to check
		if(argsReader != null) 
			try {
				line = argsReader.readLine();
			} 
			catch (IOException e1) {
				// ignore, line will be null
			}
		
		/*
		 * Initialize seedsToCheck, check continue.txt, then input args , then args.text, 
		 * finally use the default
		 */		
		try {
			if(wf.contSeeds != null) {
				
				int i =0;
				wf.seedsToCheck = 0;
				while (i< wf.contSeeds.length)  {
					wf.seedsToCheck += -Long.parseLong(wf.contSeeds[i++]) + Long.parseLong(wf.contSeeds[i++]);
				}
			}
			else {
				if (args.length > 1) {
					wf.seedsToCheck = Long.parseLong(args[1]) * 1000000;
				}
				else {
					if (line != null && line.length() > 0 )
						wf.seedsToCheck = Long.parseLong(line) * 1000000;
					
					// otherwise keep the default
				}
	
			}
		}
		catch(NumberFormatException e){
			// use default
		}
		
		// read third line, number of threads
		if(argsReader != null) 
			try {
				line = argsReader.readLine();
			} 
			catch (IOException e1) {
				// ignore, line will be null
			}
		
		/*
		 * Initialize numberOfThreads, check continue.txt, then input args , then args.text, 
		 * finally use the default
		 */		
		try {
			if (args.length > 2) {
				wf.numberOfThreads = Integer.parseInt(args[2]);
			}
			else {
				if (line != null && line.length() > 0 )
					wf.numberOfThreads = Integer.parseInt(line);
				
				// otherwise use default
			}
		}
		catch(NumberFormatException e){
			// keep default
		}

		// read forth line, radius
		if(argsReader != null) 
			try {
				line = argsReader.readLine();
			} 
			catch (IOException e1) {
				// ignore, line will be null
			}
		
		/*
		 * Initialize radius, check input args, then args.text, 
		 * finally use the default
		 */		
		try {
			if (args.length > 3) {
				wf.radius = Integer.parseInt(args[3]);
			}
			else {
				if (line != null && line.length() > 0 )
					wf.radius = Integer.parseInt(line);
				
				// otherwise keep default
				
			}
		}
		catch(NumberFormatException e){
			// keep default
		}

		/*
		 * Ignore quality setting, this program doesn't use it
		 */
		
		//if(in != null) line = in.readLine();
		//WitchFinder.quality = args.length > 4? 88 + Integer.parseInt(args[4]): line != null && line != ""? 88 + Integer.parseInt(line): 88;
		
		/*
		 * Close the argReader
		 */
		try {
			if(argsReader != null)
				argsReader.close();
		} 
		catch (IOException e) {
			// ignore
		}
		
		/*
		 * Initialize endSeed, use values from continue.txt first, otherwise calculate it
		 */		
		if(wf.contSeeds != null){
			wf.endSeed = Long.parseLong(wf.contSeeds[wf.contSeeds.length-1]);
			wf.startMessage = "continuing to analyze the remaining ";
		}
		else{
			wf.endSeed = wf.startSeed + wf.seedsToCheck - 1;
			wf.startMessage =  "checking ";
		}
		return wf;
	}
}