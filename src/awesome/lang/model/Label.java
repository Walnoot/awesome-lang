package awesome.lang.model;

public class Label {
	private String name;
	private Instruction instr;
	
	public Label(String name) {
		this.name = name;
	}
	
	public void setInstr(Instruction instr) {
		if (this.instr != null)
			throw new IllegalStateException("Label already assigned to " + instr);
		
		this.instr = instr;
	}
	
	public String getName() {
		return name;
	}
	
	public Instruction getInstr() {
		return instr;
	}
	
	@Override
	public String toString() {
		if (instr == null) {
			return "unset(" + name + ")";
		} else {
			return Integer.toString(instr.getPosition());
		}
	}
}
