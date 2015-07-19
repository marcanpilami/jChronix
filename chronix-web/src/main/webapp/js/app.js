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
                console.debug(ui.newPanel);
                console.debug(i);
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
    this.panelId = "app-" + app.id + "-chain";
    this.panel = $('#' + this.panelId);
    this.chain = app.chains[0];
    this.app = app;

    this.jspInstance = jsPlumb.getInstance({
        Connector: ["Bezier", {curviness: 50}],
        DragOptions: {cursor: "pointer", zIndex: 2000},
        PaintStyle: {strokeStyle: "gray", lineWidth: 2},
        EndpointStyle: {radius: 9, fillStyle: "gray"},
        ConnectionOverlays: [['Arrow', {width: 12, location: 1}]],
        HoverPaintStyle: {strokeStyle: "#ec9f2e"},
        EndpointHoverStyle: {fillStyle: "#ec9f2e"},
        Container: this.panelId
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
        console.debug(e.val);
        var c;
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
    this.panel.on('click', '.dn', function ()
    {
        var node = $(this);
        node.toolbar({
            content: '#toolbar-options', //-' + app.id,
            event: 'click',
            hideOnClick: true
        });
    });
};

PanelChain.prototype.initPanel = function ()
{
    // Cleanup    
    this.jspInstance.reset();
    $("#" + this.panelId).find(".dn").remove();

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
    return "<div>" + s.label + "</div><div class='dn-smalltext'>" + s.runsOnName + (s.parallel ? ' //' : '') + (s.calendarId ? 'cal ' + s.calendarShift : '') +
            "</div>";
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
    d.appendTo($("#" + this.panelId));
    d.css('left', s.x);
    d.css('top', s.y);
    this.setJSPNode(d);
};