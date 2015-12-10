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
import jasima_gui.util.DiscardingListener;
import jasima_gui.util.TypeUtil;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;

public class IntEditor extends EditorWidget implements FocusListener, ModifyListener, SelectionListener {

	private Button btnNull = null;
	private Spinner spinner;
	private boolean modifying = false;
	private boolean dirty = false;
	private boolean enabled = true;

	public IntEditor(Composite parent) {
		super(parent);
	}

	public void createControls() {
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = layout.marginWidth = 0;
		setLayout(layout);

		spinner = new Spinner(this, SWT.BORDER);
		if (SWT.getPlatform().equals("gtk")) {
			// by default, GTK's spinner uses the mouse wheel to change value,
			// which interacts badly with ScrolledForm's scrolling
			spinner.addListener(SWT.MouseVerticalWheel, new DiscardingListener());
		}
		GridDataFactory.fillDefaults().grab(true, false).applyTo(spinner);
		spinner.setValues(0, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 1, 50);
		toolkit.adapt(spinner, true, true);
		spinner.addFocusListener(this);
		spinner.addModifyListener(this);
		if (property.canBeNull()) {
			layout.numColumns = 2;
			btnNull = toolkit.createButton(this, "null", SWT.CHECK);
			btnNull.addSelectionListener(this);
		}
	}

	@Override
	public void modifyText(ModifyEvent e) {
		if (modifying)
			return;
		storeValue();
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		updateEnabled();
	}

	protected void updateEnabled() {
		boolean writable = property.isWritable();
		boolean isNull = false;
		if (btnNull != null) {
			btnNull.setEnabled(enabled && writable);
			isNull = btnNull.getSelection();
		}
		spinner.setEnabled(enabled && writable && !isNull);
	}

	@Override
	public void focusGained(FocusEvent e) {
		// ignore
	}

	@Override
	public void focusLost(FocusEvent e) {
		if (!property.isWritable())
			return;
		if (!dirty)
			return;
		try {
			storeValue();
		} finally {
			loadValue();
		}
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		// ignore
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		assert e.widget == btnNull;
		boolean nullSelected = btnNull.getSelection();
		updateEnabled();
		spinner.setSelection(0);
		if (!nullSelected) {
			dirty = true;
			spinner.setFocus();
		} else {
			storeValue();
		}
	}

	@Override
	public void loadValue() {
		modifying = true;
		Number num = null;
		try {
			num = (Number) property.getValue();
		} catch (PropertyException e) {
			showError(e.getLocalizedMessage());
			// pretend the value is null
		}
		spinner.setSelection(num == null ? 0 : num.intValue());
		if (btnNull != null) {
			btnNull.setSelection(num == null);
		}
		updateEnabled();
		dirty = false;
		modifying = false;
	}

	@Override
	public void storeValue() {
		try {
			Object val;
			if (btnNull != null && btnNull.getSelection()) {
				val = null;
			} else {
				Class<?> type = TypeUtil.toClass(property.getType());
				if (type.isPrimitive()) {
					type = TypeUtil.getPrimitiveWrapper(type);
				}
				val = type.getConstructor(String.class).newInstance(String.valueOf(spinner.getSelection()));
			}
			property.setValue(val);
			dirty = false;
			hideError();
		} catch (Exception ex) {
			showError(ex.getLocalizedMessage());
		}
	}
}
