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

import jasima_gui.dialogs.streamEditor.FormProperty;


public class BiggerThan extends SmallerThan {

	public BiggerThan(FormProperty p1, FormProperty p2, boolean strict) {
		super(p1, p2, strict);
	}

	public BiggerThan(FormProperty p1, Comparable<?> constant, boolean strict) {
		super(p1, constant, strict);
	}

	@Override
	protected boolean doComparison(Comparable<Object> n1, Comparable<Object> n2) {
		if (strict)
			return n1.compareTo(n2) > 0;
		else
			return n1.compareTo(n2) >= 0;
	}

	@Override
	public String getMessage() {
		return String.format("'%s' has to be larger than '%s'.", p1.labelText,
				p2 != null ? p2.labelText : constant);
	}

}
