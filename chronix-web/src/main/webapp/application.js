var cxfApplication = null;
var cxfShellCommands = new Array();
var cxfChains = new Object();
var cxfPlaces = [];
var appLoader = null;

function loadApplication()
{
	proxy.getFirstApplication(getApplicationOK, getApplicationKO);
}

function loadApplicationId(id)
{
	proxy.getApplicationById(getApplicationOK, getApplicationKO, id);
}

function getApplicationKO(responseObject)
{
	alert("Could not retrieve the application from the server: " + responseObject);
}

function getApplicationOK(responseObject)
{
	cxfApplication = responseObject.getReturn();

	// Reset panels
	loaded = new Array();
	$("#tab-ln").empty();
	$("#tab-pn").empty();
	$("#tab-cal").empty();
	$("#tab-schedule").empty();
	$("#gridCommands").empty();
	$("#chaintabs").empty();
	$("#chaintabs").append($("<ul></ul>"));

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
	/*
	 * if (cxfApplication._active) $("#appActivity").text("loaded production version"); else $("#appActivity").text("loaded draft version");
	 */

	$("[href='#tab-pn']").trigger("click");
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
	proxy.validateApp(validateAppPreSwitchOk, validateAppKo, cxfApplication);
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

function createOrLoadApp()
{
	if (appLoader === null)
		appLoader = new ChangeApp();
	else
		appLoader.show();
}

function ChangeApp()
{
	this.applications = null;

	this.mainDiv = $("<div></div>");
	$("body").append(this.mainDiv);

	// First thing: hide the application
	this.mainDiv.css(
	{
		"position" : "absolute",
		"top" : "0",
		"bottom" : "0",
		"left" : "0",
		"right" : "0",
		"background-color" : "grey",
		"z-index" : "999",
	});

	this.appListDiv = $("<div></div>");
	this.mainDiv.append(this.appListDiv);

	this.newName = $("<input type='text' maxLength='20'>");
	this.newDescription = $("<input type='text' maxLength='50'>");
	this.newBt = $("<button>Create</button>");

	this.newDiv = $("<div class='app-short'></div>");
	this.newDiv.append($("<label>New application name: </label>"));
	this.newDiv.append(this.newName);
	this.newDiv.append($("<br><label>New application description: </label>"));
	this.newDiv.append(this.newDescription);
	this.newDiv.append(this.newBt);
	this.mainDiv.append(this.newDiv);

	this.newBt.click(this.newApp.bind(this));

	// Get the list of applications
	this.show();
}

ChangeApp.prototype.getAllApplicationsOK = function(response)
{
	this.applications = response.getReturn().getDTOApplicationShort();
	for ( var i = 0; i < this.applications.length; i++)
	{
		var aa = this.applications[i];
		var l = $("<div class='app-short' id='short" + aa._id + "' ><div>" + aa._name + "</div><div class='descrtxt'>" + aa._description
				+ "</div></div>");
		this.appListDiv.append(l);
		l.on('click', this.loadApp.bind(this));
	}
};

ChangeApp.prototype.newApp = function()
{
	proxy.createApplication(getApplicationOK, getApplicationKO, this.newName.val(), this.newDescription.val());
	this.mainDiv.hide(600);
};

ChangeApp.prototype.loadApp = function(e)
{
	loadApplicationId($(e.target).parents(".app-short").prop('id').substr(5));
	this.mainDiv.hide(600);
};

ChangeApp.prototype.getAllApplicationsKO = function(response)
{
	alert("Could not load application list. Server returned: " + response);
};

ChangeApp.prototype.hide = function()
{
	this.mainDiv.hide();
};

ChangeApp.prototype.show = function()
{
	this.appListDiv.empty();
	proxy.getAllApplications(this.getAllApplicationsOK.bind(this), this.getAllApplicationsKO.bind(this));
	this.mainDiv.show(300);
};

function validateAppPreSwitchOk(responseObject)
{
	alerts = responseObject.getReturn().getDTOValidationError();
	if (alerts.length > 0)
	{
		// Display the errors and don't switch
		validateAppOk(responseObject);
	}
	else
	{
		// No errors - switch the apps
		proxy.storeApplication(sendApplicationOK, sendApplicationKO, cxfApplication._id);
	}
}

function validateAppOk(responseObject)
{
	$("#alert").text("validation result arrived");

	alerts = responseObject.getReturn().getDTOValidationError();

	var str = "<table><tr><th>Item type</th><th>Item name</th><th>Faulty attribute</th><th>Error message</th><th>Faulty value</th></tr>";
	for ( var i = 0; i < alerts.length; i++)
	{
		var issue = alerts[i];
		str += "<tr>";

		str += "<td>" + issue._itemType + "</td>";
		str += "<td>" + issue._itemIdentification + "</td>";
		str += "<td>" + issue._errorPath + "</td>";
		str += "<td>" + issue._errorMessage + "</td>";
		str += "<td>" + issue._erroneousValue + "</td>";
		str += "</tr>";
	}
	str = str + "</table>";

	if (alerts.length > 0)
	{
		$("#alert").text("issues were detected during validation");
		$('<div />').html(str).dialog(
		{
			modal : true,
			title : 'invalid plan',
			width : '70%',
			maxHeight : 400,
			buttons :
			{
				Close : function()
				{
					$(this).dialog("close");
				}
			}
		});
	}
	else
	{
		$("#alert").text("no issues detected during validation");
	}
}

function validateAppKo(responseObject)
{
	alert("Could not validate the application on the server: " + responseObject);
}

function validateApp()
{
	proxy.validateApp(validateAppOk, validateAppKo, cxfApplication);
}
