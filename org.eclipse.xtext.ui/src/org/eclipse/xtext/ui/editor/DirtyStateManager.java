/*******************************************************************************
 * Copyright (c) 2009 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.ui.editor;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.IExternalContentSupport.IExternalContentProvider;
import org.eclipse.xtext.resource.IResourceDescription;
import org.eclipse.xtext.resource.IResourceDescription.Delta;
import org.eclipse.xtext.resource.impl.AbstractResourceDescriptionChangeEventSource;
import org.eclipse.xtext.resource.impl.ResourceDescriptionChangeEvent;
import org.eclipse.xtext.resource.persistence.ResourceStorageLoadable;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.MapMaker;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 */
// TODO: batch events according to the contract of IDirtyStateManager
public class DirtyStateManager extends AbstractResourceDescriptionChangeEventSource implements IDirtyStateManager2, IDirtyStateManagerExtension {

	private ConcurrentMap<URI, IDirtyResource> managedResources;
	private ConcurrentMap<URI, IDirtyResource> discardCandidates;
	
	public DirtyStateManager() {
		managedResources = new MapMaker().makeMap();
		discardCandidates = new MapMaker().makeMap();
	}
	
	@Override
	public void announceDirtyStateChanged(IDirtyResource dirtyResource) {
		// avoid putting a dirtyResource into the map that wasn't managed before
		URI uri = dirtyResource.getURI();
		if (managedResources.replace(uri, dirtyResource) != null) {
			// the resource is dirty and may not be discarded, ensure that it's 
			// not in the list to be discarded 
			discardCandidates.remove(uri);
			notifyListeners(dirtyResource, true);
		}
	}

	@Override
	public void discardDirtyState(IDirtyResource dirtyResource) {
		// flag dirtyResource to be discarded but don't really do it until index updated this resource
		// so that even with auto build = off, we see the newest references 
		discardCandidates.put(dirtyResource.getURI(), dirtyResource);
	}

	protected void notifyListeners(final IDirtyResource dirtyResource, boolean managed) {
		if (managed) {
			IResourceDescription.Delta delta = new IResourceDescription.Delta() {
				@Override
				public boolean haveEObjectDescriptionsChanged() {
					return true;
				}
				
				@Override
				public IResourceDescription getOld() {
					return null;
				}
				
				@Override
				public IResourceDescription getNew() {
					return dirtyResource.getDescription();
				}

				@Override
				public URI getUri() {
					return dirtyResource.getURI();
				}
			};
			notifyListeners(new ResourceDescriptionChangeEvent(Collections.singletonList(delta)));
		} else {
			IResourceDescription.Delta delta = new IResourceDescription.Delta() {
				@Override
				public boolean haveEObjectDescriptionsChanged() {
					return true;
				}
				
				@Override
				public IResourceDescription getOld() {
					return dirtyResource.getDescription();
				}
				
				@Override
				public IResourceDescription getNew() {
					return null;
				}
				@Override
				public URI getUri() {
					return dirtyResource.getURI();
				}
			};
			notifyListeners(new ResourceDescriptionChangeEvent(Collections.singletonList(delta)));
		}
	}

	@Override
	public boolean manageDirtyState(IDirtyResource dirtyResource) {
		IDirtyResource prevValue = managedResources.putIfAbsent(dirtyResource.getURI(), dirtyResource);
		return prevValue == null || prevValue == dirtyResource;
	}
	
	@Override
	public void unmanageDirtyState(IDirtyResource dirtyResource) {
		URI uri = dirtyResource.getURI();
		discardCandidates.remove(uri);
		if (managedResources.remove(uri, dirtyResource)) {
			notifyListeners(dirtyResource, false);
		}
	}
	
	public IDirtyResource getDirtyResource(URI uri) {
		return managedResources.get(uri);
	}
	
	@Override
	public IResourceDescription getDirtyResourceDescription(URI uri) {
		IDirtyResource dirtyResource = getDirtyResource(uri);
		if (dirtyResource != null) {
			if (dirtyResource instanceof IDirtyResource.InitializationAware) {
				return ((IDirtyResource.InitializationAware) dirtyResource).getDescriptionIfInitialized();
			}
			return dirtyResource.getDescription();
		}
		return null;
	}

	@Override
	public String getContent(URI uri) {
		IDirtyResource dirtyResource = findDirtyResourcebyURIorNormalizedURI(uri);
		if (dirtyResource != null) {
			if (dirtyResource instanceof IDirtyResource.InitializationAware) {
				return ((IDirtyResource.InitializationAware) dirtyResource).getContentsIfInitialized();
			}
			return dirtyResource.getContents();
		}
		return null;
	}
	
	/**
	 * @since 2.8
	 * @noreference This method is not intended to be referenced by clients.
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 */
	public ResourceStorageLoadable getResourceStorageLoadable(URI uri) {
		IDirtyResource dirtyResource = findDirtyResourcebyURIorNormalizedURI(uri);
		if (dirtyResource instanceof IDirtyResource.ICurrentStateProvidingExtension) {
			return ((IDirtyResource.ICurrentStateProvidingExtension)dirtyResource).getResourceStorageLoadable();
		}
		return null;
	}

	/**
	 * @since 2.4
	 */
	protected IDirtyResource findDirtyResourcebyURIorNormalizedURI(URI uri) {
		IDirtyResource dirtyResource = managedResources.get(uri);
		if (dirtyResource == null) {
			Iterator<IDirtyResource> iterator = managedResources.values().iterator();
			while (dirtyResource == null && iterator.hasNext()) {
				IDirtyResource res = iterator.next();
				if (res instanceof IDirtyResource.NormalizedURISupportExtension) {
					URI normalizedURI = ((IDirtyResource.NormalizedURISupportExtension) res).getNormalizedURI();
					if (normalizedURI.equals(uri)) {
						dirtyResource = res;
					}
				}
			}
		}
		return dirtyResource;
	}
	
	@Override
	public IExternalContentProvider getActualContentProvider() {
		return new IExternalContentProvider() {
			
			@Override
			public boolean hasContent(URI uri) {
				return DirtyStateManager.this.hasContent(uri);
			}
			
			@Override
			public String getContent(URI uri) {
				IDirtyResource dirtyResource = DirtyStateManager.this.findDirtyResourcebyURIorNormalizedURI(uri);
				if (dirtyResource != null) {
					if (dirtyResource instanceof IDirtyResource.InitializationAware) {
						return ((IDirtyResource.InitializationAware) dirtyResource).getActualContentsIfInitialized();
					}
					return dirtyResource.getActualContents();
				}
				return null;
			}
			
			@Override
			public IExternalContentProvider getActualContentProvider() {
				return this;
			}
		};
	}

	@Override
	public boolean hasContent(URI uri) {
		return findDirtyResourcebyURIorNormalizedURI(uri) != null;
	}

	@Override
	public boolean isEmpty() {
		return managedResources.isEmpty();
	}
	
	@Override
	public Iterable<IEObjectDescription> getExportedObjects() {
		return Iterables.concat(Iterables.transform(managedResources.values(), new Function<IDirtyResource, Iterable<IEObjectDescription>>() {
			@Override
			public Iterable<IEObjectDescription> apply(IDirtyResource from) {
				if (from != null)
					return from.getDescription().getExportedObjects();
				return Collections.emptyList();
			}
		}));
	}
	
	@Override
	public Iterable<IEObjectDescription> getExportedObjects(final EClass type, final QualifiedName name, final boolean ignoreCase) {
		return Iterables.concat(Iterables.transform(managedResources.values(), new Function<IDirtyResource, Iterable<IEObjectDescription>>() {
			@Override
			public Iterable<IEObjectDescription> apply(IDirtyResource from) {
				if (from != null)
					return from.getDescription().getExportedObjects(type, name, ignoreCase);
				return Collections.emptyList();
			}
		}));
	}
	
	@Override
	public Iterable<IEObjectDescription> getExportedObjectsByObject(EObject object) {
		URI resourceURI = EcoreUtil2.getPlatformResourceOrNormalizedURI(object).trimFragment();
		IDirtyResource dirtyResource = getDirtyResource(resourceURI);
		if (dirtyResource != null) {
			return dirtyResource.getDescription().getExportedObjectsByObject(object);
		}
		return Collections.emptyList();
	}
	
	@Override
	public Iterable<IEObjectDescription> getExportedObjectsByType(final EClass type) {
		return Iterables.concat(Iterables.transform(managedResources.values(), new Function<IDirtyResource, Iterable<IEObjectDescription>>() {
			@Override
			public Iterable<IEObjectDescription> apply(IDirtyResource from) {
				if (from != null)
					return from.getDescription().getExportedObjectsByType(type);
				return Collections.emptyList();
			}
		}));
	}

	/**
	 * @since 2.7
	 */
	@Override
	public List<URI> getDirtyResourceURIs() {
		return ImmutableList.copyOf(managedResources.keySet());
	}
	
	@Override
	public void indexUpdated(List<Delta> indexChanges) {
		// builder's index updated some resources
		indexChanges.forEach(delta -> {
			IDirtyResource dirtyResource = discardCandidates.get(delta.getUri());
			// see if this resource was flagged to be discarded
			if (dirtyResource != null) {
				unmanageDirtyState(dirtyResource);
			}
		});
	}
}
