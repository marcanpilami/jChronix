/* global jsPlumb, item2name, uuid */

var PanelChain = function (app)
{
    var t = this;
    this.tabId = "app-chain-" + app.id;
    this.drawPanelId = "app-" + app.id + "-chain";
    this.tab = $('#' + this.tabId);
    this.drawPanel = $('#' + this.drawPanelId);
    this.chain = app.chains[0];
    this.app = app;
    this.selectedStateDiv = null;
    this.selectedState = null;
    this.chainselect = $("div.app-chain-" + app.id + "-planchoice");
    this.connectorColor = "DodgerBlue";
    this.connectorColorSelected = "red";
    this.connectorColorHover = "Sienna";

    this.jspInstance = jsPlumb.getInstance({
        Connector: ["Bezier", {curviness: 50}],
        DragOptions: {cursor: "pointer", zIndex: 2000},
        PaintStyle: {strokeStyle: this.connectorColor, lineWidth: 2, fillStyle: this.connectorColor},
        EndpointStyle: {radius: 9, fillStyle: "gray"},
        ConnectionOverlays: [['Arrow', {width: 12, location: 1}]],
        HoverPaintStyle: {strokeStyle: this.connectorColorHover},
        EndpointHoverStyle: {fillStyle: this.connectorColorHover},
        Container: this.drawPanelId
    });


    // Chain/plan selection and creation
    this.chainselect.select2({
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

    this.tab.find("button[name=addplan]").click(function ()
    {
        var n = newPlan(t.app);
        t.chainselect.select2('val', n.id);
        t.chain = n;
        t.initPanel();
    });
    this.tab.find("button[name=addchain]").click(function ()
    {
        var n = newChain(t.app);
        t.chainselect.select2('val', n.id);
        t.chain = n;
        t.initPanel();
    });
    this.tab.find("div > div > div > span").click(function ()
    {
        if (t.chain)
        {
            removeChain(t.app, t.chain, true);
            t.chain = null;
            t.initPanel();
            t.chainselect.select2('val', null);
        }
    });

    // Node selection
    var t = this;
    this.drawPanel.on('click', '.dn', function ()
    {
        t.deselectAll();
        var node = $(this);
        t.selectedStateDiv = node;
        t.selectedState = node[0]._source;
        node.addClass('drawingPanel-selected');

        t.toggleMenu();
    });

    // Transition selection: inside initPanel (linked to jsPlumb)

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

    this.initMenu();
};

PanelChain.prototype.initPanel = function ()
{
    // Cleanup
    this.selectedState = null;
    this.selectedStateDiv = null;
    this.jspInstance.reset();
    this.drawPanel.find(".dn").remove();
    this.toggleMenu();

    if (!this.chain)
    {
        return;
    }

    // Pass1: nodes
    var t = this;
    $.each(this.chain.states, function ()
    {
        t.drawState(this);
    });

    // Pass2: transitions
    $.each(this.chain.transitions, function ()
    {
        var tr = t.jspInstance.connect({
            source: this.from,
            target: this.to,
            label: t.getTransitionLabel(this)
        });
        tr._source = this;
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

    // New connections
    this.jspInstance.bind("beforeDrop", function (params) {
        var dest = null;
        $.each(t.chain.states, function ()
        {
            if (params.targetId === this.id)
            {
                dest = this;
            }
        });

        var received = 0;
        $.each(t.chain.transitions, function ()
        {
            if (this.to === params.targetId)
            {
                received++;
            }
        });

        if (received !== 0 && !dest.canReceiveMultipleLinks)
        {
            return false;
        }

        // It's OK, register the link
        var tr = {from: params.sourceId, to: params.targetId, guard1: 0, id: uuid.v4(), calendarAware: false, calendarShift: 0};
        t.chain.transitions.push(tr);
        params.connection._source = tr;
        params.connection.setLabel(t.getTransitionLabel(tr));
        return true;
    });

    // Click on connections (not inside constructor because jsplumb reset)
    this.jspInstance.bind("click", function (connector, origEvent) {
        t.deselectAll();

        t.selectedTransitionConnector = connector;
        t.selectedTransition = connector._source;
        t.selectedTransitionConnector.setPaintStyle({fillStyle: t.connectorColorSelected, strokeStyle: t.connectorColorSelected});

        t.toggleMenu();
        return true;
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
    res = "<div>" + s.label + "</div>" +
            "<div class='dn-smalltext'>" + s.runsOnName + (s.parallel ? '//' : '') + "</div>" +
            "<div class='dn-smalltext'>" + (s.calendarId ? 'cal ' + s.calendarShift : '') + "</div>";
    if (s.canEmitLinks)
    {
        res += "<div class='anchor arrow2' style='position: absolute; bottom: -16px; right: 5%; min-width: 20px;'>+</div>";
    }
    return res;
};

PanelChain.prototype.setJSPNode = function (node)
{
    // Not using classic anchors as source, as we want the links to be perimeter links
    if (node[0]._source.canEmitLinks)
    {
        this.jspInstance.makeSource(node, {
            maxConnections: 100,
            endpoint: 'Blank',
            anchor: ["Perimeter", {shape: "Rectangle"}],
            filter: "div.arrow2"
        });
    }

    if (node[0]._source.canReceiveLink)
    {
        this.jspInstance.makeTarget(node, {
            isTarget: true,
            maxConnections: 100,
            endpoint: 'Blank',
            allowLoopback: false,
            anchor: ["Perimeter", {shape: "Rectangle"}]
        });
    }

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

PanelChain.prototype.getTransitionLabel = function (dtoTransition)
{
    var res = "<div style='text-align: center;'>[" + dtoTransition.guard1 + "]";
    if (dtoTransition.calendarAware)
    {
        res += "<div>[same cal ocurrence]</div>";
    }
    res += "</div>";
    return res;
};

PanelChain.prototype.redrawSelectedTransition = function ()
{
    this.selectedTransitionConnector.setLabel(this.getTransitionLabel(this.selectedTransition));
};

PanelChain.prototype.deselectAll = function ()
{
    this.selectedStateDiv = null;
    this.selectedState = null;
    this.selectedTransition = null;
    this.selectedTransitionConnector = null;

    this.drawPanel.find('.drawingPanel-selected').removeClass('drawingPanel-selected');
    var t = this;
    $.each(this.jspInstance.getConnections(), function ()
    {
        this.setPaintStyle({fillStyle: t.connectorColor, strokeStyle: t.connectorColor});
    });
};

PanelChain.prototype.initMenu = function ()
{
    ///////////////////////////////////////////
    // State menu

    var m = this.tab.find('ul.statemenu');

    var grouplist = m.find("li.c-groups > ul");
    grouplist.empty();
    $.each(this.app.groups, function ()
    {
        if (this.name)
        {
            $("<li value='" + this.id + "'>" + this.name + "</li>").appendTo(grouplist);
        }
    });

    var calendarlist = m.find("li.c-cals > ul");
    calendarlist.empty();
    $("<li>none</li>").appendTo(calendarlist);
    $.each(this.app.calendars, function ()
    {
        if (this.name)
        {
            $("<li value='" + this.id + "'>" + this.name + "</li>").appendTo(calendarlist);
        }
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

    ///////////////////////////////////////////
    // Transition menu
    m = this.tab.find('ul.trmenu');
    m.menu();

    t.tab.find('input').change(function ()
    {
        t.selectedTransition.guard1 = $(this).val();
        t.redrawSelectedTransition();
    });
    m.find('li.c-calaware').click(function () {
        t.selectedTransition.calendarAware = !t.selectedTransition.calendarAware;
        t.redrawSelectedTransition();
    });
    m.find('li.c-remove').click(function () {
        t.chain.transitions.splice(t.chain.transitions.indexOf(t.selectedTransition), 1);
        t.initPanel();
    });

};

PanelChain.prototype.toggleMenu = function ()
{
    this.tab.find("ul.menu.statemenu > li.c-remove").addClass('ui-state-disabled');
    this.tab.find("ul.menu > li.c-para").addClass('ui-state-disabled');
    this.tab.find("ul.menu > li.c-cals").addClass('ui-state-disabled');
    this.tab.find("ul.menu > li.c-calshifts").addClass('ui-state-disabled');
    this.tab.find("ul.menu > li.c-groups").addClass('ui-state-disabled');

    this.tab.find("ul.menu.statemenu").show();
    this.tab.find("ul.menu.trmenu").hide();
    this.tab.find("ul.menu.trmenu").next().hide();

    if (!this.selectedState && !this.selectedTransition)
    {
        return;
    }

    if (this.selectedState)
    {
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
    }

    if (this.selectedTransition)
    {
        this.tab.find("ul.menu.statemenu").hide();
        this.tab.find("ul.menu.trmenu").show();
        this.tab.find("ul.menu.trmenu").next().show();

        this.tab.find("ul.menu.trmenu").next().val(this.selectedTransition.guard1);
    }
};