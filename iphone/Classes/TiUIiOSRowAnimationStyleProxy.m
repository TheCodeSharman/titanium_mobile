/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UIIOSROWANIMATIONSTYLE

#import "TiUIiOSRowAnimationStyleProxy.h"
#import "TiBase.h"

@implementation TiUIiOSRowAnimationStyleProxy

-(NSString*)apiName
{
    return @"Ti.UI.iPhone.RowAnimationStyle";
}

MAKE_SYSTEM_PROP(NONE,UITableViewRowAnimationNone);
MAKE_SYSTEM_PROP(LEFT,UITableViewRowAnimationLeft);
MAKE_SYSTEM_PROP(RIGHT,UITableViewRowAnimationRight);
MAKE_SYSTEM_PROP(TOP,UITableViewRowAnimationTop);
MAKE_SYSTEM_PROP(BOTTOM,UITableViewRowAnimationBottom);
MAKE_SYSTEM_PROP(FADE,UITableViewRowAnimationFade);

// used prior 0.9 in KS
MAKE_SYSTEM_PROP(UP,UITableViewRowAnimationTop);
MAKE_SYSTEM_PROP(DOWN,UITableViewRowAnimationBottom);

@end

#endif