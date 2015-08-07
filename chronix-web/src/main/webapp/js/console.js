
$(document).ready(function ()
{
    $('#history').DataTable({
        columns: [
            {data: 'id', title: 'id'},
            {data: 'activeNodeName', title: 'Item'},
            {data: 'chainName', title: 'inside chain'},
            {data: 'lastKnownStatus', title: 'status'},
            {data: 'markedForRunAt', title: 'enqueued'},
            {data: 'stoppedRunningAt', title: 'ended'}
        ],
        serverSide: true,
        ajax: function (data, callback, settings)
        {
            console.debug(data);
            console.debug(settings);

            $.postJSON(
                    'ws/live/logs',
                    {},
                    function (serverdata)
                    {
                        var res = {
                            draw: data.draw,
                            recordsTotal: -1,
                            recordsFiltered: -1,
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
});


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
