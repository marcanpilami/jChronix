function CommandPanel(cxfApplication)
{
	var options =
	{
		editable : true,
		enableAddRow : true,
		enableCellNavigation : true,
		enableColumnReorder : false,
		enableRowReordering : false,
		asyncEditorLoading : true,
		showHeaderRow : false,
		multiSelect : false,
		autoEdit : true,
		enableTextSelectionOnCells : false,
		autoHeight : false,
		forceFitColumns : true,
		fullWidthRows : true,
		explicitInitialization : true,
		syncColumnCellResize : true,
	};

	var columns = [
	{
		id : "name",
		name : "Command short name",
		field : "_name",
		minWidth : 70,
		cssClass : "cell-title",
		editor : Slick.Editors.Text,
		validator : requiredFieldValidatorCmd,
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
		validator : requiredFieldValidatorCmd,
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
		validator : requiredFieldValidatorCmd,
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

	this.cmdDataViewCmds = new Slick.Data.DataView(
	{
		inlineFilters : true
	});
	this.cmdGrid = new Slick.Grid("#gridCommands", this.cmdDataViewCmds, columns, options);

	this.cmdGrid.setSelectionModel(new Slick.RowSelectionModel());
	this.cmdGrid.onAddNewRow.subscribe(onNewRow);
	this.cmdGrid.onCellChange.subscribe((function(e, args)
	{
		this.cmdDataViewCmds.updateItem(args.item.id, args.item);
	}).bind(this));
	this.cmdGrid.onSort.subscribe((function(e, args)
	{
		sortdir = args.sortAsc ? 1 : -1;
		sortcol = args.sortCol.field;
		this.cmdDataViewCmds.sort(cmdNameComparer, args.sortAsc);
	}).bind(this));
	this.cmdDataViewCmds.onRowCountChanged.subscribe((function(e, args)
	{
		this.cmdGrid.updateRowCount();
		this.cmdGrid.render();
	}).bind(this));

	this.cmdDataViewCmds.onRowsChanged.subscribe((function(e, args)
	{
		this.cmdGrid.invalidateRows(args.rows);
		this.cmdGrid.render();
	}).bind(this));
	$("#txtSearchCmd").keyup((function(e)
	{
		Slick.GlobalEditorLock.cancelCurrentEdit();
		
		// Clear on Esc
		if (e.which == 27)
		{
			$(e.currentTarget).val("");
		}
		// Update dataview filter
		this.cmdDataViewCmds.setFilterArgs(
		{
			searchString : $.trim($(e.currentTarget).val()),
		});
		// Refresh dataview
		this.cmdDataViewCmds.refresh();
	}).bind(this));
	$('#gridCommands').on('click', '.delcmd', (function()
	{
		var me = $(this), id = me.attr('_id');

		// Clean states of this Place????
		// TODO

		// Destroy the cmd through the dataview (will in turn update cxfApplication)
		this.cmdDataViewCmds.deleteItem(id);
	}).bind(this));

	// Populate & go
	this.cmdDataViewCmds.beginUpdate();
	this.cmdDataViewCmds.setItems(cxfApplication.getShells().getDTOShellCommand(), '_id');
	this.cmdDataViewCmds.setFilterArgs(
	{
		searchString : "",
	});
	this.cmdDataViewCmds.setFilter(placeFilter);
	this.cmdDataViewCmds.endUpdate();

	this.resize();
	this.cmdGrid.init();
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
	this.cmdGrid.resizeCanvas();
};

function onNewRow(e, args)
{
	var v = new dto_chronix_oxymores_org_DTOShellCommand();
	for ( var o in args.item)
	{
		v[o] = args.item[o];
	}
	v._id = uuid.v4();
	v.id = v._id;
	cmdDataViewCmds.addItem(v);
}

function requiredFieldValidatorCmd(value)
{
	if (value == null || value == undefined || !value.length)
	{
		var res =
		{
			valid : false,
			msg : "This is a required field"
		};
		return res;
	}
	else
	{
		var res =
		{
			valid : true,
			msg : null
		};
		return res;
	}
}

function cmdNameComparer(a, b)
{
	var x = a[sortcol], y = b[sortcol];
	return x.toLowerCase().localeCompare(y.toLowerCase());
}

function cmdFilter(item, args)
{
	if (args.searchString != "" && (item["_name"].indexOf(args.searchString) == -1 && item["_description"].indexOf(args.searchString) == -1))
	{
		return false;
	}
	return true;
}

function delCmdBtFormatter(row, cell, value, columnDef, dataContext)
{
	return "<button class='delcmd' type='button' id='del" + dataContext._id + "' >X</button>";
}
function editCmdBtFormatter(row, cell, value, columnDef, dataContext)
{
	return "<button class='editcmd' type='button' id='edit" + dataContext._id + "' >E</button>";
}