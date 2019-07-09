/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-Present by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_DATABASE

#import "TiDatabaseProxy.h"
#import "TiDatabaseResultSetProxy.h"
@import TitaniumKit.TiFilesystemFileProxy;
@import TitaniumKit.TiUtils;

@implementation TiDatabaseProxy

#pragma mark Internal

- (void)dealloc
{
  [self _destroy];
  RELEASE_TO_NIL(name);
  [super dealloc];
}

- (void)shutdown:(id)sender
{
  if (database != nil) {
    [self performSelector:@selector(close) withObject:nil];
  }
}

- (void)_destroy
{
  WARN_IF_BACKGROUND_THREAD_OBJ; //NSNotificationCenter is not threadsafe!
  [[NSNotificationCenter defaultCenter] removeObserver:self name:kTiShutdownNotification object:nil];
  [self shutdown:nil];
  [super _destroy];
}

- (void)_configure
{
  WARN_IF_BACKGROUND_THREAD_OBJ; //NSNotificationCenter is not threadsafe!
  [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(shutdown:) name:kTiShutdownNotification object:nil];
  [super _configure];
}

- (NSString *)apiName
{
  return @"Ti.Database.DB";
}

- (NSString *)dbDir
{
  // See this apple tech note for why this changed: https://developer.apple.com/library/ios/#qa/qa1719/_index.html
  // Apparently following these guidelines is now required for app submission

  NSString *rootDir = [NSSearchPathForDirectoriesInDomains(NSLibraryDirectory, NSUserDomainMask, YES) objectAtIndex:0];
  NSString *dbPath = [rootDir stringByAppendingPathComponent:@"Private Documents"];
  NSFileManager *fm = [NSFileManager defaultManager];

  BOOL isDirectory;
  BOOL exists = [fm fileExistsAtPath:dbPath isDirectory:&isDirectory];

  // Because of sandboxing, this should never happen, but we still need to handle it.
  if (exists && !isDirectory) {
    NSLog(@"[WARN] Recreating file %@... should be a directory and isn't.", dbPath);
    [fm removeItemAtPath:dbPath error:nil];
    exists = NO;
  }

  // create folder, and migrate the old one if necessary
  if (!exists) {
    [fm createDirectoryAtPath:dbPath withIntermediateDirectories:YES attributes:nil error:nil];
  }

  // Migrate any old data if available
  NSString *oldRoot = [NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, YES) objectAtIndex:0];
  NSString *oldPath = [oldRoot stringByAppendingPathComponent:@"database"];
  BOOL oldCopyExists = [fm fileExistsAtPath:oldPath isDirectory:&isDirectory];
  if (oldCopyExists && isDirectory) {
    NSDirectoryEnumerator *contents = [fm enumeratorAtPath:oldPath];
    //This gives relative paths. So create full path before moving
    for (NSString *oldFile in contents) {
      [fm moveItemAtPath:[oldPath stringByAppendingPathComponent:oldFile] toPath:[dbPath stringByAppendingPathComponent:oldFile] error:nil];
    }

    // Remove the old copy after migrating everything
    [fm removeItemAtPath:oldPath error:nil];
  }

  return dbPath;
}

- (NSString *)dbPath:(NSString *)name_
{
  NSString *dbDir = name_;
  if (![name_ hasPrefix:@"/"] && ![name_ hasPrefix:@"file:"]) {
    dbDir = [[self dbDir] stringByAppendingPathComponent:name_];
  }

  return [dbDir stringByAppendingPathExtension:@"sql"];
}

- (void)open:(NSString *)name_
{
  name = [name_ retain];
  NSString *path = [self dbPath:name];

  database = [[PLSqliteDatabase alloc] initWithPath:path];
  if (![database open]) {
    [self throwException:@"couldn't open database" subreason:name_ location:CODELOCATION];
  }
}

- (void)install:(NSString *)path name:(NSString *)name_
{
  BOOL isDirectory;
  NSFileManager *fm = [NSFileManager defaultManager];
  NSURL *url = [TiUtils toURL:path proxy:self];
  path = [url path];

#if TARGET_IPHONE_SIMULATOR
  //TIMOB-6081. Resources are right now symbolic links when running in simulator) so the copy method
  //of filemanager just creates a link to the original resource.
  //Resolve the symbolic link if running in simulator
  NSError *pathError = nil;
  NSDictionary *attributes = [fm attributesOfItemAtPath:path error:&pathError];
  if (pathError != nil) {
    [self throwException:@"Could not retrieve attributes" subreason:[pathError description] location:CODELOCATION];
  }
  NSString *fileType = [attributes valueForKey:NSFileType];
  if ([fileType isEqual:NSFileTypeSymbolicLink]) {
    pathError = nil;
    path = [fm destinationOfSymbolicLinkAtPath:path error:&pathError];

    if (pathError != nil) {
      [self throwException:@"Could not resolve symbolic link" subreason:[pathError description] location:CODELOCATION];
    }
  }

#endif

  BOOL exists = [fm fileExistsAtPath:path isDirectory:&isDirectory];
  if (!exists) {
    [self throwException:@"invalid database install path" subreason:path location:CODELOCATION];
  }

  // get the install path
  NSString *installPath = [self dbPath:name_];

  // see if we have already installed the DB
  exists = [fm fileExistsAtPath:installPath isDirectory:&isDirectory];
  if (!exists) {
    NSError *error = nil;
    // install it by copying it
    [fm copyItemAtPath:path toPath:installPath error:&error];
    if (error != nil) {
      [self throwException:@"couldn't install database" subreason:[error description] location:CODELOCATION];
    }
  }

  [self open:name_];
}

- (void)removeStatement:(PLSqliteResultSet *)statement
{
  @synchronized(self) {
    [statement close];
    if (statements != nil) {
      [statements removeObject:statement];
    }
  }
}

- (void)addStatement:(PLSqliteResultSet *)statement
{
  @synchronized(self) {
    if (statements == nil) {
      statements = [[NSMutableArray alloc] initWithCapacity:5];
    }
    [statements addObject:statement];
  }
}

- (NSArray *)sqlParams:(NSArray *)array
{
  NSMutableArray *result = [NSMutableArray arrayWithCapacity:[array count]];
  for (JSValue *jsValue in array) {
    // This uses the Titanium conversion method. We could probably write a nicer one
    // that simply used the Obj-C JSC API checking isString/isDate/isArray/isObject/isBoolean/isNumber
    id value = [self JSValueToNative:jsValue];
    if (value == nil) {
      [result addObject:[NSNull null]];
    } else {
      [result addObject:value];
    }
  }

  return result;
}

- (PLSqlitePreparedStatement *)prepareStatement:(NSString *)sql withParams:(NSArray *)params withError:(NSError *__nullable *__nullable)error
{
  // FIXME: Can we use NSError reference and avoid try/catch/throw? Obj-C doesn't like using exceptions
  if (database == nil) {
    *error = [NSError errorWithDomain:NSCocoaErrorDomain
                                 code:123
                             userInfo:@{
                               NSLocalizedDescriptionKey : @"database has been closed"
                             }];
    return nil;
  }
  NSString *sqlCleaned = [sql stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];

  PLSqlitePreparedStatement *statement;
  // Just use a lock here?
  @synchronized(self) {
    statement = (PLSqlitePreparedStatement *)[database prepareStatement:sqlCleaned error:error];
  }
  if (*error != nil) {
    return nil;
  }
  if (params && [params count] > 0) {
    [statement bindParameters:params];
  }

  return statement;
}

- (TiDatabaseResultSetProxy *)executeSQL:(NSString *)sql withParams:(NSArray *)params withError:(NSError *__nullable *__nullable)error
{
  @synchronized(self) {
    PLSqlitePreparedStatement *statement = [self prepareStatement:sql withParams:params withError:error];
    if (*error != nil) {
      return nil;
    }
    PLSqliteResultSet *result = (PLSqliteResultSet *)[statement executeQuery];

    // Do we need to lock for the next/close calls?
    if ([[result fieldNames] count] == 0) {
      [result next]; // we need to do this to make sure lastInsertRowId and rowsAffected work
      [result close];
      return nil;
    }

    [self addStatement:result];

    return [[[TiDatabaseResultSetProxy alloc] initWithResults:result database:self] autorelease];
  }
}

- (TiDatabaseResultSetProxy *)execute:(NSString *)sql
{
  NSArray *params = @[];
  // Check for varargs for perepared statement params
  NSArray *currentArgs = [JSContext currentArguments];
  if ([currentArgs count] > 1) {
    JSValue *possibleParams = [currentArgs objectAtIndex:1];
    if ([possibleParams isArray]) {
      params = [possibleParams toArray];
    } else {
      params = [self sqlParams:[currentArgs subarrayWithRange:NSMakeRange(1, [currentArgs count] - 1)]];
    }
  }

  NSError *error = nil;
  TiDatabaseResultSetProxy *result = [self executeSQL:sql withParams:params withError:&error];
  if (error != nil) {
    [self throwException:@"failed to execute SQL statement" subreason:[error description] location:CODELOCATION];
    return nil;
  }
  return result;
}

- (void)executeAsync:(NSString *)sql
{
  NSArray *currentArgs = [JSContext currentArguments];
  if ([currentArgs count] < 2) {
    [self throwException:@"callback function must be supplied" subreason:@"" location:CODELOCATION];
    return;
  }
  JSValue *callback = [currentArgs objectAtIndex:[currentArgs count] - 1];

  NSArray *params = @[];
  if ([currentArgs count] > 2) {
    JSValue *possibleParams = [currentArgs objectAtIndex:1];
    if ([possibleParams isArray]) {
      params = [possibleParams toArray];
    } else {
      params = [self sqlParams:[currentArgs subarrayWithRange:NSMakeRange(1, [currentArgs count] - 2)]];
    }
  }

  // FIXME: Use a queue per-database! Also, use queue in the sync variants!
  dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
    NSError *error = nil;
    TiDatabaseResultSetProxy *proxy = [self executeSQL:sql withParams:params withError:&error];
    if (error != nil) {
      dispatch_async(dispatch_get_main_queue(), ^{
        JSValue *jsError = [JSValue valueWithNewErrorFromMessage:[NSString stringWithFormat:@"failed to execute SQL statement: %@", [error description]] inContext:[callback context]];
        [callback callWithArguments:@[ jsError ]];
      });
      return;
    }

    dispatch_async(dispatch_get_main_queue(), ^{
      JSContext *context = [callback context];
      [callback callWithArguments:@[ [JSValue valueWithUndefinedInContext:context], proxy == nil ? [JSValue valueWithNullInContext:context] : proxy ]];
    });
  });
}

- (NSArray<TiDatabaseResultSetProxy *> *)executeAll:(NSArray<NSString *> *)queries withContext:(JSContext *)context withError:(NSError *__nullable *__nullable)error
{
  // Do we need to copy the array or something to retain the args?
  NSMutableArray *results = [NSMutableArray arrayWithCapacity:[queries count]];
  for (NSString *sql in queries) {
    TiDatabaseResultSetProxy *result = [self executeSQL:sql withParams:nil withError:error];
    if (*error != nil) {
      // Should we continue trying the others? Right now let's fail right away
      return nil;
    }
    if (result == nil) {
      [results addObject:[JSValue valueWithNullInContext:context]];
    } else {
      [results addObject:result];
    }
  }
  return results;
}

- (NSArray<TiDatabaseResultSetProxy *> *)executeAll:(NSArray<NSString *> *)queries
{
  NSError *error = nil;
  NSArray<TiDatabaseResultSetProxy *> *result = [self executeAll:queries withContext:[JSContext currentContext] withError:&error];
  if (error != nil) {
    [self throwException:@"failed to execute SQL statements" subreason:[error description] location:CODELOCATION];
    return nil;
  }
  return result;
}

- (void)executeAllAsync:(NSArray<NSString *> *)queries withCallback:(JSValue *)callback
{
  dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
    JSContext *context = [callback context];
    NSError *error = nil;
    NSArray<TiDatabaseResultSetProxy *> *results = [self executeAll:queries withContext:context withError:&error];
    if (error != nil) {
      dispatch_async(dispatch_get_main_queue(), ^{
        JSValue *jsError = [JSValue valueWithNewErrorFromMessage:[NSString stringWithFormat:@"failed to execute SQL statements: %@", [error description]] inContext:[callback context]];
        [callback callWithArguments:@[ jsError ]];
      });
      return;
    }

    dispatch_async(dispatch_get_main_queue(), ^{
      [callback callWithArguments:@[ [JSValue valueWithUndefinedInContext:context], results ]];
    });
  });
}

- (void)close
{
  @synchronized(self) {
    if (statements != nil) {
      for (PLSqliteResultSet *result in statements) {
        [result close];
      }
      RELEASE_TO_NIL(statements);
    }
    if (database != nil) {
      if ([database goodConnection]) {
        @try {
          [database close];
        }
        @catch (NSException *e) {
          NSLog(@"[WARN] attempting to close database, returned error: %@", e);
        }
      }
      RELEASE_TO_NIL(database);
    }
  }
}

- (void)remove
{
  NSString *dbPath = [self dbPath:name];
  [[NSFileManager defaultManager] removeItemAtPath:dbPath error:nil];
}

- (NSUInteger)lastInsertRowId
{
  if (database != nil) {
    return [database lastInsertRowId];
  }
  return 0;
}
GETTER_IMPL(NSUInteger, lastInsertRowId, LastInsertRowId);

- (NSUInteger)rowsAffected
{
  if (database != nil) {
    return sqlite3_changes([database sqliteDB]);
  }
  return 0;
}
GETTER_IMPL(NSUInteger, rowsAffected, RowsAffected);

- (NSString *)name
{
  return name;
}
GETTER_IMPL(NSString *, name, Name);

- (JSValue *)file
{
  return [self NativeToJSValue:[[[TiFilesystemFileProxy alloc] initWithFile:[self dbPath:name]] autorelease]];
}
GETTER_IMPL(JSValue *, file, File);

#pragma mark Internal
- (PLSqliteDatabase *)database
{
  return database;
}

@end

#endif
