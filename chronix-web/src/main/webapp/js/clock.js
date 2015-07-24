function PanelClock(app)
{
    this.app = app;
    this.tab = $("#app-clock-" + app.id);
    this.clocks = app.clocks;
    this.rrules = app.rrules;

    // Load the panel
    var t = this;
    this.tab.load("clock.html", function ()
    {
        // Replace ids
        t.tab.html(t.tab[0].innerHTML.replace(/IDIDID/g, app.id));

        // Init contents
        t.list_rec = t.tab.find("div#app-" + app.id + "-rrulelist");
        t.list_clock = t.tab.find("div#app-" + app.id + "-clocklist");
        t.list_recplus = t.tab.find("div#app-" + app.id + "-recpluslist");
        t.list_recminus = t.tab.find("div#app-" + app.id + "-recminuslist");
        t.list_dateplus = t.tab.find("div#app-" + app.id + "-datepluslist");
        //t.list_dateminus = t.tab.find("div#app-" + app.id + "-dateminuslist");

        // Init data handlers/bindings
        t.tab.find("div#pane-rrulebasics-" + app.id + " > input[type=number]").change(function ()
        {
            t.selectedRule.interval = $(this).val();
        });
        t.tab.find("div#pane-rrulebasics-" + app.id + " > select").change(function ()
        {
            t.selectedRule.period = $(this).val();
        });
        $("div#pane-byminute-" + t.app.id + " > div > span > input").change(function () {
            t.selectedRule['bn' + $(this).val()] = $(this).prop('checked');
        });
        $("div#pane-byhour-" + t.app.id + " > div > span > input").change(function () {
            t.selectedRule['bh' + $(this).val()] = $(this).prop('checked');
        });
        $("div#pane-byweekday-" + t.app.id + " > div > span > input").change(function () {
            t.selectedRule['bd' + $(this).val()] = $(this).prop('checked');
        });
        $("div#pane-bymonthday-" + t.app.id + " > div > span > input").change(function () {
            t.selectedRule['bmd' + $(this).val()] = $(this).prop('checked');
        });
        $("div#pane-bymonthdayminus-" + t.app.id + " > div > span > input").change(function () {
            t.selectedRule['bmdn' + $(this).val()] = $(this).prop('checked');
        });
        $("div#pane-bymonth-" + t.app.id + " > div > span > input").change(function () {
            t.selectedRule['bm' + $(this).val()] = $(this).prop('checked');
        });

        // Init test buttons
        t.tab.find("div#pane-rrulebasics-" + app.id + " > button[name=day]").click(function ()
        {
            t.test(getTomorrowPlus(0), getTomorrowPlus(1));
        });
        t.tab.find("div#pane-rrulebasics-" + app.id + " > button[name=week]").click(function ()
        {
            t.test(getTomorrowPlus(0), getTomorrowPlus(7));
        });
        t.tab.find("div#pane-rrulebasics-" + app.id + " > button[name=month]").click(function ()
        {
            t.test(getTomorrowPlus(0), getTomorrowPlus(31));
        });
        t.tab.find("div#pane-rrulebasics-" + app.id + " > button[name=year]").click(function ()
        {
            t.test(getTomorrowPlus(0), getTomorrowPlus(365));
        });

        // Init add/remove RRule buttons
        t.list_recplus.on('click', "button[title=remplus]", function ()
        {
            var bt = this;
            $.each(t.selectedClock.rulesADD, function (i)
            {
                if (String(this) === bt.value)
                {
                    t.selectedClock.rulesADD.splice(i, 1);
                }
            });
            t.initClock(t.selectedClock);
        });
        t.list_recminus.on('click', "button[title=remminus]", function ()
        {
            var bt = this;
            $.each(t.selectedClock.rulesEXC, function (i)
            {
                if (String(this) === bt.value)
                {
                    t.selectedClock.rulesEXC.splice(i, 1);
                }
            });
            t.initClock(t.selectedClock);
        });
        t.list_dateplus.on('click', "button[title=remdateplus]", function ()
        {

        });

        // ADD rules buttons
        t.tab.find('button[name=adddateplus]').click(function ()
        {
            alert('tt');
            var bt = this;
            var found = false;
            $.each(t.selectedClock.rulesADD, function (i)
            {
                if (String(this) === bt.value)
                {
                    found = true;
                    return;
                }
            });
            if (found)
            {
                t.selectedClock.rulesADD.push(t.selectedRule.id);
                t.initClock(t.selectedClock);
            }

        });

        t.tab.find('button[name=addrecplus]').click(function ()
        {
            var bt = this;
            $.each(t.selectedClock.rulesADD, function ()
            {
                if (String(this) === bt.value)
                {
                    return;
                }
            });
            t.selectedClock.rulesADD.push(t.selectedRule.id);
            t.initClock(t.selectedClock);
        });

        t.tab.find('button[name=addrecminus]').click(function ()
        {
            var bt = this;
            $.each(t.selectedClock.rulesEXC, function ()
            {
                if (String(this) === bt.value)
                {
                    return;
                }
            });
            t.selectedClock.rulesEXC.push(t.selectedRule.id);
            t.initClock(t.selectedClock);
        });
    });
}

PanelClock.prototype.initPanel = function ()
{
    var t = this;
    this.tale_rrule = new Handsontable(this.list_rec[0], {
        data: this.rrules,
        minSpareRows: 1,
        rowHeaders: false,
        colHeaders: true,
        contextMenu: false,
        manualColumnResize: true,
        manualRowResize: false,
        height: 200,
        columns: [
            {data: 'name', title: 'Name'},
            {data: 'description', title: 'Description'}
        ],
        afterSelectionEnd: function (event, col, line)
        {
            var occ = this.getSourceDataAtRow(line);
            t.selectedRule = occ;
            t.initRRule(occ);
        },
        dataSchema: newRec
    });

    this.table_clocks = new Handsontable(this.list_clock[0], {
        data: this.clocks,
        minSpareRows: 1,
        rowHeaders: false,
        colHeaders: true,
        contextMenu: false,
        manualColumnResize: true,
        manualRowResize: false,
        height: 200,
        columns: [
            {data: 'name', title: 'Name'},
            {data: 'description', title: 'Description'}
        ],
        afterSelectionEnd: function (event, col, line)
        {
            var c = this.getSourceDataAtRow(line);
            t.selectedClock = c;
            t.initClock(c);
        },
        dataSchema: newClock
    });
};

PanelClock.prototype.initRRule = function (rrule)
{
    this.tab.find("div#pane-rrulebasics-" + this.app.id + " > input[type=number]").val(rrule.interval);
    this.tab.find("div#pane-rrulebasics-" + this.app.id + " > select").val(rrule.period);

    for (var i = 0; i <= 59; i++)
    {
        var j = i.zeroPad(2);
        var a = this.tab.find("div#pane-byminute-" + this.app.id + " > div > span > input[value=" + j + "]");
        a.prop('checked', rrule['bn' + j]);
    }
    for (var i = 0; i <= 23; i++)
    {
        var j = i.zeroPad(2);
        var a = this.tab.find("div#pane-byhour-" + this.app.id + " > div > span > input[value=" + j + "]");
        a.prop('checked', rrule['bh' + j]);
    }
    for (var i = 1; i <= 7; i++)
    {
        var j = i.zeroPad(2);
        var a = this.tab.find("div#pane-byweekday-" + this.app.id + " > div > span > input[value=" + j + "]");
        a.prop('checked', rrule['bd' + j]);
    }
    for (var i = 1; i <= 31; i++)
    {
        var j = i.zeroPad(2);
        var a = this.tab.find("div#pane-bymonthday-" + this.app.id + " > div > span > input[value=" + j + "]");
        a.prop('checked', rrule['bmd' + j]);
    }
    for (var i = -1; i >= -7; i--)
    {
        var j = i.zeroPad(2);
        var a = this.tab.find("div#pane-bymonthdayminus-" + this.app.id + " > div > span > input[value=" + j + "]");
        a.prop('checked', rrule['bmdn' + j]);
    }
    for (var i = 1; i <= 12; i++)
    {
        var j = i.zeroPad(2);
        var a = this.tab.find("div#pane-bymonth-" + this.app.id + " > div > span > input[value=" + j + "]");
        a.prop('checked', rrule['bm' + j]);
    }
};

PanelClock.prototype.initClock = function (clock)
{
    this.list_recplus.empty();
    this.list_recminus.empty();
    var t = this;
    $.each(clock.rulesADD, function ()
    {
        var r_id = String(this);
        var r_name = null;

        // Find the rrule name
        $.each(t.rrules, function ()
        {
            if (this.id === r_id)
            {
                r_name = this.name;
            }
        });

        $("<div title='" + r_id + "'>" + r_name + "<button title='remplus' value='" + r_id + "'>X</button></div>").appendTo(t.list_recplus);
    });

    $.each(clock.rulesEXC, function ()
    {
        var r_id = String(this);
        var r_name = null;

        // Find the rrule name
        $.each(t.rrules, function ()
        {
            if (this.id === r_id)
            {
                r_name = this.name;
            }
        });

        $("<div title='" + r_id + "'>" + r_name + "<button title='remminus' value='" + r_id + "'>X</button></div>").appendTo(t.list_recminus);
    });

    /*$.each(clock.datesADD, function ()
     {
     var d = this;
     $("<div>" + d + "<button title='remdateplus' value='" + d + "'>X</button></div>").appendTo(t.list_dateplus);
     });*/
};

PanelClock.prototype.test = function (start, end)
{
    this.selectedRule.simulStart = start;
    this.selectedRule.simulEnd = end;
    $.postJSON('ws/meta/rrule/test', this.selectedRule, function (data)
    {
        alert(data.res);
    });
};

function newClock()
{
    var res = new Object();
    res.id = uuid.v4();
    res.rulesADD = [];
    res.rulesEXC = [];
    res.datesADD = [];
    res.datesEXC = [];
    return res;
}

function newRec()
{
    var res = new Object();
    res.id = uuid.v4();
    res.interval = 1;
    res.period = 'DAILY';
    for (var i = 0; i <= 59; i++)
    {
        res["bn" + i.zeroPad(2)] = false;
    }
    for (var i = 0; i <= 23; i++)
    {
        res["bh" + i.zeroPad(2)] = false;
    }
    for (var i = 1; i <= 7; i++)
    {
        res["bd" + i.zeroPad(2)] = false;
    }
    for (var i = 1; i <= 31; i++)
    {
        res["bmd" + i.zeroPad(2)] = false;
    }
    for (var i = -1; i >= -7; i--)
    {
        res["bmdn" + i.zeroPad(2)] = false;
    }
    for (var i = 1; i <= 12; i++)
    {
        res["bm" + i.zeroPad(2)] = false;
    }
    return res;
}