package com.erdem.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * This represents observations and ground truth for these observations.
 * @author erdem
 *
 */
public class PathGroundTruth implements Serializable{
	
	private static final long serialVersionUID = 8280634986477563061L;
	
	private String userId;
	
	private int startObservation;
	
	private int endObservation;
	
	private List<ObservationRN> observations = new ArrayList<ObservationRN>();
	
	private List<String> edges = new ArrayList<String>();

	public List<String> getEdges() {
		return edges;
	}

	public void setEdges(List<String> edges) {
		this.edges = edges;
	}

	public List<ObservationRN> getObservations() {
		return observations;
	}

	public void setObservations(List<ObservationRN> observations) {
		this.observations = observations;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public int getStartObservation() {
		return startObservation;
	}

	public void setStartObservation(int startObservation) {
		this.startObservation = startObservation;
	}

	public int getEndObservation() {
		return endObservation;
	}

	public void setEndObservation(int endObservation) {
		this.endObservation = endObservation;
	}
	
}
