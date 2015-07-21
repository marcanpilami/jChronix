function initApp(uuid)
{
    var app;
    var content = $("#tab-" + uuid + " > div");
    var chainPanel;

    $.getJSON("ws/meta/app/id/" + uuid).done(function (data)
    {
        app = data;
        initCommand(app);
        chainPanel = new PanelChain(app);


    });

    content.load("app.html", function ()
    {
        // Replace ids  
        content.html(content[0].innerHTML.replace(/IDIDID/g, uuid));

        // Setup
        content.find(".tabs").tabs({
            activate: function (e, ui)
            {
                var i = ui.newPanel[0].id;
                if (i.indexOf("app-chain") === 0)
                {
                    chainPanel.initPanel();
                }
            }
        });

    });
}

function initCommand(app)
{
    var content = $("#app-command-" + app.id);
    var commands = app.shells;

    new Handsontable(content[0], {
        data: commands,
        minSpareRows: 1,
        rowHeaders: true,
        colHeaders: true,
        contextMenu: false,
        columns: [
            {data: 'id', title: 'ID'},
            {data: 'name', title: 'Name'},
            {data: 'description', title: 'Description'},
            {data: 'command', title: 'Command'}
        ],
        afterChange: initIdIfNone
    });
}