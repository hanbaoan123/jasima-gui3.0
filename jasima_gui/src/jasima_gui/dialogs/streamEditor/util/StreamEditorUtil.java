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
package jasima_gui.dialogs.streamEditor.util;

import jasima_gui.dialogs.streamEditor.DetailsPageBase;
import jasima_gui.dialogs.streamEditor.DetailsPageDblConst;
import jasima_gui.dialogs.streamEditor.DetailsPageDblExp;
import jasima_gui.dialogs.streamEditor.DetailsPageDblTriangular;
import jasima_gui.dialogs.streamEditor.DetailsPageDblUniform;
import jasima_gui.dialogs.streamEditor.DetailsPageIntConst;
import jasima_gui.dialogs.streamEditor.DetailsPageIntEmpirical;
import jasima_gui.dialogs.streamEditor.DetailsPageIntUniform;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Objects;
import java.util.StringTokenizer;

public class StreamEditorUtil {

	private static final String LIST_DELIMS = ", \t\r\n";
	@SuppressWarnings("unchecked")
	public static Class<DetailsPageBase>[] TYPES_ALL = new Class[] {
			DetailsPageDblUniform.class, DetailsPageDblExp.class,
			DetailsPageDblConst.class, DetailsPageDblTriangular.class,
			DetailsPageIntUniform.class, DetailsPageIntConst.class,
			DetailsPageIntEmpirical.class };

	public static Class<?> loadClass(String name, ClassLoader classLoader) {
		try {
			return classLoader.loadClass(name);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static Object instanciate(Class<?> klass) {
		try {
			return klass.newInstance();
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public static Object createStreamFromStreamDef(Object streamDef) {
		Objects.requireNonNull(streamDef);

		try {
			Method m = streamDef.getClass().getMethod("createStream");
			return m.invoke(streamDef);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static Object createStreamDefFromStream(Object stream) {
		Objects.requireNonNull(stream);
		// find and call method by reflection
		try {
			Method m = stream.getClass().getMethod("createStreamDefFromStream");
			return m.invoke(stream);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static Object[] createCompatibleStreamDefs(Class<?> baseClass,
			Class<DetailsPageBase>[] allPageTypes, ClassLoader classLoader) {
		ArrayList<Object> streamDefInsts = new ArrayList<Object>();
		ArrayList<DetailsPageBase> pages = new ArrayList<DetailsPageBase>();
		for (int i = 0; i < allPageTypes.length; i++) {
			DetailsPageBase page = (DetailsPageBase) instanciate(allPageTypes[i]);
			Class<?> streamDefClass = loadClass(page.getInputType(),
					classLoader);

			// create placeholder StreamDef object
			Object streamDef = instanciate(streamDefClass);
			// create accompanying stream
			Object streamInst = createStreamFromStreamDef(streamDef);

			// is stream compatible with base type?
			if (baseClass.isAssignableFrom(streamInst.getClass())) {
				streamDefInsts.add(streamDef);
				pages.add(page);
			}
		}

		return new Object[] { streamDefInsts.toArray(),
				pages.toArray(new DetailsPageBase[pages.size()]) };
	}

	/**
	 * Examples: "5" -> {5}; "23,5,10,3" -> {23,5,10,3}; "1,2,3" -> {1,2,3}
	 */
	public static int[] parseIntList(String list) {
		ArrayList<Integer> res = new ArrayList<Integer>();
		StringTokenizer st = new StringTokenizer(list, LIST_DELIMS);
		while (st.hasMoreElements()) {
			int v = Integer.parseInt(st.nextToken().trim());
			res.add(v);
		}

		// convert Integer[] to int[]
		int[] is = new int[res.size()];
		for (int i = 0; i < res.size(); i++)
			is[i] = res.get(i);
		return is;
	}

	/**
	 * Converts a list of comma-separated double values (with dot as decimal
	 * separator) to a double-array. Example: parseDblList("1.23,4.56") ->
	 * {1.23,4.56}
	 */
	public static double[] parseDblList(String s) {
		ArrayList<Double> ll = new ArrayList<Double>();
		StringTokenizer st = new StringTokenizer(s, LIST_DELIMS);
		while (st.hasMoreElements()) {
			double v = Double.parseDouble(st.nextToken().trim());
			ll.add(v);
		}

		double[] res = new double[ll.size()];
		for (int i = 0; i < res.length; i++) {
			res[i] = ll.get(i);
		}
		return res;
	}

	private StreamEditorUtil() {
	}

}
