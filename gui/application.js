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
	alert("Could not retrieve the application from the server: " + responseObject);
}

function getApplicationOK(responseObject)
{
	cxfApplication = responseObject.getReturn();

	// Quick access shortcuts
	cxfApplication.places = new Array();
	cxfApplication.placeGroups = new Array();
	cxfApplication.chains = new Array();

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
	if (cxfApplication._active)
		$("#appActivity").text("loaded production version");
	else
		$("#appActivity").text("loaded draft version");

	handleTabs('tab-pn');
}

function addChain(c)
{
	cxfChains[c._id] = c;
	// editChain(c);
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
	alert("Could not store the application. Server returned: " + response);
}

function addCommand(DTOShellCommand)
{

}

function deletePlace(dtoApplication, dtoPlace)
{
	// Get a usable PG table
	var placeGroups = dtoApplication.getGroups().getDTOPlaceGroup();
	var placeGroupsA = new Object();
	var pg = null;
	for ( var i = 0; i < placeGroups.length; i++)
	{
		pg = placeGroups[i];
		placeGroupsA[pg._id] = pg;
	}

	// Remove from groups
	var groupids = dtoPlace.getMemberOf().getString();
	var tmp = null;
	for ( var i = 0; i < groupids.length; i++)
	{
		pg = placeGroupsA[groupids[i]];
		tmp = pg.getPlaces().getString();
		tmp.splice(tmp.indexOf(dtoPlace._id));
	}

	// Remove from application
	tmp = dtoApplication.getPlaces().getDTOPlace();
	tmp.splice(tmp.indexOf(dtoPlace));
}