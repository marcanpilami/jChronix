var singleInfo = null;
var singleAlert = null;
var singleAlertContent = null;
var singleAlertTitle = null;
var singleAlertDetails= null;
var singleAlertDetailsBt = null;

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

function ualert(msg, title, details) {
	title = typeof title !== 'undefined' ? title: "An error has occured";
	details = typeof details !== 'undefined' ? details: "";
	
	if (singleAlert === null)
	{
		var body = $(document.body);
		singleAlert = $("<div class='alert'></div>");
		body.append(singleAlert);
		singleAlert.hide();
		
		var holder = $("<div class='alertcontent'></div>");
		singleAlert.append(holder);
		
		singleAlertTitle = $("<div class='alerttitle'></div>");
		holder.append(singleAlertTitle);
		singleAlertContent = $("<div class='alerttext'></div>");
		holder.append(singleAlertContent);
		
		
		
		var holder3 = $("<div/>");
		holder.append(holder3);
		var holder2 = $("<button>Close message</button>");
		holder2.click(function() {singleAlert.hide();});
		holder3.append(holder2);
		
		singleAlertDetailsBt = $("<button>Show details</button>");
		singleAlertDetailsBt.click(function() {singleAlertDetails.toggle();});
		holder3.append(singleAlertDetailsBt);
		
		singleAlertDetails = $("<div class='alertdetails'></div>");
		singleAlertDetails.hide();
		holder.append(singleAlertDetails);
	}
	else
		singleAlert.hide();
	
	
	if (details === "")
		singleAlertDetailsBt.hide();
	else
	{
		singleAlertDetails.text(details);
		singleAlertDetailsBt.show();
	}
	singleAlertTitle.text(title);
	singleAlertContent.text(msg);
	singleAlert.fadeToggle(200);
}
