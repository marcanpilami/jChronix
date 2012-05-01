var canvas;
var ctx;

var states = new Array();
var chain;

var paper = null;

function State()
{
	
	
}

function dragStart(x, y, event)
{
	this.xs = this.attr("cx");
	this.ys = this.attr("cy");
}

function dragMove(dx, dy, x, y, event)
{
	//alert("toto");
	//this.attr({cx: this.attr("cx") + dx});
	this.attr({cx: this.xs + dx});
	this.attr({cy: this.ys + dy});
}

function dragEnd(event)
{
	
}

function draw() {
	canvas = document.getElementById("cnMain");
	ctx = canvas.getContext("2d");

	ctx.fillStyle = "rgb(200,0,0)";
	ctx.fillRect(10, 10, 55, 50);

	ctx.fillStyle = "rgba(0, 0, 200, 0.5)";
	ctx.fillRect(30, 30, 55, 50);
	
	ctx.strokeText('Hello world!', 0, 50);


	paper = new Raphael("raph", 1600, 600);
	var circle = paper.circle(200,200,50);
	circle.attr("fill", "#f00");
	circle.attr({fill: 'firebrick', stroke: 'darkorchid', 'stroke-width': 5});  
	//circle.animate({"fill": "#f00", "transform": 't100,100'}, 2000, 'linear', function() {});
	circle.animate({cx:10, cy:10}, 2000, 'linear' );
	
	circle.drag(dragMove, dragStart, dragEnd);
}


function errorCallback(httpStatus, httpStatusText) 
{
	alert('error ' + httpStatusText);
}

function successCallback(responseObject) 
{
	//alert(responseObject.getReturn());
	$("#greetings").html(responseObject.getReturn() + " - " + uuid.v4());
}

function getChainOK(responseObject)
{
	states.push(responseObject);
	chain = responseObject.getReturn();
	
	/*for ( var i = 0; i < array.length; i++) {
		
	}*/
}

function getChainKO(responseObject)
{
	alert("oooops");
}

function loadChain()
{
	var p = new internalapi_chronix_oxymores_org__IServiceClientPortType();
	p.getChain(getChainOK, getChainKO);
}

function pingServer()
{
	var p = new internalapi_chronix_oxymores_org__IServiceClientPortType();
	p.sayHello(successCallback, errorCallback, "marsu", 12);
}

$(document).ready(function() {
	draw();
	$("#tabs").tabs();
//	$("#meuh").addClass("red");  // red is a css class
	loadChain();
});
