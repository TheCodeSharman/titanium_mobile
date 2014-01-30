/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2014 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 *
 * Special thanks to Pedro Enrique for implementing this.
 */

#import "TiHTTPClient.h"

@implementation TiHTTPOperation

- (void)dealloc
{
    RELEASE_TO_NIL(_connection);
    [super dealloc];
}

- (id)initWithConnection:(NSURLConnection *)connection
{
    self = [super init];
    if (self) {
        _connection = [connection retain];
        _cancelled = NO;
        _executing = NO;
        _finished = NO;
        _ready = NO;
    }
    [self willChangeValueForKey: @"isReady"];
    _ready = YES;
    [self didChangeValueForKey: @"isReady"];

    return self;
}

- (void)start
{
    [self willChangeValueForKey: @"isExecuting"];
    _executing = YES;
    [self didChangeValueForKey: @"isExecuting"];

    if(!_cancelled) {
        [[self connection] start];
        // Keep running the run loop until all asynchronous operations are completed
        while (![self isFinished]) {
            [[NSRunLoop currentRunLoop] runMode:NSDefaultRunLoopMode beforeDate:[NSDate distantFuture]];
        }
    }
}

- (void)cancel
{
    [self willChangeValueForKey: @"isCancelled"];
    _cancelled = YES;
    [self didChangeValueForKey: @"isCancelled"];
    
    if(_executing) {
        [[self connection] cancel];
    } else {
        [self start];
    }
    
    [self setFinished:YES];
    [super cancel];
}

-(void)setFinished:(BOOL)finished
{
    [self willChangeValueForKey: @"isFinished"];
    _finished = finished;
    [self didChangeValueForKey: @"isFinished"];

}
-(BOOL)isReady
{
    return _ready;
}
- (BOOL)isCancelled
{
    return _cancelled;
}
- (BOOL)isExecuting
{
    return _executing;
}
- (BOOL)isFinished
{
    return _finished;
}
-(BOOL)isConcurrent
{
    return NO;
}

@end
