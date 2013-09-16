/*******************************************************************************
 * Copyright (c) 2013 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.junit4.internal;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.texteditor.MarkerUtilities;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.*;
import org.junit.Test;

/**
 * @author dhuebner - Initial contribution and API
 */
public class InternalBuilderTest {

	@Test
	public void test() throws CoreException {
		final IMarker[] markers = ResourcesPlugin.getWorkspace().getRoot()
				.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
		List<String> errors = new ArrayList<String>();
		for (IMarker marker : markers) {
			String msg = MarkerUtilities.getMessage(marker);
			if (MarkerUtilities.getSeverity(marker) == IMarker.SEVERITY_ERROR) {
				errors.add(msg);
			}
		}

		List<String> top10;
		if (errors.size() > 10) {
			top10 = toList(take(errors, 10));
		} else {
			top10 = errors;
		}
		assertTrue("Problems found (" + top10.size() + " from " + errors.size() + "): " + join(errors, ", "),
				errors.isEmpty());
	}

}