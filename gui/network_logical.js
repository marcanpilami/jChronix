// /////////////////////////////////////////////////////////////
// Variables
// /////////////////////////////////////////////////////////////
var nlDataViewPlaces = null, nlDataViewGroups = null, nlDataViewGroupContent = null, nlDataViewPlaceMembership = null;
var nlGridPlaces = null, nlGridGroups = null, nlGridGroupContent = null, nlGridPlaceMembership = null;
var nlSelectedGroup = null, nlSelectedPlace = null;

// /////////////////////////////////////////////////////////////
// Global panel
// /////////////////////////////////////////////////////////////

function LogicalNetworkPanel(divId, dtoApplication)
{
	this.mainDiv = $("#" + divId);
	this.dtoApplication = dtoApplication;

	// Left bar (Place Group content & Membership for places)
	this.leftBarDiv = $("<div  class='leftbar'></div>");
	this.mainDiv.append(this.leftBarDiv);
	this.groupContentDiv = $("<div style='height:50%;'></div>");
	this.leftBarDiv.append(this.groupContentDiv);
	this.placeMemberOfDiv = $("<div style='height:50%;'></div>");
	this.leftBarDiv.append(this.placeMemberOfDiv);

	this.groupContent = new GroupContentPanel(this.groupContentDiv, this.dtoApplication, this);
	this.placeMemberOf = new PlaceMemberOfPanel(this.placeMemberOfDiv, this.dtoApplication, this);

	// Main panel (places & place groups)
	this.mainPanelDiv = $("<div class='rightpanel'></div>");
	this.mainDiv.append(this.mainPanelDiv);
	this.groupPanelDiv = $("<div style='height:50%;'></div>");
	this.mainPanelDiv.append(this.groupPanelDiv);
	this.placePanelDiv = $("<div style='height:50%;'></div>");
	this.mainPanelDiv.append(this.placePanelDiv);

	this.placePanel = new PlacePanel(this.placePanelDiv, dtoApplication, this);
	this.placeGroupPanel = new PlaceGroupPanel(this.groupPanelDiv, this.dtoApplication, this);

	// Selected items
	this.selectedGroup = null;
	this.selectedPlace = null;

	// Add
	this.placePanel.btAddToGroup.click(this.addPlaceToGroupClick.bind(this));

	// End
	$(window).resize(this.redisplay.bind(this));
}

LogicalNetworkPanel.prototype.addPlaceToGroupClick = function()
{
	if (this.selectedGroup == null || this.selectedPlace == null)
		return;

	if (-1 !== jQuery.inArray(this.selectedPlace._id, this.selectedGroup._places.getString()))
		return; // Don't add a place twice in the same group

	this.selectedGroup._places.getString().push(this.selectedPlace._id);
	this.selectedPlace._memberOf.getString().push(this.selectedGroup._id);

	this.groupContent.dataview.refresh();
	this.placeMemberOf.dataview.refresh();
};

LogicalNetworkPanel.prototype.redisplay = function()
{
	this.placePanel.resize();
	this.placeGroupPanel.resize();
	this.groupContent.resize();
	this.placeMemberOf.resize();
	
	this.placePanel.dataview.refresh();
};

// /////////////////////////////////////////////////////////////
// PLACES
// /////////////////////////////////////////////////////////////
function PlacePanel(mainDiv, dtoApplication, lnPanel)
{
	this.dtoApplication = dtoApplication;
	this.mainDiv = mainDiv;
	this.parent = lnPanel;

	this.mainDiv.append($("<label>Only show <strong>places</strong> which name or description contains:</label>"));
	this.searchBox = $("<input type='text' style='width: 100px;'>");
	this.mainDiv.append(this.searchBox);
	this.btAddToGroup = $("<input type='button' value='Add to selected group'>");
	this.mainDiv.append(this.btAddToGroup);

	this.containerContainer = $("<div class='containerContainer'></div>");
	this.mainDiv.append(this.containerContainer);
	this.slickContainer = $("<div></div>");
	this.containerContainer.append(this.slickContainer);

	// Slickgrid options
	var options = getSlickGridOptionsEditable();

	var columns = [
	{
		id : "name",
		name : "Place name",
		field : "_name",
		minWidth : 50,
		maxWidth : 300,
		// cssClass : "cell-title",
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
		id : "phynode",
		name : "Physical node",
		field : "_nodeid",
		minWidth : 100,
		selectable : false,
		resizable : true,
		editor : SelectCellEditor,
		options: this.dtoApplication.getNodes().getDTOExecutionNode(),
		formatter: dnsFormatter,
		cannotTriggerInsert : true,
		sortable : false,
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

	// Slickgrid dataview (we need sort & co)
	this.dataview = new Slick.Data.DataView(
	{
		inlineFilters : true
	});

	// Create SlickGrid
	this.mainGrid = new Slick.Grid(this.slickContainer, this.dataview, columns, options);
	this.mainGrid.setSelectionModel(new Slick.RowSelectionModel());

	// Generic events
	this.mainGrid.onCellChange.subscribe(onCellChange.bind(this)); // Update dataview on change
	this.mainGrid.onSort.subscribe(onSort.bind(this)); // A-Z sorting
	this.dataview.onRowCountChanged.subscribe(onRowCountChanged.bind(this)); // Rerender the grid
	this.dataview.onRowsChanged.subscribe(onRowsChanged.bind(this)); // Invalidate the updated rows & rerender grid
	this.searchBox.keyup(searchBoxKeyup.bind(this)); // wire up the search textbox to apply the filter

	// Specific events
	this.mainGrid.onAddNewRow.subscribe((function(e, args)
	{
		var v = new dto_chronix_oxymores_org_DTOPlace();
		for ( var o in args.item)
		{
			v[o] = args.item[o];
		}
		v._id = uuid.v4();
		v._memberOf = new internalapi_chronix_oxymores_org__ArrayOfString();
		this.dataview.addItem(v);
	}).bind(this));

	this.mainGrid.onSelectedRowsChanged.subscribe((function()
	{
		this.parent.selectedPlace = this.mainGrid.getDataItem(this.mainGrid.getSelectedRows()[0]);

		if (this.parent.selectedPlace != undefined)
		{
			this.parent.placeMemberOf.dataview.setFilterArgs(
			{
				searchString : this.parent.selectedPlace._id,
			});
		}
		else
		{
			this.parent.placeMemberOf.dataview.setFilterArgs(
			{
				searchString : null,
			});
		}
		this.parent.placeMemberOf.dataview.refresh();
	}).bind(this));

	this.slickContainer.on('click', '.deldto_chronix_oxymores_org_DTOPlace', (function()
	{
		// Void detail view
		this.parent.placeMemberOf.dataview.setFilterArgs(
		{
			searchString : null,
		});

		// Clean groups of this Place
		var gg = this.dtoApplication.getGroups().getDTOPlaceGroup();
		for ( var i = 0; i < gg.length; i++)
		{
			var idx = jQuery.inArray(this.parent.selectedPlace._id, gg[i]._places.getString());
			if (-1 !== idx)
				gg[i]._places.getString().splice(idx, 1);
		}

		// Destroy Place through the dataview (will in turn update dtoApplication & propagate)
		this.dataview.deleteItem(this.parent.selectedPlace._id);
		this.parent.placeMemberOf.dataview.refresh();
		this.parent.groupContent.dataview.refresh();
	}).bind(this));

	// Initialize the model after all the events have been hooked up
	this.dataview.beginUpdate();
	this.dataview.setItems(this.dtoApplication.getPlaces().getDTOPlace(), '_id');
	this.dataview.setFilterArgs(
	{
		searchString : "",
	});
	this.dataview.setFilter(nameDescriptionFilter);
	this.dataview.endUpdate();

	this.resize();
	this.mainGrid.init();
}

PlacePanel.prototype.resize = slResize;

// /////////////////////////////////////////////////////////////
// GROUPS
// /////////////////////////////////////////////////////////////
function PlaceGroupPanel(mainDiv, dtoApplication, lnPanel)
{
	this.dtoApplication = dtoApplication;
	this.mainDiv = mainDiv;
	this.parent = lnPanel;

	this.mainDiv.append($("<label>Only show <strong>place groups</strong> which name or description contains: </label>"));
	this.searchBox = $("<input type='text' style='width: 100px;'>");
	this.mainDiv.append(this.searchBox);
	this.containerContainer = $("<div class='containerContainer'></div>");
	this.mainDiv.append(this.containerContainer);
	this.slickContainer = $("<div></div>");
	this.containerContainer.append(this.slickContainer);

	// Slickgrid options
	var options = getSlickGridOptionsEditable();

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
		formatter : delBtFormatter,
		cannotTriggerInsert : true,
		resizable : true,
	}, ];

	// Slickgrid dataview (we need sort & co)
	this.dataview = new Slick.Data.DataView(
	{
		inlineFilters : true
	});

	// Create SlickGrid
	this.mainGrid = new Slick.Grid(this.slickContainer, this.dataview, columns, options);
	this.mainGrid.setSelectionModel(new Slick.RowSelectionModel());

	// Generic events
	this.mainGrid.onCellChange.subscribe(onCellChange.bind(this)); // Update dataview on change
	this.mainGrid.onSort.subscribe(onSort.bind(this)); // A-Z sorting
	this.dataview.onRowCountChanged.subscribe(onRowCountChanged.bind(this)); // Rerender the grid
	this.dataview.onRowsChanged.subscribe(onRowsChanged.bind(this)); // Invalidate the updated rows & rerender grid
	this.searchBox.keyup(searchBoxKeyup.bind(this)); // wire up the search textbox to apply the filter

	// Specific events
	this.mainGrid.onAddNewRow.subscribe((function(e, args)
	{
		var v = new dto_chronix_oxymores_org_DTOPlaceGroup();
		for ( var o in args.item)
		{
			v[o] = args.item[o];
		}
		v._id = uuid.v4();
		v._places = new internalapi_chronix_oxymores_org__ArrayOfString();
		this.dataview.addItem(v);
	}).bind(this));

	this.slickContainer.on('click', '.deldto_chronix_oxymores_org_DTOPlaceGroup', (function()
	{
		// Unselect Group (hide its content from the detail view)
		this.parent.groupContent.dataview.setFilterArgs(
		{
			searchString : null,
		});

		// Clean Places of this Group
		var gg = this.dtoApplication.getPlaces().getDTOPlace();
		for ( var i = 0; i < gg.length; i++)
		{
			var idx = jQuery.inArray(this.parent.selectedGroup._id, gg[i]._memberOf.getString());
			if (-1 !== idx)
				gg[i]._memberOf.getString().splice(idx, 1);
		}

		// Destroy Group through the dataview (will in turn update dtoApplication & propagate)
		this.dataview.deleteItem(this.parent.selectedGroup._id);
		this.parent.placeMemberOf.dataview.refresh();
		this.parent.groupContent.dataview.refresh();
	}).bind(this));

	this.mainGrid.onSelectedRowsChanged.subscribe((function()
	{
		this.parent.selectedGroup = this.mainGrid.getDataItem(this.mainGrid.getSelectedRows()[0]);

		if (this.parent.selectedGroup != undefined)
		{
			this.parent.groupContent.dataview.setFilterArgs(
			{
				searchString : this.parent.selectedGroup._id,
			});
		}
		else
		{
			this.parent.groupContent.dataview.setFilterArgs(
			{
				searchString : null,
			});
		}
		this.parent.groupContent.dataview.refresh();
	}).bind(this));

	// Initialize the model after all the events have been hooked up
	this.dataview.beginUpdate();
	this.dataview.setItems(this.dtoApplication.getGroups().getDTOPlaceGroup(), '_id');
	this.dataview.setFilterArgs(
	{
		searchString : "",
	});
	this.dataview.setFilter(nameDescriptionFilter);
	this.dataview.endUpdate();

	this.resize();
	this.mainGrid.init();
}

PlaceGroupPanel.prototype.resize = slResize;

// /////////////////////////////////////////////////////////////
// GROUP CONTENT
// /////////////////////////////////////////////////////////////
function GroupContentPanel(mainDiv, dtoApplication, lnPanel)
{
	this.dtoApplication = dtoApplication;
	this.mainDiv = mainDiv;
	this.parent = lnPanel;

	this.containerContainer = $("<div class='containerContainer'></div>");
	this.mainDiv.append(this.containerContainer);
	this.slickContainer = $("<div></div>");
	this.containerContainer.append(this.slickContainer);

	// Slickgrid options
	var options = getSlickGridOptionsReadOnly();

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
		formatter : delBtFormatter,
		cannotTriggerInsert : true,
		resizable : false,
	}, ];

	// Slickgrid dataview (we need sort & co)
	this.dataview = new Slick.Data.DataView(
	{
		inlineFilters : true
	});

	// Create SlickGrid
	this.mainGrid = new Slick.Grid(this.slickContainer, this.dataview, columns, options);
	this.mainGrid.setSelectionModel(new Slick.RowSelectionModel());

	// Generic events
	this.mainGrid.onSort.subscribe(onSort.bind(this)); // A-Z sorting
	this.dataview.onRowCountChanged.subscribe(onRowCountChanged.bind(this)); // Rerender the grid
	this.dataview.onRowsChanged.subscribe(onRowsChanged.bind(this)); // Invalidate the updated rows & rerender grid

	// Specific events
	this.slickContainer.on('click', '.deldto_chronix_oxymores_org_DTOPlace', (function()
	{
		var placeToRemove = this.mainGrid.getDataItem(this.mainGrid.getSelectedRows()[0]);

		this.parent.selectedGroup._places.getString().splice(jQuery.inArray(placeToRemove._id, this.parent.selectedGroup._places.getString()), 1);
		placeToRemove._memberOf.getString().splice(jQuery.inArray(this.parent.selectedGroup._id, placeToRemove._memberOf.getString()), 1);

		this.parent.placeMemberOf.dataview.refresh();
		this.parent.groupContent.dataview.refresh();
	}).bind(this));

	// Initialize the grid after all the events have been hooked up
	this.dataview.beginUpdate();
	this.dataview.setItems(this.dtoApplication.getPlaces().getDTOPlace(), '_id');
	this.dataview.setFilterArgs(
	{
		searchString : "",
	});
	this.dataview.setFilter(this.placeGroupContentFilter);
	this.dataview.endUpdate();

	this.resize();
	this.mainGrid.init();
}

GroupContentPanel.prototype.placeGroupContentFilter = function(item, args)
{
	if (args.searchString != null)
	{
		var mm = item.getMemberOf().getString();
		return (-1 !== jQuery.inArray(args.searchString, mm));
	}
	return false;
};

GroupContentPanel.prototype.resize = slResize;

// /////////////////////////////////////////////////////////////
// PLACE GROUP MEMBERSHIP
// /////////////////////////////////////////////////////////////
function PlaceMemberOfPanel(mainDiv, dtoApplication, lnPanel)
{
	this.dtoApplication = dtoApplication;
	this.mainDiv = mainDiv;
	this.parent = lnPanel;

	this.containerContainer = $("<div class='containerContainer'></div>");
	this.mainDiv.append(this.containerContainer);
	this.slickContainer = $("<div></div>");
	this.containerContainer.append(this.slickContainer);

	// Slickgrid options
	var options = getSlickGridOptionsReadOnly();

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
		formatter : delBtFormatter,
		cannotTriggerInsert : true,
		resizable : false,
	}, ];

	// Slickgrid dataview (we need sort & co)
	this.dataview = new Slick.Data.DataView(
	{
		inlineFilters : true
	});

	// Create SlickGrid
	this.mainGrid = new Slick.Grid(this.slickContainer, this.dataview, columns, options);
	this.mainGrid.setSelectionModel(new Slick.RowSelectionModel());

	// Generic events
	this.mainGrid.onSort.subscribe(onSort.bind(this)); // A-Z sorting
	this.dataview.onRowCountChanged.subscribe(onRowCountChanged.bind(this)); // Rerender the grid
	this.dataview.onRowsChanged.subscribe(onRowsChanged.bind(this)); // Invalidate the updated rows & rerender grid

	// Specific events
	this.slickContainer.on('click', '.deldto_chronix_oxymores_org_DTOPlaceGroup',
			(function()
			{
				var groupToRemove = this.mainGrid.getDataItem(this.mainGrid.getSelectedRows()[0]);

				this.parent.selectedPlace._memberOf.getString().splice(
						jQuery.inArray(groupToRemove._id, this.parent.selectedPlace._memberOf.getString()), 1);
				groupToRemove._places.getString().splice(jQuery.inArray(this.parent.selectedPlace._id, groupToRemove._places.getString()), 1);

				this.parent.placeMemberOf.dataview.refresh();
				this.parent.groupContent.dataview.refresh();
			}).bind(this));

	// Initialize the model after all the events have been hooked up
	this.dataview.beginUpdate();
	this.dataview.setItems(this.dtoApplication.getGroups().getDTOPlaceGroup(), '_id');
	this.dataview.setFilterArgs(
	{
		searchString : "",
	});
	this.dataview.setFilter(this.placeGroupMembershipContentFilter);
	this.dataview.endUpdate();

	this.resize();
	this.mainGrid.init();
}

PlaceMemberOfPanel.prototype.placeGroupMembershipContentFilter = function(item, args)
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
};

PlaceMemberOfPanel.prototype.resize = slResize;
