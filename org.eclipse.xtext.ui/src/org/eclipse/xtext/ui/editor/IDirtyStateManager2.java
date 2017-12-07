/*******************************************************************************
 * Copyright (c) 2018 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.ui.editor;

import org.eclipse.emf.common.util.URI;

/**
 * @author prietom - Initial contribution and API
 * 
 * This interface changes the meaning of the discardDirtyState method which is used in {@link IDirtyStateManager}
 * to remove the resource from the DirtyAwareResourceDescriptions. This action happens now when the new method
 * unmanageDirtyState gets called.
 * The method discardDirtyState should be called when the dirty state of the resource should get discarded because
 * it is not valid/needed.
 * The method unmanageDirtyState should be called when the descriptions changed in the dirty state are merged with
 * the index, indicating this way, that this resource has no dirty state anymore.
 */
public interface IDirtyStateManager2 extends IDirtyStateManager {
	/**
	 * Mark the given dirty resource as unmanaged. This method may be called even if
	 * manageDirtyState has not been called before. Another dirty resource with the
	 * same {@link URI} will not become unmanaged. A call to this method will raise an event.
	 * @see #announceDirtyStateChanged(IDirtyResource)
	 */
	void unmanageDirtyState(IDirtyResource dirtyResource);
	
	/**
	 * Mark the given dirty resource as to be discarded. The dirty resource will be kept until
	 * the index for it gets updated so that the new state is available even when auto build is off.
	 */
	@Override
	void discardDirtyState(IDirtyResource dirtyResource);
}
