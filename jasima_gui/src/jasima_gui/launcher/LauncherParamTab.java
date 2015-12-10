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
package jasima_gui.launcher;

import static org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME;
import static org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS;
import static org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS;
import jasima_gui.Activator;
import jasima_gui.util.EnablingSelectionListener;
import jasima_gui.util.TypeUtil;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaLaunchTab;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class LauncherParamTab extends JavaLaunchTab {

	protected Combo launcherClass;
	protected Text experimentFile;
	protected Combo logLevel;

	protected Button printNoResults;
	protected Button saveXML;
	protected Combo saveXMLPath;
	protected Button saveXLS;
	protected Combo saveXLSPath;

	protected Text additionalArgs;
	protected Text vmArgs;

	protected final ChangeListener updater = new ChangeListener();
	protected final Image jasimaIcon = Activator.getImageDescriptor("icons/jasima.png").createImage();

	protected class ChangeListener extends SelectionAdapter implements ModifyListener {
		@Override
		public void modifyText(ModifyEvent e) {
			scheduleUpdateJob();
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			scheduleUpdateJob();
		}
	}

	protected static GridDataFactory indentFactory() {
		return GridDataFactory.swtDefaults().indent(20, 0);
	}

	protected static void addTitleLabel(Composite parent, String text) {
		StyledText lbl = new StyledText(parent, SWT.NONE);
		lbl.setBackground(parent.getBackground());
		lbl.setText(text);
		StyleRange range = new StyleRange();
		range.start = 0;
		range.length = text.length();
		range.fontStyle = SWT.BOLD;
		lbl.setStyleRange(range);
		GridDataFactory.swtDefaults().span(2, 1).applyTo(lbl);
	}

	protected static void addIndentedLabel(Composite parent, String text) {
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText(text);
		indentFactory().align(SWT.LEAD, SWT.CENTER).applyTo(lbl);
	}

	@Override
	public void createControl(Composite parent) {
		Composite comp = new Composite(parent, SWT.NO_RADIO_GROUP);
		setControl(comp);
		comp.setLayout(new GridLayout(2, false));

		addTitleLabel(comp, "Input");

		addIndentedLabel(comp, "Launcher class:");
		launcherClass = new Combo(comp, SWT.BORDER);
		launcherClass.add(TypeUtil.XLS_RUNNER_CLASS);
		launcherClass.add(TypeUtil.XML_RUNNER_CLASS);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(launcherClass);
		launcherClass.addModifyListener(updater);

		addIndentedLabel(comp, "Experiment file:");
		experimentFile = new Text(comp, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(experimentFile);
		experimentFile.addModifyListener(updater);

		addTitleLabel(comp, "Logging");

		addIndentedLabel(comp, "Log level:");
		logLevel = new Combo(comp, SWT.BORDER);
		logLevel.setItems(new String[] { "OFF", "ERROR", "WARN", "INFO", "TRACE", "ALL" });
		GridDataFactory.fillDefaults().applyTo(logLevel);
		logLevel.addModifyListener(updater);

		addTitleLabel(comp, "Results");

		printNoResults = new Button(comp, SWT.CHECK);
		printNoResults.setText("Do not print results to console");
		printNoResults.addSelectionListener(updater);
		indentFactory().span(2, 1).applyTo(printNoResults);

		saveXML = new Button(comp, SWT.CHECK);
		saveXML.setText("Save results to XML file:");
		saveXML.addSelectionListener(updater);
		indentFactory().applyTo(saveXML);
		saveXMLPath = new Combo(comp, SWT.BORDER);
		saveXMLPath.add("(default file name)");
		GridDataFactory.fillDefaults().applyTo(saveXMLPath);
		saveXMLPath.addModifyListener(updater);
		saveXML.addSelectionListener(new EnablingSelectionListener(saveXMLPath));

		saveXLS = new Button(comp, SWT.CHECK);
		saveXLS.setText("Save results to XLS file:");
		saveXLS.addSelectionListener(updater);
		indentFactory().applyTo(saveXLS);
		saveXLSPath = new Combo(comp, SWT.BORDER);
		saveXLSPath.add("(default file name)");
		GridDataFactory.fillDefaults().applyTo(saveXLSPath);
		saveXLSPath.addModifyListener(updater);
		saveXLS.addSelectionListener(new EnablingSelectionListener(saveXLSPath));

		addTitleLabel(comp, "Other arguments");

		addIndentedLabel(comp, "Additional launcher arguments:");
		additionalArgs = new Text(comp, SWT.BORDER);
		GridDataFactory.fillDefaults().applyTo(additionalArgs);
		additionalArgs.addModifyListener(updater);

		addIndentedLabel(comp, "Java VM arguments:");
		vmArgs = new Text(comp, SWT.BORDER);
		GridDataFactory.fillDefaults().applyTo(vmArgs);
		vmArgs.addModifyListener(updater);
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(ATTR_PROGRAM_ARGUMENTS, "");
		configuration.setAttribute(ATTR_VM_ARGUMENTS, "");
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(ATTR_MAIN_TYPE_NAME, launcherClass.getText());

		Arguments args = new Arguments();
		args.append(experimentFile.getText());

		args.append("--log=" + logLevel.getText());
		
		if (printNoResults.getSelection())
			args.append("--nores");
		
		if (saveXML.getSelection()) {
			String path = saveXMLPath.getText();
			if (path.equals(saveXMLPath.getItem(0))) { // default
				args.append("--xmlres");
			} else {
				args.append("--xmlres=" + path);
			}
		}
		
		if (saveXLS.getSelection()) {
			String path = saveXLSPath.getText();
			if (path.equals(saveXLSPath.getItem(0))) { // default
				args.append("--xlsres");
			} else {
				args.append("--xlsres=" + path);
			}
		}

		String extraArgs = additionalArgs.getText().trim();
		if (!extraArgs.isEmpty()) {
			args.appendUnquoted(extraArgs);
		}

		configuration.setAttribute(ATTR_PROGRAM_ARGUMENTS, args.toString());
		configuration.setAttribute(ATTR_VM_ARGUMENTS, vmArgs.getText());
	}

	@Override
	public void initializeFrom(ILaunchConfiguration config) {
		super.initializeFrom(config);
		try {
			launcherClass.setText(config.getAttribute(ATTR_MAIN_TYPE_NAME, ""));
			vmArgs.setText(config.getAttribute(ATTR_VM_ARGUMENTS, ""));
			ArrayList<String> argsArr = Arguments.parseArgs(config);

			experimentFile.setText("");
			logLevel.setText("INFO");
			printNoResults.setSelection(false);
			saveXML.setSelection(false);
			saveXMLPath.select(0);
			saveXLS.setSelection(false);
			saveXLSPath.select(0);

			boolean hasFilename = false;
			Arguments unparsed = new Arguments();
			for (final String arg : argsArr) {
				String[] s = arg.split("=", 2);

				if (s[0].equals("--log")) {
					if (s.length > 1)
						logLevel.setText(s[1]);
				} else if (s[0].equals("--nores")) {
					printNoResults.setSelection(true);
				} else if (s[0].equals("--xmlres")) {
					saveXML.setSelection(true);
					if (s.length > 1)
						saveXMLPath.setText(s[1]);
					else
						saveXMLPath.select(0);
				} else if (s[0].equals("--xlsres")) {
					saveXLS.setSelection(true);
					if (s.length > 1)
						saveXLSPath.setText(s[1]);
					else
						saveXLSPath.select(0);
				} else if (!hasFilename) {
					experimentFile.setText(arg);
					hasFilename = true;
				} else {
					unparsed.append(arg);
				}
			}
			additionalArgs.setText(unparsed.toString());
		} catch (CoreException e) {
			throw new RuntimeException(e);
		} finally {
			saveXMLPath.setEnabled(saveXML.getSelection());
			saveXLSPath.setEnabled(saveXLS.getSelection());
		}
	}

	@Override
	public String getName() {
		return "Jasima";
	}

	@Override
	public Image getImage() {
		return jasimaIcon;
	}
}
