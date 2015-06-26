package awesome.lang.model;

import java.util.Arrays;

public class Type {
	public static final Type INT = new Type(1, "int");
	public static final Type BOOL = new Type(1, "bool");
	public static final Type VOID = new Type(1, "void");
	
	private int size;
	private String name;
	
	private Type(int size, String name) {
		this.size = size;
		this.name = name;
	}
	
	public int getSize() {
		return size;
	}
	
	public boolean isArray() {
		return false;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	/**
	 * Returns the type of a fixed size array with given type and size.
	 */
	public static ArrayType array(Type type, int size) {
		return new ArrayType(type, size);
	}
	/**
	 * Returns the type of a fixed size function with given type and arguments.
	 */
	public static FunctionType function(Type type, Type... args) {
		return new FunctionType(type, args);
	}
	
	public static class ArrayType extends Type {
		private Type type;
		
		private ArrayType(Type type, int size) {
			super(size*type.getSize(), String.format("[%s:%d]", type.toString(), size));
			this.type = type;
		}
		
		public Type getType() {
			return type;
		}
		
		@Override
		public boolean isArray() {
			return true;
		}
	}
	
	public static class FunctionType extends Type {
		private final Type returnType;
		private final Type[] arguments;

		private FunctionType(Type returnType, Type...arguments) {
			super(1, String.format("(%s -> %s)", Arrays.toString(arguments), returnType));
			this.returnType = returnType;
			this.arguments = arguments;
		}

		public Type getReturnType() {
			return returnType;
		}
		
		public Type[] getArguments() {
			return arguments;
		}
	}
}
