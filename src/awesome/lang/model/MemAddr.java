package awesome.lang.model;

public class MemAddr {
	private String type;
	private Object arg;
	
	private MemAddr(String type, Object arg) {
		this.type = type;
		this.arg = arg;
	}
	
	public static MemAddr direct(int address) {
		return new MemAddr("Addr", address);
	}
	
	public static MemAddr deref(Reg reg) {
		return new MemAddr("Deref", reg);
	}
	
	@Override
	public String toString() {
		return String.format("(%s %s)", type, arg);
	}
}
