var proxy = null;

$(document).ready(function() {
	$("#tabs").tabs();
	
	proxy = new internalapi_chronix_oxymores_org__IServiceClientPortType();
	
	initChainPanel();
	//initCommandPanel();
	
	loadApplication();
});

function drawDebug() {
	canvas = document.getElementById("cnMain");
	ctx = canvas.getContext("2d");

	ctx.fillStyle = "rgb(200,0,0)";
	ctx.fillRect(10, 10, 55, 50);

	ctx.fillStyle = "rgba(0, 0, 200, 0.5)";
	ctx.fillRect(30, 30, 55, 50);

	ctx.strokeText('Hello world!', 0, 50);
}

function loadApplicationsList() {
}

function pingServer() {
	var p = new internalapi_chronix_oxymores_org__IServiceClientPortType();
	p.sayHello(successCallback, errorCallback, "marsu", 12);
}

function errorCallback(httpStatus, httpStatusText) {
	alert('error ' + httpStatusText);
}

function successCallback(responseObject) {
	// alert(responseObject.getReturn());
	$("#greetings").html(responseObject.getReturn() + " - " + uuid.v4());
}

