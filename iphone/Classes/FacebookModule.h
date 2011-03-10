/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_FACEBOOK
#import "TiModule.h"
#import "Facebook.h"

@protocol TiFacebookStateListener
@required
-(void)login;
-(void)logout;
@end


@interface FacebookModule : TiModule <FBSessionDelegate2, FBRequestDelegate2>
{
	Facebook *facebook;
	BOOL loggedIn;
	NSString *uid;
	NSString *url;
	NSString *appid;
	NSArray *permissions;
	NSMutableArray *stateListeners;
    BOOL forceDialogAuth;
}

@property(nonatomic,readonly) Facebook *facebook;

-(BOOL)isLoggedIn;
-(void)addListener:(id<TiFacebookStateListener>)listener;
-(void)removeListener:(id<TiFacebookStateListener>)listener;

-(void)authorize:(id)args;
-(void)logout:(id)args;

@end
#endif