/* * Appcelerator Titanium Mobile * Copyright (c) 2011-2012 by Appcelerator, Inc. All Rights Reserved. * Licensed under the terms of the Apache Public License * Please see the LICENSE included with this distribution for details. */module.exports = new function() {	var finish;	var valueOf;	this.init = function(testUtils) {		finish = testUtils.finish;		valueOf = testUtils.valueOf;	}		this.name = "ui_textArea";	this.tests = [		{name: "hasTextMethod"},		{name: "maxlimit"},		{name: "setselectionProperty"},		{name: "selectedEvent"}	]		//TIMOB-8303	this.hasTextMethod = function(testRun) {		var textArea1 = Ti.UI.createTextArea();		var textArea2 = Ti.UI.createTextArea({			value : 'I am a textarea'		});		var textArea3 = Ti.UI.createTextArea({			value : '',		});		valueOf(testRun,textArea1.hasText()).shouldBeFalse();		valueOf(testRun, textArea2.hasText()).shouldBeTrue();		valueOf(testRun, textArea3.hasText()).shouldBeFalse();				finish(testRun);	}		//TIMOB-10222	this.maxlimit = function(testRun) {		var win = Ti.UI.createWindow();		var txt = Ti.UI.createTextArea({			top: 150,			height: 100,			value:'abcde',			maxLength:5,			backgroundColor: "white" 		});		valueOf(testRun, txt.value).shouldBe('abcde');		txt.maxLength=3;		txt.addEventListener("focus", function(){			if(Ti.Platform.osname === 'iphone'){				valueOf(testRun, txt.value).shouldBe('abc');			}			finish(testRun);		})		win.add(txt);		win.open();		txt.focus();	}		//TIMOB-10460	this.setselectionProperty = function(testRun) {		var text = Ti.UI.createTextArea({			top: 10,			value: "This is Sparta.",			left:10		});		var win = Ti.UI.createWindow({backgroundColor: "#fff"});		text.addEventListener('focus', function(e) {			valueOf(testRun, function(){				text.setSelection(0, 4);			}).shouldNotThrowException();			valueOf(testRun, text.top).shouldBe(10);			valueOf(testRun, text.left).shouldBe(10);					finish(testRun);		});		win.add(text);		win.open();		text.focus();	}		//TIMOB-8606	this.selectedEvent=function(testRun){		if(Ti.Platform.osname === 'iphone'){			var win1 = Titanium.UI.createWindow();			var ta = Ti.UI.createTextArea({				value:'I am a text area',				width:Ti.UI.FILL,				height:100,			})			ta.addEventListener('selected',function(e){				valueOf(testRun, ta.height).shouldBe(100);				finish(testRun);			})			win1.add(ta);			win1.open();			ta.focus();		}		else		finish(testRun);	}}