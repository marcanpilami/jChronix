function ConsoleGrid(divId)
{
	this.logs = null;
	this.histGrid = null;
	this.options = null;
	this.columns = null;
	this.dataView = null;
	this.toolbar = null;
	this.container = null;
	this.extendOnRefresh = false;
	this.lastRefreshTime = new Date(1, 1, 1);
	this.columnFilters =
	{};

	// Divisions
	this.mainDiv = $("#" + divId);
	this.commandPanelDiv = $("<div/>");
	this.mainDiv.append(this.commandPanelDiv);
	this.gridDiv = $("<div id='grd" + divId + "' class='consoleGridDiv'></div>");
	this.mainDiv.append(this.gridDiv);
	this.mainDiv.append($("<div id='pager'/>"));

	// Grid options & columns
	this.options =
	{
		editable : false,
		enableAddRow : false,
		enableCellNavigation : true,
		enableColumnReorder : false,
		enableRowReordering : false,
		asyncEditorLoading : true,
		multiSelect : false,
		autoEdit : false,
		forceFitColumns : true,
		showHeaderRow : true,
		topPanelHeight : 90,
		explicitInitialization : true,
	};

	this.columns = [
	{
		id : "activeNodeName",
		name : "Node",
		field : "activeNodeName",
		cssClass : "cell-title",
		sortable : true,
		resizable : true,
		withFilter : true,
	},
	/*
	 * { id : "id", name : "ID", field : "id", cssClass : "cell-title", sortable : true, resizable : true, withFilter : true, },
	 */
	{
		id : "applicationName",
		name : "Application",
		field : "applicationName",
		cssClass : "cell-title",
		sortable : true,
		resizable : true,
		withFilter : true,
	},
	{
		id : "chainName",
		name : "Chain",
		field : "chainName",
		cssClass : "cell-title",
		sortable : true,
		resizable : true,
		withFilter : true,
	},
	{
		id : "beganRunningAt",
		name : "Started",
		field : "beganRunningAt",
		cssClass : "cell-title",
		sortable : true,
		resizable : true,
		minWidth : 70,
		formatter : this.dateFormatter,
		withFilter : true,
	},
	{
		id : "stoppedRunningAt",
		name : "Stopped",
		field : "stoppedRunningAt",
		cssClass : "cell-title",
		sortable : true,
		resizable : true,
		formatter : this.dateFormatter,
	},
	{
		id : "placeName",
		name : "Place",
		field : "placeName",
		cssClass : "cell-title",
		sortable : true,
		resizable : true,
		withFilter : true,
	},
	{
		id : "dns",
		name : "DNS",
		field : "dns",
		cssClass : "cell-title",
		sortable : true,
		resizable : true,
	},
	{
		id : "osAccount",
		name : "User account",
		field : "osAccount",
		cssClass : "cell-title",
		sortable : true,
		resizable : true,
	},
	{
		id : "executionNodeName",
		name : "Node",
		field : "executionNodeName",
		cssClass : "cell-title",
		sortable : true,
		resizable : true,
	},
	{
		id : "calendarName",
		name : "Calendar",
		field : "calendarName",
		cssClass : "cell-title",
		sortable : true,
		resizable : true,
		withFilter : true,
	},
	{
		id : "calendarOccurrence",
		name : "Occurrence",
		field : "calendarOccurrence",
		cssClass : "cell-title",
		sortable : true,
		resizable : true,
	},
	{
		id : "dataIn",
		name : "DataIn",
		field : "dataIn",
		cssClass : "cell-title",
		sortable : true,
		resizable : true,
		maxWidth : 65,
	},
	{
		id : "dataOut",
		name : "DataOut",
		field : "dataOut",
		cssClass : "cell-title",
		sortable : true,
		resizable : true,
		maxWidth : 75,
	}, ];

	// Create DataView (empty for now)
	this.dataView = new Slick.Data.DataView(
	{
		inlineFilters : true
	});
	this.dataView.getItemMetadata = this.getItemMetadata.bind(this);

	this.dataView.onRowCountChanged.subscribe((function(e, args)
	{
		this.histGrid.updateRowCount();
		this.histGrid.render();
	}).bind(this));

	this.dataView.onRowsChanged.subscribe((function(e, args)
	{
		this.histGrid.invalidateRows(args.rows);
		this.histGrid.render();
	}).bind(this));

	this.dataView.setFilter(this.filter);
	this.dataView.setFilterArgs(
	{
		local : this
	});

	// Create Grid
	this.histGrid = new Slick.Grid(this.gridDiv, this.dataView, this.columns, this.options);
	this.histGrid.setSelectionModel(new Slick.RowSelectionModel());

	// Initialize floating panel
	this.container = this.mainDiv.after($("<div/>"));
	this.toolbar = new ConsoleFloatingPanel($("#mainGrid"), this);

	// Events
	this.histGrid.onSelectedRowsChanged.subscribe($.proxy(function(e, args)
	{
		if (args.rows.length >= 1)
		{
			var pos = this.histGrid.getActiveCellPosition();
			this.toolbar.show(pos.left, pos.top, this.logs[args.rows[0]]);
		}
		else
			this.toolbar.hide();
	}, this));
	this.histGrid.onScroll.subscribe((function()
	{
		this.toolbar.hide();
	}).bind(this));

	// Create date filter panel
	this.commandPanelDiv.addClass("topFixedPanel");
	this.startTime = $("<input id='" + divId + "from' type='text'/>");
	this.commandPanelDiv.append($("<label for='" + divId + "from'>From: </label>"));
	this.commandPanelDiv.append(this.startTime);
	this.startTime.datetimepicker(
	{
		timeFormat : "HH:mm",
		dateFormat : 'dd/mm/yy',
	});
	this.endTime = $("<input id='" + divId + "to' type='text'/>");
	this.commandPanelDiv.append($("<label for='" + divId + "to'>To: </label>"));
	this.commandPanelDiv.append(this.endTime);
	this.endTime.datetimepicker(
	{
		timeFormat : "HH:mm",
		dateFormat : 'dd/mm/yy',
	});
	this.startTime.val(new Date().addHours(-24).toString("dd/MM/yyyy HH:mm"));
	this.endTime.val(new Date().addMinutes(1).toString("dd/MM/yyyy HH:mm"));

	var btRefresh = $("<button type='button'>Refresh</button>");
	this.commandPanelDiv.append(btRefresh);
	btRefresh.click(this.refreshData.bind(this));

	this.endTime.change(this.endDateChange.bind(this));
	this.endDateChange();
	this.startTime.change(this.startDateChange.bind(this));

	// Pager
	this.pager = new Slick.Controls.Pager(this.dataView, this.histGrid, $("#pager"));

	// Size
	$(window).resize(this.onViewPortResize.bind(this));
	this.onViewPortResize();

	this.columnpicker = new Slick.Controls.ColumnPicker(this.columns, this.histGrid, this.options);

	// Add filters
	$(this.histGrid.getHeaderRow()).delegate(":input", "change keyup", (function(e)
	{
		var columnId = $(e.currentTarget).data("columnId");
		if (columnId != null)
		{
			// Clear on Esc
			if (e.which == 27)
			{
				$(e.currentTarget).val("");
				this.columnFilters[columnId] = "";
			}
			else
				this.columnFilters[columnId] = $.trim($(e.currentTarget).val());
			this.dataView.refresh();
		}
	}).bind(this));

	this.histGrid.onHeaderRowCellRendered.subscribe((function(e, args)
	{
		// $(args.node).empty();
		if (args.column.withFilter)
		{
			$(args.node).empty();
			$("<input type='text'>").data("columnId", args.column.id).val("").appendTo(args.node);
		}
	}).bind(this));

	this.histGrid.init();

	// Initialize data
	this.refreshData();

	cf = this.columnFilters;
}

ConsoleGrid.prototype.filter = function(item, args)
{
	for ( var columnId in args.local.columnFilters)
	{
		if (columnId !== undefined && args.local.columnFilters[columnId] !== "")
		{
			var c = args.local.histGrid.getColumns()[args.local.histGrid.getColumnIndex(columnId)];
			if (item[c.field].indexOf(args.local.columnFilters[columnId]) === -1)
			{
				return false;
			}
		}
	}
	return true;
};

ConsoleGrid.prototype.onViewPortResize = function()
{
	// this.gridDiv.width($(window).width());
	this.gridDiv.height($(window).height() - $("#pager").height() - this.gridDiv.css("padding-top").replace("px", "") - 5);
	this.histGrid.resizeCanvas();
};

ConsoleGrid.prototype.dateFormatter = function(row, cell, value, columnDef, dataContext)
{
	if (value == null || value === "")
	{
		return "-";
	}
	return new Date(value).toString("dd/MM HH:mm:ss");
};

ConsoleGrid.prototype.endDateChange = function()
{
	this.extendOnRefresh = false;
	if (this.endTime.datetimepicker('getDate').isAfter(new Date().addMinutes(-30)))
		this.extendOnRefresh = true;
	this.lastRefreshTime = new Date(1, 1, 1);
};

ConsoleGrid.prototype.startDateChange = function()
{
	this.lastRefreshTime = new Date(1, 1, 1);
};

ConsoleGrid.prototype.refreshData = function()
{
	d = this.extendOnRefresh;
	var from = this.startTime.datetimepicker('getDate');
	var to = this.endTime.datetimepicker('getDate');

	if (this.extendOnRefresh)
	{
		$.getJSON("console/rest/main/logs/" + from.toJSON() + "/" + to.toJSON() + "/" + this.lastRefreshTime.toJSON(), this.getLogsOK.bind(this));
		this.endTime.val(new Date().addMinutes(1).toString("dd/MM/yyyy HH:mm"));
		this.lastRefreshTime = new Date();
	}
	else
		$.getJSON("console/rest/main/logs/" + from.toJSON() + "/" + to.toJSON() + "/" + from.toJSON(), this.getLogsOK.bind(this));

};

ConsoleGrid.prototype.getItemMetadata = function(index)
{
	var obj = this.logs[index];
	var res =
	{
		cssClasses : "consoleRowUnkwown"
	};

	if (typeof (obj.lastKnownStatus) == 'string' && obj.lastKnownStatus.substring(0, 9) === 'OVERRIDEN')
	{
		res =
		{
			cssClasses : "consoleRowUser"
		};
		return res;
	}
	if (typeof (obj.lastKnownStatus) == 'string' && obj.lastKnownStatus.substring(0, 4) !== 'DONE')
	{
		res =
		{
			cssClasses : "consoleRowNotEnded"
		};
		return res;
	}

	if (obj.resultCode === 0)
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

ConsoleGrid.prototype.getLogsOK = function(json)
{
	if (!this.extendOnRefresh || this.logs === null || this.logs.length === 0)
	{
		this.logs = json.DTORunLog;
		this.dataView.beginUpdate();
		this.dataView.setItems(this.logs, "id");
		this.dataView.endUpdate();
	}
	else
	{
		var m = json.DTORunLog.length;
		for ( var i = 0; i < m; i++)
			this.dataView.addItem(json.DTORunLog[i]);
	}
	ll = this.logs.length;
};

ConsoleGrid.prototype.getLogsKO = function(httpStatus, httpStatusText)
{
	alert('error HTTP ' + httpStatus + "\n" + httpStatusText);
};