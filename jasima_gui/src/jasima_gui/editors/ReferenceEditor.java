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

import jasima_gui.JasimaAction;
import jasima_gui.editor.EditorWidget;
import jasima_gui.editor.EditorWidgetFactory;
import jasima_gui.editor.IProperty;
import jasima_gui.editor.PropertyException;
import jasima_gui.util.TypeUtil;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Formatter;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.ui.wizards.NewClassCreationWizard;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.wizards.NewClassWizardPage;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;

import com.thoughtworks.xstream.XStreamException;

@SuppressWarnings("restriction")
public class ReferenceEditor extends EditorWidget {

	private ReferenceEditor parent = null;
	private Action actionNull;
	private Action actionNew;
	private Action actionNewClass;
	private Action actionLoad;
	private Action actionSave;
	private Composite toolBar;
	private Composite cmpEditor = null;
	private Object object;
	private ArrayList<EditorWidget> editors = new ArrayList<EditorWidget>();

	public ReferenceEditor(final Composite parent) {
		super(parent);
		Composite parentRefEditor = parent;
		do {
			if (parentRefEditor instanceof ReferenceEditor) {
				this.parent = (ReferenceEditor) parentRefEditor;
				break;
			}
			parentRefEditor = parentRefEditor.getParent();
		} while (parentRefEditor != null);
	}

	public void createControls() {
		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);

		toolBarManager.add(actionNull = new JasimaAction("...objDelete") {
			@Override
			public void run() {
				object = null;
				storeValue();
				rebuildEditors();
			}
		});

		toolBarManager.add(actionNew = new JasimaAction("...objNew") {
			@Override
			public void run() {
				createNewObject();
			}
		});

		if (TypeUtil.canSubclass(TypeUtil.toClass(property.getType()))) {
			toolBarManager.add(actionNewClass = new JasimaAction("...objNewClass") {
				@Override
				public void run() {
					createNewClass();
				}
			});
		}

		toolBarManager.add(actionLoad = new JasimaAction("...objLoad") {
			@Override
			public void run() {
				loadObject();
			}
		});

		toolBarManager.add(actionSave = new JasimaAction("...objSave") {
			@Override
			public void run() {
				saveObject();
			}
		});

		toolBar = toolBarManager.createControl(this);
		toolBar.setBackground(toolkit.getColors().getBackground());
	}

	private boolean addSuggestion(Formatter fmt, boolean first, Class<?> klass) {
		return addSuggestion(fmt, first, klass, false);
	}

	private boolean addSuggestion(Formatter fmt, boolean first, Class<?> klass, boolean important) {
		Class<?> propType = TypeUtil.toClass(property.getType());
		if (propType.isPrimitive())
			propType = TypeUtil.getPrimitiveWrapper(propType);
		if (!propType.isAssignableFrom(klass)) {
			return false;
		}
		if (first) {
			fmt.format("%s", ", or pick one of the following primitive types:</p>");
		}
		fmt.format("<li><a href='create:%s'%s>%s</a></li>", klass.getCanonicalName(), important ? " bold='yes'" : "",
				klass.getSimpleName());
		return true;
	}

	protected void rebuildEditors() {
		editors.clear();

		if (cmpEditor != null)
			cmpEditor.dispose();

		setStatusText(String.valueOf(object));

		if (object == null) {
			actionNull.setEnabled(false);
			actionSave.setEnabled(false);

			cmpEditor = toolkit.createComposite(this);
			cmpEditor.setLayout(new FillLayout());
			FormText nullText = toolkit.createFormText(cmpEditor, true);

			Formatter fmt = new Formatter();
			fmt.format("%s", "<form><p>This property is set to null. " //
					+ "You may <a href='create'>create</a> or " //
					+ "<a href='load'>load</a> a complex object");
			boolean hasSugg = false;
			hasSugg |= addSuggestion(fmt, !hasSugg, String.class, true);
			hasSugg |= addSuggestion(fmt, !hasSugg, Character.class);
			hasSugg |= addSuggestion(fmt, !hasSugg, Boolean.class);
			hasSugg |= addSuggestion(fmt, !hasSugg, Byte.class);
			hasSugg |= addSuggestion(fmt, !hasSugg, Short.class);
			hasSugg |= addSuggestion(fmt, !hasSugg, Integer.class, true);
			hasSugg |= addSuggestion(fmt, !hasSugg, Long.class, true);
			hasSugg |= addSuggestion(fmt, !hasSugg, Float.class);
			hasSugg |= addSuggestion(fmt, !hasSugg, Double.class, true);
			if (!hasSugg) {
				fmt.format("%s", ".</p>");
			}
			fmt.format("%s", "</form>");
			nullText.setText(fmt.toString(), true, false);
			fmt.close();
			nullText.addHyperlinkListener(new HyperlinkAdapter() {

				@Override
				public void linkActivated(HyperlinkEvent e) {
					String href = e.getHref().toString();
					if (href.startsWith("create:")) {
						String className = href.substring("create:".length());
						createAndShowObject(className);
					} else if (href.equals("create")) {
						createNewObject();
					} else if (href.equals("load")) {
						loadObject();
					}
				}
			});

			reLayout();

			return;
		}

		ReferenceEditor oe = parent;
		while (oe != null) {
			if (oe.object == object) {
				// there's a reference cycle
				actionNull.setEnabled(true);
				actionSave.setEnabled(false);

				cmpEditor = toolkit.createComposite(this);
				cmpEditor.setLayout(new FillLayout());
				FormText cycleText = toolkit.createFormText(cmpEditor, true);
				cycleText.setText(
						String.format("<form>This property is set to " + "a reference back to %s.</form>",
								oe.property.getName()), true, false);
				// TODO more eye candy

				reLayout();

				return;
			}
			oe = oe.parent;
		}

		actionNull.setEnabled(true);
		actionSave.setEnabled(!TypeUtil.isPrimitiveWrapper(object.getClass()));
		cmpEditor = toolkit.createComposite(this);
		GridLayout editorsLayout = new GridLayout(2, false);
		editorsLayout.marginWidth = 0;
		editorsLayout.marginHeight = 0;
		cmpEditor.setLayout(editorsLayout);

		final EditorWidget typeDisplay = EditorWidgetFactory.getInstance().createEditorWidgetWithLabel(topLevelEditor,
				cmpEditor, new IProperty() {

					@Override
					public void setValue(Object val) throws PropertyException {
						throw new AssertionError("setValue when it's not writable?");
					}

					@Override
					public boolean isWritable() {
						return false;
					}

					@Override
					public boolean isImportant() {
						return false;
					}

					@Override
					public Object getValue() throws PropertyException {
						return object.getClass();
					}

					@Override
					public Class<?> getType() {
						return Class.class;
					}

					@Override
					public String getName() {
						return "type";
					}

					@Override
					public String getHTMLDescription() {
						return "The type of the currently assigned value. This may be a subtype of the property type.";
					}

					@Override
					public boolean canBeNull() {
						return false;
					}
				}, property);
		editors.add(typeDisplay);
		typeDisplay.loadValue();

		final IProperty prop = new IProperty() {

			@Override
			public void setValue(Object val) throws PropertyException {
				property.setValue(val);
				object = val;
				setStatusText(String.valueOf(object));
			}

			@Override
			public boolean isWritable() {
				return property.isWritable();
			}

			@Override
			public boolean isImportant() {
				return true;
			}

			@Override
			public Object getValue() throws PropertyException {
				return object;
			}

			@Override
			public Class<?> getType() {
				return object.getClass();
			}

			@Override
			public String getName() {
				return property.getName();
			}

			@Override
			public String getHTMLDescription() {
				return property.getHTMLDescription();
			}

			@Override
			public boolean canBeNull() {
				return false;
			}
		};

		final EditorWidget editor = EditorWidgetFactory.getInstance().createEditorWidget(topLevelEditor, cmpEditor,
				prop, property);
		editors.add(editor);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(editor);
		Control c = editor.getToolBar();
		if (c != null)
			c.dispose();
		editor.loadValue();

		reLayout();

		return;
	}

	@Override
	public void setEnabled(boolean enabled) {
		for (EditorWidget editor : editors) {
			editor.setEnabled(enabled);
		}
		actionNull.setEnabled(enabled);
		actionNew.setEnabled(enabled);
		actionNewClass.setEnabled(enabled);
		actionLoad.setEnabled(enabled);
		actionSave.setEnabled(enabled && !TypeUtil.isPrimitiveWrapper(object.getClass()));
	}

	@Override
	public boolean isExpandable() {
		return true;
	}

	@Override
	public Control getToolBar() {
		return toolBar;
	}

	@Override
	public void storeValue() {
		try {
			property.setValue(object);
			hideError();
		} catch (PropertyException e) {
			showError(e.getLocalizedMessage());
		}
	}

	@Override
	public void loadValue() {
		try {
			object = property.getValue();
			rebuildEditors();
		} catch (PropertyException e) {
			showError(e.getLocalizedMessage());
		}
	}

	public void createNewObject() {
		String className = showDialogClassSelection();
		if (className == null)
			return;

		createAndShowObject(className);
	}

	public void createNewClass() {
		StructuredSelection projectSel = new StructuredSelection(topLevelEditor.getJavaProject());

		NewClassWizardPage page = new NewClassWizardPage();
		page.init(projectSel);

		Class<?> type = TypeUtil.toClass(property.getType());
		if (type.isInterface()) {
			page.addSuperInterface(type.getName());
		} else {
			page.setSuperClass(type.getName(), true);
		}

		NewClassCreationWizard wizard = new NewClassCreationWizard(page, true);
		wizard.init(PlatformUI.getWorkbench(), projectSel);

		WizardDialog dlg = new WizardDialog(getShell(), wizard);
		PixelConverter converter = new PixelConverter(JFaceResources.getDialogFont());
		dlg.setMinimumPageSize(converter.convertWidthInCharsToPixels(70), converter.convertHeightInCharsToPixels(20));
		dlg.create();
		dlg.open();
	}

	protected void createAndShowObject(String className) {
		try {
			Class<?> klass = topLevelEditor.getClassLoader().loadClass(className);

			Object obj;
			if (TypeUtil.isPrimitiveWrapper(klass)) {
				obj = TypeUtil.createDefaultPrimitive(klass);
			} else {
				obj = klass.getConstructor().newInstance((Object[]) null);
			}
			property.setValue(obj);
			object = obj;
			rebuildEditors();
		} catch(ClassNotFoundException e) {
			showErrorDialog("Can't load class %s.", className);
		} catch (NoSuchMethodException e) {
			showErrorDialog("Can't instantiate a class without a public default constructor.");
		} catch (InvocationTargetException e) {
			showErrorDialog("The constructor threw an instance of %s: %s", e.getCause().getClass().getSimpleName(), e
					.getCause().getLocalizedMessage());
		} catch (InstantiationException e) {
			showErrorDialog("Can't instantiate an abstract class.");
		} catch (PropertyException ex) {
			showErrorDialog(ex.getLocalizedMessage());
		} catch (Throwable e) {
			StringWriter sw = new StringWriter(512);
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			pw.flush();

			showErrorDialog("Some unexpected error occurred %s: \n%s", e.getLocalizedMessage(), sw.toString());
		}
	}

	protected String showDialogClassSelection() {
		IJavaProject proj = topLevelEditor.getJavaProject();
		Class<?> propType = TypeUtil.toClass(property.getType());
		SelectionDialog dlg;
		try {
			dlg = JavaUI.createTypeDialog(getShell(), null, SearchEngine.createStrictHierarchyScope(proj,
					proj.findType(propType.getCanonicalName()), true, true, null),
					IJavaElementSearchConstants.CONSIDER_CLASSES, false, "?");
			dlg.setTitle(String.format("Types compatible with %s", TypeUtil.toString(propType, true)));
			if (dlg.open() != SelectionDialog.OK) {
				return null;
			}
			IType type = (IType) dlg.getResult()[0];
			return type.getFullyQualifiedName();
		} catch (JavaModelException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	protected IPath getDefaultPath() {
		IPath path = ((IFileEditorInput) topLevelEditor.getEditorInput()).getFile().getLocation();
		return path.removeLastSegments(1); // get directory
	}

	protected void loadObject() {
		// TODO make it non-modal (how?)
		FileDialog dlg = new FileDialog(PlatformUI.getWorkbench().getModalDialogShellProvider().getShell(), SWT.OPEN);
		dlg.setFilterPath(getDefaultPath().toOSString());
		dlg.setFilterExtensions(new String[] { "*.jasima", "*.xml" });
		dlg.setFilterNames(new String[] { "jasima files (*.jasima)", "XML files (*.xml)" });
		String path = dlg.open();
		if (path == null)
			return;
		try (FileInputStream is = new FileInputStream(path)) {
			Object obj = topLevelEditor.getSerialization().deserialize(is);
			property.setValue(obj);
			object = obj;
			rebuildEditors();
		} catch (IOException ex) {
			showErrorDialog("Can't read file: %s", ex.getLocalizedMessage());
		} catch (XStreamException ex) {
			showErrorDialog("Can't load file: %s", ex.getLocalizedMessage());
		} catch (PropertyException ex) {
			showErrorDialog(ex.getLocalizedMessage());
		}
	}

	protected void saveObject() {
		// TODO make it non-modal (how?)
		SaveAsDialog dlg = new SaveAsDialog(PlatformUI.getWorkbench().getModalDialogShellProvider().getShell());
		IPath path = getDefaultPath();
		path = path.append(property.getName() + ".jasima"); // suggest file name
		dlg.setOriginalFile(ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(path));
		dlg.create();
		if (dlg.open() == SaveAsDialog.CANCEL)
			return;

		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(dlg.getResult());
		byte[] byteArr = topLevelEditor.getSerialization().serialize(object);
		try {
			if (file.exists()) {
				file.setContents(new ByteArrayInputStream(byteArr), false, true, null);
			} else {
				file.create(new ByteArrayInputStream(byteArr), false, null);
			}
		} catch (CoreException e) {
			ErrorDialog.openError(PlatformUI.getWorkbench().getModalDialogShellProvider().getShell(),
					"Couldn't save file", null, e.getStatus());
		}
	}
}
