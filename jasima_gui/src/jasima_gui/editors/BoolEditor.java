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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public class BoolEditor extends EditorWidget implements SelectionListener {

	private Boolean val;
	private Button chkFalse;
	private Button chkTrue;
	private Button chkNull = null;

	public BoolEditor(Composite parent) {
		super(parent);
	}

	public void createControls() {
		GridLayout layout = new GridLayout(3, false);
		layout.marginHeight = layout.marginWidth = 0;
		setLayout(layout);

		chkTrue = toolkit.createButton(this, "true", SWT.RADIO);
		chkTrue.setEnabled(property.isWritable());
		chkTrue.addSelectionListener(this);

		chkFalse = toolkit.createButton(this, "false", SWT.RADIO);
		chkFalse.setEnabled(property.isWritable());
		chkFalse.addSelectionListener(this);

		if (property.canBeNull() && property.isWritable()) {
			chkNull = toolkit.createButton(this, "null", SWT.RADIO);
			chkNull.addSelectionListener(this);
		}
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		// ignore
	}

	@Override
	public void widgetSelected(SelectionEvent evt) {
		storeValue();
	}

	@Override
	public void setEnabled(boolean enabled) {
		chkFalse.setEnabled(enabled && property.isWritable());
	}

	@Override
	public void loadValue() {
		try {
			val = (Boolean) property.getValue();
			if (val == null) {
				chkNull.setSelection(true);
			} else if (val.booleanValue()) {
				chkTrue.setSelection(true);
			} else {
				chkFalse.setSelection(true);
			}
		} catch (PropertyException e) {
			showError(e.getLocalizedMessage());
			chkFalse.setSelection(false);
			chkTrue.setSelection(false);
			chkNull.setSelection(false);
		}
	}

	@Override
	public void storeValue() {
		try {
			Boolean newVal = (chkNull != null && chkNull.getSelection()) ? null
					: chkTrue.getSelection();
			if (val == null) {
				if (newVal == null)
					return;
			} else if (val.equals(newVal)) {
				return;
			}
			property.setValue(newVal);
			val = newVal;
			hideError();
		} catch (PropertyException e) {
			showError(e.getLocalizedMessage());
			loadValue();
		}
	}
}
