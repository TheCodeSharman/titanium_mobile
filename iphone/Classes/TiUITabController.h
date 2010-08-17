/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UITAB

#import "TiWindowProxy.h"
#import "TiUITabProxy.h"
#import "TiTabController.h"

@interface TiUITabController : TiWindowViewController<TiTabController> {
@private
//Window is now the superclass's proxy.
	TiUITabProxy *tab;
}

-(id)initWithProxy:(TiWindowProxy*)proxy tab:(TiUITabProxy*)tab;
@property(nonatomic,readonly)	TiWindowProxy *window;
@property(nonatomic,readonly)	TiUITabProxy *tab;

@end

#endif