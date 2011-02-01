var win = Ti.UI.currentWindow;

function makeNameRows() {
	var names = ['Joanie', 'Mickey', 'Jean Pierre', 'Gustav', 'Raul', 'Mimi', 'Emily', 'Sandra', 'Carrie', 'Chachi'];
	var rows = [];
	for (var i = 0; i < names.length; i++) {
		rows.push(Ti.UI.createPickerRow({title: names[i]}));
	}
	return rows;
}

function makeVerbRows() {
	var verbs = ['loves', 'likes', 'visits', 'loathes', 'waves to', 'babysits', 'accompanies', 'teaches', 'announces', 'supports', 'knows', 'high-fives'];
	var rows = [];
	for (var i = 0; i < verbs.length; i++) {
		rows.push(Ti.UI.createPickerRow({title: verbs[i]}));
	}
	return rows;
}

function makeNameColumn() {
	return Ti.UI.createPickerColumn({rows: makeNameRows()});
}

function makeVerbColumn() {
	return Ti.UI.createPickerColumn({rows: makeVerbRows()});
}

win.add(Ti.UI.createLabel({
	backgroundColor: 'yellow',
	color: 'blue',
	left: 0, right: 0, top: 0, height: 35,
	font: {fontWeight: 'bold'},
	text: "SOME LAYOUT EXAMPLES",
	textAlign: Ti.UI.TEXT_ALIGNMENT_CENTER
}));
var l = Ti.UI.createLabel({
	text: 'Defaults:',
	left: 5, top: 40, height: 35
});
win.add(l);

var picker = Ti.UI.createPicker({
	top: 80, 
	columns: [makeNameColumn(), makeVerbColumn(), makeNameColumn()],
	useSpinner: true
});
win.add(picker);

win.add(Ti.UI.createLabel({
	text: 'visibleItems: 3, explicit col. widths, selectionIndicator: false',
	left: 5, top: 200, height: 35,
	font: {fontSize: 12}
}));

var nameColumn1 = makeNameColumn();
nameColumn1.width = 100;
var nameColumn2 = makeNameColumn();
nameColumn2.width = 100;
var verbColumn = makeVerbColumn();
verbColumn.width = 75;
win.add(Ti.UI.createPicker({
	top: 240,
	useSpinner: true,
	columns: [nameColumn1, verbColumn, nameColumn2],
	visibleItems: 3,
	selectionIndicator: false
}));

win.add(Ti.UI.createLabel({
	left: 5, top: 300, height: 35,
	text: "font size/weight/family + color"
}));

nameColumn1 = makeNameColumn();
nameColumn1.font = {fontSize: 10, fontFamily: 'serif'};
nameColumn1.color = "red";
nameColumn2 = makeNameColumn();
nameColumn2.font = {fontSize: 10, fontFamily: 'sans-serif', fontWeight: 'bold'};
nameColumn2.color = "purple";
verbColumn = makeVerbColumn();
verbColumn.font = {fontSize: 10, fontFamily: 'serif', fontWeight: 'bold'};
verbColumn.color = "blue";

win.add(Ti.UI.createPicker({
	top: 340, useSpinner: true,
	columns: [nameColumn1, verbColumn, nameColumn2]
}));


