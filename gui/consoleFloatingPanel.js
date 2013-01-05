function ConsoleFloatingPanel(containerDiv)
{
	this.cxfRunLog = null;
	this.container = null;
	this.viewportHeight, this.viewportWidth;

	if (typeof containerDiv === 'string')
		this.container = $("#" + containerDiv);
	else if (typeof containerDiv === 'object')
		this.container = containerDiv;

	this.mainDiv = $("<div id='test'></div>").addClass("floatingPanel");
	this.mainDiv.hide();
	this.container.append(this.mainDiv);

	this.secondaryDiv = $("<div></div>").addClass("floatingPanel");
	this.secondaryDiv.hide();
	this.container.append(this.secondaryDiv);

	// Dimensions
	this.setViewport($(window).width(), $(window).height());

	// add buttons
	this.btSameLaunch = $("<button type='button'>new launch for this job</button>").click($.proxy(this.relaunch, this)).addClass('chrConsoleButton');
	this.mainDiv.append(this.btSameLaunch);
	this.btRestart = $("<button type='button'>restart after crash</button>").click($.proxy(this.todo, this)).addClass('chrConsoleButton');
	this.mainDiv.append(this.btRestart);
	this.btDlLog = $("<button type='button'>download log</button>").click($.proxy(this.dlLogFile, this)).addClass('chrConsoleButton');
	this.mainDiv.append(this.btDlLog);
	this.btKill = $("<button type='button'>kill</button>").click($.proxy(this.todo, this)).addClass('chrConsoleButton');
	this.mainDiv.append(this.btKill);
	this.btGiveUp = $("<button type='button'>override failure</button>").click($.proxy(this.forceOK, this)).addClass('chrConsoleButton');
	this.mainDiv.append(this.btGiveUp);
	this.btHide = $("<button type='button'>hide toolbar</button>").click($.proxy(this.hide, this)).addClass('chrConsoleButton');
	this.mainDiv.append(this.btHide);
}

ConsoleFloatingPanel.prototype.setViewport = function(width, height)
{
	this.viewportHeight = height;
	this.viewportWidth = width;
};

ConsoleFloatingPanel.prototype.receiveDetails = function(text)
{
	this.secondaryDiv.css("left", 10);
	this.secondaryDiv.css("top", 55 + parseInt(this.mainDiv.css("top").replace("px", "")));

	con = $("<span class='log'/>");
	this.secondaryDiv.html(con);
	if (text !== "")
	{
		con.html("Log starts with: " + text.replace(/\n/g, '<br />'));
		this.btDlLog.show();
	}
	else
	{
		con.html("No log");
		this.btDlLog.hide();
	}

	this.secondaryDiv.show();
};

ConsoleFloatingPanel.prototype.show = function(xRelativeToContainer, yRelativeToContainer, cxfRunLogToDisplay)
{
	// Select buttons to display
	this.cxfRunLog = cxfRunLogToDisplay;
	if (this.cxfRunLog.lastKnownStatus.substring(0, 4) !== 'DONE')
	{
		this.btSameLaunch.show();
		this.btRestart.hide();
		this.btDlLog.hide();
		this.btKill.show();
		this.btGiveUp.hide();
	}
	else if (this.cxfRunLog.resultCode === 0)
	{
		this.btSameLaunch.show();
		this.btRestart.hide();
		this.btDlLog.show();
		this.btKill.hide();
		this.btGiveUp.hide();
	}
	else if (this.cxfRunLog.resultCode !== 0)
	{
		this.btSameLaunch.hide();
		this.btRestart.show();
		this.btDlLog.show();
		this.btKill.hide();
		this.btGiveUp.show();
	}

	// Position the panel
	this.mainDiv.css("left", 0);
	this.mainDiv.css("top", 0);

	var cw = this.container.width();
	// var ch = this.container.height();

	var ww = $(window).width();
	// var wh = $(window).height();

	var vpX = xRelativeToContainer - $(window).scrollLeft();
	var vpY = yRelativeToContainer - $(window).scrollTop();

	var ctX = this.container.position().left;
	var ctY = this.container.position().top;

	var bw = this.mainDiv.width();
	var bh = this.mainDiv.height();

	var posX = xRelativeToContainer;
	var posY = yRelativeToContainer;

	if (vpX + bw > ww)
		posX = cw - bw - 20 - ctX;

	if (vpY < bh + 2)
		posY = yRelativeToContainer + bh + 8 - ctY;
	else
		posY = yRelativeToContainer - bh - 2 - ctY;

	this.mainDiv.css("left", posX);
	this.mainDiv.css("top", posY);
	this.mainDiv.show();

	$.get("console/rest/main/shortlog/" + this.cxfRunLog.id, this.receiveDetails.bind(this));
};

ConsoleFloatingPanel.prototype.hide = function()
{
	this.mainDiv.hide();
	this.secondaryDiv.hide();
};

ConsoleFloatingPanel.prototype.todo = function()
{
	alert(this.cxfRunLog.id);
};

ConsoleFloatingPanel.prototype.forceOK = function()
{
	$.getJSON('/console/rest/main/order/forceok/' + this.cxfRunLog.id, this.forceOKDone.bind(this));
};

ConsoleFloatingPanel.prototype.forceOKDone = function(json)
{
	var res = json.ResOrder;
	if (res.success)
		uinfo("Override order was delivered successfuly", 5000);
	else
		ualert(res.message);
};

ConsoleFloatingPanel.prototype.dlLogFile = function()
{
	var NWin = window.open('/console/rest/main/logfile/' + this.cxfRunLog.id);
	if (window.focus)
	{
		NWin.focus();
	}
};

ConsoleFloatingPanel.prototype.relaunch = function()
{
	$.getJSON('/console/rest/main/order/launch/outofplan/duplicatelaunch/' + this.cxfRunLog.id, this.relaunchDone.bind(this));
};

ConsoleFloatingPanel.prototype.relaunchDone = function(json)
{
	var res = json.ResOrder;
	if (res.success)
		uinfo("New launch (out of plan) order was delivered successfuly", 5000);
	else
		ualert(res.message);
};
