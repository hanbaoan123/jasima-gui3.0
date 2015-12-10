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

import jasima_gui.ConversionReport;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class ConversionReportView {

	protected FormText message;

	public ConversionReportView(Composite parent, final TopLevelEditor editor) {
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		message = toolkit.createFormText(parent, true);
		message.setFont("code", JFaceResources.getTextFont());
		message.setColor("light", parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
		message.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				if(e.getHref().toString().equals(ConversionReport.HREF_CONFIRM)) {
					editor.confirmConversionReport();
				}
			}
		});
	}

	public void setReport(ConversionReport report) {
		message.setText(report.toString(), true, false);
	}
}
