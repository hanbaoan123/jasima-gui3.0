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

import jasima_gui.util.IOUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.Adler32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class EclipseProjectClassLoader extends ClassLoader implements IResourceChangeListener, IElementChangedListener {
	protected ArrayList<ClassLoaderListener> listeners = new ArrayList<>();
	protected IJavaProject project;
	protected HashMap<IPath, ArrayList<WatchedClass>> watchedResources = new HashMap<>();
	protected ClassLoaderState state = new ClassLoaderState();
	protected boolean classesChanged = false;

	public EclipseProjectClassLoader(IJavaProject project) {
		this.project = project;
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
		JavaCore.addElementChangedListener(this, ElementChangedEvent.POST_CHANGE);
	}

	public ClassLoaderState getState() {
		return state;
	}

	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
	}

	public void addListener(ClassLoaderListener listener) {
		listeners.add(listener);
	}

	public void removeListener(ClassLoaderListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void elementChanged(ElementChangedEvent event) {
		if (hasClasspathChanged(event.getDelta())) {
			state.setClassPathChanged(true);
			for (ClassLoaderListener listener : listeners) {
				listener.classPathChanged();
			}
		}
	}

	protected boolean hasClasspathChanged(IJavaElementDelta delta) {
		int t = delta.getElement().getElementType();
		if (t == IJavaElement.JAVA_PROJECT) {
			if ((delta.getFlags() & IJavaElementDelta.F_RESOLVED_CLASSPATH_CHANGED) != 0) {
				return true;
			}
		} else if (t == IJavaElement.JAVA_MODEL) {
			for (IJavaElementDelta d : delta.getAffectedChildren()) {
				if (hasClasspathChanged(d))
					return true;
			}
		}
		return false;
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		try {
			event.getDelta().accept(new IResourceDeltaVisitor() {
				@Override
				public boolean visit(IResourceDelta delta) throws CoreException {
					IPath path = delta.getFullPath().makeRelative();
					ArrayList<WatchedClass> res = watchedResources.get(path);
					if (res != null) {
						for (WatchedClass wc : res) {
							byte[] content = readResource(path, wc.name);
							long newsum = content == null ? 0xFFFFFFFF : checksum(content);
							state.markDirty(wc.name, newsum != wc.checksum);
							classesChanged = true;
						}
						return false; // doesn't matter, path points to a file
					}

					// determine if the path contains any watched resources
					for (IPath wp : watchedResources.keySet()) {
						if (path.isPrefixOf(wp)) {
							return true; // if yes, recurse
						}
					}
					return false; // if not, ignore all subdirectories
				}
			});
		} catch (CoreException e) {
			// ignore
		}

		if (classesChanged) {
			classesChanged = false;
			for (ClassLoaderListener listener : listeners) {
				listener.classesChanged();
			}
		}
	}

	protected Resource findResource(final String name, IJavaProject proj, boolean onlyExported) throws CoreException {
		IClasspathEntry[] classpath = proj.getResolvedClasspath(true);

		byte[] content;

		content = readResource(proj.getOutputLocation().makeRelative(), name);
		if (content != null) {
			return new Resource(proj.getOutputLocation().makeRelative(), content);
		}

		for (IClasspathEntry entry : classpath) {
			if (onlyExported && !entry.isExported())
				continue;

			if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
				content = readResource(entry.getPath(), name);
				if (content != null) {
					return new Resource(entry.getPath(), content);
				}
			} else if (entry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
				IProject projEntry = (IProject) ResourcesPlugin.getWorkspace().getRoot().findMember(entry.getPath());
				Resource result = findResource(name, JavaCore.create(projEntry), true);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}

	@Override
	protected URL findResource(String name) {
		return super.findResource(name);
	}

	protected byte[] readResource(IPath path, String name) {
		if (!path.isAbsolute()) {
			path = ResourcesPlugin.getWorkspace().getRoot().getFolder(path).getLocation();
		}

		if(path == null) {
			return null;
		}

		File f = path.toFile();
		if (f.isDirectory()) {
			f = new File(f, name);
			if (!f.exists())
				return null;
			try {
				return IOUtil.readFully(new FileInputStream(f));
			} catch (FileNotFoundException e) {
			}
		} else if (f.exists()) {
			try (ZipFile zf = new ZipFile(f)) {
				ZipEntry e = zf.getEntry(name);
				if (e != null) {
					return IOUtil.readFully(zf.getInputStream(e));
				}
			} catch (IOException e) {
			}
		}
		return null;
	}

	@Override
	protected Class<?> findClass(final String name) throws ClassNotFoundException {
		// should state.isDirty be checked here?
		try {
			String fileName = name.replace('.', '/').concat(".class");
			Resource res = findResource(fileName, project, false);
			if (res == null) {
				throw new ClassNotFoundException(name);
			}

			byte[] data = res.data;
			Class<?> retVal = defineClass(name, data, 0, data.length);

			if (!res.path.isAbsolute()) {
				ArrayList<WatchedClass> wr = watchedResources.get(res.path);
				if (wr == null) {
					wr = new ArrayList<WatchedClass>();
					watchedResources.put(res.path, wr);
				}
				WatchedClass wc = new WatchedClass(retVal, fileName, checksum(data));
				wr.add(wc);
			}

			return retVal;
		} catch (CoreException e) {
			throw new ClassNotFoundException(name, e);
		}
	}

	protected static long checksum(byte[] data) {
		Adler32 chk = new Adler32();
		chk.update(data);
		return chk.getValue();
	}

	protected static class Resource {
		final IPath path;
		final byte[] data;

		Resource() {
			path = null;
			data = null;
		}

		Resource(IPath path, byte[] data) {
			this.path = path;
			this.data = data;
		}
	}

	protected static class WatchedClass {
		final Class<?> klass;
		final String name;
		final long checksum;

		WatchedClass(Class<?> klass, String name, long checksum) {
			this.klass = klass;
			this.name = name;
			this.checksum = checksum;
		}
	}
}
