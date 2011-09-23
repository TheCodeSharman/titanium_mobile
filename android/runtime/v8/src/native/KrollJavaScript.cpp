/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#include <v8.h>

#include "EventEmitter.h"
#include "KrollJavaScript.h"
#include "V8Util.h"
#include "KrollNatives.cpp"

#include "org.appcelerator.kroll.KrollProxy.h"
#include "org.appcelerator.kroll.KrollModule.h"

namespace titanium {
using namespace v8;

void KrollJavaScript::DefineNatives(Handle<Object> target)
{
	HandleScope scope;
	for (int i = 0; natives[i].name; ++i) {
		if (natives[i].source == kroll_native) continue;
		Local<String> name = String::New(natives[i].name);
		Handle<String> source = IMMUTABLE_STRING_LITERAL_FROM_ARRAY(natives[i].source, natives[i].source_length);
		target->Set(name, source);
	}
}

Handle<String> KrollJavaScript::MainSource()
{
	return IMMUTABLE_STRING_LITERAL_FROM_ARRAY(kroll_native, sizeof(kroll_native)-1);
}

void KrollJavaScript::initBaseTypes(Handle<Object> target)
{
	KrollProxy::Initialize(target);
	DEFINE_TEMPLATE(target, "KrollProxy", KrollProxy::proxyTemplate);

	KrollModule::Initialize(target);
}

}
