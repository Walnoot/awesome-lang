package awesome.lang.model;

/**
 * Jump target
 */
public class Target {
	private String type;
	private Object arg;
	
	private Target(String type, Object arg) {
		this.type = type;
		this.arg = arg;
	}
	
	public static Target abs(int address) {
		return new Target("Abs", address);
	}
	
	public static Target abs(Label label) {
		return new Target("Abs", label);
	}
	
	public static Target rel(int address) {
		return new Target("Rel", address);
	}
	
	public static Target ind(Reg reg) {
		return new Target("Abs", reg);
	}
	
	@Override
	public String toString() {
		return String.format("%s %s", type, arg);
	}
}
