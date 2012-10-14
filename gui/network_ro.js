var places = new Object();

var ArborRaphaelRenderer = function(){
	var rpaper = null;
	var particleSystem = null;
	var latestExpanded = null;
	var drawnNodes = new Object();
	var drawnEdges = new Object();
	var refNodes = new Object();
	var refEdges = new Object();
	
	var that = {
		init:function(system){   
			rpaper = new Raphael("raphael-pnro", 1000, 500);  
			rpaper.clear();  
			  
			particleSystem = system;
			particleSystem.screenSize(rpaper.width, rpaper.height); 
			particleSystem.screenPadding(80); // leave an extra 80px of whitespace per side
		},
		  
		redraw:function(){
			refNodes = new Object();
			refEdges = new Object();
			
			particleSystem.eachEdge(function(edge, pt1, pt2){
				// edge: {source:Node, target:Node, length:#, data:{}}
				// pt1: {x:#, y:#} source position in screen coords
				// pt2: {x:#, y:#} target position in screen coords
				
				refEdges[edge.data["id"]] = edge;
				
				// Check if already drawn & update it if it is the case
				if (drawnEdges[edge.data["id"]] != null)
				{
					var arrow = drawnEdges[edge.data["id"]];
					var path = arrow.attr("path");
					var pc = "M" + pt1.x + "," + pt1.y + ",L" + pt2.x + "," + pt2.y;
					var shifted1 = Raphael.getPointAtLength(pc, nodeSize);
					var shifted2 = Raphael.getPointAtLength(pc, Raphael.getTotalLength(pc) - nodeSize);
					if (edge.data["internal"] != null)
						shifted2 = Raphael.getPointAtLength(pc, Raphael.getTotalLength(pc) - 20);

					path[0][1] = shifted1.x;
					path[0][2] = shifted1.y;
					path[1][1] = shifted2.x;
					path[1][2] = shifted2.y;					
					arrow.attr({"path" : path});
					return;
				}
				
				
				// If not already drawn, draw a line from pt1 to pt2			
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
				
				if (edge.data["style"] === "TCP")
					arrow.attr({"stroke" : 'blue'});
				
				// Store the line to avoid creating a new object later
				drawnEdges[edge.data["id"]] = arrow;
			});
			
			
			particleSystem.eachNode(function(node, pt){
				// node: {mass:#, p:{x,y}, name:"", data:{}}
				// pt: {x:#, y:#} node position in screen coords
				
				refNodes[node.data["id"]] = node;
				var rnode = null;
				
				// If already drawn, just update the SVG element
				if ( (node.data["cxfExecutionNode"] != null && drawnNodes[node.data["cxfExecutionNode"]._id] != null)
						|| (node.data["cxfPlace"] != null && drawnNodes[node.data["cxfPlace"]._id] != null))
				{
					if (node.data["cxfExecutionNode"] != null)
						rnode = drawnNodes[node.data["cxfExecutionNode"]._id];
					if (node.data["cxfPlace"] != null)
						rnode = drawnNodes[node.data["cxfPlace"]._id];
					
					rnode.attr({cx: pt.x, cy: pt.y});
					for(var e in rnode.contents) 
						rnode.contents[e].attr({x : pt.x,y : pt.y});
					return;
				}
				
				// If not already drawn, do it
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
					var rtxt = rpaper.text(pt.x, pt.y, txt);
				
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
							var placeId = this.cxfExecutionNode._places.getString()[i];
							particleSystem.addNode(this.cxfExecutionNode._places.getString()[i], {id:placeId,cxfPlace: places[placeId]});
							particleSystem.addEdge(this.cxfExecutionNode._id, placeId, {length:0.15,internal:true,});
						}
						
					},
					function() {
						// Do nothing on hover out
					});
					
					// Store the node so as to avoid redrawing it later
					rnode.contents = new Object();
					rnode.contents["text"] = rtxt;
					drawnNodes[rnode.cxfExecutionNode._id] = rnode;
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
					var rtxt = rpaper.text(pt.x, pt.y, txt);
					
					// Store the node so as to avoid redrawing it later
					rnode.contents = new Object();
					rnode.contents["text"] = rtxt;
					drawnNodes[rnode.cxfPlace._id] = rnode;
				}
			});    		
			
			// Remove all nodes that are not in the particle system anymore
			drawnNodesLoop:
			for (var key in drawnNodes)
			{
				for (var key2 in refNodes)
				{
					if (key2 === key) 
						continue drawnNodesLoop; // node found in the particle system - go to next drawn node
				}
				// If here : drawn node should be deleted
				for(var e in drawnNodes[key].contents) 
					drawnNodes[key].contents[e].remove();
				drawnNodes[key].remove();
				delete drawnNodes[key];
			}
			
			// END OF NODES HANDLING
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
		
		sys.addNode(cxfNode._id, {cxfExecutionNode:cxfNode,id:cxfNode._id,});
		
		var ss = cxfNode.getToTCP().getString();
		for(var j = 0; j < ss.length; j++) {
			sys.addEdge(cxfNode._id, ss[j], {style:"TCP", id:cxfNode._id + ss[j]});
		}
	}
}