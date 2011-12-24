define("Ti/UI/Window", ["Ti/_/declare", "Ti/Gesture", "Ti/_/UI/SuperView", "Ti/UI"], function(declare, Gesture, SuperView, UI) {

	var undef;

	return declare("Ti.UI.Window", SuperView, {

		properties: {
			orientation: {
				get: function() {
					return Gesture.orientation;
				}
			},

			title: {
				set: function(value) {
					return this.setWindowTitle(value);
				}
			},

			titleid: {
				get: function(value) {
					// TODO
					console.debug('Property "Titanium.UI.Window#.titleid" is not implemented yet.');
					return value;
				},
				set: function(value) {
					console.debug('Property "Titanium.UI.Window#.titleid" is not implemented yet.');
					return value;
				}
			},

			titlePrompt: {
				get: function(value) {
					// TODO
					console.debug('Property "Titanium.UI.Window#.titlePrompt" is not implemented yet.');
					return value;
				},
				set: function(value) {
					console.debug('Property "Titanium.UI.Window#.titlePrompt" is not implemented yet.');
					return value;
				}
			},

			titlepromptid: {
				get: function(value) {
					// TODO
					console.debug('Property "Titanium.UI.Window#.titlepromptid" is not implemented yet.');
					return value;
				},
				set: function(value) {
					console.debug('Property "Titanium.UI.Window#.titlepromptid" is not implemented yet.');
					return value;
				}
			},

			url: {
				get: function(value) {
					// TODO
					console.debug('Property "Titanium.UI.Window#.url" is not implemented yet.');
					return value;
				},
				set: function(value) {
					console.debug('Property "Titanium.UI.Window#.url" is not implemented yet.');
					return value;
				}
			}
		},

		open: function() {
			SuperView.prototype.open.apply(this);
			this.setWindowTitle(this.title);
			this.fireEvent("open", { source: null });
			this.fireEvent("focus", { source: this.domNode });
		},

		close: function() {
			SuperView.prototype.close.apply(this, arguments);
			this.fireEvent("blur", { source: this.domNode });
			this.fireEvent("close", { source: null });
		}

	});

});