/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UITAB

#import "TiUIView.h"

//To handle the more tab, we're a delegate of it.
@class TiUITabProxy;
@interface TiUITabGroup : TiUIView<UITabBarControllerDelegate,UINavigationControllerDelegate> {
@private
	UITabBarController *controller;
	TiUITabProxy *focusedTabProxy;
	BOOL allowConfiguration;
	NSString* editTitle;
	
	TiColor *barColor;
	TiColor *navTintColor;
	NSMutableDictionary *theAttributes;
}

-(UITabBarController*)tabController;

-(void)open:(id)args;
-(void)close:(id)args;

-(UITabBar*)tabbar;

/*
 //Removed Orientation

-(void)willAnimateRotationToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration;
-(void)willRotateToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration;
-(void)didRotateFromInterfaceOrientation:(UIInterfaceOrientation)fromInterfaceOrientation;
*/

- (void)viewWillTransitionToSize:(CGSize)size withTransitionCoordinator:(id <UIViewControllerTransitionCoordinator>)coordinator;
@end

#endif
