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

import static org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaLaunchDelegate;

public class SimulationLauncher extends JavaLaunchDelegate {
	// debug listener is used to refresh project when run is complete
	private static void addDebugEventListener(final ILaunchConfiguration conf) {
		final DebugPlugin debugPlugin = DebugPlugin.getDefault();
		debugPlugin.addDebugEventListener(new IDebugEventSetListener() {
			@Override
			public void handleDebugEvents(DebugEvent[] events) {
				for (DebugEvent evt : events) {
					if (evt.getKind() == DebugEvent.TERMINATE) {
						Object source = evt.getSource();
						if (source instanceof IProcess) {
							handleProcessTerminate((IProcess) source);
						}
					}
				}
			}

			private void handleProcessTerminate(IProcess proc) {
				if (!proc.getLaunch().getLaunchConfiguration().equals(conf))
					return;
				debugPlugin.removeDebugEventListener(this);
				try {
					String project = conf.getAttribute(ATTR_PROJECT_NAME,
							(String) null);
					if (project == null)
						return;
					ResourcesPlugin.getWorkspace().getRoot()
							.getProject(project)
							.refreshLocal(IResource.DEPTH_INFINITE, null);
				} catch (CoreException e) {
					// ignore
				}
			}
		});
	}

	@Override
	public IVMRunner getVMRunner(final ILaunchConfiguration launchCfg,
			String mode) throws CoreException {
		addDebugEventListener(launchCfg);
		return super.getVMRunner(launchCfg, mode);
	}
}
