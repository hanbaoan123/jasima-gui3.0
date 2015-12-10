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
package jasima_gui.editor;

import jasima_gui.util.TypeUtil;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class PropertyLister {
	private static PropertyLister instance = null;

	public static PropertyLister getInstance() {
		if (instance == null)
			instance = new PropertyLister();
		return instance;
	}

	private Map<String, IConfigurationElement> filters = new HashMap<String, IConfigurationElement>();

	private PropertyLister() {
		for (IConfigurationElement elem : RegistryFactory.getRegistry()
				.getConfigurationElementsFor("jasima_gui.propertyFilters")) {
			String propType = elem.getAttribute("propertyType");
			if (filters.containsKey(propType)) {
				System.err.printf("Two filters defined for %s!%n", propType);
			}
			filters.put(propType, elem);
		}
	}

	protected class Filter {
		protected IProperty parentProp;
		protected Map<String, IProperty> properties;

		protected Filter(IProperty parentProp, Map<String, IProperty> properties) {
			this.parentProp = parentProp;
			this.properties = properties;
		}

		protected void hideProperty(IConfigurationElement elem) {
			properties.remove(elem.getAttribute("name"));
		}

		protected void addProperty(IConfigurationElement elem) {
			Map<String, String> attributes = new HashMap<String, String>();
			for (IConfigurationElement attr : elem.getChildren("attribute")) {
				attributes.put(attr.getAttribute("name"),
						attr.getAttribute("value"));
			}
			Bundle bundle = FrameworkUtil
					.getBundle(EditorWidgetFactory.class)
					.getBundleContext()
					.getBundle(
							Long.parseLong(((RegistryContributor) elem
									.getContributor()).getActualId()));
			try {
				IProperty prop = (IProperty) bundle
						.loadClass(elem.getAttribute("propertyClass"))
						.getConstructor(Bundle.class, IProperty.class,
								Map.class)
						.newInstance(bundle, parentProp, attributes);
				properties.put(prop.getName(), prop);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		protected void apply() {
			apply(TypeUtil.toClass(parentProp.getType()));
		}

		protected void apply(Class<?> type) {
			Class<?> superClass = type.getSuperclass();

			if (superClass != null) {
				// apply filter for the super class, if it exists
				apply(superClass);

				// apply filters for all newly implemented interfaces
				Set<Class<?>> superInterfaces = new HashSet<Class<?>>(
						Arrays.asList(superClass.getInterfaces()));
				for (Class<?> k : type.getInterfaces()) {
					if (superInterfaces.contains(k))
						continue; // we only care about new ifaces
					apply(k);
				}
			}

			// apply filters for the class itself
			IConfigurationElement elem = filters.get(type.getCanonicalName());
			if (elem != null) {
				for (IConfigurationElement el : elem.getChildren()) {
					String n = el.getName();
					if (n.equals("hideProperty")) {
						hideProperty(el);
					} else if (n.equals("addProperty")) {
						addProperty(el);
					} else {
						assert false;
					}
				}
			}
		}
	}

	public Collection<IProperty> listProperties(IProperty bean,
			TopLevelEditor editor) {
		try {
			BeanInfo bi = Introspector.getBeanInfo(TypeUtil.toClass(bean
					.getType()));
			PropertyDescriptor[] propDesc = bi.getPropertyDescriptors();
			SortedMap<String, IProperty> properties = new TreeMap<String, IProperty>();
			for (PropertyDescriptor pd : propDesc) {
				if (pd.isHidden())
					continue;
				if (pd.getPropertyType() == null) // indexed property
					continue;
				if (pd.getReadMethod() == null) // write-only property
					continue;
				if (pd.getWriteMethod() == null) // read-only property
					continue;
				BeanProperty prop = new BeanProperty(bean, editor, pd);
				properties.put(prop.getName(), prop);
			}

			new Filter(bean, properties).apply();

			return properties.values();
		} catch (IntrospectionException e) {
			return Collections.emptyList();
		}
	}

	public static Collection<IProperty> listWritableProperties(IProperty bean,
			TopLevelEditor editor) {
		try {
			BeanInfo bi = Introspector.getBeanInfo(TypeUtil.toClass(bean
					.getType()));
			PropertyDescriptor[] propDesc = bi.getPropertyDescriptors();
			SortedMap<String, IProperty> properties = new TreeMap<String, IProperty>();
			for (PropertyDescriptor pd : propDesc) {
				if (pd.getWriteMethod() == null) // read-only property
					continue;
				BeanProperty prop = new BeanProperty(bean, editor, pd);
				properties.put(prop.getName(), prop);
			}

			return properties.values();
		} catch (IntrospectionException e) {
			return Collections.emptyList();
		}
	}
}
