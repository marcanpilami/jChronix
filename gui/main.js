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
	
	// RAPHAEL BUG
	var r = $("#raphBug")[0];
	var rpaper = new Raphael(r, 10, 10);
	var arrow = rpaper.path("M,1,1,L,20,20");
	arrow.attr(
	{
		"arrow-end" : "classic-wide-long",
		"stroke-width" : arrowSize
	});
	
	// Always validate SlickGrid edits when loosing focus
	$("#tabs").on('blur', 'input.editor-text', function() {
	    Slick.GlobalEditorLock.commitCurrentEdit();
	});
	$("#tabs").on('blur', 'select.editor-select', function() {
	    Slick.GlobalEditorLock.commitCurrentEdit();
	});


	// Load data from webservice
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
	if (tabId === 'tab-pn')
		n = new NPPanel("tab-pn", cxfApplication);
	if (tabId === 'tab-ln')
		n = new LogicalNetworkPanel('tab-ln', cxfApplication);
	if (tabId === 'tab-cal')
		n = new CalendarPanel('tab-cal', cxfApplication);

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