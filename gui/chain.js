// Global parameters
nodeSize = 30;
arrowSize = 3;

function ChainPanel(divId, cxfApplication)
{
	this.cxfApplication = cxfApplication;
	
	// Holder for all chain tabs (JQuery-UI object)
	this.chaintabs = null;
	
	// Container for all the CXF chains that are currently displayed in their respective tabs
	this.drawnChains = new Object();
	
	
	
	// Helpers
	this.dtoDropped;
	
	// Create the tab panel with a template that will create a host for a Raphaël object
	chaintabs = $("#chaintabs").tabs();
	
	// Palette: list of shell commands
	var options =
	{
		editable : false,
		enableAddRow : false,
		enableCellNavigation : false,
		enableColumnReorder : false,
		enableRowReordering : false,
		asyncEditorLoading : false,
		showHeaderRow : false,
		multiSelect : false,
		autoEdit : false,
		enableTextSelectionOnCells : true,
		autoHeight : false,
		forceFitColumns : true,
		fullWidthRows : true,
		explicitInitialization : false,
		syncColumnCellResize : true,
	};

	var columns = [{
		id : "name",
		name : "Commands",
		field : "_name",
		width : 200,
		cssClass : "cell-title"
	}];
	grid = new Slick.Grid("#dgChainPaletteCommands", this.cxfApplication.getShells().getDTOShellCommand(), columns, options);
	grid.setSelectionModel(new Slick.RowSelectionModel());
	
	grid2 = new Slick.Grid("#dgChainPaletteChains", this.cxfApplication.getChains().getDTOChain(), [{id : "name", name : "Chains", field : "_name", width : 200, cssClass : "cell-title"}], options);
	grid2.setSelectionModel(new Slick.RowSelectionModel());
	
	// Drag&Drop
	grid.onDragInit.subscribe(function(e, dd) 
	{
		// prevent the grid from canceling drag'n'drop by default
		e.stopImmediatePropagation();
	});
	
	grid.onDragStart.subscribe(function(e, dd) 
	{
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
	});
	
	$("#chaintabs").on("drop", ".raph", function(e, dd) {
		var v = new dto_chronix_oxymores_org_DTOState();
		v._id = uuid.v4();
		v._representsId = dtoDropped._id;
		v._x = e.screenX - $(this).offset().left;
		v._y = e.screenY - $(this).offset().top;
		;
		v._label = dtoDropped._name;

		// Get selected tab
		//var selected = chaintabs.tabs(chaintabs.tabs('option', 'selected'));
		var paper = this.paper;
		addState(v, paper);
		var dtoChain = this.dtoChain;
		dtoChain.getStates().getDTOState().push(v);

		//alert("f" + paper + v._label + "X : " + v._x + " - " + v._y);
	});
	
	$("#chaintabs").on("dropend", ".raph", function(e, dd) {
		//alert("g");
	});
	
	// Register edit chain hook
	grid2.onDblClick.subscribe((function(e, args)
	{marsu = args;
	    var row = args.row;
		var item = grid2.getDataItem(row);
		this.editChain(item);
	}).bind(this));
	
	this.editChain(this.cxfApplication.getChains().getDTOChain()[0]);
}


// Function to call to add a tab displaying a given DTOChain
ChainPanel.prototype.editChain = function(cxfObject) 
{
	// Check if already displayed
	if(this.drawnChains[cxfObject._id] !== undefined)
	{
		$("[href='#chaintab-"+cxfObject._id+"']").trigger( "click" );
		return;
	}
	
	// Adds the chain to the list
	this.drawnChains[cxfObject._id] = cxfObject;

	// Create a new tab
	$("#chaintabs").append("<div class='tabPanel' id='chaintab-" + cxfObject._id + "'><div style='height:100%;width:100%;' id='raphcontainer_"+cxfObject._id+"'><div class='raph'></div></div></div>");
	$("#chaintabs > ul").append( "<li><a href='" + "#chaintab-" + cxfObject._id + "'>" + cxfObject._name + "</a></li>");
	chaintabs.tabs('refresh');
	$("[href='#chaintab-"+cxfObject._id+"']").trigger( "click" );
	
	var r = $("#chaintab-" + cxfObject._id + " div.raph")[0];
	var rpaper = new Raphael(r, 10 + $("#raphcontainer_" + cxfObject._id).width(), 10 + $("#raphcontainer_" + cxfObject._id).height());
	rpaper.states = new Array();
	rpaper.transitions = new Array();
	r.paper = rpaper;
	r.dtoChain = cxfObject;
	this.drawChain(cxfObject, rpaper);
	
};

// Actually draws the chain inside a clean Raphael paper
ChainPanel.prototype.drawChain = function(cxfChain, raphaelPaper) 
{
	// Reinitialize panel
	raphaelPaper.clear();
	raphaelPaper.states = new Array();
	raphaelPaper.transitions = new Array();

	// add chain states to the list and draw them
	var ss = cxfChain.getStates().getDTOState();
	for(var i = 0; i < ss.length; i++) {
		this.addState(ss[i], raphaelPaper);
	}

	// add transitions to the list and draw them
	var trs = cxfChain.getTransitions().getDTOTransition();
	for(var i = 0; i < trs.length; i++) {
		this.addTransition(trs[i], raphaelPaper);
	}
};


ChainPanel.prototype.addState = function (DTOState, raphaelPaper) 
{
	// Add state to list, both as member of the object (access by id) and to the
	// list itself (enumerator access)
	raphaelPaper.states[DTOState._id] = DTOState;
	raphaelPaper.states.push(DTOState);

	// Draw state as a circle
	var circle = raphaelPaper.circle(DTOState._x, DTOState._y, nodeSize);
	circle.attr({
		fill : 'firebrick',
		stroke : 'darkorchid',
		'stroke-width' : 2
	});
	tx = raphaelPaper.text(DTOState._x, DTOState._y, DTOState._label + "\n(" + DTOState._runsOnName + ")");
	tx.node.setAttribute("pointer-events", "none");
	circle.contents = new Object();
	circle.contents["text"] = tx;

	// Set reference between business model object and representation object
	DTOState._drawing = circle;
	circle.modeldata = DTOState;
	circle.node.id = DTOState._id;
	circle.id = DTOState._id;
	DTOState.trFromHere = new Array();
	DTOState.trReceivedHere = new Array();

	// Allow dragging with callback taking connections and business objects into account
	circle.drag(dragStateMove, dragStateStart, dragStateEnd);
};

ChainPanel.prototype.addTransition = function(DTOTransition, raphaelPaper) 
{
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
		"stroke-width" : arrowSize
	});

	// Set cross references to allow easy navigation between SVG and model elements
	from.trFromHere.push(arrow);
	to.trReceivedHere.push(arrow);
	arrow.from = from;
	arrow.to = to;
	DTOTransition._drawing = arrow;
	arrow.modeldata = DTOTransition;
};

	
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

function dragStateEnd(event) {

}

function shiftArrow(RaphaelArrow) 
{
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



