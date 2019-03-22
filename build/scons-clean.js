#!/usr/bin/env node
'use strict';

const version = require('../package.json').version;
const program = require('commander');

program.option('-v, --sdk-version [version]', 'Override the SDK version we report', process.env.PRODUCT_VERSION || version)
	.option('-t, --version-tag [tag]', 'Override the SDK version tag we report')
	.option('-a, --api-level [number]', 'Explicitly set the Android SDK API level used for building')
	.option('-s, --android-sdk [path]', 'Explicitly set the path to the Android SDK used for building', process.env.ANDROID_SDK)
	.option('-n, --android-ndk [path]', 'Explicitly set the path to the Android NDK used for building', process.env.ANDROID_NDK)
	.parse(process.argv);

const Builder = require('./lib/builder');
new Builder(program).clean()
	.then(() => process.exit(0))
	.catch(err => {
		console.error(err);
		return process.exit(1);
	});
