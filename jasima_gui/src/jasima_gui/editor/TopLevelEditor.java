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

import jasima_gui.ClassLoaderListener;
import jasima_gui.ConversionReport;
import jasima_gui.EclipseProjectClassLoader;
import jasima_gui.JasimaAction;
import jasima_gui.JavaLinkHandler;
import jasima_gui.PropertyToolTip;
import jasima_gui.Serialization;
import jasima_gui.launcher.SimulationLaunchShortcut;
import jasima_gui.util.BrowserEx;
import jasima_gui.util.IOUtil;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLinks;
import org.eclipse.jdt.ui.JavadocContentAccess;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;

@SuppressWarnings("restriction")
public class TopLevelEditor extends EditorPart implements SelectionListener {

	protected static final String CLASS_URL_PREFIX = "jasima-javaclass:";
	protected static final String HREF_MORE = "jasima-command:more";
	protected static final String HREF_LESS = "jasima-command:less";
	private EditorUpdater updater;
	private Object root;
	private Throwable loadError;
	private ConversionReport conversionReport;
	private FormToolkit toolkit;
	private ScrolledForm form;
	private boolean dirty;
	private Serialization serialization;
	private ClassLoaderListener classLoaderListener = new ClassLoaderListener() {
		@Override
		public void classesChanged() {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					TopLevelEditor.this.classesChanged();
				};
			});
		}

		public void classPathChanged() {
			classesChanged();
		}
	};
	private Action runExperiment;
	private Action debugExperiment;

	// exactly one of these is always shown:
	private Object mainControl;

	public TopLevelEditor() {
		updater = new EditorUpdater(this);
	}

	public IJavaProject getJavaProject() {
		return serialization.getProject();
	}

	public EclipseProjectClassLoader getClassLoader() {
		return serialization.getClassLoader();
	}

	public FormToolkit getToolkit() {
		return toolkit;
	}

	@Override
	public void dispose() {
		updater.dispose();
		if (toolkit != null) {
			toolkit.dispose();
		}
		super.dispose();
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		IFileEditorInput fei = (IFileEditorInput) input;
		setFileInput(fei);
		setSite(site);
		try {
			fei.getFile().refreshLocal(0, null);
			IProject project = fei.getFile().getProject();
			try (InputStream is = fei.getStorage().getContents()) {
				serialization = new Serialization(project);
				try {
					serialization.startConversionReport();
					serialization.getClassLoader().addListener(classLoaderListener);
					root = serialization.deserialize(is);
				} finally {
					conversionReport = serialization.finishConversionReport();
				}
			}
			loadError = null;
		} catch (LinkageError e) {
			loadError = e;
			conversionReport = null;
		} catch (Exception e) {
			loadError = e;
			conversionReport = null;
		}
	}

	protected void migrateClassLoader() {
		Serialization newSer;
		if (serialization.getClassLoader().getState().isDirty()) {
			newSer = new Serialization(serialization.getProject());
		} else {
			newSer = serialization;
		}

		if (!isDirty()) {
			// if there are no unsaved changes, there's no need to keep root
			// around
			root = null;
		}

		try {
			if (root != null) {
				String xml = serialization.convertToString(root);
				try {
					newSer.startConversionReport();
					root = newSer.convertFromString(xml);
				} finally {
					conversionReport = newSer.finishConversionReport();
				}
			} else {
				IFileEditorInput fei = (IFileEditorInput) getEditorInput();
				fei.getFile().refreshLocal(0, null);
				try (InputStream is = fei.getStorage().getContents()) {
					try {
						newSer.startConversionReport();
						root = newSer.deserialize(is);
					} finally {
						conversionReport = newSer.finishConversionReport();
					}
				}
			}

			if (serialization != newSer) {
				serialization.getClassLoader().dispose();
				serialization = newSer;
			}
			newSer.getClassLoader().addListener(classLoaderListener);
			loadError = null;
		} catch (Throwable e) {
			// While catching only Exception subclasses would be nice,
			// LinkageError and plain Error (unresolved compilation problem) can
			// also be thrown during deserialization.
			if (e instanceof VirtualMachineError)
				throw (VirtualMachineError) e;

			loadError = e;
			conversionReport = null;
		}
	}

	protected void classesChanged() {
		if (!getClassLoader().getState().isDirty() && isValidData()) {
			// class loader isn't dirty and root was successfully loaded
			// no need to do anything
			return;
		}
		migrateClassLoader();

		if (form == null || form.isDisposed())
			return;

		createBody();
	}

	/**
	 * Sets the input to this editor.
	 * 
	 * If the new input is contained in a different project than the old one,
	 * the editor will not update and check the class path. In that case, the
	 * editor should only be used to save the file and then be closed and
	 * re-opened.
	 * 
	 * @param input
	 *            the editor input
	 */
	protected void setFileInput(IFileEditorInput input) {
		super.setInput(input);
		updateHeadline();
	}

	protected boolean isValidData() {
		return loadError == null;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return isValidData();
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	protected void updateHeadline() {
		if (form == null)
			return;

		setPartName(getEditorInput().getName());

		Composite head = new Composite(form.getForm().getHead(), SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(head);

		Link headline = new Link(head, SWT.NONE);
		headline.setForeground(form.getForeground());
		headline.setBackground(null);
		headline.setFont(form.getFont());
		if (isValidData()) {
			headline.setText(String.format("%s - <a href=\"%s%s\">%s</a>", getEditorInput().getName(),
					CLASS_URL_PREFIX, root.getClass().getCanonicalName(), root.getClass().getSimpleName()));
		} else {
			headline.setText(getEditorInput().getName());
		}
		headline.addSelectionListener(this);

		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);

		toolBarManager.add(runExperiment = new JasimaAction("...runExperiment") {
			@Override
			public void run() {
				IFile file = ((IFileEditorInput) getEditorInput()).getFile();
				new SimulationLaunchShortcut().launch(file, "run");
			}
		});

		toolBarManager.add(debugExperiment = new JasimaAction("...debugExperiment") {
			@Override
			public void run() {
				IFile file = ((IFileEditorInput) getEditorInput()).getFile();
				new SimulationLaunchShortcut().launch(file, "debug");
			}
		});

		toolBarManager.add(new JasimaAction("...openWebpage") {
			@Override
			public void run() {
				Program.launch("https://code.google.com/p/jasima/");
			}
		});

		ToolBar toolBar = toolBarManager.createControl(head);
		GridDataFactory.swtDefaults().align(SWT.RIGHT, SWT.CENTER).grab(true, false).applyTo(toolBar);

		Control oldHead = form.getForm().getHeadClient();
		if (oldHead != null)
			oldHead.dispose();
		form.setHeadClient(head);
		toolkit.decorateFormHeading(form.getForm());
	}

	protected static String buildDocument(String javadoc) {
		StringBuilder htmlDoc = new StringBuilder();
		htmlDoc.append("<!DOCTYPE html><html><head>" //
				+ "<title>Tooltip</title><style type='text/css'>");
		htmlDoc.append(PropertyToolTip.getJavadocStylesheet());
		htmlDoc.append("html {padding-top: -1px; padding-bottom: -1px; margin: 0px} " //
				+ "body {padding: 0px; margin: 0px} " //
				+ "dl {margin: 0px} " //
				+ "dt {margin-top: 0.5em} " //
				+ "#jasima-content {padding-top: 1px; padding-bottom: 1px}");
		htmlDoc.append("</style></head><body><div id='jasima-content'>");
		htmlDoc.append(javadoc);
		htmlDoc.append("</div></body></html>");
		return htmlDoc.toString();
	}

	@Override
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());

		form = toolkit.createScrolledForm(parent);
		form.setExpandHorizontal(true);
		updateHeadline();

		Layout layout = new Layout() {
			static final int SPACING = 10;
			static final int VMARGIN = 10;
			static final int HMARGIN = 5;

			@Override
			protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
				Point retVal = new Point(composite.getSize().x, 0);
				if (retVal.x == 0) {
					retVal.x = -1;
				} else {
					retVal.x -= 2 * HMARGIN;
				}
				retVal.y += VMARGIN;
				for (Control c : composite.getChildren()) {
					retVal.y += SPACING;
					retVal.y += determineSize(c, SWT.DEFAULT).y;
				}
				retVal.y += VMARGIN - SPACING;
				retVal.x = 0;
				return retVal;
			}

			@Override
			protected void layout(Composite composite, boolean flushCache) {
				int w = composite.getSize().x - 2 * HMARGIN;
				int posY = VMARGIN;
				for (Control c : composite.getChildren()) {
					Point size = determineSize(c, w);
					c.setSize(size);
					c.setLocation(HMARGIN, posY);
					posY += size.y;
					posY += SPACING;
				}
			}

			protected Point determineSize(Control c, int width) {
				Object ld = c.getLayoutData();
				if (ld instanceof Point) {
					Point p = (Point) ld;
					int wHint = (p.x == SWT.DEFAULT) ? width : p.x;
					return c.computeSize(wHint, p.y);
				}
				return c.computeSize(width, SWT.DEFAULT);
			}
		};

		form.getBody().setLayout(layout);

		createBody();
	}

	protected void wipeBody() {
		for (Control ctrl : form.getBody().getChildren()) {
			ctrl.dispose();
		}
	}

	protected void createBody() {
		if (!isValidData()) {
			runExperiment.setEnabled(false);
			debugExperiment.setEnabled(false);

			if (!(mainControl instanceof DeserializationFailure)) {
				wipeBody();
				mainControl = new DeserializationFailure(form.getBody());
			}

			DeserializationFailure failure = (DeserializationFailure) mainControl;
			failure.setException(loadError);
		} else if (conversionReport != null) {
			runExperiment.setEnabled(false);
			debugExperiment.setEnabled(false);

			if (!(mainControl instanceof ConversionReportView)) {
				wipeBody();
				mainControl = new ConversionReportView(form.getBody(), this);
			}

			ConversionReportView crv = (ConversionReportView) mainControl;
			crv.setReport(conversionReport);
		} else {
			runExperiment.setEnabled(true);
			debugExperiment.setEnabled(true);

			// we should never show an editor based on old class definitions
			assert !getClassLoader().getState().isDirty();
			mainControl = null;
			wipeBody();
			createJavaDocDescription();
			createMainEditor();
		}
		form.getBody().layout(true, true);
		form.reflow(true);
	}

	protected void createMainEditor() {
		IProperty topLevelProperty = new IProperty() {
			public void setValue(Object val) throws PropertyException {
				root = val;
				makeDirty();
			}

			public boolean isWritable() {
				return true;
			}

			public boolean isImportant() {
				return true;
			}

			public Object getValue() throws PropertyException {
				return root;
			}

			public Class<?> getType() {
				return root.getClass();
			}

			public String getName() {
				return getEditorInput().getName();
			}

			@Override
			public String getHTMLDescription() {
				return "";
			}

			@Override
			public boolean canBeNull() {
				return false;
			}
		};

		EditorWidget editor = EditorWidgetFactory.getInstance().createEditorWidget(this, form.getBody(),
				topLevelProperty, null);
		editor.loadValue();
	}

	protected void createJavaDocDescription() {
		// get JavaDoc as HTML (content only)

		String doc;
		try {
			IType type = getJavaProject().findType(root.getClass().getCanonicalName());
			doc = IOUtil.readFully(JavadocContentAccess.getHTMLContentReader(type, true, true));
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		if (doc == null || doc.trim().length() == 0)
			return;

		String summary = createSummary(doc);
		if (summary == null) {
			summary = doc;
		} else {
			summary += String.format(" <a href=\"%s\">%s</a>", HREF_MORE, "more");
			doc += String.format("<br><a href=\"%s\">%s</a>", HREF_LESS, "hide detailed description");
		}

		final String summaryDoc = buildDocument(summary);
		final String mainDoc = buildDocument(doc);

		final BrowserEx browser = new BrowserEx(form.getBody(), SWT.NONE);
		browser.setText(summaryDoc, false);

		browser.addSizeListener(new Listener() {
			public void handleEvent(Event evt) {
				form.getBody().layout(true, true);
				form.reflow(true);
			}
		});

		final LocationListener linkHandler = JavaElementLinks.createLocationListener(new JavaLinkHandler());

		browser.addLocationListener(new LocationListener() {
			@Override
			public void changing(LocationEvent event) {
				if (event.location.equals(HREF_LESS)) {
					browser.setText(summaryDoc, false);
					event.doit = false;
				} else if (event.location.equals(HREF_MORE)) {
					browser.setText(mainDoc, false);
					event.doit = false;
				} else {
					linkHandler.changing(event);
				}
			}

			public void changed(LocationEvent event) {
				linkHandler.changed(event);
			}
		});
	}

	/**
	 * Find first sentence of "doc".
	 */
	private String createSummary(String doc) {
		int summaryEnd = doc.indexOf(". ") + 1;
		if (summaryEnd == 0) {
			summaryEnd = doc.indexOf(".\n") + 1;
		}
		if (summaryEnd == 0) {
			summaryEnd = doc.indexOf("<dl>");
		}

		if (summaryEnd != -1) {
			String summary = doc.substring(0, summaryEnd).trim();
			if (summary.isEmpty()) {
				summary = "<span style=\"color:#888\">No Javadoc summary.</span>";
			}
			return summary;
		} else
			return null;
	}

	@Override
	public void setFocus() {
		form.setFocus();
	}

	protected void doSaveReally() throws CoreException {
		assert isValidData();
		byte[] byteArr = serialization.serialize(root);
		IFileEditorInput fei = (IFileEditorInput) getEditorInput();
		if (fei.getFile().exists()) {
			fei.getFile().setContents(new ByteArrayInputStream(byteArr), false, true, null);
		} else {
			fei.getFile().create(new ByteArrayInputStream(byteArr), false, null);
		}
		dirty = false;
		firePropertyChange(PROP_DIRTY);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		try {
			doSaveReally();
		} catch (CoreException e) {
			ErrorDialog.openError(PlatformUI.getWorkbench().getModalDialogShellProvider().getShell(),
					"Couldn't save file", null, e.getStatus());
		}
	}

	@Override
	public void doSaveAs() {
		// TODO compare to AbstractDecoratedTextEditor.performSaveAs
		SaveAsDialog dlg = new SaveAsDialog(PlatformUI.getWorkbench().getModalDialogShellProvider().getShell());
		IFileEditorInput oldInput = (IFileEditorInput) getEditorInput();
		dlg.setOriginalFile(oldInput.getFile());
		dlg.create();
		if (dlg.open() == SaveAsDialog.CANCEL)
			return;
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(dlg.getResult());
		FileEditorInput input = new FileEditorInput(file);
		setInput(input);
		try {
			doSaveReally();
			firePropertyChange(PROP_INPUT);
			updateHeadline();
		} catch (CoreException e) {
			setInput(oldInput);
			ErrorDialog.openError(PlatformUI.getWorkbench().getModalDialogShellProvider().getShell(),
					"Couldn't save file", null, e.getStatus());
		}
	}

	public void makeDirty() {
		if (dirty)
			return;
		dirty = true;
		firePropertyChange(PROP_DIRTY);
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		// ignore
	}

	@Override
	public void widgetSelected(SelectionEvent evt) {
		try {
			String href = evt.text;
			if (href.startsWith(CLASS_URL_PREFIX)) {
				IJavaElement elem = getJavaProject().findType(href.substring(CLASS_URL_PREFIX.length()));
				JavaLinkHandler.openInEditor(elem);
			}
		} catch (Exception e) {
			// ignore
		}
	}

	public void confirmConversionReport() {
		makeDirty();
		conversionReport = null;
		createBody();
		setFocus();
	}

	public Serialization getSerialization() {
		return serialization;
	}
}
