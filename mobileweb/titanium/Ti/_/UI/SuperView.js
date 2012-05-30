define(["Ti/_/declare", "Ti/UI", "Ti/UI/View"], function(declare, UI, View) {

	var windows = [];

	return declare("Ti._.UI.SuperView", View, {

		destroy: function() {
			this.close();
			View.prototype.destroy.apply(this, arguments);
		},

		open: function(args) {
			if (!this._opened) {
				this._opened = 1;
				UI._addWindow(this, 1).show();

				var len = windows.length;
				len && windows[len-1]._handleBlurEvent(2); // only blur the active tab
				windows.push(this);

				this.fireEvent("open");
				this._handleFocusEvent();
			}
		},

		close: function(args) {
			if (this.tab) {
				this.tab.close(this);
			} else if (this._opened) {
				var i = windows.indexOf(this),
					len = windows.length - 1;

				UI._removeWindow(this);

				i !== -1 && i === len && this._handleBlurEvent(1); // blur all tabs
				this.fireEvent("close");

				~i && windows.splice(i, 1);

				// if we just closed the active window, focus the next top-most window
				if (i === len) {
					for (i = windows.length - 1; i >= 0 && !windows[i]._opened; i--) {}
					i >= 0 && windows[i]._handleFocusEvent();
				}

				this._opened = 0;
			}
		},

		_handleFocusEvent: function(args) {
			this.fireEvent("focus", args);
		},

		_handleBlurEvent: function(args) {
			this.fireEvent("blur", args);
		}

	});

});