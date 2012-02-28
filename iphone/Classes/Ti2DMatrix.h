/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#import "TiProxy.h"

/**
 The proxy class for 2D matrix.
 */
@interface Ti2DMatrix : TiProxy {
@protected
	CGAffineTransform matrix;
}

/**
 Initializes the proxy with properties.
 @param dict_ The properties dictionary.
 */
-(id)initWithProperties:(NSDictionary*)dict_;

/**
 Initializes the proxy with transform matrix.
 @param matrix_ The transform matrix.
 */
-(id)initWithMatrix:(CGAffineTransform)matrix_;

/**
 Returns transform matrix.
 @return The transform matrix.
 */
-(CGAffineTransform)matrix;

-(Ti2DMatrix*)translate:(id)args;
-(Ti2DMatrix*)scale:(id)args;
-(Ti2DMatrix*)rotate:(id)args;
-(Ti2DMatrix*)invert:(id)args;
-(Ti2DMatrix*)multiply:(id)args;

@property(nonatomic,readwrite,retain) NSNumber* a;
@property(nonatomic,readwrite,retain) NSNumber* b;
@property(nonatomic,readwrite,retain) NSNumber* c;
@property(nonatomic,readwrite,retain) NSNumber* d;
@property(nonatomic,readwrite,retain) NSNumber* tx;
@property(nonatomic,readwrite,retain) NSNumber* ty;

@end

