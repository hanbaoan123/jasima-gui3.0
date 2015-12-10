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

import jasima_gui.Activator;

import java.util.ArrayList;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public abstract class EditorWidget extends Composite {

	protected final static Image decoImg;
	static {
		FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(
				FieldDecorationRegistry.DEC_ERROR);
		decoImg = fieldDecoration.getImage();
	}

	public interface EditorListener {
		public void statusTextChanged(String statusText);
	}

	protected final ArrayList<EditorListener> editorListeners = new ArrayList<EditorWidget.EditorListener>();
	protected FormToolkit toolkit;
	protected IProperty property;
	protected TopLevelEditor topLevelEditor;
	protected String statusText = "";
	protected ControlDecoration decoration;

	public EditorWidget(Composite parent) {
		super(parent, 0);
		toolkit = new FormToolkit(getDisplay());
		toolkit.adapt(this);
		setLayout(new FillLayout());
		decoration = new ControlDecoration(this, SWT.LEFT);
		decoration.setImage(decoImg);
		decoration.hide();
	}

	public final void initialize(IProperty property, TopLevelEditor topLevelEditor) {
		this.property = property;
		this.topLevelEditor = topLevelEditor;
	}

	/**
	 * This method must be overridden and used to create the user interface of
	 * the editor. It is called after initialize.
	 */
	public abstract void createControls();

	public void addEditorListener(EditorListener listener) {
		editorListeners.add(listener);
		listener.statusTextChanged(statusText);
	}

	protected void setStatusText(String statusText) {
		assert statusText != null;
		assert isExpandable();
		this.statusText = statusText;
		for (EditorListener listener : editorListeners) {
			listener.statusTextChanged(statusText);
		}
	}

	/**
	 * Retrieve the value previously set by {@link #setStatusText(String)}.
	 */
	public final String getStatusText() {
		return statusText;
	}

	/**
	 * An expandable editor will be shown inside an {@link ExpandableComposite}.
	 * This should be used for big editors that can not fit into one line.
	 * 
	 * @return true if this widget must be shown inside an
	 *         {@link ExpandableComposite}
	 */
	public boolean isExpandable() {
		return false;
	}

	/**
	 * Can be used by an expandable editor to return any control that will be
	 * shown in the header.
	 * 
	 * @return any control or null
	 */
	public Control getToolBar() {
		return null;
	}

	@Override
	public void dispose() {
		toolkit.dispose();
		super.dispose();
	}

	/**
	 * Must be implemented to set the property's value to this widget's value.
	 * 
	 * The implementation should use IProperty.setValue.
	 */
	public abstract void storeValue();

	/**
	 * Must be implemented to set this widget's value to the property's value.
	 * 
	 * The implementation should use IProperty.getValue.
	 */
	public abstract void loadValue();

	protected void reLayout() {
		ScrolledForm sf = getScrolledForm();
		sf.getBody().layout(true, true);
		sf.reflow(true);
	}

	/**
	 * Looks for the innermost ScrolledForm parent.
	 * 
	 * @return the ScrolledForm this editor widget is contained in
	 */
	protected ScrolledForm getScrolledForm() {
		Composite comp = getParent();
		while (!(comp instanceof ScrolledForm)) {
			comp = comp.getParent();
			assert comp != null : "EditorWidget must always appear in a ScrolledForm.";
		}
		return (ScrolledForm) comp;
	}

	/**
	 * Shows a modal error dialog. This should only be used when the user's work
	 * flow has to be interrupted.
	 * 
	 * @param format
	 *            the format string
	 * @param args
	 *            arguments for the format string
	 */
	protected void showErrorDialog(String format, Object... args) {
		ErrorDialog.openError(getShell(), null, null,
				new Status(IStatus.ERROR, Activator.PLUGIN_ID, String.format(format, args)));
	}

	/**
	 * Shows an error without interrupting the user's work flow. This should be
	 * used for errors that can be ignored temporarily, like invalid property
	 * values.
	 * 
	 * @param message
	 *            the error message that should be shown
	 */
	protected void showError(String message) {
		if (message != null && (message = message.trim()).isEmpty())
			message = null;

		if (message == null) {
			decoration.hide();
		} else {
			decoration.setDescriptionText(message);
			decoration.show();
			decoration.showHoverText(message);
		}
	}

	/**
	 * Hides any error that is shown.
	 */
	protected void hideError() {
		showError(null);
	}

}
