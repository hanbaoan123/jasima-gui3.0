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

import jasima_gui.editor.EditorDialog;
import jasima_gui.editor.EditorWidget;
import jasima_gui.editor.INoExceptProperty;
import jasima_gui.editor.IProperty;
import jasima_gui.editor.PropertyException;
import jasima_gui.editor.PropertyLister;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.events.TreeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

public class FactorsEditor extends EditorWidget implements SelectionListener,
		TreeListener {

	protected final static Object INVALID = new Object();

	private Image factorImage;
	private Tree factorsTree;
	private TreeEditor factorsTreeEditor;
	private Button btnAddFactor;
	private Button btnAddValue;
	private Button btnEdit;
	private Button btnRemove;

	public FactorsEditor(Composite parent) {
		super(parent);

		factorImage = ImageDescriptor.createFromFile(getClass(),
				"/icons/factor.png").createImage(true);
	}

	@Override
	public void dispose() {
		factorImage.dispose();
		super.dispose();
	}

	@Override
	public void createControls() {
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		setLayout(layout);

		factorsTree = new Tree(this, SWT.BORDER | SWT.V_SCROLL);
		GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 230)
				.span(1, 4).applyTo(factorsTree);
		factorsTree.addSelectionListener(this);
		factorsTree.addTreeListener(this);

		factorsTreeEditor = new TreeEditor(factorsTree);
		factorsTreeEditor.grabHorizontal = true;

		btnAddFactor = toolkit.createButton(this, "Add factor...", SWT.PUSH);
		btnAddFactor.addSelectionListener(this);
		GridDataFactory.fillDefaults().hint(130, SWT.DEFAULT)
				.applyTo(btnAddFactor);

		btnAddValue = toolkit.createButton(this, "Add value...", SWT.PUSH);
		btnAddValue.addSelectionListener(this);
		GridDataFactory.fillDefaults().applyTo(btnAddValue);

		btnEdit = toolkit.createButton(this, "Edit...", SWT.PUSH);
		btnEdit.addSelectionListener(this);
		GridDataFactory.fillDefaults().applyTo(btnEdit);

		btnRemove = toolkit.createButton(this, "Remove", SWT.PUSH);
		btnRemove.addSelectionListener(this);
		GridDataFactory.swtDefaults().grab(false, true)
				.align(SWT.FILL, SWT.BEGINNING).applyTo(btnRemove);

		treeSelectionChanged();
	}

	@Override
	public void storeValue() {
		try {
			Object ffe = property.getValue();
			Class<?> klass = ffe.getClass();

			klass.getMethod("clearFactors").invoke(ffe);

			Method addFactor = klass.getMethod("addFactor", String.class,
					Object.class);

			for (TreeItem factorItem : factorsTree.getItems()) {
				Object facName = factorItem.getData();
				for (TreeItem valueItem : factorItem.getItems()) {
					addFactor.invoke(ffe, facName, valueItem.getData());
				}
			}

			property.setValue(ffe);
		} catch (PropertyException e) {
			showError(e.getLocalizedMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void loadValue() {
		factorsTree.clearAll(true);
		try {
			Object ffe = property.getValue();
			Class<?> klass = ffe.getClass();

			Collection<?> facNames = (Collection<?>) klass.getMethod(
					"getFactorNames").invoke(ffe);
			for (Object facName : facNames) {
				TreeItem factorItem = createFactorNameItem((String) facName);
				for (Object facVal : (Collection<?>) klass.getMethod(
						"getFactorValues", String.class).invoke(ffe, facName)) {
					TreeItem valueItem = new TreeItem(factorItem, SWT.DEFAULT);
					valueItem.setData(facVal);
					valueItem.setText(String.valueOf(facVal));
				}
			}

			hideError();
		} catch (PropertyException e) {
			showError(e.getLocalizedMessage());
		} catch (Exception e) {
			factorsTree.clearAll(true);
			e.printStackTrace();
		}
	}

	protected TreeItem createFactorNameItem(String factorName) {
		TreeItem retVal = new TreeItem(factorsTree, SWT.DEFAULT);
		if (factorName == null) {
			retVal.setData(INVALID);
		} else {
			retVal.setData(factorName);
			retVal.setText(factorName);
		}
		retVal.setImage(factorImage);
		return retVal;
	}

	private interface FocusAndSelectionListener extends FocusListener,
			SelectionListener {
		// empty
	}

	protected void editFactorName(final TreeItem itm) {
		assert isFactorName(itm);
		factorsTree.setSelection(itm);
		treeSelectionChanged();

		final Combo facNameEditor = new Combo(factorsTree, 0);
		if (itm.getData() != INVALID) {
			facNameEditor.setText((String) itm.getData());
		}
		factorsTreeEditor.setEditor(facNameEditor, itm);

		factorsTree.setFocus();
		facNameEditor.setFocus();

		try {
			Object ffe = property.getValue();
			final Object baseExp = ffe.getClass()
					.getMethod("getBaseExperiment").invoke(ffe);
			Collection<IProperty> props = PropertyLister
					.listWritableProperties(new INoExceptProperty() {
						public void setValue(Object val) {
							throw new UnsupportedOperationException();
						}

						public boolean isWritable() {
							throw new UnsupportedOperationException();
						}

						public boolean isImportant() {
							throw new UnsupportedOperationException();
						}

						public Object getValue() {
							throw new UnsupportedOperationException();
						}

						public Type getType() {
							return baseExp.getClass();
						}

						public String getName() {
							throw new UnsupportedOperationException();
						}

						public String getHTMLDescription() {
							throw new UnsupportedOperationException();
						}

						public boolean canBeNull() {
							throw new UnsupportedOperationException();
						}
					}, topLevelEditor);
			for (IProperty prop : props) {
				facNameEditor.add(prop.getName());
			}
		} catch (Exception e) {
			e.printStackTrace();
			// no proposals
		}

		FocusAndSelectionListener listener = new FocusAndSelectionListener() {
			boolean validate(String newVal) {
				if (newVal.isEmpty() | newVal.contains(" "))
					return false;
				for (TreeItem factorItem : factorsTree.getItems()) {
					if (newVal.equals(factorItem.getData())) {
						return false;
					}
				}
				return true;
			}

			void finishEdit() {
				String newVal = facNameEditor.getText().trim();
				if (!validate(newVal)) {
					if (itm.getData() == INVALID) {
						itm.dispose();
						treeSelectionChanged();
					}
				} else {
					itm.setText(newVal);
					itm.setData(newVal);
					storeValue();
				}
				facNameEditor.dispose();
			}

			public void widgetSelected(SelectionEvent evt) {
				// ignore
			}

			public void widgetDefaultSelected(SelectionEvent evt) {
				finishEdit();
			}

			public void focusLost(FocusEvent evt) {
				finishEdit();
			}

			public void focusGained(FocusEvent e) {
				// ignore
			}
		};
		facNameEditor.addFocusListener(listener);
		facNameEditor.addSelectionListener(listener);
	}

	protected void editFactorValue(final TreeItem itm) {
		editFactorValue(itm, itm.getData());
	}

	protected void editFactorValue(final TreeItem itm, final Object initValue) {
		assert !isFactorName(itm);

		final String factorName = (String) itm.getParentItem().getData();

		final IProperty property = new INoExceptProperty() {
			Object value = initValue;

			public void setValue(Object val) {
				value = val;
			}

			public boolean isWritable() {
				return true;
			}

			public boolean isImportant() {
				return true;
			}

			public Object getValue() {
				return value == INVALID ? null : value;
			}

			public Type getType() {
				return Object.class;
			}

			public String getName() {
				return String.format("value for factor %s", factorName);
			}

			public String getHTMLDescription() {
				return String.format("One possible value for the base "
						+ "experiment's property %s", factorName);
			}

			@Override
			public boolean canBeNull() {
				return true;
			}
		};

		EditorDialog dlg = new EditorDialog(topLevelEditor, property);
		int dlgReturn = dlg.open();
		try {
			Object propVal = property.getValue();
			if (dlgReturn != EditorDialog.OK && itm.getData() == INVALID) {
				itm.dispose();
				return;
			}
			itm.setData(propVal);
			itm.setText(String.valueOf(propVal));
			factorsTree.setSelection(itm);
			treeSelectionChanged();
			if (dlgReturn == EditorDialog.OK) {
				storeValue();
			}
		} catch (PropertyException e) {
			assert false;
		}
	}

	protected void editSelection() {
		TreeItem[] selection = factorsTree.getSelection();
		if (selection.length != 1)
			return;
		if (isFactorName(selection[0])) {
			editFactorName(selection[0]);
		} else {
			editFactorValue(selection[0]);
		}
	}

	@Override
	public void widgetSelected(SelectionEvent evt) {
		Object source = evt.getSource();
		if (source == btnAddFactor) {
			editFactorName(createFactorNameItem(null));
		} else if (source == btnAddValue) {
			TreeItem[] selection = factorsTree.getSelection();
			if (selection.length != 1)
				return;
			TreeItem factorName = selection[0];
			if (!isFactorName(factorName)) {
				factorName = factorName.getParentItem();
			}

			Object initVal;
			if (selection[0] != factorName) {
				initVal = selection[0].getData();
			} else if (factorName.getItemCount() > 0) {
				initVal = factorName.getItem(0).getData();
			} else {
				initVal = INVALID;
			}
			TreeItem valueItem = new TreeItem(factorName, SWT.DEFAULT);
			selection[0].setExpanded(true);
			valueItem.setData(INVALID);
			editFactorValue(valueItem, initVal);
		} else if (source == btnEdit) {
			editSelection();
		} else if (source == btnRemove) {
			for (TreeItem itm : factorsTree.getSelection()) {
				itm.dispose();
			}
			treeSelectionChanged();
			storeValue();
		} else if (source == factorsTree) {
			treeSelectionChanged();
		}
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent evt) {
		Object source = evt.getSource();
		if (source == factorsTree) {
			editSelection();
		}
	}

	private void treeSelectionChanged() {
		int selectionLength = factorsTree.getSelectionCount();
		btnAddValue.setEnabled(selectionLength == 1);
		btnEdit.setEnabled(selectionLength == 1);
		btnRemove.setEnabled(selectionLength > 0);
	}

	private boolean isFactorName(TreeItem item) {
		return item.getParentItem() == null;
	}

	@Override
	public void treeCollapsed(TreeEvent e) {
		// ignore
	}

	@Override
	public void treeExpanded(TreeEvent e) {
		// Workaround: On Gtk, expanding a tree item would cause it to get
		// selected, but won't notify the selection listener.
		treeSelectionChanged();
	}
}
