/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UIDASHBOARDVIEW

#import "TiUIView.h"
#import "LauncherView.h"

@interface TiUIDashboardView : TiUIView<LauncherViewDelegate> {

@private
	LauncherView *launcher;
    NSMutableArray *allItems;

}

-(LauncherView*)launcher;
-(void)setViewData:(NSArray*)data;
-(NSArray *)allItems;
@end


#endif