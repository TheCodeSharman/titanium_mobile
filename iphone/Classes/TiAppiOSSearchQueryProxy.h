/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2016 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef IS_XCODE_8
#ifdef USE_TI_APPIOSSEARCHQUERY
#import "TiProxy.h"
#import <CoreSpotlight/CoreSpotlight.h>

@interface TiAppiOSSearchQueryProxy : TiProxy {
    CSSearchQuery *query;
}

- (CSSearchQuery*)query;

- (void)start:(id)unused;

- (void)cancel:(id)unused;

- (NSNumber*)isCancelled:(id)unused;

@end
#endif
#endif
