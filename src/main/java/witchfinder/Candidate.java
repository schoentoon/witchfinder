package witchfinder;

import minecraft.Minecraft;
import minecraft.MinecraftObject;
import net.minecraft.src.BiomeGenBase;
import net.minecraft.src.IntCache;

public class Candidate {
	public final static int TOPRIGHT = 0;
	public final static int BOTTOMRIGHT = 1;
	public final static int BOTTOMLEFT = 2;
	public final static int TOPLEFT = 3;
	
	public final static int NONE = 0;
	public final static int HUT = 1;
	public final static int DESERTTEMPLE = 2;
	public final static int JUNGLETEMPLE = 3;

	public final static int NORTHSOUTHORIENTATION = 0;
	public final static int EASTWESTORIENTATION = 1;
	
	public final static String[] TYPESTRING = new String[] { "NONE", "Witch Hut", "Desert Temple", "Jungle Temple" };
	public final static String[] ORIENTATIONSTRING = new String[] { "N-S Orientation", "E-W Orientation" };
	
	protected long seed;
	protected Minecraft minecraft;
	
	protected int[] xpos;
	protected int[] zpos;
	protected int[] xrand;
	protected int[] zrand;
	
	protected int[] structureOrientations;
	
	protected int xcenter;
	protected int zcenter;
	
	protected double[] distanceToCenter;
	protected double maxDistanceToCenter;
	
	protected int[][] biomeInts;
	protected int[] biomeIds;
	protected boolean[] validBiomes;
	
	protected int[] type;
	protected int hutCount;
	
	protected int[] witchSpawningArea;
	protected int totalWitchSpawningArea;

	public Candidate(Minecraft minecraft) {
		this.minecraft = minecraft;
		this.hutCount = 0;
		this.xpos = new int[4];
		this.zpos = new int[4];
		this.biomeInts = new int[4][];
		this.biomeIds = new int[4];
		this.validBiomes = new boolean[4];
		this.type = new int[4];
		this.distanceToCenter = new double[4];
		this.witchSpawningArea = new int[4];
	}
	
	public void init(long seed, int x0, int x1, int x2, int x3, int z0, int z1, int z2, int z3, int[] xrand, int[] zrand, int[] structureOrientations) {
		this.seed = seed;
		this.xrand = xrand;
		this.zrand = zrand;
		this.structureOrientations = structureOrientations;
		this.hutCount = 0;
		
		xpos[TOPRIGHT] = 512 * x0 + 16 * xrand[TOPRIGHT];
		zpos[TOPRIGHT] = 512 * z0 + 16 * zrand[TOPRIGHT];
		xpos[BOTTOMRIGHT] = 512 * x1 + 16 * xrand[BOTTOMRIGHT];
		zpos[BOTTOMRIGHT] = 512 * z1 + 16 * zrand[BOTTOMRIGHT];
		xpos[BOTTOMLEFT] = 512 * x2 + 16 * xrand[BOTTOMLEFT];
		zpos[BOTTOMLEFT] = 512 * z2 + 16 * zrand[BOTTOMLEFT];
		xpos[TOPLEFT] = 512 * x3 + 16 * xrand[TOPLEFT];
		zpos[TOPLEFT] = 512 * z3 + 16 * zrand[TOPLEFT];
		this.validBiomes[0] = false;
		this.validBiomes[1] = false;
		this.validBiomes[2] = false;
		this.validBiomes[3] = false;
		this.type[0] = 0;
		this.type[1] = 0;
		this.type[2] = 0;
		this.type[3] = 0;
		this.distanceToCenter[0] = 0;
		this.distanceToCenter[1] = 0;
		this.distanceToCenter[2] = 0;
		this.distanceToCenter[3] = 0;
		this.witchSpawningArea[0] = 0;
		this.witchSpawningArea[1] = 0;
		this.witchSpawningArea[2] = 0;
		this.witchSpawningArea[3] = 0;
	}

	private static final String GET_INTS = "getInts";
	private static final String BIOME_INIT = "initializeAllBiomeGenerators";
	public void initializeBiomeValues() {
		MinecraftObject biomeIndexLayer = new MinecraftObject(WitchFinderThread.genLayerClass, ((Object[]) WitchFinderThread.genLayerClass.callFunction(BIOME_INIT
															,seed
															,WitchFinderThread.defaultGeneration.get()))[1]); //Don't ask, really don't.

		IntCache.resetIntCache();

		// get ints, use a 24 x24 array so that it is large enough to hold a 21x21 desert temple
		biomeInts[TOPRIGHT] = (int[]) biomeIndexLayer.callFunction(GET_INTS, xpos[TOPRIGHT],zpos[TOPRIGHT],24,24);
		biomeInts[BOTTOMRIGHT] = (int[]) biomeIndexLayer.callFunction(GET_INTS, xpos[BOTTOMRIGHT],zpos[BOTTOMRIGHT],24,24);
		biomeInts[BOTTOMLEFT] = (int[]) biomeIndexLayer.callFunction(GET_INTS, xpos[BOTTOMLEFT],zpos[BOTTOMLEFT],24,24);
		biomeInts[TOPLEFT] = (int[]) biomeIndexLayer.callFunction(GET_INTS, xpos[TOPLEFT],zpos[TOPLEFT],24,24);
		
		/*
		 * There is some wierdness in the minecraft code here, as it seems like it looks
		 * at the biomeInts code at xpos = 6, zpos = 8 or 8,6 depending on orientation
		 * to determine the type of structure.
		 * 
		 * This makes sense for witch huts as they are 7x9 but temples are bigger, and
		 * yet it appears they are determined by the same location.
		 *
		 * use 
		 * 		198 here as we have gotten a 24x24 array, so 8 * 24 + 6 = 198
		 * or 
		 * 		152 here as we have gotten a 24x24 array, so 6 * 24 + 8 = 152
		 */
		biomeIds[TOPRIGHT] = structureOrientations[TOPRIGHT] == NORTHSOUTHORIENTATION ?
			BiomeGenBase.biomeList[biomeInts[TOPRIGHT][198]] : BiomeGenBase.biomeList[biomeInts[TOPRIGHT][152]];
		biomeIds[BOTTOMRIGHT] = structureOrientations[BOTTOMRIGHT] == NORTHSOUTHORIENTATION ?
			BiomeGenBase.biomeList[biomeInts[BOTTOMRIGHT][198]] : BiomeGenBase.biomeList[biomeInts[BOTTOMRIGHT][152]];
		biomeIds[BOTTOMLEFT] = structureOrientations[BOTTOMLEFT] == NORTHSOUTHORIENTATION ?
			BiomeGenBase.biomeList[biomeInts[BOTTOMLEFT][198]] : BiomeGenBase.biomeList[biomeInts[BOTTOMLEFT][152]];
		biomeIds[TOPLEFT] = structureOrientations[TOPLEFT] == NORTHSOUTHORIENTATION ?
				BiomeGenBase.biomeList[biomeInts[TOPLEFT][198]] : BiomeGenBase.biomeList[biomeInts[TOPLEFT][152]];
		
		switch(biomeIds[TOPRIGHT]) {
		case BiomeGenBase.desertID:
		case BiomeGenBase.desertHillsID:
		case BiomeGenBase.desertHillsM:
		case BiomeGenBase.desertM:
			validBiomes[TOPRIGHT] = true;
			type[TOPRIGHT] = DESERTTEMPLE;
			break;
		case BiomeGenBase.jungleID:
		case BiomeGenBase.jungleHillsID:
		case BiomeGenBase.jungleEdge:
		case BiomeGenBase.jungleEdgeM:
		case BiomeGenBase.jungleM:
			validBiomes[TOPRIGHT] = true;
			type[TOPRIGHT] = JUNGLETEMPLE;
			break;
		case BiomeGenBase.swamplandID:
		case BiomeGenBase.swamplandM:
			validBiomes[TOPRIGHT] = true;
			type[TOPRIGHT] = HUT;
			hutCount++;
			break;
		}
		
		switch(biomeIds[BOTTOMRIGHT]) {
		case BiomeGenBase.desertID:
		case BiomeGenBase.desertHillsID:
		case BiomeGenBase.desertHillsM:
		case BiomeGenBase.desertM:
			validBiomes[BOTTOMRIGHT] = true;
			type[BOTTOMRIGHT] = DESERTTEMPLE;
			break;
		case BiomeGenBase.jungleID:
		case BiomeGenBase.jungleHillsID:
		case BiomeGenBase.jungleEdge:
		case BiomeGenBase.jungleEdgeM:
		case BiomeGenBase.jungleM:
			validBiomes[BOTTOMRIGHT] = true;
			type[BOTTOMRIGHT] = JUNGLETEMPLE;
			break;
		case BiomeGenBase.swamplandID:
		case BiomeGenBase.swamplandM:
			validBiomes[BOTTOMRIGHT] = true;
			type[BOTTOMRIGHT] = HUT;
			hutCount++;
			break;
		}

		switch(biomeIds[BOTTOMLEFT]) {
		case BiomeGenBase.desertID:
		case BiomeGenBase.desertHillsID:
		case BiomeGenBase.desertHillsM:
		case BiomeGenBase.desertM:
			validBiomes[BOTTOMLEFT] = true;
			type[BOTTOMLEFT] = DESERTTEMPLE;
			break;
		case BiomeGenBase.jungleID:
		case BiomeGenBase.jungleHillsID:
		case BiomeGenBase.jungleEdge:
		case BiomeGenBase.jungleEdgeM:
		case BiomeGenBase.jungleM:
			validBiomes[BOTTOMLEFT] = true;
			type[BOTTOMLEFT] = JUNGLETEMPLE;
			break;
		case BiomeGenBase.swamplandID:
		case BiomeGenBase.swamplandM:
			validBiomes[BOTTOMLEFT] = true;
			type[BOTTOMLEFT] = HUT;
			hutCount++;
			break;
		}

		switch(biomeIds[TOPLEFT]) {
		case BiomeGenBase.desertID:
		case BiomeGenBase.desertHillsID:
			validBiomes[TOPLEFT] = true;
			type[TOPLEFT] = DESERTTEMPLE;
			break;
		case BiomeGenBase.jungleID:
		case BiomeGenBase.jungleHillsID:
			validBiomes[TOPLEFT] = true;
			type[TOPLEFT] = JUNGLETEMPLE;
			break;
		case BiomeGenBase.swamplandID:
		case BiomeGenBase.swamplandM:
			validBiomes[TOPLEFT] = true;
			type[TOPLEFT] = HUT;
			hutCount++;
			break;
		}

	}
	
    /*
     * check whether the biome for all 4 structures is a valid biome for a structure
     */
	public boolean isQuadInAValidBiome(){		
		return (validBiomes[TOPRIGHT] && validBiomes[BOTTOMRIGHT] && validBiomes[BOTTOMLEFT]&& validBiomes[TOPLEFT]);
	}

	public double calculateMaxDistance() {
		int minx = xpos[TOPRIGHT];
		if ( xpos[BOTTOMRIGHT] < minx )
			minx = xpos[BOTTOMRIGHT];
		if ( xpos[BOTTOMLEFT] < minx )
			minx = xpos[BOTTOMLEFT];
		if ( xpos[TOPLEFT] < minx )
			minx = xpos[TOPLEFT];
		
		int maxx = xpos[TOPRIGHT];
		if ( xpos[BOTTOMRIGHT] > maxx )
			maxx = xpos[BOTTOMRIGHT];
		if ( xpos[BOTTOMLEFT] > maxx )
			maxx = xpos[BOTTOMLEFT];
		if ( xpos[TOPLEFT] > maxx )
			maxx = xpos[TOPLEFT];
		
		int minz = zpos[TOPRIGHT];
		if ( zpos[BOTTOMRIGHT] < minz )
			minz = zpos[BOTTOMRIGHT];
		if ( zpos[BOTTOMLEFT] < minz )
			minz = zpos[BOTTOMLEFT];
		if ( zpos[TOPLEFT] < minz )
			minz = zpos[TOPLEFT];
		
		int maxz = zpos[TOPRIGHT];
		if ( zpos[BOTTOMRIGHT] > maxz )
			maxz = zpos[BOTTOMRIGHT];
		if ( zpos[BOTTOMLEFT] > maxz )
			maxz = zpos[BOTTOMLEFT];
		if ( zpos[TOPLEFT] > maxz )
			maxz = zpos[TOPLEFT];
		
		xcenter = minx + (maxx - minx) /2;
		zcenter = minz + (maxz - minz) /2;
		
		int dx=xcenter - xpos[TOPRIGHT];
		int dz=zpos[TOPRIGHT] - zcenter;
		distanceToCenter[TOPRIGHT] = Math.sqrt(dx*dx + dz*dz);
		dx=xcenter - xpos[BOTTOMRIGHT];
		dz=zcenter - zpos[BOTTOMRIGHT];
		distanceToCenter[BOTTOMRIGHT] = Math.sqrt(dx*dx + dz*dz);
		dx=xpos[BOTTOMLEFT] - xcenter;
		dz=zcenter - zpos[BOTTOMLEFT];
		distanceToCenter[BOTTOMLEFT] = Math.sqrt(dx*dx + dz*dz);
		dx=xpos[TOPLEFT] - xcenter;
		dz=zpos[TOPLEFT] - zcenter;
		distanceToCenter[TOPLEFT] = Math.sqrt(dx*dx + dz*dz);
		
		maxDistanceToCenter = distanceToCenter[TOPRIGHT];
		if ( distanceToCenter[BOTTOMRIGHT] > maxDistanceToCenter )
			maxDistanceToCenter = distanceToCenter[BOTTOMRIGHT];
		if ( distanceToCenter[BOTTOMLEFT] > maxDistanceToCenter )
			maxDistanceToCenter = distanceToCenter[BOTTOMLEFT];
		if ( distanceToCenter[TOPLEFT] > maxDistanceToCenter )
			maxDistanceToCenter = distanceToCenter[TOPLEFT];
		
		return maxDistanceToCenter;
	}
	
	public int calculateWitchSpawnableArea() {
		witchSpawningArea[TOPRIGHT] = calculateWitchSpawnableArea2(xpos[TOPRIGHT], zpos[TOPRIGHT], 
			type[TOPRIGHT], structureOrientations[TOPRIGHT], biomeInts[TOPRIGHT]);
		witchSpawningArea[BOTTOMRIGHT] = calculateWitchSpawnableArea2(xpos[BOTTOMRIGHT], zpos[BOTTOMRIGHT], 
				type[BOTTOMRIGHT], structureOrientations[BOTTOMRIGHT], biomeInts[BOTTOMRIGHT]);
		witchSpawningArea[BOTTOMLEFT] = calculateWitchSpawnableArea2(xpos[BOTTOMLEFT], zpos[BOTTOMLEFT], 
				type[BOTTOMLEFT], structureOrientations[BOTTOMLEFT], biomeInts[BOTTOMLEFT]);
		witchSpawningArea[TOPLEFT] = calculateWitchSpawnableArea2(xpos[TOPLEFT], zpos[TOPLEFT], 
				type[TOPLEFT], structureOrientations[TOPLEFT], biomeInts[TOPLEFT]);
		
		totalWitchSpawningArea = witchSpawningArea[TOPRIGHT] + witchSpawningArea[BOTTOMRIGHT] + 
			witchSpawningArea[BOTTOMLEFT] + witchSpawningArea[TOPLEFT];
		
		return totalWitchSpawningArea; 
	}
	
	private int calculateWitchSpawnableArea2(int x, int z, int type, int structureOrientation, int[] biomeInts) {
		int count = 0;
		switch ( type ){
		case NONE:
			break;
		case HUT:
			if ( structureOrientation == NORTHSOUTHORIENTATION ) {
				for ( int xx = 0; xx < 7; xx++)
					for ( int zz = 0; zz < 9; zz++) {
						if (isSwamp(xx, zz, biomeInts))
							count+=2;
					}
			}
			else {
				// otherwise EW orientation
				for ( int xx = 0; xx < 9; xx++)
					for ( int zz = 0; zz < 7; zz++) {
						if (isSwamp(xx, zz, biomeInts))
							count+=2;
					}
				
			}
			break;
		case DESERTTEMPLE:
			for ( int xx = 0; xx < 21; xx++)
				for ( int zz = 0; zz < 21; zz++) {
					if (isSwamp(xx, zz, biomeInts))
						count+=5;
				}
			break;
		case JUNGLETEMPLE:			
			if ( structureOrientation == NORTHSOUTHORIENTATION ) {
				for ( int xx = 0; xx < 12; xx++)
					for ( int zz = 0; zz < 15; zz++) {
						if (isSwamp(xx, zz, biomeInts))
							count+=4;
					}
			}
			else {
				// otherwise EW orientation
				for ( int xx = 0; xx < 12; xx++)
					for ( int zz = 0; zz < 15; zz++) {
						if (isSwamp(xx, zz, biomeInts))
							count+=4;
					}
				
			}
		}
		
		return count;
	}

	private boolean isSwamp(int x, int z, int[] biomeInts) {
		return BiomeGenBase.biomeList[biomeInts[z * 24 + x]] == BiomeGenBase.swamplandID;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("{ center (x, z) : (").append(xcenter).append(", ").append(zcenter);
		sb.append(",  maximum distance from Center : ").append(maxDistanceToCenter);
		sb.append(",  seed : ").append(seed);
		sb.append(", # of huts : ").append(hutCount);
		sb.append(", estimated total spawning area : ").append(totalWitchSpawningArea);
		sb.append(", \n  Top Right : { type : ").append(TYPESTRING[type[TOPRIGHT]]);
		sb.append(", coord (x,z) : (").append(xpos[TOPRIGHT]).append(",").append(zpos[TOPRIGHT]);
		sb.append(", spawning area : ").append(witchSpawningArea[TOPRIGHT]);
		sb.append(", orientation : ").append(ORIENTATIONSTRING[structureOrientations[TOPRIGHT]]);
		sb.append(") }, \n  Bottom Right : { type : ").append(TYPESTRING[type[BOTTOMRIGHT]]);
		sb.append(", coord (x,z) : (").append(xpos[BOTTOMRIGHT]).append(",").append(zpos[BOTTOMRIGHT]);
		sb.append(", spawning area : ").append(witchSpawningArea[BOTTOMRIGHT]);
		sb.append(", orientation : ").append(ORIENTATIONSTRING[structureOrientations[BOTTOMRIGHT]]);
		sb.append(") }, \n  Bottom Left : { type : ").append(TYPESTRING[type[BOTTOMLEFT]]);
		sb.append(", coord (x,z) : (").append(xpos[BOTTOMLEFT]).append(",").append(zpos[BOTTOMLEFT]);
		sb.append(", spawning area : ").append(witchSpawningArea[BOTTOMLEFT]);
		sb.append(", orientation : ").append(ORIENTATIONSTRING[structureOrientations[BOTTOMLEFT]]);
		sb.append(") }, \n  Top Left : { type : ").append(TYPESTRING[type[TOPLEFT]]);
		sb.append(", coord (x,z) : (").append(xpos[TOPLEFT]).append(",").append(zpos[TOPLEFT]);
		sb.append(", spawning area : ").append(witchSpawningArea[TOPLEFT]);
		sb.append(", orientation : ").append(ORIENTATIONSTRING[structureOrientations[TOPLEFT]]).append(") }}\n");
		
		return sb.toString();
	}

	public void free() {
		xpos = null;
		zpos = null;
		xrand = null;
		zrand = null;
		structureOrientations = null;
		distanceToCenter = null;
		biomeInts = null;
		biomeIds = null;
		validBiomes = null;
		type = null;
		witchSpawningArea = null;
	}
}
