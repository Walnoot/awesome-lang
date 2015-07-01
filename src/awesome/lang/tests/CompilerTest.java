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
	public void testSwitch() throws IOException, InterruptedException, CompilationException {
		testProgram("int i = 5;"
				+ "switch(i) {"
				+ "		case 5 { print(1); next; }"
				+ "		case 6 { print(2); }"
				+ "		case 7 { print(3); }"
				+ "		default{ print(4); }"
				+ "}", "12");
		testProgram("int i = 5;"
				+ "int j = 5;"
				+ "switch(i) {"
				+ "		case 5 { print(1); j = 10; next; }"
				+ "		case 6 { print(2); if (j == 10) next; }"
				+ "		case 7 { print(3); }"
				+ "		default{ print(4); }"
				+ "}", "123");

		testProgram("int i = 700;"
				+ "switch(i) {"
				+ "		case 5 { print(1); next; }"
				+ "		case 6 { print(2); }"
				+ "		case 7 { print(3); }"
				+ "		default{ print(4); }"
				+ "}", "4");

		testProgram("int i = 7;"
				+ "switch(i) {"
				+ "		case 5 { print(1); next; }"
				+ "		case 6 { print(2); }"
				+ "		case 7 { print(3); }"
				+ "}", "3");

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
	public void testEnums() throws IOException, InterruptedException, CompilationException {
		testProgram("enum pers { michiel, jacco} if (pers:michiel != pers:jacco) { print(1); }","1");
		testProgram("room location = room:basement; "
				  + "enum room { kitchen, livingroom, basement } "
				  + "switch(location) {"
				  + " case room:kitchen { print(1); }"
				  + " case room:livingroom { print(2); }"
				  + " case room:basement { print(3); } "
				  + "}", "3");
	}

	@Test
	public void testArrays() throws IOException, InterruptedException, CompilationException {
		testProgram("[int] x = [5]; print(x[0]);", "5");
		testProgram("int x=1;[int] y = [0,0]; int z=4; y[0]=2; y[1]=9; print(x); print(z);", "14");
		testProgram("int i=0;[int] x = [0,0,0,0]; while(i < 4) {x[i] = i; i = i + 1;} print(x[3]);", "3");
		testProgram("[[int]] x = [[0,7], [1,2], [2,6]]; for(int i=0; i<3;i=i+1)print(x[i][1]);", "726");
		testProgram("[[int]] x = [[0,0], [0]]; x[1] = [4,7]; print(x[1][0]); print(x[1][1]);", "47");
		testProgram("[int] x = int[2]; [int] y = int[2]; x[1] = 3; y[1] = 2; print(y[1]);", "2");
		testProgram("[[int]] x = [int][2]; x[1] = [3]; print(x[1][0]);", "3");
	}

	@Test
	public void testStrings() throws IOException, InterruptedException, CompilationException {
		testProgram("print(\"abc\");", "abc");
		testProgram("print(\"abc\\n\");", "abc\n");
	}

	@Test
	public void testFunctionsTypecheck() throws IOException, InterruptedException, CompilationException {
		testProgram("int i = 1; int add(int a, bool b, int c):{ i = a; return 5; }","");
		testProgram("int i = 1; int add(int a, bool b, int c):{ i = a; int b; b=a; return b; } print(add(5,true,2));","5");
		testProgram("int i = 7; bool add():{ i = 2; return true; } int add(int a):{ i=a; return i; } add(4); print(i);","4");
		testProgram("int i = 1; add(); int add():{ i = 2; return 0;} int add(int a):{ i=a; return 0;} print(i);","2");
		testProgram("int a(int n):return b(n); int b(int n):return n-1; print(a(2)); ","1");
		testProgram("int fac(int n):{if(n == 0) return 1; else return n * fac(n-1);} print(fac(3)); ","6");
		testProgram("int fib(int n):{if(n <= 1) return 1; else return fib(n-2) + fib(n-1);} print(fib(5)); ","8");
		testProgram("int i = 1; int add(int a, int b) -> a+b; int add(int a, int b, int c) -> add(a, b) + c; print(add(1,2,3));","6");
	}
	
	@Test
	public void testObjects() throws IOException, InterruptedException, CompilationException {
		testProgram("class Int {int x;} Int obj = new Int; obj.x = 4; print(obj.x);", "4");
	}
	
	@Test
	public void testThreads() throws IOException, InterruptedException, CompilationException {
		String prog = "thread do1(): {print(12345);} thread do2(): {print(12345);} do1(); do2();";
		testProgram(prog, "1234512345");
	}
	
	@Test
	public void testStdlib() throws IOException, InterruptedException, CompilationException {
		testProgram("print(120034);", "120034");
		testProgram("print(-1);", "-1");
	}
	
	@Test
	public void testGlobals() throws IOException, InterruptedException, CompilationException {
		//array x is stored in global mem
		testProgram("[int] x = [0,0]; func(): x[1]=7; func(); print(x[1]);", "7");
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
