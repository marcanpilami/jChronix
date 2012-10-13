
var ArborRaphaelRenderer = function(){
	var rpaper = null;
	var particleSystem = null;
	
	var that = {
		init:function(system){   
			rpaper = new Raphael("raphael-pnro", 1000, 500);  
			rpaper.clear();  
			  
			particleSystem = system;
			particleSystem.screenSize(rpaper.width, rpaper.height); 
			particleSystem.screenPadding(80); // leave an extra 80px of whitespace per side
		},
		  
		redraw:function(){
			rpaper.clear();  
			particleSystem.eachEdge(function(edge, pt1, pt2){
				// edge: {source:Node, target:Node, length:#, data:{}}
				// pt1: {x:#, y:#} source position in screen coords
				// pt2: {x:#, y:#} target position in screen coords
				
				// draw a line from pt1 to pt2
				var pc = "M" + pt1.x + "," + pt1.y + ",L" + pt2.x + "," + pt2.y;
				var shifted1 = Raphael.getPointAtLength(pc, nodeSize);
				var shifted2 = Raphael.getPointAtLength(pc, Raphael.getTotalLength(pc) - nodeSize);
				
				var arrow = rpaper.path("M" + shifted1.x + "," + shifted1.y + ",L" + shifted2.x + "," + shifted2.y);
				arrow.attr({
					"arrow-end" : "classic-wide-long",
					"stroke-width" : 5});	
				
				if (edge.data === "TCP")
					arrow.attr({"stroke" : 'blue'});
			});
			
			particleSystem.eachNode(function(node, pt){
				// node: {mass:#, p:{x,y}, name:"", data:{}}
				// pt: {x:#, y:#} node position in screen coords
				
				var rnode = rpaper.circle(pt.x, pt.y, 50);
				rnode.attr({
					fill : 'thistle',
					stroke : 'darkorchid',
					'stroke-width' : 2});
				if (node.data._console)
					rnode.attr({"fill" : 'yellow'});
				
				var txt = node.data._dns + ":" + node.data._qPort + "\n" + node.data._osusername;
				rpaper.text(pt.x, pt.y, txt);
			});    			
		},
	};
	
	return that;
};    

function initNetworkROPanel()
{
	sys = arbor.ParticleSystem({friction:.5, stiffness:100, repulsion:2000});
	sys.renderer = ArborRaphaelRenderer();
	
	var aa = cxfApplication.getNodes().getDTOExecutionNode();
	for ( var i = 0; i < aa.length; i++) {
		var cxfNode = aa[i];
		
		sys.addNode(cxfNode._id, cxfNode);
		
		var ss = cxfNode.getToTCP().getString();
		for(var j = 0; j < ss.length; j++) {
			sys.addEdge(cxfNode._id, ss[j], "TCP");
		}
	}
}