package com.erdem.model;

import java.util.List;

public class PathRN {
	
	private double score;
	
	private List<EdgeNodeRN> edges;
	
	public double getScore() {
		return score;
	}
	public void setScore(double score) {
		this.score = score;
	}
	public List<EdgeNodeRN> getEdges() {
		return edges;
	}
	public void setEdges(List<EdgeNodeRN> edges) {
		this.edges = edges;
	}
}
