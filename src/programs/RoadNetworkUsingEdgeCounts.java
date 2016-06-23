package programs;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
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
@WebServlet("/RoadNetworkUsingEdgeCounts")
public class RoadNetworkUsingEdgeCounts extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private HashMap<String, NodeRN> nodesOriginal;
	private List<WayRN> waysOriginal;
	private HashMap<String, NodeRN> newRoadMap;
	HashMap<String, Integer> counts;

	@Override
	public void init() throws ServletException {

		super.init();

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

			counts = (HashMap<String, Integer>) loader
					.deserialize("/mnt/RoadNetworkWeb/EdgeUsageCounts");

			//System.out.println(counts);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public RoadNetworkUsingEdgeCounts() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		/**
		 * Keep rate so we can made it not fine sampled.
		 */
		String keepRate = "1";
		String covariance = "1e-7";
		String start = "5";
		String end = "10";
		String sampleRate = "10";
		String driver = "152";

		if (request.getParameter("driver") != null) {
			driver = request.getParameter("driver");
		}

		if (request.getParameter("sampleRate") != null) {
			sampleRate = request.getParameter("sampleRate");
		}

		if (request.getParameter("covariance") != null) {
			covariance = request.getParameter("covariance");
		}
		/**
		 * Start index for the taxi driver.
		 */
		if (request.getParameter("start") != null) {
			start = request.getParameter("start");
		}

		/**
		 * End index for the taxi driver.
		 */
		if (request.getParameter("end") != null) {
			end = request.getParameter("end");
		}

		if (request.getParameter("keepRate") != null) {
			keepRate = request.getParameter("keepRate");
		}

		DatasetLoader loader = new DatasetLoader();
		List<ObservationRN> observations = null;

		// load observations from disk otherwise search for it.
		File f = new File("/mnt/RoadNetworkWeb/driver"
				+ driver);
		if (f.exists()) {
			observations = (List<ObservationRN>) loader
					.deserialize("/mnt/RoadNetworkWeb/driver"
							+ driver);

		} else {
			try {
				observations = loader
						.getObservationSequences(driver,
								"/mnt/RoadNetworkWeb/WebContent/WEB-INF/taxi_february.txt");

				loader.serialize(observations,
						"/mnt/RoadNetworkWeb/driver" + driver);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		int keepRateInt = new Integer(keepRate);
		int startInt = new Integer(start);
		int endInt = new Integer(end);

		// lets get first 10 observations.
		List<ObservationRN> observationsFiltered = new ArrayList<ObservationRN>();

		while (startInt < endInt) {
			observationsFiltered.add(observations.get(startInt));
			startInt += keepRateInt;
		}
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

		double threshold = 0.0003;

		GraphUtility utility = new GraphUtility();

		HashMap<String, NodeRN> filteredMap = utility.filterNodes(newRoadMap,
				minLat - threshold, maxLat + threshold, minLon - threshold,
				maxLon + threshold);
		System.out.println("Before filter : " + filteredMap.size());
		// filter nodes.
		System.out.println("After filter : " + filteredMap.size());
		// create an edge graph
		HashMap<String, EdgeNodeRN> edges = utility
				.convertToAnEdgeGraph(filteredMap);

		/**
		 * Preparation of nodeList
		 */
		List<String> nodeList = new ArrayList<String>();
		for (NodeRN node : filteredMap.values()) {
			nodeList.add(node.getLongitude() + "," + node.getLatitude());
		}

		String centerObservationMap = observationsFiltered.get(0).getLon()
				+ "," + observationsFiltered.get(0).getLat();

		List<EdgeNodeRN> edgesList = new ArrayList<EdgeNodeRN>(edges.values());

		if (edgesList.size() == 0) {
			return;
		}

		// construct pi probabilities
		double[] pi = new double[edgesList.size()];
		for (int i = 0; i < pi.length; i++) {
			pi[i] = 1.0 / edgesList.size();
		}
		// construct aij probabilities
		double[][] aij = new double[edgesList.size()][edgesList.size()];
		int K = 5; // this is a belief that we have used every possible path
		// 5 times before.
		for (int i = 0; i < aij.length; i++) {

			EdgeNodeRN source = edgesList.get(i);

			for (int j = 0; j < aij.length; j++) {

				EdgeNodeRN target = edgesList.get(j);

				if (source == target
						|| source.getNodeTarget() == target.getNodeSource()) {

					int outEdgesFromI = 0;
					// unfortunately this count does not give that, it gives how
					// many times that road j is being used.
					// if j is at some intersection, then it will have more
					// count than others.

					int outToJFromI = getCountFromSourceToTarget(source, target, K);

					for (NodeRN neighbor : source.getNodeTarget()
							.getNeighbors()) {
						
						EdgeNodeRN temp = new EdgeNodeRN();
						temp.setNodeSource(source.getNodeTarget());
						temp.setNodeTarget(neighbor);
						
						outEdgesFromI += getCountFromSourceToTarget(source, temp, K);
					}
					outEdgesFromI += getCountFromSourceToTarget(source, source, K);

					aij[i][j] = 1.0 * outToJFromI / outEdgesFromI;
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
			variance[0][0] = new Double(covariance);
			variance[0][1] = 0;
			variance[1][0] = 0;
			variance[1][1] = new Double(covariance);

			gaussian.add(new OpdfMultiGaussian(mean, variance));
		}

		// create a hidden markov model for the states.

		Hmm<ObservationVector> hmm = new Hmm<ObservationVector>(pi, aij,
				gaussian);

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

			oSeq.add(new ObservationVector(new double[] { next.getLat(),
					next.getLon() }));
		}

		int[] sequence = hmm.mostLikelyStateSequence(oSeq);

		// preparation of sequence list
		List<String> edgeList = new ArrayList<String>();
		for (int i = 0; i < sequence.length; i++) {
			edgeList.add(edgesList.get(sequence[i]).getNodeSource()
					.getLongitude()
					+ ","
					+ edgesList.get(sequence[i]).getNodeSource().getLatitude());

		}
		edgeList.add(edgesList.get(sequence[sequence.length - 1])
				.getNodeTarget().getLongitude()
				+ ","
				+ edgesList.get(sequence[sequence.length - 1]).getNodeTarget()
						.getLatitude());

		/**
		 * Preparation of observation list.
		 */

		List<String> observationList = new ArrayList<String>();

		for (ObservationRN o : observationsFiltered) {
			observationList.add(o.getLon() + "," + o.getLat());
		}

		try {
			// simulate a database query

			// get UI
			RequestDispatcher requestDispatcher = request
					.getRequestDispatcher("edges.vm");
			request.setAttribute("nodeList", nodeList);
			request.setAttribute("edgeList", edgeList);
			request.setAttribute("centerObservationMap", centerObservationMap);
			request.setAttribute("observationList", observationList);

			requestDispatcher.forward(request, response);
		} catch (Exception ex) {

		}

	}

	private int getCountFromSourceToTarget(EdgeNodeRN source,
			EdgeNodeRN target, int K) {

		String sourceId = source.getNodeSource().getId() + "-"
				+ source.getNodeTarget().getId();

		String targetId = target.getNodeSource().getId() + "-"
				+ target.getNodeTarget().getId();

		if (counts.get(sourceId + "->" + targetId) == null) {
			return K;
		} else {
			return counts.get(sourceId + "->" + targetId);
		}

	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

	}

}
