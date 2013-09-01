var proxy = null;

$(document).ready(function()
{
	$(window).resize(onViewPortResize);
	onViewPortResize();

	$("#tabs").tabs(
	{
		activate : function(event, ui)
		{
			handleTabs(ui.newPanel.attr('id'));
		}
	});

	proxy = new internalapi_chronix_oxymores_org__IServiceClientPortType();
	loadApplication(); // will trigger first panel init when done
});

// Function to create panels only when they are displayed (Slickgrid requires a displayed div with known size to init) 
var loaded = new Array();
function handleTabs(tabId)
{
	if (loaded[tabId] !== undefined && loaded[tabId] !== null)
		loaded[tabId].redisplay(); // just update display

	if (loaded[tabId] !== undefined)
		return; // already initialized

	// Create new tab
	var n = null;
	if (tabId === 'tab-command')
		n = new CommandPanel(cxfApplication);
	if (tabId === 'tab-schedule')
		n = new SchedulePanel("tab-schedule", cxfApplication);
	if (tabId === 'tab-chain')
		n = new ChainPanel("tab-chain", cxfApplication);

	loaded[tabId] = n;
}

function loadApplicationsList()
{
}

function pingServer()
{
	var p = new internalapi_chronix_oxymores_org__IServiceClientPortType();
	p.sayHello(successCallback, errorCallback, "marsu", 12);
}

function errorCallback(httpStatus, httpStatusText)
{
	alert('error ' + httpStatusText);
}

function successCallback(responseObject)
{
	// alert(responseObject.getReturn());
	$("#greetings").html(responseObject.getReturn() + " - " + uuid.v4());
}

function onViewPortResize()
{
	$("#tabs").height($(window).height() - 50);
};