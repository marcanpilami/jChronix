var instance;

function setDN(node, jspInstance)
{
    // Not using classic anchors as source, as we want the links to be perimeter links
    var anch1 = $("<div class='anchor arrow1' style='position: absolute; bottom: -15px; left: 10%;'>remote control</div>");
    anch1.appendTo(node);
    var anch2 = $("<div class='anchor arrow2' style='position: absolute; bottom: -15px; left: 70%;'>channel</div>");
    anch2.appendTo(node);

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
    var instance = jsPlumb.getInstance({
        Connector: ["Bezier", {curviness: 50}],
        DragOptions: {cursor: "pointer", zIndex: 2000},
        PaintStyle: {strokeStyle: "gray", lineWidth: 2},
        EndpointStyle: {radius: 9, fillStyle: "gray"},
        ConnectionOverlays: [['Arrow', {width: 12, location: 1}]],
        HoverPaintStyle: {strokeStyle: "#ec9f2e"},
        EndpointHoverStyle: {fillStyle: "#ec9f2e"},
        Container: "node-c"
    });

    $('.dn').each(function ()
    {
        setDN(this, instance);
    });

    $('#node-c').droppable({drop: function (event, ui) {
            var t = ui.helper.clone();
            t.appendTo(this);
            setDN(t, instance);
        }});
    $('.paletteitem').draggable({helper: 'clone', appendTo: '#node-c'});

    $('.dn').click(function ()
    {
        $('.dn').removeClass('drawingPanel-selected');
        $(this).addClass('drawingPanel-selected');
    });
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