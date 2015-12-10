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

import static jasima_gui.dialogs.streamEditor.util.StreamEditorUtil.TYPES_ALL;
import static jasima_gui.dialogs.streamEditor.util.StreamEditorUtil.createCompatibleStreamDefs;
import static jasima_gui.dialogs.streamEditor.util.StreamEditorUtil.createStreamDefFromStream;
import static jasima_gui.dialogs.streamEditor.util.StreamEditorUtil.createStreamFromStreamDef;
import jasima_gui.dialogs.streamEditor.DblStreamDialog;
import jasima_gui.dialogs.streamEditor.DetailsPageBase;
import jasima_gui.editor.EditorWidget;
import jasima_gui.editor.PropertyException;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class DblStreamEditor extends EditorWidget implements FocusListener {

	private Text text;
	private Button btnNull = null;
	private Button btnChange = null;
	private boolean dirty = false;

	public DblStreamEditor(Composite parent) {
		super(parent);
	}

	public void createControls() {
		GridLayout layout = new GridLayout(3, false);
		layout.marginHeight = layout.marginWidth = 0;
		setLayout(layout);

		text = toolkit.createText(this, "", property.isWritable() ? SWT.BORDER
				: 0);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(text);
		text.setEditable(property.isWritable());
		text.addFocusListener(this);
		// text.addModifyListener(this);
		text.setEnabled(false);

		if (property.isWritable()) {
			btnChange = toolkit.createButton(this, "change", SWT.PUSH);
			btnChange.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					try {
						Object val = property.getValue();
						selectNewStream(val);
					} catch (PropertyException e1) {
						throw new RuntimeException(e1);
					}
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// ignore
				}
			});
		}

		if (property.canBeNull() && property.isWritable()) {
			btnNull = toolkit.createButton(this, "clear", SWT.PUSH);
			btnNull.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// ignore
				}

				@Override
				public void widgetSelected(SelectionEvent e) {
					assert e.widget == btnNull;
					// property set to null
					try {
						property.setValue(null);
					} catch (PropertyException e1) {
						throw new RuntimeException(e1);
					}
					loadValue();
				}
			});
		}
	}

	protected void selectNewStream(Object oldStream) {
		// TODO: make type list extensible with extension point
		Object[] os = createCompatibleStreamDefs((Class<?>) property.getType(),
				TYPES_ALL, topLevelEditor.getClassLoader());
		assert os.length == 2;
		Object[] streamDefs = (Object[]) os[0];
		DetailsPageBase[] pages = (DetailsPageBase[]) os[1];
		assert streamDefs.length == pages.length;

		DblStreamDialog dialog = new DblStreamDialog(getShell(), topLevelEditor);
		dialog.create();
		dialog.getShell().setSize(500, 500);
		dialog.initContents(streamDefs, pages);

		Object currentStreamDef = null;
		if (oldStream != null) {
			currentStreamDef = createStreamDefFromStream(oldStream);
			assert currentStreamDef != null;
		}
		dialog.setStreamDef(currentStreamDef);

		try {
			int retVal = dialog.open();
			if (retVal == IDialogConstants.OK_ID) {
				// get streamDef from dialog
				Object newStreamDef = dialog.getStreamDef();
				assert newStreamDef != null;

				// convert to stream
				Object newStream = createStreamFromStreamDef(newStreamDef);
				assert newStreamDef != null;

				// update property
				property.setValue(newStream);
			}
		} catch (PropertyException ex) {
			showError(ex.getLocalizedMessage());
		} finally {
			dirty = true;
			loadValue();
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		text.setEnabled(enabled);
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
		loadValue(); // reformat numbers, for example
	}

	@Override
	public void loadValue() {
		try {
			Object val = property.getValue();

			String s;
			if (val != null) {
				s = String.valueOf(val);
			} else {
				s = "<none>";
			}
			text.setText(s);

			if (btnNull != null) {
				btnNull.setEnabled(val != null);
			}

			dirty = false;
		} catch (PropertyException e) {
			showError(e.getLocalizedMessage());
			text.setText("?");
		}
	}

	@Override
	public void storeValue() {
		throw new UnsupportedOperationException();
	}

}
