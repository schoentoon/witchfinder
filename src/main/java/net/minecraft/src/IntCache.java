package net.minecraft.src;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class IntCache{
	private int intCacheSize = 256;
	private List freeSmallArrays = new ArrayList();
	private List inUseSmallArrays = new ArrayList();
	private List freeLargeArrays = new ArrayList();
	private List inUseLargeArrays = new ArrayList();
	
	private int[] getIntCache_orig(int par0){
        int[] var1;

        if (par0 <= 256){
            if (freeSmallArrays.isEmpty()){
                var1 = new int[256];
                inUseSmallArrays.add(var1);
                return var1;
            }else{
                var1 = (int[])freeSmallArrays.remove(freeSmallArrays.size() - 1);
                inUseSmallArrays.add(var1);
                return var1;
            }
        }else if (par0 > intCacheSize){
            intCacheSize = par0;
            freeLargeArrays.clear();
            inUseLargeArrays.clear();
            var1 = new int[intCacheSize];
            inUseLargeArrays.add(var1);
            return var1;
        }else if (freeLargeArrays.isEmpty()){
            var1 = new int[intCacheSize];
            inUseLargeArrays.add(var1);
            return var1;
        }else{
            var1 = (int[])freeLargeArrays.remove(freeLargeArrays.size() - 1);
            inUseLargeArrays.add(var1);
            return var1;
        }
    }

    private void resetIntCache_orig(){
        if (!freeLargeArrays.isEmpty()) freeLargeArrays.remove(freeLargeArrays.size() - 1);
        if (!freeSmallArrays.isEmpty()) freeSmallArrays.remove(freeSmallArrays.size() - 1);

        freeLargeArrays.addAll(inUseLargeArrays);
        freeSmallArrays.addAll(inUseSmallArrays);
        inUseLargeArrays.clear();
        inUseSmallArrays.clear();
    }
    
    
    private static HashMap<Long, IntCache> threadCache = new HashMap<Long, IntCache>();
    
    public static int[] getIntCache(int par0){
    	IntCache cache;
    	synchronized(threadCache){
	    	cache = threadCache.get(Thread.currentThread().getId());
	    	if(cache == null) threadCache.put(Thread.currentThread().getId(), cache = new IntCache());
    	}
    	return cache.getIntCache_orig(par0);
    }
    
    public static void resetIntCache(){
    	IntCache cache;
    	synchronized(threadCache){
			cache = threadCache.get(Thread.currentThread().getId());
			if(cache == null) threadCache.put(Thread.currentThread().getId(), cache = new IntCache());
		}
    	cache.resetIntCache_orig();
    }
}
