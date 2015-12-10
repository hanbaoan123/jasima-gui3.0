/*
This file is part of jasima, v1.3, the Java simulator for manufacturing and logistics.
 
Copyright (c) 2015 		jasima solutions UG
Copyright (c) 2010-2015 Torsten Hildebrandt and jasima contributors

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package jasima_gui.util;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;

/**
 * This class contains static methods that are missing from java.lang.Class.
 */
public class TypeUtil {

	public static final String XML_RUNNER_CLASS = "jasima.core.run.ConsoleRunner";
	public static final String XLS_RUNNER_CLASS = "jasima.core.run.ExcelExperimentRunner";
	
	private static final HashMap<Class<?>, Class<?>> PRIMITIVE_WRAPPERS = new HashMap<Class<?>, Class<?>>();
	private static final HashMap<Class<?>, Object> PRIMITIVE_DEFAULTS = new HashMap<Class<?>, Object>();

	static {
		PRIMITIVE_WRAPPERS.put(boolean.class, Boolean.class);
		PRIMITIVE_WRAPPERS.put(byte.class, Byte.class);
		PRIMITIVE_WRAPPERS.put(short.class, Short.class);
		PRIMITIVE_WRAPPERS.put(int.class, Integer.class);
		PRIMITIVE_WRAPPERS.put(long.class, Long.class);
		PRIMITIVE_WRAPPERS.put(char.class, Character.class);
		PRIMITIVE_WRAPPERS.put(float.class, Float.class);
		PRIMITIVE_WRAPPERS.put(double.class, Double.class);
		PRIMITIVE_WRAPPERS.put(void.class, Void.class);

		PRIMITIVE_DEFAULTS.put(Boolean.class, false);
		PRIMITIVE_DEFAULTS.put(Byte.class, (byte) 0);
		PRIMITIVE_DEFAULTS.put(Short.class, (short) 0);
		PRIMITIVE_DEFAULTS.put(Integer.class, 0);
		PRIMITIVE_DEFAULTS.put(Long.class, 0L);
		PRIMITIVE_DEFAULTS.put(Character.class, '\0');
		PRIMITIVE_DEFAULTS.put(Float.class, 0f);
		PRIMITIVE_DEFAULTS.put(Double.class, 0.);
	}

	/**
	 * If a class name is longer than this, it will be abbreviated.
	 */
	private static final int MAX_UNABBR_CLASS_LENGTH = 30;

	/**
	 * Determines if klass represents a wrapper for a primitive type.
	 * 
	 * @return true if klass represents one of Boolean, Byte, Short, Integer,
	 *         Long, Character, Float, Double or Void
	 */
	public static boolean isPrimitiveWrapper(Class<?> klass) {
		return PRIMITIVE_WRAPPERS.containsValue(klass);
	}

	/**
	 * Determines if klass represents either a wrapper for a primitive type or a
	 * primitive type.
	 * 
	 * @return klass.isPrimitive() || isPrimitiveWrapper(klass)
	 */
	public static boolean isPrimitiveOrWrapper(Class<?> klass) {
		return klass.isPrimitive() || isPrimitiveWrapper(klass);
	}

	/**
	 * Determines the primitive wrapper class for a primitive type.
	 * 
	 * @return the primitive wrapper class (e.g. Integer for int), or null if
	 *         type doesn't represent a primitive type.
	 */
	public static Class<?> getPrimitiveWrapper(Class<?> type) {
		return PRIMITIVE_WRAPPERS.get(type);
	}

	/**
	 * Returns the default value (JLS 4.12.5) of a primitive type.
	 * 
	 * @param type
	 *            a primitive wrapper class
	 * @return an object wrapping 0, null, false, ...
	 */
	public static <K> K createDefaultPrimitive(Class<K> type) {
		return type.cast(PRIMITIVE_DEFAULTS.get(type));
	}

	/**
	 * Converts a Type instance to a Class instance, erasing all generic
	 * information.
	 * 
	 * <ul>
	 * <li>If t is a Class, it is directly returned.</li>
	 * <li>If t is a ParametrizedType, its raw type is converted to Class and
	 * returned</li>
	 * <li>If t is a GenericArrayType, its component type is converted to Class,
	 * which will be the return value's component type.</li>
	 * <li>If t is any other type, Object.class is returned.</li>
	 * </ul>
	 * 
	 * @param t
	 *            an instance of Type
	 * @return t converted to a Class instance
	 */
	public static Class<?> toClass(Type t) {
		if (t instanceof Class)
			return (Class<?>) t;
		if (t instanceof ParameterizedType)
			return (Class<?>) ((ParameterizedType) t).getRawType();
		if (t instanceof GenericArrayType)
			return Array.newInstance(toClass(((GenericArrayType) t).getGenericComponentType()), 0).getClass();
		return Object.class;
	}

	public static String toString(Class<?> c, boolean abbrev) {
		String name = c.getCanonicalName();
		if (!abbrev)
			return name;
		name = name.replaceFirst("^java\\.(?:lang|util)\\.", "");
		StringBuilder prefix = new StringBuilder(16);
		while (prefix.length() + name.length() > MAX_UNABBR_CLASS_LENGTH) {
			int dot = name.indexOf('.');
			if (dot == -1)
				break;
			if (Character.isLowerCase(name.charAt(0))) {
				prefix.append(name.charAt(0));
				prefix.append('.');
			} else {
				// don't abbreviate outer class
				prefix.append(name.substring(0, dot + 1));
			}
			name = name.substring(dot + 1);
		}
		return prefix + name;
	}

	public static String toString(Type t) {
		return toString(t, false);
	}

	public static String toString(Type t, boolean abbrev) {
		Class<?> c = toClass(t);
		Type[] tParams = c.getTypeParameters();
		if (tParams.length > 0) {
			if (t instanceof ParameterizedType) {
				tParams = ((ParameterizedType) t).getActualTypeArguments();
			}
			Formatter name = new Formatter(Locale.ROOT);
			name.format("%s<", toString(c, abbrev));
			for (int i = 0; i < tParams.length; ++i) {
				name.format(i == 0 ? "%s" : ", %s",
						tParams[i] instanceof TypeVariable ? "?" : toString(tParams[i], abbrev));
			}
			name.format(">");
			String retVal = name.toString();
			name.close();
			return retVal;
		} else {
			return toString(c, abbrev);
		}
	}

	public static Type applyParameters(Type type, Type superType) {
		if (superType instanceof ParameterizedType && type instanceof ParameterizedType) {
			final ParameterizedType pSuper = (ParameterizedType) superType;
			final ParameterizedType pType = (ParameterizedType) type;
			return new ParameterizedType() {
				@Override
				public Type getRawType() {
					return pSuper.getRawType();
				}

				@Override
				public Type getOwnerType() {
					return pSuper.getOwnerType();
				}

				@Override
				public Type[] getActualTypeArguments() {
					if (pSuper.getActualTypeArguments().length == 0)
						return new Type[0];
					return pType.getActualTypeArguments();
					// FIXME
				}
			};
		}
		return superType;
	}

	/**
	 * Creates an array type with a given component type.
	 * 
	 * @param klass
	 *            the component type (E.class)
	 * @return an array type (E[].class)
	 */
	@SuppressWarnings("unchecked")
	public static <E> Class<E[]> toArray(Class<E> klass) {
		return (Class<E[]>) Array.newInstance(klass, 0).getClass();
	}

	private TypeUtil() {
		// never called
	}

	/**
	 * Determines whether a subclass of <code>klass</code> can be declared.
	 * Primitives or <code>final</code> classes can not be subclassed.
	 * 
	 * @param klass
	 *            the type to check
	 * @return <code>true</code> if <code>klass</code> can be subclassed
	 */
	public static boolean canSubclass(Class<?> klass) {
		return (klass.getModifiers() & Modifier.FINAL) == 0;
	}
}
