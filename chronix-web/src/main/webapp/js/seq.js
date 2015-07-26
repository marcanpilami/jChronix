/* global uuid */

function PanelRec(app)
{
    this.app = app;
    this.tab = $("#app-seq-" + app.id);
    this.calendars = app.calendars;
}

PanelRec.prototype.initPanel = function ()
{
    var t = this;
    this.tab.find("#app-" + this.app.id + "-reclist").empty();

    this.occurrences_table = new Handsontable(this.tab.find("#app-" + this.app.id + "-occlist")[0], {
        data: null,
        minSpareRows: 1,
        rowHeaders: false,
        colHeaders: true,
        contextMenu: false,
        manualColumnResize: true,
        manualRowResize: false,
        columns: [
            //{data: 'id', title: 'ID'},
            {data: 'seq', title: 'Occurrence name'}
        ],
        dataSchema: function ()
        {
            return {id: uuid.v4(), seq: null};
        }
    });

    this.maintable = new Handsontable(this.tab.find("#app-" + this.app.id + "-reclist")[0], {
        data: this.app.calendars,
        minSpareRows: 1,
        rowHeaders: false,
        colHeaders: true,
        contextMenu: false,
        manualColumnResize: true,
        manualRowResize: false,
        columns: [
            //{data: 'id', title: 'ID'},
            {data: 'name', title: 'Name'},
            {data: 'description', title: 'Description'}
        ],
        dataSchema: function ()
        {
            return {id: uuid.v4(), name: null, description: null, days: []};
        },
        afterSelectionEnd: function (event, col, line)
        {
            var cal = this.getSourceDataAtRow(line);
            t.occurrences_table.loadData(cal.days);
        }
    });
};