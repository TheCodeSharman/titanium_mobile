/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UIIOSTRANSITIONANIMATION

#import "TiUIiOSTransitionAnimationProxy.h"
#import "TiViewController.h"
#import "TiViewProxy.h"


@implementation TiUIiOSTransitionAnimationProxy

- (id)init
{
    self = [super init];
    if (self) {
    }
    return self;
}

- (void)dealloc
{
    if(_transitionFrom != nil) {
        [self forgetProxy:_transitionFrom];
        RELEASE_TO_NIL(_transitionFrom)
    }
    if(_transitionTo != nil) {
        [self forgetProxy:_transitionTo];
        RELEASE_TO_NIL(_transitionTo)
    }
    RELEASE_TO_NIL(_duration)
    RELEASE_TO_NIL(_transitionContext)
    [super dealloc];
}

-(id)duration
{
    return _duration;
}

-(void)setDuration:(id)arg
{
    ENSURE_SINGLE_ARG(arg, NSNumber)
    RELEASE_TO_NIL(_duration)
    _duration = [arg retain];
}

-(void)setTransitionTo:(id)args
{
    RELEASE_TO_NIL(_transitionTo)
    _transitionTo = [TiAnimation animationFromArg:args context:[self executionContext] create:NO];
    if([_transitionTo isTransitionAnimation]) {
        DebugLog(@"[ERROR] Transition animations are not supported yet");
        _transitionTo = nil;
        return;
    }
    [_transitionTo setDelegate:self];
    [_transitionTo retain];
    [self rememberProxy:_transitionTo];
}

-(void)setTransitionFrom:(id)args
{
    RELEASE_TO_NIL(_transitionFrom)
    _transitionFrom = [TiAnimation animationFromArg:args context:[self executionContext] create:NO];
    if([_transitionFrom isTransitionAnimation]) {
        DebugLog(@"[ERROR] Transition animations are not supported yet");
        _transitionFrom = nil;
        return;
    }
    [_transitionFrom setDelegate:self];
    [_transitionFrom retain];
    [self rememberProxy:_transitionFrom];
}

-(TiAnimation*)transitionTo
{
    return _transitionTo;
}

-(TiAnimation*)transitionFrom
{
    return _transitionFrom;
}

- (void)animateTransition:(id<UIViewControllerContextTransitioning>)transitionContext
{
    
    TiViewController *fromViewController = (TiViewController*)[transitionContext viewControllerForKey:UITransitionContextFromViewControllerKey];
    TiViewController *toViewController = (TiViewController*)[transitionContext viewControllerForKey:UITransitionContextToViewControllerKey];
    
    TiViewProxy *fromProxy = [fromViewController proxy];
    TiViewProxy *toProxy = [toViewController proxy];

    if([self _hasListeners:@"start"])
    {
        NSDictionary *dict = [NSDictionary dictionaryWithObjectsAndKeys:
                              toProxy, @"toWindow",
                              fromProxy, @"fromWindow",
                              nil];
        [self fireEvent:@"start" withObject:dict propagate:NO reportSuccess:NO errorCode:0 message:nil];
    }

    _endedFrom = [self transitionFrom] == nil;
    _endedTo = [self transitionTo] == nil;

    _transitionContext = [transitionContext retain];
    
    
    [fromProxy setParentVisible:YES];
    [toProxy setParentVisible:YES];
    
    UIView *container = [transitionContext containerView];
    [container setUserInteractionEnabled:NO];
    
    [container addSubview:[fromViewController view]];
    [container addSubview:[toViewController view]];
    
    if([self transitionFrom] != nil) {
        [fromProxy animate: [self transitionFrom]];
    }
    if ([self transitionTo] != nil) {
        [toProxy animate: [self transitionTo]];
    }
    
    if([self transitionTo] == nil && [self transitionFrom] == nil)
    {
        [_transitionContext completeTransition:YES];
    }
}

-(void)animationDidComplete:(TiAnimation *)animation;
{
    if(animation == _transitionFrom) {
        _endedFrom = YES;
    }
    if(animation == _transitionTo) {
        _endedTo = YES;
    }
    if(_endedTo && _endedFrom) {
        [_transitionContext completeTransition:YES];
    }
}

- (NSTimeInterval)transitionDuration:(id<UIViewControllerContextTransitioning>)transitionContext
{
    if([self transitionTo] == nil && [self transitionFrom] == nil)
    {
        return 0;
    }
    if(_duration == nil) {
        return MAX(
                   [TiUtils floatValue:[[self transitionTo] duration] def:0],
                   [TiUtils floatValue:[[self transitionFrom] duration] def:0]
                ) / 1000;
    }
    return [TiUtils floatValue:_duration def:0] / 1000;
}

- (void)animationEnded:(BOOL) transitionCompleted;
{
    if([self _hasListeners:@"end"]) {
        TiViewController *fromViewController = (TiViewController*)[_transitionContext viewControllerForKey:UITransitionContextFromViewControllerKey];
        TiViewController *toViewController = (TiViewController*)[_transitionContext viewControllerForKey:UITransitionContextToViewControllerKey];
        
        TiViewProxy *fromProxy = [fromViewController proxy];
        TiViewProxy *toProxy = [toViewController proxy];
        
        NSDictionary *dict = [NSDictionary dictionaryWithObjectsAndKeys:
                              toProxy, @"toWindow",
                              fromProxy, @"fromWindow",
                              nil];
        [self fireEvent:@"end" withObject:dict propagate:NO reportSuccess:NO errorCode:0 message:nil];
    }
}

@end
#endif
