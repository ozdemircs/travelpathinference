package com.erdem.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import javax.xml.stream.XMLStreamReader;

import com.erdem.datasetparser.DatasetLoader;

public class Algorithm implements Serializable{
	
	private static final long serialVersionUID = 1L;

	// Extracted from OSM maps.
	private HashMap<String, NodeRN> nodesOriginal;
	private List<WayRN> waysOriginal;
	

	
	// New road map is a conversion to NodeRN nodes.
	private HashMap<String, NodeRN> newRoadMap;
	
	public void init()
	{
		initRoadNetwork();
	}
	
	public void initRoadNetwork() {
		
		try {
			
			DatasetLoader loader = new DatasetLoader();

			XMLStreamReader document = loader
					.loadMapDatasetAsXML("/mnt/RoadNetworkWeb/WebContent/WEB-INF/romaMap.xml");

			// construct nodes.
			nodesOriginal = loader.loadNodes(document);

			// get ways.
			document = loader
					.loadMapDatasetAsXML("/mnt/RoadNetworkWeb/WebContent/WEB-INF/romaMap.xml");
			
			waysOriginal = loader.loadWays(document);

			newRoadMap = loader.getRoadTruncatedGraph(nodesOriginal,
					waysOriginal);
						
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public List<EdgeNodeRN> computeEdges(List<ObservationRN> observations)
	{
		return null;
	}
	

	public HashMap<String, NodeRN> getNodesOriginal() {
		return nodesOriginal;
	}


	public void setNodesOriginal(HashMap<String, NodeRN> nodesOriginal) {
		this.nodesOriginal = nodesOriginal;
	}


	public List<WayRN> getWaysOriginal() {
		return waysOriginal;
	}


	public void setWaysOriginal(List<WayRN> waysOriginal) {
		this.waysOriginal = waysOriginal;
	}


	public HashMap<String, NodeRN> getNewRoadMap() {
		return newRoadMap;
	}

	public void setNewRoadMap(HashMap<String, NodeRN> newRoadMap) {
		this.newRoadMap = newRoadMap;
	}
	
	public String getParameters()
	{
		return "";
	}
}
