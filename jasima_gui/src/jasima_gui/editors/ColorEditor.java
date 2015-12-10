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
package jasima_gui.editors;

import jasima_gui.editor.EditorWidget;
import jasima_gui.editor.PropertyException;
import jasima_gui.util.TypeUtil;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;

public class ColorEditor extends EditorWidget implements MouseListener,
		DisposeListener, PaintListener, SelectionListener {

	private Canvas colorPreview;
	private Button btnNull = null;
	private Color color = null;

	public ColorEditor(Composite parent) {
		super(parent);
		addDisposeListener(this);
	}

	@Override
	public void widgetDisposed(DisposeEvent e) {
		if (color != null)
			color.dispose();
	}

	@Override
	public void paintControl(PaintEvent e) {
		assert e.widget == colorPreview;
		GC gc = e.gc;
		Point size = colorPreview.getSize();
		gc.setBackground(ColorEditor.this.getBackground());
		gc.fillRectangle(0, 0, size.x, size.y);
		if (color == null) {
			for (int i = -size.x; i < size.x; i += 4) {
				gc.drawLine(i, size.y, i + size.x, 0);
			}
		} else {
			gc.setBackground(color);
			gc.fillRoundRectangle(0, 0, size.x, size.y, 16, 16);
		}
	}

	@Override
	public void createControls() {
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		setLayout(layout);

		colorPreview = new Canvas(this, 0);
		GridDataFactory.swtDefaults().hint(24, 24).applyTo(colorPreview);
		colorPreview.addMouseListener(this);
		colorPreview.addPaintListener(this);

		if (property.canBeNull()) {
			btnNull = toolkit.createButton(this, "Delete", SWT.PUSH);
			btnNull.addSelectionListener(this);
		}
	}

	@Override
	public void storeValue() {
		try {
			Class<?> type = TypeUtil.toClass(property.getType());
			RGB rgb = color.getRGB();
			if (type.isAssignableFrom(RGB.class)) {
				property.setValue(rgb);
			} else if (type.isAssignableFrom(java.awt.Color.class)) {
				property.setValue(new java.awt.Color(rgb.red, rgb.green,
						rgb.blue));
			} else if (type.isAssignableFrom(Integer.class)) {
				property.setValue((rgb.red << 020) | (rgb.green << 010)
						| rgb.blue);
			} else if (type.isAssignableFrom(String.class)) {
				property.setValue(String.format("#%02X02X02X", rgb.red,
						rgb.green, rgb.blue));
			}
			hideError();
		} catch (PropertyException e) {
			showError(e.getLocalizedMessage());
		}
	}

	@Override
	public void loadValue() {
		clearColor();
		try {
			Object clr = property.getValue();
			if (clr == null)
				return;
			if (clr instanceof RGB) {
				setColor((RGB) clr);
			} else if (clr instanceof java.awt.Color) {
				java.awt.Color awtColor = (java.awt.Color) clr;
				setColor(new RGB(awtColor.getRed(), awtColor.getGreen(),
						awtColor.getBlue()));
			} else if (clr instanceof Integer) {
				int rgb = (Integer) clr;
				setColor(new RGB(0xFF & (rgb >> 020), 0xFF & (rgb >> 010),
						0xFF & rgb));
			} else if (clr instanceof String) {
				java.awt.Color awtColor = java.awt.Color.decode((String) clr);
				setColor(new RGB(awtColor.getRed(), awtColor.getGreen(),
						awtColor.getBlue()));
			} else {
				assert false;
			}
		} catch (PropertyException e) {
			showError(e.getLocalizedMessage());
		}
	}

	protected void clearColor() {
		if (color != null) {
			color.dispose();
			color = null;
		}
		btnNull.setEnabled(false);
		colorPreview.redraw();
	}

	protected void setColor(RGB rgb) {
		assert color == null;
		color = new Color(getDisplay(), rgb);
		colorPreview.redraw();
		btnNull.setEnabled(true);
	}

	@Override
	public void mouseDoubleClick(MouseEvent e) {
		// ignore
	}

	@Override
	public void mouseDown(MouseEvent e) {
		// ignore
	}

	@Override
	public void mouseUp(MouseEvent e) {
		ColorDialog dlg = new ColorDialog(getShell());
		if (color != null) {
			dlg.setRGB(color.getRGB());
		}
		RGB rgb = dlg.open();
		if (rgb == null)
			return;
		clearColor();
		setColor(rgb);
		storeValue();
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		if (e.getSource() == btnNull) {
			clearColor();
			storeValue();
		}
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		// ignore
	}
}
