package awesome.lang.model;

public class Label {
	private String name;
	private Instruction instr;

	public Label(String name){
		this.name = name;
	}
	
	public void setInstr(Instruction instr) {
		if(instr != null) throw new IllegalStateException("Label already assigned to " + instr);
		
		this.instr = instr;
	}
	
	@Override
	public String toString() {
		return Integer.toString(instr.getPosition());
	}
}
