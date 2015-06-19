package awesome.lang.model;

public enum OpCode {
	Compute(4), Const(2), Branch(2), Jump(1), Load(2), Store(2), Push(1), Pop(1), Read(1), Receive(1), Write(2), TestAndSet(
			1), EndProg(0), Nop(0), Debug(1);
	
	private int numOperands;
	
	private OpCode(int numOperands) {
		this.numOperands = numOperands;
	}
	
	public int getNumOperands() {
		return numOperands;
	}
}
