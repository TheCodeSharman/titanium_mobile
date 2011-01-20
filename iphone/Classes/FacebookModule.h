/**
 * Appcelerator Commercial License. Copyright (c) 2010 by Appcelerator, Inc.
 *
 * Appcelerator Titanium is Copyright (c) 2009-2010 by Appcelerator, Inc.
 * and licensed under the Apache Public License (version 2)
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
}

@property(nonatomic,readonly) Facebook *facebook;

-(BOOL)isLoggedIn;
-(void)addListener:(id<TiFacebookStateListener>)listener;
-(void)removeListener:(id<TiFacebookStateListener>)listener;

-(void)authorize:(id)args;
-(void)logout:(id)args;

@end
#endif