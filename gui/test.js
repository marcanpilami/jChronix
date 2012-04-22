var canvas;
var ctx;

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

function pouet()
{
	var p = new internalapi_chronix_oxymores_org__IServiceClientPortType();
	p.sayHello(successCallback, errorCallback, "marsu", 12);
}

$(document).ready(function() {
	//alert("meuh");
	draw();
	$("#meuh").addClass("red");
	pouet();
	//$("#marsu").load("http://localhost:9000/Hello")
});
