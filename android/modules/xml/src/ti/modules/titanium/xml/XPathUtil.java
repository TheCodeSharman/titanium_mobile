/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.xml;

import java.util.ArrayList;
import java.util.List;

import org.appcelerator.titanium.util.Log;
import org.jaxen.JaxenException;
import org.jaxen.XPath;
import org.jaxen.dom.DOMXPath;

public class XPathUtil {

	private static final String LCAT = "XPath";
	
	public static XPathNodeListProxy evaluate(NodeProxy start, String xpathExpr)
	{
		try {
			XPath xpath = new DOMXPath(xpathExpr);
			List nodes= xpath.selectNodes(start.getNode());
			
			return new XPathNodeListProxy(start.getTiContext(), nodes);
		} catch (JaxenException e) {
			Log.e(LCAT, "Exception selecting nodes in XPath ("+xpathExpr+")", e);
		}
		
		return new XPathNodeListProxy(start.getTiContext(), new ArrayList());
	}
}
