function CalendarPanel(mainDivId, dtoApplication)
{
	this.mainDiv = $("#" + mainDivId);
	this.dtoApplication = dtoApplication;
	var options = getSlickGridOptionsEditable();
	this.selectedCalendar = null;

	this.calPane = $("<div class='leftbar' ></div>");
	this.mainDiv.append(this.calPane);
	this.txtSearchCal = $("<input type='text' style='width: 100px;'>");
	this.calPane.append(this.txtSearchCal);

	this.containerContainer = $("<div class='containerContainer'></div>");
	this.calPane.append(this.containerContainer);
	this.slickContainer = $("<div></div>");
	this.containerContainer.append(this.slickContainer);
	this.slickContainer = $("<div></div>");
	this.containerContainer.append(this.slickContainer);

	this.dayPane = $("<div class='rightpanel'></div>");
	this.mainDiv.append(this.dayPane);
	this.daysPanelDiv = $("<div style='width:100%; height:100%'></div>");
	this.dayPane.append(this.daysPanelDiv);

	var columns = [
	{
		id : "name",
		name : "Name",
		field : "_name",
		minWidth : 70,
		cssClass : "cell-title",
		editor : Slick.Editors.Text,
		validator : requiredFieldValidator,
		sortable : true,
		resizable : true,
	},
	{
		id : "description",
		name : "Description",
		field : "_description",
		minWidth : 150,
		// selectable : false,
		editor : Slick.Editors.Text,
		validator : requiredFieldValidator,
		cannotTriggerInsert : true,
		sortable : false,
		resizable : true,
	},
	{
		id : "del",
		name : "",
		field : "del",
		maxWidth : 35,
		formatter : delCmdBtFormatter,
		cannotTriggerInsert : true,
	}, ];

	this.dataview = new Slick.Data.DataView(
	{
		inlineFilters : true
	});
	this.mainGrid = new Slick.Grid(this.slickContainer, this.dataview, columns, options);
	this.mainGrid.setSelectionModel(new Slick.RowSelectionModel());

	// Plumbing events
	this.mainGrid.onCellChange.subscribe(onCellChange.bind(this));
	this.mainGrid.onSort.subscribe(onSort.bind(this));
	this.dataview.onRowCountChanged.subscribe(onRowCountChanged.bind(this));
	this.dataview.onRowsChanged.subscribe(onRowsChanged.bind(this));
	this.txtSearchCal.keyup(searchBoxKeyup.bind(this));

	// Specific events
	this.mainGrid.onAddNewRow.subscribe((function(e, args)
	{
		var v = new dto_chronix_oxymores_org_DTOCalendar();
		for ( var o in args.item)
		{
			v[o] = args.item[o];
		}
		v._id = uuid.v4();
		v._days = new dto_chronix_oxymores_org_ArrayOfDTOCalendarDay();
		this.dataview.addItem(v);
		this.daysPanel.setItem(v);
	}).bind(this));

	this.mainDiv.on('click', '.delcmd', (function(e)
	{
		var id = e.currentTarget.id.substr(3);

		// Check the calendar is not used in a chain
		var usedBy = new Array();
		var chains = this.dtoApplication.getChains().getDTOChain();

		for ( var j = 0; j < chains.length; j++)
		{
			var chain = chains[j];
			var states = chain.getStates().getDTOState();
			for ( var i = 0; i < states.length; i++)
			{
				var state = states[i];
				if (state._representsId === id)
					usedBy.push(chain);
			}
		}

		if (usedBy.length > 0)
		{
			alert("This command is used inside chains and therefore cannot be deleted. First free it from its chains.");
			return;
		}

		// Destroy the calendar through the dataview (will in turn update cxfApplication)
		this.dataview.deleteItem(id);
	}).bind(this));

	this.mainGrid.onSelectedRowsChanged.subscribe((function()
	{
		this.selectedCalendar = this.mainGrid.getDataItem(this.mainGrid.getSelectedRows()[0]);

		if (this.selectedCalendar != undefined)
		{
			this.daysPanel.setItem(this.selectedCalendar);
		}
		else
		{
			this.daysPanel.setItem(null);
		}
		this.daysPanel.dataview.refresh();
	}).bind(this));

	// Populate & go
	this.dataview.beginUpdate();
	this.dataview.setItems(cxfApplication.getCalendars().getDTOCalendar(), '_id');
	this.dataview.setFilterArgs(
	{
		searchString : "",
	});
	this.dataview.setFilter(nameDescriptionFilter);
	this.dataview.endUpdate();

	this.resize();
	this.mainGrid.init();
	$(window).resize(slResize.bind(this));

	this.daysPanel = new CalendarDaySubPanel(this.daysPanelDiv);
}

CalendarPanel.prototype.resize = slResize;

function CalendarDaySubPanel(mainDiv)
{
	this.mainDiv = mainDiv;
	var options = getSlickGridOptionsEditable();

	this.containerContainer = $("<div class='containerContainer'></div>");
	this.mainDiv.append(this.containerContainer);
	this.slickContainer = $("<div></div>");
	this.containerContainer.append(this.slickContainer);

	var columns = [
	{
		id : "_seq",
		name : "Occurrence",
		field : "_seq",
		minWidth : 70,
		cssClass : "cell-title",
		editor : Slick.Editors.Text,
		validator : requiredFieldValidator,
		sortable : false,
		resizable : true,
	},
	{
		id : "del",
		name : "",
		field : "del",
		maxWidth : 35,
		formatter : delCmdBtFormatter,
		cannotTriggerInsert : true,
	}, ];

	this.dataview = new Slick.Data.DataView(
	{
		inlineFilters : false
	});
	this.mainGrid = new Slick.Grid(this.slickContainer, this.dataview, columns, options);
	this.mainGrid.setSelectionModel(new Slick.RowSelectionModel());

	// Plumbing events
	this.mainGrid.onCellChange.subscribe(onCellChange.bind(this));
	this.dataview.onRowCountChanged.subscribe(onRowCountChanged.bind(this));
	this.dataview.onRowsChanged.subscribe(onRowsChanged.bind(this));

	// Specific events
	this.mainGrid.onAddNewRow.subscribe((function(e, args)
	{
		var v = new dto_chronix_oxymores_org_DTOCalendarDay();
		for ( var o in args.item)
		{
			v[o] = args.item[o];
		}
		v._id = uuid.v4();
		this.dataview.addItem(v);
	}).bind(this));

	this.mainDiv.on('click', '.delcmd', (function(e)
	{
		var id = e.currentTarget.id.substr(3);

		// Destroy the calendar through the dataview (will in turn update cxfApplication)
		this.dataview.deleteItem(id);
	}).bind(this));
	uuu = this.dataview;
	$(window).resize(slResize.bind(this));
}

CalendarDaySubPanel.prototype.setItem = function(dtoCalendar)
{
	this.dataview.beginUpdate();
	if (dtoCalendar !== null)
	{
		this.dataview.setItems(dtoCalendar.getDays().getDTOCalendarDay(), '_id');
	}
	else
	{
		this.dataview.setItems([]);
	}
	this.dataview.endUpdate();
	this.resize();
	this.mainGrid.init();
};

CalendarDaySubPanel.prototype.resize = slResize;