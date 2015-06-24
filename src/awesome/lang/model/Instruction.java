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
		
		if (operands.length != opCode.getNumOperands()) {
			throw new IllegalArgumentException("Unexpected number of operands: expected " + opCode.getNumOperands()
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
	
	public void setLabel(Label label) {
		this.label = label;
		label.setInstr(this);
	}
	
	public Label getLabel() {
		return label;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		
		builder.append(opCode);
		
		for (int i = 0; i < operands.length; i++) {
			builder.append(" (");
			
			builder.append(operands[i].toString());

			builder.append(")");
		}
		
		if (label != null) {
			builder.append("--(label: ");
			builder.append(label.getName());
			builder.append(")");
		}
		
		if (comment != null) {
			builder.append("--");
			builder.append(comment);
		}
		
		return builder.toString();
	}
}
