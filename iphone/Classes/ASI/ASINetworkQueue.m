//
//  ASINetworkQueue.m
//  Part of ASIHTTPRequest -> http://allseeing-i.com/ASIHTTPRequest
//
//  Created by Ben Copsey on 07/11/2008.
//  Copyright 2008-2009 All-Seeing Interactive. All rights reserved.
//

#import "ASINetworkQueue.h"
#import "ASIHTTPRequest.h"
#import "ASIHTTPRequestConfig.h"

// Private stuff
@interface ASINetworkQueue ()
	- (void)resetProgressDelegate:(id)progressDelegate;
	@property (assign) int requestsCount;
@end

@implementation ASINetworkQueue

- (id)init
{
	self = [super init];
	[self setShouldCancelAllRequestsOnFailure:YES];
	[self setMaxConcurrentOperationCount:4];
	[self setSuspended:YES];
	
	return self;
}

+ (id)queue
{
	return [[[self alloc] init] autorelease];
}

- (void)dealloc
{
	//We need to clear the queue on any requests that haven't got around to cleaning up yet, as otherwise they'll try to let us know if something goes wrong, and we'll be long gone by then
	for (ASIHTTPRequest *request in [self operations]) {
		[request setQueue:nil];
	}
	[userInfo release];
	[super dealloc];
}

- (BOOL)isNetworkActive
{
	return ([self requestsCount] > 0 && ![self isSuspended]);
}

- (void)updateNetworkActivityIndicator
{
#if defined(TARGET_OS_IPHONE) && ASI_AUTOUPDATE_NETWORK_INDICATOR==1
	[[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:[self isNetworkActive]];
#endif
}

- (void)setSuspended:(BOOL)suspend
{
	[super setSuspended:suspend];
	[self updateNetworkActivityIndicator];
}

- (void)reset
{
	[self cancelAllOperations];
	[self setDelegate:nil];
	[self setDownloadProgressDelegate:nil];
	[self setUploadProgressDelegate:nil];
	[self setRequestDidStartSelector:NULL];
	[self setRequestDidReceiveResponseHeadersSelector:NULL];
	[self setRequestDidFailSelector:NULL];
	[self setRequestDidFinishSelector:NULL];
	[self setQueueDidFinishSelector:NULL];
	[self setSuspended:YES];
}


- (void)go
{
	[self setSuspended:NO];
}

- (void)cancelAllOperations
{
	[self setBytesUploadedSoFar:0];
	[self setTotalBytesToUpload:0];
	[self setBytesDownloadedSoFar:0];
	[self setTotalBytesToDownload:0];
	[super cancelAllOperations];
	[self updateNetworkActivityIndicator];
}

- (void)setUploadProgressDelegate:(id)newDelegate
{
	uploadProgressDelegate = newDelegate;
	[self resetProgressDelegate:newDelegate];

}

- (void)setDownloadProgressDelegate:(id)newDelegate
{
	downloadProgressDelegate = newDelegate;
	[self resetProgressDelegate:newDelegate];
}

- (void)resetProgressDelegate:(id)progressDelegate
{
#if !TARGET_OS_IPHONE
	// If the uploadProgressDelegate is an NSProgressIndicator, we set its MaxValue to 1.0 so we can treat it similarly to UIProgressViews
	SEL selector = @selector(setMaxValue:);
	if ([progressDelegate respondsToSelector:selector]) {
		double max = 1.0;
		[ASIHTTPRequest performSelector:selector onTarget:progressDelegate withObject:nil amount:&max mainThread:shouldPerformCallbacksOnMainThread];
	}
	selector = @selector(setDoubleValue:);
	if ([progressDelegate respondsToSelector:selector]) {
		double value = 0.0;
		[ASIHTTPRequest performSelector:selector onTarget:progressDelegate withObject:nil amount:&value mainThread:shouldPerformCallbacksOnMainThread];
	}
#else
	SEL selector = @selector(setProgress:);
	if ([progressDelegate respondsToSelector:selector]) {
		float value = 0.0f;
		[ASIHTTPRequest performSelector:selector onTarget:progressDelegate withObject:nil amount:&value mainThread:shouldPerformCallbacksOnMainThread];
	}
#endif
}

- (void)addHEADOperation:(NSOperation *)operation
{
	if ([operation isKindOfClass:[ASIHTTPRequest class]]) {
		
		ASIHTTPRequest *request = (ASIHTTPRequest *)operation;
		[request setRequestMethod:@"HEAD"];
		[request setQueuePriority:10];
		[request setShowAccurateProgress:YES];
		[request setQueue:self];
		
		// Important - we are calling NSOperation's add method - we don't want to add this as a normal request!
		[super addOperation:request];
	}
}

// Only add ASIHTTPRequests to this queue!!
- (void)addOperation:(NSOperation *)operation
{
	if (![operation isKindOfClass:[ASIHTTPRequest class]]) {
		[NSException raise:@"AttemptToAddInvalidRequest" format:@"Attempted to add an object that was not an ASIHTTPRequest to an ASINetworkQueue"];
	}
		
	[self setRequestsCount:[self requestsCount]+1];
	
	ASIHTTPRequest *request = (ASIHTTPRequest *)operation;
	
	if ([self showAccurateProgress]) {
		
		// Force the request to build its body (this may change requestMethod)
		[request buildPostBody];
		
		// If this is a GET request and we want accurate progress, perform a HEAD request first to get the content-length
		// We'll only do this before the queue is started
		// If requests are added after the queue is started they will probably move the overall progress backwards anyway, so there's no value performing the HEAD requests first
		// Instead, they'll update the total progress if and when they receive a content-length header
		if ([[request requestMethod] isEqualToString:@"GET"]) {
			if ([self isSuspended]) {
				ASIHTTPRequest *HEADRequest = [request HEADRequest];
				[self addHEADOperation:HEADRequest];
				[request addDependency:HEADRequest];
				if ([request shouldResetDownloadProgress]) {
					[self resetProgressDelegate:[request downloadProgressDelegate]];
					[request setShouldResetDownloadProgress:NO];
				}
			}
		}
		[request buildPostBody];
		[self request:nil incrementUploadSizeBy:[request postLength]];


	} else {
		[self request:nil incrementDownloadSizeBy:1];
		[self request:nil incrementUploadSizeBy:1];
	}
	// Tell the request not to increment the upload size when it starts, as we've already added its length
	if ([request shouldResetUploadProgress]) {
		[self resetProgressDelegate:[request uploadProgressDelegate]];
		[request setShouldResetUploadProgress:NO];
	}
	
	[request setShowAccurateProgress:[self showAccurateProgress]];
	
	[request setQueue:self];
	[super addOperation:request];
	[self updateNetworkActivityIndicator];

}

- (void)requestStarted:(ASIHTTPRequest *)request
{
	if ([self requestDidStartSelector]) {
		[[self delegate] performSelector:[self requestDidStartSelector] withObject:request];
	}
}

- (void)requestReceivedResponseHeaders:(ASIHTTPRequest *)request
{
	if ([self requestDidReceiveResponseHeadersSelector]) {
		[[self delegate] performSelector:[self requestDidReceiveResponseHeadersSelector] withObject:request];
	}	
}


- (void)requestFinished:(ASIHTTPRequest *)request
{
	[self setRequestsCount:[self requestsCount]-1];
	[self updateNetworkActivityIndicator];
	if ([self requestDidFinishSelector]) {
		[[self delegate] performSelector:[self requestDidFinishSelector] withObject:request];
	}
	if ([self requestsCount] == 0) {
		if ([self queueDidFinishSelector]) {
			[[self delegate] performSelector:[self queueDidFinishSelector] withObject:self];
		}
	}
}

- (void)requestFailed:(ASIHTTPRequest *)request
{
	[self setRequestsCount:[self requestsCount]-1];
	[self updateNetworkActivityIndicator];
	if ([self requestDidFailSelector]) {
		[[self delegate] performSelector:[self requestDidFailSelector] withObject:request];
	}
	if ([self requestsCount] == 0) {
		if ([self queueDidFinishSelector]) {
			[[self delegate] performSelector:[self queueDidFinishSelector] withObject:self];
		}
	}
	if ([self shouldCancelAllRequestsOnFailure] && [self requestsCount] > 0) {
		[self cancelAllOperations];
	}
	
}


- (void)request:(ASIHTTPRequest *)request didReceiveBytes:(long long)bytes
{
	[self setBytesDownloadedSoFar:[self bytesDownloadedSoFar]+bytes];
	if ([self downloadProgressDelegate]) {
		[ASIHTTPRequest updateProgressIndicator:[self downloadProgressDelegate] withProgress:[self bytesDownloadedSoFar] ofTotal:[self totalBytesToDownload] mainThread:shouldPerformCallbacksOnMainThread];
	}
}

- (void)request:(ASIHTTPRequest *)request didSendBytes:(long long)bytes
{
	[self setBytesUploadedSoFar:[self bytesUploadedSoFar]+bytes];
	if ([self uploadProgressDelegate]) {
		[ASIHTTPRequest updateProgressIndicator:[self uploadProgressDelegate] withProgress:[self bytesUploadedSoFar] ofTotal:[self totalBytesToUpload] mainThread:shouldPerformCallbacksOnMainThread];
	}
}

- (void)request:(ASIHTTPRequest *)request incrementDownloadSizeBy:(long long)newLength
{
	[self setTotalBytesToDownload:[self totalBytesToDownload]+newLength];
}

- (void)request:(ASIHTTPRequest *)request incrementUploadSizeBy:(long long)newLength
{
	[self setTotalBytesToUpload:[self totalBytesToUpload]+newLength];
}


// Since this queue takes over as the delegate for all requests it contains, it should forward authorisation requests to its own delegate
- (void)authenticationNeededForRequest:(ASIHTTPRequest *)request
{
	if ([[self delegate] respondsToSelector:@selector(authenticationNeededForRequest:)]) {
		[[self delegate] performSelector:@selector(authenticationNeededForRequest:) withObject:request];
	}
}

- (void)proxyAuthenticationNeededForRequest:(ASIHTTPRequest *)request
{
	if ([[self delegate] respondsToSelector:@selector(proxyAuthenticationNeededForRequest:)]) {
		[[self delegate] performSelector:@selector(proxyAuthenticationNeededForRequest:) withObject:request];
	}
}


- (BOOL)respondsToSelector:(SEL)selector
{
	if (selector == @selector(authenticationNeededForRequest:)) {
		if ([[self delegate] respondsToSelector:@selector(authenticationNeededForRequest:)]) {
			return YES;
		}
		return NO;
	} else if (selector == @selector(proxyAuthenticationNeededForRequest:)) {
		if ([[self delegate] respondsToSelector:@selector(proxyAuthenticationNeededForRequest:)]) {
			return YES;
		}
		return NO;
	}
	return [super respondsToSelector:selector];
}

#pragma mark NSCopying

- (id)copyWithZone:(NSZone *)zone
{
	ASINetworkQueue *newQueue = [[[self class] alloc] init];
	[newQueue setDelegate:[self delegate]];
	[newQueue setRequestDidStartSelector:[self requestDidStartSelector]];
	[newQueue setRequestDidFinishSelector:[self requestDidFinishSelector]];
	[newQueue setRequestDidFailSelector:[self requestDidFailSelector]];
	[newQueue setQueueDidFinishSelector:[self queueDidFinishSelector]];
	[newQueue setUploadProgressDelegate:[self uploadProgressDelegate]];
	[newQueue setDownloadProgressDelegate:[self downloadProgressDelegate]];
	[newQueue setShouldCancelAllRequestsOnFailure:[self shouldCancelAllRequestsOnFailure]];
	[newQueue setShowAccurateProgress:[self showAccurateProgress]];
	[newQueue setUserInfo:[[[self userInfo] copyWithZone:zone] autorelease]];
	return newQueue;
}


@synthesize requestsCount;
@synthesize bytesUploadedSoFar;
@synthesize totalBytesToUpload;
@synthesize bytesDownloadedSoFar;
@synthesize totalBytesToDownload;
@synthesize shouldCancelAllRequestsOnFailure;
@synthesize shouldPerformCallbacksOnMainThread;
@synthesize uploadProgressDelegate;
@synthesize downloadProgressDelegate;
@synthesize requestDidStartSelector;
@synthesize requestDidReceiveResponseHeadersSelector;
@synthesize requestDidFinishSelector;
@synthesize requestDidFailSelector;
@synthesize queueDidFinishSelector;
@synthesize delegate;
@synthesize showAccurateProgress;
@synthesize userInfo;
@end
