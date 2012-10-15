var dataView = null, dataView2 = null, dataView3 = null, dataView3 = null;
var gr = null;

// /////////////////////////////////////////////////////////////
// PLACES
// /////////////////////////////////////////////////////////////
function initLogicalNetworkPanel(cxfApplication)
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
		forceFitColumns : true
	};

	var columns = [
	{
		id : "name",
		name : "Place name",
		field : "_name",
		width : 200,
		cssClass : "cell-title",
		editor : Slick.Editors.Text,
		validator : requiredFieldValidator,
		sortable : true,
	},
	{
		id : "description",
		name : "Description",
		field : "_description",
		width : 250,
		selectable : false,
		resizable : false,
		editor : Slick.Editors.Text,
		validator : requiredFieldValidator,
		cannotTriggerInsert : true,
		sortable : true,
	},
	{
		id : "prop1",
		name : "Prop1",
		field : "_prop1",
		width : 150,
		cssClass : "cell-title",
		editor : Slick.Editors.Text,
		cannotTriggerInsert : true,
	},
	{
		id : "prop2",
		name : "Prop2",
		field : "_prop2",
		width : 150,
		cssClass : "cell-title",
		editor : Slick.Editors.Text,
		cannotTriggerInsert : true,
	},
	{
		id : "prop3",
		name : "Prop3",
		field : "_prop3",
		width : 150,
		cssClass : "cell-title",
		editor : Slick.Editors.Text,
		cannotTriggerInsert : true,
	},
	{
		id : "prop4",
		name : "Prop4",
		field : "_prop4",
		width : 150,
		cssClass : "cell-title",
		editor : Slick.Editors.Text,
		cannotTriggerInsert : true,
	}, ];

	dataView = new Slick.Data.DataView(
	{
		inlineFilters : true
	});
	var grid = new Slick.Grid("#gridLN", dataView, columns, options);
	grid.setSelectionModel(new Slick.RowSelectionModel());

	grid.onAddNewRow.subscribe(onNewPlaceRow);

	grid.onCellChange.subscribe(function(e, args)
	{
		dataView.updateItem(args.item.id, args.item);
	});

	grid.onSort.subscribe(function(e, args)
	{
		sortdir = args.sortAsc ? 1 : -1;
		sortcol = args.sortCol.field;

		// using native sort with comparer
		// preferred method but can be very slow in IE with huge datasets
		dataView.sort(comparer, args.sortAsc);
	});

	grid.onSelectedRowsChanged.subscribe(function()
	{
		dataView4.setFilterArgs(
		{
			searchString : this.getDataItem(this.getSelectedRows()[0])._id,
		});
		dataView4.refresh();
	});

	// wire up model events to drive the grid
	dataView.onRowCountChanged.subscribe(function(e, args)
	{
		grid.updateRowCount();
		grid.render();
	});

	dataView.onRowsChanged.subscribe(function(e, args)
	{
		grid.invalidateRows(args.rows);
		grid.render();
	});

	// wire up the search textbox to apply the filter to the model
	$("#txtSearchLN").keyup(function(e)
	{
		Slick.GlobalEditorLock.cancelCurrentEdit();

		// Clear on Esc
		if (e.which == 27)
		{
			this.value = "";
		}
		// Update filter
		dataView.setFilterArgs(
		{
			searchString : this.value,
		});
		// Refresh dataview
		dataView.refresh();
	});

	// Initialize the model after all the events have been hooked up
	dataView.beginUpdate();
	dataView.setItems(cxfApplication.getPlaces().getDTOPlace());
	dataView.setFilterArgs(
	{
		searchString : "",
	});
	dataView.setFilter(placeFilter);
	dataView.endUpdate();

	// Second panel
	initLogicalNetworkPanel2(cxfApplication);
}

function placeFilter(item, args)
{
	if (args.searchString != "" && (item["_name"].indexOf(args.searchString) == -1 && item["_description"].indexOf(args.searchString) == -1))
	{
		return false;
	}
	return true;
}
var toto = null;
function comparer(a, b)
{
	toto = a[sortcol];
	var x = a[sortcol], y = b[sortcol];
	return x.toLowerCase().localeCompare(y.toLowerCase());
}

function requiredFieldValidator(value)
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

function onNewPlaceRow(e, args)
{
	var v = new dto_chronix_oxymores_org_DTOPlace();
	for ( var o in args.item)
	{
		v[o] = args.item[o];
	}
	v.id = dataView.getItems().length;
	dataView.addItem(v);
}

// /////////////////////////////////////////////////////////////
// GROUPS
// /////////////////////////////////////////////////////////////
function initLogicalNetworkPanel2(cxfApplication)
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
		forceFitColumns : true
	};

	var columns = [
	{
		id : "name",
		name : "Place Group name",
		field : "_name",
		width : 100,
		cssClass : "cell-title",
		editor : Slick.Editors.Text,
		validator : requiredFieldValidator,
		sortable : true,
	},
	{
		id : "description",
		name : "Description",
		field : "_description",
		width : 250,
		selectable : false,
		resizable : false,
		editor : Slick.Editors.Text,
		validator : requiredFieldValidator,
		cannotTriggerInsert : true,
		sortable : true,
	}, ];

	dataView2 = new Slick.Data.DataView(
	{
		inlineFilters : true
	});
	var grid = new Slick.Grid("#gridLNGRP", dataView2, columns, options);
	gr = grid;
	grid.setSelectionModel(new Slick.RowSelectionModel());

	grid.onAddNewRow.subscribe(onNewGroupRow);

	grid.onCellChange.subscribe(function(e, args)
	{
		dataView2.updateItem(args.item.id, args.item);
	});

	grid.onSort.subscribe(function(e, args)
	{
		sortdir = args.sortAsc ? 1 : -1;
		sortcol = args.sortCol.field;
		dataView2.sort(comparer, args.sortAsc); // same comparer for all grids (lexicographic)
	});

	grid.onSelectedRowsChanged.subscribe(function()
	{
		dataView3.setFilterArgs(
		{
			searchString : this.getDataItem(this.getSelectedRows()[0])._id,
		});
		dataView3.refresh();
	});

	dataView2.onRowCountChanged.subscribe(function(e, args)
	{
		grid.updateRowCount();
		grid.render();
	});

	dataView2.onRowsChanged.subscribe(function(e, args)
	{
		grid.invalidateRows(args.rows);
		grid.render();
	});

	// wire up the search textbox to apply the filter to the model
	$("#txtSearchLNGRP").keyup(function(e)
	{
		Slick.GlobalEditorLock.cancelCurrentEdit();

		// Clear on Esc
		if (e.which == 27)
		{
			this.value = "";
		}
		// Update filter
		dataView2.setFilterArgs(
		{
			searchString : this.value,
		});
		// Refresh dataview
		dataView2.refresh();
	});

	// Initialize the model after all the events have been hooked up
	dataView2.beginUpdate();
	dataView2.setItems(cxfApplication.getGroups().getDTOPlaceGroup());
	dataView2.setFilterArgs(
	{
		searchString : "",
	});
	dataView2.setFilter(placeFilter); // same field names in both groups and places
	dataView2.endUpdate();

	initLogicalNetworkPanel3(cxfApplication);
}

function onNewGroupRow(e, args)
{
	var v = new dto_chronix_oxymores_org_DTOPlaceGroup();
	for ( var o in args.item)
	{
		v[o] = args.item[o];
	}
	v._id = uuid.v4();
	v.id = v._id;
	dataView2.addItem(v);
}

// /////////////////////////////////////////////////////////////
// GROUP CONTENT
// /////////////////////////////////////////////////////////////
function initLogicalNetworkPanel3(cxfApplication)
{
	var options =
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
		forceFitColumns : true
	};

	var columns = [
	{
		id : "name",
		name : "Group content",
		field : "_name",
		width : 200,
		cssClass : "cell-title",
		sortable : true,
	}, ];

	dataView3 = new Slick.Data.DataView(
	{
		inlineFilters : true
	});
	var grid = new Slick.Grid("#gridLNGRPCNT", dataView3, columns, options);
	grid.setSelectionModel(new Slick.RowSelectionModel());

	grid.onSort.subscribe(function(e, args)
	{
		sortdir = args.sortAsc ? 1 : -1;
		sortcol = args.sortCol.field;
		dataView3.sort(comparer, args.sortAsc);
	});

	dataView3.onRowCountChanged.subscribe(function(e, args)
	{
		grid.updateRowCount();
		grid.render();
	});

	dataView3.onRowsChanged.subscribe(function(e, args)
	{
		grid.invalidateRows(args.rows);
		grid.render();
	});

	// Initialize the model after all the events have been hooked up
	dataView3.beginUpdate();
	dataView3.setItems(cxfApplication.getPlaces().getDTOPlace());
	dataView3.setFilterArgs(
	{
		searchString : "",
	});
	dataView3.setFilter(placeGroupContentFilter);
	dataView3.endUpdate();

	// Final panel
	initLogicalNetworkPanel4(cxfApplication);
}

function placeGroupContentFilter(item, args)
{
	if (args.searchString != null)
	{
		var mm = item.getMemberOf().getString();
		for ( var i = 0; i < mm.length; i++)
		{
			if (mm[i] === args.searchString)
				return true;
		}
	}
	return false;
}

// /////////////////////////////////////////////////////////////
// PLACE GROUP MEMBERSHIP
// /////////////////////////////////////////////////////////////
function initLogicalNetworkPanel4(cxfApplication)
{
	var options =
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
		forceFitColumns : true
	};

	var columns = [
	{
		id : "name",
		name : "Place is member of",
		field : "_name",
		width : 200,
		cssClass : "cell-title",
		sortable : true,
	}, ];

	dataView4 = new Slick.Data.DataView(
	{
		inlineFilters : true
	});
	var grid = new Slick.Grid("#gridLNPLCGRP", dataView4, columns, options);
	grid.setSelectionModel(new Slick.RowSelectionModel());

	grid.onSort.subscribe(function(e, args)
	{
		sortdir = args.sortAsc ? 1 : -1;
		sortcol = args.sortCol.field;
		dataView4.sort(comparer, args.sortAsc);
	});

	dataView4.onRowCountChanged.subscribe(function(e, args)
	{
		grid.updateRowCount();
		grid.render();
	});

	dataView4.onRowsChanged.subscribe(function(e, args)
	{
		grid.invalidateRows(args.rows);
		grid.render();
	});

	// Initialize the model after all the events have been hooked up
	dataView4.beginUpdate();
	dataView4.setItems(cxfApplication.getGroups().getDTOPlaceGroup());
	dataView4.setFilterArgs(
	{
		searchString : "",
	});
	dataView4.setFilter(placeGroupMembershipContentFilter);
	dataView4.endUpdate();
}

function placeGroupMembershipContentFilter(item, args)
{
	if (args.searchString != null)
	{
		var mm = item.getPlaces().getString();
		for ( var i = 0; i < mm.length; i++)
		{
			if (mm[i] === args.searchString)
				return true;
		}
	}
	return false;
}