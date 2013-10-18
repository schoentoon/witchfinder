package minecraft;

import bytedata.ByteClass;
import bytedata.CCLongMatch;
import bytedata.CCMethodPreset;
import bytedata.CCMulti;
import bytedata.CCPropertyPreset;
import bytedata.CCRequire;
import bytedata.CCStringMatch;
import bytedata.CCWildcardByteSearch;
import bytedata.ClassChecker;

import java.io.BufferedInputStream;
import java.io.File;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Minecraft {
	private static final int MAX_CLASSES = 128;
	private Class<?> mainClass;
	private URLClassLoader classLoader;
	private URL urlToJar;
	public HashMap<String, MinecraftObject> globalMap = new HashMap<String, MinecraftObject>();
	
	private static ClassChecker[] classChecks = new ClassChecker[] {
			new CCWildcardByteSearch("IntCache", DeobfuscationData.intCache),
			new CCStringMatch("BiomeGenBase", "MushroomIsland"),
			new CCStringMatch("WorldType", "default_1_1"),
			new CCLongMatch("GenLayer", 1000L, 2001L, 2000L),
			new CCRequire(
					new CCPropertyPreset(
							"WorldType",
							"b", "default",
							"c", "flat",
							"d", "largeBiomes"
					)
			, "WorldType"),
			new CCRequire(
					new CCMethodPreset(
							"GenLayer",
							"a(long, @WorldType)", "initializeAllBiomeGenerators",
							"a(int, int, int, int)", "getInts"
					)
			, "GenLayer"),
			new CCRequire(new CCMulti(
					new CCMethodPreset(
							"IntCache",
							"a(int)", "getIntCache",
							"a()", "resetIntCache",
							"b()", "getInformation"
					),
					new CCPropertyPreset(
							"IntCache",
							"a", "intCacheSize",
							"b","freeSmallArrays",
							"c","inUseSmallArrays",
							"d","freeLargeArrays",
							"e","inUseLargeArrays"
					)
			), "IntCache")
	};
	private HashMap<String, ByteClass> byteClassMap;
	private HashMap<String, MinecraftClass> nameMap;
	private HashMap<String, MinecraftClass> classMap;
	private Vector<String> byteClassNames;
	
	public String versionId;
	
	public Minecraft(File jarFile)  throws Exception {
		byteClassNames = new Vector<String>();
		byteClassMap = new HashMap<String, ByteClass>(MAX_CLASSES);
		urlToJar = jarFile.toURI().toURL();

		Stack<ByteClass> byteClassStack = new Stack<ByteClass>();
		try {
			ZipFile jar = new ZipFile(jarFile);
			Enumeration<? extends ZipEntry> enu = jar.entries();
			
			while (enu.hasMoreElements()) {
				ZipEntry entry = enu.nextElement();
				String currentEntry = entry.getName();
				String[] nameSplit = currentEntry.split("\\.");
				if (!entry.isDirectory() && (nameSplit.length == 2) && (nameSplit[0].indexOf('/') == -1) && nameSplit[1].equals("class")) {
			        BufferedInputStream is = new BufferedInputStream(jar.getInputStream(entry));
			        if (is.available() < 8000) { // TODO: Double check that this filter won't mess anything up.
				        byte[] classData = new byte[is.available()];
				        is.read(classData);
				        is.close();
						byteClassStack.push(new ByteClass(nameSplit[0], classData));
			        }
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		int checksRemaining = classChecks.length;
		Object[] byteClasses = byteClassStack.toArray();
		boolean[] found = new boolean[byteClasses.length];
		while (checksRemaining != 0) {
			for (int q = 0; q < classChecks.length; q++) {
				for (int i = 0; i < byteClasses.length; i++) {
					if (!found[q]) {
						classChecks[q].check(this, (ByteClass)byteClasses[i]);
						if (classChecks[q].isComplete) {
							found[q] = true;
							checksRemaining--;
						}
						// TODO: What is this line, and why is it commented
						//byteClassMap.put(classChecks[q].getName(), classFiles[i].getName().split("\\.")[0]);
					}
				}
				if (!found[q]) {
					classChecks[q].passes--;
					if (classChecks[q].passes == 0) {
						found[q] = true;
						checksRemaining--;
					}
				}
				

			}
		}
		use();
		if (classLoader.findResource("net/minecraft/client/Minecraft.class") != null)
			mainClass = classLoader.loadClass("net.minecraft.client.Minecraft");
		else if (classLoader.findResource("net/minecraft/server/MinecraftServer.class") != null)
			mainClass = classLoader.loadClass("net.minecraft.server.MinecraftServer");
		else
			throw new RuntimeException();
		String typeDump = "";
		Field fields[] = null;
		fields = mainClass.getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			String typeString = fields[i].getType().toString();
			if (typeString.startsWith("class ") && !typeString.contains("."))
				typeDump += typeString.substring(6);
		}
		versionId = typeDump;
		nameMap = new HashMap<String, MinecraftClass>();
		classMap = new HashMap<String, MinecraftClass>();

		for (String name : byteClassNames) {
			ByteClass byteClass = byteClassMap.get(name);
			MinecraftClass minecraftClass = new MinecraftClass(name, byteClass.getClassName());
			minecraftClass.load(this);
			nameMap.put(minecraftClass.getName(), minecraftClass);
			classMap.put(minecraftClass.getClassName(), minecraftClass);
		}
		
		for (MinecraftClass minecraftClass : nameMap.values()) {
			ByteClass byteClass = byteClassMap.get(minecraftClass.getName());
			for (String[] property : byteClass.getProperties())
				minecraftClass.addProperty(new MinecraftProperty(minecraftClass, property[1], property[0]));
			for (String[] method : byteClass.getMethods()) {
				String methodString = obfuscateStringClasses(method[0]);
				methodString = methodString.replaceAll(",INVALID", "").replaceAll("INVALID,","").replaceAll("INVALID", "");
				String methodDeobfName = method[1];
				String methodObfName = methodString.substring(0, methodString.indexOf('('));
				String parameterString = methodString.substring(methodString.indexOf('(') + 1, methodString.indexOf(')'));
				
				if (parameterString.equals("")) {
					minecraftClass.addMethod(new MinecraftMethod(minecraftClass, methodDeobfName, methodObfName));
				} else {
					String[] parameterClasses = parameterString.split(",");
					minecraftClass.addMethod(new MinecraftMethod(minecraftClass, methodDeobfName, methodObfName, parameterClasses));
				}
			}
			for (String[] constructor : byteClass.getConstructors()) {
				String methodString = obfuscateStringClasses(constructor[0]).replaceAll(",INVALID", "").replaceAll("INVALID,","").replaceAll("INVALID", "");
				String methodDeobfName = constructor[1];
				String methodObfName = methodString.substring(0, methodString.indexOf('('));
				String parameterString = methodString.substring(methodString.indexOf('(') + 1, methodString.indexOf(')'));
				
				if (parameterString.equals("")) {
					minecraftClass.addMethod(new MinecraftMethod(minecraftClass, methodDeobfName, methodObfName));
				} else {
					String[] parameterClasses = parameterString.split(",");
					minecraftClass.addMethod(new MinecraftMethod(minecraftClass, methodDeobfName, methodObfName, parameterClasses));
				}
			}
		}
	}

	private String obfuscateStringClasses(String inString) {
		inString = inString.replaceAll(" ", "");
		Pattern cPattern = Pattern.compile("@[A-Za-z]+");
		Matcher cMatcher = cPattern.matcher(inString);
		String tempOutput = inString;
		while (cMatcher.find()) {
			String match = inString.substring(cMatcher.start(), cMatcher.end());
			ByteClass byteClass = getByteClass(match.substring(1));
			if (byteClass != null) {
				tempOutput = tempOutput.replaceAll(match, byteClass.getClassName());
			} else {
				tempOutput = tempOutput.replaceAll(match, "INVALID");
			}
			cMatcher = cPattern.matcher(tempOutput);
		}
		return tempOutput;
	}
	
	public URL getPath() {
		return urlToJar;
	}
	
	private Stack<URL> getLibraries(File path) {
		return getLibraries(path, new Stack<URL>());
	}

	private Stack<URL> getLibraries(File path, Stack<URL> urls) {
		File[] files = path.listFiles();
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				getLibraries(files[i], urls);
			} else {
				try {
					urls.push(files[i].toURI().toURL());
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
		}
		return urls;
	}

	private static final File minecraftDirectory;
	static {
		File mcDir = null;
		File homeDirectory = new File(System.getProperty("user.home", "."));
		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("win")) {
			File appData = new File(System.getenv("APPDATA"));
			if (appData.isDirectory())
				mcDir = new File(appData, ".minecraft");
		} else if (os.contains("mac"))
			mcDir = new File(homeDirectory, "Library/Application Support/minecraft");
		minecraftDirectory = (mcDir != null) ? mcDir : new File(homeDirectory, ".minecraft");
	}
	
	public void use() {
		File librariesPath = new File(minecraftDirectory + "/libraries/");
		if (librariesPath.exists()) {
			Stack<URL> libraries = getLibraries(librariesPath);
			URL[] libraryArray = new URL[libraries.size() + 1];
			libraries.toArray(libraryArray);
			libraryArray[libraries.size()] = urlToJar;
			classLoader = new URLClassLoader(libraryArray);
		} else {
			classLoader = new URLClassLoader(new URL[] { urlToJar });
		}
		Thread.currentThread().setContextClassLoader(classLoader);
	}

	public MinecraftClass getClassByName(String name) {
		return nameMap.get(name);
	}
	public URLClassLoader getClassLoader() {
		return classLoader;
	}
	public Class<?> loadClass(String name) {
		try {
			return classLoader.loadClass(name);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
	public MinecraftClass getClassByType(String name) {
		return classMap.get(name);
	}
	public void registerClass(String publicName, ByteClass bClass) {
		if (byteClassMap.get(publicName)==null) {
			byteClassMap.put(publicName, bClass);
			byteClassNames.add(publicName);
		}
	}
	public ByteClass getByteClass(String name) {
		return byteClassMap.get(name);
	}

	public void setGlobal(String name, MinecraftObject object) {
		globalMap.put(name, object);
	}
	public MinecraftObject getGlobal(String name) {
		return globalMap.get(name);
	}
	
}
