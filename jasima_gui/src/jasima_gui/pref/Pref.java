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
package jasima_gui.pref;

import jasima_gui.Activator;

import org.eclipse.jface.preference.IPreferenceStore;

public abstract class Pref {

	public static final BoolPref FIRST_RUN = new BoolPref("first-run-with-plugin", true);

	public static final StrPref EXP_RES_FMT = new StrPref("default-experiment-result-format", "--xlsres");

	public static final StrPref XLS_EXP_RES_FMT = new StrPref("default-excel-experiment-result-format", "--xlsres");

	public static final StrPref JASIMA_VERSION = new StrPref("default-jasima-version", "1.3.0");

	public final String key;

	Pref(String key) {
		this.key = key;
	}

	public static IPreferenceStore prefStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	public abstract void initDefault();
}
