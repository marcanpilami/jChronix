
function initPlaces()
{
    new Handsontable($('#pl-table')[0], {
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
        afterChange: function (changes, action)
        {
            if (!changes)
            {
                return;
            }
            var grid = this;
            $.each(changes, function ()
            {
                var n = grid.getSourceDataAtRow(this[0]);
                if (!n.id)
                {
                    n.id = uuid.v4();
                }
            });
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