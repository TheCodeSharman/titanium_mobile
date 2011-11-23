Ti._5.createClass('Titanium.UI.Animation', function(args){
	var obj = this;

	// Properties
	var _autoreverse = null;
	Object.defineProperty(this, 'autoreverse', {
		get: function(){return _autoreverse;},
		set: function(val){return _autoreverse = val;}
	});

	var _backgroundColor = null;
	Object.defineProperty(this, 'backgroundColor', {
		get: function(){return _backgroundColor;},
		set: function(val){return _backgroundColor = val;}
	});

	var _color = null;
	Object.defineProperty(this, 'color', {
		get: function(){return _color;},
		set: function(val){return _color = val;}
	});

	var _curve = null;
	Object.defineProperty(this, 'curve', {
		get: function(){return _curve;},
		set: function(val){return _curve = val;}
	});

	var _delay = null;
	Object.defineProperty(this, 'delay', {
		get: function(){return _delay;},
		set: function(val){return _delay = val;}
	});

	var _duration = null;
	Object.defineProperty(this, 'duration', {
		get: function(){return _duration;},
		set: function(val){return _duration = val;}
	});

	var _opacity = null;
	Object.defineProperty(this, 'opacity', {
		get: function(){return _opacity;},
		set: function(val){return _opacity = val;}
	});

	var _opaque = null;
	Object.defineProperty(this, 'opaque', {
		get: function(){return _opaque;},
		set: function(val){return _opaque = val;}
	});

	var _repeat = null;
	Object.defineProperty(this, 'repeat', {
		get: function(){return _repeat;},
		set: function(val){return _repeat = val;}
	});

	var _transform = null;
	Object.defineProperty(this, 'transform', {
		get: function(){return _transform;},
		set: function(val){return _transform = val;}
	});

	var _transition = null;
	Object.defineProperty(this, 'transition', {
		get: function(){return _transition;},
		set: function(val){return _transition = val;}
	});

	var _visible = null;
	Object.defineProperty(this, 'visible', {
		get: function(){return _visible;},
		set: function(val){return _visible = val;}
	});

	var _zIndex = null;
	Object.defineProperty(this, 'zIndex', {
		get: function(){return _zIndex;},
		set: function(val){return _zIndex = val;}
	});
	
	var _top = null;
	Object.defineProperty(obj, 'top', {
		get: function(){return _top;},
		set: function(val){return _top = val;}
	});

	var _bottom;
	Object.defineProperty(obj, 'bottom', {
		get: function(){return _bottom;},
		set: function(val){return _bottom = val;}
	});

	var _left;
	Object.defineProperty(obj, 'left', {
		get: function(){return _left;},
		set: function(val){return _left = val;}
	});		

	var _right;
	Object.defineProperty(obj, 'right', {
		get: function(){return _right;},
		set: function(val){return _right = val;}
	});	
	
	var _width;
	Object.defineProperty(obj, 'width', {
		get: function(){return _width;},
		set: function(val){return _width = val;}
	});	
	
	var _height;
	Object.defineProperty(obj, 'height', {
		get: function(){return _height;},
		set: function(val){return _height = val;}
	});

	var _center, isAdded = false;
	Object.defineProperty(obj, 'center', {
		get: function(){return _center;},
		set: function(val){return _center = val;}
	});

	Ti._5.presetUserDefinedElements(this, args);
});