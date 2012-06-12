define(["Ti/_/declare", "Ti/_/UI/KineticScrollView", "Ti/_/style", "Ti/_/lang", "Ti/UI"],
	function(declare, KineticScrollView, style, lang, UI) {

	var isDef = lang.isDef,

		// The amount of deceleration (in pixels/ms^2)
		deceleration = 0.00175;

	return declare("Ti.UI.ScrollView", KineticScrollView, {

		constructor: function(args) {
			var self = this,
				contentContainer,
				scrollbarTimeout;
			this._initKineticScrollView(contentContainer = UI.createView({
				width: UI.SIZE,
				height: UI.SIZE,
				left: 0,
				top: 0
			}), "both", "both");
		},

		_handleMouseWheel: function() {
			self._isScrollBarActive && self.fireEvent("scroll",{
				x: -self._currentTranslationX,
				y: -self._currentTranslationY,
				dragging: false
			});
		},

		_handleDragStart: function() {
			this.fireEvent("dragStart",{});
		},

		_handleDrag: function() {
			this.fireEvent("scroll",{
				x: -this._currentTranslationX,
				y: -this._currentTranslationY,
				dragging: true
			});
		},

		_handleDragEnd: function(e, velocityX, velocityY) {
			if (isDef(velocityX)) {
				var self = this,
					velocity = Math.sqrt(velocityX * velocityX + velocityY * velocityY),
					distance = velocity * velocity / (1.724 * deceleration),
					duration = velocity / deceleration,
					theta = Math.atan(Math.abs(velocityY / velocityX)),
					distanceX = distance * Math.cos(theta) * (velocityX < 0 ? -1 : 1),
					distanceY = distance * Math.sin(theta) * (velocityY < 0 ? -1 : 1),
					translationX = Math.min(0, Math.max(self._minTranslationX, self._currentTranslationX + distanceX)),
					translationY = Math.min(0, Math.max(self._currentTranslationY + distanceY));
				self.fireEvent("dragEnd",{
					decelerate: true
				});
				self._animateToPosition(translationX, translationY, duration, "ease-out", function() {
					self._setTranslation(translationX, translationY);
					self._endScrollBars();
				});
			}
		},

		scrollTo: function(x, y) {
			self._setTranslation(x !== null ? -x : this._currentTranslationX, y !== null ? -y : this._currentTranslationX);
		},

		_defaultWidth: UI.FILL,

		_defaultHeight: UI.FILL,

		_getContentOffset: function(){
			return this.contentOffset;
		},

		_preLayout: function() {
			var needsRecalculation = this._contentContainer.layout === this.layout
			this._contentContainer.layout = this.layout;
			return needsRecalculation;
		},

		add: function(view) {
			this._contentContainer._add(view);
			this._publish(view);
		},

		remove: function(view) {
			this._contentContainer.remove(view);
			this._unpublish(view);
		},

		properties: {
			contentHeight: {
				get: function(value) {
					return this._contentContainer.height;
				},
				set: function(value) {
					this._contentContainer.height = value;
					return value;
				}
			},

			contentOffset: {
				get: function(value) {
					return {
						x: -this._currentTranslationX,
						y: -this._currentTranslationX
					};
				},
				set: function(value) {
					this._setTranslation(isDef(value.x) ? -value.x : this._currentTranslationX,
						isDef(value.y) ? -value.y : this._currentTranslationY);
					return value;
				}
			},

			contentWidth: {
				get: function(value) {
					return this._contentContainer.width;
				},
				set: function(value) {
					this._contentContainer.width = value;
					return value;
				}
			},

			showHorizontalScrollIndicator: {
				set: function(value, oldValue) {
					if (value !== oldValue) {
						if (value) {
							this._createHorizontalScrollBar();
						} else {
							this._destroyHorizontalScrollBar();
						}
					}
					return value;
				},
				value: true
			},

			showVerticalScrollIndicator: {
				set: function(value, oldValue) {
					if (value !== oldValue) {
						if (value) {
							this._createVerticalScrollBar();
						} else {
							this._destroyVerticalScrollBar();
						}
					}
					return value;
				},
				value: true
			}
		}

	});

});