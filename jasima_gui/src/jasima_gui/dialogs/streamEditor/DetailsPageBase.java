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

import jasima_gui.dialogs.streamEditor.util.ConstraintValidator;
import jasima_gui.dialogs.streamEditor.util.DoubleListProperty;
import jasima_gui.dialogs.streamEditor.util.DoubleProperty;
import jasima_gui.dialogs.streamEditor.util.IntegerListProperty;
import jasima_gui.dialogs.streamEditor.util.IntegerProperty;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.IMessage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public abstract class DetailsPageBase implements IDetailsPage {

	public static class FormParseError {
		public FormParseError(String errorMsg, String hoverText,
				Exception reason) {
			super();
			this.errorMsg = errorMsg;
			this.hoverText = hoverText;
			this.reason = reason;
		}

		public final String errorMsg;
		public final String hoverText;
		public final Exception reason;
	}

	private HashMap<String, FormProperty> props;

	private MasterBlock master;
	protected IManagedForm mForm;
	protected FormToolkit toolkit;
	private Composite client;

	protected Object input;
	private boolean isStale;

	private ConstraintValidator[] constraints = new ConstraintValidator[] {};

	boolean disableModifyEvent = false;

	protected Section section;

	public DetailsPageBase() {
		super();
		props = new LinkedHashMap<String, FormProperty>();
	}

	public FormProperty addDoubleProperty(String beanPropertyName,
			String labelText) {
		return addProperty(new DoubleProperty(this, beanPropertyName, labelText));
	}

	public FormProperty addDoubleListProperty(String beanPropertyName,
			String labelText) {
		return addProperty(new DoubleListProperty(this, beanPropertyName,
				labelText));
	}

	public FormProperty addIntegerListProperty(String beanPropertyName,
			String labelText) {
		return addProperty(new IntegerListProperty(this, beanPropertyName,
				labelText));
	}

	public FormProperty addIntegerProperty(String beanPropertyName,
			String labelText) {
		return addProperty(new IntegerProperty(this, beanPropertyName,
				labelText));
	}

	public FormProperty addProperty(FormProperty p) {
		props.put(p.propertyName, p);
		return p;
	}

	public FormProperty getProperty(String propName) {
		return props.get(propName);
	}

	@Override
	public void initialize(IManagedForm form) {
		this.mForm = form;
		this.toolkit = mForm.getToolkit();

		isStale = true;
	}

	@Override
	public void selectionChanged(IFormPart part, ISelection selection) {
		IStructuredSelection ssel = (IStructuredSelection) selection;
		if (ssel.size() == 1) {
			setFormInput(ssel.getFirstElement());
			assert getInputType().equals(input.getClass().getName());
		} else
			setFormInput(null);
	}

	@Override
	public void createContents(Composite parent) {
		parent.setLayout(new FillLayout());

		section = toolkit.createSection(parent, Section.DESCRIPTION
				| Section.TITLE_BAR);
		section.setText(getTitle());
		section.setDescription(getDescription());
		toolkit.createSeparator(section, SWT.NULL);

		client = toolkit.createComposite(section, SWT.WRAP);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = layout.marginHeight = 2;
		client.setLayout(layout);

		for (FormProperty p : props.values()) {
			p.createWidget();
		}

		section.setClient(client);
		toolkit.paintBordersFor(client);
	}

	protected abstract String getDescription();

	protected abstract String getTitle();

	public abstract String getInputType();

	private void checkPropDescriptor(PropertyDescriptor pd) {
		FormProperty p = props.get(pd.getName());
		if (p != null) {
			p.beanDescriptor = pd;
		}
	}

	@Override
	public boolean setFormInput(Object input) {
		if (this.input != input) {
			this.input = input;
			isStale = true;

			// find getter/setter methods for each property
			BeanInfo bi;
			try {
				bi = Introspector.getBeanInfo(input.getClass());
				PropertyDescriptor[] propDesc = bi.getPropertyDescriptors();
				for (PropertyDescriptor pd : propDesc) {
					checkPropDescriptor(pd);
				}
			} catch (IntrospectionException e) {
				throw new RuntimeException(e);
			}
		}

		// refresh gui
		disableModifyEvent = true;
		try {
			refresh();
		} finally {
			disableModifyEvent = false;

			updateModel();
		}

		return false;
	}

	protected void updateModel() {
		// force validating constraints
		for (FormProperty p : props.values()) {
			p.updateModel();
		}
	}

	@Override
	public void dispose() {
	}

	@Override
	public boolean isDirty() {
		boolean isDirty = false;
		for (FormProperty p : props.values()) {
			if (p.isDirty) {
				isDirty = true;
				break;
			}
		}

		return isDirty;
	}

	@Override
	public void commit(boolean onSave) {
		for (FormProperty p : props.values()) {
			if (p.error == null && p.isDirty) {
				// call setter
				try {
					p.beanDescriptor.getWriteMethod().invoke(input,
							p.getValue());
					p.isDirty = false;
				} catch (Exception shouldNotOccur) {
					throw new RuntimeException(shouldNotOccur);
				}
			}
		}
	}

	@Override
	public void setFocus() {
	}

	@Override
	public boolean isStale() {
		return isStale;
	}

	@Override
	public void refresh() {
		isStale = false;
		if (input == null)
			return;

		for (FormProperty p : props.values()) {
			try {
				// get current value
				Object v = p.beanDescriptor.getReadMethod().invoke(input);
				p.setValue(v);

				p.updateGui();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	public boolean checkGlobalConstraints() {
		hideError();

		// any local error?
		boolean anyError = false;
		for (FormProperty p : props.values()) {
			ControlDecoration deco = p.getDecoration();
			if (p.error != null) {
				anyError = true;
				deco.setDescriptionText(p.error.errorMsg);
				deco.show();
				deco.showHoverText(p.error.hoverText);
			} else {
				deco.setDescriptionText(null);
				deco.hide();
			}
		}

		if (!anyError) {
			// check global constraints
			ArrayList<ConstraintValidator> failed = new ArrayList<ConstraintValidator>();
			for (ConstraintValidator v : getConstraints()) {
				if (!v.isValid())
					failed.add(v);
			}

			// report errors
			if (failed.size() > 0) {
				anyError = true;
				String mainMsg = failed.size() == 1 ? "1 error" : String
						.format("%d errors", failed.size());
				master.getManagedForm()
						.getForm()
						.setMessage(mainMsg, IMessageProvider.ERROR,
								failed.toArray(new IMessage[failed.size()]));

				// show error decoration on causing properties
				for (ConstraintValidator cv : failed) {
					for (FormProperty p : cv.dependsOn()) {
						ControlDecoration deco = p.getDecoration();
						String s = deco.getDescriptionText();
						if (s != null)
							s += "\n" + cv.getMessage();
						else
							s = cv.getMessage();
						deco.setDescriptionText(s);
						deco.show();
					}
				}
			} else {
				anyError = false;
			}
		}

		master.okButton.setEnabled(!anyError);

		return !anyError;
	}

	private void hideError() {
		master.getManagedForm().getForm()
				.setMessage(null, IMessageProvider.NONE);
	}

	public MasterBlock getMaster() {
		return master;
	}

	public void setMaster(MasterBlock master) {
		this.master = master;
	}

	public Composite getClient() {
		return client;
	}

	public ConstraintValidator[] getConstraints() {
		return constraints;
	}

	public void setConstraints(ConstraintValidator[] constraints) {
		this.constraints = constraints;
	}

	public void addConstraint(ConstraintValidator v) {
		ArrayList<ConstraintValidator> l = new ArrayList<ConstraintValidator>();
		if (constraints != null) {
			l.addAll(Arrays.asList(constraints));
		}
		l.add(v);
		constraints = l.toArray(new ConstraintValidator[l.size()]);
	}

}
