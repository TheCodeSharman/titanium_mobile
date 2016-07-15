/*
 * Appcelerator Titanium Mobile
 * Copyright (c) 2011-2016 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
var should = require('./utilities/assertions'),
	utilities = require('./utilities/utilities'),
	didFocus = false,
	didPostlayout = false;

function createWindow(_args) {
	_args = _args || {};
	_args.backgroundColor = _args.backgroundColor || 'red';
	return Ti.UI.createWindow(_args);
}

describe('Titanium.UI.Layout', function () {
	this.timeout(5000);
	var win;

	beforeEach(function() {
		didFocus = false;
		didPostlayout = false;
	});

	afterEach(function() {
		if (win) {
			win.close();
		}
		win = null;
	});

	// functional test cases #1010, #1011, #1025, #1025a
	// rect and size properties should not be undefined
	// FIXME Get working on iOS and Android. They don't currently fire postlayout event for Ti.UI.Window
	((utilities.isIOS() || utilities.isAndroid()) ? it.skip : it)('viewSizeAndRectPx', function (finish) {
		win = createWindow();
		var view = Ti.UI.createView(),
			label = Ti.UI.createLabel({
				text: 'a',
				font: {
					fontSize: 14,
					fontFamily: 'monospace'
				}
			});

		win.add(view);
		win.add(label);
		win.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				Ti.API.info('Got postlayout');
				should(view.size).not.be.undefined;
				should(view.size.width).not.be.undefined;
				should(view.size.height).not.be.undefined;
				should(view.size.x).not.be.undefined;
				should(view.size.y).not.be.undefined;
				should(view.rect).not.be.undefined;
				should(view.rect.width).not.be.undefined;
				should(view.rect.height).not.be.undefined;
				should(view.rect.x).not.be.undefined;
				should(view.rect.y).not.be.undefined;
				//size and rect properties return the same width and height
				should(view.size.width).eql(view.rect.width);
				should(view.size.height).eql(view.rect.height);
				//size property returns 0 for x and y
				should(view.size.x).eql(0);
				should(view.size.y).eql(0);
				//Functonal test case 1025
				should(view.top).be.undefined;
				should(view.bottom).be.undefined;
				should(view.left).be.undefined;
				should(view.right).be.undefined;
				should(view.center).be.undefined;
				should(view.zIndex).be.undefined;
				//Functonal test case 1025a
				should(label.top).be.undefined;
				should(label.bottom).be.undefined;
				should(label.left).be.undefined;
				should(label.right).be.undefined;
				should(label.center).be.undefined;
				should(label.zIndex).be.undefined;
				//FILL behavior
				should(view.rect.x).eql(0);
				should(view.rect.y).eql(0);
				should(win.size.height / view.size.height).eql(1);
				should(win.size.width / view.size.width).eql(1);

				finish();
			} catch (e) {
				finish(e);
			}
		});
		win.open();
	});

	// functional test cases #1012, #1014:
	// ViewLeft and ViewRight
	// FIXME Get working on iOS and Android. They need to fire Ti.UI.Window postlayout events!
	((utilities.isIOS() || utilities.isAndroid()) ? it.skip : it)('viewLeft', function (finish) {
		win = createWindow();
		var view = Ti.UI.createView({
				left: 10,
				width: 10
			}),
			view2 = Ti.UI.createView({
				right: 10,
				width: 10
			});

		win.add(view);
		win.add(view2);
		win.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				should(view.left).eql(10);
				should(view.rect.x).eql(10);
				should(view.rect.width).eql(10);
				should(view.right).be.undefined;
				should(view2.right).eql(10);
				should(view2.rect.x).eql(win.size.width - 20);
				should(view2.rect.width).eql(10);
				should(view2.left).be.undefined;

				finish();
			} catch (e) {
				finish(e);
			}
		});
		win.open();
	});

	// functional test case #1016, #1018
	// ViewTop and ViewBottom
	// FIXME Get working on iOS and Android. They don't currently fire postlayout event for Ti.UI.Window
	((utilities.isIOS() || utilities.isAndroid()) ? it.skip : it)('viewTop', function (finish) {
		win = createWindow();
		var view = Ti.UI.createView({
				top: 10,
				height: 10
			}),
			view2 = Ti.UI.createView({
				bottom: 10,
				height: 10
			});

		win.add(view);
		win.add(view2);
		win.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				should(view.top).eql(10);
				should(view.rect.y).eql(10);
				should(view.rect.height).eql(10);
				should(view.bottom).be.undefined;
				should(view2.bottom).eql(10);
				should(view2.rect.y).eql(win.size.height - 20);
				should(view2.rect.height).eql(10);
				should(view2.top).be.undefined;

				finish();
			} catch (e) {
				finish(e);
			}
		});
		win.open();
	});

	// functional test case #1020: ViewCenter
	it.skip('viewCenter', function (finish) {
		win = createWindow();
		var view = Ti.UI.createView({
			center: {
				x: 50,
				y: 50
			},
			height: 40,
			width: 40
		});

		win.add(view);
		view.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				should(view.center.x).eql(50);
				should(view.center.y).eql(50);
				should(view.rect.x).eql(30);
				should(view.rect.y).eql(30);

				finish();
			} catch (e) {
				finish(e);
			}
		});
		win.open();
	});

	// functional test case #1022, #1024
	// ViewWidth, ViewHeight
	// FIXME Get working on iOS and Android. They need to fire Ti.UI.Window postlayout events!
	(((utilities.isWindows10() && utilities.isWindowsDesktop()) || (utilities.isIOS() || utilities.isAndroid())) ? it.skip : it)('viewWidth', function (finish) {
		win = createWindow();
		var view = Ti.UI.createView({
			width: 10,
			height: 10
		});
		win.add(view);
		win.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				should(view.width).eql(10);
				should(view.size.width).eql(10);
				should(view.height).eql(10);
				should(view.size.height).eql(10);
				should(view.left).be.undefined;
				should(view.right).be.undefined;
				should(view.top).be.undefined;
				should(view.bottom).be.undefined;
				//Centered View with width and height defined
				// FIXME There's nothing to indicate that x/y should be integers, but this test assumed they were, so I had to rewrite to wrap them in Math.floor
				should(view.rect.x).eql(Math.floor((win.size.width - view.size.width) / 2));
				should(view.rect.y).eql(Math.floor((win.size.height - view.size.height) / 2));
				//should(Math.floor(view.rect.x)).eql(Math.floor((win.size.width - view.size.width) / 2));
				//should(Math.floor(view.rect.y)).eql(Math.floor((win.size.height - view.size.height) / 2));

				finish();
			} catch (e) {
				finish(e);
			}
		});
		win.open();
	});

	// functional test #1026 ViewError
	it.skip('viewError', function (finish) {
		win = createWindow();
		var view = Ti.UI.createView({
			backgroundColor: 'green',
			left: 'leftString',
			right: 'rightString',
			top: 'topString',
			bottom: 'bottomString',
			width: 'widthString',
			height: 'heightString',
			center: {
				x: 'centerXString',
				y: 'centerYString'
			}
		});
		win.add(view);
		view.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				should(view.left).eql('leftString');
				should(view.right).eql('rightString');
				should(view.top).eql('topString');
				should(view.bottom).eql('bottomString');
				should(view.center.y).eql('centerYString');
				should(view.center.x).eql('centerXString');
				should(view.width).eql('widthString');
				should(view.height).eql('heightString');

				finish();
			} catch (e) {
				finish(e);
			}
		});
		win.open();
	});

	// functional test #1033, 1033a, 1033b
	// UndefinedWidth Implicit calculations
	// FIXME Get working on iOS and Android. They don't currently fire postlayout event for Ti.UI.Window
	((utilities.isIOS() || utilities.isAndroid()) ? it.skip : it)('undefinedWidth', function (finish) {
		win = createWindow();
		var parentView = Ti.UI.createView({
				width: 100,
				height: 100
			}),
			view1 = Ti.UI.createView({
				left: 5,
				right: 10
			}),
			view2 = Ti.UI.createView({
				left: 5,
				center: {
					x: 10
				}
			}),
			view3 = Ti.UI.createView({
				center: {
					x: 75
				},
				right: 10
			});

		win.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				should(view1.width).be.undefined;
				should(view2.width).be.undefined;
				should(view3.width).be.undefined;
				should(view1.rect.width).eql(85);
				/*
				// This is wrong... i think
				should(view2.rect.width).eql(10);
				should(view3.rect.width).eql(30);
				*/

				finish();
			} catch (e) {
				finish(e);
			}
		});
		parentView.add(view1);
		parentView.add(view2);
		parentView.add(view3);
		win.add(parentView);
		win.open();
	});

	// functional test #1034/1034a/1034b UndefinedLeft
	// FIXME Get working on iOS and Android. They don't currently fire postlayout event for Ti.UI.Window
	((utilities.isIOS() || utilities.isAndroid()) ? it.skip : it)('undefinedLeft', function (finish) {
		win = createWindow();
		var view1 = Ti.UI.createView({
				width: 120,
				center: {
					x: 80
				}
			}),
			view2 = Ti.UI.createView({
				right: 120,
				center: {
					x: 80
				}
			}),
			view3 = Ti.UI.createView({
				right: 80,
				width: 120
			});

		win.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				should(view1.left).be.undefined;
				should(view2.left).be.undefined;
				should(view3.left).be.undefined;
				should(view1.rect.x).not.be.undefined;
				should(view2.rect.x).not.be.undefined;
				should(view3.rect.x).not.be.undefined;
				should(view1.rect.y).not.be.undefined;
				should(view2.rect.y).not.be.undefined;
				should(view3.rect.y).not.be.undefined;
				should(view1.rect.width).not.be.undefined;
				should(view2.rect.width).not.be.undefined;
				should(view3.rect.width).not.be.undefined;
				should(view1.rect.height).not.be.undefined;
				should(view2.rect.height).not.be.undefined;
				should(view3.rect.height).not.be.undefined;

				finish();
			} catch (e) {
				finish(e);
			}
		});
		win.add(view1);
		win.add(view2);
		win.add(view3);
		win.open();
	});

	// functional test #1035 & #1039 UndefinedCenter
	// FIXME Get working on iOS and Android. They don't currently fire postlayout event for Ti.UI.Window
	((utilities.isIOS() || utilities.isAndroid()) ? it.skip : it)('undefinedCenter', function (finish) {
		win = createWindow();
		var view = Ti.UI.createView({});
		view.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				should(view.center).be.undefined;
				//Dynamic center can be calculated from view.rect
				should(view.rect).not.be.undefined;

				finish();
			} catch (e) {
				finish(e);
			}
		});
		win.add(view);
		win.open();
	});

	// functional test #1036 UndefinedRight
	// FIXME Open a JIRA to fix this on iOS, because it causes a crash!
	// FIXME Android doesn't fire postlayout on Window or View right now
	((utilities.isIOS() || utilities.isAndroid()) ? it.skip : it)('undefinedRight', function (finish) {
		win = createWindow();
		var view = Ti.UI.createView({
			backgroundColor: 'yellow',
			center: {
				x: 50
			},
			left: 10
		});
		view.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				should(view.right).be.undefined;
				// this is wrong
				// should(view.rect.width).eql(80);
				should(view.rect.x).eql(10);

				finish();
			} catch (e) {
				finish(e);
			}
		});
		win.add(view);
		win.open();
	});

	// functional test #1037, #1037a, #1037b
	// UndefinedHeight Implicit calculations
	// FIXME Get working on iOS and Android. They don't currently fire postlayout event for Ti.UI.Window
	((utilities.isIOS() || utilities.isAndroid()) ? it.skip : it)('undefinedHeight', function (finish) {
		win = createWindow();
		var parentView = Ti.UI.createView({
				width: 100,
				height: 100
			}),
			view1 = Ti.UI.createView({
				top: 5,
				bottom: 10
			}),
			view2 = Ti.UI.createView({
				top: 5,
				center: {
					y: 10
				}
			}),
			view3 = Ti.UI.createView({
				center: {
					y: 75
				},
				bottom: 10
			});

		win.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				should(view1.height).be.undefined;
				should(view2.height).be.undefined;
				should(view3.height).be.undefined;
				should(view1.rect.height).eql(85);
				// should(view2.rect.height).eql(10);
				// should(view3.rect.height).eql(30);

				finish();
			} catch(e) {
				finish(e);
			}
		});
		parentView.add(view1);
		parentView.add(view2);
		parentView.add(view3);
		win.add(parentView);
		win.open();
	});

	// functional test #1038, 1038a, 1038b
	// UndefinedTop. Dynamic top calculation
	it.skip('undefinedTop', function (finish) {
		win = createWindow();
		var view1 = Ti.UI.createView({
				height: 50,
				center: {
					y: 200
				}
			}),
			view2 = Ti.UI.createView({
				center: {
					y: 50
				},
				bottom: 200
			}),
			view3 = Ti.UI.createView({
				bottom: 200,
				height: 100
			});

		win.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				//Static Tops
				should(view1.top).be.undefined;
				should(view2.top).be.undefined;
				should(view3.top).be.undefined;
				//Dynamic Tops
				should(view1.rect.y).eql(175);
				if (win.size.height <= 250) {//View Height of 0 positioned at center
					should(view2.rect.y).eql(50);
				} else {//View height = 2x(wh - bottom - center)
					//View top = center - height/2 = 2c + b - wh
					should(view2.rect.y).eql(300 - win.size.height);
				}
				should(view3.rect.y).eql(win.size.height - 300);

				finish();
			} catch (e) {
				finish(e);
			}
		});
		win.add(view1);
		win.add(view2);
		win.add(view3);
		win.open();
	});

	// functional test #1040 UndefinedBottom
	// FIXME Get working on iOS and Android. They don't currently fire postlayout event for Ti.UI.Window
	((utilities.isIOS() || utilities.isAndroid()) ? it.skip : it)('undefinedBottom', function (finish) {
		win = createWindow();
		var view = Ti.UI.createView({
			backgroundColor: 'yellow',
			center: {
				y: 50
			},
			top: 10
		});

		view.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				should(view.bottom).be.undefined;
				//Dynamic bottom is rect.y + rect.height
				should(view.rect.height).not.be.undefined;

				finish();
			} catch (e) {
				finish(e);
			}
		});
		win.add(view);
		win.open();
	});

	// functional test #1042 WidthPrecedence
	// FIXME Get working on Android. Doesn't currently fire postlayout for Ti.UI.View base class
	(utilities.isAndroid() ? it.skip : it)('widthPrecedence', function (finish) {
		win = createWindow();
		var view = Ti.UI.createView({
			backgroundColor: 'yellow',
			left: 10,
			right: 15,
			width: 10
		});
		view.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				should(view.size.width).eql(10);

				finish();
			} catch (e) {
				finish(e);
			}
		});
		win.add(view);
		win.open();
	});

	// functional test #1043 LeftPrecedence
	// Chris W: Skipping because we don't have any precedence set up yet for the properties, as far as I know...
	it.skip('leftPrecedence', function (finish) {
		win = createWindow();
		var view = Ti.UI.createView({
			backgroundColor: 'yellow',
			left: 10,
			right: 100,
			center: {
				x: 30
			}
		});

		view.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				should(view.size.width).eql(40);

				finish();
			} catch (e) {
				finish(e);
			}
		});
		win.add(view);
		win.open();
	});

	// functional test #1044 CenterXPrecedence
	it.skip('centerXPrecedence', function (finish) {
		win = createWindow();
		var view = Ti.UI.createView({
				height: 200,
				width: 200,
				backgroundColor: 'yellow'
			}),
			viewChild = Ti.UI.createView({
				backgroundColor: 'red',
				center: {
					x: 100
				},
				right: 50
			});

		viewChild.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				should(viewChild.size.width).eql(100);

				finish();
			} catch (e) {
				finish(e);
			}
		});
		view.add(viewChild);
		win.add(view);
		win.open();
	});

	// functional test #1046 HeightPrecedence
	// FIXME Get working on Android. Doesn't fire postlayout on Window or standard View class
	(utilities.isAndroid() ? it.skip : it)('heightPrecedence', function (finish) {
		win = createWindow();
		var view = Ti.UI.createView({
			backgroundColor: 'yellow',
			top: 10,
			bottom: 15,
			height: 10
		});

		view.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				should(view.size.height).eql(10);

				finish();
			} catch (e) {
				finish(e);
			}
		});
		win.add(view);
		win.open();
	});

	// functional test #1047 TopPrecedence
	// Chris W: Skipping because we don't have any precedence set up yet for the properties, as far as I know...
	it.skip('topPrecedence', function (finish) {
		win = createWindow();
		var view = Ti.UI.createView({
			backgroundColor: 'yellow',
			top: 10,
			bottom: 100,
			center: {
				y: 30
			}
		});

		view.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				should(view.size.height).eql(40);

				finish();
			} catch (e) {
				finish(e);
			}
		});
		win.add(view);
		win.open();
	});

	// functional test #1048 CenterYPrecedence
	it.skip('centerYPrecedence', function (finish) {
		win = createWindow();
		var view = Ti.UI.createView({
				height: 200,
				width: 200,
				backgroundColor: 'yellow'
			}),
			viewChild = Ti.UI.createView({
				backgroundColor: 'red',
				center: {
					y: 100
				},
				bottom: 50
			});

		viewChild.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				should(viewChild.size.height).eql(100);

				finish();
			} catch (e) {
				finish(e);
			}
		});
		view.add(viewChild);
		win.add(view);
		win.open();
	});

	// functional test #1053 ScrollViewSize
	// This is completely wrong. Adding a scrollview to a label?
	// Really? Skipping
	it.skip('scrollViewSize', function (finish) {
		win = createWindow();
		var label = Ti.UI.createLabel({
				color: 'red'
			}),
			label2 = Ti.UI.createLabel({
				text: 'View Size is: ',
				top: 20,
				left: 10,
				height: 200,
				color: 'black'
			}),
			scrollView = Titanium.UI.createScrollView({
				contentHeight: 'auto',
				contentWidth: 'auto',
				showVerticalScrollIndicator: true,
				showHorizontalScrollIndicator: true,
				width: Ti.UI.SIZE,
				height: Ti.UI.SIZE
			}),
			scrollView2 = Titanium.UI.createScrollView({
				contentHeight: 'auto',
				contentWidth: 'auto',
				showVerticalScrollIndicator: true,
				showHorizontalScrollIndicator: true
			}),
			view;

		label.add(scrollView);
		label2.add(scrollView2);
		view = Ti.UI.createView({
			backgroundColor: 'green',
			borderRadius: 10,
			width: 200,
			height: 200
		});
		//var scrollView3 = Titanium.UI.createScrollView({
		//	contentHeight: 'auto',
		//	contentWidth: 'auto',
		//	showVerticalScrollIndicator: true,
		//	showHorizontalScrollIndicator: true
		//});
		win.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				//LABEL HAS SIZE AUTO BEHAVIOR.
				//SCROLLVIEW HAS FILL BEHAVIOR
				//LABEL will have 0 size (no text)
				//LABEL2 will have non 0 size (has text/pins)
				should(label.size).not.be.undefined;
				should(label2.size).not.be.undefined;
				should(scrollView.size).not.be.undefined;
				should(scrollView2.size).not.be.undefined;
				if (utilities.isIPhone()) {
					//Android does not return 0 height even when there is no text
					should(label.size.width).eql(0);
					should(label.size.height).eql(0);
					// Adding a scroll view to a label does not work in android: TIMOB-7817
					should(scrollView.size.width).eql(0);
					should(scrollView.size.height).eql(0);
					should(label2.size.height).not.be.eql(0);
					should(label2.size.width).not.be.eql(0);
					should(scrollView2.size.height).not.be.eql(0);
					should(scrollView2.size.width).not.be.eql(0);
					should(label2.size.width).eql(scrollView2.size.width);
					should(label2.size.height).eql(scrollView2.size.height);
				}
				// This is not working yet due to TIMOB-5303
				// valueOf(testRun, scrollView3.size.height).shouldNotBe(0);
				// valueOf(testRun, scrollView3.size.width).shouldNotBe(0);
				//
				// valueOf(testRun, view.size.width).shouldBe(scrollView3.size.width);
				// valueOf(testRun, view.size.height).shouldBe(scrollView3.size.height);

				finish();
			} catch (e) {
				finish(e);
			}
		});
		view.add(scrollView);
		win.add(view);
		win.add(scrollView2);
		win.add(label);
		win.open();
	});

	// functional test #1106 ZIndexMultiple
	// FIXME Get working on iOS and Android. They don't fire Ti.UI.Window.postlayout event
	((utilities.isIOS() || utilities.isAndroid()) ? it.skip : it)('zIndexMultiple', function (finish) {
		win = createWindow();
		var view1 = Ti.UI.createView({
				backgroundColor: 'red',
				zIndex: 0,
				height: 50,
				width: 50,
				top: 10
			}),
			view2 = Ti.UI.createView({
				backgroundColor: 'orange',
				zIndex: 1,
				height: 50,
				width: 50,
				top: 20
			}),
			view3 = Ti.UI.createView({
				backgroundColor: 'yellow',
				zIndex: 2,
				height: 50,
				width: 50,
				top: 30
			}),
			view4 = Ti.UI.createView({
				backgroundColor: 'green',
				zIndex: 3,
				height: 50,
				width: 50,
				top: 40
			}),
			view5 = Ti.UI.createView({
				backgroundColor: 'blue',
				zIndex: 4,
				height: 50,
				width: 50,
				top: 50
			});

		win.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				should(view1.zIndex).eql(0);
				should(view2.zIndex).eql(1);
				should(view3.zIndex).eql(2);
				should(view4.zIndex).eql(3);
				should(view5.zIndex).eql(4);

				finish();
			} catch (e) {
				finish(e);
			}
		});
		win.add(view5);
		win.add(view4);
		win.add(view3);
		win.add(view2);
		win.add(view1);
		win.open();
	});

	// FIXME Android doesn't fire Ti.UI.View.postlayout event on standard View
	// FIXME times out on iOS, I assume never fires the postlayout event?
	((utilities.isAndroid() || utilities.isIOS()) ? it.skip : it)('fillInVerticalLayout', function (finish) {
		win = createWindow();
		var parent = Ti.UI.createView({
				height: 50,
				width: 40,
				layout: 'vertical'
			}),
			child = Ti.UI.createView({});

		parent.add(child);
		win.add(parent);

		parent.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				should(parent.size.width).eql(40);
				should(parent.size.height).eql(50);
				should(child.size.width).eql(40);
				should(child.size.height).eql(50);

				finish();
			} catch (e) {
				finish(e);
			}
		});
		win.open();
	});

	// FIXME Get working on iOS and Android. They don't currently fire postlayout event for Ti.UI.Window
	((utilities.isIOS() || utilities.isAndroid()) ? it.skip : it)('sizeFillConflict', function (finish) {
		win = createWindow();
		var grandParent = Ti.UI.createView({
				height: 300,
				width: 200
			}),
			parent = Ti.UI.createView({
				height: Ti.UI.SIZE
			}),
			child1 = Ti.UI.createView({
				height: Ti.UI.SIZE
			}),
			child2 = Ti.UI.createView({
				height: 50
			}),
			child3 = Ti.UI.createView({
				width: 30
			});

		child1.add(child2);
		child1.add(child3);
		parent.add(child1);
		grandParent.add(parent);
		win.add(grandParent);

		win.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				should(grandParent.size.width).eql(200);
				should(grandParent.size.height).eql(300);
				should(parent.size.width).eql(200);
				// should(parent.size.height).eql(300); // TIMOB-18684?
				should(child1.size.width).eql(200);
				// should(child1.size.height).eql(300); // TIMOB-18684?
				should(child2.size.width).eql(200);
				should(child2.size.height).eql(50);
				should(child3.size.width).eql(30);
				should(child3.size.height).eql(300);

				finish();
			} catch (e) {
				finish(e);
			}
		});
		win.open();
	});

	// Functional Test #1000 SystemMeasurement
	// FIXME Android doesn't fire Ti.UI.View.postlayout event on standard View
	// FIXME Times out on iOS. Never fires postlayout?
	((utilities.isAndroid() || utilities.isIOS()) ? it.skip : it)('systemMeasurement', function (finish) {
		win = createWindow();
		var parent = Ti.UI.createView({
				height: '50dip',
				width: '40px',
				layout: 'vertical'
			}),
			child = Ti.UI.createView({});

		parent.add(child);
		win.add(parent);

		parent.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				if (utilities.isAndroid()) {
					should(parent.size.width).eql(40);
				}
				else if (utilities.isIOS()) {
					should(parent.size.height).eql(50);
				} else {
					should(parent.size.width).eql(40);
				}
				finish();
			} catch (e) {
				finish(e);
			}
		});
		win.open();
	});

	// Functional Test #1001 #1002 #1003 #1004 #1005 #1006
	// Skip on Windows 10 Desktop for now, it hangs
	// FIXME Get working on iOS and Android. They don't fire Ti.UI.Window.postlayout event
	(((utilities.isWindows10() && utilities.isWindowsDesktop()) ||
	(utilities.isIOS() || utilities.isAndroid())) ? it.skip : it)('unitMeasurements', function (finish) {
		win = createWindow();
		var child = Ti.UI.createView({
				height: '50mm',
				width: '40cm'
			}),
			child1 = Ti.UI.createView({
				height: '1in',
				width: '100px'
			}),
			child2 = Ti.UI.createView({
				height: '50dip',
				width: '40dp'
			}),
			child3 = Ti.UI.createView({
				//inavlid measurement
				height: 'invalid',
				width: 'inavlid'
			});

		win.add(child);
		win.add(child1);
		win.add(child2);

		win.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				should(child.size.width).not.be.eql(0);
				should(child.size.height).not.be.eql(0);
				should(child1.size.width).not.be.eql(0);
				should(child1.size.height).not.be.eql(0);
				should(child2.size.width).not.be.eql(0);
				should(child2.size.height).not.be.eql(0);
				should(child3.size.width).eql(0);
				should(child3.size.height).eql(0);

				finish();
			} catch(e) {
				finish(e);
			}
		});
		win.open();
	});

	// Scrollview
	/*
	it('scrollViewAutoContentHeight', function (finish) {
		win = Ti.UI.createWindow();
		var scrollView = Titanium.UI.createScrollView({
				contentHeight: 'auto',
				contentWidth: 'auto',
				showVerticalScrollIndicator: true,
				showHorizontalScrollIndicator: true
			}),
			view2 = Ti.UI.createView({});

		scrollView.add(view2);

		win.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				should(view2.size.width).eql(scrollView.size.width);
				should(view2.size.height).eql(scrollView.size.height);

				finish();
			} catch (e) {
				finish(e);
			}
		});
		win.add(scrollView);
		win.open();
	});

	it('scrollViewLargeContentHeight', function (finish) {
		win = Ti.UI.createWindow();
		var scrollView = Titanium.UI.createScrollView({
				contentHeight: '2000',
				contentWidth: 'auto',
				showVerticalScrollIndicator: true,
				showHorizontalScrollIndicator: true
			}),
			view2 = Ti.UI.createView({});

		scrollView.add(view2);

		win.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				should(view2.size.width).eql(scrollView.size.width);
				should(view2.size.height).eql(2e3);

				finish();
			} catch (e) {
				finish(e);
			}
		});
		win.add(scrollView);
		win.open();
	});

	it('scrollViewMinimumContentHeight', function (finish) {
		win = Ti.UI.createWindow();
		var scrollView = Titanium.UI.createScrollView({
				contentHeight: '50',
				contentWidth: 'auto',
				showVerticalScrollIndicator: true,
				showHorizontalScrollIndicator: true
			}),
			view2 = Ti.UI.createView({});

		scrollView.add(view2);

		win.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				should(view2.size.width).eql(scrollView.size.width);
				should(view2.size.height).eql(scrollView.size.height);

				finish();
			} catch (e) {
				finish(e);
			}
		});
		win.add(scrollView);
		win.open();
	});

	it('horizontalScrollViewMinimumContentHeight', function (finish) {
		win = Ti.UI.createWindow();
		var scrollView = Titanium.UI.createScrollView({
				contentHeight: 'auto',
				contentWidth: '50',
				showVerticalScrollIndicator: true,
				showHorizontalScrollIndicator: true,
				scrollType: 'horizontal'
			}),
			view2 = Ti.UI.createView({});

		scrollView.add(view2);

		win.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				should(view2.size.width).eql(scrollView.size.width);
				should(view2.size.height).eql(scrollView.size.height);

				finish();
			} catch (e) {
				finish(e);
			}
		});
		win.add(scrollView);
		win.open();
	});

	it('horizontalScrollViewLargeContentHeight', function (finish) {
		win = Ti.UI.createWindow();
		var scrollView = Titanium.UI.createScrollView({
				contentHeight: 'auto',
				contentWidth: '50',
				showVerticalScrollIndicator: true,
				showHorizontalScrollIndicator: true,
				scrollType: 'horizontal'
			}),
			view2 = Ti.UI.createView({});

		scrollView.add(view2);

		win.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				should(view2.size.width).eql(scrollView.size.width);
				should(view2.size.height).eql(scrollView.size.height);

				finish();
			} catch (e) {
				finish(e);
			}
		});
		win.add(scrollView);
		win.open();
	});
	*/

	//TIMOB-8362
	// FIXME Android doesn't fire Ti.UI.ScrollView.postlayout event
	(utilities.isAndroid() ? it.skip : it)('scrollViewWithSIZE', function (finish) {
		win = createWindow({
			backgroundColor: '#7B6700',
			layout: 'vertical'
		});
		var NavBarView = Ti.UI.createView({
				height: '25',
				top: 0,
				backgroundColor: 'green',
				width: '100%'
			}),
			scrollView = Ti.UI.createScrollView({
				height: Ti.UI.SIZE,
				width: Ti.UI.SIZE,
				scrollType: 'vertical',
				layout: 'vertical',
				backgroundColor: 'red'
			}),
			button = Ti.UI.createButton({
				title: 'Click',
				width: '100',
				height: '50'
			});

		scrollView.add(button);
		win.add(NavBarView);
		win.add(scrollView);

		scrollView.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				should(scrollView.size.height).eql(50);
				should(scrollView.size.width).eql(100);

				finish();
			} catch (e) {
				finish(e);
			}
		});
		win.open();
	});

	//TIMOB-20385
	// FIXME Android doesn't fire Ti.UI.ScrollView.postlayout event
	(utilities.isAndroid() ? it.skip : it)('scrollViewWithTop', function (finish) {
		win = createWindow({
			backgroundColor: '#7B6700',
			layout: 'vertical'
		});
		var NavBarView = Ti.UI.createView({
				height: '25',
				top: 0,
				backgroundColor: 'green',
				width: '100%'
			}),
			scrollView = Ti.UI.createScrollView({
				height: 300,
				width: Ti.UI.FILL,
				scrollType: 'vertical',
				layout: 'vertical',
				backgroundColor: 'red'
			}),
			button = Ti.UI.createButton({
				title: 'Click',
				width: '100',
				height: '50',
				top: 20, left: 40
			});

		scrollView.add(button);
		win.add(NavBarView);
		win.add(scrollView);

		scrollView.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				should(scrollView.size.height).eql(300);
				should(button.top).eql(20);
				should(button.left).eql(40);
				finish();
			} catch (e) {
				finish(e);
			}
		});
		win.open();
	});

	// TIMOB-8891
	// FIXME Android doesn't fire Ti.UI.ScrollView.postlayout event
	// FIXME Fails on iOS due to timeout. Never fires postlayout?
	(((utilities.isWindows8_1() && utilities.isWindowsDesktop()) || utilities.isAndroid() || utilities.isIOS()) ? it.skip : it)('scrollViewWithLargeVerticalLayoutChild', function (finish) {
		win = createWindow();
		var scrollView = Ti.UI.createScrollView({
				contentHeight: 'auto',
				backgroundColor: 'green'
			}),
			innerView,
			colors = ['red', 'blue', 'pink', 'white', 'black'],
			max = 10;

		win.add(scrollView);
		innerView = Ti.UI.createView({
			height: Ti.UI.SIZE,
			// works if set to 1000
			layout: 'vertical',
			left: 0,
			top: 0,
			right: 0
		});
		scrollView.add(innerView);

		for (var i = 0; max > i; i++) {
			innerView.add(Ti.UI.createView({
				backgroundColor: colors[i % colors.length],
				height: 100,
				top: 20
			}));
		}

		scrollView.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				should(innerView.size.height).eql(1200);
				should(innerView.size.width).eql(scrollView.size.width);

				finish();
			} catch (e) {
				finish(e);
			}
		});
		win.open();
	});

	/*
	// Functional Test #1087-#1097
	it('convertUnits', function (finish) {
		// android
		var dpi = Ti.Platform.displayCaps.dpi;
		if (utilities.isAndroid()) {
			// 1087
			should(Ti.UI.convertUnits('1in', Ti.UI.UNIT_PX)).eql(dpi);
			should(Ti.UI.convertUnits('100', Ti.UI.UNIT_PX)).eql(100);
			// 1092
			should(Ti.UI.convertUnits('25.4mm', Ti.UI.UNIT_PX)).eql(dpi);
		} else if (utilities.isIOS()) {
			// 1091
			// TODO: This needs to support retina
			should(Ti.UI.convertUnits('1in', Ti.UI.UNIT_DIP)).eql(dpi);
			should(Ti.UI.convertUnits('100', Ti.UI.UNIT_DIP)).eql(100);
			should(Ti.UI.convertUnits('25.4mm', Ti.UI.UNIT_DIP)).eql(dpi);
		}
		// 1088
		should(Math.round(Ti.UI.convertUnits(dpi.toString(), Ti.UI.UNIT_MM))).eql(25);
		// 1089
		should(Math.round(Ti.UI.convertUnits(dpi.toString(), Ti.UI.UNIT_CM))).eql(3);
		// 1088
		should(Math.round(Ti.UI.convertUnits(dpi.toString(), Ti.UI.UNIT_MM))).eql(25);
		// 1089
		should(Math.round(Ti.UI.convertUnits(dpi.toString(), Ti.UI.UNIT_CM))).eql(3);
		// 1090
		should(Math.round(Ti.UI.convertUnits(dpi.toString(), Ti.UI.UNIT_IN))).eql(1);
		// 1093
		should(Ti.UI.convertUnits('100cm', Ti.UI.UNIT_MM)).eql(1e3);
		// 1094
		should(Ti.UI.convertUnits('100in', Ti.UI.UNIT_CM)).eql(254);
		// 1097
		should(Ti.UI.convertUnits('abc', Ti.UI.UNIT_PX)).eql(0);

	});
	*/

	// FIXME Android doesn't fire postlayout event on Ti.UI.Window or standard Ti.UI.View right now
	// FIXME Times out on iOS. Never fires postlayout?
	(((utilities.isWindows8_1() && utilities.isWindowsDesktop()) || utilities.isAndroid() || utilities.isIOS()) ? it.skip : it)('twoPins', function (finish) {
		win = createWindow();
		var view = Ti.UI.createView({
				width: 100,
				height: 100
			}),
			inner_view = Ti.UI.createView({
				left: 10,
				right: 10
			});

		view.add(inner_view);
		win.add(view);

		inner_view.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				should(inner_view.size.width).eql(80);
				should(inner_view.rect.width).eql(80);

				finish();
			} catch (e) {
				finish(e);
			}
		});
		win.open();
	});

	// FIXME Android doesn't fire Ti.UI.View.postlayout event on standard View
	// FIXME Tiems out on iOS. Never fires postlayout?
	(((utilities.isWindows8_1() && utilities.isWindowsDesktop()) || utilities.isAndroid() || utilities.isIOS()) ? it.skip : it)('fourPins', function (finish) {
		win = createWindow();
		var view = Ti.UI.createView({
				width: 100,
				height: 100
			}),
			inner_view = Ti.UI.createView({
				left: 10,
				right: 10,
				top: 10,
				bottom: 10
			});

		view.add(inner_view);
		win.add(view);

		inner_view.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				should(inner_view.size.width).eql(80);
				should(inner_view.size.height).eql(80);
				should(inner_view.left).eql(10);
				should(inner_view.right).eql(10);
				should(inner_view.top).eql(10);
				should(inner_view.bottom).eql(10);
				should(inner_view.rect.x).eql(10);
				should(inner_view.rect.width).eql(80);
				should(inner_view.rect.y).eql(10);
				should(inner_view.rect.height).eql(80);

				finish();
			} catch (e) {
				finish(e);
			}
		});
		win.open();
	});

	// TIMOB-18684
	// FIXME Android doesn't fire postlayout event for Ti.UI.Window or standard Ti.UI.View class right now
	(((utilities.isWindows8_1() && utilities.isWindowsDesktop()) || utilities.isAndroid()) ? it.skip : it)('layoutWithSIZE_and_fixed', function (finish) {
		win = createWindow();
		var view = Ti.UI.createView({
				backgroundColor: 'green',
				width: 100,
				height: Ti.UI.SIZE
			}),
			innerView = Ti.UI.createView({
				backgroundColor: 'blue',
				width: 100,
				height: 50
			});

		view.add(innerView);

		view.addEventListener('postlayout', function (e) {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				should(view.size.height).eql(innerView.size.height);
				should(view.size.width).eql(innerView.size.width);

				finish();
			} catch (e) {
				finish(e);
			}
		});
		win.add(view);
		win.open();
	});

	// TIMOB-23372 #1
	//
	// left/right/top/bottom should just work for child view
	// when both left/right/top/bottom are specified to parent
	//
	// FIXME Get working on iOS and Android. They don't fire Ti.UI.Window.postlayout event
	((utilities.isIOS() || utilities.isAndroid()) ? it.skip : it)('TIMOB-23372 #1', function (finish) {
		var a = Ti.UI.createView({
			backgroundColor: 'orange',
			top: 10,
			left: 10,
			right: 10,
			bottom: 10,
		}),
		b = Ti.UI.createView({
			backgroundColor: 'yellow',
			top: 10,
			left: 10,
			right: 10,
			bottom: 10,
		});
		win = createWindow();
		win.addEventListener('postlayout' , function() {
			try {
				should(a.rect.x).eql(10); // iOS gives 0
				should(a.rect.y).eql(10);
				should(b.rect.x).eql(10);
				should(b.rect.y).eql(10);
				should(b.rect.width).eql(a.rect.width - 20);
				should(b.rect.height).eql(a.rect.height - 20);
				finish();
			} catch (err) {
				finish(err);
			}
		});
		a.add(b);
		win.add(a);
		win.open();
	});

	// TIMOB-23372 #2
	//
	// left & right should just work for child view (vertical)
	// when both left & right are specified to parent
	//
	// FIXME Get working on iOS and Android. We can't rely on rect/size being valid in focus event!
	((utilities.isIOS() || utilities.isAndroid()) ? it.skip : it)('TIMOB-23372 #2', function (finish) {
		var view = Ti.UI.createView({
			backgroundColor: 'orange',
			layout: 'vertical',
			top: 10,
			left: 10,
			right: 10,
			height: Ti.UI.SIZE,
			width: Ti.UI.SIZE,
		}),
		label = Ti.UI.createLabel({
			left: 10,
			right: 10,
			color: 'green',
			backgroundColor: 'yellow',
			text: 'this is test text'
		});
		win = createWindow();

		win.addEventListener('focus', function () {
			if (didFocus) return;
			didFocus = true;

			Ti.API.info('Got focus event');
			try {
				should(view.rect.x).eql(10);
				should(view.rect.y).eql(10);
				should(label.rect.x).eql(10);
				should(label.rect.y).eql(0);
				should(label.rect.width).eql(view.rect.width - 20);
				finish();
			} catch (err) {
				finish(err);
			}
		});

		view.add(label);
		win.add(view);
		win.open();
	});

	// TIMOB-23372 #3
	//
	// left & right should just work for child view (composite)
	// when both left & right are specified to parent
	//
	// FIXME Get working on iOS and Android. We can't rely on rect/size being valid in focus event!
	((utilities.isIOS() || utilities.isAndroid()) ? it.skip : it)('TIMOB-23372 #3', function (finish) {
		var view = Ti.UI.createView({
			backgroundColor: 'yellow',
			layout: 'composite',
			top: 10,
			left: 10,
			right: 10,
			height: Ti.UI.SIZE,
			width: Ti.UI.SIZE
		}),
		label = Ti.UI.createLabel({
			left: 10,
			right: 10,
			color: 'blue',
			text: 'this is test text'
		});

		view.add(label);

		win = createWindow();

		win.addEventListener('focus', function () {
			if (didFocus) return;
			didFocus = true;

			Ti.API.info('Got focus event');
			try {
				should(view.rect.x).eql(10);
				should(view.rect.y).eql(10);
				should(label.rect.x).eql(10);
				should(label.rect.y).eql(0);
				should(label.rect.width).eql(view.rect.width - 20);
				finish();
			} catch (err) {
				finish(err);
			}
		});

		win.add(view);
		win.open();
	});

	// TIMOB-23372 #4
	//
	// left & right should just work for child view (horizontal)
	// when both left & right are specified to parent
	//
	// FIXME Get working on iOS and Android. We can't rely on rect/size being valid in focus event!
	((utilities.isIOS() || utilities.isAndroid()) ? it.skip : it)('TIMOB-23372 #4', function (finish) {
		var view = Ti.UI.createView({
			backgroundColor: 'yellow',
			layout: 'horizontal',
			top: 10,
			left: 10,
			right: 10,
			height: Ti.UI.SIZE,
			width: Ti.UI.SIZE
		}),
		label = Ti.UI.createLabel({
			left: 10,
			right: 10,
			color: 'blue',
			text: 'this is test text'
		});

		view.add(label);

		win = createWindow();

		win.addEventListener('focus', function () {
			if (didFocus) return;
			didFocus = true;

			Ti.API.info('Got focus event');
			try {
				should(view.rect.x).eql(10);
				should(view.rect.y).eql(10);
				should(label.rect.x).eql(10);
				should(label.rect.y).eql(0);
				should(label.rect.width).eql(view.rect.width - 20);
				finish();
			} catch (err) {
				finish(err);
			}
		});

		win.add(view);
		win.open();
	});

	// TIMOB-23372 #5
	//
	// left & right should just work for label (horizontal)
	// even when parent view doesn't have right value.
	// parent view should fit the size of the child, not Window
	//
	// FIXME Get working on iOS and Android. We can't rely on rect/size being valid in focus event!
	((utilities.isIOS() || utilities.isAndroid()) ? it.skip : it)('TIMOB-23372 #5', function (finish) {
		var view = Ti.UI.createView({
			backgroundColor: 'orange',
			layout: 'horizontal',
			top: 10,
			left: 10,
			height: Ti.UI.SIZE,
			width: Ti.UI.SIZE,
		}),
		label = Ti.UI.createLabel({
			left: 10,
			right: 10,
			color: 'green',
			backgroundColor: 'yellow',
			text: 'this is test text'
		});

		win = createWindow();

		win.addEventListener('focus', function () {
			if (didFocus) return;
			didFocus = true;

			Ti.API.info('Got focus event');
			try {
				should(view.rect.x).eql(10);
				should(view.rect.y).eql(10);
				should(label.rect.x).eql(10);
				should(label.rect.y).eql(0);
				should(label.rect.width).eql(view.rect.width - 20);
				should(view.rect.width).not.eql(win.rect.width - 20);
				finish();
			} catch (err) {
				finish(err);
			}
		});

		view.add(label);
		win.add(view);
		win.open();
	});

	// TIMOB-23372 #6
	//
	// left & right should just work for label (vertical)
	// even when parent view doesn't have right value.
	// parent view should fit the size of the child, not Window
	//
	// FIXME Get working on iOS and Android. We can't rely on rect/size being valid in focus event!
	((utilities.isIOS() || utilities.isAndroid()) ? it.skip : it)('TIMOB-23372 #6', function (finish) {
		var view = Ti.UI.createView({
			backgroundColor: 'orange',
			layout: 'vertical',
			top: 10,
			left: 10,
			height: Ti.UI.SIZE,
			width: Ti.UI.SIZE,
		}),
		label = Ti.UI.createLabel({
			left: 10,
			right: 10,
			color: 'green',
			backgroundColor: 'yellow',
			text: 'this is test text'
		});

		win = createWindow();

		win.addEventListener('focus', function () {
			if (didFocus) return;
			didFocus = true;

			Ti.API.info('Got focus event');
			try {
				should(view.rect.x).eql(10);
				should(view.rect.y).eql(10);
				should(label.rect.x).eql(10);
				should(label.rect.y).eql(0);
				should(label.rect.width).eql(view.rect.width - 20);
				should(view.rect.width).not.eql(win.rect.width - 20);
				finish();
			} catch (err) {
				finish(err);
			}
		});

		view.add(label);
		win.add(view);
		win.open();
	});

	// TIMOB-23372 #7
	//
	// left & right should just work for label (composite)
	// even when parent view doesn't have right value.
	// parent view should fit the size of the child, not Window
	//
	// FIXME Get working on iOS and Android. We can't rely on rect/size being valid in focus event!
	((utilities.isIOS() || utilities.isAndroid()) ? it.skip : it)('TIMOB-23372 #7', function (finish) {
		var view = Ti.UI.createView({
			backgroundColor: 'orange',
			layout: 'composite',
			top: 10,
			left: 10,
			height: Ti.UI.SIZE,
			width: Ti.UI.SIZE,
		}),
		label = Ti.UI.createLabel({
			left: 10,
			right: 10,
			color: 'green',
			backgroundColor: 'yellow',
			text: 'this is test text'
		});

		win = createWindow();

		win.addEventListener('focus', function () {
			if (didFocus) return;
			didFocus = true;

			Ti.API.info('Got focus event');
			try {
				should(view.rect.x).eql(10);
				should(view.rect.y).eql(10);
				should(label.rect.x).eql(10);
				should(label.rect.y).eql(0);
				should(label.rect.width).eql(view.rect.width - 20);
				should(view.rect.width).not.eql(win.rect.width - 20);
				finish();
			} catch (err) {
				finish(err);
			}
		});

		view.add(label);
		win.add(view);
		win.open();
	});

	// TIMOB-23372 #8
	//
	// left & right should just work for child view when parent is Window (composite)
	//
	// FIXME Get working on iOS and Android. We can't rely on rect/size being valid in focus event!
	((utilities.isIOS() || utilities.isAndroid()) ? it.skip : it)('TIMOB-23372 #8', function (finish) {
		var label = Ti.UI.createLabel({
			left: 10,
			right: 10,
			backgroundColor:'yellow',
			color: 'green',
			text: 'this is test text'
		});

		win = createWindow();

		win.addEventListener('focus', function () {
			if (didFocus) return;
			didFocus = true;

			Ti.API.info('Got focus event');
			try {
				should(label.rect.x).eql(10);
				should(label.rect.width).eql(win.rect.width - 20);
				finish();
			} catch (err) {
				finish(err);
			}
		});

		win.add(label);
		win.open();
	});

	// TIMOB-23372 #9
	//
	// left & right should just work for child view when parent is Window (horizontal)
	//
	// FIXME Get working on iOS and Android. We can't rely on rect/size being valid in focus event!
	((utilities.isIOS() || utilities.isAndroid()) ? it.skip : it)('TIMOB-23372 #9', function (finish) {
		var label = Ti.UI.createLabel({
			left: 10,
			right: 10,
			backgroundColor:'yellow',
			color: 'green',
			text: 'this is test text'
		});

		win = createWindow();

		win.addEventListener('focus', function () {
			if (didFocus) return;
			didFocus = true;

			Ti.API.info('Got focus event');
			try {
				should(label.rect.x).eql(10);
				should(label.rect.width).eql(win.rect.width - 20); // Android gives us 97, should be 1260
				finish();
			} catch (err) {
				finish(err);
			}
		});

		win.add(label);
		win.open();
	});

	// TIMOB-23372 #10
	//
	// left & right should just work for child view when parent is Window (vertical)
	//
	// FIXME Get working on iOS and Android. We can't rely on rect/size being valid in focus event!
	((utilities.isIOS() || utilities.isAndroid()) ? it.skip : it)('TIMOB-23372 #10', function (finish) {
		var label = Ti.UI.createLabel({
			left: 10,
			right: 10,
			backgroundColor:'yellow',
			color: 'green',
			text: 'this is test text'
		});

		win = createWindow({layout:'vertical'});

		label.addEventListener('focus', function () {
			if (didFocus) return;
			didFocus = true;

			Ti.API.info('Got focus event');
			try {
				should(label.rect.x).eql(10);
				should(label.rect.width).eql(win.rect.width - 20);
				finish();
			} catch (err) {
				finish(err);
			}
		});

		win.add(label);
		win.open();
	});

	// TIMOB-23305
	//
	// Label width should be updated when setting new text
	// FIXME Get working on iOS and Android. We can't rely on rect/size being valid in focus event!
	((utilities.isIOS() || utilities.isAndroid()) ? it.skip : it)('TIMOB-23305', function (finish) {
		var label = Ti.UI.createLabel({
				text: 'Lorem ipsum dolor sit amet',
				backgroundColor: 'orange',
			}),
			savedRect = {},
			error;


		win = createWindow();

		// FIXME Make sure we call finish after both events/assertion blocks happen!
		// FIXME we can't rely on size/rect being valid on a focus event!
		win.addEventListener('focus', function () {
			if (didFocus) return;
			didFocus = true;

			Ti.API.info('Got focus event');
			try {
				should(label.rect.width).not.eql(0);
				should(label.rect.height).not.eql(0);
				should(label.rect.width).greaterThan(savedRect.width);
				if (utilities.isWindowsPhone()) {
					should(label.rect.height).greaterThan(savedRect.height);
				}
			} catch (err) {
				error = err;
			}

			finish(error);
		});
		label.addEventListener('postlayout', function () {
			if (didPostlayout) return;
			didPostlayout = true;

			try {
				savedRect = label.rect;
				should(label.rect.width).not.eql(0);
				should(label.rect.height).not.eql(0);
				label.text = 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Ut mollis rutrum dignissim.';
			} catch (err) {
				error = err;
			}
		});
		win.add(label);
		win.open();
	});
});
