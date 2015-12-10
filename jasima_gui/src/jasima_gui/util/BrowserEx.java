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

import java.util.ArrayList;

import org.eclipse.swt.SWTException;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

public class BrowserEx extends Browser implements ProgressListener {

	/**
	 * If the content is higher than {@link #MAXIMUM_HEIGHT}, the height of this
	 * control will be set to this value to prevent a scroll bar being shown
	 * when there's very little to scroll.
	 */
	public static final int OVERFLOW_HEIGHT = 200;

	/**
	 * The absolute maximum height of this control.
	 */
	public static final int MAXIMUM_HEIGHT = 250;

	protected ArrayList<Listener> sizeListeners = new ArrayList<>();

	public BrowserEx(Composite parent, int style) {
		super(parent, style);
		addProgressListener(this);
	}

	public void addSizeListener(Listener listener) {
		sizeListeners.add(listener);
	}

	public void removeSizeListener(Listener listener) {
		sizeListeners.remove(listener);
	}

	@Override
	protected void checkSubclass() {
		// we *are* subclassing Browser
	}

	@Override
	public Point computeSize(int wHint, int hHint) {
		// ignore hHint
		Point oldSize = getSize();
		if (wHint <= 0)
			wHint = 100;
		setSize(wHint, oldSize.y);
		final String getHeight = "var contentElement = document.getElementById('jasima-content');" //
				+ "return (null==contentElement ? " + Integer.MAX_VALUE + ":" //
				+ "contentElement.scrollHeight)";

		Double height;
		try {
			height = (Double) evaluate(getHeight);
		} catch (SWTException ignore) {
			ignore.printStackTrace();
			height = null;
		}

		if (height == null) {
			height = (double) Integer.MAX_VALUE;
		}

		int h = (int) Math.ceil(height.doubleValue());
		if (h > MAXIMUM_HEIGHT) {
			h = OVERFLOW_HEIGHT;
		}

		setSize(oldSize);
		return new Point(wHint, h);
	}

	@Override
	public void changed(ProgressEvent event) {
	}

	@Override
	public void completed(ProgressEvent event) {
		for (Listener lstnr : sizeListeners) {
			lstnr.handleEvent(new Event());
		}
	}

}
