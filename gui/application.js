var cxfApplication = null;
var cxfShellCommands = new Array();
var cxfChains = new Object();
var cxfPlaces = [];

function loadApplication()
{
	proxy.getApplication(getApplicationOK, getApplicationKO, "MEUH");
}

function getApplicationKO(responseObject)
{
	alert("oooops");
}

function getApplicationOK(responseObject)
{
	cxfApplication = responseObject.getReturn();

	var aa = cxfApplication.getChains().getDTOChain();
	var first = null;
	for ( var i = 0; i < aa.length; i++)
	{
		addChain(aa[i]);
		if (first === null)
		{
			first = aa[i];
		}
	}

	var ss = cxfApplication.getShells().getDTOShellCommand();
	for ( var i = 0; i < ss.length; i++)
	{
		ss[i].id = ss[i]._id;
		addShell(ss[i]);
	}

	var bb = cxfApplication.getPlaces().getDTOPlace();
	for ( var i = 0; i < bb.length; i++)
	{
		cxfPlaces[i] = bb[i];
		bb[i].id = bb[i]._id;
	}
	
	bb = cxfApplication.getGroups().getDTOPlaceGroup();
	for ( var i = 0; i < bb.length; i++)
	{
		bb[i].id = bb[i]._id;
	}

	$("#appName").text(cxfApplication._name);
	$("#appDescr").text(cxfApplication._description);

	initCommandPanel(cxfApplication);
	fillInPaletteData(cxfShellCommands);
	initNetworkROPanel(aa);
	initLogicalNetworkPanel(cxfApplication);
}

function addChain(c)
{
	cxfChains[c._id] = c;
	editChain(c);
}

function addShell(s)
{
	cxfShellCommands.push(s);
}

function send()
{
	proxy.stageApplication(sendApplicationOK, sendApplicationKO, cxfApplication);
}

function sendApplicationOK(response)
{
	$("#alert").text("stored");
}

function sendApplicationKO(response)
{
	alert("oups");
}

function addCommand(DTOShellCommand)
{

}