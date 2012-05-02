var canvas;
var ctx;

var states = new Array();
var transitions = new Array();
var chain;

var paper = null;

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
	for ( var i = 0; i < this.modeldata.trFromHere.length; i++) {
		var tr = this.modeldata.trFromHere[i];
		shiftArrow(tr);
	}
	for ( var i = 0; i < this.modeldata.trReceivedHere.length; i++) {
		var tr = this.modeldata.trReceivedHere[i];
		shiftArrow(tr);
	}

	// Drag state contents (text, and so on)
	for ( var e in this.contents) {
		this.contents[e].attr({
			x : newx,
			y : newy
		});
	}
}

function shiftArrow(RaphaelArrow) {
	var pc = "M" + RaphaelArrow.from._x + "," + RaphaelArrow.from._y + ",L"
			+ RaphaelArrow.to._x + "," + RaphaelArrow.to._y;
	var shifted1 = Raphael.getPointAtLength(pc, 50);
	var shifted2 = Raphael
			.getPointAtLength(pc, Raphael.getTotalLength(pc) - 50);

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

function drawDebug() {
	canvas = document.getElementById("cnMain");
	ctx = canvas.getContext("2d");

	ctx.fillStyle = "rgb(200,0,0)";
	ctx.fillRect(10, 10, 55, 50);

	ctx.fillStyle = "rgba(0, 0, 200, 0.5)";
	ctx.fillRect(30, 30, 55, 50);

	ctx.strokeText('Hello world!', 0, 50);
}

function drawInit() {
	paper = new Raphael("raph", 1600, 600);

	// Debug animation - don't remove till GUI nearing completion
	var circle = paper.circle(200, 200, 50);
	circle.attr("fill", "#f00");
	circle.attr({
		fill : 'firebrick',
		stroke : 'darkorchid',
		'stroke-width' : 5
	});
	// circle.animate({"fill": "#f00", "transform": 't100,100'}, 2000, 'linear',
	// function() {});
	circle.animate({
		cx : 10,
		cy : 10
	}, 2000, 'linear');
}

function errorCallback(httpStatus, httpStatusText) {
	alert('error ' + httpStatusText);
}

function successCallback(responseObject) {
	// alert(responseObject.getReturn());
	$("#greetings").html(responseObject.getReturn() + " - " + uuid.v4());
}

function getChainOK(responseObject) {
	chain = responseObject.getReturn();

	// add chain states to the list and draw them
	var ss = chain.getStates().getDTOState();
	for ( var i = 0; i < ss.length; i++) {
		addState(ss[i]);
	}

	// add transitions to the list and draw them
	var trs = chain.getTransitions().getDTOTransition();
	for ( var i = 0; i < trs.length; i++) {
		addTransition(trs[i]);
	}
}

function addState(DTOState) {
	// Add state to list, both as member of the object (access by id) and to the
	// list itself (enumerator access)
	states[DTOState._id] = DTOState;
	states.push(DTOState);

	// Draw state as a circle
	var circle = paper.circle(DTOState._x, DTOState._y, 50);
	circle.attr({
		fill : 'firebrick',
		stroke : 'darkorchid',
		'stroke-width' : 2
	});
	var tx = paper.text(DTOState._x, DTOState._y, DTOState._label + "\n("
			+ DTOState._runsOnName + ")");
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

function addTransition(DTOTransition) {
	// Add to collection for future reference
	transitions[DTOTransition._id] = DTOTransition;
	transitions.push(DTOTransition);

	// Draw as an arrow
	var from = states[DTOTransition._from];
	var to = states[DTOTransition._to];
	var x1 = from._x;
	var y1 = from._y;
	var x2 = to._x;
	var y2 = to._y;

	// var arrow = paper.path("M" + x1 + "," + y1 + ",L" + x2 + "," + y2);// +
	// "Z");
	var pc = "M" + x1 + "," + y1 + ",L" + x2 + "," + y2;
	var shifted1 = Raphael.getPointAtLength(pc, 50);
	var shifted2 = Raphael
			.getPointAtLength(pc, Raphael.getTotalLength(pc) - 50);
	var arrow = paper.path("M" + shifted1.x + "," + shifted1.y + ",L"
			+ shifted2.x + "," + shifted2.y);// + "Z");
	/*
	 * arrow.attr({ x : shifted1.x });
	 */

	arrow.attr({
		"arrow-end" : "classic-wide-long",
		"stroke-width" : 5
	});
	// arrow.transform("s0.5,0.5");

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
	var p = new internalapi_chronix_oxymores_org__IServiceClientPortType();
	p.getChain(getChainOK, getChainKO);
}

function pingServer() {
	var p = new internalapi_chronix_oxymores_org__IServiceClientPortType();
	p.sayHello(successCallback, errorCallback, "marsu", 12);
}

$(document).ready(function() {
	drawInit();
	$("#tabs").tabs();
	// $("#meuh").addClass("red"); // red is a css class
	loadChain();
});
