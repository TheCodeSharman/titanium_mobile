/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_FACEBOOK
#import "TiFacebookDialogRequest.h"


@implementation TiFacebookDialogRequest

#pragma mark Lifecycle

-(id)initWithCallback:(KrollCallback*)callback_ module:(FacebookModule*)module_
{
	if (self = [super init])
	{
		callback = [callback_ retain];
		module = [module_ retain];
		[self retain]; // this is because we return autoreleased and as a delegate we're not retained
	}
	return self;
}

-(void)dealloc
{
	RELEASE_TO_NIL(callback);
	RELEASE_TO_NIL(module);
	[super dealloc];
}


#pragma mark Delegate

/**
 * Called when the dialog succeeds with a returning url.
 */
- (void)dialogCompleteWithUrl:(NSURL *)url
{
	NSLog(@"[INFO] dialogCompleteWithUrl = %@",url);
	
	[self autorelease];

	NSDictionary *event = [NSDictionary dictionaryWithObjectsAndKeys:NUMBOOL(NO),@"cancelled",NUMBOOL(YES),@"success",url,@"url",nil];
	[module _fireEventToListener:@"result" withObject:event listener:callback thisObject:nil];
}

/**
 * Called when the dialog get canceled by the user.
 */
- (void)dialogDidNotCompleteWithUrl:(NSURL *)url
{
	NSLog(@"[INFO] dialogDidNotCompleteWithUrl = %@",url);

	[self autorelease];

	NSDictionary *event = [NSDictionary dictionaryWithObjectsAndKeys:NUMBOOL(YES),@"cancelled",NUMBOOL(NO),@"success",nil];
	[module _fireEventToListener:@"result" withObject:event listener:callback thisObject:nil];
}

/**
 * Called when dialog failed to load due to an error.
 */
- (void)dialog:(FBDialog2*)dialog didFailWithError:(NSError *)error
{
	NSLog(@"[INFO] didFailWithError = %@",error);
	
	[self autorelease];
	
	NSDictionary *event = [NSDictionary dictionaryWithObjectsAndKeys:NUMBOOL(NO),@"cancelled",NUMBOOL(NO),@"success",[error localizedDescription],@"error",nil];
	[module _fireEventToListener:@"result" withObject:event listener:callback thisObject:nil];
}

/**
 * Asks if a link touched by a user should be opened in an external browser.
 *
 * If a user touches a link, the default behavior is to open the link in the Safari browser, 
 * which will cause your app to quit.  You may want to prevent this from happening, open the link
 * in your own internal browser, or perhaps warn the user that they are about to leave your app.
 * If so, implement this method on your delegate and return NO.  If you warn the user, you
 * should hold onto the URL and once you have received their acknowledgement open the URL yourself
 * using [[UIApplication sharedApplication] openURL:].
 */
- (BOOL)dialog:(FBDialog2*)dialog shouldOpenURLInExternalBrowser:(NSURL *)url
{
	return NO;
}


@end
#endif