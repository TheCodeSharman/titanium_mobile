/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#if defined(USE_TI_XML) || defined(USE_TI_NETWORK)

#import "TiProxy.h"
#import "GDataXMLNode.h"
#import "TiDOMElementProxy.h"

@interface TiDOMNamedNodeMapProxy : TiProxy {
@private
	TiDOMElementProxy* element;
}

@property(nonatomic,readonly) NSNumber* length;

-(void)setElement:(TiDOMElementProxy*)element;

@end

#endif