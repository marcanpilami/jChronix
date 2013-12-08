function CommandPanel(cxfApplication)
{
	this.dtoApplication = cxfApplication;

	var options = getSlickGridOptionsEditable();

	var columns = [
	{
		id : "name",
		name : "Command short name",
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
		id : "cmde",
		name : "Shell command line to run",
		field : "_command",
		minWidth : 100,
		cssClass : "cell-title",
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
	this.mainGrid = new Slick.Grid("#gridCommands", this.dataview, columns, options);
	this.mainGrid.setSelectionModel(new Slick.RowSelectionModel());

	// Plumbing events
	this.mainGrid.onCellChange.subscribe(onCellChange.bind(this));
	this.mainGrid.onSort.subscribe(onSort.bind(this));
	this.dataview.onRowCountChanged.subscribe(onRowCountChanged.bind(this));
	this.dataview.onRowsChanged.subscribe(onRowsChanged.bind(this));
	$("#txtSearchCmd").keyup(searchBoxKeyup.bind(this));

	// Specific events
	this.mainGrid.onAddNewRow.subscribe((function(e, args)
	{
		var v = new dto_chronix_oxymores_org_DTOShellCommand();
		for ( var o in args.item)
		{
			v[o] = args.item[o];
		}
		v._id = uuid.v4();
		v.id = v._id;
		this.dataview.addItem(v);
	}).bind(this));

	$('#gridCommands').on('click', '.delcmd', (function(e)
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
			alert("This command is used inside chains and therefore cannot be deleted. First free it from its chains.");
			return;
		}

		// Destroy the cmd through the dataview (will in turn update cxfApplication)
		this.dataview.deleteItem(id);
	}).bind(this));

	// Populate & go
	this.dataview.beginUpdate();
	this.dataview.setItems(cxfApplication.getShells().getDTOShellCommand(), '_id');
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

CommandPanel.prototype.redisplay = function()
{
	this.resize();
};

CommandPanel.prototype.resize = function()
{
	if (!$("#gridCommands").is(':visible'))
		return;
	$("#gridCommands").height(0);
	$("#gridCommands").height($("#gridCommandsContainer").height());
	$("#gridCommands").width($("#gridCommandsContainer").width());
	this.mainGrid.resizeCanvas();
};

function delCmdBtFormatter(row, cell, value, columnDef, dataContext)
{
	return "<button class='delcmd' type='button' id='del" + dataContext._id + "' >X</button>";
}
function editCmdBtFormatter(row, cell, value, columnDef, dataContext)
{
	return "<button class='editcmd' type='button' id='edit" + dataContext._id + "' >E</button>";
}