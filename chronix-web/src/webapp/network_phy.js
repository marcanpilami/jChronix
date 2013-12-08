enNodeSize = 30;

function NPPanel(divId, dtoApplication)
{
	this.dtoApplication = dtoApplication;
	this.baseId = this.dtoApplication._id;
	this.mainDivId = divId;
	this.mainDiv = $("#" + divId);
	this.allNodes = new Array();
	this.btAddEngine = null;
	this.btAddRunner = null;
	this.node_panel = new ENPanel();
	this.shadowArrow = null;
	this.shadowFrom = null;

	this.mainDiv.empty();

	this.mainDiv
			.append($("<div id='phy-container-"
					+ this.baseId
					+ "' style='height: 100%;'><div id='phy-palette-"
					+ this.baseId
					+ "' style='width:150px; display: inline-block; vertical-align: top;'></div><div style='overflow: auto; width: calc(100% - 150px); height: 100%; display: inline-block;'><div id='phy-raph-"
					+ this.baseId + "'></div></div></div>"));

	this.raphDiv = $("#phy-raph-" + this.baseId);
	this.raphPaper = new Raphael("phy-raph-" + this.baseId, 3000, 3000);
	this.raphDiv.mousemove(this.onMouseMove.bind(this));
	this.raphDiv.click((function()
	{
		this.haltNewArrow();
		this.node_panel.hide();
	}).bind(this));

	this.paletteDiv = $("#phy-palette-" + this.baseId);

	var ss = this.dtoApplication.getNodes().getDTOExecutionNode();
	for ( var i = 0; i < ss.length; i++)
	{
		this.allNodes.push(ss[i]);
		this.allNodes[ss[i]._id] = ss[i];
	}
	for ( var i = 0; i < ss.length; i++)
	{
		this.drawNode(ss[i]);
	}

	this.drawPalette();
}

NPPanel.prototype.drawNode = function(dtoExecutionNode)
{
	rr = dtoExecutionNode;
	var circle = this.raphPaper.circle(dtoExecutionNode._x, dtoExecutionNode._y, enNodeSize);
	circle.attr(
	{
		fill : 'firebrick',
		stroke : 'midnightblue',
		'stroke-width' : 2
	});

	// Glue
	circle.modeldata = dtoExecutionNode;
	circle.text = null;
	circle.arrowsFromHere = new Array();
	circle.panel = this;
	dtoExecutionNode.drawing = circle;

	// Text
	this.textNode(circle);

	// Draw links
	var links = dtoExecutionNode.getToTCP().getString();
	for ( var i = 0; i < links.length; i++)
	{
		circle.arrowsFromHere.push(this.drawLink(dtoExecutionNode, this.allNodes[links[i]]));
	}

	// Events
	circle.drag($.throttle(this.dragNodeMove, 20), this.dragNodeStart, this.dragNodeEnd);
	circle.click(this.onNodeClick);
};

NPPanel.prototype.textNode = function(circle)
{
	var dto = circle.modeldata;
	var paper = circle.paper;
	var txt = dto._dns + ":" + dto._qPort;

	if (!circle.text)
	{
		var tx = paper.text(dto._x, dto._y, txt);
		tx.node.setAttribute("pointer-events", "none");
		circle.text = tx;
	}
	else
	{
		circle.text.attr(
		{
			text : txt
		});
	}

	if (dto._simpleRunner)
		circle.attr(
		{
			fill : 'mediumseagreen'
		});
	else
		circle.attr(
		{
			fill : 'cornflowerblue'
		});
};

NPPanel.prototype.drawLink = function(dtoFrom, dtoTo)
{
	var pc = [ "M", dtoFrom._x.toFixed(1), dtoFrom._y.toFixed(1), "L", dtoTo._x.toFixed(1), dtoTo._y.toFixed(1) ].join(",");
	var arrow = this.raphPaper.path(pc);

	// Helper glue data
	arrow.from = dtoFrom;
	arrow.to = dtoTo;

	// Appearance
	arrow.attr(
	{
		"arrow-end" : "classic-wide-long",
		"stroke-width" : arrowSize
	});

	// Text & resizing
	this.adaptArrow(arrow);

	// Event: double-click = remove
	arrow.dblclick(this.onDeleteArrow);

	// Done
	return arrow;
};

NPPanel.prototype.textLink = function(RaphaelArrow)
{
	var paper = RaphaelArrow.paper;
	var txt = "";
	var middle = RaphaelArrow.getPointAtLength(RaphaelArrow.getTotalLength() / 2);

	var tx = paper.text(middle._x, middle._y, txt);
	tx.node.setAttribute("pointer-events", "none");

	RaphaelArrow.text = tx;
};

NPPanel.prototype.adaptArrow = function(RaphaelArrow)
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

	this.textLink(RaphaelArrow);

	// IE 10 specific bug - https://connect.microsoft.com/IE/feedback/details/781964/ - http://stackoverflow.com/a/17421050/252642
	if (navigator.appVersion.indexOf("MSIE 10") != -1)
	{
		node = RaphaelArrow.node;
		node.parentNode.insertBefore(node, node);
	}
};

NPPanel.prototype.dragNodeStart = function(x, y, event)
{
	this.xs = this.attr("cx");
	this.ys = this.attr("cy");
};

NPPanel.prototype.dragNodeMove = function(dx, dy, x, y, event)
{
	ee = event;
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

	// Drag links ends
	for ( var i = 0; i < this.arrowsFromHere.length; i++)
	{
		var tr = this.arrowsFromHere[i];
		this.panel.adaptArrow(tr);
	}
	for ( var i = 0; i < this.modeldata.getFromTCP().getString().length; i++)
	{
		var otherNode = this.panel.allNodes[this.modeldata.getFromTCP().getString()[i]];
		for ( var j = 0; j < otherNode.drawing.arrowsFromHere.length; j++)
		{
			var tr = otherNode.drawing.arrowsFromHere[i];
			this.panel.adaptArrow(tr);
		}
	}

	// Drag text content
	this.text.attr(
	{
		x : newx,
		y : newy
	});
};

NPPanel.prototype.dragNodeEnd = function(event)
{

};

NPPanel.prototype.drawPalette = function()
{
	this.btAddEngine = $("<button type='button'>Add engine</button>");
	this.paletteDiv.append(this.btAddEngine);
	this.btAddEngine.click(this.addEngine.bind(this));

	this.btAddRunner = $("<button type='button'>Add runner</button>");
	this.paletteDiv.append(this.btAddRunner);
	this.btAddRunner.click(this.addRunner.bind(this));
};

NPPanel.prototype.createNode = function()
{
	var v = new dto_chronix_oxymores_org_DTOExecutionNode();
	v._id = uuid.v4();
	v._x = 50;
	v._y = 50;
	v._dns = "localhost";
	v.osusername = "";
	v._console = false;
	v._qPort = 1789;
	v._wsPort = 2000;
	v._remoteExecPort = 0;
	v._jmxPort = 9010;
	v._toTCP = new internalapi_chronix_oxymores_org__ArrayOfString();
	v._toRCTRL = new internalapi_chronix_oxymores_org__ArrayOfString();
	v._fromTCP = new internalapi_chronix_oxymores_org__ArrayOfString();
	v._fromRCTRL = new internalapi_chronix_oxymores_org__ArrayOfString();
	v._places = new internalapi_chronix_oxymores_org__ArrayOfString();
	v._simpleRunner = true;

	this.dtoApplication.getNodes().getDTOExecutionNode().push(v);
	this.allNodes.push(v);
	this.allNodes[v._id] = v;
	return v;
};

NPPanel.prototype.addRunner = function()
{
	var v = this.createNode();
	v._simpleRunner = true;
	this.drawNode(v);
};

NPPanel.prototype.addEngine = function()
{
	var v = this.createNode();
	v._simpleRunner = false;
	this.drawNode(v);
};

// "this" is a circle Raphael object. Use "panel" for the usual "this".
NPPanel.prototype.onNodeClick = function(event)
{
	event.stopPropagation();
	var dto = this.modeldata;
	var panel = this.panel;
	panel.node_panel.show(dto, this.panel.dtoApplication);

	if (panel.shadowArrow !== null)
	{
		if (panel.shadowFrom === dto)
		{
			alert("cannot link a node to itself");
			panel.haltNewArrow();
		}
		else
		{
			var from = panel.shadowFrom;
			var to = dto;

			from.getToTCP().getString().push(to._id);
			to.getFromTCP().getString().push(from._id);
			from.drawing.arrowsFromHere.push(panel.drawLink(from, to));
			panel.haltNewArrow();
		}
	}
};

NPPanel.prototype.onMouseMove = function(event)
{
	if (this.shadowArrow)
	{
		var pc = [ "M", this.shadowArrow.getPath()[0][1], this.shadowArrow.getPath()[0][2], "L", event.offsetX, event.offsetY ].join(",");
		this.shadowArrow.attr(
		{
			'path' : pc,
			"arrow-end" : "classic-wide-long",
			"stroke-width" : arrowSize
		});
	}
};

NPPanel.prototype.beginNewArrow = function(dtoENFrom)
{
	var pc = [ "M", dtoENFrom._x.toFixed(1), dtoENFrom._y.toFixed(1), "L", dtoENFrom._x.toFixed(1) + 10, dtoENFrom._y.toFixed(1) + 10 ].join(",");
	this.shadowArrow = this.raphPaper.path(pc);
	this.shadowArrow.toBack();
	this.shadowFrom = dtoENFrom;
};

NPPanel.prototype.haltNewArrow = function()
{
	if (this.shadowArrow)
	{
		this.shadowArrow.remove();
		this.shadowArrow = null;
		this.shadowFrom = null;
	}
};

// "this" is a Raphaelpath
NPPanel.prototype.onDeleteArrow = function(event)
{
	this.from.drawing.arrowsFromHere.splice(this.from.drawing.arrowsFromHere.indexOf(this), 1);
	this.from.getToTCP().getString().splice(this.from.getToTCP().getString().indexOf(this._id), 1);
	this.to.getFromTCP().getString().splice(this.to.getFromTCP().getString().indexOf(this._id), 1);
	this.remove();
};

NPPanel.prototype.redisplay = function()
{
	// Nothing to do. It's a pretty simple panel.
};

// ///////////////////////////////////////////////////////////////////////////
// EN PANEL
// ///////////////////////////////////////////////////////////////////////////
function ENPanel()
{
	this.dtoExecutionNode = null;
	this.dtoApplication = null;

	this.mainDiv = $("<div style='display: table; position: fixed; width: auto;' class='floatingToolbar'></div>");
	$("body").append(this.mainDiv);

	var row = $("<div style='display:table-row;'></div>");

	this.txtDns = $("<input style='display:table-cell;' type='text'>");
	this.mainDiv.append(row.clone().append($("<label style='display:table-cell;'>DNS</label>")).append(this.txtDns));
	this.txtDns.change(this.change.bind(this));

	this.txtOsUser = $("<input style='display:table-cell;' type='text'>");
	this.mainDiv.append(row.clone().append($("<label style='display:table-cell;'>Os User</label>")).append(this.txtOsUser));
	this.txtOsUser.change(this.change.bind(this));

	this.txtPort = $("<input style='display:table-cell;' type='number' max='9999'>");
	this.mainDiv.append(row.clone().append($("<label style='display:table-cell;'>Main port</label>")).append(this.txtPort));
	this.txtPort.change(this.change.bind(this));

	this.txtJmxPort = $("<input style='display:table-cell;' type='number' max='9999'>");
	this.mainDiv.append(row.clone().append($("<label style='display:table-cell;'>JMX port</label>")).append(this.txtJmxPort));
	this.txtJmxPort.change(this.change.bind(this));

	this.txtHttpPort = $("<input style='display:table-cell;' type='number' max='9999'>");
	this.mainDiv.append(row.clone().append($("<label style='display:table-cell;'>HTTP port</label>")).append(this.txtHttpPort));
	this.txtHttpPort.change(this.change.bind(this));

	this.cbRunner = $("<input style='display:table-cell;' type='checkbox'>");
	this.mainDiv.append(row.clone().append($("<label style='display:table-cell;'>Simple runner</label>")).append(this.cbRunner));
	this.cbRunner.change(this.change.bind(this));

	this.btLink = $("<input style='display:table-cell;' type='button' value='Link to'>");
	this.btDelete = $("<input style='display:table-cell;' type='button' value='Delete'>");
	this.btHide = $("<input style='display:table-cell;' type='button' value='Close'>");
	this.mainDiv.append(row.clone().append($("<label style='display:table-cell;'>Actions</label>")).append(this.btLink).append(this.btDelete).append(
			this.btHide));
	this.btHide.click(this.hide.bind(this));
	this.btDelete.click(this.remove.bind(this));
	this.btLink.click(this.link.bind(this));
}

ENPanel.prototype.show = function(dtoExecutionNode, dtoApplication)
{
	// Stop events (we are going to change the values)
	this.dtoExecutionNode = null;
	this.dtoApplication = dtoApplication;

	// Set values inside
	this.txtDns.val(dtoExecutionNode._dns);
	this.txtOsUser.val(dtoExecutionNode._osusername);
	this.txtPort.val(dtoExecutionNode._qPort);
	this.txtJmxPort.val(dtoExecutionNode._jmxPort);
	this.txtHttpPort.val(dtoExecutionNode._wsPort);
	this.cbRunner.prop('checked', dtoExecutionNode._simpleRunner);

	// Place the panel above
	this.mainDiv.css(
	{
		left : Math.max(dtoExecutionNode._x - 70, 0),
		top : Math.max(dtoExecutionNode._y - enNodeSize - 30, 0),
	});
	this.dtoExecutionNode = dtoExecutionNode;
	this.mainDiv.show();
};

ENPanel.prototype.hide = function()
{
	this.mainDiv.hide();
	this.dtoExecutionNode = null;
};

ENPanel.prototype.change = function()
{
	if (this.dtoExecutionNode == null)
		return;

	this.dtoExecutionNode._dns = this.txtDns.val();
	this.dtoExecutionNode._osusername = this.txtOsUser.val();
	this.dtoExecutionNode._qPort = this.txtPort.val();
	this.dtoExecutionNode._jmxPort = this.txtJmxPort.val();
	this.dtoExecutionNode._wsPort = this.txtHttpPort.val();
	this.dtoExecutionNode._simpleRunner = this.cbRunner.is(':checked');

	this.dtoExecutionNode.drawing.panel.textNode(this.dtoExecutionNode.drawing);
};

ENPanel.prototype.link = function()
{
	this.dtoExecutionNode.drawing.panel.beginNewArrow(this.dtoExecutionNode);
};

ENPanel.prototype.remove = function()
{
	// Remove arrows from the chart
	var tmp = this.dtoApplication.getNodes().getDTOExecutionNode();
	var arrowsToRemove = new Array();
	for ( var i = 0; i < tmp.length; i++) // Look for arrows on all nodes
	{
		var on = tmp[i];
		for ( var j = 0; j < on.drawing.arrowsFromHere.length; j++)
		{
			var arrow = on.drawing.arrowsFromHere[j];
			if (arrow.from === this.dtoExecutionNode || arrow.to === this.dtoExecutionNode)
				arrowsToRemove.push(arrow);
		}
	}
	var tmp2 = arrowsToRemove.pop();
	while (tmp2 != undefined)
	{
		var dtoFrom = tmp2.from;
		var dtoTo = tmp2.to;
		dtoFrom.getToTCP().getString().splice(dtoFrom.getToTCP().getString().indexOf(dtoTo._id), 1);
		dtoTo.getFromTCP().getString().splice(dtoTo.getFromTCP().getString().indexOf(dtoFrom._id), 1);
		dtoFrom.drawing.arrowsFromHere.splice(dtoFrom.drawing.arrowsFromHere.indexOf(tmp2), 1);
		tmp2.remove();
		tmp2 = arrowsToRemove.pop();
	}

	// Remove the node from the chart
	this.dtoExecutionNode.drawing.text.remove();
	this.dtoExecutionNode.drawing.remove();

	// Remove all PLACEs that use this EN
	var places = this.dtoApplication.getPlaces().getDTOPlace();

	var p = null;
	for ( var i = 0; i < places.length; i++)
	{
		p = places[i];
		if (p._nodeid !== this.dtoExecutionNode._id)
			continue;
		deletePlace(this.dtoApplication, p);
	}

	// Remove the place itself
	tmp.splice(tmp.indexOf(this.dtoExecutionNode), 1);

	// Done
	this.hide();
};

// Misc.
dto_chronix_oxymores_org_DTOExecutionNode.prototype.prettyLabel = function()
{
	return this._dns + ":" + this._qPort + " (" + (this._simpleRunner ? "runner" : "engine") + ")";
};
