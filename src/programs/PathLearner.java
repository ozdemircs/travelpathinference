package programs;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

/**
 * Servlet implementation class RoadNetwork.
 */

public class PathLearner {

	private HashMap<String, NodeRN> nodesOriginal;
	private List<WayRN> waysOriginal;
	private HashMap<String, NodeRN> newRoadMap;

	public static void main(String args[]) throws ServletException {
		PathLearner learner = new PathLearner();
		learner.learn();

	}

	public void learn() {

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

			List<String> drivers = null;

			File f = new File("drivers");

			if (!f.exists()) {
				drivers = loader
						.loadTaxiDrivers("/mnt/RoadNetworkWeb/WebContent/WEB-INF/taxi_february.txt");
				loader.serialize(drivers, "drivers");
			} else {
				drivers = (List<String>) loader.deserialize("drivers");
			}

			HashMap<String, Integer> counts = new HashMap<String, Integer>();

			for (int driverIndex = 0; driverIndex < drivers.size() / 2; driverIndex++) {

				String driver = drivers.get(driverIndex);

				List<ObservationRN> observationsOfTaxiDriver;

				f = new File("driver" + driver);

				if (!f.exists()) {
					observationsOfTaxiDriver = loader
							.getObservationSequences(driver,
									"/mnt/RoadNetworkWeb/WebContent/WEB-INF/taxi_february.txt");
					loader.serialize(observationsOfTaxiDriver, "driver"
							+ driver);
				} else {
					observationsOfTaxiDriver = (List<ObservationRN>) loader
							.deserialize("driver" + driver);
				}
				System.out.println("Total observations : "
						+ observationsOfTaxiDriver.size());
				int sampleRate = 10;
				int keepRateInt = 1;

				for (int start = 0; start <= observationsOfTaxiDriver.size() - 7; start += 7) {
					List<ObservationRN> observationsFiltered = observationsOfTaxiDriver
							.subList(start, start + 7);
					// filter map for observations.
					// find min, max lat lon coordinates.
					//System.out.println(start);
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

					double threshold = 0.0003;

					GraphUtility utility = new GraphUtility();

					HashMap<String, NodeRN> filteredMap = utility.filterNodes(
							newRoadMap, minLat - threshold, maxLat + threshold,
							minLon - threshold, maxLon + threshold);
					// create an edge graph
					HashMap<String, EdgeNodeRN> edges = utility
							.convertToAnEdgeGraph(filteredMap);

					List<EdgeNodeRN> edgesList = new ArrayList<EdgeNodeRN>(
							edges.values());

					if (edgesList.size() == 0 || edgesList.size() > 300) {
						continue;
					}

					// construct pi probabilities
					double[] pi = new double[edgesList.size()];
					for (int i = 0; i < pi.length; i++) {
						pi[i] = 1.0 / edgesList.size();
					}
					// construct aij probabilities
					double[][] aij = new double[edgesList.size()][edgesList
							.size()];
					for (int i = 0; i < aij.length; i++) {
						EdgeNodeRN source = edgesList.get(i);
						for (int j = 0; j < aij.length; j++) {
							EdgeNodeRN target = edgesList.get(j);

							if (source == target
									|| source.getNodeTarget() == target
											.getNodeSource()) {
								aij[i][j] = 1.0 / (source.getNeighbors().size() + 1);
							} else {
								aij[i][j] = 0;
							}
						}
					}

					//
					// create gaussians.
					List<OpdfMultiGaussian> gaussian = new ArrayList<OpdfMultiGaussian>();
					for (int i = 0; i < edgesList.size(); i++) {
						double[] mean = new double[2];
						mean[0] = (edgesList.get(i).getNodeSource()
								.getLatitude() + edgesList.get(i)
								.getNodeTarget().getLatitude()) / 2;
						mean[1] = (edgesList.get(i).getNodeSource()
								.getLongitude() + edgesList.get(i)
								.getNodeTarget().getLongitude()) / 2;

						double[][] variance = new double[2][2];
						variance[0][0] = new Double(1e-7);
						variance[0][1] = 0;
						variance[1][0] = 0;
						variance[1][1] = new Double(1e-7);

						gaussian.add(new OpdfMultiGaussian(mean, variance));
					}

					// create a hidden markov model for the states.

					Hmm<ObservationVector> hmm = new Hmm<ObservationVector>(pi,
							aij, gaussian);

					List<ObservationVector> oSeq = new ArrayList<ObservationVector>();

					// populate observations. // 10 more for each
					for (int i = 1; i < observationsFiltered.size(); i++) {

						ObservationRN prev = observationsFiltered.get(i - 1);
						ObservationRN next = observationsFiltered.get(i);

						int sampleRateInt = new Integer(sampleRate);
						double latDelta = (next.getLat() - prev.getLat())
								/ (sampleRateInt * keepRateInt);

						double lonDelta = (next.getLon() - prev.getLon())
								/ (sampleRateInt * keepRateInt);

						for (int j = 0; j < sampleRateInt * keepRateInt; j++) {
							oSeq.add(new ObservationVector(new double[] {
									prev.getLat() + latDelta * j,
									prev.getLon() + lonDelta * j }));
						}

						oSeq.add(new ObservationVector(new double[] {
								next.getLat(), next.getLon() }));

					}
					int[] sequence = hmm.mostLikelyStateSequence(oSeq);

					// check if sequence is all same
					boolean hasAllSame = true;
					int number = sequence[0];

					for (int j = 0; j < sequence.length; j++) {
						if (sequence[j] != number) {
							hasAllSame = false;
						}
					}

					if (!hasAllSame) {
						for (int j = 1; j < sequence.length; j++) {

							EdgeNodeRN edgePrevious = edgesList
									.get(sequence[j - 1]);
							EdgeNodeRN edge = edgesList.get(sequence[j]);

							String key = edgePrevious.getNodeSource().getId()
									+ "-"
									+ edgePrevious.getNodeTarget().getId()
									+ "->" + edge.getNodeSource().getId() + "-"
									+ edge.getNodeTarget().getId();

							// counts from edge prev to edge.
							if (counts.get(key) == null) {
								counts.put(key, 1);
							} else {
								counts.put(key, counts.get(key) + 1);
							}
						}
					}

				}

			}

			loader.serialize(counts, "EdgeUsageCounts");

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public PathLearner() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

	}

}
