/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#import "TiBase.h"

#ifdef USE_TI_UIIPADSPLITWINDOW

#import "TiUIiPadSplitWindowProxy.h"
#import "TiUIiPadSplitWindow.h"
#import "TiUtils.h"
#import "TiApp.h"

@implementation TiUIiPadSplitWindowProxy

-(TiUIView*)newView
{
	return [[TiUIiPadSplitWindow alloc] init];
}

- (UIViewController *)childViewController
{
	return [(TiUIiPadSplitWindow*)[self view] controller];
}

-(void)windowDidOpen 
{
	[super windowDidOpen];
	[self reposition];
}

-(void)windowWillClose
{
    if ([self viewAttached]) {
        [(TiUIiPadSplitWindow*)[self view] splitViewController:nil willShowViewController:nil invalidatingBarButtonItem:nil];
    }
}

-(void)setToolbar:(id)items withObject:(id)properties
{
	ENSURE_UI_THREAD_WITH_OBJ(setToolbar,items,properties);
	[(TiUIiPadSplitWindow*)[self view] setToolbar:items withObject:properties];
}


-(void)setDetailView:(id<NSObject,TiOrientationController>)newDetailView
{
	ENSURE_UI_THREAD_1_ARG(newDetailView);
	if (newDetailView == detailView)
	{
		return;
	}
	[detailView setParentOrientationController:nil];
	[newDetailView setParentOrientationController:self];
	RELEASE_AND_REPLACE(detailView,newDetailView);
	[self replaceValue:newDetailView forKey:@"detailView" notification:YES];
}

-(TiOrientationFlags)orientationFlags
{
    // Not even WE follow this convention in the documentation, so we need to make sure
    // there's a failsafe. Note that because of how orienting works (views pick up their
    // parent's orientation) we need to query the splitview's orientation FIRST.
    
    TiOrientationFlags orientations = [super orientationFlags];
    if (orientations == TiOrientationNone) {
        orientations = [detailView orientationFlags];
    }
	return orientations;
}

-(BOOL)_handleClose:(id)args
{
    // Ensure popup isn't visible so it can be dealloced
	[(TiUIiPadSplitWindow*)[self view] setMasterPopupVisible_:NO];
    
    return [super _handleClose:args];
}

-(void)viewWillAppear:(BOOL)animated
{
    if ([self viewAttached]) {
        [[(TiUIiPadSplitWindow*)[self view] controller] viewWillAppear:animated];
    }
    [super viewWillAppear:animated];
}
-(void)viewWillDisappear:(BOOL)animated
{
    if ([self viewAttached]) {
        [[(TiUIiPadSplitWindow*)[self view] controller] viewWillDisappear:animated];
    }
    [super viewWillDisappear:animated];
}

-(void)viewDidAppear:(BOOL)animated
{
    if ([self viewAttached]) {
        [[(TiUIiPadSplitWindow*)[self view] controller] viewDidAppear:animated];
    }
    [super viewDidAppear:animated];
}
-(void)viewDidDisappear:(BOOL)animated
{
    if ([self viewAttached]) {
        [[(TiUIiPadSplitWindow*)[self view] controller] viewDidDisappear:animated];
    }
    [super viewDidDisappear:animated];
    
}
-(void)willAnimateRotationToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration
{
    if ([self viewAttached]) {
        [[(TiUIiPadSplitWindow*)[self view] controller] willAnimateRotationToInterfaceOrientation:toInterfaceOrientation duration:duration];
    }
    [super willAnimateRotationToInterfaceOrientation:toInterfaceOrientation duration:duration];
}
-(void)willRotateToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration
{
    if ([self viewAttached]) {
        [[(TiUIiPadSplitWindow*)[self view] controller] willRotateToInterfaceOrientation:toInterfaceOrientation duration:duration];
    }
    [super willRotateToInterfaceOrientation:toInterfaceOrientation duration:duration];
}
-(void)didRotateFromInterfaceOrientation:(UIInterfaceOrientation)fromInterfaceOrientation
{
    if ([self viewAttached]) {
        [[(TiUIiPadSplitWindow*)[self view] controller] didRotateFromInterfaceOrientation:fromInterfaceOrientation];
    }
    [super didRotateFromInterfaceOrientation:fromInterfaceOrientation];
}

@end

#endif
