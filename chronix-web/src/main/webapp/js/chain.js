/* global jsPlumb, item2name */

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
        formatSelection: item2name,
        formatResult: item2name
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
        $.each(app.plans, function ()
        {
            if (this.id === e.val)
            {
                t.chain = this;
                t.initPanel();
            }
        });
    }).select2('val', t.chain.id);
    //$("div.app-chain-" + app.id + "-planchoice").val(app.chains[0]);

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

    // Palette lists
    $("#app-" + t.app.id + "-palette-shell").select2({
        data: t.app.shells,
        formatSelection: item2name,
        formatResult: item2name
    });
    $("#app-" + t.app.id + "-palette-chain").select2({
        data: t.app.chains,
        formatSelection: item2name,
        formatResult: item2name
    });
    $("#app-" + t.app.id + "-palette-clock").select2({
        data: t.app.clocks,
        formatSelection: item2name,
        formatResult: item2name
    });
    $("#app-" + t.app.id + "-palette-external").select2({
        data: t.app.externals,
        formatSelection: item2name,
        formatResult: item2name
    });
    $("#app-" + t.app.id + "-palette-calnext").select2({
        data: t.app.calnexts,
        formatSelection: item2name,
        formatResult: item2name
    });

    this.tab.find("div.palette > div > button").click(function ()
    {
        var source = null;
        var orig = $(this).attr('name');

        switch (orig)
        {
            case 'shell' :
                source = $("#app-" + t.app.id + "-palette-shell").val();
                break;
            case 'chain' :
                source = $("#app-" + t.app.id + "-palette-chain").val();
                break;
            case 'clock' :
                source = $("#app-" + t.app.id + "-palette-clock").val();
                break;
            case 'external' :
                source = $("#app-" + t.app.id + "-palette-external").val();
                break;
            case 'calnext' :
                source = $("#app-" + t.app.id + "-palette-calnext").val();
                break;
            case 'and':
                source = t.app.andId;
                break;
            case 'or':
                source = t.app.orId;
                break;
        }
        if (!source)
        {
            alert("Please select an item before trying to add it");
            return;
        }

        var state = newState(t.app, 10, 10, source, t.app.groups[0].id);
        t.chain.states.push(state);
        t.drawState(state);
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

    // Show only neede palette items
    this.plan = true;
    $.each(this.chain.states, function ()
    {
        if (this.start)
        {
            t.plan = false;
        }
    });
    if (this.plan)
    {
        this.tab.find(".palette > .block").show();
    }
    else
    {
        this.tab.find(".palette > .block > div[id$=chain]").parent().hide();
        this.tab.find(".palette > .block > div[id$=clock]").parent().hide();
        this.tab.find(".palette > .block > div[id$=external]").parent().hide();
    }
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

    this.jspInstance.draggable(node, {containment: "parent", filter: 'div.anchor', filterExclude: true,
        stop: function (event)
        {
            event.el._source.x = event.pos[0];
            event.el._source.y = event.pos[1];
        }}
    );
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