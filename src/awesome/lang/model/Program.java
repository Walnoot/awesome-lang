package awesome.lang.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Program {
	private ArrayList<Instruction> instructions = new ArrayList<Instruction>();
	private int numSprockells;
	
	public Program(){
		this(1);
	}
	
	public Program(int numSprockells){
		this.numSprockells = numSprockells;
	}
	
	public Instruction addInstr(OpCode opCode, Object...operands){
		return addInstr((Label) null, opCode, operands);
	}
	
	public Instruction addInstr(String label, OpCode opCode, Object...operands){
		return addInstr(new Label(label), opCode, operands);
	}
	
	public Instruction addInstr(Label label, OpCode opCode, Object...operands){
		Instruction instr = new Instruction(label, opCode, operands);
		instr.setPosition(instructions.size());
		addInstr(instr);
		
		return instr;
	}
	
	public void addInstr(Instruction instr){
		instructions.add(instr);
	}
	
	public void writeSprockell(String pathName) throws IOException{
		String prog = generateSprockell();
		Path path = Paths.get("gen", pathName);
		path.toFile().getParentFile().mkdirs();
		Files.write(path, prog.getBytes());
	}
	
	public String generateSprockell(){
		StringBuilder builder = new StringBuilder();

		builder.append("import Sprockell.System\n");
		builder.append("prog :: [Instruction]\n");
		builder.append("prog = [\n");
		
		for (int i = 0; i < instructions.size(); i++) {
			Instruction instr = instructions.get(i);
			
			builder.append(i == 0 ? "\t  " : "\t, ");
			builder.append(instr);
			builder.append('\n');
		}

		builder.append("\t]\n");
		builder.append("main = run " + numSprockells + " prog\n");
		
		return builder.toString();
	}
}
