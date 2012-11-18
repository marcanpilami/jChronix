function ConsoleFloatingPanel(containerDiv)
{
	this.cxfRunLog = null;
	this.container = null;

	if (typeof containerDiv === 'string')
		this.container = $("#" + containerDiv);
	else if (typeof containerDiv === 'object')
		this.container = containerDiv;

	this.mainDiv = $("<div id='test'></div>").addClass("floatingPanel");
	this.mainDiv.hide();
	this.container.append(this.mainDiv);

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

ConsoleFloatingPanel.prototype.show = function(xRelativeToContainer, yRelativeToContainer, cxfRunLogToDisplay)
{
	// Select buttons to display
	this.cxfRunLog = cxfRunLogToDisplay;
	if (this.cxfRunLog._lastKnownStatus.substring(0, 4) !== 'DONE')
	{
		this.btSameLaunch.show();
		this.btRestart.hide();
		this.btDlLog.hide();
		this.btKill.show();
		this.btGiveUp.hide();
	}
	else if (obj._resultCode === 0)
	{
		this.btSameLaunch.show();
		this.btRestart.hide();
		this.btDlLog.show();
		this.btKill.hide();
		this.btGiveUp.hide();
	}
	else if (obj._resultCode !== 0)
	{
		this.btSameLaunch.show();
		this.btRestart.show();
		this.btDlLog.show();
		this.btKill.hide();
		this.btGiveUp.show();
	}

	// Position the panel
	this.mainDiv.css("left", 0);
	this.mainDiv.css("top", 0);

	cw = this.container.width();
	ch = this.container.height();

	ww = $(window).width();
	wh = $(window).height();

	vpX = xRelativeToContainer - $(window).scrollLeft();
	vpY = yRelativeToContainer - $(window).scrollTop();

	bw = this.mainDiv.width();
	bh = this.mainDiv.height();

	u = xRelativeToContainer;
	v = yRelativeToContainer;

	posX = xRelativeToContainer;
	posY = yRelativeToContainer;

	if (vpX + bw > ww)
		posX = cw - bw - 20;

	if (vpY < bh)
		posY = yRelativeToContainer + bh + 2;
	// else if (vpY + bh + 2 > wh)
	// posY = yRelativeToContainer - bh - 10;
	else
		posY = yRelativeToContainer - bh - 2;

	this.mainDiv.css("left", posX);
	this.mainDiv.css("top", posY);
	this.mainDiv.show();
};

ConsoleFloatingPanel.prototype.hide = function()
{
	this.mainDiv.hide();
};

ConsoleFloatingPanel.prototype.copyLaunch = function()
{
	alert(this.cxfRunLog._id);
};