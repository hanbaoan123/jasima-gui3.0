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

import jasima_gui.util.IOUtil;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class Serialization {
	protected IJavaProject project;
	protected XStream xStream, xStreamWithoutPBC;
	protected EclipseProjectClassLoader epcl;
	protected PermissiveBeanConverter converter;

	public Serialization(IProject project) {
		this(JavaCore.create(project));
	}

	public Serialization(IJavaProject project) {
		this.project = project;
		epcl = new EclipseProjectClassLoader(project);
		xStream = new XStream(new DomDriver());
		converter = new PermissiveBeanConverter(xStream.getMapper());
		xStream.registerConverter(converter, -10);
		xStream.setClassLoader(epcl);
	}

	public EclipseProjectClassLoader getClassLoader() {
		return epcl;
	}

	public IJavaProject getProject() {
		return project;
	}

	public byte[] serialize(Object o) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream(4096);
			OutputStreamWriter wrtr = new OutputStreamWriter(bos, "UTF-8");
			wrtr.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<?jasima bean?>\n");
			xStream.toXML(o, wrtr);
			wrtr.close();
			byte[] retVal = bos.toByteArray();
			return retVal;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Object deserialize(InputStream is) {
		if (!is.markSupported()) {
			is = new BufferedInputStream(is);
		}

		try {
			byte[] header = new byte[1024];
			is.mark(header.length);
			int offset = 0;
			int ret;
			while ((ret = is.read(header, offset, header.length - offset)) > 0)
				offset += ret;
			is.reset();

			return deserialize(new ByteArrayInputStream(header), is);
		} catch (IOException e) {
			IOUtil.tryClose(is);
			throw new RuntimeException(e);
		}
	}

	public Object deserialize(InputStream header, InputStream complete) {
		try (BufferedReader rdr = new BufferedReader(new InputStreamReader(header, "UTF-8"))) {
			boolean useBeanConverter = false;
			String line;
			while ((line = rdr.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("<?xml ")) {
					// ignore
				} else if (line.startsWith("<?jasima bean")) {
					useBeanConverter = true;
				} else {
					break; // we know enough
				}
			}

			XStream converter;
			if (useBeanConverter) {
				converter = xStream;
			} else {
				if (xStreamWithoutPBC == null) {
					xStreamWithoutPBC = new XStream(new DomDriver());
					xStreamWithoutPBC.setClassLoader(epcl);
				}
				converter = xStreamWithoutPBC;
			}
			try (InputStreamReader rdr2 = new InputStreamReader(complete, "UTF-8")) {
				return converter.fromXML(rdr2);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void startConversionReport() {
		converter.startConversionReport();
	}

	public ConversionReport finishConversionReport() {
		return converter.finishConversionReport();
	}

	public String convertToString(Object o) {
		return xStream.toXML(o);
	}

	public Object convertFromString(String str) {
		return xStream.fromXML(str);
	}
}
