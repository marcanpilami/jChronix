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
	this.btSameLaunch = $("<button type='button'>new launch for this job</button>").click($.proxy(this.copyLaunch, this))
			.addClass('chrConsoleButton');
	this.mainDiv.append(this.btSameLaunch);
	this.btRestart = $("<button type='button'>restart after crash</button>").click($.proxy(this.copyLaunch, this)).addClass('chrConsoleButton');
	this.mainDiv.append(this.btRestart);
	this.btDlLog = $("<button type='button'>download log</button>").click($.proxy(this.copyLaunch, this)).addClass('chrConsoleButton');
	this.mainDiv.append(this.btDlLog);
	this.btKill = $("<button type='button'>kill</button>").click($.proxy(this.copyLaunch, this)).addClass('chrConsoleButton');
	this.mainDiv.append(this.btKill);
	this.btGiveUp = $("<button type='button'>override failure</button>").click($.proxy(this.copyLaunch, this)).addClass('chrConsoleButton');
	this.mainDiv.append(this.btGiveUp);
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
	con.html("Log starts with: " + text);

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
};

ConsoleFloatingPanel.prototype.copyLaunch = function()
{
	alert(this.cxfRunLog.id);
};