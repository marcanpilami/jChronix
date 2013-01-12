function RecurrencePanel(divId, cxfApp)
{
	this.cxfApplication = cxfApp;
	this.html = "<div>  <div><label>Recurrence name: </label><input/><label>(Double-click to edit recurrence)</label></div>  <div id='" + divId
			+ "grid' style='height:100%;'/>  <div id='" + divId + "gridrecpanel'/> </div>";
	var parentDiv = $("#" + divId);
	parentDiv.html(this.html);
	var gridDiv = $("#" + divId + "grid");
	
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
		autoEdit : false,
		enableTextSelectionOnCells : false,
		//autoHeight : true,		
		forceFitColumns : true,
		fullWidthRows : true,
		explicitInitialization : true,
	};

	var columns = [
	{
		id : "name",
		name : "Recurrence name",
		field : "_name",
		minWidth : 100,
		cssClass : "cell-title",
		validator : requiredFieldValidatorCmd,
		sortable : false,
		resizable : true,
	},
	{
		id : "description",
		name : "Description",
		field : "_description",
		minWidth : 150,
		cssClass : "cell-title",
		validator : requiredFieldValidatorCmd,
		sortable : false,
		resizable : true,
	}, ];

	var recGrid = new Slick.Grid("#" + divId + "grid", cxfApp._rrules.getDTORRule(), columns, options);

	recGrid.onDblClick.subscribe(function(e, args)
	{
		var cell = recGrid.getCellFromEvent(e);
		var row = cell.row;
		new RecurrenceEditPanel(divId + "gridrecpanel", cxfApp._rrules.getDTORRule()[row]);
	});
	
	recGrid.init();
	gridDiv.height(gridDiv.closest("div.ui-tabs-panel").height()); // mystery: why cannot init find the height by itself?
	recGrid.resizeCanvas();
};

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
			.append("<div class='txtPair'><label class='txtLabel'>Short name</label><input name='_name' required='required' maxlength='30' size='33' type='text' value='"
					+ cxfRRule._name + "'/></div>");
	basicsDiv
			.append("<div class='txtPair'><label class='txtLabel'>Description</label><input name='_description' required='required' maxlength='200' size='65' type='text' value='"
					+ cxfRRule._description + "'/></div>");
	basicsDiv
			.append("<br/><div class='txtPair'><label class='txtLabel'>Every</label><input name='_interval' required='required' size='5' type='number' max='999' min='1' value='"
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

