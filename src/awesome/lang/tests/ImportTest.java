package awesome.lang.tests;

import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import awesome.lang.ImportResolver;

public class ImportTest {
	@Test
	public void test() {
		//tests circular dependencies
		testImport("import1.awl", "default.awl", "import1.awl", "import2.awl", "import3.awl");
		testImport("import4.awl", "default.awl", "import4.awl", "import3.awl");
	}

	private void testImport(String file, String...expected) {
		ImportResolver resolver = new ImportResolver(Paths.get("src/awesome/lang/tests/files/" + file));
		
		Assert.assertEquals(Arrays.asList(expected), resolver.getImports());
	}
}
