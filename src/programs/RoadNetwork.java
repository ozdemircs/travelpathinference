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

import com.erdem.datasetparser.DatasetLoader;
import com.erdem.model.EdgeNodeRN;
import com.erdem.model.NodeRN;
import com.erdem.model.ObservationRN;

/**
 * Servlet implementation class RoadNetwork.
 */
@WebServlet("/RoadNetwork")
public class RoadNetwork extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private RoadNetworkVTrackAlgorithm algorithm;

	@Override
	public void init() throws ServletException {

		super.init();

		algorithm = new RoadNetworkVTrackAlgorithm(1, 0.004, 1e-7, 5);

		algorithm.init();
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
		String sampleRate = "3";
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
			startInt += 1;
		}
		// filter map for observations.
		// find min, max lat lon coordinates.

		List<EdgeNodeRN> pathEdges = algorithm.computePathEdges(
				observationsFiltered, keepRateInt, 0.004,
				new Double(covariance), new Integer(sampleRate));

		// according to keep rate
		observationsFiltered = algorithm.getObservationsFiltered();
		HashMap<String, NodeRN> filteredMap = algorithm.getFilteredMap();

		String centerObservationMap = observationsFiltered.get(0).getLon()
				+ "," + observationsFiltered.get(0).getLat();

		/**
		 * Preparation of nodeList
		 */
		List<String> nodeList = new ArrayList<String>();
		for (NodeRN node : filteredMap.values()) {
			nodeList.add(node.getLongitude() + "," + node.getLatitude());
		}

		/**
		 * Preparation of observation list.
		 */

		List<String> observationList = new ArrayList<String>();

		for (ObservationRN o : observationsFiltered) {
			observationList.add(o.getLon() + "," + o.getLat());
		}

		List<String> edgeList = new ArrayList<String>();

		for (int i = 0; i < pathEdges.size(); i++) {

			EdgeNodeRN edge = pathEdges.get(i);

			System.out.println(edge);

			edgeList.add(edge.getNodeSource().getLongitude() + ","
					+ edge.getNodeSource().getLatitude());
			edgeList.add(edge.getNodeTarget().getLongitude() + ","
					+ edge.getNodeTarget().getLatitude());

		}

		try {
			// simulate a database query

			// get UI
			RequestDispatcher requestDispatcher = request
					.getRequestDispatcher("edges.vm");
			request.setAttribute("nodeList", nodeList); // ok
			request.setAttribute("edgeList", edgeList);
			request.setAttribute("centerObservationMap", centerObservationMap); // ok
			request.setAttribute("observationList", observationList); // ok

			requestDispatcher.forward(request, response);
		} catch (Exception ex) {

		}

	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

	}

	public RoadNetworkVTrackAlgorithm getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(RoadNetworkVTrackAlgorithm algorithm) {
		this.algorithm = algorithm;
	}

}
