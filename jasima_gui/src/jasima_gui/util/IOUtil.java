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
package jasima_gui.util;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;

public class IOUtil {

	private IOUtil() {
		// never called
	}

	/**
	 * Reads from <code>inputStream</code> until either an error occurs or EOF
	 * is reached. <code>inputStream</code> is closed in either case.
	 * 
	 * @param inputStream
	 *            the stream to read from
	 * @return all data read from <code>is</code>, or <code>null</code> if an
	 *         error occurred
	 */
	public static byte[] readFully(InputStream inputStream) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream(inputStream.available());
			copyFully(inputStream, bos);
			return bos.toByteArray();
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Reads from <code>inputStream</code> and writes to
	 * <code>outputStream</code> until either an error occurs or EOF is reached.
	 * <code>inputStream</code> is closed in either case, while
	 * <code>outputStream</code> is never closed.
	 * 
	 * @param inputStream
	 *            the stream to read from
	 * @param outputStream
	 *            the stream to write to
	 * @throws IOException
	 *             if an error occurred
	 */
	public static void copyFully(InputStream inputStream, OutputStream outputStream) throws IOException {
		final int BUFFER_SIZE = 65000;
		try {
			byte[] buffer = new byte[BUFFER_SIZE];
			int r;
			while ((r = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, r);
			}
		} finally {
			inputStream.close();
		}
	}

	public static String readFully(Reader reader) {
		final int BUFFER_SIZE = 32000;
		if (reader == null)
			return null;

		try (Reader rdr = reader) {
			StringBuffer buf = new StringBuffer();
			char[] buffer = new char[BUFFER_SIZE];
			int r;
			while ((r = rdr.read(buffer)) != -1) {
				buf.append(buffer, 0, r);
			}
			return buf.toString();
		} catch (IOException e) {
			return null;
		}
	}

	public static void tryClose(Closeable c) {
		try {
			c.close();
		} catch (IOException e) {
			// ignore
		}
	}

}
