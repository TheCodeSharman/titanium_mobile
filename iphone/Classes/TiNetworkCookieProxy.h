/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2014 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#import "TiProxy.h"

@interface TiNetworkCookieProxy : TiProxy
{
    NSHTTPCookie *_cookie;
    NSMutableDictionary *_cookieDict;
}

-(id)initWithCookie:(NSHTTPCookie*)cookie andPageContext:(id<TiEvaluator>)context;
-(NSHTTPCookie*)newCookie;
@end