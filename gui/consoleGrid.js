function ConsoleGrid(cxfProxy, divId)
{
	this.proxy = cxfProxy;
	this.logs = null;
	this.histGrid = null;
	this.options = null;
	this.columns = null;
	this.dataView = null;
	this.toolbar = null;
	this.container = null;

	this.options =
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

	this.columns = [
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

	// Create DataView (empty for now)
	this.dataView = new Slick.Data.DataView(
	{
		inlineFilters : true
	});
	this.dataView.getItemMetadata = $.proxy(this.getItemMetadata, this);

	this.dataView.onRowCountChanged.subscribe($.proxy(function(e, args)
	{
		this.histGrid.updateRowCount();
		this.histGrid.render();
	}, this));

	this.dataView.onRowsChanged.subscribe($.proxy(function(e, args)
	{
		this.histGrid.invalidateRows(args.rows);
		this.histGrid.render();
	}, this));

	// Create Grid
	this.histGrid = new Slick.Grid("#" + divId, this.dataView, this.columns, this.options);
	this.histGrid.setSelectionModel(new Slick.RowSelectionModel());

	// Call web service to get initial data
	this.proxy.getLog($.proxy(this.getLogsOK, this), $.proxy(this.getLogsKO, this), null, null);

	// Init floating toolbar
	this.container = $("#mainGrid").after($("<div/>"));
	this.toolbar = new ConsoleFloatingPanel($("#mainGrid"));

	// Events
	this.histGrid.onSelectedRowsChanged.subscribe($.proxy(function(e, args)
	{
		var pos = this.histGrid.getActiveCellPosition();
		this.toolbar.show(pos.left, pos.top, this.logs[args.rows[0]]);
	}, this));
}

ConsoleGrid.prototype.getItemMetadata = function(index)
{
	var obj = this.logs[index];
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
};

ConsoleGrid.prototype.getLogsOK = function(responseObject)
{
	this.logs = responseObject.getReturn().getRunLog();

	this.dataView.beginUpdate();
	this.dataView.setItems(this.logs, "_id");
	this.dataView.endUpdate();
};

ConsoleGrid.prototype.getLogsKO = function(httpStatus, httpStatusText)
{
	alert('error HTTP ' + httpStatus + "\n" + httpStatusText);
};