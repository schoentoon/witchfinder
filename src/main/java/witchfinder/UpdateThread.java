package witchfinder;

import java.util.Date;


public class UpdateThread extends Thread {
	
	private boolean finished = false;
	
	private static final int INITIALDELAY = 60000;
	
	private static final int UPDATEDELAY = 300000;
	
	private double startTime;
	private long seedsToCheck;
	
	private WitchFinderThread[] witchFinderThreads;
	
	public UpdateThread(WitchFinderThread[] witchFinderThreads, double startTime, long seedsToCheck) {
		this.startTime = startTime;
		this.seedsToCheck = seedsToCheck;
		this.witchFinderThreads = witchFinderThreads;
	}
	
	public void run(){
		
		try{
			Thread.sleep(INITIALDELAY);
		}
		catch(InterruptedException t){ /*ignore*/ }
		
		double passedTime, percentRemaining;
		long seedsRemaining;
		while(!finished){
			
			seedsRemaining = 0;
			for(WitchFinderThread wft: witchFinderThreads) 
				seedsRemaining += wft.getSeedsToCheck();
			
			percentRemaining = (double)seedsRemaining / seedsToCheck;
			passedTime = (double)(System.currentTimeMillis() - startTime) / 60000d;
			System.out.format("%tT: Analyzed %d/%d seeds (%.2f%%), estimated time left: %.2f minutes%n", 
				new Date(), seedsRemaining, seedsToCheck, percentRemaining * 100,  passedTime / percentRemaining - passedTime);
			
			System.out.print("Status:");
			for(WitchFinderThread wf: witchFinderThreads) 
				System.out.print(" " + wf.currentSeed + " " + wf.endSeed);
			System.out.println(System.lineSeparator());
			
			try{
				Thread.sleep(UPDATEDELAY);
			}
			catch(InterruptedException t){ /*ignore*/ }
		}
	}


}
