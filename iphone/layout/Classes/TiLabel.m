/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2015 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#import "TiLabel.h"

@interface TiLabel()
{
    UILabel* label;
}
@end

@implementation TiLabel

- (instancetype)init
{
    self = [super init];
    if (self) {
        label = [[UILabel alloc] initWithFrame:[self bounds]];
        [label setAutoresizingMask:UIViewAutoresizingFlexibleHeight|UIViewAutoresizingFlexibleWidth];
        [label setNumberOfLines:0];
        [self addSubview:label];
        [self setDefaultHeight:TiDimensionAutoSize];
        [self setDefaultWidth:TiDimensionAutoSize];
    }
    return self;
}


-(void)setText:(NSString*) text
{
    [label setText:text];
}

-(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds
{
    [TiUtils setView:label positionRect:bounds];
    [super frameSizeChanged:frame bounds:bounds];
}

@end
