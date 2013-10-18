package minecraft;

public class MinecraftObject {
	private final MinecraftClass type;
	private final Object value;
	public MinecraftObject(MinecraftClass type, Object value) {
		this.type = type;
		this.value = value;
	}

	public MinecraftObject(Minecraft mc, Object value) {
		this.type = mc.getClassByType(value.getClass().getCanonicalName());
		this.value = value;
	}

	public Object get() {
		return value;
	}
	
	public Object callFunction(String funcName, Object... args) {
		return type.callFunction(funcName, this, args);
	}
}
