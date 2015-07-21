function PanelRec(app)
{
    this.app = app;
    this.tab = $("#app-rec-" + app.id);
    this.calendars = app.calendars;

    // Load the panel
    t = this;
    this.tab.load("rec.html", function ()
    {
        // Replace ids  
        t.tab.html(t.tab[0].innerHTML.replace(/IDIDID/g, app.id));

        // Init contents
    });
}

PanelRec.prototype.initPanel = function ()
{
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
            {data: 'id', title: 'ID'},
            {data: 'seq', title: 'Occurrence name'}          
        ]
    });
    console.debug(this.occurrences_table);
    
    var t = this;
    this.maintable  = new Handsontable(this.tab.find("#app-" + this.app.id + "-reclist")[0], {
        data: this.app.calendars,
        minSpareRows: 1,
        rowHeaders: false,
        colHeaders: true,
        contextMenu: false,
        manualColumnResize: true,
        manualRowResize: false,
        columns: [
            {data: 'id', title: 'ID'},
            {data: 'name', title: 'Name'},
            {data: 'description', title: 'Description'}           
        ],
        afterChange: initIdIfNone,
        afterSelectionEnd: function(event, col, line)
        {
            var cal = this.getSourceDataAtRow(line);
            t.occurrences_table.loadData(cal.days);
        }
    });
};