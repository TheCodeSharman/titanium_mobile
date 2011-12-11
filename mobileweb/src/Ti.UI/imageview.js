Ti._5.createClass("Titanium.UI.ImageView", function(args){
	args = require.mix({
		unselectable: true
	}, args);

	var obj = this,
		domNode = Ti._5.DOMView(this, "img", args, "ImageView"),
		domStyle = domNode.Style,
		isError = false,
		_reverse = false,
		_canScale = true,
		_src = "",
		_images = [],
		_preventDefaultImage = false,
		_height;

	// Interfaces
	Ti._5.Touchable(this, args);
	Ti._5.Styleable(this, args);
	Ti._5.Positionable(this, args);
	Ti._5.Clickable(this, args);

	function loadImages(images) {
		isError = false;
		_preventDefaultImage || (domNode.src = Ti._5.getAbsolutePath(obj.defaultImage));

		var img = new Image();
			h = require.on(img, "load", function () {
				if (iCounter < images.length) return true;
				domNode.src = Ti._5.getAbsolutePath(images[0]);
				h && h();
				obj.fireEvent("load", {
					state: 2 < images.length ? obj.image : obj.images
				});
			};

		require.on(img, "error",  function () {
			isError = true;
			h && h();
		});

		// start preloading
		require.each(images, function(i) {
			img.src = Ti._5.getAbsolutePath(i);
		});
	}

	// Properties
	Ti._5.prop(this, {
		"animating": null,
		"canScale": {
			get: function(){return _canScale;},
			set: function(val){
				_canScale = !!val;
				if (!_canScale) {
					domStyle.width = "auto";
					domStyle.height = "auto";
				}
			}
		},
		"defaultImage": "",
		"duration": null,
		"enableZoomControls": true,
		"height": {
			get: function() {
				return _height;
			},
			set: function(val) {
				_height = val;
				domStyle.height = Ti._5.px(val);
			}
		},
		// indicates whether or not the source image is in 2x resolution for retina displays. 
		// Use for remote images ONLY. (iOS)
		"hires": false,
		"image": {
			get: function(){return _src;},
			set: function(val){loadImages([_src = val]);}
		},
		"images": {
			get: function(){return _images;},
			set: function(val){
				_images = -1 != val.constructor.toString().indexOf("Array") ? val : [val];
				loadImages(_images);
			}
		},
		"paused": null,
		"preventDefaultImage": {
			get: function(){return _preventDefaultImage;},
			set: function(val){_preventDefaultImage = !!val;}
		},
		"repeatCount": 0,
		"reverse": {
			get: function(){return _reverse;},
			set: function(val){_reverse = !!val;}
		},
		"size": {
			get: function() {
				return {
					width	: obj.width,
					height	: obj.height
				}
			},
			set: function(val) {
				val.width && (obj.width = Ti._5.px(val.width));
				val.height && (obj.height = Ti._5.px(val.height));
			}
		},
		"width": {
			get: function() {
				if (!domStyle.width || !obj.canScale) {
					return "";
				}
				return /%/.test(domStyle.width) ? parseInt(domStyle.width)+"%" : parseInt(domStyle.width);
			},
			set: function(val) {
				obj.canScale && (domStyle.width = /%/.test(val+"") ? parseInt(val) + "%" : parseInt(val) + "px");
			}
		}
	});

	require.mix(this, args);

	// Methods
	this.pause = function(){
		console.debug('Method "Titanium.UI.ImageView#.pause" is not implemented yet.');
	};
	this.start = function(){
		console.debug('Method "Titanium.UI.ImageView#.start" is not implemented yet.');
	};
	this.stop = function(){
		console.debug('Method "Titanium.UI.ImageView#.stop" is not implemented yet.');
	};
	this.toBlob = function(){
		console.debug('Method "Titanium.UI.ImageView#.toBlob" is not implemented yet.');
	};

	// Events
	this.addEventListener("change", function(){
		console.debug('Event "change" is not implemented yet.');
	});
	this.addEventListener("start", function(){
		console.debug('Event "start" is not implemented yet.');
	});
	this.addEventListener("stop", function(){
		console.debug('Event "stop" is not implemented yet.');
	});
});
