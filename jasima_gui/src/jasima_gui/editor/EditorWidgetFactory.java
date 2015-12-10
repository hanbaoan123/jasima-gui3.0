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

import jasima_gui.PropertyToolTip;
import jasima_gui.editor.EditorWidget.EditorListener;
import jasima_gui.util.TypeUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.IExpansionListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class EditorWidgetFactory {

	private static boolean matchConfigurationElement(
			IConfigurationElement elem, Class<?> propertyType,
			String propertyName, boolean handlesNull) {
		if (!propertyType.getCanonicalName().equals(
				elem.getAttribute("propertyType")))
			return false;
		if (propertyName == null) {
			if (elem.getAttribute("propertyName") != null)
				return false;
		} else {
			if (!propertyName.equals(elem.getAttribute("propertyName")))
				return false;
		}
		if ("true".equals(elem.getAttribute("handlesLifeCycle")) != handlesNull) {
			return false;
		}
		return true;
	}

	private Iterable<Class<?>> getNewInterfaces(Class<?> klass) {
		ArrayList<Class<?>> retVal = new ArrayList<Class<?>>();
		Class<?> sklass = klass.getSuperclass();
		Set<Class<?>> superInterfaces;
		if (sklass != null) {
			superInterfaces = new HashSet<Class<?>>(Arrays.asList(sklass
					.getInterfaces()));
		} else {
			superInterfaces = Collections.emptySet();
		}
		for (Class<?> k : klass.getInterfaces()) {
			if (superInterfaces.contains(k))
				continue; // we only care about new ifaces
			retVal.add(k);
		}
		return retVal;
	}

	private IConfigurationElement findEditor(final Class<?> propType,
			String propName, boolean canBeNull,
			IConfigurationElement[] allEditors) {
		return findEditor(propType, propName, canBeNull, allEditors, false);
	}

	private IConfigurationElement findEditor(final Class<?> propType,
			String propName, boolean canBeNull,
			IConfigurationElement[] allEditors, boolean ignoreFallback) {
		Class<?> klass = propType;

		assert klass != null;

		// System.out.printf("Trying %s... ", propType);

		if (!canBeNull) {
			// look for a direct match that doesn't handle the life cycle
			for (IConfigurationElement elem : allEditors) {
				if (matchConfigurationElement(elem, klass, propName, false)) {
					return elem;
				}
			}
		}

		// look for a direct match that can handle the life cycle
		for (IConfigurationElement elem : allEditors) {
			if (matchConfigurationElement(elem, klass, propName, true)) {
				return elem;
			}
		}

		// no direct match -> first, try all newly implemented interfaces
		// interfaces might be matched twice
		for (Class<?> k : getNewInterfaces(propType)) {
			IConfigurationElement match = findEditor(k, propName, canBeNull,
					allEditors, true);
			if (match != null)
				return match;
		}

		// then, try the superclass
		if (klass.isPrimitive()) {
			return findEditor(TypeUtil.getPrimitiveWrapper(klass), propName,
					canBeNull, allEditors, false);
		} else if (klass.isArray()) {
			klass = klass.getComponentType();
			if (klass == Object.class)
				return null;

			if (klass.isPrimitive()) {
				return findEditor(
						TypeUtil.toArray(TypeUtil.getPrimitiveWrapper(klass)),
						propName, canBeNull, allEditors, false);
			}
			for (Class<?> k : getNewInterfaces(klass)) {
				IConfigurationElement match = findEditor(TypeUtil.toArray(k),
						propName, canBeNull, allEditors, true);
				// FIXME don't match Object[] here
				if (match != null)
					return match;
			}
			if (klass.isInterface()) {
				if (ignoreFallback)
					return null;
				return findEditor(Object[].class, propName, canBeNull,
						allEditors, true);
			}
			return findEditor(TypeUtil.toArray(klass.getSuperclass()),
					propName, canBeNull, allEditors, false);
		} else if (klass.isInterface()) {
			if (ignoreFallback)
				return null;
			return findEditor(Object.class, propName, canBeNull, allEditors,
					true);
		} else {
			klass = klass.getSuperclass();
			if (klass == null)
				return null;
			return findEditor(klass, propName, canBeNull, allEditors, false);
		}

	}

	public EditorWidget createEditorWidget(final TopLevelEditor ote,
			Composite parent, IProperty prop, IProperty parentProp) {
		Class<?> propType = TypeUtil.toClass(prop.getType());

		if (EditorWidget.class.isAssignableFrom(propType)) {
			try {
				EditorWidget editor = (EditorWidget) propType.getConstructor(
						Composite.class).newInstance(parent);
				editor.initialize(prop, ote);
				editor.createControls();
				return editor;
			} catch (Exception e) {
				throw new AssertionError(e);
			}
		}

		IConfigurationElement[] allEditors = RegistryFactory.getRegistry()
				.getConfigurationElementsFor("jasima_gui.objectEditors");

		IConfigurationElement bestMatch;

		if (parentProp != null) {
			bestMatch = findEditor(TypeUtil.toClass(parentProp.getType()),
					prop.getName(), prop.canBeNull(), allEditors);
		} else {
			bestMatch = null;
		}

		// we didn't find an editor matching type.name, let's look for one
		// matching just type
		if (bestMatch == null) {
			bestMatch = findEditor(propType, null, prop.canBeNull(), allEditors);
		}

		assert bestMatch != null; // we should have one by now

		try {
			Bundle bundle = FrameworkUtil
					.getBundle(EditorWidgetFactory.class)
					.getBundleContext()
					.getBundle(
							Long.parseLong(((RegistryContributor) bestMatch
									.getContributor()).getActualId()));
			EditorWidget editor = (EditorWidget) bundle
					.loadClass(bestMatch.getAttribute("class"))
					.getConstructor(Composite.class).newInstance(parent);
			editor.initialize(prop, ote);
			editor.createControls();
			return editor;
		} catch (Exception e) {
			throw new AssertionError(e);
		}

	}

	/**
	 * parent must have a GridLayout for this to work.
	 */
	public EditorWidget createEditorWidgetWithLabel(final TopLevelEditor tle,
			final Composite parent, IProperty prop, IProperty parentProp) {
		Label lbl = tle.getToolkit().createLabel(parent, prop.getName(),
				prop.isImportant() ? SWT.BOLD : 0);
		final EditorWidget editor = createEditorWidget(tle, parent, prop,
				parentProp);
		if (editor.isExpandable()) {
			lbl.dispose();

			ExpandableComposite ec = tle.getToolkit().createSection(parent,
					ExpandableComposite.TWISTIE);
			ec.setFont(null); // match labels on the same level
			ec.setText(prop.getName());
			ec.clientVerticalSpacing = 0;
			for (Control c : ec.getChildren()) {
				new PropertyToolTip(prop, tle, c).activate();
			}

			Composite col2 = tle.getToolkit().createComposite(parent);
			GridLayout hdrLayout = new GridLayout(2, false);
			hdrLayout.marginWidth = hdrLayout.marginHeight = 0;
			hdrLayout.horizontalSpacing = 10;
			col2.setLayout(hdrLayout);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(col2);

			final Label lblStatus = tle.getToolkit().createLabel(col2, "", 0);
			lblStatus.setForeground(lblStatus.getDisplay().getSystemColor(SWT.COLOR_GRAY));
			editor.addEditorListener(new EditorListener() {
				@Override
				public void statusTextChanged(String statusText) {
					lblStatus.setText(statusText);
				}
			});

			Control toolBar = editor.getToolBar();
			if (toolBar != null) {
				toolBar.setParent(col2);
				GridDataFactory.swtDefaults().grab(true, false) //
						.align(SWT.FILL, SWT.CENTER).applyTo(lblStatus);
			} else {
				GridDataFactory.swtDefaults().grab(true, false) //
						.align(SWT.FILL, SWT.CENTER).span(2, 1).applyTo(lblStatus);
			}

			final Composite client = tle.getToolkit().createComposite(parent);
			GridDataFactory.fillDefaults().span(2, 1).grab(true, false)
					.exclude(true).applyTo(client);
			GridLayout clientLayout = new GridLayout(2, false);
			clientLayout.horizontalSpacing = 2;
			clientLayout.marginLeft = 40;
			clientLayout.marginHeight = 0;
			clientLayout.marginWidth = 0;
			client.setLayout(clientLayout);
			client.setVisible(false);

			ec.addExpansionListener(new IExpansionListener() {
				@Override
				public void expansionStateChanging(ExpansionEvent e) {
					// ignore
				}

				@Override
				public void expansionStateChanged(ExpansionEvent e) {
					boolean expanded = e.getState();
					client.setVisible(expanded);
					((GridData) client.getLayoutData()).exclude = !expanded;
					editor.reLayout();
				}
			});

			editor.setParent(client);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(editor);
		} else {
			new PropertyToolTip(prop, tle, lbl).activate();
			GridDataFactory.swtDefaults().indent(14, 0).applyTo(lbl);

			GridDataFactory.fillDefaults().grab(true, false).applyTo(editor);
		}
		return editor;
	}

	private static final EditorWidgetFactory instance = new EditorWidgetFactory();

	public static EditorWidgetFactory getInstance() {
		return instance;
	}

}
