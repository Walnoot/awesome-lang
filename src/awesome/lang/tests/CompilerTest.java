package awesome.lang.tests;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.Scanner;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import awesome.lang.Compiler;
import awesome.lang.Compiler.CompilationException;

public class CompilerTest {
	private static Compiler compiler;
	
	@BeforeClass
	public static void setup(){
		compiler = new Compiler();
	}
	
	@Test
	public void testParse() throws CompilationException {
		compiler.compile("{}");
		compiler.compile("int i = 0;");
		compiler.compile("int i = 1+1+2+3+4;");
	}
	
	@Test
	public void testAssignment() throws IOException, InterruptedException, CompilationException {
		testProgram("int i = 0; i = 7; print(i);", "7");
		testProgram("int i; if(false) i = 7; else i = 4; print(i);", "4");
	}
	
	@Test
	public void testCompile() throws IOException, InterruptedException, CompilationException {
		testProgram("print(2+2+3);", "7");
		testProgram("int x = 1;print(x);", "1");
		testProgram("int x = 1;print(x+3);", "4");
		testProgram("int x = 1; int y = 2;print(x+y);", "3");
	}
	
	@Test
	public void testIf() throws IOException, InterruptedException, CompilationException {
		testProgram("if(true) print(4);", "4");
		testProgram("if(false) print(4); else print(3);", "3");
		testProgram("int x = 1;if(x==1) print(4);", "4");
		testProgram("bool x = true;if(x) print(4);", "4");
		testProgram("bool x = false;if(not x) print(8);", "8");
	}
	
	@Test
	public void testWhile() throws IOException, InterruptedException, CompilationException {
		testProgram("while(false){} print(4);", "4");
		testProgram("int i=0;while(i<5){i=i+1;} print(i);", "5");
		testProgram("int i=0;int j=0;while(i<3){i=i+1;j=j+2;} print(j);", "6");
	}

	@Test
	public void testArrays() throws IOException, InterruptedException, CompilationException {
		testProgram("[int:5] x; x[0] = 5; print(x[0]);", "5");
		testProgram("int x=1;[int:2] y; int z=4; y[0]=2; y[1]=9; print(x); print(z);", "14");
		testProgram("int i=0;[int:4] x; while(i < 4) {x[i] = i; i = i + 1;} print(x[3]);", "3");
		testProgram("[[int:2]:5] x; x[0][0] = 5;", "");
	}

	@Test
	public void testFunctionsTypecheck() throws IOException, InterruptedException, CompilationException {
		testProgram("int i = 1; int add(int a, bool b, int c):{ i = a; }","");
		testProgram("int i = 1; int add(int a, bool b, int c):{ i = a; int b; a=b; }","");
		//testProgram("int i = 1; int add():{ i = 2; } int add(int a):{ i=a; } add(4); print(i);","4");
		testProgram("int i = 1; add(); int add():{ i = 2; } int add(int a):{ i=a; } print(i);","2");
	}
	
	
	@Test
	public void testFor() throws IOException, InterruptedException, CompilationException {
		testProgram("for (int i = 0; i < 10; i = i + 1) { print(i); }", "0123456789");
	}
	
	@Test
	public void testDo() throws IOException, InterruptedException, CompilationException {
		testProgram("int i = 0; do { print(i); i = i + 1; } while(i < 10);", "0123456789");
	}
	
	private void testProgram(String prog, String expected) throws IOException, InterruptedException, CompilationException {
		try{
			compiler.compile(prog).writeSprockell("test.hs");
		} catch(CompilationException e) {
			//print type errors for better debug
			for(String error : e.getErrors()){
				System.out.println(error);
			}
			
			throw e;
		}
		
		Process process = Runtime.getRuntime().exec("ghc -isprockell/src gen/test.hs -e main");

		String output = getOutput(process.getInputStream());
		String err = getOutput(process.getErrorStream());
		
		int exitCode = process.waitFor();
		if(exitCode != 0){
			System.out.println(err);
			Assert.fail("Haskell compilation failed");
		}
		
		Assert.assertEquals("program output", expected, output);
	}
	
	private String getOutput(InputStream stream) throws IOException {
		String output;
		
		try {
			Scanner scanner = new Scanner(stream);
			scanner.useDelimiter("\\A");
			output = scanner.next();
			
			scanner.close();
		} catch(NoSuchElementException e){
			//scanner will throw this exception when there is no output
			output = "";
		}
		
		return output;
	}
}
