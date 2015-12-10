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
package jasima_gui;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.ui.forms.widgets.FormText;

public class ClassLoaderState {
	protected Set<String> dirtyClasses = new HashSet<>();
	protected boolean classPathChanged = false;

	public boolean isDirty() {
		return classPathChanged || !dirtyClasses.isEmpty();
	}

	public void markDirty(String klass, boolean dirty) {
		if (dirty) {
			dirtyClasses.add(klass);
		} else {
			dirtyClasses.remove(klass);
		}
	}

	public void setClassPathChanged(boolean value) {
		classPathChanged = value;
	}

	/**
	 * Generates a report of changes in {@link FormText} format.
	 * 
	 * @param builder
	 *            buffer to append the result to
	 */
	public void generateReport(StringBuilder builder) {
		if (isDirty()) {
			builder.append("<p>The following classes were changed:</p>");
			for (String s : dirtyClasses) {
				builder.append("<li><span font='monospace'>");
				builder.append(s);
				builder.append("</span></li>");
			}
		} else {
			// probably never displayed
			builder.append("<p>There class loader is in a consistent state.</p>");
		}
	}
}
