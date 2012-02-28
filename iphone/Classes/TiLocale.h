/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#import "TiBase.h"

/**
 The class represents locale.
 */
@interface TiLocale : NSObject {
	NSString *currentLocale;
	NSBundle *bundle;
}

/**
 Returns the current locate.
 @see currentLocale
 */
@property(nonatomic,readwrite,retain) NSString *currentLocale;

/**
 Returns bundle.
 */
@property(nonatomic,readwrite,retain) NSBundle *bundle;

/**
 Return current locale.
 @see currentLocale
 */
+(NSString*)currentLocale;

/**
 Sets current locale.
 @param locale The locale to set.
 */
+(void)setLocale:(NSString*)locale;

/**
 Return localized text for the key.
 @param key The text key.
 @param comment The default value.
 */
+(NSString*)getString:(NSString*)key comment:(NSString*)comment;

@end
