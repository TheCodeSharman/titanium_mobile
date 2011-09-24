/*
 * Appcelerator Titanium Mobile
 * Copyright (c) 2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#ifndef ASSETS_MODULE_H
#define ASSETS_MODULE_H

#include <v8.h>

using namespace v8;

namespace titanium {

class AssetsModule
{
public:
	static void Initialize(Handle<Object> target);
	static Handle<Value> readResource(const Arguments& args);
};

}

#endif
