/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#import <objc/runtime.h>

#import "TiProxy.h"
#import "TiHost.h"
#import "KrollCallback.h"
#import "KrollBridge.h"
#import "TiModule.h"
#import "ListenerEntry.h"
#import "TiComplexValue.h"
#import "TiViewProxy.h"

#define TI_USE_PROPERTY_LOCK 0


//Common exceptions to throw when the function call was improper
NSString * const TiExceptionInvalidType = @"Invalid type passed to function";
NSString * const TiExceptionNotEnoughArguments = @"Invalid number of arguments to function";
NSString * const TiExceptionRangeError = @"Value passed to function exceeds allowed range";

//Should be rare, but also useful if arguments are used improperly.
NSString * const TiExceptionInternalInconsistency = @"Value was not the value expected";

//Rare exceptions to indicate a bug in the titanium code (Eg, method that a subclass should have implemented)
NSString * const TiExceptionUnimplementedFunction = @"Subclass did not implement required method";



SEL SetterForKrollProperty(NSString * key)
{
	NSString *method = [NSString stringWithFormat:@"set%@%@_:", [[key substringToIndex:1] uppercaseString], [key substringFromIndex:1]];
	return NSSelectorFromString(method);
}

SEL SetterWithObjectForKrollProperty(NSString * key)
{
	NSString *method = [NSString stringWithFormat:@"set%@%@_:withObject:", [[key substringToIndex:1] uppercaseString], [key substringFromIndex:1]];
	return NSSelectorFromString(method);
}

void DoProxyDelegateChangedValuesWithProxy(UIView<TiProxyDelegate> * target, NSString * key, id oldValue, id newValue, TiProxy * proxy)
{
	// default implementation will simply invoke the setter property for this object
	// on the main UI thread
	
	// first check to see if the property is defined by a <key>:withObject: signature
	SEL sel = SetterWithObjectForKrollProperty(key);
	if ([target respondsToSelector:sel])
	{
		id firstarg = newValue;
		id secondarg = [NSDictionary dictionary];
		
		if ([firstarg isKindOfClass:[TiComplexValue class]])
		{
			firstarg = [(TiComplexValue*)newValue value];
			secondarg = [(TiComplexValue*)newValue properties];
		}
		
		if ([NSThread isMainThread])
		{
			[target performSelector:sel withObject:firstarg withObject:secondarg];
		}
		else
		{
			if (![key hasPrefix:@"set"])
			{
				key = [NSString stringWithFormat:@"set%@%@_", [[key substringToIndex:1] uppercaseString], [key substringFromIndex:1]];
			}
			NSArray *arg = [NSArray arrayWithObjects:key,firstarg,secondarg,target,nil];
			[proxy performSelectorOnMainThread:@selector(_dispatchWithObjectOnUIThread:) withObject:arg waitUntilDone:NO];
		}
		return;
	}
	
	sel = SetterForKrollProperty(key);
	if ([target respondsToSelector:sel])
	{
		if ([NSThread isMainThread])
		{
			[target performSelector:sel withObject:newValue];
		}
		else
		{
			[target performSelectorOnMainThread:sel withObject:newValue waitUntilDone:NO modes:[NSArray arrayWithObject:NSRunLoopCommonModes]];
		}
	}
}

void DoProxyDispatchToSecondaryArg(UIView<TiProxyDelegate> * target, SEL sel, NSString *key, id newValue, TiProxy * proxy)
{
	id firstarg = newValue;
	id secondarg = [NSDictionary dictionary];
	
	if ([firstarg isKindOfClass:[TiComplexValue class]])
	{
		firstarg = [(TiComplexValue*)newValue value];
		secondarg = [(TiComplexValue*)newValue properties];
	}
	
	if ([NSThread isMainThread])
	{
		[target performSelector:sel withObject:firstarg withObject:secondarg];
	}
	else
	{
		if (![key hasPrefix:@"set"])
		{
			key = [NSString stringWithFormat:@"set%@%@_", [[key substringToIndex:1] uppercaseString], [key substringFromIndex:1]];
		}
		NSArray *arg = [NSArray arrayWithObjects:key,firstarg,secondarg,target,nil];
		[proxy performSelectorOnMainThread:@selector(_dispatchWithObjectOnUIThread:) withObject:arg waitUntilDone:NO];
	}
}

void DoProxyDelegateReadKeyFromProxy(UIView<TiProxyDelegate> * target, NSString *key, TiProxy * proxy, NSNull * nullValue, BOOL useThisThread)
{
	// use valueForUndefined since this should really come from dynprops
	// not against the real implementation
	id value = [proxy valueForUndefinedKey:key];
	if (value == nil)
	{
		return;
	}
	if (value == nullValue)
	{
		value = nil;
	}

	SEL sel = SetterWithObjectForKrollProperty(key);
	if ([target respondsToSelector:sel])
	{
		DoProxyDispatchToSecondaryArg(target,sel,key,value,proxy);
		return;
	}
	sel = SetterForKrollProperty(key);
	if (![target respondsToSelector:sel])
	{
		return;
	}
	if (useThisThread)
	{
		[target performSelector:sel withObject:value];
	}
	else
	{
		[target performSelectorOnMainThread:sel withObject:value
				waitUntilDone:NO modes:[NSArray arrayWithObject:NSRunLoopCommonModes]];
	}
}

void DoProxyDelegateReadValuesWithKeysFromProxy(UIView<TiProxyDelegate> * target, id<NSFastEnumeration> keys, TiProxy * proxy)
{
	BOOL isMainThread = [NSThread isMainThread];
	NSNull * nullObject = [NSNull null];
	BOOL viewAttached = YES;
	
	NSArray * keySequence = [proxy keySequence];
	
	// assume if we don't have a view that we can send on the 
	// main thread to the proxy
	if ([target isKindOfClass:[TiViewProxy class]])
	{
		viewAttached = [(TiViewProxy*)target viewAttached];
	}
	
	BOOL useThisThread = isMainThread==YES || viewAttached==NO;

	for (NSString * thisKey in keySequence)
	{
		DoProxyDelegateReadKeyFromProxy(target, thisKey, proxy, nullObject, useThisThread);
	}

	
	for (NSString * thisKey in keys)
	{
		if ([keySequence containsObject:thisKey])
		{
			continue;
		}
		DoProxyDelegateReadKeyFromProxy(target, thisKey, proxy, nullObject, useThisThread);
	}
}



@implementation TiProxy

@synthesize pageContext, executionContext;
@synthesize modelDelegate;


#pragma mark Private

-(id)init
{
	if (self = [super init])
	{
#if PROXY_MEMORY_TRACK == 1
		NSLog(@"INIT: %@ (%d)",self,[self hash]);
#endif
		modelDelegate = nil;
		pageContext = nil;
		executionContext = nil;
		destroyLock = [[NSRecursiveLock alloc] init];
	}
	return self;
}

-(id)_initWithPageContext:(id<TiEvaluator>)context
{
	if (self = [self init])
	{   
		pageContext = (id)context; // do not retain 

		contextListeners = [[NSMutableDictionary alloc] init];
		if (context!=nil)
		{
			[contextListeners setObject:pageContext forKey:[pageContext description]];
		}
		
		[pageContext registerProxy:self];
		
		// allow subclasses to configure themselves
		[self _configure];
	}
	return self;
}

-(void)contextWasShutdown:(id<TiEvaluator>)context
{
}

-(void)contextShutdown:(id)sender
{
	id<TiEvaluator> context = (id<TiEvaluator>)sender;
	if (contextListeners!=nil)
	{
		id key = [context description];
		id value = [contextListeners objectForKey:key];
		if (value!=nil)
		{
			[contextListeners removeObjectForKey:key];
			
			// remove any listeners that match this context being destroyed that we have registered
			if (listeners!=nil)
			{
				for (id type in [NSDictionary dictionaryWithDictionary:listeners])
				{
					NSArray *a = [listeners objectForKey:type];
					for (KrollCallback *callback in [NSArray arrayWithArray:a])
					{
						if ([context krollContext] == [callback context])
						{
							[self removeEventListener:[NSArray arrayWithObjects:type,callback,nil]];
						}
					}
				}
			}
			
			[self _destroy];
			[self _contextDestroyed];
		}
	}
	[self contextWasShutdown:context];
}

-(void)setExecutionContext:(id<TiEvaluator>)context
{
	// the execution context is different than the page context
	//
	// the page context is the owning context that created (and thus owns) the proxy
	//
	// the execution context is the context which is executing against the context when 
	// this proxy is being touched.  since objects can be referenced from one context 
	// in another, the execution context should be used to resolve certain things like
	// paths, etc. so that the proper context can be contextualized which is different
	// than the owning context (page context).
	//
	executionContext = context; //don't retain
}

-(void)_configurationSet
{
	// for subclass
}

-(void)_initWithProperties:(NSDictionary*)properties
{
	[self setValuesForKeysWithDictionary:properties];
	[self _configurationSet];
}

-(void)_initWithCallback:(KrollCallback*)callback
{
}

-(void)_configure
{
	// for subclasses
}

-(id)_initWithPageContext:(id<TiEvaluator>)context_ args:(NSArray*)args
{
	if (self = [self _initWithPageContext:context_])
	{
		id a = nil;
		int count = [args count];
		
		if (count > 0 && [[args objectAtIndex:0] isKindOfClass:[NSDictionary class]])
		{
			a = [args objectAtIndex:0];
		}
		
		if (count > 1 && [[args objectAtIndex:1] isKindOfClass:[KrollCallback class]])
		{
			[self _initWithCallback:[args objectAtIndex:1]];
		}
		
		if (![NSThread isMainThread] && [self _propertyInitRequiresUIThread])
		{
			[self performSelectorOnMainThread:@selector(_initWithProperties:) withObject:a waitUntilDone:NO];
		}		
		else 
		{
			[self _initWithProperties:a];
		}
	}
	return self;
}

-(void)_contextDestroyed
{
}

-(void)_destroy
{
	[destroyLock lock];
	
	if (destroyed)
	{
		[destroyLock unlock];
		return;
	}
	
	destroyed = YES;
	
#if PROXY_MEMORY_TRACK == 1
	NSLog(@"DESTROY: %@ (%d)",self,[self hash]);
#endif

	if (pageContext!=nil)
	{
		[pageContext unregisterProxy:self];
	}
	
	if (executionContext!=nil)
	{
		executionContext = nil;
	}
	
	// remove all listeners JS side proxy
	if (listeners!=nil)
	{
		if (pageContext!=nil)
		{
			TiHost *host = [self _host];
			if (host!=nil)
			{
				for (id type in listeners)
				{
					NSArray *array = [listeners objectForKey:type];
					for (id listener in array)
					{
						[host removeListener:listener context:pageContext];
					}
				}
			}
		}
		RELEASE_TO_NIL(listeners);
	}
	if (dynprops!=nil)
	{
#if TI_USE_PROPERTY_LOCK == 1		
		[dynPropsLock lock];
#endif		
		RELEASE_TO_NIL(dynprops);
#if TI_USE_PROPERTY_LOCK == 1		
		[dynPropsLock unlock];
#endif
	}
	RELEASE_TO_NIL(listeners);
	RELEASE_TO_NIL(baseURL);
	RELEASE_TO_NIL(krollDescription);
	RELEASE_TO_NIL(contextListeners);
	RELEASE_TO_NIL(dynPropsLock);
	pageContext=nil;
	modelDelegate=nil;
	[destroyLock unlock];
}

-(void)dealloc
{
#if PROXY_MEMORY_TRACK == 1
	NSLog(@"DEALLOC: %@ (%d)",self,[self hash]);
#endif
	[self _destroy];
	RELEASE_TO_NIL(destroyLock);
	[super dealloc];
}

-(TiHost*)_host
{
	if (pageContext==nil && executionContext==nil)
	{
		return nil;
	}
	if (pageContext!=nil)
	{
		TiHost *h = [pageContext host];
		if (h!=nil)
		{
			return h;
		}
	}
	if (executionContext!=nil)
	{
		return [executionContext host];
	}
	return nil;
}

-(TiProxy*)currentWindow
{
	return [[self pageContext] preloadForKey:@"currentWindow"];
}

-(NSURL*)_baseURL
{
	if (baseURL==nil)
	{
		TiProxy *currentWindow = [self currentWindow];
		if (currentWindow!=nil)
		{
			// cache it
			[self _setBaseURL:[currentWindow _baseURL]];
			return baseURL;
		}
		return [[self _host] baseURL];
	}
	return baseURL;
}

-(void)_setBaseURL:(NSURL*)url
{
	if (url!=baseURL)
	{
		RELEASE_TO_NIL(baseURL);
		baseURL = [[url absoluteURL] retain];
	} 
}

-(BOOL)_hasListeners:(NSString*)type
{
	return listeners!=nil && [listeners objectForKey:type]!=nil;
}

-(void)_willChangeValue:(id)property value:(id)value
{
	// called before a dynamic property is set against this instance
	// the value is the old value before the change
}

-(void)_didChangeValue:(id)property value:(id)value
{
	// called after a dynamic property is set againt this instance
	// the value is the new value after the change
}

-(void)_fireEventToListener:(NSString*)type withObject:(id)obj listener:(KrollCallback*)listener thisObject:(TiProxy*)thisObject_
{
	[destroyLock lock];
	
	TiHost *host = [self _host];
	
	NSMutableDictionary* eventObject = nil;
	if ([obj isKindOfClass:[NSDictionary class]])
	{
		eventObject = [NSMutableDictionary dictionaryWithDictionary:obj];
	}
	else 
	{
		eventObject = [NSMutableDictionary dictionary];
	}
	
	// common event properties for all events we fire
	[eventObject setObject:type forKey:@"type"];
	[eventObject setObject:self forKey:@"source"];
	
	id<TiEvaluator> evaluator = (id<TiEvaluator>)[listener context].delegate;
	[host fireEvent:listener withObject:eventObject remove:NO context:evaluator thisObject:thisObject_];
	
	[destroyLock unlock];
}

-(void)_listenerAdded:(NSString*)type count:(int)count
{
	// for subclasses
}

-(void)_listenerRemoved:(NSString*)type count:(int)count
{
	// for subclasses
}

// this method will allow a proxy to return a different object back
// for itself when the proxy serialization occurs from native back
// to the bridge layer - the default is to just return ourselves, however,
// in some concrete implementations you really want to return a different
// representation which this will allow. the resulting value should not be 
// retained
-(id)_proxy:(TiProxyBridgeType)type
{
	return self;
}

#pragma mark Public

-(id<NSFastEnumeration>)allKeys
{
	return [dynprops allKeys];
}

/*
 *	In views where the order in which keys are applied matter (I'm looking at you, TableView), this should be
 *  an array of which keys go first, and in what order. Otherwise, this is nil.
 */
-(NSArray *)keySequence
{
	return nil;
}

-(void)addEventListener:(NSArray*)args
{
	NSString *type = [args objectAtIndex:0];
	KrollCallback* listener = [args objectAtIndex:1];
	ENSURE_TYPE(listener,KrollCallback);
	
	if (listeners==nil)
	{
		listeners = [[NSMutableDictionary alloc] init];
	}

	NSMutableArray *l = [listeners objectForKey:type];
	if (l==nil)
	{
		l = [[NSMutableArray alloc] init];
		[listeners setObject:l forKey:type];
		[l release];
	}
	ListenerEntry *entry = [[[ListenerEntry alloc] initWithListener:listener context:[self executionContext] proxy:self type:type] autorelease];
	[l addObject:entry];
	[self _listenerAdded:type count:[l count]];
}
	  
-(void)removeEventListener:(NSArray*)args
{
	NSString *type = [args objectAtIndex:0];
	KrollCallback *listener = [args objectAtIndex:1];
	
	// hold during event
	[[listener retain] autorelease];

	int count = 0;
	
	NSMutableArray *l = [listeners objectForKey:type];
	if (l!=nil && [l count]>0)
	{
		for (ListenerEntry *entry in [NSArray arrayWithArray:l])
		{
			if ([[entry listener] isEqual:listener])
			{
				[l removeObject:entry];
				break;
			}
		}
		
		count = [l count];
		
		// once empty, remove the object
		if (count==0)
		{
			[listeners removeObjectForKey:type];
		}
		
		// once we have no more listeners, release memory!
		if ([listeners count]==0)
		{
			[listeners autorelease];
			listeners = nil;
		}
	}
	id<TiEvaluator> ctx = (id<TiEvaluator>)[listener context];
	[[self _host] removeListener:listener context:ctx];
	[self _listenerRemoved:type count:count];
}

-(void)fireEvent:(id)args
{
	NSString *type = nil;
	id params = nil;
	if ([args isKindOfClass:[NSArray class]])
	{
		type = [args objectAtIndex:0];
		if ([args count] > 1)
		{
			params = [args objectAtIndex:1];
		}
	}
	else if ([args isKindOfClass:[NSString class]])
	{
		type = (NSString*)args;
	}
	[self fireEvent:type withObject:params withSource:self propagate:YES];
}

-(void)fireEvent:(NSString*)type withObject:(id)obj
{
	[self fireEvent:type withObject:obj withSource:self propagate:YES];
}

-(void)fireEvent:(NSString*)type withObject:(id)obj withSource:(id)source
{
	[self fireEvent:type withObject:obj withSource:source propagate:YES];
}

-(void)fireEvent:(NSString*)type withObject:(id)obj propagate:(BOOL)yn
{
	[self fireEvent:type withObject:obj withSource:self propagate:yn];
}

-(void)fireEvent:(NSString*)type withObject:(id)obj withSource:(id)source propagate:(BOOL)propagate
{
	if (![self _hasListeners:type])
	{
		return;
	}

	[destroyLock lock];
	
	if (listeners!=nil)
	{
		NSMutableArray *l = [listeners objectForKey:type];
		if (l!=nil)
		{
			TiHost *host = [self _host];
			
			NSMutableDictionary* eventObject = nil;
			if ([obj isKindOfClass:[NSDictionary class]])
			{
				eventObject = [NSMutableDictionary dictionaryWithDictionary:obj];
			}
			else 
			{
				eventObject = [NSMutableDictionary dictionary];
			}
			
			// common event properties for all events we fire
			[eventObject setObject:type forKey:@"type"];
			[eventObject setObject:source forKey:@"source"];
			
			// unfortunately we have to make a copy to be able to mutate and still iterate
			NSMutableArray *_listeners = [NSMutableArray arrayWithArray:l];
			for (ListenerEntry *entry in _listeners)
			{
				KrollCallback* listener = [entry listener];
				id<TiEvaluator> evaluator = (id<TiEvaluator>)[listener context].delegate;
				if ([[listener context] running])
				{
					[host fireEvent:listener withObject:eventObject remove:NO context:evaluator thisObject:nil];
				}
				else
				{
					// this happens when we have stored an event callback for a context that has
					// been shutdown... in this case, we go ahead and remove the listener and clean
					// up the listener
					[l removeObject:entry];
					[[self _host] removeListener:listener context:pageContext];
					[self _listenerRemoved:type count:[l count]];
				}
			}
			// if we ended up removing all our listeners
			if ([l count]==0)
			{
				[listeners removeObjectForKey:type];
			}
		}
	}
	[destroyLock unlock];
}

- (void)setValuesForKeysWithDictionary:(NSDictionary *)keyedValues
{
	NSArray * keySequence = [self keySequence];

	for (NSString * thisKey in keySequence)
	{
		id thisValue = [keyedValues objectForKey:thisKey];
		if (thisValue == nil) //Dictionary doesn't have this key. Skip.
		{
			continue;
		}
		if (thisValue == [NSNull null]) 
		{ 
			//When a null, we want to write a nil.
			thisValue = nil;
		}
		[self setValue:thisValue forKey:thisKey];
	}

	for (NSString * thisKey in keyedValues)
	{
		if ([keySequence containsObject:thisKey]) continue;
		[self setValue:[keyedValues valueForKey:thisKey] forKey:thisKey];
	}
}
 
DEFINE_EXCEPTIONS


-(BOOL)_propertyInitRequiresUIThread
{
	// tell our constructor not to place _initWithProperties on UI thread by default
	return NO;
}

- (id) valueForUndefinedKey: (NSString *) key
{
	if ([key isEqualToString:@"toString"] || [key isEqualToString:@"valueOf"])
	{
		return [self description];
	}
	if (dynprops != nil)
	{
#if TI_USE_PROPERTY_LOCK == 1		
		[dynPropsLock lock];
#endif
		id result = [dynprops objectForKey:key];
#if TI_USE_PROPERTY_LOCK == 1		
		[dynPropsLock unlock];
#endif		
		// if we have a stored value as complex, just unwrap 
		// it and return the internal value
		if ([result isKindOfClass:[TiComplexValue class]])
		{
			TiComplexValue *value = (TiComplexValue*)result;
			return [value value];
		}
		return result;
	}
	//NOTE: we need to return nil here since in JS you can ask for properties
	//that don't exist and it should return undefined, not an exception
	return nil;
}

- (void) replaceValue:(id)value forKey:(NSString*)key notification:(BOOL)notify
{
	// used for replacing a value and controlling model delegate notifications
	if (value==nil)
	{
		value = [NSNull null];
	}
	id current = nil;
	if (!ignoreValueChanged)
	{
#if TI_USE_PROPERTY_LOCK == 1		
		if (dynPropsLock==nil)
		{
			dynPropsLock = [[NSRecursiveLock alloc] init];
		}
		[dynPropsLock lock];
#endif
		if (dynprops==nil)
		{
			dynprops = [[NSMutableDictionary alloc] init];
		}
		else
		{
			// hold it for this invocation since set may cause it to be deleted
			current = [dynprops objectForKey:key];
			if (current!=nil)
			{
				current = [[current retain] autorelease];
			}
		}
		if ((current!=value)&&![current isEqual:value])
		{
			[dynprops setValue:value forKey:key];
		}
#if TI_USE_PROPERTY_LOCK == 1		
		[dynPropsLock unlock];
#endif
	}
	
	if (notify && self.modelDelegate!=nil)
	{
		[self.modelDelegate propertyChanged:key oldValue:current newValue:value proxy:self];
	}
}

- (void) deleteKey:(NSString*)key
{
	if (dynprops!=nil)
	{
		[dynprops removeObjectForKey:key];
	}
}

- (void) setValue:(id)value forUndefinedKey: (NSString *) key
{
	id current = nil;
#if TI_USE_PROPERTY_LOCK == 1		
	if (dynPropsLock==nil)
	{
		dynPropsLock = [[NSRecursiveLock alloc] init];
	}
	[dynPropsLock lock];
#endif
	if (dynprops!=nil)
	{
		// hold it for this invocation since set may cause it to be deleted
		current = [[[dynprops objectForKey:key] retain] autorelease];
		if (current==[NSNull null])
		{
			current = nil;
		}
	}
	else
	{
		dynprops = [[NSMutableDictionary alloc] init];
	}

	id propvalue = value;
	
	if (value == nil)
	{
		propvalue = [NSNull null];
	}
	else if (value == [NSNull null])
	{
		value = nil;
	}
		
	// notify our delegate
	if (current!=value)
	{
		[dynprops setValue:propvalue forKey:key];
#if TI_USE_PROPERTY_LOCK == 1		
		[dynPropsLock unlock];
#endif
		if (self.modelDelegate!=nil)
		{
			[[(NSObject*)self.modelDelegate retain] autorelease];
			[self.modelDelegate propertyChanged:key oldValue:current newValue:value proxy:self];
		}
		return; // so we don't unlock twice
	}
#if TI_USE_PROPERTY_LOCK == 1		
	[dynPropsLock unlock];
#endif
}

-(NSDictionary*)allProperties
{
	return [[dynprops copy] autorelease];
}

-(id)sanitizeURL:(id)value
{
	if (value == [NSNull null])
	{
		return nil;
	}

	NSURL * result = [TiUtils toURL:value proxy:self];
	if (result == nil)
	{
		return value;
	}
	
	return result;
}

#pragma mark Memory Management

-(void)didReceiveMemoryWarning:(NSNotification*)notification
{
	//FOR NOW, we're not dropping anything but we'll want to do before release
	//subclasses need to call super if overriden
}
 
#pragma mark Dispatching Helper

-(void)_dispatchWithObjectOnUIThread:(NSArray*)args
{
	//NOTE: this is called by ENSURE_UI_THREAD_WITH_OBJ and will always be on UI thread when we get here
	id method = [args objectAtIndex:0];
	id firstobj = [args count] > 1 ? [args objectAtIndex:1] : nil;
	id secondobj = [args count] > 2 ? [args objectAtIndex:2] : nil;
	id target = [args count] > 3 ? [args objectAtIndex:3] : self;
	if (firstobj == [NSNull null])
	{
		firstobj = nil;
	}
	if (secondobj == [NSNull null])
	{
		secondobj = nil;
	}
	SEL selector = NSSelectorFromString([NSString stringWithFormat:@"%@:withObject:",method]);
	[target performSelector:selector withObject:firstobj withObject:secondobj];
}

#pragma mark Description for nice toString in JS
 
-(id)toString:(id)args
{
	if (krollDescription==nil) 
	{ 
		// if we have a cached id, use it for our identifier
		NSString *cn = [self valueForUndefinedKey:@"id"];
		if (cn==nil)
		{
			cn = [[self class] description];
		}
		krollDescription = [[NSString stringWithFormat:@"[object %@]",[cn stringByReplacingOccurrencesOfString:@"Proxy" withString:@""]] retain];
	}

	return krollDescription;
}

-(id)description
{
	return [self toString:nil];
}

-(id)toJSON
{
	// this is called in the case you try and use JSON.stringify and an object is a proxy 
	// since you can't serialize a proxy as JSON, just return null
	return [NSNull null];
}

@end
