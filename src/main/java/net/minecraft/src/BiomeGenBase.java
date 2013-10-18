package net.minecraft.src;

public abstract class BiomeGenBase{
    public static final int[] biomeList = new int[256];
    public static final int oceanID = 0;
    public static final int plainsID = 1;
    public static final int desertID = 2;
    public static final int extremeHillsID = 3;
    public static final int forestID = 4;
    public static final int taigaID = 5;
    public static final int swamplandID = 6;
    public static final int riverID = 7;
    public static final int hellID = 8;
    public static final int skyID = 9;
    public static final int frozenOceanID = 10;
    public static final int frozenRiverID = 11;
    public static final int icePlainsID = 12;
    public static final int iceMountainsID = 13;
    public static final int mushroomIslandID = 14;
    public static final int mushroomIslandShoreID = 15;
    public static final int beachID = 16;
    public static final int desertHillsID = 17;
    public static final int forestHillsID = 18;
    public static final int taigaHillsID = 19;
    public static final int extremeHillsEdgeID = 20;
    public static final int jungleID = 21;
    public static final int jungleHillsID = 22;
    
    static{
    	for(int i=0; i<biomeList.length; i++) biomeList[i] = i;
    }
}