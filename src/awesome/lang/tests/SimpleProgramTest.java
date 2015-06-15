package awesome.lang.tests;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import awesome.lang.model.Instruction.OpCode;
import awesome.lang.model.Program;
import awesome.lang.model.Reg;

public class SimpleProgramTest {
	private Program program;
	
	@Before
	public void setup() {
		program = new Program();
	}
	
	@Test
	public void test() {
		program.addInstr(OpCode.Const, 78, Reg.RegA);
		program.addInstr(OpCode.Nop).setComment("this is a comment");;
		
		System.out.println(program.generateSprockell());
	}
	
	@Test
	public void testArgs() {
		try {
			program.addInstr(OpCode.Nop, Reg.RegA);//invalid, should fail
			Assert.fail();
		} catch (Exception e) {
		}
	}
}
