/* global uuid */

var tabs;
var network;
var apps_short;
var apps = new Object();
var panel_phynode, panel_places;

$(
        function ()
        {
            panel_phynode = new PanelPhyNode();
            panel_places = new PanelPlace();
            tabs = $('#tabs').tabs({
                //active: 0,
                heightStyle: 'fill',
                activate: function (e, ui)
                {
                    var i = ui.newPanel[0].id;
                    if (i.indexOf("environment") === 0)
                    {
                        initEnvironment();
                    }
                    if (i.indexOf("applications") === 0)
                    {
                        initAppChoice();
                    }
                }
            });
        }
);

function initEnvironment()
{
    if (network)
    {
        return;
    }

    $.getJSON("ws/meta/network").done(function (data)
    {
        network = data;
        tabs.find("#tabsenvt").tabs({
            activate: function (e, ui)
            {
                var i = ui.newPanel[0].id;
                if (i.indexOf("phynode") === 0)
                {
                    console.debug('rrrrrrrr');
                    panel_phynode.initPanel();
                }
                if (i.indexOf("place") === 0)
                {
                    panel_places.initPanel();
                }
            }
        });
        panel_phynode.initPanel();
    }).fail(function (o, status)
    {
        alert("failed to fetch network " + status);
    });

    $("#bt_saveenvt").click(function ()
    {
        // Work on a copy, as we are going to modify the data
        var n = JSON.parse(JSON.stringify(network));

        // Remove all empty lines added by the grids
        removeIfNoName(n.places);
        removeIfNoName(n.nodes);

        $.postJSON("ws/meta/network", n, null, function (jqXHR, errorType, exc) {
            alert(errorType);
        });
    });

    $("#bt_validateenvt").click(function ()
    {
        var errors = $("#envt_errors > tbody");

        // Clear errors
        errors.empty();

        // Work on a copy, as we are going to modify the data
        var n = JSON.parse(JSON.stringify(network));

        // Remove all empty lines added by the grids
        removeIfNoName(n.places);
        removeIfNoName(n.nodes);

        $.postJSON("ws/meta/network/test", n, function (data)
        {
            if (data.length === 0)
            {
                $("<tr><td></td><td>No errors detected!</td><td></td><td></td><td></td></tr>").appendTo(errors);
            }
            else
            {
                $.each(data, function ()
                {
                    $("<tr><td>" + this.itemType + "</td><td>" + this.errorMessage + "</td><td>" + this.errorPath + "</td><td>" +
                            this.itemIdentification + "</td><td>" + this.erroneousValue + "</td></tr>").appendTo(errors);
                });
            }
        }, function (jqXHR, errorType, exc) {
            alert(errorType);
        });
    });
}

function initIdIfNone(changes, action)
{
    if (!changes)
    {
        return;
    }
    var grid = this;
    $.each(changes, function ()
    {
        var n = grid.getSourceDataAtRow(this[0]);
        if (!n.id)
        {
            n.id = uuid.v4();
        }
    });
}

Number.prototype.zeroPad = function (numZeros)
{
    var n = Math.abs(this);
    var zeros = Math.max(0, numZeros - Math.floor(n).toString().length);
    var zeroString = Math.pow(10, zeros).toString().substr(1);
    if (this < 0) {
        zeroString = '-' + zeroString;
    }
    return zeroString + n;
};
function getTomorrowPlus(days)
{
    if (!days)
    {
        days = 0;
    }
    var d = new Date();
    d.setHours(0);
    d.setMinutes(0);
    d.setSeconds(0);
    d.setMilliseconds(0);
    d.setDate(d.getDate() + 1 + days);
    return d;
}

$.postJSON = function (url, data, callback, errorcallback) {
    return jQuery.ajax({
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
        },
        'type': 'POST',
        'url': url,
        'data': JSON.stringify(data),
        'dataType': 'json',
        'success': callback,
        'error': errorcallback
    });
};
function printStackTrace()
{
    var e = new Error('dummy');
    var stack = e.stack.replace(/^[^\(]+?[\n$]/gm, '')
            .replace(/^\s+at\s+/gm, '')
            .replace(/^Object.<anonymous>\s*\(/gm, '{anonymous}()@')
            .split('\n');
    console.log(stack);
}

function item2name(item)
{
    return item.name;
}

function removeIfNoName(collection)
{
    var toRemove = [];
    $.each(collection, function ()
    {
        if (!this.name)
        {
            toRemove.push(this);
        }
    });
    $.each(toRemove, function ()
    {
        collection.splice(collection.indexOf(this), 1);
    });
}