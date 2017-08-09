/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2017-Present by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UITOOLBAR

#import "TiToolbar.h"
#import "TiViewProxy.h"

@interface TiUIToolbarProxy : TiViewProxy <TiToolbar> {
    @private
    NSString *_apiName;
}

- (id)_initWithPageContext:(id<TiEvaluator>)context_ args:(NSArray *)args apiName:(NSString *)apiName;

@end

#endif
