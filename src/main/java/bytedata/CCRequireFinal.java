package bytedata;

import minecraft.Minecraft;

public class CCRequireFinal extends CCRequireSimple {
	public CCRequireFinal(ClassChecker checker) {
		super(checker);
	}

	public boolean canPass(Minecraft mc, ByteClass bClass) {
		return bClass.isFinal();
	}
}
