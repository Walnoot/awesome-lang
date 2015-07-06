A main method that compiles AWL programs is found in src/awesome/lang/Compiler.java
Documentation on how to use this is found there.

Unit tests are found in src/awesome/lang/tests
Note that in order for CompilerTest to work ghc must be installed.

Test programs found in src/awesome/lang/examples are already compiled and found in gen/*.hs
Running these can be done as follows:
> cd /path/to/project/gen
> ghc -i../sprockell/src <example>.hs -e main
