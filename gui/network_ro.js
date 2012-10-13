var places = new Object();

var ArborRaphaelRenderer = function(){
	var rpaper = null;
	var particleSystem = null;
	var latestExpanded = null;
	
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
				if (edge.data["internal"] != null)
					shifted2 = Raphael.getPointAtLength(pc, Raphael.getTotalLength(pc) - 20);
				
				var arrow = rpaper.path("M" + shifted1.x + "," + shifted1.y + ",L" + shifted2.x + "," + shifted2.y);
				arrow.attr({
					"arrow-end" : "classic-wide-long",
					"stroke-width" : 5});	
				if (edge.data["internal"] != null)
					arrow.attr({"stroke-dasharray" : "-","stroke-width" : 2,});
				
				if (edge.data === "TCP")
					arrow.attr({"stroke" : 'blue'});
			});
			
			particleSystem.eachNode(function(node, pt){
				// node: {mass:#, p:{x,y}, name:"", data:{}}
				// pt: {x:#, y:#} node position in screen coords
				
				var rnode = null;
				
				if (node.data["cxfExecutionNode"] != null)
				{
					rnode = rpaper.circle(pt.x, pt.y, 50);
					rnode.cxfExecutionNode = node.data["cxfExecutionNode"];
					rnode.attr({
						fill : 'thistle',
						stroke : 'darkorchid',
						'stroke-width' : 2});
					if (rnode.cxfExecutionNode._console)
						rnode.attr({"fill" : 'yellow'});
					
					var txt = rnode.cxfExecutionNode._dns + ":" + rnode.cxfExecutionNode._qPort + "\n" + rnode.cxfExecutionNode._osusername;
					rpaper.text(pt.x, pt.y, txt);
				
					// Events
					rnode.hover(function() {
						if (this.cxfExecutionNode._places == undefined)
							return;
						if (latestExpanded != null && latestExpanded._id === this.cxfExecutionNode._id)
							return;
						
						// Only one expanded node at a time
						if (latestExpanded != null)
						{
							for (var i = 0; i < this.cxfExecutionNode._places.getString().length; i++)
							{
								particleSystem.pruneNode(latestExpanded._places.getString()[i]);
							}
						}
						latestExpanded = this.cxfExecutionNode;
						
						for (var i = 0; i < this.cxfExecutionNode._places.getString().length; i++)
						{
							particleSystem.addNode(this.cxfExecutionNode._places.getString()[i], {cxfPlace: places[this.cxfExecutionNode._places.getString()[i]]});
							particleSystem.addEdge(this.cxfExecutionNode._id, this.cxfExecutionNode._places.getString()[i], {length:0.15,internal:true,});
						}
						
					},
					function() {
						// Do nothing on hover out
					});
					
				}
				
				if (node.data["cxfPlace"] != null)
				{
					rnode = rpaper.circle(pt.x, pt.y, 20);
					rnode.cxfPlace = node.data["cxfPlace"];
					rnode.attr({
						fill : 'lightgreen',
						stroke : 'lightslategray',
						'stroke-width' : 1});
					var txt = "Place " + rnode.cxfPlace._name;
					rpaper.text(pt.x, pt.y, txt);
				}
			});    			
		},
	};
	
	return that;
};    

function initNetworkROPanel()
{
	sys = arbor.ParticleSystem({friction:.5, stiffness:100, repulsion:2000});
	sys.renderer = ArborRaphaelRenderer();
	
	var bb = cxfApplication.getPlaces().getDTOPlace();
	for ( var i = 0; i < bb.length; i++) {
		var cxfPlace = bb[i];
		places[cxfPlace._id] = cxfPlace;
	}
	
	var aa = cxfApplication.getNodes().getDTOExecutionNode();
	for ( var i = 0; i < aa.length; i++) {
		var cxfNode = aa[i];
		
		sys.addNode(cxfNode._id, {cxfExecutionNode:cxfNode,});
		
		var ss = cxfNode.getToTCP().getString();
		for(var j = 0; j < ss.length; j++) {
			sys.addEdge(cxfNode._id, ss[j], "TCP");
		}
	}
}