var canvas;
var ctx;

var states = new Array();
var chain;

function State()
{
	
	
}

function draw() {
	canvas = document.getElementById("cnMain");
	ctx = canvas.getContext("2d");

	ctx.fillStyle = "rgb(200,0,0)";
	ctx.fillRect(10, 10, 55, 50);

	ctx.fillStyle = "rgba(0, 0, 200, 0.5)";
	ctx.fillRect(30, 30, 55, 50);

}


function errorCallback(httpStatus, httpStatusText) 
{
	alert('error ' + httpStatusText);
}

function successCallback(responseObject) 
{
	alert(responseObject.getReturn());
}

function getChainOK(responseObject)
{
	states.push(responseObject);
	chain = responseObject.getReturn();
	
	for ( var i = 0; i < array.length; i++) {
		
	}
}

function getChainKO(responseObject)
{
	alert("oooops");
}

function pouet()
{
	var p = new internalapi_chronix_oxymores_org__IServiceClientPortType();
	p.sayHello(successCallback, errorCallback, "marsu", 12);
	
	p.getChain(getChainOK, getChainKO);
}

$(document).ready(function() {
	draw();
	$("#meuh").addClass("red");
	pouet();
});
