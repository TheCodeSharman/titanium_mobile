/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_ACCELEROMETER

#import "AccelerometerModule.h"

@implementation AccelerometerModule

- (void)dealloc
{
  [self stopGeneratingEvents];
  [super dealloc];
}

- (NSString *)apiName
{
  return @"Ti.Accelerometer";
}

- (void)startGeneratingEvents
{
  if (_motionManager == nil) {
    _motionManager = [[CMMotionManager alloc] init];
    if (_motionManager.isAccelerometerAvailable) {
      _motionQueue = [[NSOperationQueue alloc] init];
      _motionManager.accelerometerUpdateInterval = 1.0 / 10.0;
      oldTime = CFAbsoluteTimeGetCurrent();
      [_motionManager startAccelerometerUpdatesToQueue:_motionQueue
                                           withHandler:^(CMAccelerometerData *accelerometerData, NSError *error) {
                                             if (error == nil) {
                                               NSDictionary *event = [NSDictionary dictionaryWithObjectsAndKeys:
                                                                                       NUMDOUBLE(accelerometerData.acceleration.x), @"x",
                                                                                   NUMDOUBLE(accelerometerData.acceleration.y), @"y",
                                                                                   NUMDOUBLE(accelerometerData.acceleration.z), @"z",
                                                                                   NUMLONGLONG((CFAbsoluteTimeGetCurrent() - oldTime) * 1000), @"timestamp",
                                                                                   nil];
                                               [self fireEvent:@"update" withObject:event];
                                             } else {
                                               DebugLog(@"Error in event - %@", [TiUtils messageFromError:error]);
                                             }
                                           }];

    } else {
      DebugLog(@"Accelerometer is Unavailable on this device");
      RELEASE_TO_NIL(_motionManager);
    }
  }
}

- (void)stopGeneratingEvents
{
  if (_motionManager != nil) {
    [_motionManager stopAccelerometerUpdates];
    RELEASE_TO_NIL_AUTORELEASE(_motionQueue);
    RELEASE_TO_NIL_AUTORELEASE(_motionManager);
  }
}

- (void)_listenerAdded:(NSString *)type count:(int)count
{
  if ((count == 1) && [type isEqualToString:@"update"]) {
    [self startGeneratingEvents];
  }
}

- (void)_listenerRemoved:(NSString *)type count:(int)count
{
  if ((count == 0) && [type isEqualToString:@"update"]) {
    [self stopGeneratingEvents];
  }
}

@end

#endif