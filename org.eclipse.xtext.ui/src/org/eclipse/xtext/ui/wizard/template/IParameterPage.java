/*******************************************************************************
 * Copyright (c) 2018 itemis AG (http://www.itemis.de) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.ui.wizard.template;

import org.eclipse.swt.widgets.Composite;

/**
 * Interface to avoid passing concrete implementation of parameter page to {@link ProjectVariable}.
 * 
 * @author Arne Deutsch - Initial contribution and API
 * @since 2.14
 */
public interface IParameterPage {

	/**
	 * Callback to update the parameter page after a variable has changed.
	 */
	void update();

	/**
	 * Parent element for all the parameter widgets.
	 */
	Composite getControl();

}
