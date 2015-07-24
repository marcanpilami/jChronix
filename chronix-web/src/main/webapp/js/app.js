function initApp(uuid)
{
    var app;
    var content = $("#tab-" + uuid + " > div");
    var chainPanel = null;
    var seqPanel = null;
    var clockPanel = null;

    $.getJSON("ws/meta/app/id/" + uuid).done(function (data)
    {
        app = data;

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
                    if (i.indexOf("app-command") === 0)
                    {
                        initCommand(app);
                    }
                    if (i.indexOf("app-external") === 0)
                    {
                        initExternal(app);
                    }
                    if (i.indexOf("app-seq") === 0)
                    {
                        seqPanel.initPanel();
                    }
                    if (i.indexOf("app-clock") === 0)
                    {
                        clockPanel.initPanel();
                    }
                    if (i.indexOf("app-group") === 0)
                    {
                        initGroup(app);
                    }
                }
            });

            // Inits requiring both tabs + app data
            chainPanel = new PanelChain(app);
            seqPanel = new PanelRec(app);
            clockPanel = new PanelClock(app);

            // Open first tab
            initCommand(app);
        });
    });
}

function initCommand(app)
{
    var content = $("#app-command-" + app.id);
    var commands = app.shells;

    content.empty();
    new Handsontable(content[0], {
        data: commands,
        minSpareRows: 1,
        rowHeaders: false,
        colHeaders: true,
        contextMenu: false,
        manualColumnResize: true,
        manualRowResize: false,
        columns: [
            {data: 'id', title: 'ID'},
            {data: 'name', title: 'Name'},
            {data: 'description', title: 'Description'},
            {data: 'command', title: 'Command'}
        ],
        afterChange: initIdIfNone
    });
}

function initExternal(app)
{
    var content = $("#app-external-" + app.id);
    var externals = app.externals;

    content.empty();
    new Handsontable(content[0], {
        data: externals,
        minSpareRows: 1,
        rowHeaders: false,
        colHeaders: true,
        contextMenu: false,
        manualColumnResize: true,
        manualRowResize: false,
        columns: [
            {data: 'id', title: 'ID'},
            {data: 'name', title: 'Name'},
            {data: 'description', title: 'Description'},
            {data: 'accountRestriction', title: 'Optional shared secret'},
            {data: 'regularExpression', title: 'Calendar occurrence extractor (regular expression)'}
        ],
        afterChange: initIdIfNone
    });
}

function initGroup(app)
{
    var content = $("#app-group-" + app.id);
    var groups = app.groups;

    content.empty();
    new Handsontable(content[0], {
        data: groups,
        minSpareRows: 1,
        rowHeaders: true,
        colHeaders: true,
        contextMenu: ['remove_row', 'undo', 'redo'],
        manualColumnResize: true,
        manualRowResize: false,
        columns: [
            {data: 'name', title: 'Name'},
            {data: 'description', title: 'Description'}
        ],
        dataSchema: function () {
            var res = new Object();
            res.id = uuid.v4();
            return res;
        },
        beforeRemoveRow: function (row)
        {
            var c = this.getSourceDataAtRow(row);
            $.each(network.places, function ()
            {
                var place = this;
                $.each(place.memberOf, function ()
                {
                    place.memberOf.splice(place.memberOf.indexOf(c.id), 1);
                });
            });
        }
    });
}