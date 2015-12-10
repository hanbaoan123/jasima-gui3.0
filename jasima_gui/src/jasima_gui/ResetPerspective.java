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
package jasima_gui;

import jasima_gui.pref.Pref;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;

/**
 * The activator class controls the plug-in life cycle
 */
public class ResetPerspective implements IStartup {

	@Override
	public void earlyStartup() {
		if (Pref.FIRST_RUN.val()) {
			Pref.FIRST_RUN.set(false);
			final IWorkbench wb = PlatformUI.getWorkbench();
			wb.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					try {
						IWorkbenchWindow wbw = wb.getActiveWorkbenchWindow();
						wb.showPerspective("org.eclipse.jdt.ui.JavaPerspective", wbw);
						List<String> wizardShortcuts = Arrays.asList(wbw.getActivePage().getNewWizardShortcuts());
						if (wizardShortcuts.contains("jasima_gui.wizards.JasimaNewProjectWizard")) {
							// the perspective already has the jasima wizards, no need to do anything
							return;
						}
						if (!MessageDialog.openConfirm(wbw.getShell(), "Reset perspective",
								"To complete installation of the jasima plugin, the Java perspective will be reset.")) {
							return;
						}
						wbw.getActivePage().resetPerspective();
					} catch (WorkbenchException e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}
				}
			});
		}
	}

}
