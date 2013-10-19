package witchfinder;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.Date;
import java.util.Random;

public class WitchFinderV3 {
	protected int hutcount;

	protected long startSeed, endSeed, seedsToCheck;
	protected int numberOfThreads,radius;	
	protected String[] contSeeds;
	
	protected String startMessage;
	private PrintStream out = System.out;

	private WitchFinderV3() {
		seedsToCheck = 1000000000; //default is 1 billion
		numberOfThreads = 4; // default is 4 threads
		radius = 128; // default is 128
		
		hutcount = 0;
	}
	
	public void process(File jarFile) throws Exception {
		// calculate startTime
		long startTime = System.currentTimeMillis();
		
		System.out.println(startMessage + (seedsToCheck / 1000000) +
				" million seeds (" + startSeed + "-" + endSeed + ") with quality setting 0 " +
				" with a radius of " + radius + " chunks on " + numberOfThreads + " threads:" + System.lineSeparator());
		
		
		//calculate number of seeds per thread
		long seedsPerThread = seedsToCheck / numberOfThreads;

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
				((double) calcTime * 1000000 / seedsToCheck), ((double) calcTime * 1000000000 / (seedsToCheck * radius * radius * 4)));
	}
	
	public static void main(String[] args) throws Exception {
		Options opts = new Options();
		opts.addOption(OptionBuilder.withLongOpt("jar")
				.hasArgs(1)
				.withValueSeparator()
				.withDescription("Use the following minecraft.jar")
				.create('j'));
		opts.addOption(OptionBuilder.withLongOpt("help").create('h'));
		opts.addOption(OptionBuilder.withLongOpt("output")
				.hasArgs(1)
				.withDescription("Output file the seeds to")
				.create('o'));
		opts.addOption(OptionBuilder.withLongOpt("continue")
				.hasArgs(1)
				.withDescription("Continue file")
				.create('c'));
		opts.addOption(OptionBuilder.withLongOpt("startseed")
				.hasArgs(1)
				.withDescription("Seed to start from, this will be multiplied by 1.000.000")
				.create('s'));
		opts.addOption(OptionBuilder.withLongOpt("threads")
				.hasArgs(1)
				.withDescription("Run with %d amount of threads, defaults to 4")
				.create('T'));
		opts.addOption(OptionBuilder.withLongOpt("seedstocheck")
				.hasArgs(1)
				.withDescription("Amount of seeds to check, this will be multiplied by 1.000.000, defaults to 1.000.000.000")
				.create('m'));
		opts.addOption(OptionBuilder.withLongOpt("radius")
				.hasArgs(1)
				.withDescription("The radius")
				.create('r'));
		CommandLineParser parser = new PosixParser();
		CommandLine line = parser.parse(opts, args);
		if (line.hasOption('h') || args.length == 0) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("witchfinder", opts, true);
			System.exit(0);
		}
		if (!line.hasOption('j')) {
			System.err.println("We're missing the jar file?");
			System.exit(1);
		}
		WitchFinderV3 wf = new WitchFinderV3();
		if (line.hasOption('s')) {
			wf.startSeed = Long.parseLong(line.getOptionValue('s')) * 1000000;
			if (line.hasOption('m'))
				wf.seedsToCheck = Long.parseLong(line.getOptionValue('m')) * 1000000;
		} else if (line.hasOption('c')) {
			BufferedReader contReader = new BufferedReader(new FileReader(new File(line.getOptionValue('c'))));
			wf.contSeeds = contReader.readLine().split(" ");
			contReader.close();
			int i = 0;
			wf.seedsToCheck = 0;
			while (i < wf.contSeeds.length)  {
				wf.seedsToCheck += -Long.parseLong(wf.contSeeds[i++]) + Long.parseLong(wf.contSeeds[i++]);
			}
		} else {
			wf.startSeed = new Random().nextLong();
		}
		if (line.hasOption('T'))
			wf.numberOfThreads = Integer.parseInt(line.getOptionValue('T'));
		if (line.hasOption('r'))
			wf.radius = Integer.parseInt(line.getOptionValue('r'));
		if (line.hasOption('o')) {
			wf.out = new PrintStream(new FileOutputStream(line.getOptionValue('o'), true),true);
		}
		if(wf.contSeeds != null) {
			wf.endSeed = Long.parseLong(wf.contSeeds[wf.contSeeds.length-1]);
			wf.startMessage = "continuing to analyze the remaining ";
		} else {
			wf.endSeed = wf.startSeed + wf.seedsToCheck - 1;
			wf.startMessage =  "checking ";
		}
		wf.process(new File(line.getOptionValue('j')));
	}
	
	public synchronized void outputCandidate(Candidate candidate) {
 		hutcount++;
 		out.format("%tT: %s", new Date(), candidate);
 		if(candidate.hutCount == 4 && candidate.maxDistanceToCenter < 115) 
 			out.println("This is a really good quad witch hut! Please send the candidate above to BBInME, JL2579 or TheCodeRaider!"); //;D
 		if(candidate.hutCount == 3 && candidate.totalWitchSpawningArea > 504 ) 
 			out.println("This is a really good triple witch hut with a temple part way in a swamp! Please send the candidate above to BBInME, JL2579 or TheCodeRaider!"); //;D
 		out.println();
 	}
}