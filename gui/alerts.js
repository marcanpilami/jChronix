var singleInfo = null;
var singleInfoClose = null;
var singleAlert = null;

function uinfo(msg, timeout) {
	
	if (timeout === null)
		timeout = 0;
	
	if (singleInfo === null)
	{
		var body = $(document.body);
		singleInfo = $("<div class='info'></div>");
		body.append(singleInfo);
		singleInfo.hide();
		
		singleInfo.click(function() {singleInfo.fadeToggle(1000);});
	}
	else
		singleInfo.hide();
	
	singleInfo.text(msg);
	singleInfo.fadeToggle(600);
	
	if (timeout !== 0)
		singleInfo.delay(timeout).fadeToggle(1000);
}

function ualert() {
	
}
