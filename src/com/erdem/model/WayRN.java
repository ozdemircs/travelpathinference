package com.erdem.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class WayRN implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -1127115575833620004L;
	private HashMap<String, String> tags = new HashMap<String, String>();
	private List<String> nodeIds = new ArrayList<String>();
	private String id;
	
	
	public List<String> getNodeIds() {
		return nodeIds;
	}

	public void setNodeIds(List<String> nodeIds) {
		this.nodeIds = nodeIds;
	}


	public HashMap<String, String> getTags() {
		return tags;
	}

	public void setTags(HashMap<String, String> tags) {
		this.tags = tags;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
