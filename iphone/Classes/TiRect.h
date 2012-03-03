/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#import "TiProxy.h"

/**
 The class for rectangle object proxy.
 */
@interface TiRect : TiProxy {
	CGRect rect;
}

/**
 Initializes the rect proxy from rect struct.
 @param rect_ The rect struct.
 */
-(void)setRect:(CGRect)rect_;

/**
 Returns rect struct.
 @return The rect struct.
 */
-(CGRect)rect;

/**
 Provides access to rectangle x coordinate.
 */
@property(nonatomic,retain) NSNumber *x;

/**
 Provides access to rectangle y coordinate.
 */
@property(nonatomic,retain) NSNumber *y;

/**
 Provides access to rectangle width.
 */
@property(nonatomic,retain) NSNumber *width;

/**
 Provides access to rectangle height.
 */
@property(nonatomic,retain) NSNumber *height;

@end
