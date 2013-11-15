// Global parameters
nodeSize = 30;
arrowSize = 5;

function ChainPanel(divId, cxfApplication)
{
	this.cxfApplication = cxfApplication;

	// Holder for all chain tabs (JQuery-UI object)
	this.chaintabs = null;

	// Container for all the CXF chains that are currently displayed in their respective tabs
	this.drawnChains = new Object();

	// Helpers
	this.selectedChain = null;
	this.selectedPaper = null;
	this.state_toolbar = new StateToolbar();
	this.transition_toolbar = new TransitionToolbar();

	// Create the tab panel with a template that will create a host for a Raphaël object
	this.chaintabs = $("#chaintabs").tabs(
	{
		activate : this.onShowChain.bind(this)
	});
	this.panel = $("#container1");
	this.panel.data('app', cxfApplication);
	this.panel.data('panel', this);

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

	var columns = [
	{
		id : "name",
		name : "Commands",
		field : "_name",
		width : 200,
		cssClass : "cell-title"
	} ];
	this.grid = new Slick.Grid("#dgChainPaletteCommands", this.cxfApplication.getShells().getDTOShellCommand(), columns, options);
	this.grid.setSelectionModel(new Slick.RowSelectionModel());

	this.grid2 = new Slick.Grid("#dgChainPaletteChains", this.cxfApplication.getChains().getDTOChain(), [
	{
		id : "name",
		name : "Chains",
		field : "_name",
		width : 200,
		cssClass : "cell-title"
	} ], options);
	this.grid2.setSelectionModel(new Slick.RowSelectionModel());

	this.grid3 = new Slick.Grid("#dgChainPaletteClocks", this.cxfApplication.getClocks().getDTOClock(), [
	{
		id : "name",
		name : "Clocks",
		field : "_name",
		width : 200,
		cssClass : "cell-title"
	} ], options);
	this.grid3.setSelectionModel(new Slick.RowSelectionModel());

	this.grid4 = new Slick.Grid("#dgChainPaletteExternals", this.cxfApplication.getExternals().getDTOExternal(), [
	{
		id : "name",
		name : "Externals",
		field : "_name",
		width : 200,
		cssClass : "cell-title"
	} ], options);
	this.grid4.setSelectionModel(new Slick.RowSelectionModel());

	// Drag&Drop
	var activeDragStart = function(e, dd)
	{
		var cell = this.getCellFromEvent(e);
		if (!cell)
		{
			return;
		}

		// Store row ID
		dd.row = cell.row;
		daddy = $(this.getContainerNode()).parents('div[class="panelcontainer"]');
		dd.dtoDropped = this.getDataItem(dd.row);
		dd.panel = daddy.data('panel');

		if (Slick.GlobalEditorLock.isActive())
		{// ?
			return;
		}

		// Prevent default canceling behaviour
		e.stopImmediatePropagation();

		var selectedRows = this.getSelectedRows();
		if (!selectedRows.length || $.inArray(dd.row, selectedRows) == -1)
		{
			selectedRows = [ dd.row ];
			this.setSelectedRows(selectedRows);
		}

		dd.rows = selectedRows;
		dd.count = selectedRows.length;

		var proxy = $("<span></span>").css(
		{
			position : "absolute",
			display : "inline-block",
			padding : "4px 10px",
			background : "#e0e0e0",
			border : "1px solid gray",
			"z-index" : 99999,
			"-moz-border-radius" : "8px",
			"-moz-box-shadow" : "2px 2px 6px silver"
		}).text("Drag to drawing to create " + dd.count + " node(s)").appendTo("body"); // container1

		$(dd.available).css("background", "green");
		return proxy;
	};
	var activeDragInit = function(e, dd)
	{
		// Prevent the grid from canceling drag'n'drop by default
		e.stopImmediatePropagation();
		dd.selectedChain = this.selectedChain;
		dd.selectedPaper = this.selectedPaper;
	};
	var activeDrag = function(e, dd)
	{
		// e.stopImmediatePropagation();
		$(dd.proxy).css(
		{
			top : e.pageY + 5,
			left : e.pageX + 5
		});
	};
	var activeDragEnd = function(e, dd)
	{
		// e.stopImmediatePropagation();
		dd.proxy.remove();
		$(dd.available).css("background", "beige");
	};

	this.grid.onDragInit.subscribe(activeDragInit.bind(this));
	this.grid.onDragStart.subscribe(activeDragStart);
	this.grid.onDrag.subscribe(activeDrag);
	this.grid.onDragEnd.subscribe(activeDragEnd);

	this.grid2.onDragInit.subscribe(activeDragInit.bind(this));
	this.grid2.onDragStart.subscribe(activeDragStart);
	this.grid2.onDrag.subscribe(activeDrag);
	this.grid2.onDragEnd.subscribe(activeDragEnd);

	this.grid3.onDragInit.subscribe(activeDragInit.bind(this));
	this.grid3.onDragStart.subscribe(activeDragStart);
	this.grid3.onDrag.subscribe(activeDrag);
	this.grid3.onDragEnd.subscribe(activeDragEnd);

	this.grid4.onDragInit.subscribe(activeDragInit.bind(this));
	this.grid4.onDragStart.subscribe(activeDragStart);
	this.grid4.onDrag.subscribe(activeDrag);
	this.grid4.onDragEnd.subscribe(activeDragEnd);

	$("#chainPaletteAnd").bind("draginit", (function(e, dd)
	{
		dd.selectedChain = this.selectedChain;
		dd.selectedPaper = this.selectedPaper;

		daddy = $('#container1');
		dd.panel = daddy.data('panel');
		dd.and = true;
	}).bind(this));
	$("#chainPaletteOr").bind("draginit", (function(e, dd)
	{
		dd.selectedChain = this.selectedChain;
		dd.selectedPaper = this.selectedPaper;

		daddy = $('#container1');
		dd.panel = daddy.data('panel');
		dd.or = true;
	}).bind(this));

	// Register drop event handlers on all SVG charts (including future ones thanks to delegation)
	$("#chaintabs").on("dropstart", "svg", function(e, dd)
	{
		// $(this).parent().css("background", "red");
	});
	$("#chaintabs").on("dropend", "svg", function(e, dd)
	{
		// $(this).parent().css("background", "green");
	});

	$("#chaintabs").on("drop", "svg", (function(e, dd)
	{
		var v = new dto_chronix_oxymores_org_DTOState();
		v._id = uuid.v4();
		v._x = e.offsetX;
		v._y = e.offsetY;
		v._canReceiveLink = true;
		v._canEmitLinks = true;
		v._canBeRemoved = true;

		if (dd.and)
		{
			v._isAnd = true;
			v._label = "AND";
			v._canReceiveMultipleLinks = true;
		}
		else if (dd.or)
		{
			v._isOr = true;
			v._label = "OR";
			v._canReceiveMultipleLinks = true;
		}
		else if (dd.dtoDropped._regularExpression)
		{
			v._representsId = dd.dtoDropped._id;
			v._label = dd.dtoDropped._name;
			v._canReceiveLink = false;
		}
		else if (dd.dtoDropped._nextOccurrences)
		{
			v._representsId = dd.dtoDropped._id;
			v._label = dd.dtoDropped._name;
			v._canReceiveLink = false;
		}
		else
		{
			v._representsId = dd.dtoDropped._id;
			v._label = dd.dtoDropped._name;
		}

		// Default PlaceGroup
		if (dd.selectedChain.getStates().getDTOState().length > 0)
		{
			v._runsOnId = dd.selectedChain.getStates().getDTOState()[0]._runsOnId;
			v._runsOnName = dd.selectedChain.getStates().getDTOState()[0]._runsOnName;
		}

		// Add it and draw it
		dd.panel.addState(v, dd.selectedPaper);
		dd.selectedChain.getStates().getDTOState().push(v);

		this.state_toolbar.show(v);
	}).bind(this));

	$.drop(
	{
		mode : "mouse", // everything inside target to be accepted,
		delay : 2
	});

	// Register edit chain hook
	this.grid2.onDblClick.subscribe((function(e, args)
	{
		var row = args.row;
		var item = this.grid2.getDataItem(row);
		this.editChain(item);
	}).bind(this));

	// Draw the first chain
	this.editChain(this.cxfApplication.getChains().getDTOChain()[0]);

	// Add create chain button
	var btAddChain = $("<li style='border-radius: 50%; padding: 1px; margin-right: 5px; height: 20px; width: 20px; border: solid 1px white; text-align: center; vertical-align: middle;'>+</li>");
	$("#chaintabs .ui-tabs-nav").prepend(btAddChain);
	btAddChain.click((function()
	{
		var v = new dto_chronix_oxymores_org_DTOChain();
		v._id = uuid.v4();
		v._name = "new chain";
		v._description = "new chain description";
		v._states = new dto_chronix_oxymores_org_ArrayOfDTOState();
		v._transitions = new dto_chronix_oxymores_org_ArrayOfDTOTransition();

		var s = new dto_chronix_oxymores_org_DTOState();
		s._id = uuid.v4();
		s._representsId = null; // Will be completed by the Frontier
		s._x = 100;
		s._y = 50;
		s._label = "Start";
		s._canReceiveLink = false;
		s._canEmitLinks = true;
		s._canBeRemoved = false;
		s._end = false;
		s._start = true;
		s._runsOnId = this.cxfApplication.getGroups().getDTOPlaceGroup()[0]._id;
		s._runsOnName = this.cxfApplication.getGroups().getDTOPlaceGroup()[0]._name;

		var e = new dto_chronix_oxymores_org_DTOState();
		e._id = uuid.v4();
		e._representsId = null; // Will be completed by the Frontier
		e._x = 100;
		e._y = 300;
		e._label = "End";
		e._canReceiveLink = true;
		e._canEmitLinks = false;
		e._canBeRemoved = false;
		e._end = true;
		e._start = false;
		e._runsOnId = this.cxfApplication.getGroups().getDTOPlaceGroup()[0]._id;
		e._runsOnName = this.cxfApplication.getGroups().getDTOPlaceGroup()[0]._name;

		v.getStates().getDTOState().push(s);
		v.getStates().getDTOState().push(e);

		this.cxfApplication.getChains().getDTOChain().push(v);
		this.editChain(v);
	}).bind(this));
}

// Function to call to add a tab displaying a given DTOChain
ChainPanel.prototype.editChain = function(cxfObject)
{
	// Check if already displayed
	if (this.drawnChains[cxfObject._id] !== undefined)
	{
		$("[href='#chaintab-" + cxfObject._id + "']").trigger("click");
		return;
	}

	// Adds the chain to the list
	this.drawnChains[cxfObject._id] = cxfObject;

	// Create a new tab
	$("#chaintabs").append(
			"<div class='tabPanel' id='chaintab-" + cxfObject._id + "'>" + "<div style='height:100%;width:100%;' id='raphcontainer_" + cxfObject._id
					+ "'>" + "<div class='raph' style='display: inline-block; overflow: auto; width: calc(100% - 150px); height:100%;'></div>"
					+ "<div id='chainDetail-" + cxfObject._id
					+ "' style='width: 150px; border=solid 2px black; display: inline-block; vertical-align: top;'></div></div></div>");
	$("#chaintabs > ul").append("<li><a href='" + "#chaintab-" + cxfObject._id + "'>" + cxfObject._name + "</a></li>");

	// Create edit fields
	var editPanel = $("#chainDetail-" + cxfObject._id);
	editPanel.append("<label>Chain name</label>");
	editPanel.append($("<input></input>").val(cxfObject._name).change((function(event)
	{
		this.selectedChain._name = $(event.currentTarget).val();
		$("a[href='#chaintab-" + this.selectedChain._id + "']").text(this.selectedChain._name);
	}).bind(this)));
	editPanel.append("<br><label>Chain description</label>");
	editPanel.append($("<input></input>").val(cxfObject._description).change((function(event)
	{
		this.selectedChain._description = $(event.currentTarget).val();
	}).bind(this)));
	editPanel.append($("<button id='btDelChain" + cxfObject._id + "' >Supprimer la chaine</button>").click(this.onDeleteChain.bind(this)));

	// Create new Raphaël paper
	var r = $("#chaintab-" + cxfObject._id + " div.raph")[0];
	var rpaper = new Raphael(r, 3000, 3000);
	rpaper.states = new Array();
	rpaper.transitions = new Array();
	r.paper = rpaper;
	r.dtoChain = cxfObject;

	// Draw the chain on the paper
	this.drawChain(cxfObject, rpaper);

	// event handlers
	$(rpaper.canvas).click((function(event)
	{
		event.stopPropagation();
		this.state_toolbar.hide();
		this.transition_toolbar.hide();
	}).bind(this));

	// Show new tab
	this.chaintabs.tabs('refresh');
	$("[href='#chaintab-" + cxfObject._id + "']").trigger("click");
};

ChainPanel.prototype.redisplay = function()
{
	this.grid.invalidate();
	this.grid2.invalidate();
};

ChainPanel.prototype.onDeleteChain = function(event)
{
	var chainId = event.target.id.substr(10);
	$("#chaintab-" + chainId).remove();
	$("#chaintabs > ul li a[href='#chaintab-" + chainId + "'").remove();
	this.chaintabs.tabs('refresh');
	this.cxfApplication.getChains().getDTOChain().splice(this.cxfApplication.getChains().getDTOChain().indexOf(this.selectedChain), 1);
	this.selectedChain = null;
	this.grid2.invalidate();
};

ChainPanel.prototype.onShowChain = function(event, ui)
{
	var raph_jq = ui.newPanel.find('div.raph')[0];
	var rpaper = raph_jq.paper;
	var cxfChain = raph_jq.dtoChain;

	this.selectedPaper = rpaper;
	this.selectedChain = cxfChain;

	this.state_toolbar.hide();
	this.transition_toolbar.hide();
};

// Actually draws the chain inside a clean(ed) Raphael paper
ChainPanel.prototype.drawChain = function(cxfChain, raphaelPaper)
{
	// Reinitialize panel
	raphaelPaper.clear();
	raphaelPaper.states = new Array();
	raphaelPaper.transitions = new Array();

	// add chain states to the list and draw them
	var ss = cxfChain.getStates().getDTOState();
	for ( var i = 0; i < ss.length; i++)
	{
		this.addState(ss[i], raphaelPaper);
	}

	// add transitions to the list and draw them
	var trs = cxfChain.getTransitions().getDTOTransition();
	for ( var i = 0; i < trs.length; i++)
	{
		this.addTransition(trs[i], raphaelPaper);
	}
};

ChainPanel.prototype.addState = function(DTOState, raphaelPaper)
{
	// Add state to list, both as member of the object (access by id) and to the
	// list itself (enumerator access)
	raphaelPaper.states[DTOState._id] = DTOState;
	raphaelPaper.states.push(DTOState);

	// Draw state as a circle
	var circle = raphaelPaper.circle(DTOState._x, DTOState._y, nodeSize);
	circle.attr(
	{
		fill : 'firebrick',
		stroke : 'darkorchid',
		'stroke-width' : 2
	});

	circle.contents = new Object();

	// Set reference between business model object and representation object
	DTOState._drawing = circle;
	circle.modeldata = DTOState;
	circle.node.id = DTOState._id;
	circle.id = DTOState._id;
	DTOState.trFromHere = new Array();
	DTOState.trReceivedHere = new Array();
	circle.chain_panel = this;

	// Add text
	addTextToState(circle);

	// Allow dragging with callback taking connections and business objects into account
	circle.drag($.throttle(dragStateMove, 20), dragStateStart, dragStateEnd);
	circle.click(function(event)
	{
		event.stopPropagation();
		this.chain_panel.transition_toolbar.hide();
		var toolbar = this.chain_panel.state_toolbar;
		var mode = toolbar.getMode();

		if (mode === "select")
			this.chain_panel.state_toolbar.show(this.modeldata);
		else if (mode === "link")
		{
			if (toolbar.dtoState === this.modeldata)
			{
				alert("cannot link a state to itself");
				return;
			}
			if (!this.modeldata._canReceiveLink)
			{
				alert("the target state you've selected does not allow this operation (start state or similar)");
				return;
			}
			if (!this.modeldata._canReceiveMultipleLinks && this.modeldata.trReceivedHere.length > 0)
			{
				alert("the target state can only receive one arrow. Use AND or OR.");
				return;
			}

			var v = new dto_chronix_oxymores_org_DTOTransition();
			v._id = uuid.v4();
			v._from = toolbar.dtoState._id;
			v._to = this.modeldata._id;
			v._guard1 = 0;
			this.chain_panel.addTransition(v, this.paper);
			toolbar.setMode("select");
		}
	});
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

	arrow.attr(
	{
		"arrow-end" : "classic-wide-long",
		"stroke-width" : arrowSize
	});

	// Set cross references to allow easy navigation between SVG and model elements
	from.trFromHere.push(arrow);
	to.trReceivedHere.push(arrow);
	arrow.from = from;
	arrow.to = to;
	arrow.chain_panel = this;
	DTOTransition._drawing = arrow;
	arrow.modeldata = DTOTransition;

	// Add handler to display the toolbar
	arrow.click(function(event)
	{
		ee = event;
		event.stopPropagation();
		this.chain_panel.state_toolbar.hide();
		var toolbar = this.chain_panel.transition_toolbar;

		toolbar.show(this.modeldata, event.x, event.y);
	});

	// Text
	addTextToArrow(arrow);
};

function addTextToArrow(arrow)
{
	var middle = arrow.getPointAtLength(arrow.getTotalLength() / 2);
	var txt = "[" + arrow.modeldata._guard1 + "]";
	if (arrow.modeldata._calendarAware)
		txt = txt + "\n" + "[same calendar occurrence]";

	if (arrow.text)
	{
		arrow.text.attr(
		{
			x : middle.x + 10,
			y : middle.y - 10,
			text : txt,
		});

		var bbb = arrow.text.getBBox();
		arrow.txtbck.attr(
		{
			x : bbb.x,
			y : bbb.y,
			width : bbb.width + 1,
			height : bbb.height + 1
		});
	}
	else
	{
		arrow.text = arrow.paper.text(middle.x + 10, middle.y - 10, txt);
		arrow.text.node.setAttribute("pointer-events", "none");

		var bbb = arrow.text.getBBox();
		arrow.txtbck = arrow.paper.rect(bbb.x, bbb.y, bbb.width + 1, bbb.height + 1).attr(
		{
			fill : '#C0C0C0',
			stroke : '#C0C0C0',
			opacity : 0.7,
		});
		arrow.text.toFront();
	}
}

function addTextToState(stateDrawing)
{
	var DTOState = stateDrawing.modeldata;
	var paper = stateDrawing.paper;
	var text = DTOState._label + "\n(" + DTOState._runsOnName + ")";
	if (DTOState._parallel)
		text += "\n//";

	if (stateDrawing.text)
	{
		stateDrawing.text.attr(
		{
			x : DTOState._x,
			y : DTOState._y,
			text : text,
		});
	}
	else
	{
		var tx = paper.text(DTOState._x, DTOState._y, text);
		tx.node.setAttribute("pointer-events", "none");

		stateDrawing.contents["text"] = tx;
		stateDrawing.text = tx;
	}
}

function dragStateStart(x, y, event)
{
	this.xs = this.attr("cx");
	this.ys = this.attr("cy");
}

function dragStateMove(dx, dy, x, y, event)
{
	var newx = this.xs + dx;
	var newy = this.ys + dy;

	// Move state
	this.attr(
	{
		cx : newx,
		cy : newy
	});

	// Update state model
	this.modeldata._x = newx;
	this.modeldata._y = newy;

	// Drag transition ends
	for ( var i = 0; i < this.modeldata.trFromHere.length; i++)
	{
		var tr = this.modeldata.trFromHere[i];
		shiftArrow(tr);
	}
	for ( var i = 0; i < this.modeldata.trReceivedHere.length; i++)
	{
		var tr = this.modeldata.trReceivedHere[i];
		shiftArrow(tr);
	}

	// Drag state contents (text, and so on)
	for ( var e in this.contents)
	{
		this.contents[e].attr(
		{
			x : newx,
			y : newy
		});
	}
}

function dragStateEnd(event)
{

}

function shiftArrow(RaphaelArrow)
{
	var pc = [ "M", RaphaelArrow.from._x.toFixed(1), RaphaelArrow.from._y.toFixed(1), "L", RaphaelArrow.to._x.toFixed(1),
			RaphaelArrow.to._y.toFixed(1) ].join(",");
	var shifted1 = Raphael.getPointAtLength(pc, nodeSize);
	var shifted2 = Raphael.getPointAtLength(pc, Raphael.getTotalLength(pc) - nodeSize);

	var path = [ "M", shifted1.x.toFixed(1), shifted1.y.toFixed(1), "L", shifted2.x.toFixed(1), shifted2.y.toFixed(1) ].join(",");
	RaphaelArrow.attr(
	{
		"path" : path
	});

	addTextToArrow(RaphaelArrow);

	// IE 10 specific bug - https://connect.microsoft.com/IE/feedback/details/781964/ - http://stackoverflow.com/a/17421050/252642
	if (navigator.appVersion.indexOf("MSIE 10") != -1)
	{
		node = RaphaelArrow.node;
		node.parentNode.insertBefore(node, node);
	}
}

// ////////////////////////////////////////////////////////////////////
// Toolbar for State
// ////////////////////////////////////////////////////////////////////
function StateToolbar()
{
	this.mainDiv = null; // jQuery object
	this.dtoState = null;
	this.mode = "select";

	this.mainDiv = $("<div style='display: none; position: fixed;' class='floatingToolbar'></div>");
	$("body").append(this.mainDiv);

	this.btLink = $("<button type='button'>link</button>");
	this.mainDiv.append(this.btLink);
	this.btLink.click(this.link.bind(this));

	this.btSelect = $("<button type='button'>select</button>");
	this.mainDiv.append(this.btSelect);
	this.btSelect.click(this.select.bind(this));

	this.btDelete = $("<button type='button'>delete</button>");
	this.mainDiv.append(this.btDelete);
	this.btDelete.click(this.remove.bind(this));

	this.slPlaceGroup = $("<select></select>");
	this.mainDiv.append(this.slPlaceGroup);

	this.cbParLabel = $("<label>parallel</label>");
	this.mainDiv.append(this.cbParLabel);
	this.cbPar = $("<input type='checkbox'></input>");
	this.mainDiv.append(this.cbPar);

}

StateToolbar.prototype.hide = function()
{
	this.mainDiv.hide();
	this.mode = "select";
};

StateToolbar.prototype.show = function(dtoState)
{
	this.dtoState = dtoState;
	var o = $(this.dtoState._drawing.paper.canvas).offset();

	// Move the toolbar
	this.mainDiv.css(
	{
		left : o.left + dtoState._x - 50,
		top : o.top + dtoState._y - nodeSize - 30,
	});

	// Choose which elements should be displayed
	if (!dtoState._canEmitLinks)
		this.btLink.hide();
	else
		this.btLink.show();
	if (!dtoState._canBeRemoved)
		this.btDelete.hide();
	else
		this.btDelete.show();
	if (dtoState._start || dtoState._end)
	{
		this.cbPar.hide();
		this.cbParLabel.hide();
	}
	else
	{
		this.cbPar.show();
		this.cbParLabel.show();
	}
	bb = dtoState;
	// Refresh place groups list
	var panel = this.dtoState._drawing.chain_panel;
	this.slPlaceGroup.empty();
	var placeGroups = panel.cxfApplication.getGroups().getDTOPlaceGroup();
	for ( var i = 0; i < placeGroups.length; i++)
	{
		var elt = placeGroups[i];
		var selected = "";
		if (this.dtoState._runsOnId === elt._id)
			selected = "selected='selected'";

		this.slPlaceGroup.append($("<option value='" + elt._id + "'" + selected + ">" + elt._name + "</option>"));
	}
	this.slPlaceGroup.change((function()
	{
		this.dtoState._runsOnId = this.slPlaceGroup.val();
		this.dtoState._runsOnName = this.slPlaceGroup.children("option[value='" + this.slPlaceGroup.val() + "']").text();
		addTextToState(this.dtoState._drawing);
	}).bind(this));

	// Parallel checkbox
	this.cbPar.prop('checked', dtoState._parallel);
	this.cbPar.change((function(e)
	{
		this.dtoState._parallel = this.cbPar.is(':checked');
		addTextToState(this.dtoState._drawing);
	}).bind(this));

	// Show the toolbar
	this.mainDiv.show();
};

StateToolbar.prototype.link = function()
{
	this.mode = "link";
};

StateToolbar.prototype.select = function()
{
	this.mode = "select";
};

StateToolbar.prototype.getMode = function()
{
	return this.mode;
};

StateToolbar.prototype.setMode = function(mode)
{
	this.mode = mode;
};

StateToolbar.prototype.remove = function(mode)
{
	this.mode = "select";
	var panel = this.dtoState._drawing.chain_panel;

	// Remove the helper text from drawings
	for ( var e in this.dtoState._drawing.contents)
	{
		this.dtoState._drawing.contents[e].remove();
	}

	// Remove the transitions from drawing and model
	for ( var i = 0; i < this.dtoState.trFromHere.length; i++)
	{
		tr = this.dtoState.trFromHere[i];
		panel.selectedChain.getTransitions().getDTOTransition().pop(tr.modeldata);

		this.dtoState.trFromHere.pop(tr);
		tr.to.trReceivedHere.pop(tr);
		tr.remove();
	}
	for ( var i = 0; i < this.dtoState.trReceivedHere.length; i++)
	{
		tr = this.dtoState.trReceivedHere[i];
		panel.selectedChain.getTransitions().getDTOTransition().pop(tr.modeldata);

		this.dtoState.trReceivedHere.pop(tr);
		tr.from.trFromHere.pop(tr);
		tr.remove();
	}

	// Remove the state itself from drawing and from model
	this.dtoState._drawing.remove();
	panel.selectedChain.getStates().getDTOState().pop(this.dtoState);
	this.hide();
};

// ////////////////////////////////////////////////////////////////////
// Toolbar for Transition
// ////////////////////////////////////////////////////////////////////
function TransitionToolbar()
{
	this.mainDiv = null; // jQuery object
	this.dtoTransition = null;

	this.mainDiv = $("<div style='display: none; position: fixed;' class='floatingToolbar'></div>");
	$("body").append(this.mainDiv);

	this.btDelete = $("<button type='button'>delete</button>");
	this.mainDiv.append(this.btDelete);
	this.btDelete.click(this.remove.bind(this));

	this.mainDiv.append($("<label>CR</label>"));
	this.txtCR = $("<input type='number' maxlength='3' max='9999' style='width:3em;'></input>");
	this.mainDiv.append(this.txtCR);

	this.cbSameCalOc = $("<input type='checkbox'>Same calendar occurrence</input>");
	this.mainDiv.append(this.cbSameCalOc);
}

TransitionToolbar.prototype.hide = function()
{
	this.mainDiv.hide();
	this.mode = "select";
};

TransitionToolbar.prototype.show = function(dtoTransition, x, y)
{
	this.dtoTransition = dtoTransition;

	// Move the toolbar
	this.mainDiv.css(
	{
		left : x,
		top : y - 30,
	});

	this.cbSameCalOc.prop('checked', dtoTransition._calendarAware);
	this.cbSameCalOc.change((function(e)
	{
		this.dtoTransition._calendarAware = this.cbSameCalOc.is(':checked');
		addTextToArrow(this.dtoTransition._drawing);
	}).bind(this));

	this.txtCR.val(dtoTransition._guard1);
	this.txtCR.change((function(e)
	{
		if (this.txtCR.val() > 0)
		{
			this.dtoTransition._guard1 = this.txtCR.val();
			addTextToArrow(this.dtoTransition._drawing);
		}
		else
			this.txtCR.val(0);
	}).bind(this));

	this.mainDiv.show();
};

TransitionToolbar.prototype.remove = function()
{
	// this.cxfApplication.getChains().getDTOChain().splice(this.cxfApplication.getChains().getDTOChain().indexOf(this.selectedChain), 1);
	var panel = this.dtoTransition._drawing.chain_panel;
	var trs = panel.selectedChain.getTransitions().getDTOTransition();
	trs.splice(trs.indexOf(this.dtoTransition), 1);

	trs = this.dtoTransition._drawing.from.trFromHere;
	trs.splice(trs.indexOf(this.dtoTransition), 1);

	trs = this.dtoTransition._drawing.to.trReceivedHere;
	trs.splice(trs.indexOf(this.dtoTransition), 1);

	this.dtoTransition._drawing.text.remove();
	this.dtoTransition._drawing.txtbck.remove();
	this.dtoTransition._drawing.remove();
	this.dtoTransition = null;
	this.hide();
};