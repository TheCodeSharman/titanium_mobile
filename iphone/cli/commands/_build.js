/**
 * iOS build command.
 *
 * @module cli/_build
 *
 * @copyright
 * Copyright (c) 2009-2015 by Appcelerator, Inc. All Rights Reserved.
 *
 * @license
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

var appc = require('node-appc'),
	async = require('async'),
	Builder = require('titanium-sdk/lib/builder'),
	cleanCSS = require('clean-css'),
	cyan = require('colors').cyan,
	ejs = require('ejs'),
	fields = require('fields'),
	fs = require('fs'),
	humanize = require('humanize'),
	ioslib = require('ioslib'),
	iosPackageJson = appc.pkginfo.package(module),
	jsanalyze = require('titanium-sdk/lib/jsanalyze'),
	moment = require('moment'),
	path = require('path'),
	spawn = require('child_process').spawn,
	ti = require('titanium-sdk'),
	util = require('util'),
	uuid = require('node-uuid'),
	wrench = require('wrench'),
	xcode = require('xcode'),
	xcodeParser = require('xcode/lib/parser/pbxproj')
	i18n = appc.i18n(__dirname),
	__ = i18n.__,
	__n = i18n.__n,
	parallel = appc.async.parallel,
	series = appc.async.series,
	version = appc.version;

function iOSBuilder() {
	Builder.apply(this, arguments);

	this.minSupportedIosSdk = parseInt(version.parseMin(this.packageJson.vendorDependencies['ios sdk']));
	this.maxSupportedIosSdk = parseInt(version.parseMax(this.packageJson.vendorDependencies['ios sdk']));

	this.deployTypes = {
		'simulator': 'development',
		'device': 'test',
		'dist-appstore': 'production',
		'dist-adhoc': 'production'
	};

	this.targets = ['simulator', 'device', 'dist-appstore', 'dist-adhoc'];

	this.deviceFamilies = {
		iphone: '1',
		ipad: '2',
		universal: '1,2',
		watch: '4'
	};

	// populated the first time getDeviceFamily() is called
	this.deviceFamily = null;

	this.deviceFamilyNames = {
		iphone: ['ios', 'iphone'],
		ipad: ['ios', 'ipad'],
		universal: ['ios', 'iphone', 'ipad']
	};

	this.xcodeTargetSuffixes = {
		iphone: '',
		ipad: '-iPad',
		universal: '-universal'
	};

	this.simTypes = {
		iphone: 'iPhone',
		ipad: 'iPad'
	};

	this.blacklistDirectories = [
		'contents',
		'resources'
	];

	this.graylistDirectories = [
		'frameworks',
		'plugins'
	];

	this.ipadLaunchImages = [
		'Default-Landscape.png',
		'Default-Landscape@2x.png',
		'Default-Portrait.png',
		'Default-Portrait@2x.png',
		'Default-LandscapeLeft.png',
		'Default-LandscapeLeft@2x.png',
		'Default-LandscapeRight.png',
		'Default-LandscapeRight@2x.png',
		'Default-PortraitUpsideDown.png',
		'Default-PortraitUpsideDown@2x.png'
	];

	this.templatesDir = path.join(this.platformPath, 'templates', 'build');

	this.tiSymbols = {};

	// when true, uses the JavaScriptCore that ships with iOS instead of the original Titanium version
	this.useJSCore=false;

	// populated when config() is called after iOS info has been detected
	this.defaultIosVersion = null;

	// populated the first time getDeviceInfo() is called
	this.deviceInfoCache = null;

	// cache of provisioning profiles
	this.provisioningProfileLookup = {};

	// list of all extensions (including watch apps)
	this.extensions = [];

	// when true and building an app with a watch extension for the simulator and the --launch-watch-app
	// flag is passed in, then show the external display and launch the watch app
	this.hasWatchApp = false;

	// the parsed build manifest from the previous build
	this.previousBuildManifest = {};

	// contains the current build's info
	this.currentBuildManifest = {
		files: {}
	};

	// when true, the entire build dir is nuked at the start of the build
	this.forceCleanBuild = false;

	// when true, calls xcodebuild
	this.forceRebuild = false;
}

util.inherits(iOSBuilder, Builder);

iOSBuilder.prototype.assertIssue = function assertIssue(issues, name) {
	var i = 0,
		len = issues.length;
	for (; i < len; i++) {
		if ((typeof name === 'string' && issues[i].id === name) || (typeof name === 'object' && name.test(issues[i].id))) {
			this.logger.banner();
			appc.string.wrap(issues[i].message, this.config.get('cli.width', 100)).split('\n').forEach(function (line, i, arr) {
				this.logger.error(line.replace(/(__(.+?)__)/g, '$2'.bold));
				if (!i && arr.length > 1) this.logger.log();
			}, this);
			this.logger.log();
			process.exit(1);
		}
	}
};

iOSBuilder.prototype.getDeviceInfo = function getDeviceInfo() {
	if (this.deviceInfoCache) {
		return this.deviceInfoCache;
	}

	var argv = this.cli.argv,
		deviceInfo = {
			devices: [],
			udids: {},
			maxName: 0,
			preferred: null
		};

	if (argv.target === 'device') {
		// build the list of devices
		this.iosInfo.devices.forEach(function (device) {
			device.name.length > deviceInfo.maxName && (deviceInfo.maxName = device.name.length);
			deviceInfo.devices.push({
				udid: device.udid,
				name: device.name,
				deviceClass: device.deviceClass,
				productVersion: device.productVersion
			});
			deviceInfo.udids[device.udid] = device;
		});

		if (this.config.get('ios.autoSelectDevice', true) && !argv['device-id']) {
			deviceInfo.preferred = deviceInfo.devices[0];
		}
	} else if (argv.target === 'simulator') {
		deviceInfo.devices = {};

		// check if they specified the legacy settings: --sim-version, --sim-type, --retina, --tall, --sim-64bit
		if (this.config.get('ios.autoSelectDevice', true) && (argv['sim-version'] || argv['sim-type'] || argv.retina || argv.tall || argv['sim-64bit'])) {
			// try to find the closest matching simulator
			var version = argv['sim-version'] || argv['ios-version'] || this.defaultIosVersion,
				sims = this.iosInfo.simulators,
				candidates = {};

			// find all candidate simulators
			Object.keys(sims).forEach(function (ver) {
				if (!argv['sim-version'] || ver === argv['sim-version']) {
					sims[ver].forEach(function (sim) {
						if ((!argv['sim-type'] || sim.type === argv['sim-type']) && (!argv.retina || sim.retina) && (!argv.tall || sim.tall) && (!argv['sim-64bit'] || sim['64bit'])) {
							candidates[ver] || (candidates[ver] = []);
							candidates[ver].push(sim);
						}
					});
				}
			});

			// sort the candidates by iOS version, but put the preferred iOS version first
			// then find the preferred simulator, if any
			var simVers = Object.keys(candidates).sort(function (a, b) { return a === version ? -1 : 1; }),
				first, firstRetina;

			for (var i = 0, l = simVers.length; i < l; i++) {
				var simVer = simVers[i];
				for (var j = 0, k = candidates[simVer].length; j < k; j++) {
					if (!first) {
						first = candidates[simVer][j];
					}
					if (!firstRetina && candidates[simVer][j].retina) {
						firstRetina = candidates[simVer][j];
					}
					if (candidates[simVer][j].tall) {
						deviceInfo.preferred = candidates[simVer][j];
						i = l;
						break;
					}
				}
			}

			if (!deviceInfo.preferred) {
				deviceInfo.preferred = firstRetina || first;
			}
		}

		// build the list of simulators
		Object.keys(this.iosInfo.simulators).sort().reverse().forEach(function (ver) {
			deviceInfo.devices[ver] || (deviceInfo.devices[ver] = []);
			this.iosInfo.simulators[ver].forEach(function (sim) {
				sim.name.length > deviceInfo.maxName && (deviceInfo.maxName = sim.name.length);
				deviceInfo.devices[ver].push({
					udid: sim.udid,
					name: sim.name,
					deviceClass: sim.type,
					productVersion: ver,
					retina: sim.retina,
					tall: sim.tall,
					'64bit': sim['64bit']
				});
				deviceInfo.udids[sim.udid] = sim;
			});
		}, this);
	}

	return this.deviceInfoCache = deviceInfo;
};

iOSBuilder.prototype.getDeviceFamily = function getDeviceFamily() {
	if (this.deviceFamily) {
		return deviceFamily;
	}

	var deviceFamily = this.cli.argv['device-family'],
		deploymentTargets = this.cli.tiapp && this.cli.tiapp['deployment-targets'];

	if (!deviceFamily && process.env.TARGETED_DEVICE_FAMILY) {
		// device family was not specified at the command line, but we did get it via an environment variable!
		deviceFamily = process.env.TARGETED_DEVICE_FAMILY === '1' ? 'iphone' : process.env.TARGETED_DEVICE_FAMILY === '2' ? 'ipad' : 'universal';
	}

	if (!deviceFamily && deploymentTargets) {
		// device family was not an environment variable, construct via the tiapp.xml's deployment targets
		if (deploymentTargets.iphone && deploymentTargets.ipad) {
			deviceFamily = this.cli.argv.$originalPlatform === 'ipad' ? 'ipad' : 'universal';
		} else if (deploymentTargets.iphone) {
			deviceFamily = 'iphone';
		} else if (deploymentTargets.ipad) {
			deviceFamily = 'ipad';
		}
	}

	return this.deviceFamily = deviceFamily;
};

/**
 * Returns iOS build-specific configuration options.
 *
 * @param {Object} logger - The logger instance
 * @param {Object} config - The CLI config
 * @param {Object} cli - The CLI instance
 *
 * @returns {Function|undefined} A function that returns the config info or undefined
 */
iOSBuilder.prototype.config = function config(logger, config, cli) {
	Builder.prototype.config.apply(this, arguments);

	var _t = this;

	// we hook into the pre-validate event so that we can stop the build before
	// prompting if we know the build is going to fail.
	cli.on('cli:pre-validate', function (obj, callback) {
		if (cli.argv.platform && !/^(ios|iphone|ipad)$/i.test(cli.argv.platform)) {
			return callback();
		}

		// check that the iOS environment is found and sane
		this.assertIssue(this.iosInfo.issues, 'IOS_XCODE_NOT_INSTALLED');
		this.assertIssue(this.iosInfo.issues, 'IOS_NO_SUPPORTED_XCODE_FOUND');
		this.assertIssue(this.iosInfo.issues, 'IOS_NO_IOS_SDKS');
		this.assertIssue(this.iosInfo.issues, 'IOS_NO_IOS_SIMS');

		callback();
	}.bind(this));

	return function (done) {
		ioslib.detect({
			// env
			xcodeSelect: config.get('osx.executables.xcodeSelect'),
			security: config.get('osx.executables.security'),
			// provisioning
			profileDir: config.get('ios.profileDir'),
			// xcode
			searchPath: config.get('paths.xcode'),
			minIosVersion: iosPackageJson.minIosVersion,
			supportedVersions: iosPackageJson.vendorDependencies.xcode
		}, function (err, iosInfo) {
			this.iosInfo = iosInfo;

			// add itunes sync
			iosInfo.devices.push({
				udid: 'itunes',
				name: 'iTunes Sync'
			});

			// we have more than 1 device plus itunes, so we should show 'all'
			if (iosInfo.devices.length > 2) {
				iosInfo.devices.push({
					udid: 'all',
					name: 'All Devices'
				});
			}

			// get the all installed iOS SDKs and Simulators across all Xcode versions
			var allSdkVersions = {},
				sdkVersions = {},
				simVersions = {};
			Object.keys(iosInfo.xcode).forEach(function (ver) {
				if (iosInfo.xcode[ver].supported) {
					iosInfo.xcode[ver].sdks.forEach(function (sdk) {
						allSdkVersions[sdk] = 1;
						if (version.gte(sdk, this.minSupportedIosSdk)) {
							sdkVersions[sdk] = 1;
						}
					}, this);
					iosInfo.xcode[ver].sims.forEach(function (sim) {
						simVersions[sim] = 1;
					});
				}
			}, this);
			this.iosAllSdkVersions = version.sort(Object.keys(allSdkVersions));
			this.iosSdkVersions = version.sort(Object.keys(sdkVersions));
			this.iosSimVersions = version.sort(Object.keys(simVersions));

			// if we're running from Xcode, determine the default --ios-version
			var defaultIosVersion = null;
			if (iosInfo.selectedXcode && iosInfo.selectedXcode.supported) {
				defaultIosVersion = iosInfo.selectedXcode.sdks.sort().reverse()[0];
			}
			// if we didn't have a selected xcode, then just take the latest sdk from the latest xcode
			if (!defaultIosVersion) {
				Object.keys(iosInfo.xcode).filter(function (ver) {
					return iosInfo.xcode[ver].supported;
				}).sort().reverse().forEach(function (ver) {
					if (!defaultIosVersion && iosInfo.xcode[ver].sdks.length) {
						defaultIosVersion = iosInfo.xcode[ver].sdks[0];
					}
				});
			}
			this.defaultIosVersion = defaultIosVersion;

			var sdkRoot = process.env.SDKROOT || process.env.SDK_DIR;
			if (sdkRoot) {
				var m = sdkRoot.match(/\/iphone(?:os|simulator)(\d.\d).sdk/i);
				if (m) {
					defaultIosVersion = m[1];
					var file = path.join(sdkRoot, 'System', 'Library', 'CoreServices', 'SystemVersion.plist');
					if (fs.existsSync(file)) {
						var p = new appc.plist(file);
						if (p.ProductVersion) {
							defaultIosVersion = this.defaultIosVersion = p.ProductVersion;
						}
					}
				}
			}

			cli.createHook('build.ios.config', function (callback) {
				callback(null, {
					flags: {
						'force-copy': {
							desc: __('forces files to be copied instead of symlinked for %s builds only', 'simulator'.cyan)
						},
						'force-copy-all': {
							desc: __('identical to the %s flag, except this will also copy the %s libTiCore.a file', '--force-copy',
								humanize.filesize(fs.statSync(path.join(_t.platformPath, 'libTiCore.a')).size, 1024, 1).toUpperCase().cyan)
						},
						'launch-watch-app': {
							desc: __('for %s builds, after installing an app with a watch extention, launch the watch app and the main app', 'simulator'.cyan)
						},
						'launch-watch-app-only': {
							desc: __('for %s builds, after installing an app with a watch extention, launch the watch app instead of the main app', 'simulator'.cyan)
						},
						'retina': {
							desc: __('use the retina version of the iOS Simulator')
						},
						'sim-64bit': {
							desc: __('use the 64-bit version of the iOS Simulator')
						},
						'sim-focus': {
							default: true,
							desc: __('focus the iOS Simulator')
						},
						'tall': {
							desc: __('in combination with %s flag, start the tall version of the retina device', '--retina'.cyan)
						},
						'xcode': {
							// DEPRECATED
							// secret flag to perform Xcode pre-compile build step
							callback: function (value) {
								if (value) {
									// we deprecated the --xcode flag which was passed in during the Xcode pre-compile phase
									logger.error(__('The generated Titanium Xcode project is too old.'));
									logger.error(__('Please clean and rebuild the project.'));
									process.exit(1);
								}
							},
							hidden: true
						}
					},
					options: {
						'build-type': {
							hidden: true
						},
						'debug-host': {
							hidden: true
						},
						'deploy-type':                this.configOptionDeployType(100),
						'device-id':                  this.configOptionDeviceID(210),
						'developer-name':             this.configOptionDeveloperName(170),
						'distribution-name':          this.configOptionDistributionName(180),
						'device-family':              this.configOptionDeviceFamily(120),
						'external-display-type':      this.configOptionExternalDisplayType(),
						'ios-version':                this.configOptioniOSVersion(130),
						'keychain':                   this.configOptionKeychain(),
						'launch-bundle-id':           this.configOptionLaunchBundleId(),
						'launch-url': {
							// url for the application to launch in mobile Safari, as soon as the app boots up
							hidden: true
						},
						'output-dir':                 this.configOptionOutputDir(200),
						'pp-uuid':                    this.configOptionPPuuid(190),
						'profiler-host': {
							hidden: true
						},
						'sim-type':                   this.configOptionSimType(150),
						'sim-version':                this.configOptionSimVersion(160),
						'target':                     this.configOptionTarget(110),
						'watch-launch-mode':          this.configOptionWatchLaunchMode(),
						'watch-notification-payload': this.configOptionWatchNotificationPayload()
					}
				});
			}.bind(this))(function (err, result) {
				done(_t.conf = result);
			});
		}.bind(this)); // end of ioslib.detect()
	}.bind(this);
};

/**
 * Defines the --deploy-type option.
 *
 * @param {Integer} order - The order to apply to this option.
 *
 * @returns {Object}
 */
iOSBuilder.prototype.configOptionDeployType = function configOptionDeployType(order) {
	return {
		abbr: 'D',
		desc: __('the type of deployment; only used when target is %s or %s', 'simulator'.cyan, 'device'.cyan),
		hint: __('type'),
		order: order,
		values: ['test', 'development']
	};
};

/**
 * Defines the --device-id option.
 *
 * @param {Integer} order - The order to apply to this option.
 *
 * @returns {Object}
 */
iOSBuilder.prototype.configOptionDeviceID = function configOptionDeviceID(order) {
	var _t = this,
		cli = this.cli;

	return {
		abbr: 'C',
		desc: __('the udid of the iOS simulator or iOS device to install the application to; for %s builds %s',
			'device'.cyan, ('[' + 'itunes'.bold + ', <udid>, all]').grey),
		hint: __('udid'),
		order: order,
		helpNoPrompt: function (logger, msg) {
			// if prompting is disabled and there's a problem, then help will use this function to display details
			logger.error(msg);
			var info = _t.getDeviceInfo();
			if (info.devices) {
				if (cli.argv.target === 'device') {
					logger.log('\n' + __('Available iOS Devices:'));
					info.devices.forEach(function (sim) {
						logger.log('  ' + (info.devices.length > 1 ? appc.string.rpad(sim.udid, 40) : sim.udid).cyan + '  ' + sim.name);
					});
					logger.log();
				} else {
					logger.log('\n' + __('Available iOS Simulators:'));
					Object.keys(info.devices).forEach(function (ver) {
						logger.log(String(ver).grey);
						info.devices[ver].forEach(function (sim) {
							logger.log('  ' + sim.udid.cyan + '  ' + sim.name);
						});
						logger.log();
					});
				}
			}
		},
		prompt: function (callback) {
			var info = _t.getDeviceInfo();
			if (info.preferred) {
				cli.argv['device-id'] = info.preferred.udid;
				return callback();
			}

			var options = {},
				maxName = 0,
				maxDesc = 0;

			// build a filtered list of simulators based on any legacy options/flags
			if (Array.isArray(info.devices)) {
				options = info.devices;
				info.devices.forEach(function (d) {
					if (d.name.length > maxName) {
						maxName = d.name.length;
					}
					var s = d.deviceClass ? (d.deviceClass + ' (' + d.productVersion + ')') : '';
					if (s.length > maxDesc) {
						maxDesc = s.length;
					}
				});
			} else {
				Object.keys(info.devices).forEach(function (sdk) {
					if (!cli.argv['sim-version'] || sdk === cli.argv['sim-version']) {
						info.devices[sdk].forEach(function (sim) {
							if ((!cli.argv['sim-type'] || sim.deviceClass === cli.argv['sim-type']) && (!cli.argv.retina || sim.retina) && (!cli.argv.tall || sim.tall) && (!cli.argv['sim-64bit'] || sim['64bit'])) {
								options[sdk] || (options[sdk] = []);
								options[sdk].push(sim);
								if (sim.name.length > maxName) {
									maxName = sim.name.length;
								}
							}
						});
					}
				});
			}

			var params = {
				formatters: {},
				default: '1', // just default to the first one, whatever that will be
				autoSelectOne: true,
				margin: '',
				optionLabel: 'name',
				optionValue: 'udid',
				numbered: true,
				relistOnError: true,
				complete: true,
				suggest: true,
				options: options
			};

			if (cli.argv.target === 'device') {
				// device specific settings
				params.title = __('Which device do you want to install your app on?');
				params.promptLabel = __('Select an device by number or name');
				params.formatters.option = function (opt, idx, num) {
					return '  ' + num + [
						appc.string.rpad(opt.name, info.maxName).cyan,
						appc.string.rpad(opt.deviceClass ? opt.deviceClass + ' (' + opt.productVersion + ')' : '', maxDesc),
						opt.udid.grey
					].join('  ');
				};
			} else if (cli.argv.target === 'simulator') {
				// simulator specific settings
				params.title = __('Which simulator do you want to launch your app in?');
				params.promptLabel = __('Select an simulator by number or name');
				params.formatters.option = function (opt, idx, num) {
					return '  ' + num + appc.string.rpad(opt.name, maxName).cyan + '  ' + opt.udid.grey;
				};
			}

			callback(fields.select(params));
		},
		required: true,
		validate: function (udid, callback) {
			// this function is called if they specify a --device-id and we need to check that it is valid
			if (typeof udid === 'boolean') {
				return callback(true);
			}

			if (cli.argv.target === 'device' && udid === 'all') {
				// we let 'all' slide by
				return callback(null, udid);
			}

			var info = _t.getDeviceInfo();
			if (info.udids[udid]) {
				callback(null, udid)
			} else {
				callback(new Error(cli.argv.target === 'device' ? __('Invalid iOS device "%s"', udid) : __('Invalid iOS simulator "%s"', udid)));
			}
		},
		verifyIfRequired: function (callback) {
			// this function is called by the CLI when the option is not specified and is required (i.e. missing).
			// the CLI will then double check that this option is still required by calling this function
			if (cli.argv['build-only']) {
				// not required if we're build only
				return callback();
			} else if (cli.argv['device-id'] === undefined && _t.config.get('ios.autoSelectDevice', true)) {
				// --device-id not specified and we're not prompting, so pick a device

				if (cli.argv.target === 'device') {
					cli.argv['device-id'] = _t.iosInfo.devices.length ? _t.iosInfo.devices[0].udid : 'itunes';
					return callback();
				}

				if (cli.argv.target !== 'simulator') {
					return callback(true);
				}

				var info = _t.getDeviceInfo();

				if (info.preferred) {
					// we have a preferred sim based on the legacy cli args and environment
					cli.argv['device-id'] = info.preferred.udid;
					return callback();
				}

				var simVer = cli.argv['sim-version'] || cli.argv['ios-version'],
					simVers = Object.keys(info.devices).filter(function (ver) {
						return !simVer || ver === simVer;
					}),
					deviceFamily = _t.getDeviceFamily(),
					first, firstRetina;

				// try to find us a tall simulator like an iPhone 4 inch
				for (var i = 0, l = simVers.length; i < l; i++) {
					var ver = simVers[i];
					for (var j = 0, k = info.devices[ver].length; j < k; j++) {
						var sim = info.devices[ver][j];
						if (deviceFamily === 'ipad' && sim.deviceClass !== deviceFamily) {
							continue;
						}
						if (!first) {
							// just in case we don't find a tall or retina sim, then we'll just use this sim
							first = sim.udid;
						}
						if (!firstRetina && sim.retina) {
							// just in case we don't find a tall sim, then we'll just use this retina sim
							firstRetina = sim.udid;
						}
						if (sim.type === 'iphone' && sim.tall) {
							// this is the one we really are hoping to find
							cli.argv['device-id'] = sim.udid;
							return callback();
						}
					}
				}

				cli.argv['device-id'] = firstRetina || first;
				return callback();
			}

			// yup, still required
			callback(true);
		}
	};
};

/**
 * Defines the --developer-name option.
 *
 * @param {Integer} order - The order to apply to this option.
 *
 * @returns {Object}
 */
iOSBuilder.prototype.configOptionDeveloperName = function configOptionDeveloperName(order) {
	var cli = this.cli,
		iosInfo = this.iosInfo,
		developerCertLookup = {};

	Object.keys(iosInfo.certs.keychains).forEach(function (keychain) {
		(iosInfo.certs.keychains[keychain].developer || []).forEach(function (d) {
			if (!d.invalid) {
				developerCertLookup[d.name.toLowerCase()] = d.name;
			}
		});
	});

	return {
		abbr: 'V',
		default: process.env.CODE_SIGN_IDENTITY && process.env.CODE_SIGN_IDENTITY.replace(/^iPhone Developer(?:\: )?/, '') || this.config.get('ios.developerName'),
		desc: __('the iOS Developer Certificate to use; required when target is %s', 'device'.cyan),
		hint: 'name',
		order: order,
		prompt: function (callback) {
			var developerCerts = {},
				maxDevCertLen = 0;

			Object.keys(iosInfo.certs.keychains).forEach(function (keychain) {
				(iosInfo.certs.keychains[keychain].developer || []).forEach(function (d) {
					if (!d.invalid) {
						Array.isArray(developerCerts[keychain]) || (developerCerts[keychain] = []);
						developerCerts[keychain].push(d);
						maxDevCertLen = Math.max(d.name.length, maxDevCertLen);
					}
				});
			});

			// sort the certs
			Object.keys(developerCerts).forEach(function (keychain) {
				developerCerts[keychain] = developerCerts[keychain].sort(function (a, b) {
					return a.name === b.name ? 0 : a.name < b.name ? -1 : 1;
				});
			});

			callback(fields.select({
				title: __("Which developer certificate would you like to use?"),
				promptLabel: __('Select a certificate by number or name'),
				formatters: {
					option: function (opt, idx, num) {
						var expires = moment(opt.after),
							day = expires.format('D'),
							hour = expires.format('h');
						return '  ' + num + appc.string.rpad(opt.name, maxDevCertLen + 1).cyan
							+ (opt.after ? (' (' + __('expires %s', expires.format('MMM') + ' '
							+ (day.length === 1 ? ' ' : '') + day + ', ' + expires.format('YYYY') + ' '
							+ (hour.length === 1 ? ' ' : '') + hour + ':' + expires.format('mm:ss a'))
							+ ')').grey : '');
					}
				},
				margin: '',
				optionLabel: 'name',
				optionValue: 'name',
				numbered: true,
				relistOnError: true,
				complete: true,
				suggest: false,
				options: developerCerts
			}));
		},
		validate: function (value, callback) {
			if (typeof value === 'boolean') {
				return callback(true);
			}
			if (cli.argv.target !== 'device') {
				return callback(null, value);
			}
			if (value) {
				var v = developerCertLookup[value.toLowerCase()];
				if (v) {
					return callback(null, v);
				}
			}
			callback(new Error(__('Invalid developer certificate "%s"', value)));
		}
	};
};

/**
 * Defines the --distribution-name option.
 *
 * @param {Integer} order - The order to apply to this option.
 *
 * @returns {Object}
 */
iOSBuilder.prototype.configOptionDistributionName = function configOptionDistributionName(order) {
	var cli = this.cli,
		iosInfo = this.iosInfo,
		distributionCertLookup = {};

	Object.keys(iosInfo.certs.keychains).forEach(function (keychain) {
		(iosInfo.certs.keychains[keychain].distribution || []).forEach(function (d) {
			if (!d.invalid) {
				distributionCertLookup[d.name.toLowerCase()] = d.name;
			}
		});
	});

	return {
		abbr: 'R',
		default: process.env.CODE_SIGN_IDENTITY && process.env.CODE_SIGN_IDENTITY.replace(/^iPhone Distribution(?:\: )?/, '') || this.config.get('ios.distributionName'),
		desc: __('the iOS Distribution Certificate to use; required when target is %s or %s', 'dist-appstore'.cyan, 'dist-adhoc'.cyan),
		hint: 'name',
		order: order,
		prompt: function (callback) {
			var distributionCerts = {},
				maxDistCertLen = 0;

			Object.keys(iosInfo.certs.keychains).forEach(function (keychain) {
				(iosInfo.certs.keychains[keychain].distribution || []).forEach(function (d) {
					if (!d.invalid) {
						Array.isArray(distributionCerts[keychain]) || (distributionCerts[keychain] = []);
						distributionCerts[keychain].push(d);
						maxDistCertLen = Math.max(d.name.length, maxDistCertLen);
					}
				});
			});

			// sort the certs
			Object.keys(distributionCerts).forEach(function (keychain) {
				distributionCerts[keychain] = distributionCerts[keychain].sort(function (a, b) {
					return a.name === b.name ? 0 : a.name < b.name ? -1 : 1;
				});
			});

			callback(fields.select({
				title: __("Which distribution certificate would you like to use?"),
				promptLabel: __('Select a certificate by number or name'),
				formatters: {
					option: function (opt, idx, num) {
						var expires = moment(opt.after),
							day = expires.format('D'),
							hour = expires.format('h');
						return '  ' + num + appc.string.rpad(opt.name, maxDistCertLen + 1).cyan
							+ (opt.after ? (' (' + __('expires %s', expires.format('MMM') + ' '
							+ (day.length === 1 ? ' ' : '') + day + ', ' + expires.format('YYYY') + ' '
							+ (hour.length === 1 ? ' ' : '') + hour + ':' + expires.format('mm:ss a'))
							+ ')').grey : '');
					}
				},
				margin: '',
				optionLabel: 'name',
				optionValue: 'name',
				numbered: true,
				relistOnError: true,
				complete: true,
				suggest: false,
				options: distributionCerts
			}));
		},
		validate: function (value, callback) {
			if (typeof value === 'boolean') {
				return callback(true);
			}
			if (cli.argv.target !== 'dist-appstore' && cli.argv.target !== 'dist-adhoc') {
				return callback(null, value);
			}
			if (value) {
				var v = distributionCertLookup[value.toLowerCase()];
				if (v) {
					return callback(null, v);
				}
			}
			callback(new Error(__('Invalid distribution certificate "%s"', value)));
		}
	};
};

/**
 * Defines the --device-family option.
 *
 * @param {Integer} order - The order to apply to this option.
 *
 * @returns {Object}
 */
iOSBuilder.prototype.configOptionDeviceFamily = function configOptionDeviceFamily(order) {
	return {
		abbr: 'F',
		desc: __('the device family to build for'),
		order: order,
		values: Object.keys(this.deviceFamilies)
	};
};

/**
 * Defines the --external-display-type option.
 *
 * @returns {Object}
 */
iOSBuilder.prototype.configOptionExternalDisplayType = function configOptionExternalDisplayType() {
	return {
		desc: __('shows the simulator external display; only used when target is %s', 'simulator'.cyan),
		hint: __('type'),
		values: ['watch-regular', 'watch-compact']
	};
};

/**
 * Defines the --ios-version option.
 *
 * @param {Integer} order - The order to apply to this option.
 *
 * @returns {Object}
 */
iOSBuilder.prototype.configOptioniOSVersion = function configOptioniOSVersion(order) {
	var _t = this;

	return {
		abbr: 'I',
		callback: function (value) {
			try {
				if (value && _t.iosAllSdkVersions.indexOf(value) !== -1 && version.lt(value, _t.minSupportedIosSdk)) {
					logger.banner();
					logger.error(__('The specified iOS SDK version "%s" is not supported by Titanium %s', value, _t.titaniumSdkVersion) + '\n');
					if (_t.iosSdkVersions.length) {
						logger.log(__('Available supported iOS SDKs:'));
						_t.iosSdkVersions.forEach(function (ver) {
							logger.log('   ' + ver.cyan);
						});
						logger.log();
					}
					process.exit(1);
				}
			} catch (e) {
				// squelch and let the cli detect the bad version
			}
		},
		desc: __('iOS SDK version to build with'),
		order: order,
		prompt: function (callback) {
			callback(fields.select({
				title: __("Which iOS SDK version would you like to build with?"),
				promptLabel: __('Select an iOS SDK version by number or name'),
				margin: '',
				numbered: true,
				relistOnError: true,
				complete: true,
				suggest: false,
				options: _t.iosSdkVersions
			}));
		},
		values: _t.iosSdkVersions
	};
};

/**
 * Defines the --keychain option.
 *
 * @returns {Object}
 */
iOSBuilder.prototype.configOptionKeychain = function configOptionKeychain() {
	return {
		abbr: 'K',
		desc: __('path to the distribution keychain to use instead of the system default; only used when target is %s, %s, or %s', 'device'.cyan, 'dist-appstore'.cyan, 'dist-adhoc'.cyan),
		hideValues: true,
		validate: function (value, callback) {
			value && typeof value !== 'string' && (value = null);
			if (value && !fs.existsSync(value)) {
				callback(new Error(__('Unable to find keychain: %s', value)));
			} else {
				callback(null, value);
			}
		}
	};
};

/**
 * Defines the --launch-bundle-id option.
 *
 * @returns {Object}
 */
iOSBuilder.prototype.configOptionLaunchBundleId = function configOptionLaunchBundleId() {
	return {
		desc: __('after installing the app, launch an different app instead; only used when target is %s', 'simulator'.cyan),
		hint: __('id')
	};
};

/**
 * Defines the --output-dir option.
 *
 * @param {Integer} order - The order to apply to this option.
 *
 * @returns {Object}
 */
iOSBuilder.prototype.configOptionOutputDir = function configOptionOutputDir(order) {
	var _t = this,
		cli = this.cli;

	function validate(outputDir, callback) {
		callback(outputDir || !_t.conf.options['output-dir'].required ? null : new Error(__('Invalid output directory')), outputDir);
	}

	return {
		abbr: 'O',
		desc: __('the output directory when using %s', 'dist-adhoc'.cyan),
		hint: 'dir',
		order: order,
		prompt: function (callback) {
			callback(fields.file({
				promptLabel: __('Where would you like the output IPA file saved?'),
				default: cli.argv['project-dir'] && appc.fs.resolvePath(cli.argv['project-dir'], 'dist'),
				complete: true,
				showHidden: true,
				ignoreDirs: _t.ignoreDirs,
				ignoreFiles: /.*/,
				validate: validate
			}));
		},
		validate: validate
	};
};

/**
 * Defines the --pp-uuid option.
 *
 * @param {Integer} order - The order to apply to this option.
 *
 * @returns {Object}
 */
iOSBuilder.prototype.configOptionPPuuid = function configOptionPPuuid(order) {
	var _t = this,
		cli = this.cli,
		iosInfo = this.iosInfo;

	return {
		abbr: 'P',
		default: process.env.PROVISIONING_PROFILE,
		desc: __('the provisioning profile uuid; required when target is %s, %s, or %s', 'device'.cyan, 'dist-appstore'.cyan, 'dist-adhoc'.cyan),
		hint: 'uuid',
		order: order,
		prompt: function (callback) {
			var provisioningProfiles = {},
				appId = cli.tiapp.id,
				maxAppId = 0,
				pp;

			function prep(a) {
				return a.filter(function (p) {
					if (!p.expired) {
						var re = new RegExp(p.appId.replace(/\./g, '\\.').replace(/\*/g, '.*'));
						if (re.test(appId)) {
							var label = p.name;
							if (label.indexOf(p.appId) === -1) {
								label += ': ' + p.appId;
							}
							p.label = label;
							maxAppId = Math.max(p.label.length, maxAppId);
							return true;
						}
					}
				}).sort(function (a, b) {
					return a.label.toLowerCase().localeCompare(b.label.toLowerCase());
				});
			}

			if (cli.argv.target === 'device') {
				if (iosInfo.provisioning.development.length) {
					pp = prep(iosInfo.provisioning.development);
					if (pp.length) {
						provisioningProfiles[__('Available Development UUIDs:')] = pp;
					} else {
						logger.error(__('Unable to find any non-expired development provisioning profiles that match the app id "%s"', appId) + '\n');
						logger.log(__('You will need to login into %s with your Apple Download account, then create, download, and install a profile.',
							'http://appcelerator.com/ios-dev-certs'.cyan) + '\n');
						process.exit(1);
					}
				} else {
					logger.error(__('Unable to find any development provisioning profiles') + '\n');
					logger.log(__('You will need to login into %s with your Apple Download account, then create, download, and install a profile.',
						'http://appcelerator.com/ios-dev-certs'.cyan) + '\n');
					process.exit(1);
				}
			} else if (cli.argv.target === 'dist-appstore' || cli.argv.target === 'dist-adhoc') {
				if (iosInfo.provisioning.distribution.length || iosInfo.provisioning.adhoc.length) {
					pp = prep(iosInfo.provisioning.distribution);
					var valid = pp.length;
					if (pp.length) {
						provisioningProfiles[__('Available Distribution UUIDs:')] = pp;
					}

					pp = prep(iosInfo.provisioning.adhoc);
					valid += pp.length;
					if (pp.length) {
						provisioningProfiles[__('Available Adhoc UUIDs:')] = pp;
					}

					if (!valid) {
						logger.error(__('Unable to find any non-expired distribution or adhoc provisioning profiles that match the app id "%s".', appId) + '\n');
						logger.log(__('You will need to login into %s with your Apple Download account, then create, download, and install a profile.',
							'http://appcelerator.com/ios-dist-certs'.cyan) + '\n');
						process.exit(1);
					}
				} else {
					logger.error(__('Unable to find any distribution or adhoc provisioning profiles'));
					logger.log(__('You will need to login into %s with your Apple Download account, then create, download, and install a profile.',
						'http://appcelerator.com/ios-dist-certs'.cyan) + '\n');
					process.exit(1);
				}
			}

			callback(fields.select({
				title: __("Which provisioning profile would you like to use?"),
				promptLabel: __('Select a provisioning profile UUID by number or name'),
				formatters: {
					option: function (opt, idx, num) {
						var expires = moment(opt.expirationDate),
							day = expires.format('D'),
							hour = expires.format('h');
						return '  ' + num + String(opt.uuid).cyan + ' '
							+ appc.string.rpad(opt.label, maxAppId + 1)
							+ (opt.expirationDate ? (' (' + __('expires %s', expires.format('MMM') + ' '
							+ (day.length === 1 ? ' ' : '') + day + ', ' + expires.format('YYYY') + ' '
							+ (hour.length === 1 ? ' ' : '') + hour + ':' + expires.format('mm:ss a'))
							+ ')').grey : '');
					}
				},
				margin: '',
				optionLabel: 'name',
				optionValue: 'uuid',
				numbered: true,
				relistOnError: true,
				complete: true,
				suggest: false,
				options: provisioningProfiles
			}));
		},
		validate: function (value, callback) {
			if (cli.argv.target === 'simulator') {
				return callback(null, value);
			}
			if (value) {
				var v = _t.provisioningProfileLookup[value.toLowerCase()];
				if (v) {
					return callback(null, v);
				}
				return callback(new Error(__('Invalid provisioning profile UUID "%s"', value)));
			}
			callback(true);
		}
	};
};

/**
 * Defines the --sim-type option.
 *
 * @param {Integer} order - The order to apply to this option.
 *
 * @returns {Object}
 */
iOSBuilder.prototype.configOptionSimType = function configOptionSimType(order) {
	return {
		abbr: 'Y',
		desc: __('iOS Simulator type; only used when target is %s', 'simulator'.cyan),
		hint: 'type',
		order: order,
		values: Object.keys(this.simTypes)
	};
};

/**
 * Defines the --sim-version option.
 *
 * @param {Integer} order - The order to apply to this option.
 *
 * @returns {Object}
 */
iOSBuilder.prototype.configOptionSimVersion = function configOptionSimVersion(order) {
	return {
		abbr: 'S',
		desc: __('iOS Simulator version; only used when target is %s', 'simulator'.cyan),
		hint: 'version',
		order: order,
		values: this.iosSimVersions
	};
};

/**
 * Defines the --target option.
 *
 * @param {Integer} order - The order to apply to this option.
 *
 * @returns {Object}
 */
iOSBuilder.prototype.configOptionTarget = function configOptionTarget(order) {
	var _t = this,
		cli = this.cli,
		iosInfo = this.iosInfo;

	return {
		abbr: 'T',
		callback: function (value) {
			if (value !== 'simulator') {
				_t.assertIssue(iosInfo.issues, 'IOS_NO_KEYCHAINS_FOUND');
				_t.assertIssue(iosInfo.issues, 'IOS_NO_WWDR_CERT_FOUND');
			}

			// as soon as we know the target, toggle required options for validation
			switch (value) {
				case 'device':
					_t.assertIssue(iosInfo.issues, 'IOS_NO_VALID_DEV_CERTS_FOUND');
					_t.assertIssue(iosInfo.issues, 'IOS_NO_VALID_DEVELOPMENT_PROVISIONING_PROFILES');
					iosInfo.provisioning.development.forEach(function (d) {
						_t.provisioningProfileLookup[d.uuid.toLowerCase()] = d.uuid;
					});
					_t.conf.options['developer-name'].required = true;
					_t.conf.options['pp-uuid'].required = true;
					break;

				case 'dist-adhoc':
					_t.assertIssue(iosInfo.issues, 'IOS_NO_VALID_DIST_CERTS_FOUND');
					// TODO: assert there is at least one distribution or adhoc provisioning profile

					_t.conf.options['output-dir'].required = true;

					// purposely fall through!

				case 'dist-appstore':
					_t.assertIssue(iosInfo.issues, 'IOS_NO_VALID_DIST_CERTS_FOUND');

					_t.conf.options['deploy-type'].values = ['production'];
					_t.conf.options['device-id'].required = false;
					_t.conf.options['distribution-name'].required = true;
					_t.conf.options['pp-uuid'].required = true;

					// build lookup maps
					iosInfo.provisioning.distribution.forEach(function (d) {
						_t.provisioningProfileLookup[d.uuid.toLowerCase()] = d.uuid;
					});
					iosInfo.provisioning.adhoc.forEach(function (d) {
						_t.provisioningProfileLookup[d.uuid.toLowerCase()] = d.uuid;
					});
			}
		},
		default: process.env.CURRENT_ARCH && process.env.CURRENT_ARCH !== 'i386' ? 'device' : 'simulator',
		desc: __('the target to build for'),
		order: 110,
		required: true,
		values: this.targets
	};
};

/**
 * Defines the --watch-launch-mode option.
 *
 * @returns {Object}
 */
iOSBuilder.prototype.configOptionWatchLaunchMode = function configOptionWatchLaunchMode() {
	return {
		desc: __('iOS Simulator type; only used when target is %s', 'simulator'.cyan),
		hint: 'mode',
		values: ['default', 'glance', 'notification']
	};
};

/**
 * Defines the --watch-notification-payload option.
 *
 * @returns {Object}
 */
iOSBuilder.prototype.configOptionWatchNotificationPayload = function configOptionWatchNotificationPayload() {
	return {
		desc: __('iOS Simulator version; only used when target is %s', 'simulator'.cyan),
		hint: 'file',
		validate: function (value, callback) {
			if (value && !fs.existsSync(value)) {
				return callback(new Error(__('Watch notification payload file "%s" does not exist', value)));
			}
			callback(value);
		}
	};
};

/**
 * Validates the iOS build-specific arguments, tiapp.xml settings, and environment.
 *
 * @param {Object} logger - The logger instance.
 * @param {Object} config - The Titanium CLI config instance.
 * @param {Object} cli - The Titanium CLI instance.
 *
 * @returns {Function} A function to be called async which returns the actual configuration.
 */
iOSBuilder.prototype.validate = function (logger, config, cli) {
	Builder.prototype.validate.apply(this, arguments);

	this.target = cli.argv.target;
	this.deployType = !/^dist-/.test(this.target) && cli.argv['deploy-type'] ? cli.argv['deploy-type'] : this.deployTypes[this.target];
	this.buildType = cli.argv['build-type'] || '';

	// manually inject the build profile settings into the tiapp.xml
	switch (this.deployType) {
		case 'production':
			this.minifyJS = true;
			this.encryptJS = true;
			this.allowDebugging = false;
			this.allowProfiling = false;
			this.includeAllTiModules = false;
			this.compileI18N = true;
			this.compileJSS = true;
			break;

		case 'test':
			this.minifyJS = true;
			this.encryptJS = true;
			this.allowDebugging = true;
			this.allowProfiling = true;
			this.includeAllTiModules = false;
			this.compileI18N = true;
			this.compileJSS = true;
			break;

		case 'development':
		default:
			this.minifyJS = false;
			this.encryptJS = false;
			this.allowDebugging = true;
			this.allowProfiling = true;
			this.includeAllTiModules = true;
			this.compileI18N = false;
			this.compileJSS = false;
	}

	if (cli.argv['skip-js-minify']) {
		this.minifyJS = false;
	}

	// at this point we've validated everything except underscores in the app id
	if (!config.get('app.skipAppIdValidation') && !cli.tiapp.properties['ti.skipAppIdValidation']) {
		if (!/^([a-zA-Z_]{1}[a-zA-Z0-9_-]*(\.[a-zA-Z0-9_-]*)*)$/.test(cli.tiapp.id)) {
			logger.error(__('tiapp.xml contains an invalid app id "%s"', cli.tiapp.id));
			logger.error(__('The app id must consist only of letters, numbers, dashes, and underscores.'));
			logger.error(__('Note: iOS does not allow underscores.'));
			logger.error(__('The first character must be a letter or underscore.'));
			logger.error(__("Usually the app id is your company's reversed Internet domain name. (i.e. com.example.myapp)") + '\n');
			process.exit(1);
		}

		if (cli.tiapp.id.indexOf('_') !== -1) {
			logger.error(__('tiapp.xml contains an invalid app id "%s"', cli.tiapp.id));
			logger.error(__('The app id must consist of letters, numbers, and dashes.'));
			logger.error(__('The first character must be a letter.'));
			logger.error(__("Usually the app id is your company's reversed Internet domain name. (i.e. com.example.myapp)") + '\n');
			process.exit(1);
		}
	}

	if (!cli.argv['ios-version']) {
		if (this.iosSdkVersions.length) {
			// set the latest version
			cli.argv['ios-version'] = this.defaultIosVersion;
		} else {
			// this should not be possible, but you never know
			logger.error(cli.argv['ios-version'] ? __('Unable to find iOS SDK %s', cli.argv['ios-version']) + '\n' : __('Missing iOS SDK') + '\n');
			logger.log(__('Available iOS SDK versions:'));
			this.iosSdkVersions.forEach(function (ver) {
				logger.log('    ' + ver.cyan);
			});
			logger.log();
			process.exit(1);
		}
	}
	this.iosSdkVersion = cli.argv['ios-version'];

	// figure out the min-ios-ver that this app is going to support
	var defaultMinIosSdk = this.packageJson.minIosVersion;
	this.minIosVer = cli.tiapp.ios && cli.tiapp.ios['min-ios-ver'] || defaultMinIosSdk;
	this.minIosVerMessage = null; // we store the message below in this variable so that we can output info stuff after validation
	if (version.gte(this.iosSdkVersion, '6.0') && version.lt(this.minIosVer, defaultMinIosSdk)) {
		this.minIosVerMessage = __('Building for iOS %s; using %s as minimum iOS version', version.format(this.iosSdkVersion, 2).cyan, defaultMinIosSdk.cyan);
		this.minIosVer = defaultMinIosSdk;
	} else if (version.lt(this.minIosVer, defaultMinIosSdk)) {
		this.minIosVerMessage = __('The %s of the iOS section in the tiapp.xml is lower than minimum supported version: Using %s as minimum', 'min-ios-ver'.cyan, version.format(defaultMinIosSdk, 2).cyan);
		this.minIosVer = defaultMinIosSdk;
	} else if (version.gt(this.minIosVer, this.iosSdkVersion)) {
		this.minIosVerMessage = __('The %s of the iOS section in the tiapp.xml is greater than the specified %s: Using %s as minimum', 'min-ios-ver'.cyan, 'ios-version'.cyan, version.format(this.iosSdkVersion, 2).cyan);
		this.minIosVer = this.iosSdkVersion;
	}

	// check the min-ios-ver for the device we're installing to
	if (this.target === 'device') {
		this.getDeviceInfo().devices.forEach(function (device) {
			if (device.udid !== 'all' && device.udid !== 'itunes' && (cli.argv['device-id'] === 'all' || cli.argv['device-id'] === device.udid) && version.lt(device.productVersion, this.minIosVer)) {
				logger.error(__('This app does not support the device "%s"', device.name) + '\n');
				logger.log(__("The device is running iOS %s, however the app's the minimum iOS version is set to %s", device.productVersion.cyan, version.format(this.minIosVer, 2, 3).cyan));
				logger.log(__('In order to install this app on this device, lower the %s to %s in the tiapp.xml:', '<min-ios-ver>'.cyan, version.format(device.productVersion, 2, 2).cyan));
				logger.log();
				logger.log('<ti:app xmlns:ti="http://ti.appcelerator.org">'.grey);
				logger.log('    <ios>'.grey);
				logger.log(('        <min-ios-ver>' + version.format(device.productVersion, 2, 2) + '</min-ios-ver>').magenta);
				logger.log('    </ios>'.grey);
				logger.log('</ti:app>'.grey);
				logger.log();
				process.exit(0);
			}
		}, this);
	}

	// make sure the app doesn't have any blacklisted directories in the Resources directory and warn about graylisted names
	var resourcesDir = path.join(cli.argv['project-dir'], 'Resources');
	if (fs.existsSync(resourcesDir)) {
		fs.readdirSync(resourcesDir).forEach(function (filename) {
			var lcaseFilename = filename.toLowerCase(),
				isDir = fs.statSync(path.join(resourcesDir, filename)).isDirectory();

			if (this.blacklistDirectories.indexOf(lcaseFilename) !== -1) {
				if (isDir) {
					logger.error(__('Found blacklisted directory in the Resources directory') + '\n');
					logger.error(__('The directory "%s" is a reserved word.', filename));
					logger.error(__('You must rename this directory to something else.') + '\n');
				} else {
					logger.error(__('Found blacklisted file in the Resources directory') + '\n');
					logger.error(__('The file "%s" is a reserved word.', filename));
					logger.error(__('You must rename this file to something else.') + '\n');
				}
				process.exit(1);
			} else if (this.graylistDirectories.indexOf(lcaseFilename) !== -1) {
				if (isDir) {
					logger.warn(__('Found graylisted directory in the Resources directory'));
					logger.warn(__('The directory "%s" is potentially a reserved word.', filename));
					logger.warn(__('There is a good chance your app will be rejected by Apple.'));
					logger.warn(__('It is highly recommended you rename this directory to something else.'));
				} else {
					logger.warn(__('Found graylisted file in the Resources directory'));
					logger.warn(__('The file "%s" is potentially a reserved word.', filename));
					logger.warn(__('There is a good chance your app will be rejected by Apple.'));
					logger.warn(__('It is highly recommended you rename this file to something else.'));
				}
			}
		}, this);
	}

	// we have an ios sdk version, find the best xcode version to use
	this.xcodeEnv = null;
	Object.keys(this.iosInfo.xcode).forEach(function (ver) {
		if (this.iosInfo.xcode[ver].supported && (!this.xcodeEnv || this.iosInfo.xcode[ver].selected) && this.iosInfo.xcode[ver].sdks.some(function (sdk) { return version.eq(sdk, cli.argv['ios-version']); }, this)) {
			this.xcodeEnv = this.iosInfo.xcode[ver];
		}
	}, this);
	if (!this.xcodeEnv) {
		// this should never happen
		logger.error(__('Unable to find suitable Xcode install that supports iOS SDK %s', cli.argv['ios-version']) + '\n');
		process.exit(1);
	}

	// if in the prepare phase and doing a device/dist build...
	if (cli.argv.target !== 'simulator') {
		// make sure they have Apple's WWDR cert installed
		if (!this.iosInfo.certs.wwdr) {
			logger.error(__('WWDR Intermediate Certificate not found') + '\n');
			logger.log(__('Download and install the certificate from %s', 'http://appcelerator.com/ios-wwdr'.cyan) + '\n');
			process.exit(1);
		}

		// validate keychain
		var keychain = cli.argv.keychain ? appc.fs.resolvePath(cli.argv.keychain) : null;
		if (keychain && !fs.existsSync(keychain)) {
			logger.error(__('Unable to find keychain "%s"', keychain) + '\n');
			logger.log(__('Available keychains:'));
			Object.keys(this.iosInfo.certs.keychains).forEach(function (kc) {
				logger.log('    ' + kc.cyan);
			});
			logger.log();
			appc.string.suggest(keychain, Object.keys(this.iosInfo.certs.keychains), logger.log);
			process.exit(1);
		}
	}

	var deviceFamily = this.getDeviceFamily();
	if (!deviceFamily) {
		logger.info(__('No device family specified, defaulting to %s', 'universal'));
		deviceFamily = this.deviceFamily = 'universal';
	}

	if (!this.deviceFamilies[deviceFamily]) {
		logger.error(__('Invalid device family "%s"', deviceFamily) + '\n');
		appc.string.suggest(deviceFamily, Object.keys(this.deviceFamilies), logger.log, 3);
		process.exit(1);
	}

	// device family may have been modified, so set it back in the args
	cli.argv['device-family'] = deviceFamily;

	// check that the sim version exists
	if (cli.argv.target === 'simulator' && this.xcodeEnv.sims.indexOf(cli.argv['sim-version']) === -1) {
		// the preferred Xcode install we selected doesn't have this simulator, search the all again
		this.xcodeEnv = null;
		var selectedSim = this.getDeviceInfo().udids[cli.argv['device-id']];

		// check if we have a selected simulator; we won't if running with --build-only
		if (selectedSim) {
			// check the device family
			if (deviceFamily === 'ipad' && deviceFamily !== selectedSim.type) {
				logger.error(__('Unable to build an %s app for an %s simulator', this.simTypes[deviceFamily] || deviceFamily, this.simTypes[selectedSim.type]) + '\n');
				logger.log(__('Please specify "%s" to launch a compatible iOS Simulator.', ('--sim-type ' + deviceFamily).cyan));
				logger.log();
				process.exit(1);
			}
		}

		Object.keys(this.iosInfo.xcode).forEach(function (ver) {
			if (this.iosInfo.xcode[ver].supported
				&& !this.xcodeEnv
				&& this.iosInfo.xcode[ver].sdks.some(function (sdk) { return version.eq(sdk, cli.argv['ios-version']); })
				&& (!selectedSim || this.iosInfo.xcode[ver].sims.some(function (sim) { return version.eq(sim, selectedSim.ios); }))
			) {
				this.xcodeEnv = this.iosInfo.xcode[ver];
			}
		}, this);

		if (!this.xcodeEnv) {
			// this should never happen
			if (selectedSim) {
				logger.error(__('Unable to find any Xcode installs that have iOS SDK %s and iOS Simulator %s', cli.argv['ios-version'], selectedSim.ios) + '\n');
			} else {
				logger.error(__('Unable to find any Xcode installs that have iOS SDK %s', cli.argv['ios-version']) + '\n');
			}
			logger.log(__('Available iOS SDKs and iOS Simulators:'));
			Object.keys(this.iosInfo.xcode).forEach(function (ver) {
				if (this.iosInfo.xcode[ver].supported) {
					this.iosInfo.xcode[ver].sdks.forEach(function (sdk) {
						logger.log('\n  ' + __('iOS %s:', sdk));
						this.iosInfo.xcode[ver].sims.forEach(function (sim) {
							logger.log('    ' + ('--ios-version ' + sdk + ' --sim-version ' + sim).cyan);
						});
					}, this);
				}
			}, this);
			logger.log();
			process.exit(1);
		}
	}

	if (cli.argv.target !== 'dist-appstore') {
		var tool = [];
		this.allowDebugging && tool.push('debug');
		this.allowProfiling && tool.push('profiler');
		tool.forEach(function (type) {
			if (cli.argv[type + '-host']) {
				if (typeof cli.argv[type + '-host'] === 'number') {
					logger.error(__('Invalid %s host "%s"', type, cli.argv[type + '-host']) + '\n');
					logger.log(__('The %s host must be in the format "host:port".', type) + '\n');
					process.exit(1);
				}

				var parts = cli.argv[type + '-host'].split(':');

				if ((cli.argv.target === 'simulator' && parts.length < 2) || (cli.argv.target !== 'simulator' && parts.length < 4)) {
					logger.error(__('Invalid ' + type + ' host "%s"', cli.argv[type + '-host']) + '\n');
					if (cli.argv.target === 'simulator') {
						logger.log(__('The %s host must be in the format "host:port".', type) + '\n');
					} else {
						logger.log(__('The %s host must be in the format "host:port:airkey:hosts".', type) + '\n');
					}
					process.exit(1);
				}

				if (parts.length > 1 && parts[1]) {
					var port = parseInt(parts[1]);
					if (isNaN(port) || port < 1 || port > 65535) {
						logger.error(__('Invalid ' + type + ' host "%s"', cli.argv[type + '-host']) + '\n');
						logger.log(__('The port must be a valid integer between 1 and 65535.') + '\n');
						process.exit(1);
					}
				}
			}
		});
	}

	// since things are looking good, determine if files should be symlinked on copy
	this.symlinkFilesOnCopy = config.get('ios.symlinkResources', true) && !cli.argv['force-copy'] && !cli.argv['force-copy-all'];

	return function (callback) {
		// if there are any extensions, validate them
		async.eachSeries(this.tiapp.ios && Array.isArray(this.tiapp.ios.extensions) ? this.tiapp.ios.extensions : [], function (ext, next) {
			if (!ext.projectPath) {
				logger.error(__('iOS extensions must have a "projectPath" attribute that points to a folder containing an Xcode project') + '\n');
				process.exit(1);
			}

			// projectPath could be either the path to a project directory or the actual .xcodeproj
			ext.projectPath = appc.fs.resolvePath(ext.projectPath);

			var xcodeprojRegExp = /\.xcodeproj$/;

			if (!xcodeprojRegExp.test(ext.projectPath)) {
				// maybe we're the parent dir?
				ext.projectPath = path.join(ext.projectPath, path.basename(ext.projectPath) + '.xcodeproj');
			}

			var projectName = path.basename(ext.projectPath.replace(xcodeprojRegExp, ''));

			if (!fs.existsSync(ext.projectPath)) {
				logger.error(__('iOS extension "%s" Xcode project not found: %s', projectName, ext.projectPath) + '\n');
				process.exit(1);
			}

			var projFile = path.join(ext.projectPath, 'project.pbxproj');
			if (!fs.existsSync(projFile)) {
				logger.error(__('iOS extension "%s" project missing Xcode project file: %s', projectName, projFile) + '\n');
				process.exit(1);
			}

			if (!Array.isArray(ext.targets) || !ext.targets.length) {
				logger.warn(__('iOS extension "%s" has no targets, skipping.', projectName));
				return next();
			}

			var projectFile = xcode.project(path.join(ext.projectPath, 'project.pbxproj')).parseSync(),
				xobjs = projectFile.hash.project.objects,
				project = xobjs.PBXProject,
				missingTargets = {},
				swiftRegExp = /\.swift$/;

			ext.targets.forEach(function (target) { missingTargets[target.name] = 1; }),

			Object.keys(project).some(function (id) {
				if (!project[id] || typeof project[id] !== 'object') {
					return false;
				}

				project[id].targets.forEach(function (t) {
					if (missingTargets[t.comment]) {
						delete missingTargets[t.comment];

						// we have found our target!
						var nativeTarget = xobjs.PBXNativeTarget[t.value],
							cfg = xobjs.XCConfigurationList[nativeTarget.buildConfigurationList],
							cfgid = cfg.buildConfigurations
								.filter(function (c) { return c.comment.toLowerCase() === (cfg.defaultConfigurationName ? cfg.defaultConfigurationName.toLowerCase() : 'release'); })
								.map(function (c) { return c.value; })
								.shift(),
							buildSettings = xobjs.XCBuildConfiguration[cfgid].buildSettings,
							sourcesBuildPhase = nativeTarget.buildPhases.filter(function (p) { return /^Sources$/i.test(p.comment); });

						// check if this target contains any swift code
						if (sourcesBuildPhase.length && (!buildSettings.EMBEDDED_CONTENT_CONTAINS_SWIFT || /^NO$/i.test(buildSettings.EMBEDDED_CONTENT_CONTAINS_SWIFT))) {
							var files = xobjs.PBXSourcesBuildPhase[sourcesBuildPhase[0].value].files;
							if (files.some(function (f) { return swiftRegExp.test(xobjs.PBXBuildFile[f.value].fileRef_comment); })) {
								// oh no, error
								logger.error(__('iOS extension "%s" target "%s" contains Swift code, but "Embedded Content Contains Swift Code" is not enabled.', projectName, t.comment) + '\n');
								process.exit(1);
							}
						}
					}
				});
			});

			missingTargets = Object.keys(missingTargets);
			if (missingTargets.length) {
				logger.error(__n('iOS extension "%%s" does not contain a target named "%%s".', 'iOS extension "%%s" does not contain the following targets: "%%s".', missingTargets.length, projectName, missingTargets.join(', ')) + '\n');
				process.exit(1);
			}

			// check the Info.plist CFBundleIdentifier
			var appId = this.tiapp.id;
			(function walk(dir, ignore) {
				fs.readdirSync(dir).forEach(function (name) {
					if (ignore && ignore.test(name)) return;
					var file = path.join(dir, name);
					if (!fs.existsSync(file)) return;
					if (fs.statSync(file).isDirectory()) {
						walk(file);
					} else if (name === 'Info.plist') {
						var infoPlist = new appc.plist(file);
						if (infoPlist.WKWatchKitApp) {
							if (infoPlist.CFBundleIdentifier.indexOf(appId) !== 0) {
								logger.error(__('iOS extension "%s" WatchKit App bundle identifier is "%s", but must be prefixed with "%s".', projectName, infoPlist.CFBundleIdentifier, appId) + '\n');
								process.exit(1);
							}

							if (infoPlist.CFBundleIdentifier.toLowerCase() === appId.toLowerCase()) {
								logger.error(__('iOS extension "%s" WatchKit App bundle identifier must be different from the Titanium app\'s id "%s".', projectName, appId) + '\n');
								process.exit(1);
							}
						}
					}
				});
			}(path.dirname(ext.projectPath), /^build$/i));

			if (cli.argv.target !== 'simulator') {
				// check that all target provisioning profile uuids are valid
				ext.targets.forEach(function (target) {
					if (!target.ppUUIDs || !target.ppUUIDs[cli.argv.target]) {
						logger.error(__('iOS extension "%s" target "%s" is missing the %s provisioning profile UUID in tiapp.xml.', projectName, '<' + cli.argv.target + '>', target.name) + '\n');
						process.exit(1);
					}
				});
			}

			this.extensions.push(ext);

			next();
		}.bind(this), function (err) {
			this.validateTiModules(['ios', 'iphone'], this.deployType, function (err, modules) {
				this.modules = modules.found;

				this.commonJsModules = [];
				this.nativeLibModules = [];

				var nativeHashes = [];

				modules.found.forEach(function (module) {
					if (module.platform.indexOf('commonjs') !== -1) {
						module.native = false;

						module.libFile = path.join(module.modulePath, module.id + '.js');
						if (!fs.existsSync(module.libFile)) {
							this.logger.error(__('Module %s version %s is missing module file: %s', module.id.cyan, (module.manifest.version || 'latest').cyan, module.libFile.cyan) + '\n');
							process.exit(1);
						}

						this.commonJsModules.push(module);
					} else {
						module.native = true;

						module.libName = 'lib' + module.id.toLowerCase() + '.a',
						module.libFile = path.join(module.modulePath, module.libName);

						if (!fs.existsSync(module.libFile)) {
							this.logger.error(__('Module %s version %s is missing library file: %s', module.id.cyan, (module.manifest.version || 'latest').cyan, module.libFile.cyan) + '\n');
							process.exit(1);
						}

						nativeHashes.push(module.hash = this.hash(fs.readFileSync(module.libFile)));
						this.nativeLibModules.push(module);
					}

					// scan the module for any CLI hooks
					cli.scanHooks(path.join(module.modulePath, 'hooks'));
				}, this);

				this.modulesNativeHash = this.hash(nativeHashes.length ? nativeHashes.sort().join(',') : '');

				callback();
			}.bind(this));
		}.bind(this));
	}.bind(this);
};

/**
 * Performs the build operations.
 *
 * @param {Object} logger - The logger instance.
 * @param {Object} config - The Titanium CLI config instance.
 * @param {Object} cli - The Titanium CLI instance.
 * @param {Function} finished - A function to call when the build has finished or errored.
 */
iOSBuilder.prototype.run = function (logger, config, cli, finished) {
	Builder.prototype.run.apply(this, arguments);

	// force the platform to "ios" just in case it was "iphone" so that plugins can reference it
	cli.argv.platform = 'ios';

	series(this, [
		function (next) {
			cli.emit('build.pre.construct', this, next);
		},

		'doAnalytics',
		'initialize',
		'loginfo',
		'readBuildManifest',
		'checkIfNeedToRecompile',
		'initBuildDir',

		function (next) {
			cli.emit('build.pre.compile', this, next);
		},

		function () {
			// Make sure we have an app.js. This used to be validated in validate(), but since plugins like
			// Alloy generate an app.js, it may not have existed during validate(), but should exist now
			// that build.pre.compile was fired.
			ti.validateAppJsExists(this.projectDir, this.logger, ['iphone', 'ios']);
		},

		function (next) {
			parallel(this, [
				'createXcodeProject',
				'writeEntitlementsPlist',
				'writeInfoPlist',
				'writeMain',
				'writeXcodeConfigFiles',
				'copyTitaniumLibraries',
				'copyTitaniumiOSFiles'
			], next);
		},

		'copyExtensionFiles',

		// 'copyTitaniumFiles'

/*
		'copyItunesArtwork',
		'copyGraphics',

		function (next) {
			// this is a hack... for non-deployment builds we need to force xcode so that the pre-compile phase
			// is run and the ApplicationRouting.m gets updated
			if (!this.forceRebuild && this.deployType !== 'development') {
				this.logger.info(__('Forcing rebuild: deploy type is %s, so need to recompile ApplicationRouting.m', this.deployType));
				this.forceRebuild = true;
			}

			this.xcodePrecompilePhase(function () {
				if (this.forceRebuild || !fs.existsSync(this.xcodeAppDir)) {
					// we're not being called from Xcode, so we can call the pre-compile phase now
					// and save us several seconds
					parallel(this, [
						'optimizeImages',
						'invokeXcodeBuild'
					], next);
				} else {
					this.logger.info(__('Skipping xcodebuild'));
					next();
				}
			}.bind(this));
		},
*/

		'writeBuildManifest',

		function (next) {
			if (!this.buildOnly && this.target === 'simulator') {
				var delta = appc.time.prettyDiff(this.cli.startTime, Date.now());
				this.logger.info(__('Finished building the application in %s', delta.cyan));
			}

			cli.emit('build.post.compile', this, next);
		},

		function (next) {
			cli.emit('build.finalize', this, next);
		}
	], finished);
};

iOSBuilder.prototype.doAnalytics = function doAnalytics(next) {
	var cli = this.cli,
		eventName = cli.argv['device-family'] + '.' + cli.argv.target;

	if (cli.argv.target === 'dist-appstore' || cli.argv.target === 'dist-adhoc') {
		eventName = cli.argv['device-family'] + '.distribute.' + cli.argv.target.replace('dist-', '');
	} else if (this.allowDebugging && cli.argv['debug-host']) {
		eventName += '.debug';
	} else if (this.allowProfiling && cli.argv['profiler-host']) {
		eventName += '.profile';
	} else {
		eventName += '.run';
	}

	cli.addAnalyticsEvent(eventName, {
		dir: cli.argv['project-dir'],
		name: cli.tiapp.name,
		publisher: cli.tiapp.publisher,
		url: cli.tiapp.url,
		image: cli.tiapp.icon,
		appid: cli.tiapp.id,
		description: cli.tiapp.description,
		type: cli.argv.type,
		guid: cli.tiapp.guid,
		version: cli.tiapp.version,
		copyright: cli.tiapp.copyright,
		date: (new Date()).toDateString()
	});

	next();
};

iOSBuilder.prototype.initialize = function initialize() {
	var argv = this.cli.argv;

	// populate the build manifest object
	this.currentBuildManifest.target            = this.target;
	this.currentBuildManifest.deployType        = this.deployType;
	this.currentBuildManifest.sdkVersion        = this.tiapp['sdk-version'];
	this.currentBuildManifest.iosSdkVersion     = this.iosSdkVersion;
	this.currentBuildManifest.deviceFamily      = this.deviceFamily;
	this.currentBuildManifest.iosSdkPath        = this.platformPath;
	this.currentBuildManifest.tiCoreHash        = this.libTiCoreHash            = this.hash(fs.readFileSync(path.join(this.platformPath, 'libTiCore.a')));
	this.currentBuildManifest.developerName     = this.certDeveloperName        = argv['developer-name'];
	this.currentBuildManifest.distributionName  = this.certDistributionName     = argv['distribution-name'];
	this.currentBuildManifest.modulesHash       = this.modulesHash              = this.hash(!Array.isArray(this.tiapp.modules) ? '' : this.tiapp.modules.filter(function (m) {
			return !m.platform || /^iphone|ipad|ios|commonjs$/.test(m.platform);
		}).map(function (m) {
			return m.id + ',' + m.platform + ',' + m.version;
		}).join('|'));
	this.currentBuildManifest.modulesNativeHash  = this.modulesNativeHash;
	this.currentBuildManifest.gitHash            = ti.manifest.githash;
	this.currentBuildManifest.ppUuid             = this.provisioningProfileUUID = argv['pp-uuid'];
	this.currentBuildManifest.outputDir          = this.cli.argv['output-dir'],
	this.currentBuildManifest.forceCopy          = this.forceCopy               = !!argv['force-copy'];
	this.currentBuildManifest.forceCopyAll       = this.forceCopyAll            = !!argv['force-copy-all'];
	this.currentBuildManifest.name               = this.tiapp.name,
	this.currentBuildManifest.id                 = this.tiapp.id,
	this.currentBuildManifest.analytics          = this.tiapp.analytics,
	this.currentBuildManifest.publisher          = this.tiapp.publisher,
	this.currentBuildManifest.url                = this.tiapp.url,
	this.currentBuildManifest.version            = this.tiapp.version,
	this.currentBuildManifest.description        = this.tiapp.description,
	this.currentBuildManifest.copyright          = this.tiapp.copyright,
	this.currentBuildManifest.guid               = this.tiapp.guid,
	this.currentBuildManifest.skipJSMinification = !!this.cli.argv['skip-js-minify'],
	this.currentBuildManifest.encryptJS          = !!this.encryptJS

	// This is default behavior for now. Move this to true in phase 2.
	// Remove the debugHost/profilerHost check when we have debugging/profiling support with JSCore framework
	// TIMOB-17892
	this.currentBuildManifest.useJSCore          = this.useJSCore               = !this.debugHost && !this.profilerHost && this.cli.tiapp.ios && this.cli.tiapp.ios['use-jscore-framework'];

	this.moduleSearchPaths = [ this.projectDir, appc.fs.resolvePath(this.platformPath, '..', '..', '..', '..') ];
	if (this.config.paths && Array.isArray(this.config.paths.modules)) {
		this.moduleSearchPaths = this.moduleSearchPaths.concat(this.config.paths.modules);
	}

	this.debugHost     = this.allowDebugging && argv['debug-host'];
	this.profilerHost  = this.allowProfiling && argv['profiler-host'];
	this.buildOnly     = argv['build-only'];
	this.launchUrl     = argv['launch-url'];
	this.keychain      = argv['keychain'];
	this.deviceId      = argv['device-id'];
	this.deviceInfo    = this.deviceId ? this.getDeviceInfo().udids[this.deviceId] : null;
	this.xcodeTarget   = process.env.CONFIGURATION || (/^device|simulator$/.test(this.target) ? 'Debug' : 'Release');
	this.xcodeTargetOS = (this.target === 'simulator' ? 'iphonesimulator' : 'iphoneos') + version.format(this.iosSdkVersion, 2, 2);

	this.iosBuildDir            = path.join(this.buildDir, 'build', this.xcodeTarget + '-' + (this.target === 'simulator' ? 'iphonesimulator' : 'iphoneos'));
	this.xcodeAppDir            = path.join(this.iosBuildDir, this.tiapp.name + '.app');
	this.xcodeProjectConfigFile = path.join(this.buildDir, 'project.xcconfig');
	this.buildAssetsDir         = path.join(this.buildDir, 'assets');
	this.buildManifestFile      = path.join(this.buildDir, 'build-manifest.json');

	// make sure we have an icon
	if (!this.tiapp.icon || !['Resources', 'Resources/iphone', 'Resources/ios'].some(function (p) {
			return fs.existsSync(this.projectDir, p, this.tiapp.icon);
		}, this)) {
		this.tiapp.icon = 'appicon.png';
	}

	this.imagesOptimizedFile = path.join(this.buildDir, 'images_optimized');
	fs.existsSync(this.imagesOptimizedFile) && fs.unlinkSync(this.imagesOptimizedFile);
};

iOSBuilder.prototype.loginfo = function loginfo(next) {
	this.logger.debug(__('Titanium SDK iOS directory: %s', cyan(this.platformPath)));
	this.logger.info(__('Deploy type: %s', cyan(this.deployType)));
	this.logger.info(__('Building for target: %s', cyan(this.target)));
	this.logger.info(__('Building using iOS SDK: %s', cyan(version.format(this.iosSdkVersion, 2))));
	this.minIosVerMessage && this.logger.info(this.minIosVerMessage);

	if (this.buildOnly) {
		this.logger.info(__('Performing build only'));
	} else {
		if (this.target === 'simulator') {
			this.logger.info(__('Building for iOS Simulator: %s', cyan(this.deviceInfo.name)));
			this.logger.debug(__('UDID: %s', cyan(this.deviceId)));
			this.logger.debug(__('Simulator type: %s', cyan(this.deviceInfo.type)));
			this.logger.debug(__('Simulator version: %s', cyan(this.deviceInfo.ios)));
		} else if (this.target === 'device') {
			this.logger.info(__('Building for iOS device: %s', cyan(this.deviceId)));
		}
	}

	this.logger.info(__('Building for device family: %s', cyan(this.deviceFamily)));
	this.logger.debug(__('Setting Xcode target to %s', cyan(this.xcodeTarget)));
	this.logger.debug(__('Setting Xcode build OS to %s', cyan(this.xcodeTargetOS)));
	this.logger.debug(__('Xcode installation: %s', cyan(this.xcodeEnv.path)));
	this.logger.debug(__('iOS WWDR certificate: %s', cyan(this.iosInfo.certs.wwdr ? __('installed') : __('not found'))));

	if (this.target === 'device') {
		this.logger.info(__('iOS Development Certificate: %s', cyan(this.certDeveloperName)));
	} else if (/^dist-appstore|dist\-adhoc$/.test(this.target)) {
		this.logger.info(__('iOS Distribution Certificate: %s', cyan(this.certDistributionName)));
	}

	// validate the min-ios-ver from the tiapp.xml
	this.logger.info(__('Minimum iOS version: %s', cyan(version.format(this.minIosVer, 2, 3))));

	if (/^device|dist\-appstore|dist\-adhoc$/.test(this.target)) {
		if (this.keychain) {
			this.logger.info(__('Using keychain: %s', cyan(this.keychain)));
		} else {
			this.logger.info(__('Using default keychain'));
		}
	}

	if (this.debugHost) {
		this.logger.info(__('Debugging enabled via debug host: %s', cyan(this.debugHost)));
	} else {
		this.logger.info(__('Debugging disabled'));
	}

	if (this.profilerHost) {
		this.logger.info(__('Profiler enabled via profiler host: %s', cyan(this.profilerHost)));
	} else {
		this.logger.info(__('Profiler disabled'));
	}

	next();
};

iOSBuilder.prototype.readBuildManifest = function readBuildManifest() {
	// read the build manifest from the last build, if exists, so we
	// can determine if we need to do a full rebuild
	if (fs.existsSync(this.buildManifestFile)) {
		try {
			this.previousBuildManifest = JSON.parse(fs.readFileSync(this.buildManifestFile)) || {};
		} catch (e) {}
	}

	// now that we've read the build manifest, delete it so if this build
	// becomes incomplete, the next build will be a full rebuild
	fs.existsSync(this.buildManifestFile) && fs.unlinkSync(this.buildManifestFile);
};

iOSBuilder.prototype.checkIfNeedToRecompile = function checkIfNeedToRecompile() {
	var manifest = this.previousBuildManifest;

	// check if we need to clean the build directory
	this.forceCleanBuild = function () {
		// check if the --force flag was passed in
		if (this.cli.argv.force) {
			this.logger.info(__('Forcing clean build: %s flag was set', cyan('--force')));
			return true;
		}

		// check if the build manifest file exists
		if (!fs.existsSync(this.buildManifestFile)) {
			this.logger.info(__('Forcing clean build: %s does not exist', cyan(this.buildManifestFile)));
			return true;
		}

		// check the <sdk-version> from the tiapp.xml
		if (!appc.version.eq(this.tiapp['sdk-version'], manifest.sdkVersion)) {
			this.logger.info(__('Forcing rebuild: tiapp.xml Titanium SDK version changed since last build'));
			this.logger.info('  ' + __('Was: %s', cyan(manifest.sdkVersion)));
			this.logger.info('  ' + __('Now: %s', cyan(this.tiapp['sdk-version'])));
			return true;
		}

		// check if the titanium sdk version changed
		if (fs.existsSync(this.xcodeProjectConfigFile)) {
			// we have a previous build, see if the Titanium SDK changed
			var conf = fs.readFileSync(this.xcodeProjectConfigFile).toString(),
				versionMatch = conf.match(/TI_VERSION\=([^\n]*)/);

			if (versionMatch && !appc.version.eq(versionMatch[1], this.titaniumSdkVersion)) {
				this.logger.info(__('Forcing rebuild: Titanium SDK version in the project.xcconfig changed since last build'));
				this.logger.info('  ' + __('Was: %s', cyan(versionMatch[1])));
				this.logger.info('  ' + __('Now: %s', cyan(this.titaniumSdkVersion)));
				return true;
			}
		}

		return false;
	}.call(this);

	// if true, this will cause xcodebuild to be called
	// if false, it's possible that other steps after this will force xcodebuild to be called
	this.forceRebuild = this.forceCleanBuild || function () {
		// check if the xcode app directory exists
		if (!fs.existsSync(this.xcodeAppDir)) {
			this.logger.info(__('Forcing rebuild: %s does not exist', cyan(this.xcodeAppDir)));
			return true;
		}

		// check if the --force-copy or --force-copy-all flags were set
		if (this.forceCopy !== manifest.forceCopy) {
			this.logger.info(__('Forcing rebuild: force copy flag changed since last build'));
			this.logger.info('  ' + __('Was: %s', cyan(manifest.forceCopy)));
			this.logger.info('  ' + __('Now: %s', cyan(this.forceCopy)));
			return true;
		}

		if (this.forceCopyAll !== manifest.forceCopyAll) {
			this.logger.info(__('Forcing rebuild: force copy all flag changed since last build'));
			this.logger.info('  ' + __('Was: %s', cyan(manifest.forceCopyAll)));
			this.logger.info('  ' + __('Now: %s', cyan(this.forceCopyAll)));
			return true;
		}

		// check if the target changed
		if (this.target !== manifest.target) {
			this.logger.info(__('Forcing rebuild: target changed since last build'));
			this.logger.info('  ' + __('Was: %s', cyan(manifest.target)));
			this.logger.info('  ' + __('Now: %s', cyan(this.target)));
			return true;
		}

		if (fs.existsSync(this.xcodeProjectConfigFile)) {
			// we have a previous build, see if the app id changed
			var conf = fs.readFileSync(this.xcodeProjectConfigFile).toString(),
				idMatch = conf.match(/TI_APPID\=([^\n]*)/);

			if (idMatch && idMatch[1] !== this.tiapp.id) {
				this.logger.info(__('Forcing rebuild: app id changed since last build'));
				this.logger.info('  ' + __('Was: %s', cyan(idMatch[1])));
				this.logger.info('  ' + __('Now: %s', cyan(this.tiapp.id)));
				return true;
			}
		}

		// check that we have a libTiCore hash
		if (!manifest.tiCoreHash) {
			this.logger.info(__('Forcing rebuild: incomplete version file %s', cyan(this.buildVersionFile)));
			return true;
		}

		// determine the libTiCore hash and check if the libTiCore hashes are different
		if (this.libTiCoreHash !== manifest.tiCoreHash) {
			this.logger.info(__('Forcing rebuild: libTiCore hash changed since last build'));
			this.logger.info('  ' + __('Was: %s', cyan(manifest.tiCoreHash)));
			this.logger.info('  ' + __('Now: %s', cyan(this.libTiCoreHash)));
			return true;
		}

		// check if the titanium sdk paths are different
		if (manifest.iosSdkPath !== this.platformPath) {
			this.logger.info(__('Forcing rebuild: Titanium SDK path changed since last build'));
			this.logger.info('  ' + __('Was: %s', cyan(manifest.iosSdkPath)));
			this.logger.info('  ' + __('Now: %s', cyan(this.platformPath)));
			return true;
		}

		// check if the iOS SDK has changed
		if (manifest.iosSdkVersion !== this.iosSdkVersion) {
			this.logger.info(__('Forcing rebuild: iOS SDK version changed since last build'));
			this.logger.info('  ' + __('Was: %s', cyan(manifest.iosSdkVersion)));
			this.logger.info('  ' + __('Now: %s', cyan(this.iosSdkVersion)));
			return true;
		}

		// check if the device family has changed (i.e. was universal, now iphone)
		if (manifest.deviceFamily !== this.deviceFamily) {
			this.logger.info(__('Forcing rebuild: device family changed since last build'));
			this.logger.info('  ' + __('Was: %s', cyan(manifest.deviceFamily)));
			this.logger.info('  ' + __('Now: %s', cyan(this.deviceFamily)));
			return true;
		}

		// check the git hashes are different
		if (!manifest.gitHash || manifest.gitHash !== ti.manifest.githash) {
			this.logger.info(__('Forcing rebuild: githash changed since last build'));
			this.logger.info('  ' + __('Was: %s', cyan(manifest.gitHash)));
			this.logger.info('  ' + __('Now: %s', cyan(ti.manifest.githash)));
			return true;
		}

		// determine the modules hash and check if the modules hashes has changed
		if (this.modulesHash !== manifest.modulesHash) {
			this.logger.info(__('Forcing rebuild: modules hash changed since last build'));
			this.logger.info('  ' + __('Was: %s', cyan(manifest.modulesHash)));
			this.logger.info('  ' + __('Now: %s', cyan(this.modulesHash)));
			return true;
		}

		// check if the native modules hashes has changed
		if (this.modulesNativeHash !== manifest.modulesNativeHash) {
			this.logger.info(__('Forcing rebuild: native modules hash changed since last build'));
			this.logger.info('  ' + __('Was: %s', manifest.modulesNativeHash));
			this.logger.info('  ' + __('Now: %s', this.modulesNativeHash));
			return true;
		}

		// check if the provisioning profile has changed
		if (this.provisioningProfileUUID !== manifest.ppUuid) {
			this.logger.info(__('Forcing rebuild: provisioning profile changed since last build'));
			this.logger.info('  ' + __('Was: %s', manifest.ppUuid));
			this.logger.info('  ' + __('Now: %s', this.provisioningProfileUUID));
			return true;
		}

		// check if the use JavaScriptCore flag has changed
		if (this.useJSCore !== manifest.useJSCore) {
			this.logger.info(__('Forcing rebuild: use JSCore flag changed since last build'));
			this.logger.info('  ' + __('Was: %s', manifest.useJSCore));
			this.logger.info('  ' + __('Now: %s', this.useJSCore));
			return true;
		}

		// next we check if any tiapp.xml values changed so we know if we need to reconstruct the main.m
		// note: as soon as these tiapp.xml settings are written to an encrypted file instead of the binary, we can remove this whole section
		var tiappSettings = {
				'name':        'project name',
				'id':          'app id',
				'analytics':   'analytics flag',
				'publisher':   'publisher',
				'url':         'url',
				'version':     'version',
				'description': 'description',
				'copyright':   'copyright',
				'guid':        'guid'
			},
			changed = null;

		Object.keys(tiappSettings).some(function (key) {
			if (this.tiapp[key] !== manifest[key]) {
				changed = key;
				return true;
			}
		}, this);

		if (changed) {
			this.logger.info(__('Forcing rebuild: tiapp.xml %s changed since last build', tiappSettings[changed]));
			this.logger.info('  ' + __('Was: %s', cyan(manifest[changed])));
			this.logger.info('  ' + __('Now: %s', cyan(this.tiapp[changed])));
			return true;
		}

		return false;
	}.call(this);
};

iOSBuilder.prototype.initBuildDir = function initBuildDir() {
	this.logger.info(__('Initializing the build directory'));

	if (this.forceCleanBuild && fs.existsSync(this.buildDir)) {
		this.logger.debug(__('Recreating %s', cyan(this.buildDir)));
		wrench.rmdirSyncRecursive(this.buildDir);
		wrench.mkdirSyncRecursive(this.buildDir);
	} else if (!fs.existsSync(this.buildDir)) {
		this.logger.debug(__('Creating %s', cyan(this.buildDir)));
		wrench.mkdirSyncRecursive(this.buildDir);
	}
};

iOSBuilder.prototype.createXcodeProject = function createXcodeProject() {
	this.logger.info(__('Creating Xcode project'));

	var appName = this.tiapp.name,
		srcFile = path.join(this.platformPath, 'iphone', 'Titanium.xcodeproj', 'project.pbxproj'),
		destFile = path.join(this.buildDir, this.tiapp.name + '.xcodeproj', 'project.pbxproj'),
		contents = fs.readFileSync(srcFile).toString(),
		xcodeProject = xcode.project(destFile),
		xobjs;

	/*
	namespace = (function (name) {
				name = name.replace(/-/g, '_').replace(/\W/g, '')
				return /^[0-9]/.test(name) ? 'k' + name : name;
			}(this.tiapp.name)),
	*/

	// turns out it's way faster to do string replacement before parsing
	xcodeProject.hash = xcodeParser.parse(
		fs.readFileSync(srcFile)
			.toString()
			.replace(/Titanium\.plist/g, 'Info.plist')
			.replace(/Titanium_Prefix\.pch/g, appName.replace(/-/g, '_').replace(/\W/g, '') + '_Prefix.pch')
			.replace(/\.\.\/(Classes|Resources|headers|lib)/g, '$1')
			.replace(/Titanium/g, appName)
	);
	xobjs = xcodeProject.hash.project.objects;

	// delete the pre-compile build phases since we don't need it
	Object.keys(xobjs.PBXShellScriptBuildPhase).forEach(function (buildPhaseUuid) {
		if (xobjs.PBXShellScriptBuildPhase[buildPhaseUuid] && typeof xobjs.PBXShellScriptBuildPhase[buildPhaseUuid] === 'object' && /^"?Pre-Compile"?$/i.test(xobjs.PBXShellScriptBuildPhase[buildPhaseUuid].name)) {
			Object.keys(xobjs.PBXNativeTarget).forEach(function (key) {
				if (xobjs.PBXNativeTarget[key] && typeof xobjs.PBXNativeTarget[key] === 'object') {
					xobjs.PBXNativeTarget[key].buildPhases = xobjs.PBXNativeTarget[key].buildPhases.filter(function (phase) {
						return phase.value !== buildPhaseUuid;
					});
				}
			});
			delete xobjs.PBXShellScriptBuildPhase[buildPhaseUuid];
			delete xobjs.PBXShellScriptBuildPhase[buildPhaseUuid + '_comment'];
		}
	});

	// add the post-compile phase
	var postCompilePhaseUuid = xcodeProject.generateUuid();
	xobjs.PBXShellScriptBuildPhase[postCompilePhaseUuid] = {
		isa: 'PBXShellScriptBuildPhase',
		buildActionMask: 2147483647,
		files: [],
		inputPaths: [],
		name: '"Post-Compile"',
		outputPaths: [],
		runOnlyForDeploymentPostprocessing: 0,
		shellPath: '/bin/sh',
		shellScript: '"' + [
			'echo \\"Xcode Post-Compile Phase: Touching important files\\"',
			'touch -c Classes/ApplicationRouting.h Classes/ApplicationRouting.m Classes/ApplicationDefaults.m Classes/ApplicationMods.m Classes/defines.h',
			'if [ \\"x$TITANIUM_CLI_IMAGES_OPTIMIZED\\" != \\"x\\" ]; then',
			'    if [ -f \\"$TITANIUM_CLI_IMAGES_OPTIMIZED\\" ]; then',
			'        echo \\"Xcode Post-Compile Phase: Image optimization finished before xcodebuild finished, continuing\\"',
			'    else',
			'        echo \\"Xcode Post-Compile Phase: Waiting for image optimization to complete\\"',
			'        echo \\"Xcode Post-Compile Phase: $TITANIUM_CLI_IMAGES_OPTIMIZED\\"',
			'        while [ ! -f \\"$TITANIUM_CLI_IMAGES_OPTIMIZED\\" ]',
			'        do',
			'            sleep 1',
			'        done',
			'        echo \\"Xcode Post-Compile Phase: Image optimization complete, continuing\\"',
			'    fi',
			'fi'
		].join('\\n') + '"'
	};
	xobjs.PBXShellScriptBuildPhase[postCompilePhaseUuid + '_comment'] = 'Post-Compile';

	Object.keys(xobjs.PBXNativeTarget).some(function (targetUuid) {
		if (xobjs.PBXNativeTarget[targetUuid].name === appName) {
			xobjs.PBXNativeTarget[targetUuid].buildPhases.push({
				value: postCompilePhaseUuid,
				comment: 'Post-Compile'
			});
			return true;
		}
	});

	var pbxProject = xobjs.PBXProject[Object.keys(xobjs.PBXProject).filter(function (id) { return typeof xobjs.PBXProject[id] === 'object'; }).shift()];

	// add extensions and their targets to the project
	this.extensions.forEach(function (ext) {
		var extProject = xcode.project(path.join(ext.projectPath, 'project.pbxproj')).parseSync(),
			extObjs = extProject.hash.project.objects;

dump(extObjs);

		ext.targets.forEach(function (target) {
			Object.keys(extObjs.PBXProject).some(function (projectUuid) {
				var extPBXProject = extObjs.PBXProject[projectUuid];
				if (typeof extPBXProject !== 'object') return;

				return extPBXProject.targets.some(function (t) {
					if (t.comment === target.name) {
						pbxProject.targets.push(t);
						dump(t.value);

						if (extPBXProject.attributes && extPBXProject.attributes.TargetAttributes && extPBXProject.attributes.TargetAttributes[t.value]) {
							pbxProject.attributes || (pbxProject.attributes = {});
							pbxProject.attributes.TargetAttributes || (pbxProject.attributes.TargetAttributes = {});
							pbxProject.attributes.TargetAttributes[t.value] = extPBXProject.attributes.TargetAttributes[t.value];
						}

						return true;
					}
				});

/*
				extPBXProject.buildConfigurationList
				extPBXProject.mainGroup
				extPBXProject.productRefGroup
*/


			});
		});

console.log('\n\n\n\n');
dump(xobjs);

process.exit(0);
	});



/*
	if project does not exist or any source project changed
	- write the new xcode project
	- forceRebuild = true


		srcStat = fs.statSync(srcFile),
		prev = this.previousBuildManifest[srcFile],
		hash = this.hash(contents),

	if (prev && prev.size === srcStat.size && prev.mtime === srcStat.mtime && prev.hash === hash) {

		// titanium's xcode project did not change

		// what about extension projects?
	}

	this.currentBuildManifest.files[srcFile] = {
		hash: hash,
		mtime: srcStat.mtime,
		size: srcStat.size
	};
*/


	this.logger.debug(__('Writing %s', xcodeProject.filepath.cyan));
	fs.existsSync(path.dirname(xcodeProject.filepath)) || wrench.mkdirSyncRecursive(path.dirname(xcodeProject.filepath));
	fs.writeFileSync(xcodeProject.filepath, titaniumProject.writeSync());
};

iOSBuilder.prototype.writeEntitlementsPlist = function writeEntitlementsPlist() {
	this.logger.info(__('Creating Entitlements.plist'));

	// allow the project to have its own custom entitlements
	var entitlementsFile = path.join(this.projectDir, 'Entitlements.plist'),
		outputFile = path.join(this.buildDir, 'Entitlements.plist');

	if (fs.existsSync(entitlementsFile)) {
		this.logger.info(__('Found custom entitlements: %s', entitlementsFile.cyan, outputFile));
		this.copyFileSync(entitlementsFile, outputFile);
		return;
	}

	function getPP(list, uuid) {
		for (var i = 0, l = list.length; i < l; i++) {
			if (list[i].uuid === uuid) {
				return list[i];
			}
		}
	}

	var pp;
	if (this.target === 'device') {
		pp = getPP(this.iosInfo.provisioning.development, this.provisioningProfileUUID);
	} else {
		pp = getPP(this.iosInfo.provisioning.distribution, this.provisioningProfileUUID);
		if (!pp) {
			pp = getPP(this.iosInfo.provisioning.adhoc, this.provisioningProfileUUID);
		}
	}

	var plist = new appc.plist();
	if (pp) {
		// attempt to customize it by reading provisioning profile
		(this.target === 'dist-appstore') && (plist['beta-reports-active'] = true);
		plist['get-task-allow'] = !!pp.getTaskAllow;
		pp.apsEnvironment && (plist['aps-environment'] = pp.apsEnvironment);
		plist['application-identifier'] = pp.appPrefix + '.' + this.tiapp.id;
		plist['keychain-access-groups'] = [ plist['application-identifier'] ];
	}

	this.logger.debug(__('Writing %s', outputFile.cyan));
	fs.writeFileSync(outputFile, plist.toString('xml'));
};

iOSBuilder.prototype.writeInfoPlist = function writeInfoPlist() {
	this.logger.info(__('Creating Info.plist'));

	var defaultInfoPlistFile = path.join(this.platformPath, 'Info.plist'),
		customInfoPlistFile = this.projectDir + '/Info.plist',
		plist = this.infoPlist = new appc.plist(),
		iphone = this.tiapp.iphone,
		ios = this.tiapp.ios,
		fbAppId = this.tiapp.properties && this.tiapp.properties['ti.facebook.appid'] && this.tiapp.properties['ti.facebook.appid']['value'],
		iconName = this.tiapp.icon.replace(/(.+)(\..*)$/, '$1'), // note: this is basically stripping the file extension
		consts = {
			'__APPICON__': iconName,
			'__PROJECT_NAME__': this.tiapp.name,
			'__PROJECT_ID__': this.tiapp.id,
			'__URL__': this.tiapp.id,
			'__URLSCHEME__': this.tiapp.name.replace(/\./g, '_').replace(/ /g, '').toLowerCase(),
			'__ADDITIONAL_URL_SCHEMES__': fbAppId ? '<string>fb' + fbAppId + '</string>' : ''
		};

	// default Info.plist
	if (fs.existsSync(defaultInfoPlistFile)) {
		plist.parse(fs.readFileSync(defaultInfoPlistFile).toString().replace(/(__.+__)/g, function (match, key, format) {
			return consts.hasOwnProperty(key) ? consts[key] : '<!-- ' + key + ' -->'; // if they key is not a match, just comment out the key
		}));

		// override the default versions with the tiapp.xml version
		plist.CFBundleVersion = String(this.tiapp.version);
		try {
			plist.CFBundleShortVersionString = appc.version.format(this.tiapp.version, 0, 3);
		} catch (ex) {
			plist.CFBundleShortVersionString = this.tiapp.version;
		}

		// this should not exist, but nuke it so we can create it below
		delete plist.UIAppFonts;

		// override the default icons with the tiapp.xml version
		Array.isArray(plist.CFBundleIconFiles) || (plist.CFBundleIconFiles = []);
		['.png', '@2x.png', '-72.png', '-60.png', '-60@2x.png', '-60@3x.png', '-76.png', '-76@2x.png', '-Small-50.png', '-72@2x.png', '-Small-50@2x.png', '-Small.png', '-Small@2x.png', '-Small@3x.png', '-Small-40.png', '-Small-40@2x.png'].forEach(function (name) {
			name = iconName + name;
			if (fs.existsSync(path.join(this.projectDir, 'Resources', name)) ||
				fs.existsSync(path.join(this.projectDir, 'Resources', 'iphone', name)) ||
				fs.existsSync(path.join(this.projectDir, 'Resources', 'ios', name))) {
				if (plist.CFBundleIconFiles.indexOf(name) === -1) {
					plist.CFBundleIconFiles.push(name);
				}
			}
		}, this);

		// override the default launch screens
		var resourceDir = path.join(this.projectDir, 'Resources'),
			iphoneDir = path.join(resourceDir, 'iphone'),
			iosDir = path.join(resourceDir, 'ios'),
			i18nLaunchScreens = ti.i18n.splashScreens(this.projectDir, this.logger).map(function (p) { return path.basename(p); });
		[{
			'orientation': 'Portrait',
			'minimum-system-version': '8.0',
			'name': 'Default-Portrait',
			'subtype': '736h',
			'scale': ['3x'],
			'size': '{414, 736}'
		},
		{
			'orientation': 'Landscape',
			'minimum-system-version': '8.0',
			'name': 'Default-Landscape',
			'subtype': '736h',
			'scale': ['3x'],
			'size': '{414, 736}'
		},
		{
			'orientation': 'Portrait',
			'minimum-system-version': '8.0',
			'name': 'Default',
			'subtype': '667h',
			'scale': ['2x'],
			'size': '{375, 667}'
		},
		{
			'orientation': 'Portrait',
			'minimum-system-version': '7.0',
			'name': 'Default',
			'scale': ['2x', '1x'],
			'size': '{320, 480}'
		},
		{
			'orientation': 'Portrait',
			'minimum-system-version': '7.0',
			'name': 'Default',
			'subtype': '568h',
			'scale': ['2x'],
			'size': '{320, 568}'
		},
		{
			'orientation': 'Portrait',
			'idiom': 'ipad',
			'minimum-system-version': '7.0',
			'name': 'Default-Portrait',
			'scale': ['2x', '1x'],
			'size': '{768, 1024}'
		},
		{
			'orientation': 'Landscape',
			'idiom': 'ipad',
			'minimum-system-version': '7.0',
			'name': 'Default-Landscape',
			'scale': ['2x', '1x'],
			'size': '{768, 1024}'
		}].forEach(function (asset) {
			asset.scale.some(function (scale) {
				var key,
					basefilename = asset.name + (asset.subtype ? '-' + asset.subtype : ''),
					filename = basefilename + (scale !== '1x' ? '@' + scale : '') + '.png';

				if (i18nLaunchScreens.indexOf(filename) !== -1 ||
					fs.existsSync(path.join(resourceDir, filename)) ||
					fs.existsSync(path.join(iphoneDir, filename)) ||
					fs.existsSync(path.join(iosDir, filename))
				) {
					key = 'UILaunchImages' + (asset.idiom === 'ipad' ? '~ipad' : '');
					Array.isArray(plist[key]) || (plist[key] = []);
					plist[key].push({
						UILaunchImageName: basefilename,
						UILaunchImageOrientation: asset.orientation,
						UILaunchImageSize: asset.size,
						UILaunchImageMinimumOSVersion: asset['minimum-system-version']
					});
					return true;
				}
			});
		});
	}

	function merge(src, dest) {
		Object.keys(src).forEach(function (prop) {
			if (!/^\+/.test(prop)) {
				if (Object.prototype.toString.call(src[prop]) === '[object Object]') {
					dest.hasOwnProperty(prop) || (dest[prop] = {});
					merge(src[prop], dest[prop]);
				} else {
					dest[prop] = src[prop];
				}
			}
		});
	}

	// if the user has a Info.plist in their project directory, consider that a custom override
	if (fs.existsSync(customInfoPlistFile)) {
		this.logger.info(__('Copying custom Info.plist from project directory'));
		var custom = new appc.plist().parse(fs.readFileSync(customInfoPlistFile).toString());
		merge(custom, plist);
	}

	// tiapp.xml settings override the default and custom Info.plist
	plist.UIRequiresPersistentWiFi = this.tiapp.hasOwnProperty('persistent-wifi')  ? !!this.tiapp['persistent-wifi']  : false;
	plist.UIPrerenderedIcon        = this.tiapp.hasOwnProperty('prerendered-icon') ? !!this.tiapp['prerendered-icon'] : false;
	plist.UIStatusBarHidden        = this.tiapp.hasOwnProperty('statusbar-hidden') ? !!this.tiapp['statusbar-hidden'] : false;

	plist.UIStatusBarStyle = 'UIStatusBarStyleDefault';
	if (/opaque_black|opaque|black/.test(this.tiapp['statusbar-style'])) {
		plist.UIStatusBarStyle = 'UIStatusBarStyleBlackOpaque';
	} else if (/translucent_black|transparent|translucent/.test(this.tiapp['statusbar-style'])) {
		plist.UIStatusBarStyle = 'UIStatusBarStyleBlackTranslucent';
	}

	if (iphone) {
		if (iphone.orientations) {
			var orientationsMap = {
				'PORTRAIT':        'UIInterfaceOrientationPortrait',
				'UPSIDE_PORTRAIT': 'UIInterfaceOrientationPortraitUpsideDown',
				'LANDSCAPE_LEFT':  'UIInterfaceOrientationLandscapeLeft',
				'LANDSCAPE_RIGHT': 'UIInterfaceOrientationLandscapeRight'
			};

			Object.keys(iphone.orientations).forEach(function (key) {
				var entry = 'UISupportedInterfaceOrientations' + (key === 'ipad' ? '~ipad' : '');

				Array.isArray(plist[entry]) || (plist[entry] = []);
				iphone.orientations[key].forEach(function (name) {
					var value = orientationsMap[name.split('.').pop().toUpperCase()] || name;
					// name should be in the format Ti.UI.PORTRAIT, so pop the last part and see if it's in the map
					if (plist[entry].indexOf(value) === -1) {
						plist[entry].push(value);
					}
				});
			});
		}

		if (iphone.backgroundModes) {
			plist.UIBackgroundModes = (plist.UIBackgroundModes || []).concat(iphone.backgroundModes);
		}

		if (iphone.requires) {
			plist.UIRequiredDeviceCapabilities = (plist.UIRequiredDeviceCapabilities || []).concat(iphone.requiredFeatures);
		}

		if (iphone.types) {
			Array.isArray(plist.CFBundleDocumentTypes) || (plist.CFBundleDocumentTypes = []);
			iphone.types.forEach(function (type) {
				var types = plist.CFBundleDocumentTypes,
					match = false,
					i = 0;

				for (; i < types.length; i++) {
					if (types[i].CFBundleTypeName === type.name) {
						types[i].CFBundleTypeIconFiles = type.icon;
						types[i].LSItemContentTypes = type.uti;
						types[i].LSHandlerRank = type.owner ? 'Owner' : 'Alternate';
						match = true;
						break;
					}
				}

				if (!match) {
					types.push({
						CFBundleTypeName: type.name,
						CFBundleTypeIconFiles: type.icon,
						LSItemContentTypes: type.uti,
						LSHandlerRank: type.owner ? 'Owner' : 'Alternate'
					});
				}
			});
		}
	}

	// custom Info.plist from the tiapp.xml overrides everything
	ios && ios.plist && merge(ios.plist, plist);

	// override the CFBundleIdentifier to the app id
	plist.CFBundleIdentifier = this.tiapp.id;

	if (this.target === 'device' && this.deviceId === 'itunes') {
		// device builds require an additional token to ensure uniqueness so that iTunes will detect an updated app to sync.
		// we drop the milliseconds from the current time so that we still have a unique identifier, but is less than 10
		// characters so iTunes 11.2 doesn't get upset.
		plist.CFBundleVersion = String(+new Date);
		this.logger.debug(__('Building for iTunes sync which requires us to set the CFBundleVersion to a unique number to trigger iTunes to update your app'));
		this.logger.debug(__('Setting Info.plist CFBundleVersion to current epoch time %s', plist.CFBundleVersion.cyan));
	}

	// scan for ttf and otf font files
	var fontMap = {};
	(function scanFonts(dir, isRoot) {
		fs.existsSync(dir) && fs.readdirSync(dir).forEach(function (file) {
			var p = path.join(dir, file);
			if (fs.statSync(p).isDirectory() && (!isRoot || file === 'iphone' || file === 'ios' || ti.availablePlatformsNames.indexOf(file) === -1)) {
				scanFonts(p);
			} else if (/\.(otf|ttf)$/i.test(file)) {
				fontMap['/' + p.replace(iphoneDir, '').replace(iosDir, '').replace(resourceDir, '').replace(/^\//, '')] = 1;
			}
		});
	}(resourceDir, true));

	if (Array.isArray(plist.UIAppFonts)) {
		plist.UIAppFonts.forEach(function (f) {
			if (!fontMap[f]) {
				this.logger.warn(__('Info.plist references non-existent font: %s', cyan(f)));
				fontMap[f] = 1;
			}
		});
	}

	var fonts = Object.keys(fontMap);
	fonts.length && (plist.UIAppFonts = fonts);

	// write the Info.plist
	var dest = path.join(this.buildDir, 'Info.plist');
	this.logger.debug(__('Writing %s', dest.cyan));
	fs.writeFileSync(dest, plist.toString('xml'));
};

iOSBuilder.prototype.writeMain = function writeMain() {
	var consts = {
			'__PROJECT_NAME__':     this.tiapp.name,
			'__PROJECT_ID__':       this.tiapp.id,
			'__DEPLOYTYPE__':       this.deployType,
			'__APP_ID__':           this.tiapp.id,
			'__APP_ANALYTICS__':    String(this.tiapp.hasOwnProperty('analytics') ? !!this.tiapp.analytics : true),
			'__APP_PUBLISHER__':    this.tiapp.publisher,
			'__APP_URL__':          this.tiapp.url,
			'__APP_NAME__':         this.tiapp.name,
			'__APP_VERSION__':      this.tiapp.version,
			'__APP_DESCRIPTION__':  this.tiapp.description,
			'__APP_COPYRIGHT__':    this.tiapp.copyright,
			'__APP_GUID__':         this.tiapp.guid,
			'__APP_RESOURCE_DIR__': '',
			'__APP_DEPLOY_TYPE__':  this.buildType
		},
		contents = fs.readFileSync(path.join(this.platformPath, 'main.m')).toString().replace(/(__.+?__)/g, function (match, key, format) {
			var s = consts.hasOwnProperty(key) ? consts[key] : key;
			return typeof s === 'string' ? s.replace(/"/g, '\\"').replace(/\n/g, '\\n') : s;
		}),
		dest = path.join(this.buildDir, 'main.m'),
		exists = fs.existsSync(dest);

	if (!exists || contents !== fs.readFileSync(dest).toString()) {
		if (exists && !this.forceRebuild) {
			this.logger.info(__('Forcing rebuild: main.m has changed since last build'));
		}
		this.forceRebuild = true;

		this.logger.debug(__('Writing %s', dest.cyan));
		fs.writeFileSync(dest, contents);
	}
};

iOSBuilder.prototype.writeXcodeConfigFiles = function writeXcodeConfigFiles() {
	this.logger.info(__('Creating Xcode config files'));

	// write the project.xcconfig
	this.logger.debug(__('Writing %s', this.xcodeProjectConfigFile.cyan));
	fs.writeFileSync(this.xcodeProjectConfigFile, [
		'TI_VERSION=' + this.titaniumSdkVersion,
		'TI_SDK_DIR=' + this.platformPath.replace(this.titaniumSdkVersion, '$(TI_VERSION)'),
		'TI_APPID=' + this.tiapp.id,
		'JSCORE_LD_FLAGS=-weak_framework JavaScriptCore',
		'TICORE_LD_FLAGS=-weak-lti_ios_profiler -weak-lti_ios_debugger -weak-lTiCore',
		'OTHER_LDFLAGS[sdk=iphoneos*]=$(inherited) ' + (this.useJSCore ? '$(JSCORE_LD_FLAGS)' : '$(TICORE_LD_FLAGS)'),
		'OTHER_LDFLAGS[sdk=iphonesimulator*]=$(inherited) ' + (this.useJSCore ? '$(JSCORE_LD_FLAGS)' : '$(TICORE_LD_FLAGS)'),
		'#include "module"'
	].join('\n') + '\n');

	// write the module.xcconfig
	var dest = path.join(this.buildDir, 'module.xcconfig'),
		exists = fs.existsSync(dest),
		variables = {},
		contents = [
			'// this is a generated file - DO NOT EDIT',
			''
		];

	this.modules.forEach(function (m) {
		var moduleId = m.manifest.moduleid.toLowerCase(),
			moduleName = m.manifest.name.toLowerCase(),
			prefix = m.manifest.moduleid.toUpperCase().replace(/\./g, '_');

		[	path.join(m.modulePath, 'module.xcconfig'),
			path.join(this.projectDir, 'modules', 'iphone', moduleName + '.xcconfig')
		].forEach(function (file) {
			if (fs.existsSync(file)) {
				var xc = new appc.xcconfig(file);
				Object.keys(xc).forEach(function (key) {
					var name = (prefix + '_' + key).replace(/[^\w]/g, '_');
					Array.isArray(variables[key]) || (variables[key] = []);
					variables[key].push(name);
					contents.push((name + '=' + xc[key]).replace(new RegExp('\$\(' + key + '\)', 'g'), '$(' + name + ')'));
				});
			}
		});
	}, this);

	Object.keys(variables).forEach(function (v) {
		contents.push(v + '=$(inherited) ' + variables[v].map(function (x) { return '$(' + x + ') '; }).join(''));
	});
	contents = contents.join('\n');

	if (!exists || contents !== fs.readFileSync(dest).toString()) {
		if (exists && !this.forceRebuild) {
			this.logger.info(__('Forcing rebuild: main.m has changed since last build'));
		}
		this.forceRebuild = true;

		this.logger.debug(__('Writing %s', dest.cyan));
		fs.writeFileSync(dest, contents);
	}
};

iOSBuilder.prototype.copyTitaniumLibraries = function copyTitaniumLibraries() {
	this.logger.info(__('Copying Titanium libraries'));

	var libDir = path.join(this.buildDir, 'lib');
	fs.existsSync(libDir) || wrench.mkdirSyncRecursive(libDir);

	this.copyFileSync(path.join(this.platformPath, 'libTiCore.a'),          path.join(libDir, 'libTiCore.a'), this.forceCopyAll);
	this.copyFileSync(path.join(this.platformPath, 'libtiverify.a'),        path.join(libDir, 'libtiverify.a'));
	this.copyFileSync(path.join(this.platformPath, 'libti_ios_debugger.a'), path.join(libDir, 'libti_ios_debugger.a'));
	this.copyFileSync(path.join(this.platformPath, 'libti_ios_profiler.a'), path.join(libDir, 'libti_ios_profiler.a'));
};

iOSBuilder.prototype.copyTitaniumiOSFiles = function copyTitaniumiOSFiles() {
	this.logger.info(__('Copying Titanium iOS files'));

	var name = this.tiapp.name.replace(/-/g, '_').replace(/\W/g, ''),
		namespace = /^[0-9]/.test(name) ? 'k' + name : name,
		copyFileRegExps = [
			// note: order of regexps matters
			[/TitaniumViewController/g, namespace + '$ViewController'],
			[/TitaniumModule/g, namespace + '$Module'],
			[/Titanium|Appcelerator/g, namespace],
			[/titanium/g, '_' + namespace.toLowerCase()],
			[/(org|com)\.appcelerator/g, '$1.' + namespace.toLowerCase()],
			[new RegExp('\\* ' + namespace + ' ' + namespace + ' Mobile', 'g'), '* Appcelerator Titanium Mobile'],
			[new RegExp('\\* Copyright \\(c\\) \\d{4}(-\\d{4})? by ' + namespace + ', Inc\\.', 'g'), '* Copyright (c) 2009-' + (new Date).getFullYear() + ' by Appcelerator, Inc.'],
			[/(\* Please see the LICENSE included with this distribution for details.\n)(?! \*\s*\* WARNING)/g, '$1 * \n * WARNING: This is generated code. Modify at your own risk and without support.\n']
		],
		extRegExp = /\.(c|cpp|h|m|mm)$/,

		// files to watch for while copying
		appDefaultFile = path.join(this.buildDir, 'Classes', 'ApplicationDefaults.m'),
		appModsFile = path.join(this.buildDir, 'Classes', 'ApplicationMods.m');

	['Classes', 'headers'].forEach(function (dir) {
		this.copyDirSync(path.join(this.platformPath, dir), path.join(this.buildDir, dir), {
			ignoreDirs: this.ignoreDirs,
			ignoreFiles: /^(bridge\.txt|libTitanium\.a|\.gitignore|\.npmignore|\.cvsignore|\.DS_Store|\._.*|[Tt]humbs.db|\.vspscc|\.vssscc|\.sublime-project|\.sublime-workspace|\.project|\.tmproj)$/,
			callback: function (srcFile, destFile, srcStat) {
				var rel = destFile.replace(dir, ''),
					destExists = fs.existsSync(destFile),
					contents = null,
					hash = null;

				// ApplicationDefaults.m
				if (destFile === appDefaultsFile) {
					contents = ejs.render(fs.readFileSync(path.join(this.templatesDir, 'ApplicationDefaults.m')).toString(), {
						props:      this.tiapp.properties || {},
						deployType: this.deployType,
						launchUrl:  this.launchUrl
					});
					hash = this.hash(contents);

					if (destExists && fs.readFileSync(destFile).toString() !== contents) {
						if (!this.forceRebuild) {
							this.logger.info(__('Forcing rebuild: ApplicationDefaults.m has changed since last build'));
						}
						this.forceRebuild = true;
					}

				// ApplicationMods.m
				} else if (this.modules.length && destFile === appModsFile) {
					contents = ejs.render(fs.readFileSync(path.join(this.templatesDir, 'ApplicationMods.m')).toString(), {
						modules: this.modules
					});
					hash = this.hash(contents);

					if (destExists && fs.readFileSync(destFile).toString() !== contents) {
						if (!this.forceRebuild) {
							this.logger.info(__('Forcing rebuild: ApplicationMods.m has changed since last build'));
						}
						this.forceRebuild = true;
					}

				// test to make sure the file is not a binary file extension before we do string operations on it
				} else if (extRegExp.test(srcFile) && srcFile.indexOf('TiCore') === -1) {
					contents = fs.readFileSync(srcFile).toString();
					hash = this.hash(contents);

					if (destExists) {
						// look up the file to see if the original source changed
						var prev = this.previousBuildManifest.files && this.previousBuildManifest.files[rel];
						if (prev && prev.size === srcStat.size && prev.mtime === srcStat.mtime && prev.hash === hash) {
							// the original hasn't changed, so let's assume that there's nothing to do
							return null;
						}

						fs.unlinkSync(destFile);
					}

					for (var i = 0, l = copyFileRegExps.length; i < l; i++) {
						contents = contents.replace(copyFileRegExps[i][0], copyFileRegExps[i][1]);
					}
				}

				if (contents) {
					this.currentBuildManifest.files[rel] = {
						hash: hash,
						mtime: srcStat.mtime,
						size: srcStat.size
					};

					this.logger.info(__('Writing %s', destFile.cyan));
					fs.writeFileSync(destFile, contents);

					return null; // tell copyDirSync not to copy the file because we wrote it ourselves
				}

			}.bind(this)
		});
	}, this);

	this.copyFileSync(path.join(this.platformPath, this.platformName, 'Titanium_Prefix.pch'), path.join(this.buildDir, name + '_Prefix.pch'));
};

iOSBuilder.prototype.copyExtensionFiles = function copyExtensionFiles() {
	if (!this.extensions.length) return;

	this.logger.info(__('Copying Extension files'));

	this.extensions.forEach(function (extension) {
		var src = path.dirname(extension.projectPath),
			dest = path.join(this.buildDir, 'extensions', path.dirname(src));
		this.copyDirSync(src, dest, {
			rootIgnoreDirs: /^build$/i,
			ignoreDirs: this.ignoreDirs,
			ignoreFiles: this.ignoreFiles,
			callback: function (srcFile, destFile, srcStat) {
				if (path.basename(srcFile) === 'Info.plist') {
					// validate the info.plist
					var infoPlist = new appc.plist(srcFile);
					if (infoPlist.WKWatchKitApp) {
						infoPlist.WKCompanionAppBundleIdentifier = this.tiapp.id;

						if (infoPlist.CFBundleShortVersionString !== this.infoPlist.CFBundleShortVersionString) {
							this.logger.warn(__('WatchKit App\'s CFBundleShortVersionString "%s" does not match the app\'s CFBundleShortVersionString "%s".', infoPlist.CFBundleShortVersionString, this.infoPlist.CFBundleShortVersionString));
							this.logger.warn(__('Setting the WatchKit App\'s CFBundleShortVersionString to "%s"', this.infoPlist.CFBundleShortVersionString));
							infoPlist.CFBundleShortVersionString = this.infoPlist.CFBundleShortVersionString;
						}

						if (infoPlist.CFBundleVersion !== this.infoPlist.CFBundleVersion) {
							this.logger.warn(__('WatchKit App\'s CFBundleVersion "%s" does not match the app\'s CFBundleVersion "%s".', infoPlist.CFBundleVersion, this.infoPlist.CFBundleVersion));
							this.logger.warn(__('Setting the WatchKit App\'s CFBundleVersion to "%s"', this.infoPlist.CFBundleVersion));
							infoPlist.CFBundleVersion = this.infoPlist.CFBundleVersion;
						}

						this.hasWatchApp = true;

						this.logger.debug(__('Writing %s', destFile.cyan));
						fs.writeFileSync(destFile, infoPlist.toString('xml'));

						return null;
					}
				}
			}.bind(this)
		});
		extension.projectPath = path.join(dest, path.basename(extension.projectPath));
	}, this);
};

/*
	copy titanium files

	// recreate the build directory (<project dir>/build/[iphone|ios]/assets)
	fs.existsSync(this.buildAssetsDir) && wrench.rmdirSyncRecursive(this.buildAssetsDir);
	wrench.mkdirSyncRecursive(this.buildAssetsDir);
*/

iOSBuilder.prototype.copyItunesArtwork = function copyItunesArtwork(next) {
	// note: iTunesArtwork is a png image WITHOUT the file extension and the
	// purpose of this function is to copy it from the root of the project.
	// The preferred location of this file is <project-dir>/Resources/iphone
	// or <project-dir>/platform/iphone.
/*




FIX THIS SHIT



	if (/device|dist\-appstore|dist\-adhoc/.test(this.target)) {
		this.logger.info(__('Copying iTunes artwork'));
		fs.readdirSync(this.projectDir).forEach(function (file) {
			var src = path.join(this.projectDir, file),
				m = file.match(/^iTunesArtwork(@2x)?$/i);
			if (m && fs.statSync(src).isFile()) {
				appc.fs.copyFileSync(src, path.join(this.xcodeAppDir, 'iTunesArtwork' + (m[1] ? m[1].toLowerCase() : '')), {
					logger: this.logger.debug
				});
			}
		}, this);
	}
*/
	next();
};

iOSBuilder.prototype.copyGraphics = function copyGraphics(next) {
	var paths = [
			path.join(this.projectDir, 'Resources', 'iphone'),
			path.join(this.projectDir, 'Resources', 'ios'),
			path.join(this.platformPath, 'resources')
		],
		len = paths.length,
		i, src;

	for (i = 0; i < len; i++) {
		if (fs.existsSync(src = path.join(paths[i], this.tiapp.icon))) {
/*



FIX THIS SHIT



			appc.fs.copyFileSync(src, this.xcodeAppDir, {
				logger: this.logger.debug
			});
*/
			break;
		}
	}

	next();
};

iOSBuilder.prototype.writeBuildManifest = function writeBuildManifest(next) {
	this.cli.createHook('build.ios.writeBuildManifest', this, function (manifest, cb) {
		fs.existsSync(this.buildDir) || wrench.mkdirSyncRecursive(this.buildDir);
		fs.existsSync(this.buildManifestFile) && fs.unlinkSync(this.buildManifestFile);
		fs.writeFile(this.buildManifestFile, JSON.stringify(this.buildManifest = manifest, null, '\t'), cb);
	})(this.currentBuildManifest, next);
};

iOSBuilder.prototype.compileI18NFiles = function compileI18NFiles(next) {
	var data = ti.i18n.load(this.projectDir, this.logger);

	parallel(this,
		Object.keys(data).map(function (lang) {
			return function (done) {
				var contents = [
						'/**',
						' * Appcelerator Titanium',
						' * this is a generated file - DO NOT EDIT',
						' */',
						''
					],
					dir = path.join(this.xcodeAppDir, lang + '.lproj'),
					tasks = [];

				wrench.mkdirSyncRecursive(dir);

				function add(obj, filename, map) {
					obj && tasks.push(function (next) {
						var dest = path.join(dir, filename);
						fs.writeFileSync(dest, contents.concat(Object.keys(obj).map(function (name) {
							return '"' + (map && map[name] || name).replace(/\\"/g, '"').replace(/"/g, '\\"') +
								'" = "' + (''+obj[name]).replace(/%s/g, '%@').replace(/\\"/g, '"').replace(/"/g, '\\"') + '";';
						})).join('\n'));
						if (this.compileI18N) {
							appc.subprocess.run('/usr/bin/plutil', ['-convert', 'binary1', dest], function (code, out, err) {
								next();
							});
						} else {
							next();
						}
					});
				}

				add(data[lang].app, 'InfoPlist.strings', { appname: 'CFBundleDisplayName' });
				add(data[lang].strings, 'Localizable.strings');

				parallel(this, tasks, done);
			};
		}, this),
		next
	);
};

iOSBuilder.prototype.copyLocalizedLaunchScreens = function copyLocalizedLaunchScreens(next) {
	ti.i18n.splashScreens(this.projectDir, this.logger).forEach(function (launchImage) {
		var token = launchImage.split('/'),
			file = token.pop(),
			lang = token.pop(),
			lprojDir = path.join(this.xcodeAppDir, lang + '.lproj'),
			globalFile = path.join(this.xcodeAppDir, file);

		// this would never need to run. But just to be safe
		if (!fs.existsSync(lprojDir)) {
			this.logger.debug(__('Creating lproj folder %s', lprojDir.cyan));
			wrench.mkdirSyncRecursive(lprojDir);
		}

		// check for it in the root of the xcode build folder
		if (fs.existsSync(globalFile)) {
			this.logger.debug(__('Removing File %s, as it is being localized', globalFile.cyan));
			fs.unlinkSync(globalFile);
		}

/*


FIX THIS SHIT



		appc.fs.copyFileSync(launchImage, lprojDir, {
			logger: this.logger.debug
		});
*/
	}, this);
	next();
};

iOSBuilder.prototype.injectModulesAndExtensionsIntoXcodeProject = function injectModulesAndExtensionsIntoXcodeProject(next) {
	function makeUUID() {
		return uuid.v4().toUpperCase().replace(/-/g, '').substring(0, 24);
	}

	var projectFile = path.join(this.buildDir, this.tiapp.name + '.xcodeproj', 'project.pbxproj'),
		projectOrigContents = fs.readFileSync(projectFile).toString(),
		projectContents = projectOrigContents,
		tabs = '\t\t\t\t'; // tabs aren't necessary, just makes it pretty

	if (this.nativeLibModules.length) {
		// we have some libraries to add to the project file
		this.logger.info(__('Injecting native libraries into Xcode project file'));

		var fileMarkers = [],
			fileMarkers2FileRefs = {},
			refMarkers = [],
			frameworkMarkers = [],
			groupMarkers = [],
			groupUUID;

		projectContents.split('\n').forEach(function (line) {
			line.indexOf('/* libtiverify.a */;') !== -1 && fileMarkers.push(line);
			line.indexOf('/* libtiverify.a */ =') !== -1 && refMarkers.push(line);
			line.indexOf('/* libtiverify.a in Frameworks */,') !== -1 && frameworkMarkers.push(line);
			line.indexOf('/* libtiverify.a */,') !== -1 && groupMarkers.push(line);
		});

		fileMarkers.forEach(function (marker) {
			var m = marker.match(/([0-9a-zA-Z]+) \/\*/);
			if (m) {
				fileMarkers2FileRefs[m[1].trim()] = makeUUID();
				!groupUUID && (m = marker.match(/fileRef \= ([0-9a-zA-Z]+) /)) && (groupUUID = m[1]);
			}
		});

		this.nativeLibModules.forEach(function (lib) {
			if (projectContents.indexOf(lib.libName) !== -1) {
				return;
			}

			var newGroupUUID = makeUUID();

			fileMarkers.forEach(function (marker) {
				var begin = projectContents.indexOf(marker),
					end = begin + marker.length,
					m = marker.match(/([0-9a-zA-Z]+) \/\*/),
					newUUID = makeUUID(),
					line = projectContents
						.substring(begin, end)
						.replace(/libtiverify\.a/g, lib.libName)
						.replace(new RegExp(groupUUID, 'g'), newGroupUUID)
						.replace(new RegExp(m[1].trim(), 'g'), newUUID);
				fileMarkers2FileRefs[m[1].trim()] = newUUID;
				projectContents = projectContents.substring(0, end) + '\n' + line + '\n' + projectContents.substring(end + 1);
			});

			refMarkers.forEach(function (marker) {
				var begin = projectContents.indexOf(marker),
					end = begin + marker.length,
					m = marker.match(/([0-9a-zA-Z]+) \/\*/),
					line = projectContents
						.substring(begin, end)
						.replace(/lib\/libtiverify\.a/g, '"' + lib.libFile.replace(/"/g, '\\"') + '"')
						.replace(/libtiverify\.a/g, lib.libName)
						.replace(/SOURCE_ROOT/g, '"<absolute>"')
						.replace(new RegExp(m[1].trim(), 'g'), newGroupUUID);
				projectContents = projectContents.substring(0, end) + '\n' + line + '\n' + projectContents.substring(end + 1);
			});

			groupMarkers.forEach(function (marker) {
				var begin = projectContents.indexOf(marker),
					end = begin + marker.length,
					line = projectContents
						.substring(begin, end)
						.replace(/libtiverify\.a/g, lib.libName)
						.replace(new RegExp(groupUUID, 'g'), newGroupUUID);
				projectContents = projectContents.substring(0, end) + '\n' + line + '\n' + projectContents.substring(end + 1);
			});

			frameworkMarkers.forEach(function (marker) {
				var begin = projectContents.indexOf(marker),
					end = begin + marker.length,
					m = marker.match(/([0-9a-zA-Z]+) \/\*/),
					line = projectContents
						.substring(begin, end)
						.replace(/libtiverify\.a/g, lib.libName)
						.replace(new RegExp(m[1].trim(), 'g'), fileMarkers2FileRefs[m[1].trim()]);
				projectContents = projectContents.substring(0, end) + '\n' + line + '\n' + projectContents.substring(end + 1);
			});

			(function (libPath) {
				var begin = projectContents.indexOf(libPath),
					end, line;
				while (begin !== -1) {
					end = begin + libPath.length;
					line = projectContents.substring(begin, end).replace(libPath, '"\\"' + path.dirname(lib.libFile) + '\\"",');
					projectContents = projectContents.substring(0, end) + '\n                                        ' +  line + '\n' + projectContents.substring(end + 1);
					begin = projectContents.indexOf(libPath, end + line.length);
				}
			}('"\\"$(SRCROOT)/lib\\"",'));
		}, this);
	}

	if (this.builtExtensions.length) {
		this.logger.info(__('Injecting native extensions into Xcode project file'));

		this.builtExtensions.forEach(function (ext) {
			// if the Xcode project already has the extension, then bail
			if (projectContents.indexOf(ext.extensionName) !== -1) {
				return;
			}

			// find locations
			var fileMarkers = [],
				refMarkers = [],
				groupMarkers = [],
				copyFilesBuildPhaseMarkers = [],
				groupUUID,
				newGroupUUID = makeUUID(),
				fileIndex = -1;

			projectContents.split('\n').forEach(function (line) {
				line.indexOf('/* libtiverify.a */;') !== -1 && fileMarkers.push(line);
				line.indexOf('/* libtiverify.a */ =') !== -1 && refMarkers.push(line);
			});

			var groupMatch = projectContents.match(/\* Extensions \*\/ = {[^}]*};/);
			groupMatch && groupMarkers.push(groupMatch[0]);

			var copyFilesBuildPhaseMatch = projectContents.match(/\/\* Embed App Extensions \*\/ = {\s+isa = PBXCopyFilesBuildPhase;[^}]*};/g);
			copyFilesBuildPhaseMatch && copyFilesBuildPhaseMatch.forEach(function(match) {
				copyFilesBuildPhaseMarkers.push(match);
			});

			fileMarkers.forEach(function (marker) {
				var m = marker.match(/([0-9a-zA-Z]+) \/\*/);
				if (m && !groupUUID) {
					m = marker.match(/fileRef \= ([0-9a-zA-Z]+) /);
					if (m) {
						groupUUID = m[1];
					}
				}
			});

			// inject files
			fileMarkers.forEach(function (marker) {
				fileIndex++;
				if (fileIndex >= copyFilesBuildPhaseMarkers.length) {
					this.logger.error(__('Error injecting extension into Xcode project (BuildPhase markers not found or invalid)') + '\n');
					process.exit(1);
				}

				// add file references for the extension in Resources
				var begin = projectContents.indexOf(marker),
					end = begin + marker.length,
					m = marker.match(/([0-9a-zA-Z]+) \/\*/),
					newUUID = makeUUID(),
					line = projectContents
						.substring(begin, end)
						.replace(/libtiverify\.a/g, ext.extensionName)
						.replace(/in Frameworks/g, 'in Resources')
						.replace(new RegExp(groupUUID, 'g'), newGroupUUID)
						.replace(new RegExp(m[1].trim(), 'g'), newUUID);
				projectContents = projectContents.substring(0, end) + '\n' + line + '\n' + projectContents.substring(end + 1);

				// add file references for the extension in Embed App Extensions Build Phase
				var begin = projectContents.indexOf(marker),
					end = begin + marker.length,
					m = marker.match(/([0-9a-zA-Z]+) \/\*/),
					newUUID = makeUUID(),
					line = projectContents
						.substring(begin, end)
						.replace(/libtiverify\.a/g, ext.extensionName)
						.replace(/in Frameworks/g, 'in Embed App Extensions')
						.replace(new RegExp(groupUUID, 'g'), newGroupUUID)
						.replace(new RegExp(m[1].trim(), 'g'), newUUID)
						.replace(new RegExp(ext.extensionName + ' \\*/;', 'g'), ext.extensionName + ' */; settings = {ATTRIBUTES = (RemoveHeadersOnCopy, ); };');
				projectContents = projectContents.substring(0, end) + '\n' + line + '\n' + projectContents.substring(end + 1);

				var buildMarker = copyFilesBuildPhaseMarkers[fileIndex];
				addFileToBuildPhase(buildMarker, newUUID, 'Embed App Extensions');
			}, this);

			function addFileToBuildPhase(marker, uuid, inWhere) {
				var begin = projectContents.indexOf(marker),
					end = begin + marker.length,
					line = projectContents
						.substring(begin, end)
						.replace(/files = \(/g, 'files = (\n' + tabs + uuid + ' /* ' + ext.extensionName + ' in ' + inWhere + ' */,');
				projectContents = projectContents.substring(0, begin) + line + '\n' + projectContents.substring(end + 1);
			}

			refMarkers.forEach(function (marker) {
				var begin = projectContents.indexOf(marker),
					end = begin + marker.length,
					m = marker.match(/([0-9a-zA-Z]+) \/\*/),
					line = projectContents
						.substring(begin, end)
						.replace(/archive.ar/g, '\"wrapper.app-extension\"')
						.replace(/lib\/libtiverify\.a/g, '"' + ext.extensionFile.replace(/"/g, '\\"') + '"')
						.replace(/name = libtiverify.a/g, 'name = "' + ext.extensionName+ '"') // File names with spaces need quotes
						.replace(/libtiverify\.a/g, ext.extensionName)
						.replace(/SOURCE_ROOT/g, '"<group>"')
						.replace(new RegExp(m[1].trim(), 'g'), newGroupUUID);
				projectContents = projectContents.substring(0, end) + '\n' + line + '\n' + projectContents.substring(end + 1);
			});

			groupMarkers.forEach(function (marker) {
				var begin = projectContents.indexOf(marker),
					end = begin + marker.length,
					line = projectContents
						.substring(begin, end)
						.replace(/children = \(/g, 'children = (\n' + tabs + newGroupUUID + ' /* ' + ext.extensionName + ' */,');
				projectContents = projectContents.substring(0, begin) + line + '\n' + projectContents.substring(end + 1);
			});
		}, this);
	}

	if (projectContents !== projectOrigContents) {
		this.logger.debug(__('Writing %s', projectFile.cyan));
		fs.writeFileSync(projectFile, projectContents);
	}

	next();
};

iOSBuilder.prototype.compileJSSFiles = function compileJSSFiles(next) {
	ti.jss.load(path.join(this.projectDir, 'Resources'), this.deviceFamilyNames[this.deviceFamily], this.logger, function (results) {
		var appStylesheet = path.join(this.xcodeAppDir, 'stylesheet.plist'),
			plist = new appc.plist();
		appc.util.mix(plist, results);
		fs.writeFile(appStylesheet, plist.toString('xml'), function () {
			if (this.compileJSS) {
				// compile plist into binary format so it's faster to load, we can be slow on simulator
				appc.subprocess.run('/usr/bin/plutil', ['-convert', 'binary1', appStylesheet], function (code, out, err) {
					next();
				});
			} else {
				next();
			}
		}.bind(this));
	}.bind(this));
};

iOSBuilder.prototype.invokeXcodeBuild = function invokeXcodeBuild(next) {
	this.logger.info(__('Invoking xcodebuild'));

	var xcodeArgs = [
			'-target', this.tiapp.name + this.xcodeTargetSuffixes[this.deviceFamily],
			'-configuration', this.xcodeTarget,
			'-sdk', this.xcodeTargetOS,
			'IPHONEOS_DEPLOYMENT_TARGET=' + appc.version.format(this.minIosVer, 2),
			'TARGETED_DEVICE_FAMILY=' + this.deviceFamilies[this.deviceFamily],
			'ONLY_ACTIVE_ARCH=NO',
			'DEAD_CODE_STRIPPING=YES'
		],
		gccDefs = [ 'DEPLOYTYPE=' + this.deployType ];

	if (this.target === 'simulator') {
		gccDefs.push('__LOG__ID__=' + this.tiapp.guid);
		gccDefs.push('DEBUG=1');
		gccDefs.push('TI_VERSION=' + this.titaniumSdkVersion);
	}

	if (/simulator|device|dist\-adhoc/.test(this.target)) {
		this.tiapp.ios && this.tiapp.ios.enablecoverage && gccDefs.push('KROLL_COVERAGE=1');
	}

	xcodeArgs.push('GCC_PREPROCESSOR_DEFINITIONS=' + gccDefs.join(' '));

	if (/device|dist\-appstore|dist\-adhoc/.test(this.target)) {
		xcodeArgs.push('PROVISIONING_PROFILE=' + this.provisioningProfileUUID);
		xcodeArgs.push('DEPLOYMENT_POSTPROCESSING=YES');
		if (this.keychain) {
			xcodeArgs.push('OTHER_CODE_SIGN_FLAGS=--keychain ' + this.keychain);
		}
	}

	var keychains = this.iosInfo.certs.keychains;

	if (this.target === 'device') {
		Object.keys(keychains).some(function (keychain) {
			return (keychains[keychain].developer || []).some(function (d) {
				if (!d.invalid && d.name === this.certDeveloperName) {
					xcodeArgs.push('CODE_SIGN_IDENTITY=' + d.fullname);
					return true;
				}
			}, this);
		}, this);
	}

	if (/dist-appstore|dist\-adhoc/.test(this.target)) {
		Object.keys(keychains).some(function (keychain) {
			return (keychains[keychain].developer || []).some(function (d) {
				if (!d.invalid && d.name === this.certDistributionName) {
					xcodeArgs.push('CODE_SIGN_IDENTITY=' + d.fullname);
					return true;
				}
			}, this);
		}, this);
	}

	var xcodebuildHook = this.cli.createHook('build.ios.xcodebuild', this, function (exe, args, opts, done) {
			this.logger.debug(__('Invoking: %s', ('DEVELOPER_DIR=' + this.xcodeEnv.path + ' ' + exe + ' ' + args.map(function (a) { return a.indexOf(' ') !== -1 ? '"' + a + '"' : a; }).join(' ')).cyan));

			var p = spawn(exe, args, opts),
				out = [],
				err = [],
				stopOutputting = false;

			p.stdout.on('data', function (data) {
				data.toString().split('\n').forEach(function (line) {
					if (line.length) {
						out.push(line);
						if (line.indexOf('Failed to minify') !== -1) {
							stopOutputting = true;
						}
						if (!stopOutputting) {
							this.logger.trace(line);
						}
					}
				}, this);
			}.bind(this));

			p.stderr.on('data', function (data) {
				data.toString().split('\n').forEach(function (line) {
					if (line.length) {
						err.push(line);
					}
				}, this);
			}.bind(this));

			p.on('close', function (code, signal) {
				if (code) {
					// first see if we errored due to a dependency issue
					if (err.join('\n').indexOf('Check dependencies') !== -1) {
						var len = out.length;
						for (var i = len - 1; i >= 0; i--) {
							if (out[i].indexOf('Check dependencies') !== -1) {
								if (out[out.length - 1].indexOf('Command /bin/sh failed with exit code') !== -1) {
									len--;
								}
								for (var j = i + 1; j < len; j++) {
									this.logger.error(__('Error details: %s', out[j]));
								}
								this.logger.log();
								process.exit(1);
							}
						}
					}

					// next see if it was a minification issue
					var len = out.length;
					for (var i = len - 1, k = 0; i >= 0 && k < 10; i--, k++) {
						if (out[i].indexOf('Failed to minify') !== -1) {
							if (out[out.length - 1].indexOf('Command /bin/sh failed with exit code') !== -1) {
								len--;
							}
							while (i < len) {
								this.logger.log(out[i++]);
							}
							this.logger.log();
							process.exit(1);
						}
					}

					// just print the entire error buffer
					err.forEach(function (line) {
						this.logger.error(line);
					}, this);
					this.logger.log();
					process.exit(1);
				}

				// end of the line
				done(code);
			}.bind(this));
		});

	xcodebuildHook(
		this.xcodeEnv.executables.xcodebuild,
		xcodeArgs,
		{
			cwd: this.buildDir,
			env: {
				DEVELOPER_DIR: this.xcodeEnv.path,
				TMPDIR: process.env.TMPDIR,
				HOME: process.env.HOME,
				PATH: process.env.PATH,
				TITANIUM_CLI_XCODEBUILD: 'Enjoy hacking? http://jobs.appcelerator.com/',
				TITANIUM_CLI_IMAGES_OPTIMIZED: this.target === 'simulator' ? '' : this.imagesOptimizedFile
			}
		},
		next
	);
};

iOSBuilder.prototype.xcodePrecompilePhase = function xcodePrecompilePhase(finished) {
	this.logger.info(__('Initiating Xcode pre-compile phase'));

	series(this, [
		'copyResources',
		'processTiSymbols',
		'writeDebugProfilePlists',
		'compileJSSFiles',
		'compileI18NFiles',
		'copyLocalizedLaunchScreens',
		function (next) {
			// if not production and running from Xcode
			if (this.deployType !== 'production') {
				var appDefaultsFile = path.join(this.buildDir, 'Classes', 'ApplicationDefaults.m');
				fs.writeFileSync(appDefaultsFile, fs.readFileSync(appDefaultsFile).toString().replace(/return \[NSDictionary dictionaryWithObjectsAndKeys\:\[TiUtils stringValue\:@".+"\], @"application-launch-url", nil];/, 'return nil;'));
			}
			next();
		}
	], function () {
		finished();
	});
};

iOSBuilder.prototype.writeDebugProfilePlists = function writeDebugProfilePlists(next) {
	function processPlist(filename, host) {
		var dest = path.join(this.xcodeAppDir, filename),
			parts = (host || '').split(':');

		fs.writeFileSync(dest, ejs.render(fs.readFileSync(path.join(this.templatesDir, filename)).toString(), {
			host: parts.length > 0 ? parts[0] : '',
			port: parts.length > 1 ? parts[1] : '',
			airkey: parts.length > 2 ? parts[2] : '',
			hosts: parts.length > 3 ? parts[3] : ''
		}));
	}

	processPlist.call(this, 'debugger.plist', this.debugHost);
	processPlist.call(this, 'profiler.plist', this.profilerHost);

	next();
};

iOSBuilder.prototype.copyResources = function copyResources(finished) {
	var ignoreDirs = this.ignoreDirs,
		ignoreFiles = this.ignoreFiles,
		extRegExp = /\.(\w+)$/,
		icon = (this.tiapp.icon || 'appicon.png').match(/^(.*)\.(.+)$/),
		unsymlinkableFileRegExp = new RegExp("^Default.*\.png|.+\.(otf|ttf)|iTunesArtwork" + (icon ? '|' + icon[1].replace(/\./g, '\\.') + '.*\\.' + icon[2] : '') + "$"),
		jsFiles = {},
		jsFilesToEncrypt = this.jsFilesToEncrypt = [],
		htmlJsFiles = this.htmlJsFiles = {},
		symlinkFiles = this.target === 'simulator' && this.config.get('ios.symlinkResources', true) && !this.forceCopy && !this.forceCopyAll,
		_t = this;

	function copyDir(opts, callback) {
		if (opts && opts.src && fs.existsSync(opts.src) && opts.dest) {
			opts.origSrc = opts.src;
			opts.origDest = opts.dest;
			recursivelyCopy.call(this, opts.src, opts.dest, opts.ignoreRootDirs, opts, callback);
		} else {
			callback();
		}
	}

	function copyFile(from, to, next) {
		var d = path.dirname(to);
		fs.existsSync(d) || wrench.mkdirSyncRecursive(d);
		if (symlinkFiles && !unsymlinkableFileRegExp.test(path.basename(to))) {
			fs.existsSync(to) && fs.unlinkSync(to);
			this.logger.debug(__('Symlinking %s => %s', from.cyan, to.cyan));
			if (next) {
				fs.symlink(from, to, next);
			} else {
				fs.symlinkSync(from, to);
			}
		} else {
			this.logger.debug(__('Copying %s => %s', from.cyan, to.cyan));
			if (next) {
				fs.readFile(from, function (err, data) {
					if (err) throw err;
					fs.writeFile(to, data, next);
				});
			} else {
				fs.writeFileSync(to, fs.readFileSync(from));
			}
		}
	}

	function recursivelyCopy(src, dest, ignoreRootDirs, opts, done) {
		var files;
		if (fs.statSync(src).isDirectory()) {
			files = fs.readdirSync(src);
		} else {
			// we have a file, so fake a directory listing
			files = [ path.basename(src) ];
			src = path.dirname(src);
		}

		async.whilst(
			function () {
				return files.length;
			},

			function (next) {
				var filename = files.shift(),
					from = path.join(src, filename),
					to = path.join(dest, filename);

				// check that the file actually exists and isn't a broken symlink
				if (!fs.existsSync(from)) return next();

				var isDir = fs.statSync(from).isDirectory();

				// check if we are ignoring this file
				if ((isDir && ignoreRootDirs && ignoreRootDirs.indexOf(filename) !== -1) || (isDir ? ignoreDirs : ignoreFiles).test(filename)) {
					_t.logger.debug(__('Ignoring %s', from.cyan));
					return next();
				}

				// if this is a directory, recurse
				if (isDir) return recursivelyCopy.call(_t, from, path.join(dest, filename), null, opts, next);

				// we have a file, now we need to see what sort of file

				// if the destination directory does not exists, create it
				fs.existsSync(dest) || wrench.mkdirSyncRecursive(dest);

				var ext = filename.match(extRegExp),
					relPath = to.replace(opts.origDest, '').replace(/^\//, '');

				switch (ext && ext[1]) {
					case 'css':
						// if we encounter a css file, check if we should minify it
						if (_t.minifyCSS) {
							_t.logger.debug(__('Copying and minifying %s => %s', from.cyan, to.cyan));
							fs.readFile(from, function (err, data) {
								if (err) throw err;
								fs.writeFile(to, cleanCSS.process(data.toString()), next);
							});
						} else {
							copyFile.call(_t, from, to, next);
						}
						break;

					case 'html':
						// find all js files referenced in this html file
						var relPath = from.replace(opts.origSrc, '').replace(/\\/g, '/').replace(/^\//, '').split('/');
						relPath.pop(); // remove the filename
						relPath = relPath.join('/');
						jsanalyze.analyzeHtmlFile(from, relPath).forEach(function (file) {
							htmlJsFiles[file] = 1;
						});

						_t.cli.createHook('build.ios.copyResource', _t, function (from, to, cb) {
							copyFile.call(_t, from, to, cb);
						})(from, to, next);
						break;

					case 'js':
						// track each js file so we can copy/minify later

						// we use the destination file name minus the path to the assets dir as the id
						// which will eliminate dupes
						var id = to.replace(opts.origDest, '').replace(/^\//, '');
						if (!jsFiles[relPath] || !opts || !opts.onJsConflict || opts.onJsConflict(from, to, relPath)) {
							jsFiles[relPath] = from;
						}

						next();
						break;

					case 'jss':
						// ignore, these will be compiled later by compileJSS()
						next();
						break;

					default:
						// if the device family is iphone, then don't copy iPad specific images
						if (_t.deviceFamily !== 'iphone' || _t.ipadLaunchImages.indexOf(relPath) === -1) {
							// normal file, just copy it into the build/iphone/bin/assets directory
							_t.cli.createHook('build.ios.copyResource', _t, function (from, to, cb) {
								copyFile.call(_t, from, to, cb);
							})(from, to, next);
						} else {
							next();
						}
				}
			},

			done
		);
	}

	var tasks = [
		// first task is to copy all files in the Resources directory, but ignore
		// any directory that is the name of a known platform
		function (cb) {
			copyDir.call(this, {
				src: path.join(this.projectDir, 'Resources'),
				dest: this.xcodeAppDir,
				ignoreRootDirs: ti.availablePlatformsNames
			}, cb);
		},

		// next copy all files from the iOS specific Resources directory
		function (cb) {
			copyDir.call(this, {
				src: path.join(this.projectDir, 'Resources', 'iphone'),
				dest: this.xcodeAppDir
			}, cb);
		},

		function (cb) {
			copyDir.call(this, {
				src: path.join(this.projectDir, 'Resources', 'ios'),
				dest: this.xcodeAppDir
			}, cb);
		}
	];

	// copy all commonjs modules
	this.commonJsModules.forEach(function (module) {
		// copy the main module
		tasks.push(function (cb) {
			copyDir.call(this, {
				src: module.libFile,
				dest: this.xcodeAppDir,
				onJsConflict: function (src, dest, id) {
					this.logger.error(__('There is a project resource "%s" that conflicts with a CommonJS module', id));
					this.logger.error(__('Please rename the file, then rebuild') + '\n');
					process.exit(1);
				}.bind(this)
			}, cb);
		});
	});

	// copy all module assets
	this.modules.forEach(function (module) {
		// copy the assets
		tasks.push(function (cb) {
			copyDir.call(this, {
				src: path.join(module.modulePath, 'assets'),
				dest: path.join(this.xcodeAppDir, 'modules', module.id.toLowerCase())
			}, cb);
		});
	});

	var platformPaths = [
		path.join(this.projectDir, this.cli.argv['platform-dir'] || 'platform', 'iphone'),
		path.join(this.projectDir, this.cli.argv['platform-dir'] || 'platform', 'ios')
	];
	// WARNING! This is pretty dangerous, but yes, we're intentionally copying
	// every file from platform/iphone|ios and all modules into the build dir
	this.modules.forEach(function (module) {
		platformPaths.push(
			path.join(module.modulePath, 'platform', 'iphone'),
			path.join(module.modulePath, 'platform', 'ios')
		);
	});
	platformPaths.forEach(function (dir) {
		if (fs.existsSync(dir)) {
			tasks.push(function (cb) {
				copyDir.call(this, {
					src: dir,
					dest: this.xcodeAppDir
				}, cb);
			});
		}
	}, this);

	series(this, tasks, function (err, results) {
		// copy js files into assets directory and minify if needed
		this.logger.info(__('Processing JavaScript files'));

		series(this, Object.keys(jsFiles).map(function (id) {
			return function (done) {
				var from = jsFiles[id],
					to = path.join(this.xcodeAppDir, id);

				if (htmlJsFiles[id]) {
					// this js file is referenced from an html file, so don't minify or encrypt
					return copyFile.call(this, from, to, done);
				}

				// we have a js file that may be minified or encrypted
				id = id.replace(/\./g, '_');

				// if we're encrypting the JavaScript, copy the files to the assets dir
				// for processing later
				if (this.encryptJS) {
					to = path.join(this.buildAssetsDir, id);
					jsFilesToEncrypt.push(id);
				}

				try {
					this.cli.createHook('build.ios.copyResource', this, function (from, to, cb) {
						// parse the AST
						var r = jsanalyze.analyzeJsFile(from, { minify: this.minifyJS });

						// we want to sort by the "to" filename so that we correctly handle file overwriting
						this.tiSymbols[to] = r.symbols;

						var dir = path.dirname(to);
						fs.existsSync(dir) || wrench.mkdirSyncRecursive(dir);

						if (this.minifyJS) {
							this.logger.debug(__('Copying and minifying %s => %s', from.cyan, to.cyan));

							this.cli.createHook('build.ios.compileJsFile', this, function (r, from, to, cb2) {
								fs.writeFile(to, r.contents, cb2);
							})(r, from, to, cb);
						} else if (symlinkFiles) {
							copyFile.call(this, from, to, cb);
						} else {
							// we've already read in the file, so just write the original contents
							this.logger.debug(__('Copying %s => %s', from.cyan, to.cyan));
							fs.writeFile(to, r.contents, cb);
						}
					})(from, to, done);
				} catch (ex) {
					ex.message.split('\n').forEach(this.logger.error);
					this.logger.log();
					process.exit(1);
				}
			};
		}), function () {
			// write the properties file
			var appPropsFile = this.encryptJS ? path.join(this.buildAssetsDir, '_app_props__json') : path.join(this.xcodeAppDir, '_app_props_.json'),
				props = {};
			this.tiapp.properties && Object.keys(this.tiapp.properties).forEach(function (prop) {
				props[prop] = this.tiapp.properties[prop].value;
			}, this);
			fs.writeFileSync(
				appPropsFile,
				JSON.stringify(props)
			);
			this.encryptJS && jsFilesToEncrypt.push('_app_props__json');

			if (!jsFilesToEncrypt.length) {
				// nothing to encrypt, continue
				return finished();
			}

			this.cli.fireHook('build.ios.prerouting', this, function (err) {
				var titaniumPrepHook = this.cli.createHook('build.ios.titaniumprep', this, function (exe, args, opts, done) {
						var tries = 0,
							completed = false;

						this.logger.info('Encrypting JavaScript files: %s', (exe + ' "' + args.slice(0, -1).join('" "') + '"').cyan);
						jsFilesToEncrypt.forEach(function (file) {
							this.logger.debug(__('Preparing %s', file.cyan));
						}, this);

						async.whilst(
							function () {
								if (tries > 3) {
									// we failed 3 times, so just give up
									this.logger.error(__('titanium_prep failed to complete successfully'));
									this.logger.error(__('Try cleaning this project and build again') + '\n');
									process.exit(1);
								}
								return !completed;
							},
							function (cb) {
								var child = spawn(exe, args, opts),
									out = '';

								child.stdin.write(jsFilesToEncrypt.join('\n'));
								child.stdin.end();

								child.stdout.on('data', function (data) {
									out += data.toString();
								});

								child.on('close', function (code) {
									if (code) {
										this.logger.error(__('titanium_prep failed to run (%s)', code) + '\n');
										process.exit(1);
									}

									if (out.indexOf('initWithObjectsAndKeys') !== -1) {
										// success!
										var file = path.join(this.buildDir, 'Classes', 'ApplicationRouting.m');
										this.logger.debug(__('Writing application routing source file: %s', file.cyan));
										fs.writeFileSync(
											file,
											ejs.render(fs.readFileSync(path.join(this.templatesDir, 'ApplicationRouting.m')).toString(), {
												bytes: out
											})
										);

										// since we just modified the ApplicationRouting.m, we need to force xcodebuild
										this.forceRebuild = true;

										completed = true;
									} else {
										// failure, maybe it was a fluke, try again
										this.logger.warn(__('titanium_prep failed to complete successfully, trying again'));
										tries++;
									}

									cb();
								}.bind(this));
							}.bind(this),
							done
						);
					});

				titaniumPrepHook(
					path.join(this.platformPath, 'titanium_prep'),
					[ this.tiapp.id, this.buildAssetsDir, this.tiapp.guid ],
					{},
					finished
				);
			}.bind(this));
		});
	});
};

iOSBuilder.prototype.processTiSymbols = function processTiSymbols(finished) {
	var namespaces = {
			'analytics': 1,
			'api': 1,
			'network': 1,
			'platform': 1,
			'ui': 1
		},
		symbols = {};

	// generate the default symbols
	Object.keys(namespaces).forEach(function (ns) {
		symbols[ns.toUpperCase()] = 1;
	});

	function addSymbol(symbol) {
		var parts = symbol.replace(/^(Ti|Titanium)./, '').split('.');
		if (parts.length) {
			namespaces[parts[0].toLowerCase()] = 1;
			while (parts.length) {
				symbols[parts.join('.').replace(/\.create/gi, '').replace(/\./g, '').replace(/\-/g, '_').toUpperCase()] = 1;
				parts.pop();
			}
		}
	}

	// add the symbols we found
	Object.keys(this.tiSymbols).forEach(function (file) {
		this.tiSymbols[file].forEach(addSymbol);
	}, this);

	// for each module, if it has a metadata.json file, add its symbols
	this.modules.forEach(function (m) {
		var file = path.join(m.modulePath, 'metadata.json');
		if (fs.existsSync(file)) {
			try {
				var metadata = JSON.parse(fs.readFileSync(file));
				if (metadata && typeof metadata === 'object' && Array.isArray(metadata.exports)) {
					metadata.exports.forEach(addSymbol);
				}
			} catch (e) {}
		}
	});

	// for each Titanium namespace, copy any resources
	this.logger.info(__('Processing Titanium namespace resources'));
	Object.keys(namespaces).forEach(function (ns) {
		var src = path.join(this.platformPath, 'modules', ns, 'images');
		if (fs.existsSync(src)) {
			this.copyDirSync(src, path.join(this.xcodeAppDir, 'modules', ns, 'images'));
		}
	}, this);

	//This is default behavior for now. Move this to true in phase 2.
	//Remove this logic when we have debugging/profiling support with JSCore framework
	//TIMOB-17892
	var useJSCore = false;
	if (this.cli.tiapp.ios && this.cli.tiapp.ios['use-jscore-framework']){
		useJSCore = true;
	}

	if (this.debugHost || this.profilerHost) {
		useJSCore = false;
	}

	var dest = path.join(this.buildDir, 'Classes', 'defines.h');

	// if we're doing a simulator build or we're including all titanium modules,
	// return now since we don't care about writing the defines.h
	if (this.target === 'simulator' || this.includeAllTiModules) {
		// BEGIN TIMOB-17892 changes
		if (this.useJSCore) {
			this.logger.debug(__('Using JavaScriptCore Framework'));
			fs.writeFileSync(
				dest,
				fs.readFileSync(path.join(this.platformPath, 'Classes', 'defines.h')).toString() + '\n#define USE_JSCORE_FRAMEWORK'
			);
		}
		// END TIMOB-17892 changes
		return finished();
	}

	// build the defines.h file
	var contents = [
			'// Warning: this is generated file. Do not modify!',
			'',
			'#define TI_VERSION ' + this.titaniumSdkVersion
		];

	contents = contents.concat(Object.keys(symbols).sort().map(function (s) {
		return '#define USE_TI_' + s;
	}));

	var infoPlist = this.infoPlist;
	if (!infoPlist) {
		infoPlist = new appc.plist(this.buildDir + '/Info.plist');
	}

	if (Array.isArray(infoPlist.UIBackgroundModes) && infoPlist.UIBackgroundModes.indexOf('remote-notification') !== -1) {
		contents.push('#define USE_TI_SILENTPUSH');
	}
	if (Array.isArray(infoPlist.UIBackgroundModes) && infoPlist.UIBackgroundModes.indexOf('fetch') !== -1) {
		contents.push('#define USE_TI_FETCH');
	}

	contents.push(
		'#ifdef USE_TI_UILISTVIEW',
		'#define USE_TI_UILABEL',
		'#define USE_TI_UIBUTTON',
		'#define USE_TI_UIIMAGEVIEW',
		'#define USE_TI_UIPROGRESSBAR',
		'#define USE_TI_UIACTIVITYINDICATOR',
		'#define USE_TI_UISWITCH',
		'#define USE_TI_UISLIDER',
		'#define USE_TI_UITEXTFIELD',
		'#define USE_TI_UITEXTAREA',
		'#endif'
	);
	// BEGIN TIMOB-17892 changes
	if (this.useJSCore) {
		this.logger.debug(__('Using JavaScriptCore Framework'));
		contents.push('#define USE_JSCORE_FRAMEWORK')
	}
	// END TIMOB-17892 changes

	contents = contents.join('\n');

	if (!fs.existsSync(dest) || fs.readFileSync(dest).toString() !== contents) {
		this.logger.debug(__('Writing Titanium symbol file: %s', dest.cyan));
		fs.writeFileSync(dest, contents);
	} else {
		this.logger.debug(__('Titanium symbol file already up-to-date: %s', dest.cyan));
	}

	finished();
};

iOSBuilder.prototype.optimizeImages = function optimizeImages(next) {
	// if we're doing a simulator build, return now since we don't care about optimizing images
	if (this.target === 'simulator') {
		return next();
	}

	var tool = path.join(this.xcodeEnv.path, 'Platforms', 'iPhoneOS.platform', 'Developer', 'usr', 'bin', 'iphoneos-optimize');
	if (fs.existsSync(tool)) {
		this.logger.info(__('Optimizing all images in %s', this.xcodeAppDir.cyan));
		appc.subprocess.run(tool, this.xcodeAppDir, function (code, out, err) {
			// remove empty directories
			this.logger.debug(__('Removing empty directories'));
			appc.subprocess.run('find', ['.', '-type', 'd', '-empty', '-delete'], {
				cwd: this.xcodeAppDir
			}, function (code, out, err) {
				this.logger.info(__('Image optimization complete'));
				appc.fs.touch(this.imagesOptimizedFile);
				next();
			}.bind(this));
		}.bind(this));
	} else {
		this.logger.warn(__('Unable to find iphoneos-optimize, skipping image optimization'));
		appc.fs.touch(this.imagesOptimizedFile);
		next();
	}
};

// create the builder instance and expose the public api
(function (iosBuilder) {
	exports.config   = iosBuilder.config.bind(iosBuilder);
	exports.validate = iosBuilder.validate.bind(iosBuilder);
	exports.run      = iosBuilder.run.bind(iosBuilder);
}(new iOSBuilder(module)));
