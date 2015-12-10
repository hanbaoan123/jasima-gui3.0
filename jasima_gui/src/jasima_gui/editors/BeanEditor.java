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
import jasima_gui.editor.EditorWidgetFactory;
import jasima_gui.editor.IProperty;
import jasima_gui.editor.PropertyException;
import jasima_gui.editor.PropertyLister;

import java.util.ArrayList;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class BeanEditor extends EditorWidget {

	private Composite toolBar;
	private Object object;
	private ArrayList<EditorWidget> editors = new ArrayList<EditorWidget>();

	public BeanEditor(Composite parent) {
		super(parent);
	}

	public void createControls() {
		GridLayout editorsLayout = new GridLayout(2, false);
		editorsLayout.marginWidth = 0;
		editorsLayout.marginHeight = 0;
		setLayout(editorsLayout);

		for (IProperty subProp : PropertyLister.getInstance().listProperties(
				property, topLevelEditor)) {
			final EditorWidget editor = EditorWidgetFactory.getInstance()
					.createEditorWidgetWithLabel(topLevelEditor, this, subProp,
							property);
			editors.add(editor);
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		for (EditorWidget editor : editors) {
			editor.setEnabled(enabled);
		}
	}

	@Override
	public boolean isExpandable() {
		return true;
	}

	@Override
	public Control getToolBar() {
		return toolBar;
	}

	@Override
	public void storeValue() {
		try {
			property.setValue(object);
			hideError();
		} catch (PropertyException e) {
			showError(e.getLocalizedMessage());
		}
	}

	@Override
	public void loadValue() {
		try {
			object = property.getValue();
			for (EditorWidget editor : editors) {
				editor.loadValue();
			}
		} catch (PropertyException e) {
			showError(e.getLocalizedMessage());
		}
	}
}
