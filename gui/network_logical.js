// /////////////////////////////////////////////////////////////
// Variables
// /////////////////////////////////////////////////////////////
var nlDataViewPlaces = null, nlDataViewGroups = null, nlDataViewGroupContent = null, nlDataViewPlaceMembership = null;
var nlGridPlaces = null, nlGridGroups = null, nlGridGroupContent = null, nlGridPlaceMembership = null;

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

	nlDataViewPlaces = new Slick.Data.DataView(
	{
		inlineFilters : true
	});
	nlGridPlaces = new Slick.Grid("#gridLN", nlDataViewPlaces, columns, options);
	nlGridPlaces.setSelectionModel(new Slick.RowSelectionModel());

	nlGridPlaces.onAddNewRow.subscribe(onNewPlaceRow);

	nlGridPlaces.onCellChange.subscribe(function(e, args)
	{
		nlDataViewPlaces.updateItem(args.item.id, args.item);
	});

	nlGridPlaces.onSort.subscribe(function(e, args)
	{
		sortdir = args.sortAsc ? 1 : -1;
		sortcol = args.sortCol.field;

		// using native sort with comparer
		// preferred method but can be very slow in IE with huge datasets
		nlDataViewPlaces.sort(comparer, args.sortAsc);
	});

	nlGridPlaces.onSelectedRowsChanged.subscribe(function()
	{
		nlDataViewPlaceMembership.setFilterArgs(
		{
			searchString : this.getDataItem(this.getSelectedRows()[0])._id,
		});
		nlDataViewPlaceMembership.refresh();
	});

	// wire up model events to drive the grid
	nlDataViewPlaces.onRowCountChanged.subscribe(function(e, args)
	{
		nlGridPlaces.updateRowCount();
		nlGridPlaces.render();
	});

	nlDataViewPlaces.onRowsChanged.subscribe(function(e, args)
	{
		nlGridPlaces.invalidateRows(args.rows);
		nlGridPlaces.render();
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
		nlDataViewPlaces.setFilterArgs(
		{
			searchString : this.value,
		});
		// Refresh dataview
		nlDataViewPlaces.refresh();
	});

	// Initialize the model after all the events have been hooked up
	nlDataViewPlaces.beginUpdate();
	nlDataViewPlaces.setItems(cxfApplication.getPlaces().getDTOPlace());
	nlDataViewPlaces.setFilterArgs(
	{
		searchString : "",
	});
	nlDataViewPlaces.setFilter(placeFilter);
	nlDataViewPlaces.endUpdate();

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

function comparer(a, b)
{
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
	nlDataViewPlaces.addItem(v);
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

	nlDataViewGroups = new Slick.Data.DataView(
	{
		inlineFilters : true
	});
	nlGridGroups = new Slick.Grid("#gridLNGRP", nlDataViewGroups, columns, options);
	nlGridGroups.setSelectionModel(new Slick.RowSelectionModel());

	nlGridGroups.onAddNewRow.subscribe(onNewGroupRow);

	nlGridGroups.onCellChange.subscribe(function(e, args)
	{
		nlDataViewGroups.updateItem(args.item.id, args.item);
	});

	nlGridGroups.onSort.subscribe(function(e, args)
	{
		sortdir = args.sortAsc ? 1 : -1;
		sortcol = args.sortCol.field;
		nlDataViewGroups.sort(comparer, args.sortAsc); // same comparer for all grids (lexicographic)
	});

	nlGridGroups.onSelectedRowsChanged.subscribe(function()
	{
		nlDataViewGroupContent.setFilterArgs(
		{
			searchString : this.getDataItem(this.getSelectedRows()[0])._id,
		});
		nlDataViewGroupContent.refresh();
	});

	nlDataViewGroups.onRowCountChanged.subscribe(function(e, args)
	{
		nlGridGroups.updateRowCount();
		nlGridGroups.render();
	});

	nlDataViewGroups.onRowsChanged.subscribe(function(e, args)
	{
		nlGridGroups.invalidateRows(args.rows);
		nlGridGroups.render();
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
		nlDataViewGroups.refresh();
	});

	// Initialize the model after all the events have been hooked up
	nlDataViewGroups.beginUpdate();
	nlDataViewGroups.setItems(cxfApplication.getGroups().getDTOPlaceGroup());
	nlDataViewGroups.setFilterArgs(
	{
		searchString : "",
	});
	nlDataViewGroups.setFilter(placeFilter); // same field names in both groups and places
	nlDataViewGroups.endUpdate();

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
	nlDataViewGroups.addItem(v);
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

	nlDataViewGroupContent = new Slick.Data.DataView(
	{
		inlineFilters : true
	});
	nlGridGroupContent = new Slick.Grid("#gridLNGRPCNT", nlDataViewGroupContent, columns, options);
	nlGridGroupContent.setSelectionModel(new Slick.RowSelectionModel());

	nlGridGroupContent.onSort.subscribe(function(e, args)
	{
		sortdir = args.sortAsc ? 1 : -1;
		sortcol = args.sortCol.field;
		nlDataViewGroupContent.sort(comparer, args.sortAsc);
	});

	nlDataViewGroupContent.onRowCountChanged.subscribe(function(e, args)
	{
		nlGridGroupContent.updateRowCount();
		nlGridGroupContent.render();
	});

	nlDataViewGroupContent.onRowsChanged.subscribe(function(e, args)
	{
		nlGridGroupContent.invalidateRows(args.rows);
		nlGridGroupContent.render();
	});

	// Initialize the model after all the events have been hooked up
	nlDataViewGroupContent.beginUpdate();
	nlDataViewGroupContent.setItems(cxfApplication.getPlaces().getDTOPlace());
	nlDataViewGroupContent.setFilterArgs(
	{
		searchString : "",
	});
	nlDataViewGroupContent.setFilter(placeGroupContentFilter);
	nlDataViewGroupContent.endUpdate();

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

	nlDataViewPlaceMembership = new Slick.Data.DataView(
	{
		inlineFilters : true
	});
	nlGridPlaceMembership = new Slick.Grid("#gridLNPLCGRP", nlDataViewPlaceMembership, columns, options);
	nlGridPlaceMembership.setSelectionModel(new Slick.RowSelectionModel());

	nlGridPlaceMembership.onSort.subscribe(function(e, args)
	{
		sortdir = args.sortAsc ? 1 : -1;
		sortcol = args.sortCol.field;
		nlDataViewPlaceMembership.sort(comparer, args.sortAsc);
	});

	nlDataViewPlaceMembership.onRowCountChanged.subscribe(function(e, args)
	{
		nlGridPlaceMembership.updateRowCount();
		nlGridPlaceMembership.render();
	});

	nlDataViewPlaceMembership.onRowsChanged.subscribe(function(e, args)
	{
		nlGridPlaceMembership.invalidateRows(args.rows);
		nlGridPlaceMembership.render();
	});

	// Initialize the model after all the events have been hooked up
	nlDataViewPlaceMembership.beginUpdate();
	nlDataViewPlaceMembership.setItems(cxfApplication.getGroups().getDTOPlaceGroup());
	nlDataViewPlaceMembership.setFilterArgs(
	{
		searchString : "",
	});
	nlDataViewPlaceMembership.setFilter(placeGroupMembershipContentFilter);
	nlDataViewPlaceMembership.endUpdate();
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