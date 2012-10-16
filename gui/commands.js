var cmdGrid = null;
var cmdDataViewCmds = null;

function initCommandPanel(cxfApplication)
{
	var options =
	{
		editable : true,
		enableAddRow : true,
		enableCellNavigation : true,
		enableColumnReorder : false,
		enableRowReordering : false,
		asyncEditorLoading : false,
		showHeaderRow : false,
		multiSelect : false,
		enableTextSelectionOnCells : false, // ???
		rowHeight : 30,
		autoHeight : true,
		autoEdit : true,
		forceFitColumns : true,
	};

	var columns = [
	{
		id : "name",
		name : "Command short name",
		field : "_name",
		width : 200,
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
		width : 250,
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
		width : 600,
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
		width : 30,
		formatter : delCmdBtFormatter,
		cannotTriggerInsert : true,
	}, ];

	cmdDataViewCmds = new Slick.Data.DataView(
	{
		inlineFilters : true
	});
	cmdGrid = new Slick.Grid("#gridCommands", cmdDataViewCmds, columns, options);

	cmdGrid.setSelectionModel(new Slick.RowSelectionModel());
	cmdGrid.onAddNewRow.subscribe(onNewRow);
	cmdGrid.onCellChange.subscribe(function(e, args)
	{
		cmdDataViewCmds.updateItem(args.item.id, args.item);
	});
	cmdGrid.onSort.subscribe(function(e, args)
	{
		sortdir = args.sortAsc ? 1 : -1;
		sortcol = args.sortCol.field;
		cmdDataViewCmds.sort(cmdNameComparer, args.sortAsc);
	});
	cmdDataViewCmds.onRowCountChanged.subscribe(function(e, args)
	{
		cmdGrid.updateRowCount();
		cmdGrid.render();
	});

	cmdDataViewCmds.onRowsChanged.subscribe(function(e, args)
	{
		cmdGrid.invalidateRows(args.rows);
		cmdGrid.render();
	});
	$("#txtSearchCmd").keyup(function(e)
	{
		Slick.GlobalEditorLock.cancelCurrentEdit();

		// Clear on Esc
		if (e.which == 27)
		{
			this.value = "";
		}
		// Update filter
		cmdDataViewCmds.setFilterArgs(
		{
			searchString : this.value,
		});
		// Refresh dataview
		cmdDataViewCmds.refresh();
	});
	$('.delcmd').live('click', function()
	{
		var me = $(this), id = me.attr('id');

		// Clean states of this Place????
		// TODO

		// Destroy the cmd through the dataview (will in turn update cxfApplication)
		cmdDataViewCmds.deleteItem(id);
	});

	// Populate & go
	cmdDataViewCmds.beginUpdate();
	cmdDataViewCmds.setItems(cxfApplication.getShells().getDTOShellCommand());
	cmdDataViewCmds.setFilterArgs(
	{
		searchString : "",
	});
	cmdDataViewCmds.setFilter(placeFilter);
	cmdDataViewCmds.endUpdate();
}

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
	return "<button class='delcmd' type='button' id='" + dataContext.id + "' >X</button>";
}