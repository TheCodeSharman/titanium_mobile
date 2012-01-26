define("Ti/UI/TableView", ["Ti/_/declare", "Ti/UI/View", "Ti/_/style", "Ti/_/lang","Ti/UI/MobileWeb/TableViewSeparatorStyle"], 
	function(declare, View, style, lang, TableViewSeparatorStyle) {

	var set = style.set,
		is = require.is,
		isDef = require.isDef;
		
	return declare("Ti.UI.TableView", View, {
		
		constructor: function(args) {
			
			// Create the parts out of Ti controls so we can make use of the layout system
			this.layout = 'vertical';
			set(this.domNode,"overflow-x","hidden");
			set(this.domNode,"overflow-y","auto");
			
			// Use horizontal layouts so that the default location is always (0,0)
			this.add(this._header = Ti.UI.createView({height: 'auto', layout: 'vertical'}));
			this.add(this._sections = Ti.UI.createView({height: 'auto', layout: 'vertical'}));
			this.add(this._footer = Ti.UI.createView({height: 'auto', layout: 'vertical'}));
			
			// Initializing to [] will also create the default section.
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
					contentOffset: {x: 0, y: this.domNode.scrollTop + this._header._measuredHeight},
					contentSize: {width: this._sections._measuredWidth, height: this._sections._measuredHeight},
					size: {width: this._measuredWidth, height: this._measuredHeight},
					x: e.x,
					y: e.y
				});
			});
			this.addEventListener("touchmove",lang.hitch(this,function(e) {
				this.domNode.scrollTop += previousTouchLocation - e.y;
				previousTouchLocation = e.y;
				
				this._fireScrollEvent(e.x,e.y);
			}));
			
			require.on(this.domNode,"scroll",lang.hitch(this,function(e){
				if (!this._touching) {
					this._fireScrollEvent();
				}
			}));
		},
		
		_fireScrollEvent: function(x,y) {
			// Calculate the visible items
			var firstVisibleItem,
				visibleItemCount = 0,
				scrollTop = this.domNode.scrollTop,
				sections = this._sections.children;
			for(var i = 0; i < sections.length; i+= 2) {
				
				// Check if the section is visible
				var section = sections[i],
					sectionOffsetTop = section._measuredTop - scrollTop,
					sectionOffsetBottom = section._measuredTop + section._measuredHeight - scrollTop;
				if (sectionOffsetBottom > 0 && sectionOffsetTop < this._measuredHeight) {
					
					var rows = section._rows.children
					for (var j = 1; j < rows.length; j += 2) {
						var row = rows[j],
							rowOffsetTop = row._measuredTop + section._measuredTop - scrollTop,
							rowOffsetBottom = row._measuredTop + row._measuredHeight + section._measuredTop - scrollTop;
						if (rowOffsetBottom > 0 && rowOffsetTop < this._measuredHeight) {
							visibleItemCount++;
							if (!firstVisibleItem) {
								firstVisibleItem = row;
							}
						}
					}
				}
			}
			
			// Create the scroll event
			this.fireEvent("scroll",{
				contentOffset: {x: 0, y: this.domNode.scrollTop},
				contentSize: {width: this._sections._measuredWidth, height: this._sections._measuredHeight},
				firstVisibleItem: firstVisibleItem,
				size: {width: this._measuredWidth, height: this._measuredHeight},
				totalItemCount: this.data.length,
				visibleItemCount: visibleItemCount,
				x: x,
				y: y
			});
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
		
		_createSeparator: function() {
			return Ti.UI.createView({
				height: 1,
				width: "100%",
				backgroundColor: "white"
			});
		},
		
		_createDecorationLabel: function(text) {
			return Ti.UI.createLabel({
				text: text, 
				backgroundColor: this.separatorColor,
				color: "white",
				width: "100%",
				height: "auto",
				left: 0,
				font: {fontSize: 22}
			});
		},
		
		_refreshSections: function() {
			for (var i = 0; i < this._sections.children.length; i += 2) {
				this._sections.children[i]._refreshRows();
			}
			Ti.UI._doFullLayout();
		},
		
		_calculateLocation: function(index) {
			var currentOffset = 0,
				section;
			for(var i = 0; i < this._sections.children.length; i += 2) {
				section = this._sections.children[i];
				currentOffset += section.rowCount;
				if (index < currentOffset) {
					return {
						section: section,
						localIndex: section.rowCount - (currentOffset - index)
					};
				}
			}
			
			// Handle the special case of inserting after the last element in the last section
			if (index == currentOffset) {
				return {
					section: section,
					localIndex: section.rowCount
				};
			}
		},
		
		_insert: function(value, index) {
			var location = this._calculateLocation(index);
			if (location) {
				location.section.add(value,location.localIndex);
			}
			this._refreshSections();
		},
		
		_remove: function(index) {
			var location = this._calculateLocation(index);
			if (location) {
				location.section._remove(location.localIndex);
			}
		},

		appendRow: function(value) {
			this._currentSection.add(value);
			this._refreshSections();
		},
		
		deleteRow: function(index) {
			this._remove(index);
		},
		
		insertRowAfter: function(index, value) {
			this.add(value, index + 1);
		},
		
		insertRowBefore: function(index, value) {
			this.add(value, index);
		},
		
		updateRow: function(index, row) {
			this._remove(index);
			this.add(value, index);
		},
		
		scrollToIndex: function(index) {
			var location = this._calculateLocation(index);
			if (location) {
				this.domNode.scrollTop = location.section._measuredTop + location.section._rows.children[2 * location.localIndex + 1]._measuredTop;
			}
		},
		
		scrollToTop: function(top) {
			this.domNode.scrollTop = top;
		},
		
		properties: {
			data: {
				set: function(value) {
					if (is(value,'Array')) {
						
						// Remove all of the previous sections
						this._sections._removeAllChildren();
						
						// Convert any object literals to TableViewRow instances, and update TableViewRow instances with row info
						for (var i in value) {
							if (!isDef(value[i].declaredClass) || (value[i].declaredClass != "Ti.UI.TableViewRow" && value[i].declaredClass != "Ti.UI.TableViewSection")) {
								value[i] = Ti.UI.createTableViewRow(value[i]);
							}
						}
			
						// Add each element
						for (var i = 0; i < value.length; i++) {
							if (value[i].declaredClass === "Ti.UI.TableViewRow") {
								// Check if the first item is a row, meaning we need a default section
								if (i === 0) {
									this._currentSection = Ti.UI.createTableViewSection({_tableView: this});
									this._sections.add(this._currentSection);
									this._sections.add(this._createSeparator());
								}
								this._currentSection.add(value[i]);
							} else if (value[i].declaredClass === "Ti.UI.TableViewSection") {
								this._currentSection = value[i];
								this._currentSection._tableView = this;
								this._sections.add(this._currentSection);
								this._sections.add(this._createSeparator());
							}
						}
						this._refreshSections();
						
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
						this._footer._removeAllChildren();
						this._footer.add(this._createDecorationLabel(value));
					}
					return value;
				}
			},
			footerView: {
				set: function(value, oldValue) {
					if (oldValue != value) {
						this._footer._removeAllChildren();
						this._footer.add(value);
					}
					return value;
				}
			},
			headerTitle: {
				set: function(value, oldValue) {
					if (oldValue != value) {
						this._header._removeAllChildren();
						this._header.add(this._createDecorationLabel(value));
						this._header.add(this._createSeparator());
					}
					return value;
				}
			},
			headerView: {
				set: function(value, oldValue) {
					if (oldValue != value) {
						this._header._removeAllChildren();
						this._header.add(value);
					}
					return value;
				}
			},
			maxRowHeight: {
				post: function(value) {
					this._refreshSections();
					return value;
				},
				value: "100%"
			},
			minRowHeight: {
				post: function(value) {
					this._refreshSections();
					return value;
				},
				value: "0%"
			},
			rowHeight: {
				post: function(value) {
					this._refreshSections();
					return value;
				},
				value: "50px"
			},
			separatorColor: {
				post: function(value) {
					this._refreshSections();
					return value;
				},
				value: "lightGrey"
			},
			separatorStyle: {
				post: function(value) {
					this._refreshSections();
					return value;
				},
				value: TableViewSeparatorStyle.SINGLE_LINE
			}
		}

	});

});