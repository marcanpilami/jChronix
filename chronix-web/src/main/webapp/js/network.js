/* global jsPlumb, network */

function PanelPhyNode()
{
    var t = this;
    this.network_panel_root = "node-c";
    this.selected_en = null;
    this.jspInstance = null;

    // Node selection & subsequent edit panel updates
    $("#" + this.network_panel_root).on('click', '.dn', function ()
    {
        var node = $(this);
        t.selected_en = node;

        $('.dn').removeClass('drawingPanel-selected');
        node.addClass('drawingPanel-selected');
        $("#pn-name").val(node[0]._source.name);
        $("#pn-qport").val(node[0]._source.qPort);
        $("#pn-httpport").val(node[0]._source.wsPort);
        $("#pn-engine").prop('checked', !node[0]._source.simpleRunner);
    });

    // Drop node from palette
    $('.paletteitem').draggable({helper: 'clone', appendTo: '#node-c'});
    $('#' + this.network_panel_root).droppable({
        drop: function (event, ui)
        {
            var node = {name: 'new node', dns: 'localhost', qPort: 1789, wsPort: 1790, simpleRunner: false};
            var n = getExecNodeDiv(node);
            n.css('left', ui.helper.css('left'));
            n.css('top', ui.helper.css('top'));
            n[0]._source = node;
            n.appendTo(this);
            setDN(n, t.jspInstance);
        }});

    // Edit panel changes
    $("#phynodes-details input").change(function ()
    {
        var n = t.selected_en[0]._source;
        n.name = $("#pn-name").val();
        n.qPort = $("#pn-qport").val();
        n.wsPort = $("#pn-httpport").val();
        n.simpleRunner = !$("#pn-engine").prop('checked');
        t.selected_en.html(getExecNodeContent(n));
    });
}


PanelPhyNode.prototype.initPanel = function ()
{
    if (this.jspInstance)
    {
        return;
    }

    var t = this;

    this.jspInstance = jsPlumb.getInstance({
        Connector: ["Bezier", {curviness: 50}],
        DragOptions: {cursor: "pointer", zIndex: 2000},
        PaintStyle: {strokeStyle: "gray", lineWidth: 2},
        EndpointStyle: {radius: 9, fillStyle: "gray"},
        ConnectionOverlays: [['Arrow', {width: 12, location: 1}]],
        HoverPaintStyle: {strokeStyle: "#ec9f2e"},
        EndpointHoverStyle: {fillStyle: "#ec9f2e"},
        Container: this.network_panel_root
    });

    $('.dn').each(function ()
    {
        setDN(this, t.jspInstance);
    });

    ////////////////////
    // Add nodes

    // First pass: draw nodes
    $.each(network.nodes, function ()
    {
        var d = getExecNodeDiv(this);
        d[0]._source = this;
        d.appendTo($("#node-c"));
        d.css('left', this.x);
        d.css('top', this.y);
        setDN(d, t.jspInstance);
    });
    // Second pass: links
    $.each(network.nodes, function ()
    {
        var source = this;
        $.each(source.toTCP, function ()
        {
            var target = this;
            t.jspInstance.connect({source: source.id, target: target.toString()});
        });
    });
};

function getExecNodeDiv(node)
{
    var d = $("<div id='" + node.id + "' class='dn execnode'>" + getExecNodeContent(node) + "</div>");
    if (node.console)
    {
        d.addClass('execnode-console');
    }
    return d;
}

function getExecNodeContent(node)
{
    return "<div>" + node.name + "</div><div class='dn-smalltext'>" + node.dns + ":" + node.qPort +
            "<div class='anchor arrow1' style='position: absolute; bottom: -16px; left: 5%;'>remote control</div>" +
            "<div class='anchor arrow2' style='position: absolute; bottom: -16px; right: 5%;'>channel</div>" +
            "</div>";
}

function setDN(node, jspInstance)
{
    // Not using classic anchors as source, as we want the links to be perimeter links
    jspInstance.makeSource(node, {
        maxConnections: 100,
        endpoint: 'Blank',
        anchor: ["Perimeter", {shape: "Rectangle"}],
        filter: "div.arrow1",
        connectorStyle: {strokeStyle: 'red'}
    });
    jspInstance.makeSource(node, {
        maxConnections: 100,
        endpoint: 'Blank',
        anchor: ["Perimeter", {shape: "Rectangle"}],
        filter: "div.arrow2",
        connectorStyle: {strokeStyle: 'blue'}
    });
    jspInstance.makeTarget(node, {
        isTarget: true,
        maxConnections: 100,
        endpoint: 'Blank',
        allowLoopback: false,
        anchor: ["Perimeter", {shape: "Rectangle"}]
    });
    jspInstance.draggable(node, {containment: "parent", filter: 'div.anchor', filterExclude: true});
}