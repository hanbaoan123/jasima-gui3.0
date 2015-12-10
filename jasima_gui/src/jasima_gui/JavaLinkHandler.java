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

import java.net.URL;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLinks;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

@SuppressWarnings("restriction")
public class JavaLinkHandler implements JavaElementLinks.ILinkHandler {

	public static void openURL(URL url) {
		try {
			if (url.getProtocol().equals("jar")) {
				// convert jar: URLs to http://127.0.0.1:.../help/nftopic/... so
				// the browser can read them
				url = PlatformUI.getWorkbench().getHelpSystem().resolve(url.toExternalForm(), true);
				if (url == null)
					throw new RuntimeException("help UI unavailable");
			}

			PlatformUI.getWorkbench().getBrowserSupport().createBrowser(null).openURL(url);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void openJavadoc(IJavaElement target) {
		try {
			URL location = JavaUI.getJavadocLocation(target, true);
			if (location != null) {
				openURL(location);
			} else {
				openInEditor(target);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void openInEditor(IJavaElement target) {
		try {
			JavaUI.openInEditor(target);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void linkOpened() {
		// ignore, override to do things like closing popups
	}

	public void handleTextSet() {
		// ignore
	}

	public void handleJavadocViewLink(IJavaElement target) {
		linkOpened();
		openJavadoc(target);
	}

	public void handleInlineJavadocLink(IJavaElement target) {
		handleJavadocViewLink(target);
	}

	public boolean handleExternalLink(URL url, Display display) {
		linkOpened();
		openURL(url);
		return true;
	}

	public void handleDeclarationLink(IJavaElement target) {
		handleJavadocViewLink(target);
	}
}
