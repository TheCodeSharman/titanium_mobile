/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#ifdef USE_TI_UICLIPBOARD
#import "TiProxy.h"
@interface TiUIClipboardProxy : TiProxy {
@private
    UIPasteboard *_pasteboard;
    NSString *pasteboardName;
    BOOL shouldCreatePasteboard;
    BOOL persistent;
    BOOL isNamedPasteBoard;
}

#pragma mark internal
-(id)getData_:(NSString *)mimeType;

-(void)clearData:(id)args;
-(void)clearText:(id)args;
-(id)getData:(id)args;
-(NSString *)getText:(id)args;
-(id)hasData:(id)args;
-(id)hasText:(id)unused;
-(void)setData:(id)args;
-(void)setText:(id)args;

@end
#endif
