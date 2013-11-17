var cxfApplication = null;
var cxfShellCommands = new Array();
var cxfChains = new Object();
var cxfPlaces = [];

function loadApplication()
{
	proxy.getApplication(getApplicationOK, getApplicationKO, "test app");
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

function switchApp()
{
	proxy.storeApplication(sendApplicationOK, sendApplicationKO, cxfApplication._id);
}

function deletePlace(dtoApplication, dtoPlace)
{
	// Remove from groups
	var gg = dtoApplication.getGroups().getDTOPlaceGroup();
	for ( var i = 0; i < gg.length; i++)
	{
		var idx = jQuery.inArray(dtoPlace._id, gg[i]._places.getString());
		if (-1 !== idx)
			gg[i]._places.getString().splice(idx, 1);
	}

	// Remove from application
	tmp = dtoApplication.getPlaces().getDTOPlace();
	tmp.splice(tmp.indexOf(dtoPlace), 1);
}