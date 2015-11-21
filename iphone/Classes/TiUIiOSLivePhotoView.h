/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2015 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#import "TiUIView.h"
#import "TiUIiOSLivePhoto.h"
#import <PhotosUI/PhotosUI.h>

@interface TiUIiOSLivePhotoView : TiUIView<PHLivePhotoViewDelegate> {
    TiDimension width;
    TiDimension height;
    CGFloat autoHeight;
    CGFloat autoWidth;
}

@property(nonatomic,retain) PHLivePhotoView *livePhotoView;

@end
