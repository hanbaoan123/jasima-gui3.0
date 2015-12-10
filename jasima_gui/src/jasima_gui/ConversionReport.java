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

import jasima_gui.util.TypeUtil;

import java.util.EnumMap;
import java.util.Formatter;
import java.util.Map;
import java.util.TreeSet;

public class ConversionReport {
	public static final String HREF_CONFIRM = "jasima-command:confirm-conversion-report";
	protected EnumMap<ConversionReportCategory, TreeSet<String>> messages = new EnumMap<>(
			ConversionReportCategory.class);
	protected TreeSet<String> affectedClasses = new TreeSet<>();
	protected String result;

	public void finish() {
		@SuppressWarnings("resource")
		Formatter fmt = new Formatter();

		fmt.format("<form><p>Some changes to the referenced classes occured since this file was last edited.</p>"
				+ "<p><b>Affected classes</b></p>");
		for (String affectedClass : affectedClasses) {
			fmt.format("<li>%s</li>", affectedClass);
		}
		for (Map.Entry<ConversionReportCategory, TreeSet<String>> entry : messages.entrySet()) {
			fmt.format("<p><b>%s</b></p><p>%s</p>", entry.getKey().headline, entry.getKey().introText);
			for (String s : entry.getValue()) {
				fmt.format("<li>%s</li>", s);
			}
		}
		fmt.format("<p><a href='%s'>Confirm</a> and continue editing</p>", HREF_CONFIRM);
		fmt.format("</form>");
		result = fmt.toString();
	}

	public boolean isEmpty() {
		return messages.isEmpty();
	}

	public String toString() {
		return result;
	}

	protected void putMessage(ConversionReportCategory msgType, String format, Object... args) {
		TreeSet<String> val = messages.get(msgType);
		if (val == null) {
			messages.put(msgType, val = new TreeSet<String>());
		}
		val.add(String.format(format, args));
	}

	public void newProperty(Class<?> type, String propertyName) {
		String st = TypeUtil.toString(type, true);
		affectedClasses.add(type.getName());
		putMessage(ConversionReportCategory.NEW_PROPERTY, "<span color='light'>%s.</span>%s", st, propertyName);
	}

	public void propertyDisappeared(Class<?> type, String propertyName) {
		String st = TypeUtil.toString(type, true);
		affectedClasses.add(type.getName());
		putMessage(ConversionReportCategory.PROPERTY_DISAPPEARED, "<span color='light'>%s.</span>%s", st, propertyName);
	}

	public void propertyTypeChanged(Class<?> type, String propertyName, Class<?> needed, Class<?> actual) {
		String st = TypeUtil.toString(type, true);
		String sn = TypeUtil.toString(needed, true);
		String sa = TypeUtil.toString(actual, true);
		affectedClasses.add(type.getName());
		putMessage(ConversionReportCategory.TYPE_CHANGED, "<span color='light'>%s.</span>%s (%s → %s)", st,
				propertyName, sa, sn);
	}

	public void propertyRangeChanged(Class<?> type, String propertyName, String message) {
		String st = TypeUtil.toString(type, true);
		affectedClasses.add(type.getName());
		putMessage(ConversionReportCategory.ALLOWED_VALUES_CHANGED, "<span color='light'>%s.</span>%s (%s)", st,
				propertyName, message);
	}

	public void propertyTypeUnknown(Class<?> type, String propertyName, String propertyType) {
		String st = TypeUtil.toString(type, true);
		affectedClasses.add(propertyType);
		putMessage(ConversionReportCategory.TYPE_UNKNOWN, "<span color='light'>%s.</span>%s (%s)", st, propertyName,
				propertyType);
	}
}
