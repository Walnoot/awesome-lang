package awesome.lang.model;

public class Instruction {
	public enum OpCode {
		Compute, Const, Branch, Jump, Load, Store, Push, Pop, Read, Receive, Write, TestAndSet, EndProg, Nop, Debug
	}
}
