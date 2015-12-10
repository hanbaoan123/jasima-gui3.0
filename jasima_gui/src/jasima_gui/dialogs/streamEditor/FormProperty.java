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
package jasima_gui.dialogs.streamEditor;

import jasima_gui.dialogs.streamEditor.DetailsPageBase.FormParseError;

import java.beans.PropertyDescriptor;
import java.util.Objects;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

public abstract class FormProperty {
	protected final static Image decoImg;
	static {
		FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR);
		decoImg = fieldDecoration.getImage();
	}

	public FormProperty(DetailsPageBase owner, String propertyName,
			String labelText) {
		super();
		this.owner = owner;
		this.propertyName = propertyName;
		this.labelText = labelText;
		this.widget = null;
		this.beanDescriptor = null;
		this.decoration = null;
	}

	public final DetailsPageBase owner;
	public final String propertyName;
	public final String labelText;

	private Object value;
	public PropertyDescriptor beanDescriptor;
	public FormParseError error;
	public boolean isDirty;
	public boolean isStale;

	protected Control widget;
	protected ControlDecoration decoration;

	public abstract Object parseFromGui();

	protected void updateGui() {
		// convert to string
		String s = String.valueOf(value);
		((Text) widget).setText(s);

		isStale = false;
		decoration.hide();
	}

	public void createWidget() {
		FormToolkit toolkit = owner.mForm.getToolkit();

		createLabel(toolkit);
		createEditControl(toolkit);
	}

	protected void createEditControl(FormToolkit toolkit) {
		// create text widget
		Text text = toolkit.createText(owner.getClient(), "", SWT.SINGLE);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalIndent = decoImg.getBounds().width;
		text.setLayoutData(gd);

		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateModel();
			}
		});
		widget = text;

		createDecorator(text);
	}

	protected void createDecorator(Control text) {
		// add the decorator
		decoration = new ControlDecoration(text, SWT.TOP | SWT.LEFT);
		decoration.setImage(decoImg);
		decoration.hide();
	}

	protected void createLabel(FormToolkit toolkit) {
		// create label
		toolkit.createLabel(owner.getClient(), labelText);
	}

	protected void updateModel() {
		if (owner.disableModifyEvent)
			return;

		Object newValue = parseFromGui();

		if (newValue instanceof FormParseError) {
			FormParseError e = (FormParseError) newValue;
			error = e;
		} else {
			error = null;
			if (!Objects.equals(value, newValue)) {
				value = newValue;
				isDirty = true;
			}
		}

		owner.checkGlobalConstraints();
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
		isStale = true;
	}

	public ControlDecoration getDecoration() {
		return decoration;
	}

}
