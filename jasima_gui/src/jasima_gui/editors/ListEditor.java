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

import jasima_gui.Serialization;
import jasima_gui.editor.EditorDialog;
import jasima_gui.editor.EditorWidget;
import jasima_gui.editor.INoExceptProperty;
import jasima_gui.editor.PropertyException;
import jasima_gui.util.ToStringBasedComparator;
import jasima_gui.util.TypeUtil;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;

public class ListEditor extends EditorWidget implements SelectionListener {

	/**
	 * Contains a copy of the whole list optimized for random access.
	 */
	private ArrayList<Object> objects = null;

	private boolean keepsOrder;
	private boolean allowsDuplicates;

	private List listView;
	private Button btnNew;
	private Button btnDelete;
	private Button btnAdd;
	private Button btnEdit;
	private Button btnRemove;
	private Button btnUp = null;
	private Button btnDown = null;

	public ListEditor(Composite parent) {
		super(parent);
	}

	public void createControls() {
		Class<?> type = TypeUtil.toClass(property.getType());
		keepsOrder = type.isArray()
				|| java.util.List.class.isAssignableFrom(type)
				|| LinkedHashSet.class.isAssignableFrom(type);
		allowsDuplicates = !Set.class.isAssignableFrom(type);

		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 0;
		layout.marginLeft = 2;
		layout.marginHeight = 0;
		setLayout(layout);

		listView = new List(this, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
		toolkit.adapt(listView, true, true);
		listView.addSelectionListener(this);
		GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 250)
				.span(1, keepsOrder ? 7 : 5).applyTo(listView);

		btnNew = toolkit.createButton(this, "Create new", SWT.PUSH);
		btnNew.addSelectionListener(this);
		GridDataFactory.fillDefaults().applyTo(btnNew);

		btnDelete = toolkit.createButton(this, "Set to null", SWT.PUSH);
		btnDelete.addSelectionListener(this);
		GridDataFactory.swtDefaults().grab(false, true)
				.align(SWT.FILL, SWT.BEGINNING).hint(130, SWT.DEFAULT)
				.applyTo(btnDelete);

		btnAdd = toolkit.createButton(this, "Add...", SWT.PUSH);
		btnAdd.addSelectionListener(this);
		GridDataFactory.fillDefaults().applyTo(btnAdd);

		btnEdit = toolkit.createButton(this, "Edit...", SWT.PUSH);
		btnEdit.addSelectionListener(this);
		GridDataFactory.fillDefaults().applyTo(btnEdit);

		btnRemove = toolkit.createButton(this, "Remove", SWT.PUSH);
		btnRemove.addSelectionListener(this);
		GridDataFactory.fillDefaults().applyTo(btnRemove);

		if (keepsOrder) {
			btnUp = toolkit.createButton(this, "Up", SWT.PUSH);
			btnUp.addSelectionListener(this);
			GridDataFactory.swtDefaults().grab(false, true)
					.align(SWT.FILL, SWT.END).applyTo(btnUp);

			btnDown = toolkit.createButton(this, "Down", SWT.PUSH);
			btnDown.addSelectionListener(this);
			GridDataFactory.fillDefaults().applyTo(btnDown);
		}

		listSelectionChanged();
	}

	public boolean confirmDelete() {
		if (objects == null)
			return true;
		if (objects.isEmpty())
			return true;
		return MessageDialog.openConfirm(getShell(), "Confirm deletion",
				"This will delete all entries. Are you sure?");
	}

	@Override
	public boolean isExpandable() {
		return true;
	}

	@Override
	public void storeValue() {
		try {
			if (objects == null) {
				property.setValue(null);
				hideError();
				return;
			}
			Class<?> pType = TypeUtil.toClass(property.getType());
			if (pType.isArray()) {
				Class<?> elementType = pType.getComponentType();
				int size = objects.size();
				Object array = Array.newInstance(elementType, size);
				for (int i = 0; i < size; ++i) {
					Array.set(array, i, objects.get(i));
				}
				property.setValue(array);
			} else { // Collection
				try {
					if (pType == List.class) {
						property.setValue(new ArrayList<Object>(objects));
					} else {
						if (pType == Set.class || pType == SortedSet.class) {
							pType = TreeSet.class; // default implementation
						} else if (pType.isInterface()) {
							showError(String.format(
									"Can't decide on an implementation of %s.",
									pType.getName()));
						}
						@SuppressWarnings("unchecked")
						Collection<Object> coll = (Collection<Object>) pType
								.newInstance();
						coll.addAll(objects);
						property.setValue(coll);
					}
				} catch (Exception e) {
					showErrorDialog("Couldn't create %s.", pType);
				}
			}
			hideError();
		} catch (PropertyException e) {
			showError(e.getLocalizedMessage());
		}
	}

	protected void updateStatusText() {
		if (objects == null) {
			setStatusText("null");
		} else {
			int count = objects.size();
			if (count == 0) {
				setStatusText("empty list");
			} else if (count == 1) {
				setStatusText("list of one object");
			} else {
				setStatusText(String.format("list of %d objects", count));
			}
		}
	}

	private void retrieveValues() {
		try {
			Object val = property.getValue();
			if (val == null) {
				objects = null;
				return;
			}
			if (objects == null) {
				objects = new ArrayList<Object>();
			} else {
				objects.clear();
			}
			if (val.getClass().isArray()) {
				int size = Array.getLength(val);
				objects.clear();
				objects.ensureCapacity(size);
				for (int i = 0; i < size; ++i) {
					objects.add(Array.get(val, i));
				}
			} else {
				for (Object o : (Iterable<?>) val) {
					objects.add(o);
				}
			}

			if (!keepsOrder) {
				Collections.sort(objects, ToStringBasedComparator.INSTANCE);
			}
		} catch (PropertyException e) {
			showError(e.getLocalizedMessage());
		}
	}

	protected Class<?> getElementType() {
		Type type = property.getType();
		Class<?> klass = TypeUtil.toClass(type);
		if (Iterable.class.isAssignableFrom(klass)) {
			ParameterizedType pt = (ParameterizedType) type;
			return TypeUtil.toClass(pt.getActualTypeArguments()[0]);
		} else {
			assert klass.isArray();
			return klass.getComponentType();
		}
	}

	@Override
	public void loadValue() {
		retrieveValues();
		if (objects == null) {
			listView.removeAll();
			listView.add("<null>");

			listView.setEnabled(false);
			btnAdd.setEnabled(false);
			btnDelete.setEnabled(false);
			listSelectionChanged();
		} else {
			String[] strings = new String[objects.size()];
			for (int i = 0; i < strings.length; ++i) {
				strings[i] = String.valueOf(objects.get(i));
			}
			listView.setItems(strings);

			listView.setEnabled(true);
			btnAdd.setEnabled(true);
			btnDelete.setEnabled(true);
			listSelectionChanged();
		}
		updateStatusText();
	}

	@Override
	public void widgetSelected(SelectionEvent evt) {
		Object source = evt.getSource();
		if (source == btnNew) {
			if (objects == null) {
				objects = new ArrayList<Object>();
			} else {
				if (!confirmDelete())
					return;
				objects.clear();
			}
			storeValue();
			loadValue();
		} else if (source == btnDelete) {
			if (objects == null)
				return;
			if (!confirmDelete())
				return;
			objects = null;
			storeValue();
			loadValue();
		} else if (source == btnAdd) {
			editOrAddObject(true);
		} else if (source == btnRemove) {
			int[] selection = listView.getSelectionIndices();
			Arrays.sort(selection);
			for (int i = selection.length - 1; i >= 0; --i) {
				objects.remove(selection[i]);
			}
			listView.remove(selection); // still not rebuilding it
			listSelectionChanged();
			updateStatusText();
			storeValue();
		} else if (source == btnUp) {
			int[] selection = listView.getSelectionIndices();
			Arrays.sort(selection);
			for (int i = 0; i < selection.length; ++i) {
				// the first element doesn't move
				if (selection[i] == 0)
					continue;

				// an element doesn't move if the element above was selected and
				// didn't move
				if (Arrays.binarySearch(selection, selection[i] - 1) >= 0)
					continue;

				// swap with item above it
				String strAbove = listView.getItem(selection[i] - 1);
				listView.setItem(selection[i] - 1,
						listView.getItem(selection[i]));
				listView.setItem(selection[i], strAbove);

				// do the same in our object list
				Object objAbove = objects.get(selection[i] - 1);
				objects.set(selection[i] - 1, objects.get(selection[i]));
				objects.set(selection[i], objAbove);

				// correct the selection
				--selection[i];
			}
			listView.setSelection(selection);
			updateStatusText();
			storeValue();
		} else if (source == btnDown) {
			int[] selection = listView.getSelectionIndices();
			Arrays.sort(selection);
			for (int i = selection.length - 1; i >= 0; --i) {
				if (selection[i] == listView.getItemCount() - 1)
					continue;
				if (Arrays.binarySearch(selection, selection[i] + 1) >= 0)
					continue;

				// swap with item below it
				String strBelow = listView.getItem(selection[i] + 1);
				listView.setItem(selection[i] + 1,
						listView.getItem(selection[i]));
				listView.setItem(selection[i], strBelow);

				// do the same in our object list
				Object objBelow = objects.get(selection[i] + 1);
				objects.set(selection[i] + 1, objects.get(selection[i]));
				objects.set(selection[i], objBelow);

				// correct the selection
				++selection[i];
			}
			listView.setSelection(selection);
			updateStatusText();
			storeValue();
		} else if (source == btnEdit) {
			editOrAddObject(false);
		} else if (source == listView) {
			listSelectionChanged();
		}
	}

	private void listSelectionChanged() {
		boolean hasSelection = listView.getSelectionCount() > 0;
		btnRemove.setEnabled(hasSelection);
		btnEdit.setEnabled(hasSelection);
		if (btnUp != null)
			btnUp.setEnabled(hasSelection);
		if (btnDown != null)
			btnDown.setEnabled(hasSelection);
	}

	private void editOrAddObject(final boolean add) {
		final int[] selection = listView.getSelectionIndices();
		if (!add) {
			if (selection.length < 1) {
				assert false;
			} else if (selection.length > 1) {
				if (!allowsDuplicates) {
					showErrorDialog("Editing multiple objects at the same in a collection that does not allow equal elements does not make sense.");
					return;
				} else {
					showErrorDialog("Editing multiple objects at the same time is not supported.");
				}
				return;
			}
		}

		final INoExceptProperty arrayElemProperty = new INoExceptProperty() {
			Object value;

			{
				if (add) {
					Class<?> type = getElementType();
					try {
						if (type == Object.class) {
							value = null;
						} else if (type.isPrimitive()) {
							value = TypeUtil.createDefaultPrimitive(TypeUtil
									.getPrimitiveWrapper(type));
						} else if (TypeUtil.isPrimitiveWrapper(type)) {
							value = TypeUtil.createDefaultPrimitive(type);
						} else {
							value = type.newInstance();
						}
					} catch (Exception e) {
						value = null;
						// ignore
					}
				} else {
					// we need a generic way to clone objects
					Serialization ser = topLevelEditor.getSerialization();
					value = ser.convertFromString(ser.convertToString(objects.get(selection[0])));
				}
			}

			@Override
			public void setValue(Object val) {
				value = val;
			}

			@Override
			public boolean isWritable() {
				return true;
			}

			@Override
			public boolean isImportant() {
				return true;
			}

			@Override
			public Object getValue() {
				return value;
			}

			@Override
			public Class<?> getType() {
				return getElementType();
			}

			@Override
			public String getName() {
				if (add) {
					return String.format("new element of %s",
							property.getName());
				} else {
					return String.format("%s[%d]", property.getName(),
							selection[0]);
				}
			}

			@Override
			public String getHTMLDescription() {
				return null;
			}

			@Override
			public boolean canBeNull() {
				return !getElementType().isPrimitive();
			}
		};

		EditorDialog dialog = new EditorDialog(topLevelEditor,
				arrayElemProperty) {
			@Override
			protected void okPressed() {
				Object val = arrayElemProperty.getValue();
				if (!allowsDuplicates) {
					int i = objects.indexOf(val);
					if (!add && i == selection[0]) {
						cancelPressed();
						return;
					}
					if (i != -1) {
						showErrorDialog("This collection only allows unique elements and already contains an equal object.");
						return;
					}
				}
				super.okPressed();
			}
		};
		if (dialog.open() != EditorDialog.OK)
			return;

		Object val = arrayElemProperty.getValue();
		if (add) {
			int idx;
			if (keepsOrder) {
				idx = (selection.length == 1) ? (selection[0] + 1) : objects
						.size();
			} else {
				idx = Collections.binarySearch(objects, val,
						ToStringBasedComparator.INSTANCE);
				if (idx < 0)
					idx = -idx - 1;
			}
			listView.add(String.valueOf(val), idx);
			objects.add(idx, val);
			listView.setSelection(idx);
			listSelectionChanged();
			updateStatusText();
		} else {
			if (keepsOrder) {
				objects.set(selection[0], val);
				listView.setItem(selection[0], String.valueOf(val));
			} else {
				objects.remove(selection[0]);
				listView.remove(selection[0]);
				int idx = Collections.binarySearch(objects, val,
						ToStringBasedComparator.INSTANCE);
				if (idx < 0)
					idx = -idx - 1;

				listView.add(String.valueOf(val), idx);
				objects.add(idx, val);
				listView.setSelection(idx);
				listSelectionChanged();
				updateStatusText();
			}
		}
		storeValue();
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent evt) {
		if (evt.getSource() == listView) {
			editOrAddObject(false);
		}
	}

}
