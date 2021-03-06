package bytedata;

import minecraft.Minecraft;

public class CCRequire extends ClassChecker {
	private ClassChecker checker;
	private String[] names;
	public CCRequire(ClassChecker cc, String... requiredNames) {
		super(cc.getName());
		checker = cc;
		names = requiredNames;
	}
	public void check(Minecraft mc, ByteClass bClass) {
		for (int i = 0; i < names.length; i++) {
			if (mc.getByteClass(names[i]) == null) return;
		}
		checker.check(mc, bClass);
		isComplete = checker.isComplete;
	}
	public String toString() {
		return "[Require " + names[0] + " " + checker + "]";
	}
}
