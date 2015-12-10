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

public class PropertyException extends Exception {

	private static final long serialVersionUID = 4801173525092352477L;

	public PropertyException(String message) {
		super(message);
	}

	public PropertyException(String message, Throwable cause) {
		super(message, cause);
	}

	public static String formatException(Throwable t) {
		String message = String.valueOf(t.getLocalizedMessage()).trim();
		String fmt;
		if(message.isEmpty() || message.equals("null")) {
			fmt = "%s";
		} else {
			fmt = "%s: %s";
		}
		return String.format(fmt, t.getClass().getSimpleName(), message);
	}

	public static PropertyException newGetException(IProperty prop, Throwable cause) {
		return new PropertyException(String.format("Exception in getter: %s", formatException(cause)), cause);
	}

	public static PropertyException newSetException(IProperty prop, Throwable cause) {
		if (cause instanceof IllegalArgumentException) {
			return new PropertyException(String.format("Bad property value: %s", cause.getLocalizedMessage()), cause);
		}
		return new PropertyException(String.format("Exception in setter: %s", formatException(cause)), cause);
	}

}
