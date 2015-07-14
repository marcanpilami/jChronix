var instance;

$(
        function ()
        {
            $('#tabs').tabs(
                    {
                        active: 0,
                        heightStyle: 'fill',
                    });
            
            
            
            instance = jsPlumb.getInstance({
                Connector: ["Bezier", {curviness: 50}],
                DragOptions: {cursor: "pointer", zIndex: 2000},
                PaintStyle: {strokeStyle: "gray", lineWidth: 2},
                EndpointStyle: {radius: 9, fillStyle: "gray"},
                HoverPaintStyle: {strokeStyle: "#ec9f2e"},
                EndpointHoverStyle: {fillStyle: "#ec9f2e"},
                Container: "node-c"
            });

            $('.execnode').each(function ()
            {
                instance.addEndpoint(this, {
                    uuid: this.id + "-bottom",
                    anchor: "Bottom",
                    maxConnections: -1,
                    isSource: true,
                });
                
                instance.addEndpoint(this, {
                    uuid: this.id + "-top",
                    anchor: "Top",
                    maxConnections: -1,
                    isTarget: true,
                });
            });

            instance.draggable($('.execnode'));
            //instance.repaintEverything();
        }
);