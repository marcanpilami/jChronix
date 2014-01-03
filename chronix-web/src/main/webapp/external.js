function ExternalPanel(mainDivId, dtoApplication)
{
	this.mainDiv = $("#" + mainDivId);
	this.dtoApplication = dtoApplication;
	var options = getSlickGridOptionsEditable();

	this.mainDiv.append($("<label>Search: </label>"));
	this.txtSearch = $("<input type='text' style='width: 100px;'>");
	this.mainDiv.append(this.txtSearch);

	this.containerContainer = $("<div class='containerContainer'></div>");
	this.mainDiv.append(this.containerContainer);
	this.slickContainer = $("<div></div>");
	this.containerContainer.append(this.slickContainer);

	var columns = [
	{
		id : "name",
		name : "External event short name",
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
		editor : Slick.Editors.Text,
		validator : requiredFieldValidator,
		cannotTriggerInsert : true,
		sortable : true,
		resizable : true,
	},
	{
		id : "regexp",
		name : "Calendar occurrence extractor (regular expression)",
		field : "_regularExpression",
		minWidth : 100,
		cssClass : "cell-title",
		editor : Slick.Editors.Text,
		cannotTriggerInsert : true,
		sortable : false,
		resizable : true,
	},
	{
		id : "login",
		name : "Optional shared secret",
		field : "_accountRestriction",
		minWidth : 100,
		cssClass : "cell-title",
		editor : Slick.Editors.Text,
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
	this.txtSearch.keyup(searchBoxKeyup.bind(this));

	// Specific events
	this.mainGrid.onAddNewRow.subscribe((function(e, args)
	{
		var v = new dto_chronix_oxymores_org_DTOExternal();
		for ( var o in args.item)
		{
			v[o] = args.item[o];
		}
		v._id = uuid.v4();
		v.id = v._id;
		this.dataview.addItem(v);
	}).bind(this));

	this.slickContainer.on('click', '.delcmd', (function(e)
	{
		var id = e.currentTarget.id.substr(3);

		// Check the command is not used in a chain
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
			alert("This external event is used inside chains and therefore cannot be deleted. First free it from its chains.");
			return;
		}

		// Destroy the element through the dataview (will in turn update cxfApplication)
		this.dataview.deleteItem(id);
	}).bind(this));

	// Populate & go
	this.dataview.beginUpdate();
	this.dataview.setItems(cxfApplication.getExternals().getDTOExternal(), '_id');
	this.dataview.setFilterArgs(
	{
		searchString : "",
	});
	this.dataview.setFilter(nameDescriptionFilter);
	this.dataview.endUpdate();

	this.resize();
	this.mainGrid.init();
	$(window).resize(this.resize.bind(this));
}

ExternalPanel.prototype.redisplay = function()
{
	this.resize();
};

ExternalPanel.prototype.resize = slResize;
