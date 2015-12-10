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
package jasima_gui.editors;

import jasima_gui.editor.EditorWidget;
import jasima_gui.editor.PropertyException;
import jasima_gui.util.TypeUtil;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

public class EnumEditor extends EditorWidget implements SelectionListener {

	private static final String NULL_STRING = "null";
	private Combo combo;

	public EnumEditor(Composite parent) {
		super(parent);
	}

	public void createControls() {
		combo = new Combo(this, SWT.READ_ONLY);
		toolkit.adapt(combo, true, true);
		combo.add(NULL_STRING);
		for (Object o : TypeUtil.toClass(property.getType()).getEnumConstants()) {
			combo.add(((Enum<?>) o).name());
		}
		combo.addSelectionListener(this);
	}

	@Override
	public void loadValue() {
		Enum<?> val = null;
		try {
			val = (Enum<?>) property.getValue();
		} catch (PropertyException e) {
			showError(e.getLocalizedMessage());
		}
		if (val == null) {
			combo.setText(NULL_STRING);
		} else {
			combo.setText(val.name());
		}
	}

	@Override
	public void storeValue() {
		String text = combo.getText();
		Object newVal;
		outer: if (text.equals(NULL_STRING)) {
			newVal = null;
		} else {
			for (Object o : TypeUtil.toClass(property.getType())
					.getEnumConstants()) {
				Enum<?> e = (Enum<?>) o;
				if (text.equals(e.name())) {
					newVal = e;
					break outer;
				}
			}
			throw new AssertionError(
					"EnumEditor's Combo has an impossible text");
		}
		// if (newVal != property.getValue()) {
		try {
			property.setValue(newVal);
			hideError();
		} catch (PropertyException e) {
			showError(e.getLocalizedMessage());
		}
		// }

	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		storeValue();
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		// ignore
	}
}
