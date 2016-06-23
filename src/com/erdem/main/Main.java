package com.erdem.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.stream.XMLStreamReader;

import be.ac.ulg.montefiore.run.jahmm.Hmm;
import be.ac.ulg.montefiore.run.jahmm.ObservationVector;
import be.ac.ulg.montefiore.run.jahmm.OpdfMultiGaussian;

import com.erdem.datasetparser.DatasetLoader;
import com.erdem.model.EdgeNodeRN;
import com.erdem.model.NodeRN;
import com.erdem.model.ObservationRN;
import com.erdem.model.WayRN;
import com.erdem.utility.GraphUtility;

public class Main {

	public static void main(String[] args) throws Exception {
		
		
		List<Integer> listOfIntegers = new ArrayList<Integer>();
		
		listOfIntegers.add(5);
		listOfIntegers.add(7);
		listOfIntegers.add(11);
		
		for(Integer intX : listOfIntegers)
		{
		
			listOfIntegers.remove(intX);
		}
		

		DatasetLoader loader = new DatasetLoader();
		XMLStreamReader document = loader.loadMapDatasetAsXML("./romaMap.xml");

		// construct nodes.
		HashMap<String, NodeRN> nodesOriginal = loader.loadNodes(document);

		// get ways.
		document = loader.loadMapDatasetAsXML("./romaMap.xml");
		List<WayRN> waysOriginal = loader.loadWays(document);

		HashMap<String, NodeRN> newRoadMap = loader.getRoadTruncatedGraph(
				nodesOriginal, waysOriginal);

		List<ObservationRN> observations = loader.getObservationSequences(
				"152", "taxi_february.txt");

		// lets get first 10 observations.

		List<ObservationRN> observationsFiltered = observations.subList(20, 25);

		// filter map for observations.
		// find min, max lat lon coordinates.

		double minLat = Integer.MAX_VALUE;
		double maxLat = Integer.MIN_VALUE;
		double minLon = Integer.MAX_VALUE;
		double maxLon = Integer.MIN_VALUE;
		for (ObservationRN rn : observationsFiltered) {
			if (rn.getLat() > maxLat) {
				maxLat = rn.getLat();
			}

			if (rn.getLat() < minLat) {
				minLat = rn.getLat();
			}

			if (rn.getLon() < minLon) {
				minLon = rn.getLon();
			}

			if (rn.getLon() > maxLon) {
				maxLon = rn.getLon();
			}
		}

		double threshold = 0.002;
		
		GraphUtility utility = new GraphUtility();

		HashMap<String, NodeRN> filteredMap = utility.filterNodes(newRoadMap,
				minLat - threshold, maxLat + threshold, minLon - threshold, maxLon + threshold);
		
		System.out.println("Write coordinates of nodes");
		printCoordinates(filteredMap);
		

		// create an edge graph
		HashMap<String, EdgeNodeRN> edges = utility
				.convertToAnEdgeGraph(filteredMap);
		
		System.out.println("Write coordinates of edges");
		printEdgeCoordinates(edges);
		
		List<EdgeNodeRN> edgesList = new ArrayList<EdgeNodeRN>(edges.values());

		// construct pi probabilities
		double[] pi = new double[edgesList.size()];
		for (int i = 0; i < pi.length; i++) {
			pi[i] = 1.0 / edgesList.size();
		}
		// construct aij probabilities
		double[][] aij = new double[edgesList.size()][edgesList.size()];
		for (int i = 0; i < aij.length; i++) {
			EdgeNodeRN source = edgesList.get(i);
			for (int j = 0; j < aij.length; j++) {
				EdgeNodeRN target = edgesList.get(j);

				if (source == target || source.getNodeTarget() == target.getNodeSource()) {
					aij[i][j] = 1.0 / (source.getNeighbors().size() + 1);
				} else {
					aij[i][j] = 0;
				}
			}
		}

		// create gaussians.
		List<OpdfMultiGaussian> gaussian = new ArrayList<OpdfMultiGaussian>();
		for (int i = 0; i < edgesList.size(); i++) {
			double[] mean = new double[2];
			mean[0] = (edgesList.get(i).getNodeSource().getLatitude() + edgesList
					.get(i).getNodeTarget().getLatitude()) / 2;
			mean[1] = (edgesList.get(i).getNodeSource().getLongitude() + edgesList
					.get(i).getNodeTarget().getLongitude()) / 2;

			double[][] variance = new double[2][2];
			variance[0][0] = 1e-9;
			variance[0][1] = 0;
			variance[1][0] = 0;
			variance[1][1] = 1e-9;

			gaussian.add(new OpdfMultiGaussian(mean, variance));
		}

		// create a hidden markov model for the states.

		Hmm<ObservationVector> hmm = new Hmm<ObservationVector>(pi, aij,
				gaussian);

		List<ObservationVector> oSeq = new ArrayList<ObservationVector>();

		for (ObservationRN o : observationsFiltered) {
			oSeq.add(new ObservationVector(new double[] { o.getLat(),
					o.getLon() }));
		}

		int[] sequence = hmm.mostLikelyStateSequence(oSeq);

		System.out.println("Observations---");
		printObservations(observationsFiltered);
		System.out.println("Found Locations---");
		for (int i = 0; i < sequence.length; i++) {
			System.out.println("[" + edgesList.get(sequence[i]).getNodeSource()
					.getLongitude()
					+ "  ,"
					+ edgesList.get(sequence[i]).getNodeSource().getLatitude() + "],");
		}
	}
	
	private static void printObservations(List<ObservationRN> observations)
	{
		System.out.println("[");
		for(ObservationRN observation : observations)
		{
			System.out.println("[" + observation.getLon() + ", " + observation.getLat() + "],");
		}
		
		System.out.println("]");
	}

	private static void printEdgeCoordinates(HashMap<String, EdgeNodeRN> edges) {
		System.out.println("[");
		for(String key : edges.keySet())
		{
			System.out.println("[" + edges.get(key).getNodeSource().getLongitude() + ", " + edges.get(key).getNodeSource().getLatitude() + "],");
		}
		
		System.out.println("]");
		
	}

	private static void printCoordinates(HashMap<String, NodeRN> filteredMap) {
		System.out.println("[");
		for(String key : filteredMap.keySet())
		{
			System.out.println("[" + filteredMap.get(key).getLongitude() + ", " + filteredMap.get(key).getLatitude() + "],");
		}
		
		System.out.println("]");
		
	}
}
