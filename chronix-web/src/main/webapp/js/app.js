function initApp(uuid)
{
    var app;
    var content = $("#tab-" + uuid + " > div");
    var chainPanel;

    $.getJSON("ws/meta/app/id/" + uuid).done(function (data)
    {
        app = data;
        initCommand(app);
        chainPanel = new PanelChain(app);


    });

    content.load("app.html", function ()
    {
        // Replace ids  
        content.html(content[0].innerHTML.replace(/IDIDID/g, uuid));

        // Setup
        content.find(".tabs").tabs({
            activate: function (e, ui)
            {
                var i = ui.newPanel[0].id;
                if (i.indexOf("app-chain") === 0)
                {
                    chainPanel.initPanel();
                }
            }
        });

    });
}

function initCommand(app)
{
    var content = $("#app-command-" + app.id);
    var commands = app.shells;

    new Handsontable(content[0], {
        data: commands,
        minSpareRows: 1,
        rowHeaders: true,
        colHeaders: true,
        contextMenu: false,
        columns: [
            {data: 'id', title: 'ID'},
            {data: 'name', title: 'Name'},
            {data: 'description', title: 'Description'},
            {data: 'command', title: 'Command'}
        ],
        afterChange: initIdIfNone
    });
}

var PanelChain = function (app)
{
    this.tabId = "app-chain-" + app.id;
    this.drawPanelId = "app-" + app.id + "-chain";
    this.tab = $('#' + this.tabId);
    this.drawPanel = $('#' + this.drawPanelId);
    this.chain = app.chains[0];
    this.app = app;
    this.selectedStateDiv = null;
    this.selectedState = null;

    this.jspInstance = jsPlumb.getInstance({
        Connector: ["Bezier", {curviness: 50}],
        DragOptions: {cursor: "pointer", zIndex: 2000},
        PaintStyle: {strokeStyle: "gray", lineWidth: 2},
        EndpointStyle: {radius: 9, fillStyle: "gray"},
        ConnectionOverlays: [['Arrow', {width: 12, location: 1}]],
        HoverPaintStyle: {strokeStyle: "#ec9f2e"},
        EndpointHoverStyle: {fillStyle: "#ec9f2e"},
        Container: this.drawPanelId
    });

    var t = this;
    $("div.app-chain-" + app.id + "-planchoice").select2({
        data: function ()
        {
            return {results: [{name: 'Plans', children: app.plans}, {name: 'Chains', children: app.chains}]};
        },
        formatSelection: function (item) {
            return item.name;
        },
        formatResult: function (item) {
            return item.name;
        }
    }).on('change', function (e)
    {
        $.each(app.chains, function ()
        {
            if (this.id === e.val)
            {
                t.chain = this;
                t.initPanel();
            }
        });
    });

    // Toolbar
    var t = this;
    this.drawPanel.on('click', '.dn', function ()
    {
        var node = $(this);
        t.selectedStateDiv = node;
        t.selectedState = node[0]._source;
        t.drawPanel.find('.dn').removeClass('drawingPanel-selected');
        node.addClass('drawingPanel-selected');
        t.toggleMenu();
    });
};

PanelChain.prototype.initPanel = function ()
{
    // Cleanup  
    this.selectedState = null;
    this.selectedStateDiv = null;
    this.jspInstance.reset();
    this.drawPanel.find(".dn").remove();
    this.initMenu();
    this.toggleMenu();

    // Pass1: nodes
    var t = this;
    $.each(this.chain.states, function ()
    {
        t.drawState(this);
    });

    // Pass2: transitions
    $.each(this.chain.transitions, function ()
    {
        t.jspInstance.connect({
            source: this.from,
            target: this.to,
            label: "[" + this.guard1 + "]"
        });
    });
};

PanelChain.prototype.getStateDiv = function (s)
{
    var d = $("<div id='" + s.id + "' class='dn execnode'>" + this.getStateDivContent(s) + "</div>");
    if (s.isEnd || s.isStart)
    {
        d.css('background-color', 'green');
    }
    return d;
};

PanelChain.prototype.getStateDivContent = function (s)
{
    return "<div>" + s.label + "</div>" +
            "<div class='dn-smalltext'>" + s.runsOnName + (s.parallel ? '//' : '') + "</div>" +
            "<div class='dn-smalltext'>" + (s.calendarId ? 'cal ' + s.calendarShift : '') + "</div>";
};

PanelChain.prototype.setJSPNode = function (node)
{
    // Not using classic anchors as source, as we want the links to be perimeter links
    this.jspInstance.makeSource(node, {
        maxConnections: 100,
        endpoint: 'Blank',
        anchor: ["Perimeter", {shape: "Rectangle"}],
        filter: "div.arrow1",
        connectorStyle: {strokeStyle: 'red'}
    });

    this.jspInstance.makeTarget(node, {
        isTarget: true,
        maxConnections: 100,
        endpoint: 'Blank',
        allowLoopback: false,
        anchor: ["Perimeter", {shape: "Rectangle"}]
    });

    this.jspInstance.draggable(node, {containment: "parent", filter: 'div.anchor', filterExclude: true});
};

PanelChain.prototype.drawState = function (s)
{
    var d = this.getStateDiv(s);
    d[0]._source = s;
    d.appendTo(this.drawPanel);
    d.css('left', s.x);
    d.css('top', s.y);
    this.setJSPNode(d);
};

PanelChain.prototype.redrawSelectedState = function ()
{
    this.selectedStateDiv.html(this.getStateDivContent(this.selectedStateDiv[0]._source));
};

PanelChain.prototype.initMenu = function ()
{
    var m = this.tab.find('ul.menu');

    var grouplist = m.find("li.c-groups > ul");
    grouplist.empty();
    $.each(this.app.groups, function ()
    {
        $("<li value='" + this.id + "'>" + this.name + "</li>").appendTo(grouplist);
    });

    var calendarlist = m.find("li.c-cals > ul");
    calendarlist.empty();
    $("<li>none</li>").appendTo(calendarlist);
    $.each(this.app.calendars, function ()
    {
        $("<li value='" + this.id + "'>" + this.name + "</li>").appendTo(calendarlist);
    });

    m.menu();
    var t = this;
    m.find('.calshift > li').click(function ()
    {
        console.debug(this);
    });

    m.find('.c-para').click(function ()
    {
        t.selectedState.parallel = !t.selectedState.parallel;
        t.redrawSelectedState();
    });

    m.find('li.c-groups > ul > li').click(function ()
    {
        t.selectedState.runsOnId = $(this).attr('value');
        t.selectedState.runsOnName = $(this).text();
        t.redrawSelectedState();
    });

    m.find('li.c-calshifts > ul > li').click(function ()
    {
        t.selectedState.calendarShift = $(this).text();
        t.redrawSelectedState();
    });

    m.find('li.c-cals > ul > li').click(function ()
    {
        t.selectedState.calendarId = $(this).attr('value');
        t.selectedState.calendarName = $(this).text();
        t.redrawSelectedState();
    });

    m.find('li.c-remove').click(function ()
    {
        removeState(t.app, t.selectedState.id);
        t.initPanel();
    });
};

PanelChain.prototype.toggleMenu = function ()
{
    this.tab.find("ul.menu > li.c-remove").addClass('ui-state-disabled');
    this.tab.find("ul.menu > li.c-para").addClass('ui-state-disabled');
    this.tab.find("ul.menu > li.c-cals").addClass('ui-state-disabled');
    this.tab.find("ul.menu > li.c-calshifts").addClass('ui-state-disabled');
    this.tab.find("ul.menu > li.c-groups").addClass('ui-state-disabled');

    if (!this.selectedState)
    {
        return;
    }

    if (!(this.selectedState.start || this.selectedState.end))
    {
        this.tab.find("ul.menu > li.c-remove").removeClass('ui-state-disabled');
    }

    if (!(this.selectedState.start || this.selectedState.end))
    {
        this.tab.find("ul.menu > li.c-para").removeClass('ui-state-disabled');
    }

    if (!(this.selectedState.start || this.selectedState.end))
    {
        this.tab.find("ul.menu > li.c-cals").removeClass('ui-state-disabled');
    }

    if (!(this.selectedState.start || this.selectedState.end))
    {
        this.tab.find("ul.menu > li.c-calshifts").removeClass('ui-state-disabled');
    }
    
    this.tab.find("ul.menu > li.c-groups").removeClass('ui-state-disabled');
};


function removeState(app, stateId)
{
    var toDelete = [];
    $.each(app.chains, function ()
    {
        var chain = this;
        $.each(chain.states, function ()
        {
            if (this.id === stateId)
            {
                toDelete.push(this);
            }
        });
        $.each(toDelete, function ()
        {
            chain.states.splice(chain.states.indexOf(this), 1);
        });
        toDelete = [];

        $.each(chain.transitions, function ()
        {
            if (this.from === stateId || this.to === stateId)
            {
                toDelete.push(this);
            }
        });
        $.each(toDelete, function ()
        {
            chain.transitions.splice(chain.transitions.indexOf(this), 1);
        });
        toDelete = [];
    });
}