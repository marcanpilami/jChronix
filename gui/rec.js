
function SchedulePanel(divId, cxfApp)
{
	this.divId = divId;
	this.cxfApplication = cxfApp;

	//////////////////////////////////////////////////////////////////////////////////////
	// HTML template
	//////////////////////////////////////////////////////////////////////////////////////
	this.html = "\
		<div style='display: table; height: 100%; width:100%; table-layout: fixed;'>\
			<div style='display: table-row;'>\
				<label>Search: </label>\
				<input id='"
			+ divId
			+ "recsearch'></input>\
				<label></label>\
			</div> \
			<div id='"
			+ divId
			+ "gridreccontainer' style='display: table-row; height: 66%;'>\
				<div id='"
			+ divId
			+ "gridrec' style='height: 100%;'></div>\
			</div>\
			<div style='display: table-row;'>\
			<label>Search: </label>\
			<input id='"
			+ divId
			+ "schsearch'></input>\
			<label></label>\
		</div> \
			<div id='"
			+ divId
			+ "gridschcontainer' style='display: table-row; height: 34%;'>\
				<div id='"
			+ divId
			+ "gridsch' style='height: 100%;'></div>\
			</div>\
			<div id='" + divId + "gridrecpanel'></div>\
		</div>";

	var parentDiv = $("#" + this.divId);
	parentDiv.html(this.html);

	
	//////////////////////////////////////////////////////////////////////////////////////
	// Recurrence grid
	//////////////////////////////////////////////////////////////////////////////////////
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
		enableTextSelectionOnCells : true,
		autoHeight : false,
		forceFitColumns : true,
		fullWidthRows : true,
		explicitInitialization : true,
		syncColumnCellResize : true,
	};

	var columns = [
	{
		id : "name",
		name : "Recurrence name",
		field : "_name",
		minWidth : 150,
		sortable : true,
		resizable : true,
		editor : Slick.Editors.Text,
	},
	{
		id : "recdescription",
		name : "Description",
		field : "_description",
		minWidth : 200,
		sortable : true,
		resizable : true,
		editor : Slick.Editors.Text,
	},
	{
		id : "del",
		name : "",
		field : "del",
		maxWidth : 35,
		formatter : delCmdBtFormatter,
		cannotTriggerInsert : true,
	}, 
	{
		id : "edit",
		name : "",
		field : "edit",
		maxWidth : 35,
		formatter : editCmdBtFormatter,
		cannotTriggerInsert : true,
	},];

	this.recDataView = new Slick.Data.DataView(
	{
		inlineFilters : true
	});
	this.recDataView.setFilterArgs(
	{
		searchString : "",
	});
	this.recDataView.setFilter(nameDescriptionFilter);
	this.recDataView.setItems(this.cxfApplication._rrules.getDTORRule(), '_id');
	this.recGrid = new Slick.Grid("#" + this.divId + "gridrec", this.recDataView, columns, options);
	this.recGrid.setSelectionModel(new Slick.RowSelectionModel());

	// Subscribe to data change events (both ways)
	this.recDataView.onRowCountChanged.subscribe((function(e, args)
	{
		this.recGrid.updateRowCount();
		this.recGrid.render();
	}).bind(this));
	this.recDataView.onRowsChanged.subscribe((function(e, args)
	{
		this.recGrid.invalidateRows(args.rows);
		this.recGrid.render();
	}).bind(this));
	this.recGrid.onCellChange.subscribe((function(e, args)
	{
		this.recDataView.updateItem(args.item._id, args.item);
	}).bind(this));
	this.recGrid.onAddNewRow.subscribe((function(e, args)
	{
		var v = new dto_chronix_oxymores_org_DTORRule();
		for ( var o in args.item)
		{
			v[o] = args.item[o];
		}
		v._id = uuid.v4();
		this.recDataView.addItem(v);
	}).bind(this));
	
	// Edit
	$('#' + this.divId + "gridrec").on('click', '.delcmd', (function(event)
	{
		this.recDataView.deleteItem($(event.target).attr("id").substr(3));
	}).bind(this));
	$('#' + this.divId + "gridrec").on('click', '.editcmd', (function(event)
	{
		new RecurrenceEditPanel(this.divId + "gridrecpanel", this.recDataView.getItemById($(event.target).attr("id").substr(4)));
	}).bind(this));
	
	// Search
	$("#" + this.divId + "recsearch").keyup((function(e)
	{
		Slick.GlobalEditorLock.cancelCurrentEdit();

		// Clear on Esc
		if (e.which == 27)
		{
			$(e.currentTarget).val("");
		}
		// Update dataview filter
		this.recDataView.setFilterArgs(
		{
			searchString : $.trim($(e.currentTarget).val()),
		});
		// Refresh dataview
		this.recDataView.refresh();
	}).bind(this));
	
	
	
	//////////////////////////////////////////////////////////////////////////////////////
	// Schedule grid
	//////////////////////////////////////////////////////////////////////////////////////
	
	var options_sch =
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

	var columns_sch = [
	{
		id : "name",
		name : "Schedule name",
		field : "_name",
		minWidth : 150,
		sortable : true,
		resizable : true,
		editor : Slick.Editors.Text
	},
	{
		id : "schdescription",
		name : "Description",
		field : "_description",
		minWidth : 200,
		sortable : true,
		resizable : true,
		editor : Slick.Editors.Text
	}, 
	{
		id : "del",
		name : "",
		field : "del",
		maxWidth : 35,
		formatter : delCmdBtFormatter,
		cannotTriggerInsert : true,
	}, 
	{
		id : "edit",
		name : "",
		field : "edit",
		maxWidth : 35,
		formatter : editCmdBtFormatter,
		cannotTriggerInsert : true,
	},];

	this.schDataView = new Slick.Data.DataView(
	{
		inlineFilters : true
	});
	this.schDataView.setFilterArgs(
	{
		searchString : "",
	});
	this.schDataView.setFilter(nameDescriptionFilter);
	this.schDataView.setItems(this.cxfApplication._clocks.getDTOClock(), '_id');
	
	this.schGrid = new Slick.Grid("#" + this.divId + "gridsch", this.schDataView, columns_sch, options_sch);
	this.schGrid.setSelectionModel(new Slick.RowSelectionModel());

	// Subscribe to data change events (both ways)
	this.schGrid.onCellChange.subscribe((function(e, args)
	{
		this.schDataView.updateItem(args.item._id, args.item);
	}).bind(this));
	this.schDataView.onRowCountChanged.subscribe((function(e, args)
	{
		this.schGrid.updateRowCount();
		this.schGrid.render();
	}).bind(this));
	this.schDataView.onRowsChanged.subscribe((function(e, args)
	{
		this.schGrid.invalidateRows(args.rows);
		this.schGrid.render();
	}).bind(this));
	this.schGrid.onAddNewRow.subscribe((function(e, args)
	{
		var v = new dto_chronix_oxymores_org_DTOClock();
		for ( var o in args.item)
		{
			v[o] = args.item[o];
		}
		v._id = uuid.v4();
		v._rulesADD = new internalapi_chronix_oxymores_org__ArrayOfString();
		v._rulesEXC = new internalapi_chronix_oxymores_org__ArrayOfString();
		this.schDataView.addItem(v);
	}).bind(this));
	
	// Edit event
	$('#' + this.divId + "gridsch").on('click', '.delcmd', (function(event)
	{
		this.schDataView.deleteItem($(event.target).attr("id").substr(3));
	}).bind(this));
	$('#' + this.divId + "gridsch").on('click', '.editcmd', (function(event)
	{
		new ScheduleEditPanel(this.divId + "gridrecpanel", this.cxfApplication, this.schDataView.getItemById($(event.target).attr("id").substr(4)));
	}).bind(this));
	
	// Search
	$("#" + this.divId + "schsearch").keyup((function(e)
	{
		Slick.GlobalEditorLock.cancelCurrentEdit();

		// Clear on Esc
		if (e.which == 27)
		{
			$(e.currentTarget).val("");
		}
		// Update dataview filter
		this.schDataView.setFilterArgs(
		{
			searchString : $.trim($(e.currentTarget).val()),
		});
		// Refresh dataview
		this.schDataView.refresh();
	}).bind(this));
	
	
	//////////////////////////////////////////////////////////////////////////////////////
	// Finalization
	//////////////////////////////////////////////////////////////////////////////////////
	this.resize();
	this.recGrid.init();
	this.schGrid.init();
	$(window).resize(this.resize.bind(this));
};

SchedulePanel.prototype.resize = function()
{
	if (!$("#" + this.divId + "gridrec").is(':visible'))
		return;
	$("#" + this.divId + "gridrec").height(0);
	$("#" + this.divId + "gridrec").height($("#" + this.divId + "gridreccontainer").height());
	$("#" + this.divId + "gridrec").width($("#" + this.divId + "gridreccontainer").width());
	this.recGrid.resizeCanvas();
	
	if (!$("#" + this.divId + "gridsch").is(':visible'))
		return;
	$("#" + this.divId + "gridsch").height(0);
	$("#" + this.divId + "gridsch").height($("#" + this.divId + "gridschcontainer").height());
	$("#" + this.divId + "gridsch").width($("#" + this.divId + "gridschcontainer").width());
	this.schGrid.resizeCanvas();
};

SchedulePanel.prototype.redisplay = function()
{
	this.resize();
};

function ScheduleEditPanel(divId, cxfApplication, cxfSchedule)
{
	this.mainDiv = $("#" + divId);
	this.cxfApp = cxfApplication;
	this.mainDiv.hide();
	this.mainDiv.empty();
	
	this.html = "<div class='recPanel'> <form action=''> <div id='recIN" + divId + "'/> <div id='recOUT" + divId + "'/> <div id='schBOTTOM"+divId+"'/>  </form> </div>";
	this.mainDiv.append(this.html);
	
	// REC IN
	$("#recIN" + divId).append("<div id='recINContent" + divId + "' class='subPanel1'/>");
	var recIN = $("#recINContent" + divId);
	recIN.append("<label class='cbxListTitle' for='recINContent" + divId + "'>Included recurrences</label>");
	
	var rules = this.cxfApp._rrules.getDTORRule();
	var length = rules.length;
	var ch = "";
	var rule = null;
	var selected = cxfSchedule._rulesADD.getString();
	for (var i = 0; i < length; i++)
	{
		rule = rules[i];
		ch = "";
		if (selected.indexOf(rule._id) >= 0)
			ch = "checked='checked'";
		recIN.append("<div class='cbxListPair'> <input id='cbxrecin" + divId + i + "' type='checkbox' name='recin' value='" + i + "' " + ch
				+ "/><label for='cbxrecin" + divId + i + "' class='cbListLabel' title='"+rule._description+"'>" + rule._name + "</span> </div>");
	}
	
	$("#recINContent" + divId + " input:checkbox").click(function()
	{
		var rule = rules[this.value];
		if (this.checked && cxfSchedule._rulesADD.getString().indexOf(rule._id) < 0)
			cxfSchedule._rulesADD.getString().push(rule._id);
		else if ((! this.checked) && cxfSchedule._rulesADD.getString().indexOf(rule._id) >= 0)
		cxfSchedule._rulesADD.getString().pop(rule._id);
	});
	
	// REC OUT
	$("#recOUT" + divId).append("<div id='recOUTContent" + divId + "' class='subPanel1'/>");
	var recOUT = $("#recOUTContent" + divId);
	recOUT.append("<label class='cbxListTitle' for='recOUTContent" + divId + "'>Excluded recurrences</label>");
	
	var selected = cxfSchedule._rulesEXC.getString();
	for (var i = 0; i < length; i++)
	{
		rule = rules[i];
		ch = "";
		if (selected.indexOf(rule._id) >= 0)
			ch = "checked='checked'";
		recOUT.append("<div class='cbxListPair'> <input id='cbxrecout" + divId + i + "' type='checkbox' name='recout' value='" + i + "' " + ch
				+ "/><label for='cbxrecout" + divId + i + "' class='cbListLabel' title='"+rule._description+"'>" + rule._name + "</span> </div>");
	}
	
	$("#recOUTContent" + divId + " input:checkbox").click(function()
	{
		var rule = rules[this.value];
		if (this.checked && cxfSchedule._rulesEXC.getString().indexOf(rule._id) < 0)
			cxfSchedule._rulesEXC.getString().push(rule._id);
		else if ((! this.checked) && cxfSchedule._rulesEXC.getString().indexOf(rule._id) >= 0)
		cxfSchedule._rulesEXC.getString().pop(rule._id);
	});
	
	// Close button
	this.bottomDiv = $("#schBOTTOM" + divId);
	this.bottomDiv.append($("<button type='button'>Close panel </button>").click(function()
	{
		$("#" + divId).hide(600);
	}));

	// Done
	this.mainDiv.show(300);
}

function RecurrenceEditPanel(divId, cxfRRule)
{
	this.mainDiv = $("#" + divId);
	this.mainDiv.hide();
	this.mainDiv.empty();
	this.html = "<div class='recPanel'> <form action=''> <div id='basics" + divId + "'/> <div id='by" + divId + "'/> <div id='test" + divId
			+ "'/> </form> </div>";

	$("#" + divId).append(this.html);
	var basicsDiv = $("#basics" + divId);
	var byDiv = $("#by" + divId);
	var testDiv = $("#test" + divId);

	basicsDiv.addClass("subPanel1");
	byDiv.addClass("subPanel1");

	// Basic data
	basicsDiv
			.append("<div class='txtPair'><label class='txtLabel'>Every</label><input name='_interval' required='required' size='5' type='number' max='999' min='1' value='"
					+ cxfRRule._interval + "'/></div>");
	var periods_display = [ "second", "minute", "hour", "day", "week", "month", "year" ];
	var periods_value = [ "SECONLDY", "MINUTELY", "HOURLY", "DAILY", "WEEKLY", "MONTHLY", "YEARLY" ];
	basicsDiv.append("<select name='_period' id='per" + divId + "'/>");
	for ( var i = 1; i < periods_value.length; i++) // 1 -> no secondly
	{
		$("#per" + divId).append($("<option/>").val(periods_value[i]).html(periods_display[i]));
	}
	$("#per" + divId).val(cxfRRule._period);

	// Init temp vars
	var checked = false;
	var nb = "00";

	// Minutes
	byDiv.append("<div id='bn" + divId + "' class='subPanel2'/>");
	var bnDiv = $("#bn" + divId);
	bnDiv.append("<label class='cbxListTitle' for='bn" + divId + "'>Force minutes:</label>");
	for ( var i = 0; i <= 59; i++)
	{
		nb = "";
		if (i < 10)
			nb = "0" + i;
		else
			nb = "" + i;
		checked = cxfRRule["_bn_" + nb];
		ch = "";
		if (checked)
			ch = "checked='checked'";

		bnDiv.append("<div class='cbxListPair'> <input id='cbxbn" + divId + nb + "' type='checkbox' name='bn' value='" + nb + "' " + ch
				+ "/><label for='cbxbn" + divId + nb + "' class='cbListLabel'>" + nb + "</span> </div>");
	}

	// Hours
	byDiv.append("<div id='bh" + divId + "' class='subPanel2'/>");
	var bhDiv = $("#bh" + divId);
	bhDiv.append("<label class='cbxListTitle' for='bh" + divId + "'>Force hours:</label>");
	for ( var i = 0; i <= 23; i++)
	{
		nb = "";
		if (i < 10)
			nb = "0" + i;
		else
			nb = "" + i;
		checked = cxfRRule["_bh_" + nb];
		ch = "";
		if (checked)
			ch = "checked='checked'";

		bhDiv.append("<div class='cbxListPair'> <input id='cbxbh" + divId + nb + "' type='checkbox' name='bh' value='" + nb + "' " + ch
				+ "/><label for='cbxbh" + divId + nb + "' class='cbListLabel'>" + nb + "</span> </div>");
	}

	// Days in month
	byDiv.append("<div id='bmd" + divId + "' class='subPanel2'/>");
	byDiv.append("<div id='bmdn" + divId + "' class='subPanel2'/>");
	var bmdDiv = $("#bmd" + divId);
	var bmdnDiv = $("#bmdn" + divId);
	bmdDiv.append("<label class='cbxListTitle' for='bmd" + divId + "'>Force days in month:</label>");
	bmdnDiv.append("<label class='cbxListTitle' for='bmdn" + divId + "'>Force days in month (from the end of the month):</label>");

	for ( var i = 1; i <= 31; i++)
	{
		if (i < 10)
			nb = "0" + i;
		else
			nb = "" + i;

		checked = cxfRRule["_bmd_" + nb];
		ch = "";
		if (checked)
			ch = "checked='checked'";

		bmdDiv.append("<div class='cbxListPair'> <input id='cbxbmd" + divId + nb + "' type='checkbox' name='bmd' value='" + nb + "' " + ch
				+ "/><label for='cbxbmd" + divId + nb + "' class='cbListLabel'>" + nb + "</span> </div>");
	}
	for ( var i = 1; i <= 7; i++)
	{
		nb = "-0" + i;
		checked = cxfRRule["_bmdn_" + nb];
		ch = "";
		if (checked)
			ch = "checked='checked'";

		bmdnDiv.append("<div class='cbxListPair'> <input id='cbxbmdn" + divId + nb + "' type='checkbox' name='bmdn' value='0" + i + "' " + ch
				+ "/><label for='cbxbmdn" + divId + nb + "' class='cbListLabel'>" + nb + "</span> </div>");
	}

	// Weekdays
	byDiv.append("<div id='bd" + divId + "' class='subPanel2'/>");
	var bdDiv = $("#bd" + divId);
	bdDiv.append("<label class='cbxListTitle' for='bd" + divId + "'>Force days in week:</label>");
	var days = [ "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturay", "Sunday" ];
	for ( var i = 1; i <= 7; i++)
	{
		nb = "0" + i;
		checked = cxfRRule["_bd_" + nb];
		ch = "";
		if (checked)
			ch = "checked='checked'";

		bdDiv.append("<div class='cbxListPair'> <input id='cbxnb" + divId + nb + "' type='checkbox' name='bd' value='" + nb + "' " + ch
				+ "/><label for='cbxnb" + divId + nb + "' class='cbListLabel'>" + days[i - 1] + "</span> </div>");
	}

	// Months
	byDiv.append("<div id='bm" + divId + "' class='subPanel2'/>");
	var bmDiv = $("#bm" + divId);
	bmDiv.append("<label class='cbxListTitle' for='bm" + divId + "'>Force months:</label>");
	var months = [ "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December" ];
	for ( var i = 1; i <= 12; i++)
	{
		nb = "";
		if (i < 10)
			nb = "0" + i;
		else
			nb = "" + i;
		checked = cxfRRule["_bm_" + nb];
		ch = "";
		if (checked)
			ch = "checked='checked'";

		bmDiv.append("<div class='cbxListPair'> <input id='cbxbm" + divId + nb + "' type='checkbox' name='bm' value='" + nb + "' " + ch
				+ "/><label for='cbxbm" + divId + nb + "' class='cbListLabel'>" + months[i - 1] + "</span> </div>");
	}

	// Test
	function testOK(responseObject)
	{
		aa = responseObject;
		var list = responseObject.getReturn().getDateTime();
		var res = "";
		for ( var i = 0; i < list.length; i++)
			res = res + "\n" + list[i];
		alert("There were " + list.length + " ocurrences returned\n" + res);
	}

	function testKO(responseObject)
	{
		alert("KO" + responseObject);
	}

	testDiv.append($("<button type='button'>Compute ocurrences between </button>").click(
			function()
			{
				proxy.getNextRRuleOccurrences(testOK, testKO, cxfRRule, Date.parseExact($("#tstlow" + divId).val(), "dd/MM/yyyy HH:mm:ss").toString(
						"dd/MM/yyyy HH:mm"), Date.parseExact($("#tsthigh" + divId).val(), "dd/MM/yyyy HH:mm:ss").toString("dd/MM/yyyy HH:mm"));
			}));
	testDiv.append("<input id='tstlow" + divId + "' type='datetime' value='01/01/2012 19:00:00'/>");
	testDiv.append("<label> and </label>");
	testDiv.append("<input id='tsthigh" + divId + "' type='datetime' value='01/01/2012 20:00:00'/>");

	// Hide (and seek)
	function hide()
	{
		var v = cxfRRule._period;
		if (v === "YEARLY")
		{
			bmdDiv.show();
			bmdnDiv.show();
			bdDiv.show();
			bmDiv.show();
			bhDiv.show();
			bnDiv.show();
		}
		if (v === "MONTHLY")
		{
			bmdDiv.show();
			bmdnDiv.show();
			bdDiv.show();
			bmDiv.show();
			bhDiv.show();
			bnDiv.show();
		}
		if (v === "WEEKLY")
		{
			bmdDiv.show();
			bmdnDiv.show();
			bdDiv.show();
			bmDiv.hide();
			bhDiv.show();
			bnDiv.show();
		}
		if (v === "DAILY")
		{
			bmdDiv.show();
			bmdnDiv.show();
			bdDiv.show();
			bmDiv.hide();
			bhDiv.show();
			bnDiv.show();
		}
		if (v === "HOURLY")
		{
			bmdDiv.hide();
			bmdnDiv.hide();
			bdDiv.hide();
			bmDiv.hide();
			bhDiv.show();
			bnDiv.show();
		}
		if (v === "MINUTELY")
		{
			bmdDiv.hide();
			bmdnDiv.hide();
			bdDiv.hide();
			bmDiv.hide();
			bhDiv.hide();
			bnDiv.show();
		}
	}

	// Events
	$("#by" + divId + " input:checkbox").click(function()
	{
		var attr = "_" + this.name + "_" + this.value;
		if (this.checked)
			cxfRRule[attr] = true;
		else
			cxfRRule[attr] = false;
	});

	$("#basics" + divId + " input,select").change(function()
	{
		cxfRRule[this.name] = this.value;
		hide();
	});

	// Init panels
	hide();

	// Close button
	testDiv.append($("<button type='button'>Close panel </button>").click(function()
	{
		$("#" + divId).hide(600);
	}));

	// Done
	this.mainDiv.show(300);
};

