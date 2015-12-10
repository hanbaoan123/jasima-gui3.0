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

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class DeserializationFailure {
	protected FormText label;

	public DeserializationFailure(Composite parent) {
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		Composite comp = toolkit.createComposite(parent);

		GridLayout grid = new GridLayout(2, false);
		grid.marginTop = 10;
		comp.setLayout(grid);

		Label icon = toolkit.createLabel(comp, null);
		icon.setImage(icon.getDisplay().getSystemImage(SWT.ERROR));

		label = toolkit.createFormText(comp, false);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(label);
		label.setFont(JFaceResources.getTextFont());
		label.setWhitespaceNormalized(false);
	}

	public void setException(Throwable exception) {
		String type = exception.getClass().getSimpleName();
		String details = String.valueOf(exception.getLocalizedMessage()).replaceFirst("^ *: *", "");
		String message = String.format("Error reading input: %s: %s", type, details);
		label.setText(message, false, false);
	}

}
