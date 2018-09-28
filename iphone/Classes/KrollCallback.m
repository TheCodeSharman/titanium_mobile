/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#import "KrollCallback.h"
#import "KrollBridge.h"
#import "KrollObject.h"
#import "TiExceptionHandler.h"

static NSMutableArray *callbacks;
static NSLock *callbackLock;

@interface KrollCallback ()
@property (nonatomic, assign) KrollContext *context;
@end

@implementation KrollCallback

@synthesize context, type;

+ (void)shutdownContext:(KrollContext *)context
{
  [callbackLock lock];
  for (KrollCallback *callback in callbacks) {
    if ([callback context] == context) {
      callback.context = nil;
    }
  }
  [callbackLock unlock];
}

+ (void)initialize
{
  if (callbacks == nil) {
    callbackLock = [[NSLock alloc] init];
    callbacks = TiCreateNonRetainingArray();
  }
}

- (id)initWithCallback:(TiValueRef)function_ thisObject:(TiObjectRef)thisObject_ context:(KrollContext *)context_
{
  if (self = [super init]) {
    context = context_;
    bridge = (KrollBridge *)[context_ delegate];
    jsContext = [context context];
    function = TiValueToObject(jsContext, function_, NULL);
    thisObj = thisObject_;
    TiValueProtect(jsContext, function);
    TiValueProtect(jsContext, thisObj);
#ifdef TI_USE_KROLL_THREAD
    contextLock = [[NSLock alloc] init];
#endif
    [callbacks addObject:self];
  }
  return self;
}

- (void)dealloc
{
  [callbackLock lock];
  [callbacks removeObject:self];
  [callbackLock unlock];

  [type release];
#ifdef TI_USE_KROLL_THREAD
  [contextLock release];
#endif
  if ([KrollBridge krollBridgeExists:bridge]) {
    if ([context isKJSThread]) {
      TiValueUnprotect(jsContext, function);
      TiValueUnprotect(jsContext, thisObj);
    } else {
      KrollUnprotectOperation *delayedUnprotect = [[KrollUnprotectOperation alloc]
          initWithContext:jsContext
             withJsobject:function
              andJsobject:thisObj];
      [context enqueue:delayedUnprotect];
      [delayedUnprotect release];
    }
  }
  function = NULL;
  thisObj = NULL;
  context = NULL;
  [super dealloc];
}

- (BOOL)isEqual:(id)anObject
{
  if (anObject == self) {
    return YES;
  }
  if ((anObject == nil) || ![anObject isKindOfClass:[KrollCallback class]]) {
    return NO;
  }
  KrollCallback *otherCallback = (KrollCallback *)anObject;
  if (function != NULL) { //TODO: Is there ever two functions with diffent memory pointers
    // that represent the exact same function? I'm thinking not.
    TiObjectRef ref1 = function;
    TiObjectRef ref2 = [otherCallback function];
    return (ref2 == ref1);
  }
  return NO;
}

- (void)callAsync:(NSArray *)args thisObject:(id)thisObject_
{
  TiThreadPerformOnMainThread(^{
    [self call:args thisObject:thisObject_];
  },
      [NSThread isMainThread]);
}
- (id)call:(NSArray *)args thisObject:(id)thisObject_
{
#ifdef TI_USE_KROLL_THREAD
  [contextLock lock];
#endif
  if (context == nil) {
#ifdef TI_USE_KROLL_THREAD
    [contextLock unlock];
#endif
    return nil;
  }

  [context retain];

  TiValueRef _args[[args count]];
  for (size_t c = 0; c < [args count]; c++) {
    _args[c] = [KrollObject toValue:context value:[args objectAtIndex:c]];
  }
  TiObjectRef tp = thisObj;
  TiValueRef top = NULL;
  if (thisObject_ != nil) {
    // hold the this reference until this thread completes
    [[thisObject_ retain] autorelease];
    // if we have a this pointer passed in, use it instead of the one we
    // constructed this callback with -- nice for when you want to effectively
    // do fn.call(this,arg) or fn.apply(this,[args])
    //
    top = [KrollObject toValue:context value:thisObject_];
    tp = TiValueToObject(jsContext, top, NULL);
    TiValueProtect(jsContext, tp);
    TiValueProtect(jsContext, top);
  }
  TiValueRef exception = NULL;
  TiValueRef retVal = TiObjectCallAsFunction(jsContext, function, tp, [args count], _args, &exception);
  if (exception != NULL) {
    id excm = [KrollObject toID:context value:exception];
    [[TiExceptionHandler defaultExceptionHandler] reportScriptError:[TiUtils scriptErrorValue:excm]];
  }
  if (top != NULL) {
    TiValueUnprotect(jsContext, tp);
    TiValueUnprotect(jsContext, top);
  }

  id val = [KrollObject toID:context value:retVal];
  [context release];
#ifdef TI_USE_KROLL_THREAD
  [contextLock unlock];
#endif
  return val;
}

- (TiObjectRef)function
{
  return function;
}

- (KrollContext *)context
{
  return context;
}

- (void)setContext:(KrollContext *)context_
{
#ifdef TI_USE_KROLL_THREAD
  [contextLock lock];
#endif
  context = context_;
#ifdef TI_USE_KROLL_THREAD
  [contextLock unlock];
#endif
}

- (KrollWrapper *)toKrollWrapper
{
  KrollWrapper *wrapper = [[[KrollWrapper alloc] init] autorelease];
  [wrapper setBridge:(KrollBridge *)[[self context] delegate]];
  [wrapper setJsobject:[self function]];
  return wrapper;
}

@end
