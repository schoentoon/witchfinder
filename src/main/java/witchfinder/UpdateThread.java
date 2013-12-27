package witchfinder;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Date;


public class UpdateThread extends Thread {
	
	private boolean finished = false;
	
	private static final int INITIALDELAY = 60000;
	
	private static final int UPDATEDELAY = 60000;
	
	private final double startTime;
	private final long seedsToCheck;

	private final WitchFinderThread[] witchFinderThreads;
	private final PrintStream contFile;

	public UpdateThread(WitchFinderThread[] witchFinderThreads, double startTime, long seedsToCheck, PrintStream contFile) {
		this.startTime = startTime;
		this.seedsToCheck = seedsToCheck;
		this.witchFinderThreads = witchFinderThreads;
		this.contFile = contFile;
	}

	public void run() {
		
		try {
			Thread.sleep(INITIALDELAY);
		} catch(InterruptedException t) { /*ignore*/ }
		
		double passedTime, percentRemaining;
		long seedsRemaining;
		while (!finished) {
			
			seedsRemaining = 0;
			for (WitchFinderThread wft: witchFinderThreads)
				seedsRemaining += wft.getSeedsToCheck();
			
			percentRemaining = (double)seedsRemaining / seedsToCheck;
			passedTime = (double)(System.currentTimeMillis() - startTime) / 60000d;
			contFile.format("%tT: Analyzed %d/%d seeds (%.2f%%), estimated time left: %.2f minutes%n",
				new Date(), seedsRemaining, seedsToCheck, percentRemaining * 100,  passedTime / percentRemaining - passedTime);
			
			contFile.print("Status:");
			for(WitchFinderThread wf: witchFinderThreads) 
				contFile.print(" " + wf.currentSeed + " " + wf.endSeed);
			contFile.println(System.lineSeparator());

			try {
				String raw = "";
				for (int i = 0; i < witchFinderThreads.length; i++)
					raw += (i > 0 ? " " : "") + witchFinderThreads[i].currentSeed + " " + witchFinderThreads[i].endSeed;
				synchronized (WitchFinderV3.continueFile) {
					PrintStream stream = new PrintStream(new FileOutputStream(WitchFinderV3.continueFile, true), true);
					stream.write(raw.getBytes());
					stream.write('\n');
					stream.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			for (int i = 0; i < 10; i++)
				System.gc();
			try {
				Thread.sleep(UPDATEDELAY);
			} catch (InterruptedException t){ /*ignore*/ }
		}
	}


}
