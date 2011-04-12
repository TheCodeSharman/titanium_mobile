/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_NETWORKSOCKET
#import <Foundation/Foundation.h>
#import "TiStreamProxy.h"
#import "AsyncSocket.h"
#import "TiNetworkSocketProxy.h"

@interface TiNetworkSocketTCPProxy : TiStreamProxy<AsyncSocketDelegate, TiStreamInternal> {
    AsyncSocket* socket;
    SocketState internalState;
    NSCondition* listening;
    
    NSThread* socketThread;
    // We have to have an explicit "host" property because of some 'fun' undocumented KVO
    // behavior - it turns out that KVO 'getters' also look for '-(type)_key' signature
    // selectors.  TiProxy has a '_host' function.
    NSString* host;
    
    // We offer synchronous I/O.  The underlying socket implementation is asynchronous.
    // So we need to ensure our own synchronicity by signaling a condition when operations
    // complete.
    NSCondition* ioCondition;
    NSUInteger readDataLength;
    
    // In order to put the accepted socket on the right run loop, and make sure it's constructed
    // properly, we need THESE as well...
    NSMutableDictionary* acceptArgs;
    NSRunLoop* acceptRunLoop;
    NSCondition* acceptCondition;
    BOOL accepting;
    
    // Information used to hash asynch callbacks to tags.
    int asynchTagCount;
    NSMutableDictionary* asynchCallbacks;
    
    KrollCallback* connected;
    KrollCallback* accepted;
    KrollCallback* closed;
    KrollCallback* error;
}
// Properties:
// -- Stored on TiProxy dynprops --
// int port
// ----
@property (nonatomic, readwrite, retain) NSString* host;
@property (nonatomic, readonly) NSNumber* state; // Req's local processing
@property (nonatomic, readwrite, retain) KrollCallback* connected;
@property (nonatomic, readwrite, retain) KrollCallback* accepted;
@property (nonatomic, readwrite, retain) KrollCallback* closed;
@property (nonatomic, readwrite, retain) KrollCallback* error;

// Public API
-(void)connect:(id)_void;
-(void)listen:(id)arg; // arg[0]: int maxAcceptQueueSize : queue size
-(void)accept:(id)arg; // arg[0]: Object params : callbacks for created socket
-(void)close:(id)_void;

@end
#endif