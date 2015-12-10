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

import org.eclipse.swt.widgets.Text;

public class IntegerProperty extends FormProperty {
	public IntegerProperty(DetailsPageBase owner, String propertyName,
			String labelText) {
		super(owner, propertyName, labelText);
	}

	@Override
	public Object parseFromGui() {
		String s = ((Text) widget).getText();
		try {
			// convert value
			Integer i = Integer.parseInt(s);
			return i;
		} catch (NumberFormatException ignore) {
			String msg = String.format("'%s' is not a valid integer.",
					s);
			String hover = "Please enter an integer number.";
			return new FormParseError(msg, hover, ignore);
		}
	}
}