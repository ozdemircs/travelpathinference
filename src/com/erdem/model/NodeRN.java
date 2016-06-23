package com.erdem.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NodeRN implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String id;

	private double latitude;

	private double longitude;

	private List<NodeRN> neighbors = new ArrayList<NodeRN>();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public List<NodeRN> getNeighbors() {
		return neighbors;
	}

	public void setNeighbors(List<NodeRN> neighbors) {
		this.neighbors = neighbors;
	}

	public String toString() {
		return id + " " + latitude + " " + longitude;
	}

}
