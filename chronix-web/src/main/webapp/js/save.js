function initSave(app)
{
    var tab = $("#app-save-" + app.id);

    tab.find("button[name=test]").click(function ()
    {
        // Clear error tables
        tab.find("table > tbody").empty();

        // Work on a copy, as we are going to modify the data
        var a = JSON.parse(JSON.stringify(app));

        // Remove all empty lines added by the grids
        removeIfNoName(a.shells);
        removeIfNoName(a.externals);
        removeIfNoName(a.clocks);
        removeIfNoName(a.rrules);
        removeIfNoName(a.calendars);
        removeIfNoName(a.groups);

        // Now test for real...
        $.postJSON("ws/meta/app/test", a, function (data)
        {
            if (data.length === 0)
            {
                $("<tr><td></td><td>No errors detected!</td><td></td><td></td><td></td></tr>").appendTo(tab.find("table > tbody"));
            }
            else
            {
                $.each(data, function ()
                {
                    $("<tr><td>" + this.itemType + "</td><td>" + this.errorMessage + "</td><td>" + this.errorPath + "</td><td>" +
                            this.itemIdentification + "</td><td>" + this.erroneousValue + "</td></tr>").appendTo(tab.find("table > tbody"));
                });
            }
        },
                function (jqXHR, errorType, exc)
                {
                    alert("an error occured while validating the application: " + errorType);
                });
    });
}