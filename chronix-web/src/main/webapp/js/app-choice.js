
function initAppChoice()
{
    $('#ac-table').empty();

    $.getJSON("ws/meta/app").done(function (data)
    {
        apps_short = data;

        var appstab = new Handsontable($('#ac-table')[0], {
            data: apps_short,
            minSpareRows: 0,
            rowHeaders: true,
            colHeaders: true,
            contextMenu: false,
            currentRowClassName: 'selected-table-row',
            multiSelect: false,
            columns: [
                //{data: 'id', title: 'ID'},
                {data: 'name', title: 'Name'},
                {data: 'description', title: 'Description'},
                {data: 'version', title: 'Version', readOnly: true},
                {data: 'draft', title: 'Existing draft', readOnly: true},
                {data: 'latestSave', title: 'Latest save on', readOnly: true, type: 'date', dateFormat: 'MM/DD/YYYY', correctFormat: true}
            ]
        });

        $('#ac-table').on('dblclick', 'tbody tr th', function (event)
        {
            var a = appstab.getSourceDataAtRow($(this).text() - 1);
            openAppTab(a);
        });
    });
}

function openAppTab(a)
{
    var exists = $("#tabs > ul > li[id=tabhead-" + a.id);
    if (exists.length > 0)
    {
        $("#tabs").tabs("option", "active", exists.index());
    }
    else
    {
        var t = "<li id='tabhead-" + a.id + "' ><a href='#tab-" + a.id + "'>" + a.name + "</a></li>";
        $(t).appendTo($("#tabs > ul"));
        $("<div id='tab-" + a.id + "'><div></div></div>").appendTo($("#tabs"));
        $("div#tabs").tabs("refresh");
        $("div#tabs").tabs("option", "active", $("#tabhead-" + a.id).index());
        initApp(a.id);
    }
}