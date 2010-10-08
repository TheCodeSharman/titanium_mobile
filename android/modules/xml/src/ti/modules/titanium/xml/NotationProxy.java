/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.xml;

import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiContext;
import org.w3c.dom.Notation;

@Kroll.proxy
public class NotationProxy extends NodeProxy {

	private Notation notation;
	public NotationProxy(TiContext context, Notation notation)
	{
		super(context, notation);
		this.notation = notation;
	}
	
	@Kroll.getProperty @Kroll.method
	public String getPublicId() {
		return notation.getPublicId();
	}
	
	@Kroll.getProperty @Kroll.method
	public String getSystemId() {
		return notation.getSystemId();
	}
}
