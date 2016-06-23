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
import com.erdem.model.Benchmark;
import com.erdem.model.EdgeNodeRN;
import com.erdem.model.NodeRN;
import com.erdem.model.ObservationRN;
import com.erdem.model.PathGroundTruth;

/**
 * Servlet implementation class RoadNetwork.
 */
@WebServlet("/RoadNetworkNoMarkov")
public class RoadNetworkNoMarkov extends HttpServlet {

	private static final long serialVersionUID = 1L;

	RoadNetworkNoMarkovAlgorithm algorithm;
	
	List<PathGroundTruth> list;
	
	

	@Override
	public void init() throws ServletException {

		super.init();

		algorithm = new RoadNetworkNoMarkovAlgorithm(10, 10, 1, 0.05);

		algorithm.init();
		
		DatasetLoader loader = new DatasetLoader();
		
		list = (List<PathGroundTruth>) loader
					.deserialize("/mnt/RoadNetworkWeb/groundTruth");

	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		
		
		long startT = System.currentTimeMillis();
		
		/**
		 * Keep rate so we can made it not fine sampled.
		 */
		String keepRate = "1";
		String standardDev = "10";
		String start = "5";
		String end = "10";
		String driver = "152";
		int neighbors = 10;

		if (request.getParameter("driver") != null) {
			driver = request.getParameter("driver");
		}

		if (request.getParameter("neighbors") != null) {
			neighbors = new Integer(request.getParameter("neighbors"));
		}

		if (request.getParameter("standardDev") != null) {
			standardDev = request.getParameter("standardDev");
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
		
		if (request.getParameter("time") != null) {
			algorithm.setTime(true);
		}
		else
		{
			algorithm.setTime(false);
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
						.getObservationSequences(
								driver,
								"/mnt/RoadNetworkWeb/WebContent/WEB-INF/taxi_february.txt");

				loader.serialize(observations,
						"/mnt/RoadNetworkWeb/driver"
								+ driver);
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

		int tempCursor = startInt;
		while (tempCursor < endInt) {
			observationsFiltered.add(observations.get(tempCursor));
			tempCursor += 1;
		}
		ObservationRN obs = new ObservationRN();
		obs.setLat(observationsFiltered.get(0).getLat()/2 + observationsFiltered.get(1).getLat()/2);
		obs.setLon(observationsFiltered.get(0).getLon()/2 + observationsFiltered.get(1).getLon()/2);
		observationsFiltered.add(1, obs);
		

		List<EdgeNodeRN> pathEdges = algorithm.computePathEdges(
				observationsFiltered, keepRateInt, 0.05,
				new Double(standardDev), neighbors);
		// according to keep rate
		observationsFiltered = algorithm.getObservationsFiltered();

		HashMap<String, NodeRN> filteredMap = algorithm.getFilteredMap();

		List<String> edgeList = new ArrayList<String>();

		for (int i = 0; i < pathEdges.size(); i++) {

			EdgeNodeRN edge = pathEdges.get(i);

			System.out.println(edge);

			edgeList.add(edge.getNodeSource().getLongitude() + ","
					+ edge.getNodeSource().getLatitude());
			edgeList.add(edge.getNodeTarget().getLongitude() + ","
					+ edge.getNodeTarget().getLatitude());

		}

		/**
		 * Preparation of observation list.
		 */

		List<String> observationList = new ArrayList<String>();

		for (ObservationRN o : observationsFiltered) {
			observationList.add(o.getLon() + "," + o.getLat());
		}

		String centerObservationMap = observationsFiltered.get(0).getLon()
				+ "," + observationsFiltered.get(0).getLat();

		/**
		 * Preparation of nodeList
		 */
		List<String> nodeList = new ArrayList<String>();
		for (NodeRN node : filteredMap.values()) {
			nodeList.add(node.getLongitude() + "," + node.getLatitude());
		}

		// now load links

		List<String> fileContent = loader
				.readFile("/mnt/RoadNetworkWeb/benchmarkTests");

		List<String> linkList = new ArrayList<String>();

		for (String line : fileContent) {
			String[] params = line.split("-");
			String format = "RoadNetworkNoMarkov?driver=" + params[0]
					+ "&start=" + params[1] + "&end=" + params[2]
					+ "&standardDev=" + params[3] + "&evaluate=true";
			
			linkList.add(format);
			
		}
		double accuracy = 0;
		// evaluate.
		if (request.getParameter("evaluate") != null) {
			// evaluate 
			
			for(PathGroundTruth path : list)
			{
				if(path.getUserId().equals(driver) && path.getStartObservation() == startInt && path.getEndObservation() == endInt)
				{
					
					List<String> foundEdges = new ArrayList<String>();
					for(EdgeNodeRN edge : pathEdges)
					{
						foundEdges.add(edge.getId());
					}
					
					int[] results = Benchmark.doAnalysis(foundEdges, path.getEdges());
					accuracy = 1.0 * results[0] / results[1];
				}	
			}
			
			
			
		}

		long tEnd = System.currentTimeMillis();
		long tDelta = tEnd - startT;
		
		System.out.println(tDelta);
		
		try {
			// simulate a database query

			// get UI
			RequestDispatcher requestDispatcher = request
					.getRequestDispatcher("edges.vm");
			request.setAttribute("linkList", linkList);
			request.setAttribute("nodeList", nodeList);
			request.setAttribute("edgeList", edgeList);
			request.setAttribute("centerObservationMap", centerObservationMap);
			request.setAttribute("observationList", observationList);
			request.setAttribute("accuracy", accuracy);
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

}
