/*
 * clean.js: Titanium Mobile CLI clean command
 *
 * Copyright (c) 2012, Appcelerator, Inc.  All Rights Reserved.
 * See the LICENSE file for more information.
 */

var appc = require('node-appc'),
	lib = require('./lib/common'),
	fs = require('fs'),
	path = require('path'),
	wrench = require('wrench');

exports.config = function (logger, config, cli) {
	return {
		desc: __('creates a new mobile application or module'),
		options: appc.util.mix({
			platform: {
				// note: --platform is not required for the clean command
				abbr: 'p',
				desc: __('a platform to clean'),
				values: lib.availablePlatforms
			},
			dir: {
				abbr: 'd',
				desc: __('the directory containing the project, otherwise the current working directory')
			}
		}, lib.commonOptions(logger, config))
	};
};

exports.validate = function (logger, config, cli) {
	cli.argv.platform = lib.validatePlatform(cli.argv.platform);
	cli.argv.dir = lib.validateProjectDir(cli.argv.dir);
};

exports.run = function (logger, config, cli) {
	var buildDir = path.join(cli.argv.dir, 'build');
	
	appc.fs.touch(path.join(cli.argv.dir, 'tiapp.xml'));
	
	if (cli.argv.platform) {
		var dir = path.join(buildDir, cli.argv.platform);
		appc.fs.exists(dir) && wrench.rmdirSyncRecursive(dir);
	} else {
		fs.readdirSync(buildDir).forEach(function (dir) {
			dir = path.join(buildDir, dir);
			if (fs.lstatSync(dir).isDirectory()) {
				wrench.rmdirSyncRecursive(dir);
			}
		});
	}
	
	logger.log(__('Project cleaned successfully in %s', appc.time.printDiff(cli.startTime, Date.now())) + '\n');
};
