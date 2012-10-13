var cxfApplication = null;
var cxfShellCommands = new Array();
var cxfChains = new Object();

function loadApplication() {
	proxy.getApplication(getApplicationOK, getApplicationKO, "MEUH");
}

function getApplicationKO(responseObject) {
	alert("oooops");
}

function getApplicationOK(responseObject) {
	// alert("là");
	cxfApplication = responseObject.getReturn();

	var aa = cxfApplication.getChains().getDTOChain();
	var first = null;
	for ( var i = 0; i < aa.length; i++) {
		addChain(aa[i]);
		if (first === null) {
			first = aa[i];
		}
	}

	var ss = cxfApplication.getShells().getDTOShellCommand();
	for ( var i = 0; i < ss.length; i++) {
		addShell(ss[i]);
	}

	$("#appName").text(cxfApplication._name);
	$("#appDescr").text(cxfApplication._description);

	initCommandPanel(cxfShellCommands);
	fillInPaletteData(cxfShellCommands);
	initNetworkROPanel(aa);
}

function addChain(c) {
	cxfChains[c._id] = c;
	editChain(c);
}

function addShell(s) {
	cxfShellCommands.push(s);
}

function send() {
	proxy
			.stageApplication(sendApplicationOK, sendApplicationKO,
					cxfApplication);
}

function sendApplicationOK(response) {
	$("#alert").text("stored");
}

function sendApplicationKO(response) {
	alert("oups");
}

function addCommand(DTOShellCommand) {
	
}