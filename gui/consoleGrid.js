var proxy = null;
var logs =
{};
var histGrid = null;
var options, columns;

$(document).ready(function()
{
	proxy = new internalapi_chronix_oxymores_org__IServiceConsolePortType();

	options =
	{
		editable : false,
		enableAddRow : false,
		enableCellNavigation : true,
		enableColumnReorder : false,
		enableRowReordering : false,
		asyncEditorLoading : false,
		showHeaderRow : false,
		multiSelect : false,
		enableTextSelectionOnCells : false, // ???
		rowHeight : 30,
		autoHeight : true,
		autoEdit : false,
		forceFitColumns : true,
	};

	columns = [
	{
		id : "_activeNodeName",
		name : "Node",
		field : "_activeNodeName",
		cssClass : "cell-title",
		sortable : true,
		resizable : true,
	},
	{
		id : "_applicationName",
		name : "Application",
		field : "_applicationName",
		cssClass : "cell-title",
		sortable : true,
		resizable : true,
	},
	{
		id : "_chainName",
		name : "Chain",
		field : "_chainName",
		cssClass : "cell-title",
		sortable : true,
		resizable : true,
	},
	{
		id : "_beganRunningAt",
		name : "Started",
		field : "_beganRunningAt",
		cssClass : "cell-title",
		sortable : true,
		resizable : true,
		minWidth : 200,
	},
	{
		id : "_stoppedRunningAt",
		name : "Stopped",
		field : "_stoppedRunningAt",
		cssClass : "cell-title",
		sortable : true,
		resizable : true,
	},
	{
		id : "_lastKnownStatus",
		name : "Status",
		field : "_lastKnownStatus",
		cssClass : "cell-title",
		sortable : true,
		resizable : true,
	},
	{
		id : "_placeName",
		name : "Place",
		field : "_placeName",
		cssClass : "cell-title",
		sortable : true,
		resizable : true,
	},
	{
		id : "_dns",
		name : "DNS",
		field : "_dns",
		cssClass : "cell-title",
		sortable : true,
		resizable : true,
	},
	{
		id : "_osAccount",
		name : "Os account",
		field : "_osAccount",
		cssClass : "cell-title",
		sortable : true,
		resizable : true,
	},
	{
		id : "_executionNodeName",
		name : "ExecNodeName",
		field : "_executionNodeName",
		cssClass : "cell-title",
		sortable : true,
		resizable : true,
	},
	{
		id : "_calendarName",
		name : "Calendar",
		field : "_calendarName",
		cssClass : "cell-title",
		sortable : true,
		resizable : true,
	},
	{
		id : "_calendarOccurrence",
		name : "Occurrence",
		field : "_calendarOccurrence",
		cssClass : "cell-title",
		sortable : true,
		resizable : true,
	},
	{
		id : "_dataIn",
		name : "DataIn",
		field : "_dataIn",
		cssClass : "cell-title",
		sortable : true,
		resizable : true,
	},
	{
		id : "_dataOut",
		name : "DataOut",
		field : "_dataOut",
		cssClass : "cell-title",
		sortable : true,
		resizable : true,
	}, ];

	proxy.getLog(getLogsOK, getLogsKO, null, null);
});

var obj = null;
function getItemMetadata(index)
{
	obj = logs[index];
	var res =
	{
		cssClasses : "consoleRowUnkwown"
	};

	if (typeof (obj._lastKnownStatus) == 'string' && obj._lastKnownStatus.substring(0, 4) !== 'DONE')
	{
		res =
		{
			cssClasses : "consoleRowNotEnded"
		};
		return res;
	}

	if (obj._resultCode === 0)
	{
		res =
		{
			cssClasses : "consoleRowOK"
		};
		return res;
	}
	else
	{
		res =
		{
			cssClasses : "consoleRowKO"
		};
		return res;
	}

	return res;
}

function getLogsOK(responseObject)
{
	logs = responseObject.getReturn().getRunLog();
	logs.getItemMetadata = getItemMetadata;
	histGrid = new Slick.Grid("#mainGrid", logs, columns, options);
	histGrid.setSelectionModel(new Slick.RowSelectionModel());
}

function getLogsKO(httpStatus, httpStatusText)
{
	alert('error HTTP ' + httpStatus + "\n" + httpStatusText);
}
