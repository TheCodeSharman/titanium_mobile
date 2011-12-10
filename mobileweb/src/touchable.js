(function(oParentNamespace) {
	// Create interface
	oParentNamespace.Touchable = function(obj, args) {
		args = args || {};

		obj.addEventListener || oParentNamespace.EventDriven(obj);

		var on = require.on,
			domNode = obj.dom,
			bEmulate = !("ontouchstart" in window),
			_startPoint = null,
			_endPoint = null,
			_isDoubleTap = false;

		Ti._5.prop(obj, "touchEnabled", !!args.touchEnabled);

		on(domNode, bEmulate ? "mousedown" : "touchstart", function(evt) {
			if (!obj.touchEnabled) {
				return true;
			}
			
			var touches = evt.touches ? evt.touches : [evt],
				xCoord = touches[0].pageX,
				yCoord = touches[0].pageY,
				oevt = {
					globalPoint: { x:xCoord, y:yCoord },
					x: xCoord,
					y: yCoord
				};

			_startPoint = oevt.globalPoint;
			_startPoint.source = evt.target;
			_endPoint = oevt.globalPoint;
			obj.fireEvent("touchstart", oevt);

			if (touches.length > 1) {
				obj.fireEvent("twofingertap",  {
					globalPoint: { x:xCoord, y:yCoord },
					x: xCoord,
					y: yCoord
				});
			}
		});

		on(domNode, bEmulate ? "mousemove" : "touchmove", function(evt) {
			if (!obj.touchEnabled || bEmulate && !_startPoint) {
				return true;
			}

			var touches = evt.touches ? evt.touches : [evt],
				xCoord = touches[0].pageX,
				yCoord = touches[0].pageY,
				oevt = {
					globalPoint: { x:xCoord, y:yCoord },
					x: xCoord,
					y: yCoord
				};

			_endPoint = oevt.globalPoint;
			obj.fireEvent("touchmove", oevt);
		});

		on(domNode, bEmulate ? "mouseup" : "touchend", function(evt) {
			if (!obj.touchEnabled) {
				return true;
			}

			_endPoint || (_endPoint = { x: evt.pageX, y: evt.pageY });

			var oevt = {
				globalPoint: { x:_endPoint.x, y:_endPoint.y },
				x: _endPoint.x,
				y: _endPoint.y
			};
			obj.fireEvent("touchend", oevt);

			if (_startPoint && _startPoint.source && _startPoint.source == evt.target && Math.abs(_endPoint.x - _startPoint.x) >= 50) {
				oevt.direction = _endPoint.x > _startPoint.x ? "right" : "left";
				obj.fireEvent("swipe", oevt);
			}
			_startPoint = _endPoint = null;
		});

		on(domNode, "touchcancel", function(evt) {
			if (!obj.touchEnabled) {
				return true;
			}

			obj.fireEvent("touchcancel", {
				globalPoint: { x:evt.pageX, y:evt.pageY },
				x: evt.pageX,
				y: evt.pageY
			});
		});

		on(domNode, "click", function(evt) {
			if (!obj.touchEnabled) {
				return true;
			}

			var oevt = {
				globalPoint: { x:evt.pageX, y:evt.pageY },
				x: evt.pageX,
				y: evt.pageY
			};
			obj.fireEvent("singletap", oevt);

			if (_isDoubleTap = !_isDoubleTap) {
				setTimeout(function() { 
					_isDoubleTap = false;
				}, 400);
			} else {
				obj.fireEvent("doubletap", oevt);
			}
		});
	}
})(Ti._5);
