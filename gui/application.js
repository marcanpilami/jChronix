var cxfApplication = null;
var cxfShellCommands = new Object();
var cxfChains = new Object();

function loadApplication() {
	proxy.getApplication(getApplicationOK, getApplicationKO, "MEUH");
}

function getApplicationKO(responseObject) {
	alert("oooops");
}

function getApplicationOK(responseObject) {
	// alert("l�");
	cxfApplication = responseObject.getReturn();

	var aa = cxfApplication.getChains().getDTOChain();
	var first = null;
	for ( var i = 0; i < aa.length; i++) {
		addChain(aa[i]);
		if (first === null) {
			first = aa[i];
		}
	}

	$("#appName").text(cxfApplication._name);
	$("#appDescr").text(cxfApplication._description);
}

function addChain(c) {
	cxfChains[c._id] = c;
	editChain(c);
}

function send() {
	proxy.stageApplication(sendApplicationOK, sendApplicationKO, cxfApplication);
}

function sendApplicationOK(response) {
	$("#alert").text("stored");
}

function sendApplicationKO(response) {
	alert("oups");
}