/* global uuid */

function initApp(appObject)
{
    var app = appObject;
    var content = $("#tab-" + app.id + " > div");
    var chainPanel = null;
    var seqPanel = null;
    var clockPanel = null;
    var external = false;
    var groups = false;
    var save = false;

    content.load("app.html", function ()
    {
        // Replace ids
        content.html(content[0].innerHTML.replace(/IDIDID/g, app.id));

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
                    //initCommand(app); // Only init once!
                }
                if (i.indexOf("app-external") === 0)
                {
                    if (!external)
                    {
                        initExternal(app); // only init once simple panels
                        external = true;
                    }
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
                    if (!groups)
                    {
                        initGroup(app);
                        groups = true;
                    }
                }
                if (i.indexOf("app-save") === 0)
                {
                    if (!save)
                    {
                        initSave(app); // only init once simple panels
                        save = true;
                    }
                }
            },
            disabled: [5]
        });

        // Inits requiring both tabs + app data
        chainPanel = new PanelChain(app);
        seqPanel = new PanelRec(app);
        clockPanel = new PanelClock(app);

        // Open first tab
        initCommand(app);

        // Name and descr
        $("#name-" + app.id).val(app.name);
        $("#description-" + app.id).val(app.description);
        $("#name-" + app.id).change(function ()
        {
            app.name = $(this).val();
            $("a[href=#tab-" + app.id + "]").text(app.name);
            $("div#tabs").tabs("refresh");
        });
        $("#description-" + app.id).change(function ()
        {
            app.description = $(this).val();
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
        contextMenu: ['remove_row', 'undo', 'redo'],
        manualColumnResize: true,
        manualRowResize: false,
        colWidths: [150, 300],
        stretchH: 'last',
        multiSelect: false,
        columns: [
            //{data: 'id', title: 'ID'},
            {data: 'name', title: 'Name'},
            {data: 'description', title: 'Description'},
            {data: 'command', title: 'Command'}
        ],
        dataSchema: function ()
        {
            return {id: uuid.v4(), name: null, command: null, description: null};
        },
        beforeRemoveRow: function (row)
        {
            var c = this.getSourceDataAtRow(row);
            removeShell(app, c.id);
        }
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
        contextMenu: ['remove_row', 'undo', 'redo'],
        manualColumnResize: true,
        manualRowResize: false,
        colWidths: [150, 300, 200],
        stretchH: 'last',
        multiSelect: false,
        columns: [
            //{data: 'id', title: 'ID'},
            {data: 'name', title: 'Name'},
            {data: 'description', title: 'Description'},
            {data: 'accountRestriction', title: 'Optional shared secret'},
            {data: 'regularExpression', title: 'Calendar occurrence extractor (regular expression)'}
        ],
        dataSchema: function ()
        {
            return {id: uuid.v4(), name: null, description: null, accountRestriction: null, machineRestriction: null, regularExpression: null};
        },
        beforeRemoveRow: function (row)
        {
            var c = this.getSourceDataAtRow(row);
            removeExternal(app, c.id);
        }
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
        colWidths: [150, 300, 200],
        stretchH: 'last',
        multiSelect: false,
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
            removeGroup(app, c.id);
        }
    });
}