var canvas;
var ctx;

var states = new Array();
var chain;

var paper = null;

function dragStateStart(x, y, event) {
	this.xs = this.attr("cx");
	this.ys = this.attr("cy");
}

function dragStateMove(dx, dy, x, y, event) {
	this.attr({
		cx : this.xs + dx
	});
	this.attr({
		cy : this.ys + dy
	});
	this.modeldata._x = this.xs + dx;
	this.modeldata._y = this.ys + dy;
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
	//circle.animate({"fill": "#f00", "transform": 't100,100'}, 2000, 'linear', function() {});
	circle.animate({
		cx : 10,
		cy : 10
	}, 2000, 'linear');
}

function errorCallback(httpStatus, httpStatusText) {
	alert('error ' + httpStatusText);
}

function successCallback(responseObject) {
	//alert(responseObject.getReturn());
	$("#greetings").html(responseObject.getReturn() + " - " + uuid.v4());
}

function getChainOK(responseObject) {
	chain = responseObject.getReturn();

	// add chain states to the list and draw them
	var ss = chain.getStates().getDTOState();
	for(var i = 0; i < ss.length; i++) {
		addState(ss[i]);
	}
}

function addState(DTOState) {
	// Add state to list
	states.push(DTOState);

	// Draw state as a circle
	var circle = paper.circle(DTOState._x, DTOState._y, 50);
	circle.attr({
		fill : 'firebrick',
		stroke : 'darkorchid',
		'stroke-width' : 2
	});

	// Set reference between business model object and representation object
	DTOState._drawing = circle;
	circle.modeldata = DTOState;

	// Allow dragging with callback taking connections and business objects into account
	circle.drag(dragStateMove, dragStateStart, dragStateEnd);
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
	//	$("#meuh").addClass("red");  // red is a css class
	loadChain();
});
