package kangarko.chatcontrol.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;

public class VariableProcessor {

	//private static Procesable procesable = null;

	public static <T> Object process(String input, T objToCast) throws Exception {
		String[] typeCastAndLines = input.split(" -> ");
		Validate.isTrue(typeCastAndLines.length >= 2, "Usage: type -> cast -> methods OR type -> methods");

		String type = typeCastAndLines[0];
		Object cast = typeCastAndLines[1].equals("cast") ? objToCast : null;

		String[] lines = typeCastAndLines[cast == null ? 1 : 2].split("\\|");

		String clazzName = lines[0].replace("%ver", getPackageVersion()).replace("%cast", objToCast.getClass().getPackage().getName() + "." + objToCast.getClass().getSimpleName()); // TODO
		Class<?> clazz = Class.forName(clazzName);
		Objects.requireNonNull(clazz, "Class with path: \'" + clazzName + "\' does not exist");
		Validate.isTrue(lines.length > 0, "Usage: class|methods/fields... (Minimum 1, given " + lines.length + ")");

		List<Param> params = new ArrayList<>();

		for (int i = 1; i < lines.length; i++) {
			// getValue(name, default)
			String raw = lines[i];

			Param param;

			if (raw.contains("(") && raw.contains(")")) {

				// getPlugin ( name,default
				String[] divided = raw.substring(0, raw.length() - 1).split("\\(");
				String name = divided[0];

				// name , default
				String[] paramsRaw = divided[1].split(",");

				
				HashMap<Object, Class<?>> constructors = new HashMap<>();
				
				for (String paramRaw : paramsRaw) {

					Common.Log("&cRawParam: '" + paramRaw + "'");

					Integer integerValue = null;
					Double doubleValue = null;
					Float floatValue = null;
					Class<?> classValue = null;
					Boolean boolValue = null;

					try {
						integerValue = Integer.parseInt(paramRaw);
					} catch (Exception ex) {}
					try {
						doubleValue = Double.parseDouble(paramRaw);
					} catch (Exception ex) {}
					try {
						floatValue = paramRaw.endsWith("F") ? Float.parseFloat(paramRaw.replace("F", "")) : null;
					} catch (Exception ex) {}
					try {
						classValue = paramRaw.endsWith(".class") ? Class.forName(paramRaw.replace(".class", "")) : null;
					} catch (Exception ex) {}
					try {
						boolValue = paramRaw.equals("true") || paramRaw.equals("false") ? Boolean.parseBoolean(paramRaw) : null;
					} catch (Exception ex) {}

					if ("%cast".equals(paramRaw))
						constructors.put(objToCast, objToCast.getClass());
					else if (integerValue != null)
						constructors.put(integerValue, Integer.class);
					else if (doubleValue != null)
						constructors.put(doubleValue, Double.class);
					else if (floatValue != null)
						constructors.put(floatValue, Float.class);
					else if (classValue != null)
						constructors.put(classValue, Class.class);
					else if (boolValue != null)
						constructors.put(boolValue, Boolean.class);
					else
						constructors.put(paramRaw, String.class);
				}

				param = new Param(name, constructors);

			} else
				param = new Param(raw);

			params.add(param);

		}

		if ("get".equals(type)) {
			return processMethodOrField(clazz, cast, params);

		} else if (type.startsWith("list")) {
			return listMethodOrField(clazz, cast, params, type.contains("dec"));
		}

		throw new RuntimeException("Unsupported mode: " + type);
	}

	public static Object processMethodOrField(Class<?> clazz, Object objectToInstance, List<Param> params) throws Exception {
		Object instance = objectToInstance == null ? null : clazz.cast(objectToInstance);

		Param param = params.remove(0);

		Method method = null; // a)
		Field field = null;   // b)

		try {
			method = instance == null ? clazz.getMethod(param.name, param.parameters == null ? null : param.parameters.values().toArray(new Class[param.parameters.size()])) : instance.getClass().getMethod(param.name, param.parameters == null ? null : param.parameters.values().toArray(new Class[param.parameters.size()])); // a)
		} catch (ReflectiveOperationException ex) {
			field = instance == null ?  clazz.getField(param.name) : instance.getClass().getField(param.name); // b)
		}

		Object returned = method == null ? field.get(instance) : method.invoke(instance, param.parameters == null ? null : param.parameters.keySet().toArray());

		if (params.isEmpty())
			return returned;

		return processMethodOrField(returned.getClass(), returned, params);
	}

	public static Object listMethodOrField(Class<?> clazz, Object objectToInstance, List<Param> params, boolean declared) throws Exception {
		Object instance = objectToInstance == null ? null : clazz.cast(objectToInstance);

		outer: { 
			if (params.isEmpty())
				break outer;

			Param param = params.remove(0);

			Method method = null; // a)
			Field field = null;   // b)

			try {
				method = instance == null ? clazz.getMethod(param.name, param.parameters == null ? null : param.parameters.values().toArray(new Class[param.parameters.size()])) : instance.getClass().getMethod(param.name, param.parameters == null ? null : param.parameters.values().toArray(new Class[param.parameters.size()])); // a)
			} catch (ReflectiveOperationException ex) {
				field = instance == null ?  clazz.getField(param.name) : instance.getClass().getField(param.name); // b)
			}

			Object returned = method == null ? field.get(instance) : method.invoke(instance, param.parameters == null ? null : param.parameters.keySet().toArray());

			if (params.isEmpty()) {
				listClassObjects(returned.getClass(), declared);
				return "&cFields and methods printed to console";
			}

			return listMethodOrField(returned.getClass(), returned, params, declared);
		}

		listClassObjects(clazz, declared);
		return "&cFields and methods printed to console";
	}

	private static String getPackageVersion(){
		String ver = Bukkit.getServer().getClass().getPackage().getName();
		return ver.substring(ver.lastIndexOf('.') + 1);
	}

	private static void listClassObjects(Class<?> clazz, boolean declared) {
		Common.LogInFrame(false, "&bFields in " + clazz);
		for (Field f : declared ? clazz.getDeclaredFields() : clazz.getFields())
			Common.Log(f.getType() + ": &f" + f.getName());

		Common.LogInFrame(false, "&aMethods in " + clazz);
		for (Method m : declared ? clazz.getDeclaredMethods() : clazz.getMethods())
			Common.Log("&f" + m.getName() + "&7(" + StringUtils.join(m.getParameterTypes(), ", ") + ")");
	}
}

class Procesable {

	final Class<?> clazz;
	final Object caster;
	final Method method;
	final String params;

	public Procesable(Class<?> clazz, Object caster, Method method, String params) {
		this.clazz = clazz;
		this.caster = caster;
		this.method = method;
		this.params = params;
	}
}

class Param {
	final String name;
	final HashMap<Object, Class<?>> parameters;

	public Param(String name) {
		this(name, null);
	}

	public Param(String name, HashMap<Object, Class<?>> parameters) {
		this.name = name;
		this.parameters = parameters;
	}
}