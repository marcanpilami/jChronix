/* global tabs */

function initSave(app)
{
    var tab = $("#app-save-" + app.id);

    tab.find("button[name=test]").click(function ()
    {
        // Clear error tables
        tab.find("table > tbody").empty();

        var a = prepareApp(app);

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


    tab.find("button[name=stage]").click(function ()
    {
        var a = prepareApp(app);
        $.postJSON("ws/meta/app", a, null, function (jqXHR, errorType, exc)
        {
            alert("an error occured while staging the application: " + errorType);
        });
    });

    tab.find("button[name=activate]").click(function ()
    {
        var a = prepareApp(app);

        $.postJSON("ws/meta/app/test", a, function (data)
        {
            if (data.length !== 0)
            {
                alert("Cannot activate an invalid application. Correct the errors then retry.");
                return;
            }

            $.postJSON("ws/meta/liveapp", a, null, function (jqXHR, errorType, exc)
            {
                alert("an error occured while storing and sending the application: " + errorType);
            });
        });
    });

    tab.find("button[name=unstage]").click(function ()
    {
        var a = prepareApp(app);
        $.postJSON("ws/meta/xappunstage", a,
                function ()
                {
                    var apptab = $("#tabs > ul > li#tabhead-" + app.id);
                    var appdiv = $("#tabs > div#tab-" + app.id);
                    var allappstab = $("#tabs > ul > li#appstab");
                    console.debug(allappstab.index());
                    tabs.tabs("option", "active", allappstab.index());
                    apptab.remove();
                    appdiv.remove();
                    tabs.tabs("refresh");
                },
                function (jqXHR, errorType, exc)
                {
                    alert("an error occured while unstaging the application: " + errorType);
                });
    });
}

function prepareApp(appToPrepare)
{
    // Work on a copy, as we are going to modify the data
    var a = JSON.parse(JSON.stringify(appToPrepare));

    // Remove all empty lines added by the grids
    removeIfNoName(a.shells);
    removeIfNoName(a.externals);
    removeIfNoName(a.clocks);
    removeIfNoName(a.rrules);
    removeIfNoName(a.calendars);
    removeIfNoName(a.groups);

    return a;
}

