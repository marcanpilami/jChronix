package org.oxymores.chronix.core;

public class NodeLink extends MetaObject {
	private static final long serialVersionUID = 7100333809406249831L;
	protected NodeConnectionMethod method;
	protected ExecutionNode nodeFrom, nodeTo;
	
	
	
	public NodeConnectionMethod getMethod() {
		return method;
	}
	public void setMethod(NodeConnectionMethod method) {
		this.method = method;
	}
	public ExecutionNode getNodeFrom() {
		return nodeFrom;
	}
	public void setNodeFrom(ExecutionNode nodeFrom) {
		if (this.nodeFrom == null || !this.nodeFrom.getId().equals(nodeFrom.getId()))
			nodeFrom.addCanSendTo(this);
		this.nodeFrom = nodeFrom;
	}
	public ExecutionNode getNodeTo() {
		return nodeTo;
	}
	public void setNodeTo(ExecutionNode nodeTo) {
		if (this.nodeTo == null || !this.nodeTo.getId().equals(nodeTo.getId()))
			nodeTo.addCanReceiveFrom(this);
		this.nodeTo = nodeTo;
	}
}
