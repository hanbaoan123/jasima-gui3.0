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

import java.lang.reflect.InvocationTargetException;

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
import org.eclipse.swt.widgets.Text;

public class TextEditor extends EditorWidget implements FocusListener, ModifyListener, SelectionListener {

	private Text text;
	private Button btnNull = null;
	private boolean hasError = false;
	private boolean modifyingSelf = false;

	public TextEditor(Composite parent) {
		super(parent);
	}

	public void createControls() {
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = layout.marginWidth = 0;
		setLayout(layout);

		text = toolkit.createText(this, "", property.isWritable() ? SWT.BORDER : 0);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(text);
		text.setEditable(property.isWritable());
		text.addFocusListener(this);
		text.addModifyListener(this);

		if (property.canBeNull() && property.isWritable()) {
			layout.numColumns = 2;
			btnNull = toolkit.createButton(this, "null", SWT.CHECK);
			btnNull.addSelectionListener(this);
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (btnNull != null) {
			btnNull.setEnabled(enabled && property.isWritable());
			enabled &= !btnNull.getSelection();
		}
		text.setEnabled(enabled);
	}

	@Override
	public void modifyText(ModifyEvent e) {
		if(!modifyingSelf) {
			storeValue();
		}
	}

	@Override
	public void focusGained(FocusEvent e) {
		// ignore
	}

	@Override
	public void focusLost(FocusEvent e) {
		if (!property.isWritable())
			return;
		if (hasError) {
			hideError();
			loadValue(); // reformat numbers, for example
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
		text.setEnabled(!nullSelected);
		text.setText(nullSelected ? String.valueOf((Object) null) : "");
		storeValue();
		if (!nullSelected) {
			text.setFocus();
		}
	}

	@Override
	public void loadValue() {
		try {
			Object val = property.getValue();
			// val might be null if IProperty.canBeNull returned false...
			modifyingSelf = true;
			text.setText(String.valueOf(val));
			modifyingSelf = false;
			if (btnNull != null) {
				text.setEnabled(val != null);
				btnNull.setSelection(val == null);
			}
		} catch (PropertyException e) {
			showError(e.getLocalizedMessage());
			modifyingSelf = true;
			text.setText("?");
			modifyingSelf = false;
		} finally {
			hasError = false;
		}
	}

	@Override
	public void storeValue() {
		hasError = true;
		try {
			if (btnNull != null && btnNull.getSelection()) {
				property.setValue(null);
				hasError = false;
				hideError();
				return;
			}
			Object val = null;
			Class<?> type = TypeUtil.toClass(property.getType());
			if (type.isPrimitive()) {
				type = TypeUtil.getPrimitiveWrapper(type);
			}
			if (type == Number.class) {
				val = Double.valueOf(text.getText());
			} else if (Number.class.isAssignableFrom(type)) {
				try {
					val = type.getConstructor(String.class).newInstance(text.getText());
				} catch (InvocationTargetException ex) {
					if (ex.getCause() instanceof NumberFormatException) {
						showError("Input has to be numeric.");
					} else {
						showError(ex.getCause().getLocalizedMessage());
					}
					return;
				}
			} else if (type == String.class) {
				val = text.getText();
			} else if (type == Character.class) {
				if (text.getText().length() > 0) {
					val = text.getText().charAt(0);
				}
			}
			if (val != null) {
				property.setValue(val);
			}
			hasError = false;
			hideError();
		} catch (PropertyException ex) {
			showError(ex.getLocalizedMessage());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
