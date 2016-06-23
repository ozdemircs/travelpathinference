package com.erdem.utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.erdem.model.EdgeNodeRN;
import com.erdem.model.NodeRN;
import com.erdem.model.ObservationRN;

public class GraphUtility {

	/**
	 * Filter nodes based on 1 parameters - slope and distance of new edge.
	 * 
	 */

	public void filterNodesBasedOnSlope(HashMap<String, NodeRN> nodes,
			double slope, double distance) {
		boolean change = false;

		do {
			NodeRN toBeRemoved = null;
			change = false;
			for (NodeRN node : nodes.values()) {

				List<NodeRN> incomingEdgeNodes = findIncomingEdgeNodes(nodes,
						node);
				List<NodeRN> outgoingEdgeNodes = node.getNeighbors();

				// case 1
				if (incomingEdgeNodes.size() == 1
						&& outgoingEdgeNodes.size() == 1) {
					// this is for one way roads.
					NodeRN incoming = incomingEdgeNodes.get(0);
					NodeRN outgoing = outgoingEdgeNodes.get(0);

					// check for slope.

					if (checkForSlope(slope, incoming, node, outgoing)
							&& checkForDistance(distance, incoming, outgoing)) {
						incoming.getNeighbors().remove(node);
						incoming.getNeighbors().add(outgoing);
						node.getNeighbors().remove(outgoing);
						change = true;
						toBeRemoved = node;
						break;
					}

				}
				// case 2
				if (incomingEdgeNodes.size() == 2
						&& outgoingEdgeNodes.size() == 2) {
					boolean hasAll = true;
					// if incoming and outgoing nodes are same,
					for (NodeRN income : incomingEdgeNodes) {
						if (!outgoingEdgeNodes.contains(income)) {
							hasAll = false;
						}
					}

					if (hasAll) {

						// this is for one way roads.
						NodeRN incoming = incomingEdgeNodes.get(0);
						NodeRN outgoing = outgoingEdgeNodes.get(0);

						if (outgoing == incoming) {
							outgoing = outgoingEdgeNodes.get(1);
						}

						// check for slope.

						if (checkForSlope(slope, incoming, node, outgoing)) {
							incoming.getNeighbors().remove(node);
							incoming.getNeighbors().add(outgoing);
							node.getNeighbors().remove(outgoing);
							// also do the other way.
							outgoing.getNeighbors().remove(node);
							outgoing.getNeighbors().add(incoming);
							node.getNeighbors().remove(incoming);
							toBeRemoved = node;
							change = true;
							break;
						}

					}

				}
			}

			if (change) {
				nodes.remove(toBeRemoved.getId());
			}

		} while (change);

	}

	private boolean checkForDistance(double distance, NodeRN incoming,
			NodeRN outgoing) {
		double distanceOfNodes = (incoming.getLatitude() - outgoing
				.getLatitude())
				* (incoming.getLatitude() - outgoing.getLatitude());
		distanceOfNodes += (incoming.getLongitude() - outgoing.getLongitude())
				* (incoming.getLongitude() - outgoing.getLongitude());

		if (distanceOfNodes < distance) {
			return true;
		}
		return false;
	}

	private boolean checkForSlope(double slope, NodeRN incoming, NodeRN node,
			NodeRN outgoing) {
		double slope1 = (node.getLongitude() - incoming.getLongitude())
				/ (node.getLatitude() - incoming.getLatitude());
		double slope2 = (outgoing.getLongitude() - node.getLongitude())
				/ (outgoing.getLatitude() - node.getLatitude());

		if (Math.abs(slope1 - slope2) < slope) {
			return true;
		}
		return false;
	}

	private List<NodeRN> findIncomingEdgeNodes(HashMap<String, NodeRN> nodes,
			NodeRN node) {

		List<NodeRN> incomingNodes = new ArrayList<NodeRN>();

		for (NodeRN nodeRN : nodes.values()) {

			for (NodeRN neighbor : nodeRN.getNeighbors()) {
				if (neighbor == node) {
					incomingNodes.add(nodeRN);
				}
			}

		}
		return incomingNodes;
	}

	/**
	 * Very inefficient way of sorting edges by their connections to display it
	 * on map.
	 * 
	 * @param edges
	 * @param edge
	 */
	public void sortEdgesByConnection(List<EdgeNodeRN> edges, EdgeNodeRN edge,
			int depth) {

		if (!edges.contains(edge) && depth > 0) {
			edges.add(edge);
			for (EdgeNodeRN connection : edge.getNeighbors()) {
				sortEdgesByConnection(edges, connection, depth - 1);
			}
		}
	}



	public HashMap<String, EdgeNodeRN> convertToAnEdgeGraph(
			HashMap<String, NodeRN> nodes) {
		// first add nodes
		HashMap<String, EdgeNodeRN> edges = new HashMap<String, EdgeNodeRN>();

		// this extract edges
		for (String key : nodes.keySet()) {
			NodeRN node = nodes.get(key);

			for (NodeRN neighbor : node.getNeighbors()) {
				EdgeNodeRN edge = new EdgeNodeRN();
				edge.setNodeSource(node);
				edge.setNodeTarget(neighbor);
				edges.put(node.getId() + "-" + neighbor.getId(), edge);
			}
		}

		// this connect those
		for (String key : edges.keySet()) {
			String[] ids = key.split("-");
			NodeRN node = nodes.get(ids[1]);

			// get neighbors of these nodes
			for (NodeRN neighbor : node.getNeighbors()) {
				EdgeNodeRN edge = edges.get(node.getId() + "-"
						+ neighbor.getId());
				edges.get(key).getNeighbors().add(edge);
			}

		}

		return edges;
	}

	// among each node
	public HashMap<String, NodeRN> filterNodesAroundEachObservation(
			HashMap<String, NodeRN> nodes, List<ObservationRN> observation,
			double threshold) {
		// traverse through list of nodes and remove any node not within those
		// latmin latmax etc.
		HashMap<String, NodeRN> newMap = new HashMap<String, NodeRN>();

		// first go over nodes and filter ones that dont exist then establish
		// edges.
		for (String key : nodes.keySet()) {

			NodeRN node = nodes.get(key);

			for (ObservationRN o : observation) {
				if (inside(o.getLat() - threshold, o.getLat() + threshold,
						o.getLon() - threshold, o.getLon() + threshold,
						node.getLatitude(), node.getLongitude())) {
					NodeRN newNode = new NodeRN();
					newNode.setId(node.getId());
					newNode.setLatitude(node.getLatitude());
					newNode.setLongitude(node.getLongitude());
					newMap.put(key, newNode);
				}
			}
		}

		// lets establish edges.
		for (String key : newMap.keySet()) {

			NodeRN node = newMap.get(key);
			NodeRN original = nodes.get(key);

			for (NodeRN neighbor : original.getNeighbors()) {
				if (newMap.get(neighbor.getId()) != null) {
					node.getNeighbors().add(newMap.get(neighbor.getId()));
				}
			}
		}

		return newMap;
	}

	// among each node
	public HashMap<String, NodeRN> filterNodes(HashMap<String, NodeRN> nodes,
			double latMin, double latMax, double lonMin, double lonMax) {
		// traverse through list of nodes and remove any node not within those
		// latmin latmax etc.
		HashMap<String, NodeRN> newMap = new HashMap<String, NodeRN>();

		// first go over nodes and filter ones that dont exist then establish
		// edges.
		for (String key : nodes.keySet()) {
			NodeRN node = nodes.get(key);

			if (inside(latMin, latMax, lonMin, lonMax, node.getLatitude(),
					node.getLongitude())) {
				NodeRN newNode = new NodeRN();
				newNode.setId(node.getId());
				newNode.setLatitude(node.getLatitude());
				newNode.setLongitude(node.getLongitude());
				newMap.put(key, newNode);
			}

		}

		// lets establish edges.
		for (String key : newMap.keySet()) {

			NodeRN node = newMap.get(key);
			NodeRN original = nodes.get(key);

			for (NodeRN neighbor : original.getNeighbors()) {
				if (newMap.get(neighbor.getId()) != null) {
					node.getNeighbors().add(newMap.get(neighbor.getId()));
				}
			}
		}

		return newMap;
	}

	public boolean inside(double latMin, double latMax, double lonMin,
			double lonMax, double targetLat, double targetLon) {
		if (targetLat >= latMin && targetLat <= latMax && targetLon >= lonMin
				&& targetLon <= lonMax) {
			return true;
		} else {
			return false;
		}
	}
}
