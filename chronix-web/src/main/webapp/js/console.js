
$(document).ready(function ()
{
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
