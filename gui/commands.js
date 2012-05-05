var grid = null;

function initCommandPanel(listLikeDTOShellCommand) {
	var options = {
		editable : true,
		enableAddRow : true,
		enableCellNavigation : true,
		enableColumnReorder : true,
		enableRowReordering : false,
		asyncEditorLoading : false,
		showHeaderRow : false, // Weird
		multiSelect : false,
		enableTextSelectionOnCells : false, // ???
		rowHeight : 30,
		autoHeight : true,
		autoEdit : false
	};

	var columns = [ {
		id : "name",
		name : "Command short name",
		field : "_name",
		width : 200,
		cssClass : "cell-title",
		editor : Slick.Editors.Text
	}, {
		id : "description",
		name : "Description",
		field : "_description",
		width : 250,
		selectable : false,
		resizable : false,
		editor : Slick.Editors.LongText
	}, {
		id : "cmde",
		name : "Shell command line to run",
		field : "_command",
		width : 600,
		cssClass : "cell-title",
		editor : Slick.Editors.Text
	} ];

	grid = new Slick.Grid("#gridCommands", listLikeDTOShellCommand, columns,
			options);

	grid.onAddNewRow.subscribe(onNewRow);

}
var de, dargs
function onNewRow(e, args) {
	de = e;
	dargs = args;
	
	var v = new dto_chronix_oxymores_org_DTOShellCommand();
	for(var o in args.item){
		v[o] = args.item[o];
	}
	addShell(v);
	grid.updateRowCount();
	grid.invalidateRow(grid.getData().length - 1);
	grid.render();
}
