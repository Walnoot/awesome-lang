package awesome.lang.tests;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import awesome.lang.model.Label;
import awesome.lang.model.OpCode;
import awesome.lang.model.Program;
import awesome.lang.model.Reg;
import awesome.lang.model.Target;

public class SimpleProgramTest {
	private Program program;
	
	@Before
	public void setup() {
		program = new Program();
	}
	
	@Test
	public void test() {
		program.addInstr(OpCode.Const, 78, Reg.RegA);
		program.addInstr(OpCode.Nop).setComment("this is a comment");
		;
		
		System.out.println(program.generateSprockell());
	}
	
	@Test
	public void testArgs() {
		try {
			program.addInstr(OpCode.Nop, Reg.RegA);// invalid, should fail
			Assert.fail();
		} catch (Exception e) {
		}
	}
	
	@Test
	public void testLabels() {
		Label label = new Label("test");
		program.addInstr(OpCode.Nop);
		program.addInstr(OpCode.Nop);
		program.addInstr(label, OpCode.Nop);
		program.addInstr(OpCode.Jump, Target.abs(label));
		
		System.out.println(program.generateSprockell());
		Assert.assertEquals(2, label.getInstr().getPosition());
	}
	
	@Test
	public void testLabelDup() {
		try {
			Label label = new Label("test");
			program.addInstr(label, OpCode.Nop);
			program.addInstr(label, OpCode.Nop);// fails because of duplicate label
			Assert.fail();
		} catch (Exception e) {
		}
	}
}
