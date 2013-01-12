var proxy = null;

$(document).ready(function() {
	$("#tabs").tabs();
	
	proxy = new internalapi_chronix_oxymores_org__IServiceClientPortType();
	
	initChainPanel();
	//initCommandPanel();
	
	loadApplication();
});

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

