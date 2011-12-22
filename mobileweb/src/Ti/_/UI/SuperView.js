define("Ti/_/UI/SuperView", ["Ti/_/declare", "Ti/_/dom", "Ti/UI", "Ti/UI/View"], function(declare, dom, UI, View) {

	var windows = [],
		activeWindow;

	require.on(window, "popstate", function(evt) {
		var win;
		evt && evt.state && evt.state.screenIndex !== null && (win = windows[evt.state.windowIdx]) && win.open({ isBack:1 });
	});

	require.on(window, "resize", function() {
		for (var i = 0; i < windows.length; i++) {
			windows[i] && !windows[i].parent && windows[i].doLayout();
		}
	});

	return declare("Ti._.UI.SuperView", View, {

		_windowIdx: null,
		_opened: 0,

		constructor: function() {
			this._windowIdx = windows.length;
			windows.push(this);
		},

		destroy: function() {
			windows[this._windowIdx] = null;
			View.prototype.destroy.apply(this, arguments);
		},

		open: function(args) {
			if (!this._opened) {
				// TODO: if args, then do animation on open
				this._opened = 1;
				this.parent || UI.rootNode.appendChild(this.domNode);
				this.show();
				(args && args.isBack) || window.history.pushState({ windowIdx: this._windowIdx }, "", "");
			}
			activeWindow = this;
		},

		close: function(args) {
			if (this._opened) {
				// TODO: if args, then do animation on close
				this._opened = 0;
				UI.rootNode.removeChild(this.domNode);
				window.history.go(-1);
				this.fireEvent("blur", { source: this.domNode });
			}
		},

		setWindowTitle: function(title) {
			activeWindow === this && (document.title = title || require.config.project.name);
			return title;
		}

	});

});