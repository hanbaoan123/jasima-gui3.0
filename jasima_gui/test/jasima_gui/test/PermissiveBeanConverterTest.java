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
package jasima_gui.test;

import jasima_gui.ConversionReport;
import jasima_gui.PermissiveBeanConverter;
import jasima_gui.util.IOUtil;

import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import junit.framework.TestCase;

import org.junit.Test;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class PermissiveBeanConverterTest extends TestCase {

	protected XStream xStream;
	protected PermissiveBeanConverter converter;

	@Override
	protected void setUp() throws Exception {
		xStream = new XStream(new DomDriver());
		converter = new PermissiveBeanConverter(xStream.getMapper());
		xStream.registerConverter(converter, -10);
	}

	protected String readFile(String name) {
		try {
			return IOUtil.readFully(new InputStreamReader(PermissiveBeanConverterTest.class.getResourceAsStream(name),
					"utf-8"));
		} catch(UnsupportedEncodingException e) {
			throw new Error(e);
		}
	}

	@Test
	public void testEmptyReport1() throws Exception {
		testEmptyReport("pbc_clean_input_1.xml");
	}

	@Test
	public void testEmptyReport2() throws Exception {
		testEmptyReport("pbc_clean_input_2.xml");
	}

	public void testEmptyReport(String name) throws Exception {
		String input = readFile(name);

		converter.startConversionReport();
		xStream.fromXML(input); // ignore result
		ConversionReport report = converter.finishConversionReport();
		assertNull(report);
	}

	@Test
	public void testReports() throws Exception {
		for (int i = 1; i <= 4; ++i) {
			String input = readFile("pbc_input_" + i + ".xml");
			String expectedReport = readFile("pbc_report_" + i + ".xml");

			converter.startConversionReport();
			xStream.fromXML(input); // ignore result
			ConversionReport report = converter.finishConversionReport();
			assertEquals(expectedReport, report.toString());
		}
	}

}
