package awesome.lang.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Type {
	public static final Type INT = new Type(1, "int");
	public static final Type BOOL = new Type(1, "bool");
	public static final Type CHAR = new Type(1, "char");
	public static final Type VOID = new Type(1, "void");
	public static final Type LOCK = new Type(1, "lock");
	private static HashMap<String, EnumType> enums = new HashMap<String, EnumType>();
	private static HashMap<String, ClassType> classes = new HashMap<String, ClassType>();
	
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

	public static boolean enumExists(String name) {
		return Type.enums.containsKey(name);
	}
	
	public static void clearUserTypes(){
		enums.clear();
		classes.clear();
	}
	
	/**
	 * If not exists, returns null. 
	 */
	public static EnumType getEnum(String name) {
		if (Type.enumExists(name) == false)
			return null;
		return Type.enums.get(name);
	}
	

	public static boolean classExists(String name) {
		return Type.classes.containsKey(name);
	}
	
	/**
	 * If not exists, returns null. 
	 */
	public static ClassType getClass(String name) {
		if (Type.classExists(name) == false)
			return null;
		return Type.classes.get(name);
	}
	
	/**
	 * Returns the type of a fixed size array with given type and size.
	 */
	public static ArrayType array(Type type) {
		return new ArrayType(type);
	}
	/**
	 * Returns the type of a fixed size function with given type and arguments.
	 */
	public static FunctionType function(Type type, Type... args) {
		return new FunctionType(type, args);
	}
	
	/**
	 * Returns the type of an enumerator, with given name and values. Returns null if the name is already taken
	 */
	public static EnumType newEnum(String name, ArrayList<String> values) {
		if (Type.enums.containsKey(name))
			return null;
		
		EnumType newEnum = new EnumType(name, values);
		Type.enums.put(name, newEnum);
		return newEnum;
	}
	
	/**
	 * Returns the type of an class, with given name. Returns null if the name is already taken
	 */
	public static ClassType newClass(String name) {
		if (Type.classes.containsKey(name))
			return null;
		
		ClassType newClass = new ClassType(name, null);
		Type.classes.put(name, newClass);
		return newClass;
	}
	
	public static class ClassType extends Type {
		
		private Scope scope = null;

		private ClassType(String name, Scope scope) {
			super(1, name);
			this.scope = scope;
		}
		
		public void setScope(Scope scope) {
			if (this.scope != null)
				throw new IllegalStateException("Cannot overwrite an already set scope.");
			this.scope = scope;
		}
		
		public Scope getScope() {
			return this.scope;
		}
		
	}
	
	public static class EnumType extends Type {
		
		private HashMap<String, Integer> values = new HashMap<String, Integer>();

		private EnumType(String name, ArrayList<String> values) {
			super(Type.INT.getSize(), name);// mapped as int
			int i = 1; // first value
			for(String el : values) {
				this.values.put(el, i++);
			}
		}
		
		/**
		 * returns 0 if not found
		 */
		public int getValue(String name) {
			if (!this.contains(name))
				return 0;
			
			return this.values.get(name);
		}
		
		public boolean contains(String name) {
			return this.values.containsKey(name);
		}
		
		
	}
	
	public static class ArrayType extends Type {
		private Type type;
		
		private ArrayType(Type type) {
			super(1, String.format("[%s]", type.toString()));
			this.type = type;
		}
		
		public Type getType() {
			return type;
		}
		
		@Override
		public boolean isArray() {
			return true;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(obj instanceof ArrayType) {
				return this.type.equals(((ArrayType) obj).type);
			} else {
				return false;
			}
		}
	}
	
	public static class FunctionType extends Type {
		private final Type returnType;
		private final Type[] arguments;
		private boolean isMethod;

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

		public boolean isMethod() {
			return isMethod;
		}

		public void setMethod(boolean isMethod) {
			this.isMethod = isMethod;
		}
	}
}
