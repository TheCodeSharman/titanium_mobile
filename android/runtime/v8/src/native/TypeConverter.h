/*
 * Appcelerator Titanium Mobile
 * Copyright (c) 2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#ifndef TYPE_CONVERTER_H
#define TYPE_CONVERTER_H

#include <jni.h>
#include <v8.h>

namespace titanium {
class TypeConverter
{
public:
	// short convert methods
	static jshort jsNumberToJavaShort(v8::Handle<v8::Number> jsNumber);
	static v8::Handle<v8::Number> javaShortToJsNumber(jshort javaShort);

	// int convert methods
	static jint jsNumberToJavaInt(v8::Handle<v8::Number> jsNumber);
	static v8::Handle<v8::Number> javaIntToJsNumber(jint javaInt);

	// long convert methods
	static jlong jsNumberToJavaLong(v8::Handle<v8::Number> jsNumber);
	static v8::Handle<v8::Number> javaLongToJsNumber(jlong javaLong);

	// float convert methods
	static jfloat jsNumberToJavaFloat(v8::Handle<v8::Number> jsNumber);
	static v8::Handle<v8::Number> javaFloatToJsNumber(jfloat javaFloat);

	// double convert methods
	static jdouble jsNumberToJavaDouble(v8::Handle<v8::Number> jsNumber);
	static v8::Handle<v8::Number> javaDoubleToJsNumber(jdouble javaDouble);

	// boolean convert methods
	static jboolean jsBooleanToJavaBoolean(v8::Handle<v8::Boolean> jsBoolean);
	static v8::Handle<v8::Boolean> javaBooleanToJsBoolean(jboolean javaBoolean);

	// string convert methods
	static jstring jsStringToJavaString(v8::Handle<v8::String> jsString);
	static jstring jsValueToJavaString(v8::Handle<v8::Value> jsValue);
	static v8::Handle<v8::String> javaStringToJsString(jstring javaString);

	// date convert methods
	static jobject jsDateToJavaDate(v8::Handle<v8::Date> jsDate);
	static jlong jsDateToJavaLong(v8::Handle<v8::Date> jsDate);
	static v8::Handle<v8::Date> javaDateToJsDate(jobject);
	static v8::Handle<v8::Date> javaLongToJsDate(jlong);

	// function convert methods
	static jobject jsObjectToJavaFunction(v8::Handle<v8::Object> jsObject);

	// arguments conversion
	static jobjectArray jsArgumentsToJavaArray(const v8::Arguments& args);

	// BIG FUCKING WARNING!!  make sure you call "delete" on the return value otherwise the
	// you have introduced a memory leak and the world will end.  plzkthksbye
	static v8::Handle<v8::Value> * javaObjectArrayToJsArguments(jobjectArray javaObjectArray, int *length);

	// array convert methods
	static jarray jsArrayToJavaArray(v8::Handle<v8::Array>);
	static v8::Handle<v8::Array> javaArrayToJsArray(jbooleanArray javaBooleanArray);
	static v8::Handle<v8::Array> javaArrayToJsArray(jshortArray javaShortArray);
	static v8::Handle<v8::Array> javaArrayToJsArray(jintArray javaIntArray);
	static v8::Handle<v8::Array> javaArrayToJsArray(jlongArray javaLongArray);
	static v8::Handle<v8::Array> javaArrayToJsArray(jfloatArray javaFloatArray);
	static v8::Handle<v8::Array> javaArrayToJsArray(jdoubleArray javaDoubleArray);
	static v8::Handle<v8::Array> javaArrayToJsArray(jobjectArray javaObjectArray);

	// object convert methods
	static jobject jsValueToJavaObject(v8::Local<v8::Value> jsValue);
	static v8::Handle<v8::Value> javaObjectToJsValue(jobject javaObject);

	// Convert a JS object's indexed properties to a Java object array.
	// Starts at index zero and continues until length is reached.
	static jobjectArray jsObjectIndexPropsToJavaArray(v8::Handle<v8::Object> jsObject, int length);

private:
	// utility methods
	static v8::Handle<v8::Array> javaDoubleArrayToJsNumberArray(jdoubleArray javaDoubleArray);

};
}

#endif

