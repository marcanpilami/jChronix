
$(document).ready(function ()
{
    var network = $.getJSON("ws/meta/network", function (data)
    {
        network = data;
    });

    $('#history').DataTable({
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
        ajax: function (data, callback, settings)
        {
            console.debug(data);
            console.debug(settings);

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
                var places = [];
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
                            places.push(place);
                        }
                    });
                });
                console.debug(places);

                s4 = $("#place").select2({
                    data: network.places,
                    formatSelection: item2name,
                    formatResult: item2name,
                    closeOnSelect: true,
                    multiple: true,
                    placeholder: "select the places to run the job on",
                    allowClear: true,
                    initSelection: function (e, c) {
                        c(places);
                    },
                    matcher: nameMatcher
                });
            });
        });
    });

    var dialog = $("#dialog-newlaunch").dialog({
        autoOpen: false,
        height: 300,
        width: 350,
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

    dialog.dialog("open");
});

function oopLaunch()
{
    var place_ids = $("#place").select2("val");
    console.debug(place_ids);
    $.each(place_ids, function ()
    {
        var pid = this.toString();
        var url = "ws/live/order/launch/outofplan/" + $("#application").select2("val") + "/" + $("#state").select2("val") + "/" + pid;
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