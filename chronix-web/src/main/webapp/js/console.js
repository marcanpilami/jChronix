var dialog;

$(document).ready(function ()
{
    var network = $.getJSON("ws/meta/network", function (data)
    {
        network = data;
    });

    $('#history').DataTable({
        dom: 'Bfrtip',
        columns: [
            {data: 'id', title: 'id', orderable: false},
            {data: 'activeNodeName', title: 'Item'},
            {data: 'chainName', title: 'inside chain'},
            {data: 'lastKnownStatus', title: 'status'},
            {data: 'markedForRunAt', title: 'enqueued', type: 'date'}, // 2015-08-08T10:20:04.903+02:00
            {data: 'stoppedRunningAt', title: 'ended', type: 'date', orderable: false}
        ],
        order: [[1, "asc"]],
        serverSide: true,
        ajax: getRunLogs,
        select: "single",
        //buttons: ['refresh', 'stop', 'new']
        buttons: ["refresh", "copy", "excel", "getLogFile", "stopRunningJob", "openOOPL"]
    });


    ////////////////////////////////////////
    // OOPL

    var latestapp = null;
    var latestchain = null;
    var lateststate = null;
    var s1 = null;
    var s2 = null;
    var s3 = null;
    var s4 = null;


    var s1 = $("#application").select2({
        ajax: {
            url: 'ws/meta/app',
            dataType: 'json',
            cache: true,
            results: function (data, page)
            {
                return {results: data};
            }
        },
        formatSelection: item2name,
        formatResult: item2name,
        matcher: nameMatcher
    });


    s1.on('change', function ()
    {
        var app_id = $(this).val();

        latestapp = null;
        if (s4 !== null)
        {
            s4.off();
            s4.select2('destroy');
        }
        if (s3 !== null)
        {
            s3.off();
            s3.select2('destroy');
        }
        if (s2 !== null)
        {
            s2.off();
            s2.select2('destroy');
        }

        s2 = $("#chain").select2({
            ajax: {
                url: 'ws/meta/app/' + app_id,
                dataType: 'json',
                cache: true,
                results: function (data, page)
                {
                    latestapp = data;
                    return {results: [{name: 'Plans', children: data.plans}, {name: 'Chains', children: data.chains}]};
                }
            },
            formatSelection: item2name,
            formatResult: item2name,
            matcher: nameMatcher
        });


        s2.on('change', function ()
        {
            latestchain = $(this).select2('data');

            if (s3 !== null)
            {
                s3.off();
                s3.select2('destroy');
            }
            if (s4 !== null)
            {
                s4.off();
                s4.select2('destroy');
            }

            s3 = $("#state").select2({
                data: latestchain.states,
                formatSelection: item2label,
                formatResult: item2label,
                matcher: nameMatcher
            });

            s3.on('change', function ()
            {
                lateststate = $(this).select2('data');
                var group = null;
                var selectedPlaces = [];
                $.each(latestapp.groups, function ()
                {
                    if (this.id.toString() === lateststate.runsOnId)
                    {
                        group = this;
                    }
                });
                $.each(network.places, function ()
                {
                    var place = this;
                    $.each(place.memberOf, function ()
                    {
                        if (this.toString() === group.id)
                        {
                            selectedPlaces.push(place);
                        }
                    });
                });

                s4 = $("#place").select2({
                    data: network.places,
                    formatSelection: item2name,
                    formatResult: item2name,
                    closeOnSelect: true,
                    multiple: true,
                    placeholder: "select the places to run the job on",
                    initSelection: function (e, c) {
                        c(selectedPlaces);
                    },
                    matcher: nameMatcher
                });
                s4.select2("val", selectedPlaces);
            });
        });
    });

    dialog = $("#dialog-newlaunch").dialog({
        autoOpen: false,
        maxWidth: 600,
        maxHeight: 500,
        width: 600,
        height: 500,
        modal: true,
        buttons: {
            Launch: oopLaunch,
            Cancel: function () {
                dialog.dialog("close");
            }
        },
        close: function ()
        {
        }
    });
});


function oopLaunch()
{
    var place_ids = $("#place").select2("val");
    $.each(place_ids, function ()
    {
        var pid = this.toString();
        var url = "ws/live/order/launch/" + $("inside").is(':checked') + "/" + $("#application").select2("val") + "/" + $("#state").select2("val") + "/" + pid;
        $.getJSON(url, function (data)
        {
            console.debug(data);
        }, function ()
        {
            alert("error");
        });
    });
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

function item2name(item)
{
    return item.name;
}

function item2label(item)
{
    return item.label;
}

function nameMatcher(term, text, option)
{
    return option.name.toUpperCase().indexOf(term.toUpperCase()) >= 0;
}

function getRunLogs(data, callback, settings)
{
    var sorts = [];
    $.each(data.order, function ()
    {
        sorts.push({col: data.columns[this.column].data, order: this.dir.toUpperCase()});
    });

    $.postJSON(
            'ws/live/log',
            {
                pageSize: data.length,
                startLine: data.start,
                sortby: sorts
            },
    function (serverdata)
    {
        var res = {
            draw: data.draw,
            recordsTotal: serverdata.totalLogs,
            recordsFiltered: serverdata.totalLogs, // No filter for now
            data: serverdata.res
        };
        callback(res);
    },
            function (error)
            {
                alert("could not fetch data");
            });
}

$.fn.dataTable.ext.buttons.refresh = {
    text: 'Reload',
    action: function (e, dt, node, config) {
        dt.ajax.reload();
    }
};

$.fn.dataTable.ext.buttons.openOOPL = {
    text: 'New immediate additional launch',
    action: function (e, dt, node, config)
    {
        dialog.dialog("open");
    }
};

$.fn.dataTable.ext.buttons.getLogFile = {
    text: 'Get log file',
    action: function (e, dt, node, config)
    {
        alert('houba log');
    },
    init: function (dt, node, config)
    {
        var button = this;
        dt.on("select.dt.DT deselect.dt.DT", function ()
        {
            var b = dt.rows({selected: !0}).flatten().length + dt.columns({selected: !0}).flatten().length + dt.cells({selected: !0}).flatten().length;
            if (1 === b)
            {
                var row = dt.rows({selected: true}).data()[0];
                button.enable(row.stoppedRunningAt !== null);
            }
        });
        button.disable();
    }
};

$.fn.dataTable.ext.buttons.stopRunningJob = {
    text: 'Kill job',
    action: function (e, dt, node, config)
    {
        alert('houba stop');
    },
    init: function (dt, node, config)
    {
        var button = this;
        dt.on("select.dt.DT deselect.dt.DT", function ()
        {
            var b = dt.rows({selected: !0}).flatten().length + dt.columns({selected: !0}).flatten().length + dt.cells({selected: !0}).flatten().length;
            if (1 === b)
            {
                var row = dt.rows({selected: true}).data()[0];
                button.enable(row.lastKnownStatus === "RUNNING");
            }
        });
        button.disable();
    }
};