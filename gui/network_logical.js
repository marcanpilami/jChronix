// /////////////////////////////////////////////////////////////
// Variables
// /////////////////////////////////////////////////////////////
var nlDataViewPlaces = null, nlDataViewGroups = null, nlDataViewGroupContent = null, nlDataViewPlaceMembership = null;
var nlGridPlaces = null, nlGridGroups = null, nlGridGroupContent = null, nlGridPlaceMembership = null;
var nlSelectedGroup = null, nlSelectedPlace = null;

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
		enableTextSelectionOnCells : false,
		autoHeight : false,
		autoEdit : true,
		forceFitColumns : true,
		fullWidthRows : true,
	};

	var columns = [
	{
		id : "name",
		name : "Place name",
		field : "_name",
		minWidth : 50,
		maxWidth : 300,
		//cssClass : "cell-title",
		editor : Slick.Editors.Text,
		validator : requiredFieldValidator,
		sortable : true,
		resizable : true,
	},
	{
		id : "description",
		name : "Description",
		field : "_description",
		minWidth : 200,
		selectable : false,
		resizable : true,
		editor : Slick.Editors.Text,
		validator : requiredFieldValidator,
		cannotTriggerInsert : true,
		sortable : true,
	},
	{
		id : "prop1",
		name : "Prop1",
		field : "_prop1",
		maxWidth : 80,
		cssClass : "cell-title",
		editor : Slick.Editors.Text,
		cannotTriggerInsert : true,
	},
	{
		id : "prop2",
		name : "Prop2",
		field : "_prop2",
		maxWidth : 80,
		cssClass : "cell-title",
		editor : Slick.Editors.Text,
		cannotTriggerInsert : true,
	},
	{
		id : "prop3",
		name : "Prop3",
		field : "_prop3",
		maxWidth : 80,
		cssClass : "cell-title",
		editor : Slick.Editors.Text,
		cannotTriggerInsert : true,
	},
	{
		id : "prop4",
		name : "Prop4",
		field : "_prop4",
		maxWidth : 80,
		cssClass : "cell-title",
		editor : Slick.Editors.Text,
		cannotTriggerInsert : true,
	},
	{
		id : "del",
		name : "Delete",
		field : "del",
		maxWidth : 70,
		formatter : delBtFormatter,
		cannotTriggerInsert : true,
	}, ];

	nlDataViewPlaces = new Slick.Data.DataView(
	{
		inlineFilters : true
	});
	//$("#gridLN").width($("div.rightpanel").width());
	//alert($("div.rightpanel").width());
	nlGridPlaces = new Slick.Grid("#gridLN", nlDataViewPlaces, columns, options);
	nlGridPlaces.setSelectionModel(new Slick.RowSelectionModel());
	
	$("#gridLN").width($("div.rightpanel").width());
	//alert($("div.rightpanel").width());
	nlGridPlaces.resizeCanvas();

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
		if (this.getDataItem(this.getSelectedRows()[0]) != undefined)
		{
			nlSelectedPlace = this.getDataItem(this.getSelectedRows()[0]);
			nlDataViewPlaceMembership.setFilterArgs(
			{
				searchString : nlSelectedPlace._id,
			});
		}
		else
		{
			nlSelectedPlace = null;
			nlDataViewPlaceMembership.setFilterArgs(
			{
				searchString : "X",
			});
		}
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

	// Delete event
	$('.delplace').live('click', function()
	{
		var me = $(this), id = me.attr('id');

		// Void detail view
		nlDataViewPlaceMembership.setFilterArgs(
		{
			searchString : "X",
		});

		// Clean groups of this Place
		var gg = nlDataViewGroups.getItems();
		for ( var i = 0; i < gg.length; i++)
		{
			var idx = jQuery.inArray(id, gg[i]._places.getString());
			if (-1 !== idx)
				gg[i]._places.getString().splice(idx, 1);
		}

		// Destroy Place through the dataview (will in turn update cxfApplication)
		nlDataViewPlaces.deleteItem(id);
		nlDataViewGroupContent.refresh();
		nlDataViewPlaceMembership.refresh();
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
	v._id = uuid.v4();
	v.id = v._id;
	nlDataViewPlaces.addItem(v);
}

function delBtFormatter(row, cell, value, columnDef, dataContext)
{
	return "<button class='delplace' type='button' id='" + dataContext.id + "' >DELETE</button>";
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
		forceFitColumns : true,
		resizable : true,
	};

	var columns = [
	{
		id : "name",
		name : "Place Group name",
		field : "_name",
		maxWidth : 150,
		// cssClass : "cell-title",
		editor : Slick.Editors.Text,
		validator : requiredFieldValidator,
		sortable : true,
	},
	{
		id : "description",
		name : "Description",
		field : "_description",
		minWidth : 250,
		selectable : false,
		resizable : false,
		editor : Slick.Editors.Text,
		validator : requiredFieldValidator,
		cannotTriggerInsert : true,
		sortable : true,
		resizable : true,
	},
	{
		id : "del",
		name : "Delete",
		field : "del",
		width : 70,
		formatter : delGrpBtFormatter,
		cannotTriggerInsert : true,
		resizable : true,
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
		if (this.getDataItem(this.getSelectedRows()[0]) != undefined)
		{
			nlSelectedGroup = this.getDataItem(this.getSelectedRows()[0]);
			nlDataViewGroupContent.setFilterArgs(
			{
				searchString : nlSelectedGroup._id,
			});
		}
		else
		{
			nlSelectedGroup = null;
			nlDataViewGroupContent.setFilterArgs(
			{
				searchString : "X",
			});
		}
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
		nlDataViewGroups.setFilterArgs(
		{
			searchString : this.value,
		});
		// Refresh dataview
		nlDataViewGroups.refresh();
	});

	// Delete event
	$('.delgroup').live('click', function()
	{
		var me = $(this);
		var id = me.attr('id');

		// Unselect Group (hide its content from the detail view)
		nlDataViewGroupContent.setFilterArgs(
		{
			searchString : "X",
		});

		// Clean Places of this Group
		var gg = nlDataViewPlaces.getItems();
		for ( var i = 0; i < gg.length; i++)
		{
			var idx = jQuery.inArray(id, gg[i]._memberOf.getString());
			if (-1 !== idx)
				gg[i]._memberOf.getString().splice(idx, 1);
		}

		// Destroy Group through the dataview (will in turn update cxfApplication)
		nlDataViewGroups.deleteItem(id);
		nlDataViewGroupContent.refresh();
		nlDataViewPlaceMembership.refresh();
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

function delGrpBtFormatter(row, cell, value, columnDef, dataContext)
{
	return "<button class='delgroup' type='button' id='" + dataContext._id + "' >DELETE</button>";
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
	},
	{
		id : "del",
		name : "",
		field : "del",
		width : 20,
		formatter : delPlaceGrpCntBtFormatter,
		cannotTriggerInsert : true,
		resizable : false,
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

	// Delete event
	$('.removefromgroup').live('click', function()
	{
		// The given id is from the Place to remove from the currenty selected Group in grid 2
		var me = $(this);
		var id = me.attr('id');
		var placeToRemove = nlDataViewPlaces.getItemById(id);

		nlSelectedGroup._places.getString().splice(jQuery.inArray(id, nlSelectedGroup._places.getString()), 1);
		placeToRemove._memberOf.getString().splice(jQuery.inArray(nlSelectedGroup._id, placeToRemove._memberOf.getString()), 1);

		nlDataViewGroupContent.refresh();
		nlDataViewPlaceMembership.refresh();
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
		debug2 = mm;
		return (-1 !== jQuery.inArray(args.searchString, mm));
	}
	return false;
}

function delPlaceGrpCntBtFormatter(row, cell, value, columnDef, dataContext)
{
	return "<button class='removefromgroup' type='button' id='" + dataContext._id + "' >X</button>";
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
	},
	{
		id : "del",
		name : "",
		field : "del",
		width : 20,
		formatter : delRemPlaceFromGroupBtFormatter,
		cannotTriggerInsert : true,
		resizable : false,
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

	// Delete event
	$('.retiremembership').live('click', function()
	{
		// The given id is from the Group to get out from the currenty selected Place
		var me = $(this);
		var id = me.attr('id');
		var groupToRemove = nlDataViewGroups.getItemById(id);

		nlSelectedPlace._memberOf.getString().splice(jQuery.inArray(id, nlSelectedPlace._memberOf.getString()), 1);
		groupToRemove._places.getString().splice(jQuery.inArray(nlSelectedPlace._id, groupToRemove._places.getString()), 1);

		nlDataViewGroupContent.refresh();
		nlDataViewPlaceMembership.refresh();
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

function delRemPlaceFromGroupBtFormatter(row, cell, value, columnDef, dataContext)
{
	return "<button class='retiremembership' type='button' id='" + dataContext._id + "' >X</button>";
}

// /////////////////////////////////////////////////////////////
// ADD MEMBERSHIP
// /////////////////////////////////////////////////////////////

function nlAddPlaceToGroupClick()
{
	if (nlSelectedGroup == null || nlSelectedPlace == null)
		return;

	if (-1 !== jQuery.inArray(nlSelectedPlace._id, nlSelectedGroup._places.getString()))
		return; // Don't add a place twice in the same group

	nlSelectedGroup._places.getString().push(nlSelectedPlace._id);
	nlSelectedPlace._memberOf.getString().push(nlSelectedGroup._id);

	nlDataViewGroupContent.refresh();
	nlDataViewPlaceMembership.refresh();
}