package witchfinder;

import minecraft.Minecraft;
import minecraft.MinecraftClass;
import minecraft.MinecraftObject;

import java.io.File;
import java.util.Random;

public class WitchFinderThread extends Thread {
	private final WitchFinderV3 wf;
	protected final long startSeed;
	protected final long endSeed;
	private final int radius;
	private final Random rnd = new Random();
	private static Minecraft minecraft;

	protected long currentSeed;

	public static MinecraftClass genLayerClass = null;
	public static MinecraftObject defaultGeneration = null;
	
	public WitchFinderThread(WitchFinderV3 wf, long seed, int radius, File jarFile) throws Exception {
		this(wf, seed, seed, radius, jarFile);
	}

	public WitchFinderThread(WitchFinderV3 wf, long startSeed, long endSeed, int radius, File jarFile) throws Exception {
		this.wf = wf;
		this.startSeed = startSeed;
		this.endSeed = endSeed;
		this.radius = radius / 32;
		this.minecraft = new Minecraft(jarFile);
		if (genLayerClass == null)
			genLayerClass = minecraft.getClassByName("GenLayer");
		if (defaultGeneration == null)
			defaultGeneration = (MinecraftObject) minecraft.getClassByName("WorldType").getValue("default");
	}
	
	public long getSeedsToCheck() {
		return currentSeed - startSeed;
	}
	
	private boolean checkForStructureBR(int x, int z, long seed, int index, 
		int[] xrand, int[] zrand, int[] structureOrientation) {
		
		rnd.setSeed((long) x * 341873128712L + (long)z * 132897987541L + seed + 14357617);
		
		xrand[index] = rnd.nextInt(24);
		zrand[index] = rnd.nextInt(24);
		
		int or = rnd.nextInt(4);
		structureOrientation[index] = 
			( or == 0 || or == 2 ?  Candidate.NORTHSOUTHORIENTATION : Candidate.EASTWESTORIENTATION);
		
		return xrand[index] >= 21 && zrand[index] >= 21;
	}

	private boolean checkForStructureBL(int x, int z, long seed, int index, 
		int[] xrand, int[] zrand, int[] structureOrientation) {
		
		rnd.setSeed((long) x * 341873128712L + (long)z * 132897987541L + seed + 14357617);
		
		xrand[index] = rnd.nextInt(24);
		zrand[index] = rnd.nextInt(24);
		
		int or = rnd.nextInt(4);
		structureOrientation[index] = 
			( or == 0 || or == 2 ? Candidate.NORTHSOUTHORIENTATION : Candidate.EASTWESTORIENTATION);
		
		return xrand[index] <=2 && zrand[index] >= 21;
	}
	
	private boolean checkForStructureTR(int x, int z, long seed, int index, 
		int[] xrand, int[] zrand, int[] structureOrientation) {
		
		rnd.setSeed((long) x * 341873128712L + (long)z * 132897987541L + seed + 14357617);
		
		xrand[index] = rnd.nextInt(24);
		zrand[index] = rnd.nextInt(24);
		
		int or = rnd.nextInt(4);
		structureOrientation[index] = 
			( or == 0 || or == 2 ? Candidate.NORTHSOUTHORIENTATION : Candidate.EASTWESTORIENTATION);
		
		return xrand[index] >= 21 && zrand[index] <= 2;
	}

	private boolean checkForStructureTL(int x, int z, long seed, int index, 
		int[] xrand, int[] zrand, int[] structureOrientation) {
		
		rnd.setSeed((long) x * 341873128712L + (long)z * 132897987541L + seed + 14357617);
		
		xrand[index] = rnd.nextInt(24);
		zrand[index] = rnd.nextInt(24);
		
		int or = rnd.nextInt(4);
		structureOrientation[index] = 
			( or == 0 || or == 2 ? Candidate.NORTHSOUTHORIENTATION : Candidate.EASTWESTORIENTATION);
		
		return xrand[index] <=2 && zrand[index] <= 2;
	}
	
	public void run() {
		minecraft.use(this);
		long xfactor;
		int[] xrand = new int[4];
		int[] zrand = new int[4];
		int[] structureOrientation = new int[4];
		int xr, zr;
		Candidate candidate;
		for(currentSeed = startSeed; currentSeed <= endSeed; currentSeed++){
			for(int x=-radius; x<radius - 1; x+=2) {	
				
				xfactor = (long)x * 341873128712L;
				
				for(int z=-radius; z<radius - 1; z+=2) {
					//sets the seed for the random object used to calculate the witch hut's position in the 32x32 chunk areas
					rnd.setSeed(xfactor + (long)z * 132897987541L + currentSeed + 14357617);
					xr = rnd.nextInt(24);
					
					if ( xr <= 2) {
						zr = rnd.nextInt(24);
						
						if( zr <= 2 ) {
							// candidate witch hut, is in the top left of the 32x32 chunk array
							// this means that to be in a quad it would be in bottom right of the quad
							
							// check the 32x32 chunk area neighbors to the left and above
							if ( checkForStructureTR(x-1, z, currentSeed, Candidate.BOTTOMLEFT, xrand, zrand, structureOrientation) && 
								checkForStructureBR(x-1, z-1, currentSeed, Candidate.TOPLEFT, xrand, zrand, structureOrientation) &&
								checkForStructureBL(x, z-1, currentSeed, Candidate.TOPRIGHT, xrand, zrand, structureOrientation)) {
								
								xrand[Candidate.BOTTOMRIGHT] = xr; 
								zrand[Candidate.BOTTOMRIGHT] = zr;
								
								candidate = new Candidate(currentSeed, new int[] { x, x, x-1, x-1 }, new int[] { z-1, z, z, z-1 }, 
									xrand, zrand, structureOrientation, minecraft);

								candidate.initializeBiomeValues();
								if ( candidate.isQuadInAValidBiome() && candidate.calculateMaxDistance() < 128 &&
									candidate.hutCount >= 3) {
									if ( candidate.calculateWitchSpawnableArea() > 441)
										wf.outputCandidate(candidate);
								}
							}
							
						}
						else if( zr >= 21 ){
							// candidate witch hut, is in the bottom left of the 32x32 chunk array
							// this means that to be in a quad it would be in top right of the quad
							
							// check the 32x32 chunk area neighbors to the left and below
							if ( checkForStructureTL(x, z+1, currentSeed, Candidate.BOTTOMRIGHT, xrand, zrand, structureOrientation) && 
								checkForStructureTR(x-1, z+1, currentSeed, Candidate.BOTTOMLEFT, xrand, zrand, structureOrientation) &&
								checkForStructureBR(x-1, z, currentSeed, Candidate.TOPLEFT, xrand, zrand, structureOrientation)) {
								
								xrand[Candidate.TOPRIGHT] = xr; 
								zrand[Candidate.TOPRIGHT] = zr;
								
								candidate = new Candidate(currentSeed, new int[] { x, x, x-1, x-1 }, new int[] { z, z+1, z+1, z }, 
										xrand, zrand, structureOrientation, minecraft);
								
								candidate.initializeBiomeValues();
								if ( candidate.isQuadInAValidBiome() && candidate.calculateMaxDistance() < 128 &&
									candidate.hutCount >= 3) {
									if ( candidate.calculateWitchSpawnableArea() > 441)
										wf.outputCandidate(candidate);
								}
							}
						}
	
					}
					else if ( xr >= 21) {
						zr = rnd.nextInt(24);
						
						if( zr <= 3 ) {
							// candidate witch hut, is in the top right of the 32x32 chunk array
							// this means that to be in a quad it would be in bottom left of the quad
							
							// check the 32x32 chunk area neighbors to the right and above
							if ( checkForStructureBR(x, z-1, currentSeed, Candidate.TOPLEFT, xrand, zrand, structureOrientation) && 
								checkForStructureBL(x+1, z-1, currentSeed, Candidate.TOPRIGHT, xrand, zrand, structureOrientation) && 
								checkForStructureTR(x+1, z, currentSeed, Candidate.BOTTOMRIGHT, xrand, zrand, structureOrientation)) {
								
								xrand[Candidate.BOTTOMLEFT] = xr; 
								zrand[Candidate.BOTTOMLEFT] = zr;
								
								candidate = new Candidate(currentSeed, new int[] { x+1, x+1, x, x }, new int[] { z-1, z, z, z-1 }, 
										xrand, zrand, structureOrientation, minecraft);
	
								candidate.initializeBiomeValues();
								if ( candidate.isQuadInAValidBiome() && candidate.calculateMaxDistance() < 128 &&
									candidate.hutCount >= 3) {
									if ( candidate.calculateWitchSpawnableArea() > 441)
										wf.outputCandidate(candidate);
								}
							}
						}
						else if( zr >= 21 ){						
							// candidate witch hut, is in the bottom right of the 32x32 chunk array
							// this means that to be in a quad it would be in top left of the quad
							
							// check the 32x32 chunk area neighbors to the right and below
							if ( checkForStructureBL(x+1, z, currentSeed, Candidate.TOPRIGHT, xrand, zrand, structureOrientation) && 
								checkForStructureTL(x+1, z+1, currentSeed, Candidate.BOTTOMRIGHT, xrand, zrand, structureOrientation) && 
								checkForStructureTR(x, z+1, currentSeed, Candidate.BOTTOMLEFT, xrand, zrand, structureOrientation)) {
								
								xrand[Candidate.TOPLEFT] = xr; 
								zrand[Candidate.TOPLEFT] = zr;
								
								candidate = new Candidate(currentSeed, new int[] { x+1, x+1, x, x }, new int[] { z, z+1, z+1, z }, 
										xrand, zrand, structureOrientation, minecraft);
	
								candidate.initializeBiomeValues();
								if ( candidate.isQuadInAValidBiome() && candidate.calculateMaxDistance() < 128 &&
									candidate.hutCount >= 3) {
									
									if ( candidate.calculateWitchSpawnableArea() > 441)
										wf.outputCandidate(candidate);
								}
							}
	
						}
					}
				}
			}
		}
	}


}
