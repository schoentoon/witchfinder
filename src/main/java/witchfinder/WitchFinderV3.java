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
	private PrintStream contFile = System.out;
	private boolean tostderr = false;

	private WitchFinderV3() {
		seedsToCheck = 1000000000; //default is 1 billion
		numberOfThreads = 4; // default is 4 threads
		radius = 128; // default is 128
		
		hutcount = 0;
	}
	
	public void process(File jarFile) throws Exception {
		// calculate startTime
		long startTime = System.currentTimeMillis();
		
		contFile.println(startMessage + (seedsToCheck / 1000000) +
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
		UpdateThread updateThread = new UpdateThread(threads, startTime, seedsToCheck, contFile);
				
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
		if (threads[0].currentSeed != threads[0].endSeed) {
			try {
				String raw = "";
				for (int i = 0; i < threads.length; i++)
					raw += (i > 0 ? " " : "") + threads[i].currentSeed + " " + threads[i].endSeed;
				System.err.println(raw);
				synchronized (continueFile) {
					PrintStream stream = new PrintStream(new FileOutputStream(continueFile, true), true);
					stream.write(raw.getBytes());
					stream.write('\n');
					stream.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.exit(1);
		} else {
			//calculate passed time
			int calcTime = (int)(System.currentTimeMillis() - startTime);
			int seconds = calcTime / 1000;
			int minutes = seconds / 60;
			int hours = minutes / 60;
			seconds -= minutes * 60;
			minutes -= hours * 60;

			contFile.println("found " + hutcount + " witch huts in total");
			contFile.format("calculation took %02d:%02d:%02d (%.3f ns per seed)%n%n", hours, minutes, seconds,
					((double) calcTime * 1000000 / seedsToCheck), ((double) calcTime * 1000000000 / (seedsToCheck * radius * radius * 4)));
		}
	}

	public static final File continueFile = new File("continue.txt");
	
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
		opts.addOption(OptionBuilder.withDescription("Always print the quad witch hut seeds to stderr")
				.create("seedstostderr"));
		opts.addOption(OptionBuilder.withLongOpt("continue")
				.hasArgs(1)
				.withDescription("Continue file, if you use --startseed as well I'll write my update here (includes continue data)")
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
				.withDescription("The radius, defaults to 128")
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
		if (line.hasOption('T'))
			wf.numberOfThreads = Integer.parseInt(line.getOptionValue('T'));
		if (line.hasOption('s')) {
			wf.startSeed = Long.parseLong(line.getOptionValue('s')) * 1000000;
			if (line.hasOption('m'))
				wf.seedsToCheck = Long.parseLong(line.getOptionValue('m')) * 1000000;
			if (line.hasOption('c'))
				wf.contFile = new PrintStream(new FileOutputStream(new File(line.getOptionValue('c'))));
		} else if (line.hasOption('c')) {
			try {
				BufferedReader contReader = new BufferedReader(new FileReader(new File(line.getOptionValue('c'))));
				String raw_line = null;
				do {
					raw_line = contReader.readLine();
					if (raw_line != null) {
						String[] split = raw_line.split(" ");
						if (split.length == (wf.numberOfThreads * 2))
							wf.contSeeds = split;
					}
				} while (raw_line != null);
				contReader.close();
				int i = 0;
				wf.seedsToCheck = 0;
				while (i < wf.contSeeds.length)
					wf.seedsToCheck += -Long.parseLong(wf.contSeeds[i++]) + Long.parseLong(wf.contSeeds[i++]);
			} catch (NullPointerException e) {
				System.err.println("Well shit..");
				System.exit(0);
			}
		} else
			wf.startSeed = new Random().nextLong();
		if (line.hasOption('r'))
			wf.radius = Integer.parseInt(line.getOptionValue('r'));
		if (line.hasOption('o'))
			wf.out = new PrintStream(new FileOutputStream(line.getOptionValue('o'), true),true);
		if(wf.contSeeds != null) {
			wf.endSeed = Long.parseLong(wf.contSeeds[wf.contSeeds.length-1]);
			wf.startMessage = "continuing to analyze the remaining ";
		} else {
			wf.endSeed = wf.startSeed + wf.seedsToCheck - 1;
			wf.startMessage =  "checking ";
		}
		wf.tostderr = line.hasOption("seedstostderr");
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
		if (tostderr) {
			System.err.format("%tT: %s", new Date(), candidate);
			System.err.println();
		}
 	}
}