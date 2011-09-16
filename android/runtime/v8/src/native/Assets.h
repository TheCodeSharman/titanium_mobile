/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifndef ASSETS_H
#define ASSETS_H

#include <v8.h>

namespace assets
{
	v8::Handle<v8::Primitive> readResource(v8::Handle<v8::String> path);
}

#endif
