define("Ti/UI/TableView", ["Ti/_/declare", "Ti/UI/View", "Ti/_/dom", "Ti/_/css", "Ti/_/style", "Ti/_/lang"], function(declare, View, dom, css, style, lang) {

	var set = style.set,
		is = require.is,
		isDef = require.isDef,
		undef;

	return declare("Ti.UI.TableView", View, {
		
		constructor: function(args) {
			
			// Create the parts out of Ti controls so we can make use of the layout system
			this.layout = 'vertical';
			set(this.domNode,"overflow-x","hidden");
			set(this.domNode,"overflow-y","auto");
			
			// Use horizontal layouts so that the default location is always (0,0)
			this.header = Ti.UI.createView({height: 'auto', layout: 'horizontal'});
			this.rows = Ti.UI.createView({height: 'auto', layout: 'vertical'});
			this.footer = Ti.UI.createView({height: 'auto', layout: 'horizontal'});
			
			this.add(this.header);
			this.add(this.rows);
			this.add(this.footer);
			
			this.data = [];
			
			// Handle scrolling
			var previousTouchLocation;
			this.addEventListener("touchstart",function(e) {
				previousTouchLocation = e.y;
			});
			this.addEventListener("touchend",function(e) {
				previousTouchLocation = null;
				
				// Create the scroll event
				this.fireEvent("scrollEnd",{
					contentOffset: {x: 0, y: this.domNode.scrollTop},
					contentSize: {width: this.rows._measuredWidth, height: this.rows._measuredHeight},
					size: {width: this._measuredWidth, height: this._measuredHeight},
					x: e.x,
					y: e.y
				})
			});
			this.addEventListener("touchmove",lang.hitch(this,function(e) {
				this.domNode.scrollTop += previousTouchLocation - e.y;
				previousTouchLocation = e.y;
				
				// Calculate the visible items
				var firstVisibleItem,
					visibleItemCount = 1,
					scrollTop = this.domNode.scrollTop;
				for(var i = 0; i < this.data.length; i++) {
					var row = this.data[i];
					if (firstVisibleItem) {
						if (row._measuredTop - scrollTop < this._measuredHeight) {
							visibleItemCount++;
						}
					} else if (row._measuredTop <= scrollTop && row._measuredTop + row._measuredHeight > scrollTop) {
						firstVisibleItem = row;
					}
				}
				
				// Create the scroll event
				this.fireEvent("scroll",{
					contentOffset: {x: 0, y: this.domNode.scrollTop},
					contentSize: {width: this.rows._measuredWidth, height: this.rows._measuredHeight},
					firstVisibleItem: firstVisibleItem,
					size: {width: this._measuredWidth, height: this._measuredHeight},
					totalItemCount: this.data.length,
					visibleItemCount: visibleItemCount,
					x: e.x,
					y: e.y
				})
			}));
		},
		
		_createSeparator: function() {
			return Ti.UI.createView({
				height: 1,
				width: "100%",
				backgroundColor: this.separatorColor
			})
		},

		appendRow: function(value) {
			this.insertRowBefore(this.data.length, value);
		},
		
		deleteRow: function(index) {
			
			if (index < 0 || index > this.data.length) {
				return;
			}
			
			var view = this.data.splice(index,1);
			this.rows.remove(view[0]);
			this.rows.remove(view[0]._separator);
		},
		
		insertRowAfter: function(index, value) {
			index++;
			if (is(value,"Array")) {
				for (var i = 0; i < value.length; i++) {
					this._insertHelper(value[i], index++);
				}
			} else {
				this._insertHelper(value, index);
			}
		},
		
		insertRowBefore: function(index, value) {
			if (is(value,"Array")) {
				for (var i = 0; i < value.length; i++) {
					this._insertHelper(value[i], index++);
				}
			} else {
				this._insertHelper(value, index);
			}
		},
		
		_insertHelper: function(view, index) {
			
			if (index < 0 || index > this.data.length) {
				return;
			}
			
			if (!isDef(view.declaredClass) || view.declaredClass != "Ti.UI.TableViewRow") {
				view = Ti.UI.createTableViewRow(view);
			}
			
			view._separator = this._createSeparator();
			if (index == this.data.length) {
				this.data.push(view);
				this.rows.add(view);
				this.rows.add(view._separator);
			} else {
				this.data.splice(index,0,view);
				this.rows._insertAt(view,2 * index + 1);
				this.rows._insertAt(view._separator,2 * index + 2);
			}
		},
		
		updateRow: function(index, row) {
			if (index < 0 || index >= this.data.length) {
				return;
			}
			this.deleteRow(index);
			this.insertRowBefore(index,row);
		},
		
		doLayout: function() {
			
			// Update the row height info
			for (var i in this.data) {
				var row = this.data[i];
				if (isDef(row.declaredClass) && row.declaredClass == "Ti.UI.TableViewRow") {
					row._defaultHeight = this.rowHeight;
					set(row.domNode,'minHeight',this.minRowHeight);
					set(row.domNode,'maxHeight',this.maxRowHeight);
				}
			}
			
			View.prototype.doLayout.apply(this,arguments);
		},
		
		_defaultWidth: "100%",
		_defaultHeight: "100%",
		_getContentOffset: function(){
			return {x: this.domNode.scrollLeft, y: this.domNode.scrollTop};
		},
		
		_handleTouchEvent: function(type, e) {
			if (type === "click" || type === "singletap") {
				e.row = this._tableViewRowClicked;
				e.rowData = this._tableViewRowClicked;
				e.index = this.rows.children.indexOf(this._tableViewRowClicked);
				e.section = this._tableViewSectionClicked;
				e.searchMode = false;
			}
			View.prototype._handleTouchEvent.apply(this,arguments);
		},
		
		_tableViewRowClicked: null,
		_tableViewSectionClicked: null,
		
		properties: {
			data: {
				set: function(value) {
					if (is(value,'Array')) {
						
						// Remove all of the previous children
						for(var i in this.rows.children) {
							this.rows.remove(this.rows.children[i]);
						}
						
						// Convert any object literals to TableViewRow instances, and update TableViewRow instances with row info
						for (var i in value) {
							if (!isDef(value[i].declaredClass) || value[i].declaredClass != "Ti.UI.TableViewRow") {
								value[i] = Ti.UI.createTableViewRow(value[i]);
							}
						}
						
						// Add the first separator
						this.rows.add(this._createSeparator());
						
						// Add the new children
						for (var i in value) {
							this.rows.add(value[i]);
							value[i]._separator = this._createSeparator();
							this.rows.add(value[i]._separator);
						}
						
						// Relayout the screen
						Ti.UI._doFullLayout();
						
						return value;
					} else {
						// Data must be an array
						return;
					}
				}
			},
			footerTitle: {
				set: function(value, oldValue) {
					if (oldValue != value) {
						this.footerTitleControl && this.footer.remove(this.footerTitleControl);
						this.footerTitleControl = Ti.UI.createLabel({text: value});
						this.footer.add(this.footerTitleControl);
						Ti.UI._doFullLayout();
					}
					return value;
				}
			},
			footerView: {
				set: function(value, oldValue) {
					if (oldValue != value) {
						this.footerTitleControl && this.footer.remove(this.footerTitleControl);
						this.footerTitleControl = value;
						this.footer.add(this.footerTitleControl);
						Ti.UI._doFullLayout();
					}
					return value;
				}
			},
			headerTitle: {
				set: function(value, oldValue) {
					if (oldValue != value) {
						this.headerTitleControl && this.header.remove(this.headerTitleControl);
						this.headerTitleControl = Ti.UI.createLabel({text: value});
						this.header.add(this.headerTitleControl);
						Ti.UI._doFullLayout();
					}
					return value;
				}
			},
			headerView: {
				set: function(value, oldValue) {
					if (oldValue != value) {
						this.headerTitleControl && this.header.remove(this.headerTitleControl);
						this.headerTitleControl = value;
						this.header.add(this.headerTitleControl);
						Ti.UI._doFullLayout();
					}
					return value;
				}
			},
			maxRowHeight: "100%",
			minRowHeight: "0%",
			rowHeight: "50px",
			separatorColor: {
				set: function(value) {
					console.debug('Property "Titanium.UI.TableView#.separatorColor" is not implemented yet.');
					return value;
				},
				value: "lightGrey"
			},
			separatorStyle: {
				set: function(value) {
					console.debug('Property "Titanium.UI.TableView#.separatorStyle" is not implemented yet.');
					return value;
				}
			}
		}

	});

});