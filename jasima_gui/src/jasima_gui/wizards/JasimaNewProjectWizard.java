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
package jasima_gui.wizards;

import jasima_gui.pref.Pref;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.jdt.IClasspathManager;
import org.eclipse.m2e.jdt.MavenJdtPlugin;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

public class JasimaNewProjectWizard extends Wizard implements INewWizard, IExecutableExtension {

	private IConfigurationElement extensionConfigElement;
	private NewJavaProjectWizardPageOne mainPage;
	private NewJavaProjectWizardPageTwo lastPage;

	public JasimaNewProjectWizard() {
		mainPage = new NewJavaProjectWizardPageOne();
		lastPage = new NewJavaProjectWizardPageTwo(mainPage);
		setWindowTitle("New Jasima Project");
	}

	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
			throws CoreException {
		extensionConfigElement = config;
	}

	public void addPages() {
		addPage(mainPage);
		addPage(lastPage);
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		IWorkbenchPart activePart = null;
		IWorkbenchWindow win = workbench.getActiveWorkbenchWindow();
		if (win != null) {
			IWorkbenchPage page = win.getActivePage();
			if (page != null) {
				activePart = page.getActivePart();
			}
		}
		mainPage.init(selection, activePart);
	}

	public boolean canFinish() {
		return mainPage.canFlipToNextPage();
	}

	public boolean performFinish() {
		try {
			lastPage.performFinish(new NullProgressMonitor());
			IJavaProject proj = lastPage.getJavaProject();
			String line;
			BufferedReader rdr = new BufferedReader(
					new InputStreamReader(JasimaNewProjectWizard.class
							.getResourceAsStream("default_pom.xml"), "utf8"));
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			OutputStreamWriter wrtr = new OutputStreamWriter(bos, "utf8");
			StringBuffer newLine = new StringBuffer();
			while ((line = rdr.readLine()) != null) {
				newLine.setLength(0);
				Pattern var = Pattern.compile("\\$\\{([A-Z_]+)\\}");
				Matcher m = var.matcher(line);
				while (m.find()) {
					String val = m.group(1);
					if (val.equals("PROJECT_NAME")) {
						val = proj.getProject().getName();
					} else if (val.equals("JASIMA_VERSION")) {
						val = Pref.JASIMA_VERSION.val();
					} else {
						val = "$0"; // do not change
					}
					m.appendReplacement(newLine, val);
				}
				m.appendTail(newLine);
				newLine.append('\n');
				wrtr.append(newLine);
			}
			wrtr.flush();
			proj.getProject()
					.getFile("pom.xml")
					.create(new ByteArrayInputStream(bos.toByteArray()), true,
							new NullProgressMonitor());
			MavenPlugin.getProjectConfigurationManager().enableMavenNature(
					proj.getProject(), new ResolverConfiguration(),
					new NullProgressMonitor());
			IClasspathManager bpm = MavenJdtPlugin.getDefault().getBuildpathManager();
			bpm.scheduleDownload(proj.getProject(), true, true);

			BasicNewProjectResourceWizard.updatePerspective(extensionConfigElement);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public boolean performCancel() {
		lastPage.performCancel();
		return super.performCancel();
	}

}
