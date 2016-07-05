/*
 * Appcelerator Titanium Mobile
 * Copyright (c) 2011-2016 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#include "AssetsModule.h"

#include <stdio.h>
#include <sys/types.h>
#include <v8.h>

#include "AndroidUtil.h"
#include "JNIUtil.h"
#include "JSException.h"
#include "TypeConverter.h"
#include "V8Util.h"

#define TAG "AssetsModule"

namespace titanium {

using namespace v8;

void AssetsModule::Initialize(Local<Object> target, Local<Context> context)
{
	Isolate* isolate = context->GetIsolate();
	SetMethod(isolate, target, "readAsset", readAsset);
	SetMethod(isolate, target, "readFile", readFile);
}

void AssetsModule::readAsset(const FunctionCallbackInfo<Value>& args)
{
	v8::Isolate* isolate = args.GetIsolate();
	if (args.Length() < 1) {
		JSException::Error(isolate, "Missing required argument 'resourceName'.");
		return;
	}

	JNIEnv *env = JNIScope::getEnv();
	if (!env) {
		JSException::GetJNIEnvironmentError(isolate);
		return;
	}

	jstring resourceName = TypeConverter::jsStringToJavaString(env, args[0]->ToString(isolate));

	jstring assetData = (jstring) env->CallStaticObjectMethod(
		JNIUtil::krollAssetHelperClass,
		JNIUtil::krollAssetHelperReadAssetMethod,
		resourceName);

	env->DeleteLocalRef(resourceName);

	if (env->ExceptionCheck()) {
		LOGE(TAG, "Failed to load resource.");
		env->ExceptionDescribe();
		env->ExceptionClear();
		JSException::Error(isolate, "Failed to load resource, Java exception was thrown.");
		return;
	}

	if (!assetData) {
		args.GetReturnValue().Set(v8::Null(isolate));
		return;
	}

	jint len = env->GetStringLength(assetData);
	const jchar *assetChars = env->GetStringChars(assetData, NULL);
	if (!assetChars) {
		args.GetReturnValue().Set(v8::Null(isolate));
		return;
	}

	Local<String> resourceData = String::NewFromTwoByte(isolate, assetChars, v8::String::kNormalString, len);
	env->ReleaseStringChars(assetData, assetChars);
	env->DeleteLocalRef(assetData);

	args.GetReturnValue().Set(resourceData);
}

void AssetsModule::readFile(const FunctionCallbackInfo<Value>& args)
{
	v8::Isolate* isolate = args.GetIsolate();
	HandleScope scope(isolate);

	if (args.Length() == 0 || args[0]->IsNull() || args[0]->IsUndefined()) {
		JSException::Error(isolate, "assets.readFile requires a valid filename");
		return;
	}

	titanium::Utf8Value filename(args[0]);

	FILE *file = fopen(*filename, "r");

	if (!file) {
		JSException::Error(isolate, "Error opening file");
		return;
	}

	if (fseek(file, 0L, SEEK_END) != 0) {
		fclose(file);
		JSException::Error(isolate, "Error reading file");
		return;
	}

	long fileLength;
	if ((fileLength = ftell(file)) == -1) {
		fclose(file);
		JSException::Error(isolate, "Error getting file length");
		return;
	}

	rewind(file);

	char *buffer = new char[fileLength];

	fread(buffer, fileLength, 1, file);
	fclose(file);

	if (ferror(file) != 0) {
		JSException::Error(isolate, "Error while reading file");
		return;
	}

	LOGD(TAG, "got file data: %d bytes", fileLength);

	Local<String> data = String::NewFromUtf8(isolate, const_cast<const char *>(buffer), v8::String::kNormalString, fileLength);
	delete[] buffer;

	args.GetReturnValue().Set(data);
}

}
