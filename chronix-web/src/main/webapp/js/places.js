/* global network, uuid */

function PanelPlace()
{
    this.table = null;
}

PanelPlace.prototype.initPanel = function ()
{
    if (this.table)
    {
        return;
    }

    this.table = new Handsontable($('#pl-table')[0], {
        data: network.places,
        minSpareRows: 1,
        rowHeaders: true,
        colHeaders: true,
        contextMenu: true,
        columns: [
            //{data: 'id', title: 'ID'},
            {data: 'name', title: 'Name'},
            {data: 'nodeid', title: 'Node',
                editor: 'select2',
                renderer: nameIdRenderer,
                select2Options: {
                    data: network.nodes,
                    dropdownAutoWidth: true,
                    allowClear: true,
                    width: 'resolve',
                    id: 'id',
                    formatSelection: function (item) {
                        return item.name;
                    },
                    formatResult: function (item) {
                        return item.name;
                    }
                }
            }
        ],
        dataSchema: function ()
        {
            return {id: uuid.v4(), name: null, description: null};
        }
    });
}

function nameIdRenderer(instance, td, row, col, prop, value, cellProperties)
{
    $.each(network.nodes, function ()
    {
        if (this.id === value)
        {
            td.innerHTML = this.name;
            return td;
        }

        return "unknown";
    });
}