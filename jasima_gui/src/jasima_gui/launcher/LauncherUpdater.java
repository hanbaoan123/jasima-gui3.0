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

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

public class LauncherUpdater implements IResourceChangeListener, IResourceDeltaVisitor {

	public LauncherUpdater() {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
	}

	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta = event.getDelta();
		if (delta == null)
			return;
		try {
			delta.accept(this);
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean visit(IResourceDelta delta) throws CoreException {
		IPath movedTo = delta.getMovedToPath();
		if (movedTo != null) {
			ILaunchConfiguration config = SimulationLaunchShortcut.findConfiguration(delta.getResource());
			if (config != null) {
				ILaunchConfigurationWorkingCopy wc = config.getWorkingCopy();
				ArrayList<String> args = Arguments.parseArgs(wc);
				Arguments outArgs = new Arguments();
				IFile newFile = ResourcesPlugin.getWorkspace().getRoot().getFile(movedTo);
				boolean argFound = false;
				for (String arg : args) {
					if (argFound) {
						outArgs.append(arg);
					} else if (arg.startsWith("--")) {
						outArgs.append(arg);
					} else {
						argFound = true;
						outArgs.append(newFile.getProjectRelativePath().toString());
					}
				}
				wc.setMappedResources(new IResource[] { newFile });
				wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, outArgs.toString());
				wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, newFile.getProject().getName());
				wc.doSave();
			}
		} else if (delta.getKind() == IResourceDelta.REMOVED) {
			ILaunchConfiguration config = SimulationLaunchShortcut.findConfiguration(delta.getResource());
			if (config != null) {
				config.delete();
			}
		}
		return true;
	}
}
