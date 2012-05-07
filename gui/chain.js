// Holder for all chain tabs (JQuery-UI object)
var chaintabs = null;

// Container for all the CXF chains that are currently displayed in their respective tabs
var drawnChains = new Object();

// Global parameters
var nodeSize = 50;

// Helpers
var dtoDropped;

function initChainPanel() {
	// Create the tab panel with a template that will create a host for a Raphaël object
	chaintabs = $("#chaintabs").tabs({
		tabTemplate : "<li><a href='#{href}'>#{label}</a></li>", // <span class='ui-icon ui-icon-close'>Remove Tab</span></li>",
		add : function(event, ui) {
			var tab_content = "houba";
			$(ui.panel).append("<div class='raph'></div>");
		}
	});
}

function fillInPaletteData(DTOShellCommandArray) {
	// Palette: list of shell commands
	var options = {
		editable : false,
		enableAddRow : false,
		enableCellNavigation : true,
		enableColumnReorder : false,
		enableRowReordering : false,
		asyncEditorLoading : true,
		showHeaderRow : false, // Weird
		multiSelect : false,
		enableTextSelectionOnCells : false, // ???
		rowHeight : 30,
		autoHeight : true,
		autoEdit : false,
		forceFitColumns : true
	};

	var columns = [{
		id : "name",
		name : "Commands",
		field : "_name",
		width : 200,
		cssClass : "cell-title"
	}];
	grid = new Slick.Grid("#dgChainPaletteCommands", cxfShellCommands, columns, options);
	grid.setSelectionModel(new Slick.RowSelectionModel());

	grid.onDragInit.subscribe(function(e, dd) {
		// prevent the grid from canceling drag'n'drop by default
		e.stopImmediatePropagation();
	});

	grid.onDragStart.subscribe(function(e, dd) {
		var cell = grid.getCellFromEvent(e);
		if(!cell) {
			return;
		}

		// Store row ID
		dd.row = cell.row;
		dtoDropped = cxfShellCommands[dd.row];

		if(Slick.GlobalEditorLock.isActive()) {// ?
			return;
		}

		// prevent default canceling behaviour
		e.stopImmediatePropagation();

		var selectedRows = grid.getSelectedRows();
		if(!selectedRows.length || $.inArray(dd.row, selectedRows) == -1) {
			selectedRows = [dd.row];
			grid.setSelectedRows(selectedRows);
		}

		dd.rows = selectedRows;
		dd.count = selectedRows.length;

		var proxy = $("<span></span>").css({
			position : "absolute",
			display : "inline-block",
			padding : "4px 10px",
			background : "#e0e0e0",
			border : "1px solid gray",
			"z-index" : 99999,
			"-moz-border-radius" : "8px",
			"-moz-box-shadow" : "2px 2px 6px silver"
		}).text("Drag to drawing to create " + dd.count + " node(s)").appendTo("body");

		dd.helper = proxy;

		$(dd.available).css("background", "yellow");
		return proxy;
	});

	grid.onDrag.subscribe(function(e, dd) {
		e.stopImmediatePropagation();
		dd.helper.css({
			top : e.pageY + 5,
			left : e.pageX + 5
		});
	});

	grid.onDragEnd.subscribe(function(e, dd) {
		e.stopImmediatePropagation();
		dd.helper.remove();
		$(dd.available).css("background", "beige");
	});
	// register drop event handlers on all SVG charts (including future ones thanks to delegation)
	$.drop({
		mode : "mouse"
	});
	$("#chaintabs").on("dropstart", ".raph", function(e, dd) {
		alert("e");
	})

	$("#chaintabs").on("drop", ".raph", function(e, dd) {
		var v = new dto_chronix_oxymores_org_DTOState();
		v._id = uuid.v4();
		v._representsId = dtoDropped._id;
		v._x = e.screenX - $(this).offset().left;
		v._y = e.screenY - $(this).offset().top;
		;
		v._label = dtoDropped._name;

		// Get selected tab
		var selected = chaintabs.tabs(chaintabs.tabs('option', 'selected'));
		var paper = this.paper;
		addState(v, paper);
		var dtoChain = this.dtoChain;
		dtoChain.getStates().getDTOState().push(v);

		//alert("f");
	})

	$("#chaintabs").on("dropend", ".raph", function(e, dd) {
		//alert("g");
	})
}

// Function to call to add a tab displaying a given DTOChain
function editChain(cxfObject) {
	// Check if already displayed
	if(drawnChains[cxfObject._id] !== undefined)
		return;

	// Adds the chain to the list
	drawnChains[cxfObject._id] = cxfObject;

	// Create a new tab
	var ta = chaintabs.tabs("add", "#chaintab-" + cxfObject._id, cxfObject._name);
	var r = $("div.raph", $("#chaintab-" + cxfObject._id))[0];
	var rpaper = new Raphael(r, 1600, 600);
	rpaper.states = new Array();
	rpaper.transitions = new Array();
	r.paper = rpaper;
	r.dtoChain = cxfObject;
	drawChain(cxfObject, rpaper);
}

function dragStateStart(x, y, event) {
	this.xs = this.attr("cx");
	this.ys = this.attr("cy");
}

function dragStateMove(dx, dy, x, y, event) {
	var newx = this.xs + dx;
	var newy = this.ys + dy;

	// Move state
	this.attr({
		cx : newx
	});
	this.attr({
		cy : newy
	});

	// Update state model
	this.modeldata._x = this.xs + dx;
	this.modeldata._y = this.ys + dy;

	// Drag transition ends
	for(var i = 0; i < this.modeldata.trFromHere.length; i++) {
		var tr = this.modeldata.trFromHere[i];
		shiftArrow(tr);
	}
	for(var i = 0; i < this.modeldata.trReceivedHere.length; i++) {
		var tr = this.modeldata.trReceivedHere[i];
		shiftArrow(tr);
	}

	// Drag state contents (text, and so on)
	for(var e in this.contents) {
		this.contents[e].attr({
			x : newx,
			y : newy
		});
	}
}

function shiftArrow(RaphaelArrow) {
	var pc = "M" + RaphaelArrow.from._x + "," + RaphaelArrow.from._y + ",L" + RaphaelArrow.to._x + "," + RaphaelArrow.to._y;
	var shifted1 = Raphael.getPointAtLength(pc, nodeSize);
	var shifted2 = Raphael.getPointAtLength(pc, Raphael.getTotalLength(pc) - nodeSize);

	var path = RaphaelArrow.attr("path");
	path[0][1] = shifted1.x;
	path[0][2] = shifted1.y;
	path[1][1] = shifted2.x;
	path[1][2] = shifted2.y;
	RaphaelArrow.attr({
		"path" : path
	});
}

function dragStateEnd(event) {

}

function drawChain(cxfChain, raphaelPaper) {
	// Reinitialize panel
	raphaelPaper.clear();
	raphaelPaper.states = new Array();
	raphaelPaper.transitions = new Array();

	// add chain states to the list and draw them
	var ss = cxfChain.getStates().getDTOState();
	for(var i = 0; i < ss.length; i++) {
		addState(ss[i], raphaelPaper);
	}

	// add transitions to the list and draw them
	var trs = cxfChain.getTransitions().getDTOTransition();
	for(var i = 0; i < trs.length; i++) {
		addTransition(trs[i], raphaelPaper);
	}
}

function getChainOK(responseObject) {
	drawChain(responseObject.getReturn());
}

function addState(DTOState, raphaelPaper) {
	// Add state to list, both as member of the object (access by id) and to the
	// list itself (enumerator access)
	raphaelPaper.states[DTOState._id] = DTOState;
	raphaelPaper.states.push(DTOState);

	// Draw state as a circle
	var circle = raphaelPaper.circle(DTOState._x, DTOState._y, 50);
	circle.attr({
		fill : 'firebrick',
		stroke : 'darkorchid',
		'stroke-width' : 2
	});
	var tx = raphaelPaper.text(DTOState._x, DTOState._y, DTOState._label + "\n(" + DTOState._runsOnName + ")");
	circle.contents = new Object();
	circle.contents["text"] = tx;

	// Set reference between business model object and representation object
	DTOState._drawing = circle;
	circle.modeldata = DTOState;
	circle.node.id = DTOState._id;
	circle.id = DTOState._id;
	DTOState.trFromHere = new Array();
	DTOState.trReceivedHere = new Array();

	// Allow dragging with callback taking connections and business objects into
	// account
	circle.drag(dragStateMove, dragStateStart, dragStateEnd);
}

function addTransition(DTOTransition, raphaelPaper) {
	// Add to collection for future reference
	raphaelPaper.transitions[DTOTransition._id] = DTOTransition;
	raphaelPaper.transitions.push(DTOTransition);

	// Draw as an arrow
	var from = raphaelPaper.states[DTOTransition._from];
	var to = raphaelPaper.states[DTOTransition._to];
	var x1 = from._x;
	var y1 = from._y;
	var x2 = to._x;
	var y2 = to._y;

	var pc = "M" + x1 + "," + y1 + ",L" + x2 + "," + y2;
	var shifted1 = Raphael.getPointAtLength(pc, nodeSize);
	var shifted2 = Raphael.getPointAtLength(pc, Raphael.getTotalLength(pc) - nodeSize);
	var arrow = raphaelPaper.path("M" + shifted1.x + "," + shifted1.y + ",L" + shifted2.x + "," + shifted2.y);

	arrow.attr({
		"arrow-end" : "classic-wide-long",
		"stroke-width" : 5
	});

	// Set cross references to allow easy navigation between SVG and model
	// elements
	from.trFromHere.push(arrow);
	to.trReceivedHere.push(arrow);
	arrow.from = from;
	arrow.to = to;
	DTOTransition._drawing = arrow;
	arrow.modeldata = DTOTransition;
}

function getChainKO(responseObject) {
	alert("oooops");
}

function loadChain() {
	proxy.getChain(getChainOK, getChainKO);
}