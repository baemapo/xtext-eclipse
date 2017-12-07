/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.ui.tests.editor;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.resource.IResourceDescription;
import org.eclipse.xtext.resource.IResourceDescription.Event;
import org.eclipse.xtext.ui.editor.DirtyStateManager;
import org.eclipse.xtext.ui.editor.IDirtyResource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author prietom - Initial contribution and API
 */
public class DirtyStateManagerTest {
	
	private DirtyStateManager dirtyStateManager;
	private MockListener listener;
	
	
	@Before
	public void setUp() {
		listener = new MockListener();
		dirtyStateManager = new DirtyStateManager();
		listener = new MockListener();
	}
	
	@Test
	public void testIndexUpdateNotification() {
		IDirtyResource dirtyResource = createDirtyResource();
		dirtyStateManager.announceDirtyStateChanged(dirtyResource);
		Assert.assertTrue(listener.isNotified());
		Assert.assertEquals(1, listener.getTimesNotified());
	}

	private IDirtyResource createDirtyResource() {
		URI uri = URI.createURI("aFileURI");
		return new IDirtyResource() {

			@Override
			public String getContents() {
				return null;
			}

			@Override
			public String getActualContents() {
				return null;
			}

			@Override
			public IResourceDescription getDescription() {
				return null;
			}

			@Override
			public URI getURI() {
				return uri;
			}
		};
	}
	
	private class MockListener implements IResourceDescription.Event.Listener {
		private boolean notified = false;
		private AtomicInteger notifiedCount = new AtomicInteger(0);
		
		public boolean isNotified() {
			return notified;			
		}
		
		public int getTimesNotified() {
			return notifiedCount.get();
		}
		
		@Override
		public void descriptionsChanged(Event event) {
			notified = true;
			notifiedCount.incrementAndGet();
		}		
	}
}
