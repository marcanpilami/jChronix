/* global network, item2name, apps_short, item2id, nameMatcher */

function PanelDeployment()
{
    this.appList = $("div#deploy-apps");
    this.groupList = $("div#deploy-groups");
    this.placeList = $("div#deploy-places");

}

PanelDeployment.prototype.initPanel = function ()
{
    var t = this;

    if (t.s3 !== undefined)
    {
        t.s3.off();
        t.s3.select2('destroy');
    }
    if (t.s2 !== undefined)
    {
        t.s2.off();
        t.s2.select2('destroy');
    }
    if (t.s1 !== undefined)
    {
        t.s1.off();
        t.s1.select2('destroy');
    }

    this.s1 = this.appList.select2({
        data: apps_short,
        formatSelection: item2name,
        formatResult: item2name,
        placeholder: "select the application to deploy",
        allowClear: true,
        matcher: nameMatcher
    });
    this.s1.select2('val', null);
    this.s2 = null;
    this.s3 = null;

    this.s1.on('change', function ()
    {
        if (t.s3 !== null)
        {
            t.s3.off();
            t.s3.select2('destroy');
        }
        if (t.s2 !== null)
        {
            t.s2.off();
            t.s2.select2('destroy');
        }

        $.getJSON("ws/meta/app/" + t.s1.val()).done(function (data)
        {
            var app = data;
            t.s2 = t.groupList.select2({
                data: app.groups,
                formatSelection: item2name,
                formatResult: item2name,
                matcher: nameMatcher
            });
            t.s2.select2('val', null);

            t.s2.on('change', function (e)
            {
                var group = $(this).select2('data');
                var selected = [];
                var cleanPlaces = JSON.parse(JSON.stringify(network.places));
                removeIfNoName(cleanPlaces);

                $.each(cleanPlaces, function ()
                {
                    var place = this;
                    $.each(place.memberOf, function ()
                    {
                        if (this.toString() === group.id)
                        {
                            selected.push(place);
                        }
                    });
                });

                if (t.s3 !== null)
                {
                    t.s3.off();
                    t.s3.select2('destroy');
                }

                t.s3 = t.placeList.select2({
                    data: cleanPlaces,
                    formatSelection: item2name,
                    formatResult: item2name,
                    closeOnSelect: false,
                    multiple: true,
                    placeholder: "select the places to include in the group",
                    allowClear: true,
                    initSelection: function (e, c) {
                        c(selected);
                    },
                    matcher: nameMatcher
                });
                t.s3.select2("val", selected);

                t.s3.on('change', function ()
                {
                    // Changes are on the real places, not the clean ones.
                    $.each(network.places, function ()
                    {
                        if (this.memberOf && this.memberOf.indexOf(group.id) !== -1)
                        {
                            this.memberOf.splice(this.memberOf.indexOf(group.id), 1);
                        }
                    });

                    $.each($(this).select2('val'), function ()
                    {
                        var p = this;
                        $.each(network.places, function ()
                        {
                            if (this.id === p.toString())
                            {
                                this.memberOf.push(group.id);
                            }
                        });
                    });
                });
            });
        });

    });
};