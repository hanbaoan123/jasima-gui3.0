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

import java.util.HashMap;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.DetailsPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.MasterDetailsBlock;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

public class MasterBlock extends MasterDetailsBlock {

	private Object streamDef;
	public Button okButton;
	private IManagedForm mform;
	private TableViewer viewer;
	private HashMap<String, DetailsPageBase> detailPages;

	public MasterBlock() {
		super();
		detailPages = new HashMap<String, DetailsPageBase>();
	}

	@Override
	protected void applyLayout(Composite parent) {
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 5;
		parent.setLayout(layout);
	}

	@Override
	protected void createMasterPart(IManagedForm mform, Composite parent) {
		this.mform = mform;
		FormToolkit toolkit = mform.getToolkit();
		ScrolledForm form = mform.getForm();

		form.setText("Define number streams");

		// Creating the Screen
		final Section section = toolkit
				.createSection(parent, Section.TITLE_BAR);
		section.setText("Available stream types");

		final SectionPart sectionPart = new SectionPart(section);

		Table t = toolkit.createTable(section, SWT.BORDER);
		section.setClient(t);
		TableLayout tableLayout = new TableLayout();
		tableLayout.addColumnData(new ColumnPixelData(200));
		t.setLayout(tableLayout);
		
		viewer = new TableViewer(t);
		TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
		viewerColumn.getColumn().setWidth(200);

		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				DetailsPageBase page = detailPages.get(element.getClass()
						.getName());
				if (page != null)
					return page.getTitle();
				else
					return element.toString();
			};

			public Image getImage(Object element) {
				return null;
			};
		});

		viewer.setContentProvider(new ArrayContentProvider());
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				detailsPart.selectionChanged(sectionPart, event.getSelection());
				streamDef = ((StructuredSelection) event.getSelection())
						.getFirstElement();
			}
		});
	}

	@Override
	protected void registerPages(DetailsPart detailsPart) {
		// done later in initContents()
	}

	@Override
	protected void createToolBarActions(IManagedForm managedForm) {

	}

	public Object getStreamDef() {
		return streamDef;
	}

	public void setStreamDef(Object streamDef) {
		this.streamDef = streamDef;

		// find and select current streamDef, replace placeholder with actual
		// data object
		int selection = 0;
		if (streamDef != null) {
			Object[] l = (Object[]) viewer.getInput();
			assert l != null;

			for (int i = 0; i < l.length; i++) {
				Object o = l[i];
				if (streamDef.getClass().getName() == o.getClass().getName()) {
					l[selection] = streamDef;
					break;
				}
				selection++;
			}
			if (selection >= l.length)
				selection = 0;

			viewer.setInput(l);
		}

		Object elementAt = viewer.getElementAt(selection);
		viewer.setSelection(new StructuredSelection(elementAt), true);
	}

	public void initContents(Object[] streamDefs, DetailsPageBase[] pages) {
		assert streamDefs.length == pages.length;

		// register pages
		for (int i = 0; i < pages.length; i++) {
			DetailsPageBase page = pages[i];
			Object streamDef = streamDefs[i];

			page.setMaster(this);

			// // register page
			detailsPart.registerPage(streamDef.getClass(), page);
			detailPages.put(page.getInputType(), page);
		}

		// init page list
		viewer.setInput(streamDefs);
	}

	public DetailsPart getDetailsPart() {
		return detailsPart;
	}

	public IManagedForm getManagedForm() {
		return mform;
	}

}
