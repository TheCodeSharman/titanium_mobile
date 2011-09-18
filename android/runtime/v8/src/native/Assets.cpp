/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#include <jni.h>
#include <v8.h>

#include "Assets.h"
#include "JNIUtil.h"
#include "TypeConverter.h"
#include "V8Util.h"

using namespace titanium;

v8::Handle<v8::Primitive> Assets::readResource(const char* path)
{
	JNIEnv *env = JNIUtil::getJNIEnv();
	if (!env) {
		return v8::Null();
	}

	jstring jpath = env->NewStringUTF(path);
	jcharArray jarray = (jcharArray) env->CallStaticObjectMethod(JNIUtil::assetsClass, JNIUtil::assetsReadResourceMethod, jpath);
	env->DeleteLocalRef(jpath);

	if (env->ExceptionCheck()) {
		env->ExceptionClear();
		// TODO: throw JS exception
		return v8::Null();
	}

	if (!jarray) {
		return v8::Null();
	}

	jint len = env->GetArrayLength(jarray);
	jchar *pchars = (jchar*) env->GetPrimitiveArrayCritical(jarray, 0);
	if (!pchars) {
		return v8::Null();
	}

	v8::Handle<v8::String> jsString = v8::String::New(pchars, len);
	env->ReleasePrimitiveArrayCritical(jarray, pchars, 0);
	return jsString;
}


