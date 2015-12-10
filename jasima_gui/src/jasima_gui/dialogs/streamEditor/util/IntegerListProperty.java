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
package jasima_gui.dialogs.streamEditor.util;

import jasima_gui.dialogs.streamEditor.DetailsPageBase;
import jasima_gui.dialogs.streamEditor.FormProperty;
import jasima_gui.dialogs.streamEditor.DetailsPageBase.FormParseError;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class IntegerListProperty extends FormProperty {

	public static final String INT_LIST_HOVER = "Please enter a list of integers (with \n"
			+ "numbers separated by commas).\n" + "Example: 1,2,3";

	public IntegerListProperty(DetailsPageBase owner, String propertyName,
			String labelText) {
		super(owner, propertyName, labelText);
	}

	@Override
	public Object parseFromGui() {
		String s = ((Text) widget).getText();
		if (s == null)
			s = "";
		try {
			// convert value
			int[] d = StreamEditorUtil.parseIntList(s);
			if (d.length > 0)
				return d;
			else {
				String msg = String.format("'%s' can't be empty.", labelText);
				return new FormParseError(msg, INT_LIST_HOVER, null);
			}
		} catch (NumberFormatException ignore) {
			String msg = String.format("Number format error for: %s",
					ignore.getMessage());
			String hover = INT_LIST_HOVER;
			return new FormParseError(msg, hover, ignore);
		}
	}

	@Override
	public void updateGui() {
		int[] ds = (int[]) getValue();
		StringBuilder sb = new StringBuilder();
		if (ds != null)
			for (int d : ds) {
				sb.append(d).append(", ");
			}
		if (sb.length() > 0)
			sb.setLength(sb.length() - 2);
		((Text) widget).setText(sb.toString());
		decoration.hide();
	}

	@Override
	public void createEditControl(FormToolkit toolkit) {
		Text text = toolkit.createText(owner.getClient(), "", SWT.MULTI
				| SWT.WRAP);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalIndent = decoImg.getBounds().width;
		text.setLayoutData(gd);
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateModel();
			}
		});
		text.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_TAB_NEXT
						|| e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
					e.doit = true;
				}
			}
		});
		widget = text;
		createDecorator(widget);
	}
}