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
package jasima_gui.dialogs.streamEditor;

import jasima_gui.editor.TopLevelEditor;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;

public class DblStreamDialog extends FormDialog {

	private MasterBlock mdb;
	private TopLevelEditor editor;

	public DblStreamDialog(Shell shell, TopLevelEditor editor) {
		super(shell);
		setHelpAvailable(false);
		this.editor = editor;

	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		super.createFormContent(mform);

		mdb = new MasterBlock();
		mdb.createContent(mform);
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		Control c = super.createButtonBar(parent);
		mdb.okButton = getButton(IDialogConstants.OK_ID);
		return c;
	}

	@Override
	public boolean close() {
		mdb.getDetailsPart().selectionChanged(null, null);
		return super.close();
	}

	public void initContents(Object[] streamDefs, DetailsPageBase[] pages) {
		mdb.initContents(streamDefs, pages);
	}

	public Object getStreamDef() {
		return mdb.getStreamDef();
	}

	public void setStreamDef(Object streamDef) {
		mdb.setStreamDef(streamDef);
	}
}
