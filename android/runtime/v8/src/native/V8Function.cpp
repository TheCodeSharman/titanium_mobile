/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#include <jni.h>
#include <v8.h>

#include "JNIUtil.h""
#include "TypeConverter.h"

#define TAG "V8Function"

using namespace titanium;

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     org_appcelerator_kroll_runtime_v8_V8Function
 * Method:    nativeInvoke
 * Signature: (J[Ljava/lang/Object)V
 */
JNIEXPORT void JNICALL Java_org_appcelerator_kroll_runtime_v8_V8Object_nativeInvoke(
	JNIEnv *env, jobject caller, jlong functionPointer, jobjectarray functionArguments)
{
	titanium::JNIScope jniScope(env);
	HandleScope scope;

	// construct function from pointer
	v8::Handle<v8::Function> jsFunction((v8::Function *) functionPointer);

	// convert the Java array to a V8 function arguments array
	jsize arrayLength = env->GetArrayLength(functionArguments);
	v8::Handle<v8::Value> jsFunctionArguments[jsize];
	for (int i = 0; i < arrayLength; i++)
	{
		jobject arrayElement = env->GetObjectArrayElement(functionArguments);
		jsFunctionArguments[i] = TypeConverter::javaObjectToJsValue(arrayElement);
		env->DeleteLocalRef(arrayElement);
	}


	// call into the JS function with the provided argument
	jsFunction->Call(jsFunction, 1, jsFunctionArgs);
}

#ifdef __cplusplus
}
#endif

