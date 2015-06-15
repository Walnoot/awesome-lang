package awesome.lang.model;

public class Instruction {
	private Label label;// possibly null
	private int position;
	private final OpCode opCode;
	private final Object[] operands;
	
	private String comment;
	
	public Instruction(Label label, OpCode opCode, Object... operands) {
		this.label = label;
		this.opCode = opCode;
		this.operands = operands;
		
		if (operands.length != opCode.numOperands) {
			throw new IllegalArgumentException("Unexpected number of operands: expected " + opCode.numOperands
					+ ", got " + operands.length);
		}
		
		if (label != null){
			label.setInstr(this);
		}
	}
	
	public void setPosition(int position) {
		this.position = position;
	}
	
	public int getPosition() {
		return position;
	}
	
	public void setComment(String comment) {
		this.comment = comment;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		
		builder.append(opCode);
		
		for (int i = 0; i < operands.length; i++) {
			builder.append(' ');
			builder.append(operands[i].toString());
		}
		
		if (comment != null) {
			builder.append("--");
			builder.append(comment);
		}
		
		return builder.toString();
	}
	
	public static enum OpCode {
		Compute(3), Const(2), Branch(2), Jump(1), Load(2), Store(2), Push(1), Pop(1), Read(1), Receive(1), Write(2), TestAndSet(
				1), EndProg(0), Nop(0), Debug(1);
		
		private int numOperands;
		
		private OpCode(int numOperands) {
			this.numOperands = numOperands;
		}
	}
	
	public static enum Operator {
		Add, Sub, Mul, Div, Mod, Equal, NEq, Gt, GtE, LtE, And, Or, Xor, LShift, RShift
	}
	
	public static class MemAddr {
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
	
	/**
	 * Jump target
	 */
	public static class Target {
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
			return String.format("(%s %s)", type, arg);
		}
	}
}
