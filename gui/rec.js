function ReccurrencePanel(divId, cxfApp)
{
	this.cxfApplication = cxfApp;
	this.html = "<div><div><label>Schedule name: </label><input/></div></div>";

	$("#" + divId).html(this.html);
}

ReccurrencePanel.prototype.marsu = function()
{
};