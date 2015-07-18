var instance;

var en_template = "<div></div>";
var selected_en;

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

function initNetwork()
{
    var network_panel_root = "node-c";

    var jspInstance = jsPlumb.getInstance({
        Connector: ["Bezier", {curviness: 50}],
        DragOptions: {cursor: "pointer", zIndex: 2000},
        PaintStyle: {strokeStyle: "gray", lineWidth: 2},
        EndpointStyle: {radius: 9, fillStyle: "gray"},
        ConnectionOverlays: [['Arrow', {width: 12, location: 1}]],
        HoverPaintStyle: {strokeStyle: "#ec9f2e"},
        EndpointHoverStyle: {fillStyle: "#ec9f2e"},
        Container: network_panel_root
    });

    $('.dn').each(function ()
    {
        setDN(this, jspInstance);
    });

    $('#' + network_panel_root).droppable({drop: function (event, ui) {
            var t = ui.helper.clone();
            t.appendTo(this);
            setDN(t, instance);
        }});

    $("#" + network_panel_root).on('click', '.dn', function ()
    {
        var node = $(this);
        selected_en = this;
        $('.dn').removeClass('drawingPanel-selected');
        node.addClass('drawingPanel-selected');
        $("#pn-name").val(node[0]._source.name);
        $("#pn-qport").val(node[0]._source.qPort);
        $("#pn-httpport").val(node[0]._source.wsPort);
        $("#pn-engine").prop('checked', !node[0]._source.simpleRunner);
    });

    $("#phynodes-details input").change(function ()
    {
        var n = selected_en._source;
        n.name = $("#pn-name").val();
        n.qPort = $("#pn-qport").val();
        n.wsPort = $("#pn-httpport").val();
        n.simpleRunner = !$("#pn-engine").prop('checked');
        $(selected_en).html(getExecNodeContent(n));
    });

    $.getJSON("ws/meta/network").done(function (data)
    {
        // First pass: draw nodes
        $.each(data.nodes, function ()
        {
            console.debug(this);
            var d = getExecNodeDiv(this);
            d[0]._source = this;
            d.appendTo($("#node-c"));
            d.css('left', this.x);
            d.css('top', this.y);
            setDN(d, jspInstance);
        });

        // Second pass: links
        $.each(data.nodes, function ()
        {
            var source = this;
            $.each(this.toTCP, function ()
            {
                jspInstance.connect({source: source.id, target: this.toString()});
            });
        });

    }).fail(function (o, status)
    {
        alert("failed to fetch network " + status);
    });
}

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
            "<div class='anchor arrow1' style='position: absolute; bottom: -15px; left: 10%;'>remote control</div>" +
            "<div class='anchor arrow2' style='position: absolute; bottom: -15px; left: 70%;'>channel</div>" +
            "</div>";
}

$(
        function ()
        {
            $('#tabs').tabs({
                active: 0,
                heightStyle: 'fill'
            });

            initNetwork();
        }
);