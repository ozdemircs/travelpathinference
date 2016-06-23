package com.erdem.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
/**
 * Class is used to store edges.
 * It has nodeSource and nodeTarget and set of other edges where we can move.
 * 
 * @author erdem
 *
 */
public class EdgeNodeRN implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -8736873785363293247L;
	
	

	private NodeRN nodeSource;

	private NodeRN nodeTarget;

	private List<EdgeNodeRN> neighbors = new ArrayList<EdgeNodeRN>();
	
	private List<Double> speeds = new ArrayList<Double>();

	public EdgeNodeRN(){
		
	}
	
	
	public NodeRN getNodeSource() {
		return nodeSource;
	}

	public void setNodeSource(NodeRN nodeSource) {
		this.nodeSource = nodeSource;
	}

	public NodeRN getNodeTarget() {
		return nodeTarget;
	}

	public void setNodeTarget(NodeRN nodeTarget) {
		this.nodeTarget = nodeTarget;
	}

	public List<EdgeNodeRN> getNeighbors() {
		return neighbors;
	}

	public void setNeighbors(List<EdgeNodeRN> neighbors) {
		this.neighbors = neighbors;
	}
	
	public String toString()
	{
		return nodeSource.getLatitude() + " " + nodeSource.getLongitude() + " " + nodeTarget.getLatitude() + " " + nodeTarget.getLongitude();
	}
	
	public String getId()
	{
		return nodeSource.getId() + "-" + nodeTarget.getId();
	}


	public List<Double> getSpeeds() {
		return speeds;
	}


	public void setSpeeds(List<Double> speeds) {
		this.speeds = speeds;
	}

}
