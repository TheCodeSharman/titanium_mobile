/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2015 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#if IS_XCODE_7
#ifdef USE_TI_UIIOSPREVIEWCONTEXT

#import <Foundation/Foundation.h>
#import "TiWindowProxy.h"
#import "TiViewController.h"
#import "TiViewProxy.h"
#import "TiUIiOSPreviewContextProxy.h"
#import "TiUIiOSPreviewActionProxy.h"
#import "TiUIiOSPreviewActionGroupProxy.h"

@class TiUIiOSPreviewContextProxy;

@interface TiPreviewingDelegate : NSObject <UIViewControllerPreviewingDelegate>

/**
    The preview context that holds actions
    and action groups.
 */
@property(nonatomic, retain) TiUIiOSPreviewContextProxy *previewContext;

/**
    The event to be set when source is a UITableView.
 */
@property(nonatomic, retain) NSDictionary* listViewEvent;

/**
    Initializes a new previewContext.
 */
- (instancetype)initWithPreviewContext:(TiUIiOSPreviewContextProxy*)previewContext;

/**
    Creates a new source rect that represents the
    visible area that is not blurred during peek.
 */
- (CGRect)createSourceRectWithLocation:(CGPoint)location;

@end
#endif
#endif