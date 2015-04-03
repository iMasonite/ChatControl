package kangarko.chatcontrol.parser;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import kangarko.chatcontrol.utils.Common;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;

public
/** Critical warning:
 *  only God knows how
 *  this */ class /** works! */ 
 ProcessingEngine {

	static final String CODE_DELIMITER = " -> ";

	public static Object process(String input) throws Exception {
		return process(input, null);
	}

	public static <T> Object process(String rawText, T objectToCast) throws Exception {
		Input in = new Input(rawText, objectToCast);

		switch (in.getType()) {
		case GET:
			return executeMethodOrField(in.getCode().getCodeClazz(), in.getCast(), in.getCode().getPieces());

		case LIST:
		case LIST_DECLARED:
			return listClassContent(in.getCode().getCodeClazz(), in.getCast(), in.getCode().getPieces(), in.getType() == OperationType.LIST_DECLARED);

		default:
			throw new RuntimeException("Unsupported mode: " + in.getType());
		}
	}

	public static Object executeMethodOrField(Class<?> clazz, Object objectToInstance, List<CodePiece> parameters) throws Exception {
		Object instance = tryMakeInstance(clazz, objectToInstance);

		CodePiece param = parameters.remove(0);
		Object obj = getFieldOrMethod(instance, param);

		if (parameters.isEmpty())
			return obj;

		return executeMethodOrField(obj.getClass(), obj, parameters);
	}

	public static Object listClassContent(Class<?> clazz, Object objectToInstance, List<CodePiece> parameters, boolean declaredOnly) throws Exception {
		Object instance = tryMakeInstance(clazz, objectToInstance);

		outer: { 
			if (parameters.isEmpty())
				break outer;

			CodePiece code = parameters.remove(0);
			Object returned = getFieldOrMethod(instance, code);

			if (parameters.isEmpty()) {
				displayClassContentToConsole(returned.getClass(), declaredOnly);
				return "&cFields and methods printed to console";
			}

			return listClassContent(returned.getClass(), returned, parameters, declaredOnly);
		}

		displayClassContentToConsole(clazz, declaredOnly);
		return "&cFields and methods printed to console";
	}

	private static Object getFieldOrMethod(Object instance, CodePiece code) throws ReflectiveOperationException {
		Class<?> clazz = instance instanceof Class ? (Class<?>)instance : instance.getClass();

		if (code.getType() == CodeType.FIELD)
			return clazz.getField(code.getName()).get(instance);

		else if (code.getType() == CodeType.METHOD)
			return clazz.getMethod(code.getName(), code.getConstructorClasses()).invoke(instance, code.getConstructorValues());

		throw new NullPointerException("Unknown code type: " + code.getType());
	}

	private static Object tryMakeInstance(Class<?> fromClass, Object objectToInstance) {
		return objectToInstance != null ? fromClass.cast(objectToInstance) : fromClass;
	}

	private static void displayClassContentToConsole(Class<?> clazz, boolean declaredOnly) {
		Common.LogInFrame(false, "&bFields in " + clazz);
		for (Field f : declaredOnly ? clazz.getDeclaredFields() : clazz.getFields())
			Common.Log(f.getType() + ": &f" + f.getName());

		Common.LogInFrame(false, "&aMethods in " + clazz);
		for (Method m : declaredOnly ? clazz.getDeclaredMethods() : clazz.getMethods())
			Common.Log("&f" + m.getName() + "&7(" + StringUtils.join(m.getParameterTypes(), ", ") + ")");
	}
}

class Input {
	private final OperationType type;
	private Object cast = null;
	private final Code code;

	Input(String raw, Object objectToCast) throws ReflectiveOperationException {
		String[] divided = raw.split(ProcessingEngine.CODE_DELIMITER);
		Validate.isTrue(divided.length == 2 || divided.length == 3, "Wrong input! Either specify mode and code OR mode, cast and code");

		this.type = OperationType.parseType(divided[0]);

		if (divided.length == 3) {
			this.cast = objectToCast;
			this.code = new Code(divided[2], cast);
		} else
			this.code = new Code(divided[1]);
	}

	public OperationType getType() {
		return type;
	}

	public Object getCast() {
		return cast;
	}

	public Code getCode() {
		return code;
	}
}

class Code {

	private final Class<?> clazz;
	private final List<CodePiece> pieces;

	Code(String rawLine) throws ReflectiveOperationException {
		this(rawLine, null);
	}

	Code(String rawLine, Object objectToCast) throws ReflectiveOperationException {
		String[] params = rawLine.split("\\|");
		Validate.isTrue(params.length > 0, "Usage: class|methods/fields... (Minimum 1, given " + params.length + ")");

		String clazzName = params[0]
				.replace("%ver", getPackageVersion())
				.replace("%cast", getClassPath(objectToCast));

		this.clazz = Class.forName(clazzName);
		this.pieces = parseCodePieces(params, objectToCast);
	}

	public Class<?> getCodeClazz() {
		return clazz;
	}

	public List<CodePiece> getPieces() {
		return pieces;
	}

	private List<CodePiece> parseCodePieces(String[] rawParts, Object objectToCast) {
		List<CodePiece> allPieces = new ArrayList<>();

		for (int i = 1; i < rawParts.length; i++) {
			// getValue(name, 1)
			String rawPiece = rawParts[i];
			CodePiece piece;

			System.out.println("Parsing: " + rawPiece);

			if (rawPiece.contains("(") && rawPiece.contains(")")) {

				// getPlugin ->(<- name,1
				String[] divided = rawPiece.substring(0, rawPiece.length() - 1).split("\\(");

				// getPlugin is the name
				String name = divided[0];

				// no constructors
				if (divided.length == 1) {
					piece = new CodePiece(name, null);

				} else {
					// 'name' and '1' are constructors
					// now we will be parsing their type
					String[] paramsRaw = divided[1].split(",");

					Constructors constructors = new Constructors();

					for (String paramRaw : paramsRaw) {

						if ("%cast".equals(paramRaw)) // TODO
							constructors.add(objectToCast, objectToCast.getClass());

						else if (paramRaw.startsWith("\"") && paramRaw.endsWith("\""))
							constructors.add(paramRaw.substring(1, paramRaw.length() - 1), String.class);

						else {
							try {
								constructors.add(Integer.parseInt(paramRaw), Integer.class);
							} catch (Exception ex) {
							}
							try {
								constructors.add(Double.parseDouble(paramRaw), Double.class);
							} catch (Exception ex) {
							}
							try {
								constructors.add(paramRaw.endsWith("F") ? Float.parseFloat(paramRaw.replace("F", "")) : null, Double.class);
							} catch (Exception ex) {
							}
							try {
								constructors.add(paramRaw.endsWith(".class") ? Class.forName(paramRaw.replace(".class", "")) : null, Double.class);
							} catch (Exception ex) {
							}
							try {
								constructors.add(paramRaw.equals("true") || paramRaw.equals("false") ? Boolean.parseBoolean(paramRaw) : null, Double.class);
							} catch (Exception ex) {
							}
						}
					}

					piece = new CodePiece(name, constructors);
				}
			} else
				piece = new CodePiece(rawPiece);

			allPieces.add(piece);

		}
		return allPieces;
	}

	private String getPackageVersion(){
		String ver = Bukkit.getServer().getClass().getPackage().getName();
		return ver.substring(ver.lastIndexOf('.') + 1);
	}

	private String getClassPath(Object obj){
		return obj != null ? obj.getClass().getPackage().getName() + "." + obj.getClass().getSimpleName() : "";
	}
}

enum OperationType {
	// get the variable from a field or a method
	GET,

	// print all methods and fields to the console (NB: also executes methods if necessary)
	LIST,

	// do what method above does but only displays declared objects (not from parent classes)
	LIST_DECLARED;

	public static OperationType parseType(String str) {
		try {
			return valueOf(str.toUpperCase());
		} catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Unknown type! Available: " + StringUtils.join(OperationType.values(), ", "));
		}
	}
}

enum CodeType {
	FIELD,
	METHOD
}

// A part of the java code
// Can be method or a field
// Example: setName(kangarko)
class CodePiece {
	private final String name;
	private final CodeType type;
	private final Constructors constructors;

	CodePiece(String field) {
		this(field, CodeType.FIELD, null);
	}

	CodePiece(String method, Constructors constructors) {
		this(method, CodeType.METHOD, constructors);
	}

	private CodePiece(String method, CodeType type, Constructors constructors) {
		this.name = method;
		this.type = type;
		this.constructors = constructors;
	}

	public String getName() {
		return name;
	}

	public CodeType getType() {
		return type;
	}

	public Object[] getConstructorValues() {
		return constructors != null ? constructors.getValues(): null;
	}

	public Class<?>[] getConstructorClasses() {
		return constructors != null ? constructors.getClasses() : null;
	}
}

// Example: method setName("kangarko") has 
// kangarko as a constructor, which is string
class Constructors {
	private final List<Class<?>> classes = new ArrayList<>();
	private final List<Object> values = new ArrayList<>();

	public void add(Object value, Class<?> clazz) {
		if (value != null) {
			values.add(value);
			classes.add(clazz);
		}
	}

	public Object[] getValues() {
		return values.toArray();
	}

	public Class<?>[] getClasses() {
		return classes.toArray(new Class[classes.size()]);
	}
}