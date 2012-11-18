$(document).ready(function()
{
	var cxfProxy = new internalapi_chronix_oxymores_org__IServiceConsolePortType();
	
	// Create grid
	grid = new ConsoleGrid(cxfProxy, "mainGrid");

});