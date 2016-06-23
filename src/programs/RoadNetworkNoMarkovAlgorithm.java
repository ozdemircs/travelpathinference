package programs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import be.ac.ulg.montefiore.run.distributions.GaussianDistribution;

import com.erdem.main.AverageRoadDistanceMerger;
import com.erdem.model.Algorithm;
import com.erdem.model.DistanceHolder;
import com.erdem.model.EdgeNodeRN;
import com.erdem.model.NodeRN;
import com.erdem.model.ObservationRN;
import com.erdem.model.PointRN;
import com.erdem.utility.GraphUtility;
import com.erdem.utility.Utility;

public class RoadNetworkNoMarkovAlgorithm extends Algorithm {

	
	public static double thresholdMinSpeed = 3;
	public static double thresholdMaxSpeed = 45;
	
	/**
	 * Normally you should not be using anything in here , except the ones that
	 * are initialized again and again in every call. standardDev,
	 * numberOfNeighbors, keepRate and threshold are good.
	 */
	private static final long serialVersionUID = 1L;

	private double standardDev;

	private int numberOfNeighbors;

	private int keepRate;

	private double threshold;

	// but for these ones it is not a good idea to get them through servlet.
	private List<ObservationRN> observationsFiltered;

	private HashMap<String, NodeRN> filteredMap;

	private boolean time = false;
	
	private AverageRoadDistanceMerger speedStorage;

	/**
	 * 
	 * Number of neighbors to evaluate near the observation. Keep rate for the
	 * observations, having larger value for this will yield less number of
	 * observations. and threshold - is used to filter map so that
	 * 
	 * @param standardDev
	 *            is standard deviation of distance of observations to nodes.
	 * @param numberOfNeighbors
	 *            is the number of neighbors that should be evaluated for each
	 *            observation.
	 * @param keepRate
	 *            - is the frequency to keep observations.
	 * @param threshold
	 *            - is the value for filtering nodes that have larger distance
	 *            than this value from observations
	 */
	public RoadNetworkNoMarkovAlgorithm(double standardDev,
			int numberOfNeighbors, int keepRate, double threshold) {

		this.setStandardDev(standardDev);
		this.setNumberOfNeighbors(numberOfNeighbors);
		this.setKeepRate(keepRate);
		this.setThreshold(threshold);
		this.setTime(false);
	}

	public RoadNetworkNoMarkovAlgorithm(double standardDev,
			int numberOfNeighbors, int keepRate, double threshold, boolean time) {

		this.setStandardDev(standardDev);
		this.setNumberOfNeighbors(numberOfNeighbors);
		this.setKeepRate(keepRate);
		this.setThreshold(threshold);
		this.setTime(time);
		

	}

	@Override
	public void init() {
		super.init();
	}

	/**
	 * It does filtering for given thresold. It does filtering of observations
	 * given keepRate It does only use number of candidates for each
	 * observation. given in number of neighbors
	 */
	public List<EdgeNodeRN> computeEdges(List<ObservationRN> observations) {
		return computePathEdges(observations, this.getKeepRate(),
				this.getThreshold(), this.getStandardDev(),
				this.getNumberOfNeighbors());
	}

	public List<EdgeNodeRN> computePathEdges(List<ObservationRN> observations,
			int keepRate, double threshold, double standardDev,
			int numberOfNeighbors) {

		// filter observations.
		observationsFiltered = new ArrayList<ObservationRN>();
		for (int i = 0; i < observations.size(); i += keepRate) {
			observationsFiltered.add(observations.get(i));
		}

		// if last item is not included into observations list, add it as well.
		if ((observations.size() - 1) % keepRate != 0) {
			observationsFiltered.add(observations.get(observations.size() - 1));
		}

		double minLat = Integer.MAX_VALUE;
		double maxLat = Integer.MIN_VALUE;
		double minLon = Integer.MAX_VALUE;
		double maxLon = Integer.MIN_VALUE;

		/**
		 * find min and max lat lon longitudes.
		 */
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
		/**
		 * filter map with the given threshold.
		 */
		GraphUtility utility = new GraphUtility();

		// here we set it on class level which is useful in web project
		HashMap<String, NodeRN> filterNodes = utility.filterNodes(
				getNewRoadMap(), minLat - threshold, maxLat + threshold, minLon
						- threshold, maxLon + threshold);

		if (filterNodes.size() == 0) {
			throw new RuntimeException("There is no road map in this area.");
		}

		setFilteredMap(filterNodes);

		// create an edge graph
		HashMap<String, EdgeNodeRN> edges = utility
				.convertToAnEdgeGraph(getFilteredMap());

		List<EdgeNodeRN> edgesList = new ArrayList<EdgeNodeRN>(edges.values());

		if (edgesList.size() == 0) {
			throw new RuntimeException("There is no road in this area.");
		}

		// these are the possible numberOfNeighbors edges for each observation.
		EdgeNodeRN candidates[][] = new EdgeNodeRN[observationsFiltered.size()][numberOfNeighbors];

		// scores i - j means max possible score given ith observation to be
		// produced by jth.
		double scores[][] = new double[observationsFiltered.size()][numberOfNeighbors];

		// previous nodes ij means the previous selection to have max score.
		int previousNodes[][] = new int[observationsFiltered.size()][numberOfNeighbors];

		// compute candidates
		fillCandidatesToObservations(observationsFiltered, edgesList,
				numberOfNeighbors, candidates);

		// computing scores.
		computeScoresAndPreviousNodes(standardDev, observationsFiltered,
				numberOfNeighbors, candidates, scores, previousNodes, keepRate);

		// find max path
		int maxPathIndex = -1;

		double maxPathScore = Double.NEGATIVE_INFINITY;

		for (int i = 0; i < numberOfNeighbors; i++) {

			if (scores[scores.length - 1][i] > maxPathScore) {
				maxPathScore = scores[scores.length - 1][i];
				maxPathIndex = i;
			}
		}

		// when you have maxPathIndex just follow previous nodes index
		List<EdgeNodeRN> wholeEdges = new ArrayList<EdgeNodeRN>();
		for (int i = observationsFiltered.size() - 1; i >= 0; i--) {

			if (maxPathIndex < 0) {
				throw new RuntimeException("Path could not be found.");
			}

			EdgeNodeRN edge = candidates[i][maxPathIndex];

			wholeEdges.add(edge);

			maxPathIndex = previousNodes[i][maxPathIndex];
		}

		Collections.reverse(wholeEdges);

		List<EdgeNodeRN> pathEdges = new ArrayList<EdgeNodeRN>();


		for (int i = wholeEdges.size() - 1; i > 0; i--) {

			EdgeNodeRN source = wholeEdges.get(i - 1);

			EdgeNodeRN next = wholeEdges.get(i);

			PointRN sourceProjection = Utility.findClosestOnLine(source
					.getNodeSource().getLatitude(), source.getNodeSource()
					.getLongitude(), source.getNodeTarget().getLatitude(),
					source.getNodeTarget().getLongitude(), observationsFiltered
							.get(i - 1).getLat(),
					observationsFiltered.get(i - 1).getLon());

			PointRN targetProjection = Utility.findClosestOnLine(next
					.getNodeSource().getLatitude(), next.getNodeSource()
					.getLongitude(), next.getNodeTarget().getLatitude(), next
					.getNodeTarget().getLongitude(), observationsFiltered
					.get(i).getLat(), observationsFiltered.get(i).getLon());
			
			double timeDifference = (observationsFiltered.get(i).getTime() - observationsFiltered
					.get(i - 1).getTime()) / (1000.0);
			
			DistanceHolder<EdgeNodeRN> holder = isTime() ? computeScoreWithTime(
					source, next, sourceProjection, targetProjection, timeDifference)
					: computeScore(source, next, sourceProjection,
							targetProjection);

			double totalDistance = holder.distance;
			// time difference in seconds.


			double averageSpeed = totalDistance / timeDifference;

			List<EdgeNodeRN> edgesForAddingSpeed = new ArrayList<EdgeNodeRN>();

			// adds except source.
			while (holder.parent != null) {
				edgesForAddingSpeed.add(holder.node);
				pathEdges.add(holder.node);
				holder = holder.parent;
			}

			edgesForAddingSpeed.add(holder.node);

			if (averageSpeed >= thresholdMinSpeed && averageSpeed <= thresholdMaxSpeed) {
				for (EdgeNodeRN edge : edgesForAddingSpeed) {
					edge.getSpeeds().add(averageSpeed);
				}
			}
		}

		EdgeNodeRN sourceEdge = wholeEdges.get(0);

		pathEdges.add(sourceEdge);

		Collections.reverse(pathEdges);

		return pathEdges;
	}

	private void computeScoresAndPreviousNodes(double standardDev,
			List<ObservationRN> observationsFiltered, int numberOfNeighbors,
			EdgeNodeRN[][] candidates, double[][] scores,
			int[][] previousNodes, int keepRate) {

		GaussianDistribution distribution = new GaussianDistribution(0,
				standardDev * standardDev);

		for (int i = 0; i < observationsFiltered.size(); i++) {

			ObservationRN o = observationsFiltered.get(i);

			for (int j = 0; j < numberOfNeighbors; j++) {

				EdgeNodeRN edge = candidates[i][j];

				if (edge != null) {

					// compute distance of edge to observation
					double distance = Utility.closestDistance(edge
							.getNodeSource().getLatitude(), edge
							.getNodeSource().getLongitude(), edge
							.getNodeTarget().getLatitude(), edge
							.getNodeTarget().getLongitude(), o.getLat(), o
							.getLon());

					if (i == 0) {
						// scores ij = observation i to be generated from edge
						// j.

						scores[i][j] = Math.log(distribution
								.probability(distance));
						previousNodes[i][j] = -1;
					} else {
						double maxScore = Double.NEGATIVE_INFINITY;
						int maxScoreIndex = -1;

						PointRN targetProjection = Utility
								.findClosestOnLine(candidates[i][j]
										.getNodeSource().getLatitude(),
										candidates[i][j].getNodeSource()
												.getLongitude(),
										candidates[i][j].getNodeTarget()
												.getLatitude(),
										candidates[i][j].getNodeTarget()
												.getLongitude(),
										observationsFiltered.get(i).getLat(),
										observationsFiltered.get(i).getLon());

						for (int k = 0; k < numberOfNeighbors; k++) {

							if (candidates[i - 1][k] != null) {

								PointRN sourceProjection = Utility
										.findClosestOnLine(candidates[i - 1][k]
												.getNodeSource().getLatitude(),
												candidates[i - 1][k]
														.getNodeSource()
														.getLongitude(),
												candidates[i - 1][k]
														.getNodeTarget()
														.getLatitude(),
												candidates[i - 1][k]
														.getNodeTarget()
														.getLongitude(),
												observationsFiltered.get(i - 1)
														.getLat(),
												observationsFiltered.get(i - 1)
														.getLon());

								ObservationRN observationAtIMin1 = observationsFiltered
										.get(i - 1);

								double timeInSeconds = (o.getTime() - observationAtIMin1
										.getTime()) / 1000.0;

								DistanceHolder<EdgeNodeRN> path = isTime() ?

								computeScoreWithTime(candidates[i - 1][k],
										candidates[i][j], sourceProjection,
										targetProjection, timeInSeconds)

								: computeScore(candidates[i - 1][k],
										candidates[i][j], sourceProjection,
										targetProjection);

								double scoreForKToJ = (path == null) ? 0
										: (Math.exp(-path.distance / keepRate));

								double score = scores[i - 1][k]
										+ Math.log(scoreForKToJ);

								if (score > maxScore) {
									maxScore = score;
									maxScoreIndex = k;
								}
							}

						}

						scores[i][j] = Math.log(distribution
								.probability(distance) + 1e-15) + maxScore;

						previousNodes[i][j] = maxScoreIndex;

					}
				} else {
					scores[i][j] = 0;
					previousNodes[i][j] = -1;
				}

			}
		}
	}

	/**
	 * Computes candidate list for each observations. For each of them chooses
	 * 10 closest edge.
	 * 
	 * @param observationsFiltered
	 * @param edgesList
	 * @param numberOfNeighbors
	 * @param candidates
	 */
	private void fillCandidatesToObservations(
			List<ObservationRN> observationsFiltered,
			List<EdgeNodeRN> edgesList, int numberOfNeighbors,
			EdgeNodeRN[][] candidates) {
		// so lets first find neighbors to the edges.
		for (int i = 0; i < observationsFiltered.size(); i++) {

			ObservationRN o = observationsFiltered.get(i);

			PriorityQueue<DistanceHolder<EdgeNodeRN>> distances = new PriorityQueue<DistanceHolder<EdgeNodeRN>>();

			for (EdgeNodeRN edge : edgesList) {

				DistanceHolder<EdgeNodeRN> distanceNode = new DistanceHolder<EdgeNodeRN>();

				distanceNode.node = edge;

				distanceNode.distance = Utility.closestDistance(edge
						.getNodeSource().getLatitude(), edge.getNodeSource()
						.getLongitude(), edge.getNodeTarget().getLatitude(),
						edge.getNodeTarget().getLongitude(), o.getLat(), o
								.getLon());

				distances.add(distanceNode);
			}

			for (int j = 0; j < numberOfNeighbors; j++) {
				if (distances.size() > 0) {
					candidates[i][j] = distances.remove().node;
				}
			}
		}
	}

	public double decideDistance(EdgeNodeRN edge, EdgeNodeRN source,
			EdgeNodeRN target, PointRN sourcePoint, PointRN targetPoint) {

		if (edge == source && edge == target) {
			return Utility.distFrom(sourcePoint.getLatitude(),
					sourcePoint.getLongitude(), targetPoint.getLatitude(),
					targetPoint.getLongitude());
		} else if (edge == source) {
			return Utility.distFrom(sourcePoint.getLatitude(), sourcePoint
					.getLongitude(), source.getNodeTarget().getLatitude(),
					source.getNodeTarget().getLongitude());
		} else if (edge == target) {
			return Utility.distFrom(target.getNodeSource().getLatitude(),
					target.getNodeSource().getLongitude(),
					targetPoint.getLatitude(), targetPoint.getLongitude());
		} else {
			return Utility.distFrom(edge.getNodeSource().getLatitude(), edge
					.getNodeSource().getLongitude(), edge.getNodeTarget()
					.getLatitude(), edge.getNodeTarget().getLongitude());
		}

	}

	private DistanceHolder<EdgeNodeRN> computeScore(EdgeNodeRN source,
			EdgeNodeRN target, PointRN sourcePoint, PointRN targetPoint) {

		DistanceHolder<EdgeNodeRN> sourceWithDistance = new DistanceHolder<EdgeNodeRN>();

		sourceWithDistance.node = source;
		sourceWithDistance.distance = decideDistance(sourceWithDistance.node,
				source, target, sourcePoint, targetPoint);

		Set<EdgeNodeRN> expandedNodes = new HashSet<EdgeNodeRN>();

		PriorityQueue<DistanceHolder<EdgeNodeRN>> queue = new PriorityQueue<DistanceHolder<EdgeNodeRN>>();
		queue.add(sourceWithDistance);

		while (!queue.isEmpty()) {

			DistanceHolder<EdgeNodeRN> candidate = queue.remove();

			if (candidate.node == target) {
				return candidate;
			} else if (!expandedNodes.contains(candidate.node)) {

				for (EdgeNodeRN neighbor : candidate.node.getNeighbors()) {

					DistanceHolder<EdgeNodeRN> neighborWithDistance = new DistanceHolder<EdgeNodeRN>();
					neighborWithDistance.node = neighbor;
					neighborWithDistance.parent = candidate;
					neighborWithDistance.distance = candidate.distance
							+ decideDistance(candidate.node, source, target,
									sourcePoint, targetPoint);
					queue.add(neighborWithDistance);

				}
				expandedNodes.add(candidate.node);
			}
		}

		return null;
	}
	
	
	protected double computeTimeFactor(double averageSpeed, DistanceHolder<EdgeNodeRN> neighborWithDistance)
	{
		
		List<Double> speeds = new ArrayList<Double>();
		
		DistanceHolder<EdgeNodeRN> edge = neighborWithDistance;
		
		while(edge != null)
		{
			double averageSpeedFromStorage = getSpeedStorage().getSpeed(edge.node.getId(), averageSpeed);	
			
			if(averageSpeedFromStorage > 0)
			{
				speeds.add(averageSpeedFromStorage);
			}
			edge = edge.parent;

		}
		// create arrays.
		
		double[] vector1 = new double[speeds.size()];
		double[] vector2 = new double[speeds.size()];
		
		for(int i = 0; i < vector1.length; i++)
		{
			vector1[i] = speeds.get(i);
			vector2[i] = averageSpeed;
		}
		
		return 1 + computeDistanceBetweenSpeedVectors(vector1, vector2);
	}
	
	
	/**
	 * Lets use cosine distance then.
	 * @param vector1
	 * @param vector2
	 * @return
	 */
	protected double computeDistanceBetweenSpeedVectors(double[] vector1, double[] vector2)
	{
		
		if(vector1.length == 0)
		{
			return 0;
		}
		
		double topMagnitude = 0;
		double vector1Magnitude = 0;
		double vector2Magnitude = 0;
		
		for(int i = 0; i < vector1.length; i++)
		{
			topMagnitude += (vector1[i] * vector2[i]);
			
			vector1Magnitude += vector1[i] * vector1[i];
			
			vector2Magnitude += vector2[i] * vector2[i];
		}
		
		vector2Magnitude = Math.sqrt(vector2Magnitude);
		vector1Magnitude = Math.sqrt(vector1Magnitude);
		
		return 1 - (topMagnitude / (vector1Magnitude * vector2Magnitude));
		
	}

	private DistanceHolder<EdgeNodeRN> computeScoreWithTime(EdgeNodeRN source,
			EdgeNodeRN target, PointRN sourcePoint, PointRN targetPoint,
			double seconds) {

		DistanceHolder<EdgeNodeRN> sourceWithDistance = new DistanceHolder<EdgeNodeRN>();

		sourceWithDistance.node = source;
		sourceWithDistance.distance = decideDistance(sourceWithDistance.node,
				source, target, sourcePoint, targetPoint);


		// if it is just a single node lets dont look at time. since cosine distance for instance would return 0.

		Set<EdgeNodeRN> expandedNodes = new HashSet<EdgeNodeRN>();

		PriorityQueue<DistanceHolder<EdgeNodeRN>> queue = new PriorityQueue<DistanceHolder<EdgeNodeRN>>();
		queue.add(sourceWithDistance);

		while (!queue.isEmpty()) {

			DistanceHolder<EdgeNodeRN> candidate = queue.remove();

			if (candidate.node == target) {
				return candidate;
			} else if (!expandedNodes.contains(candidate.node)) {

				for (EdgeNodeRN neighbor : candidate.node.getNeighbors()) {

					DistanceHolder<EdgeNodeRN> neighborWithDistance = new DistanceHolder<EdgeNodeRN>();
					neighborWithDistance.node = neighbor;
					neighborWithDistance.parent = candidate;
					
					neighborWithDistance.distance = candidate.distance
							+ decideDistance(candidate.node, source, target,
									sourcePoint, targetPoint);
					
					if(neighborWithDistance.node == target)
					{
						// double average speed.
						double averageSpeed = neighborWithDistance.distance / seconds;
						
						double timeFactor = computeTimeFactor(averageSpeed, neighborWithDistance);
						
						neighborWithDistance.distance *= timeFactor;
						
					}
					
					
					
					queue.add(neighborWithDistance);

				}
				expandedNodes.add(candidate.node);
			}
		}

		return null;
	}

	public int getNumberOfNeighbors() {
		return numberOfNeighbors;
	}

	public void setNumberOfNeighbors(int numberOfNeighbors) {
		this.numberOfNeighbors = numberOfNeighbors;
	}

	public int getKeepRate() {
		return keepRate;
	}

	public void setKeepRate(int keepRate) {
		this.keepRate = keepRate;
	}

	public double getThreshold() {
		return threshold;
	}

	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}

	public double getStandardDev() {
		return standardDev;
	}

	public void setStandardDev(double standardDev) {
		this.standardDev = standardDev;
	}

	public List<ObservationRN> getObservationsFiltered() {
		return observationsFiltered;
	}

	public void setObservationsFiltered(List<ObservationRN> observationsFiltered) {
		this.observationsFiltered = observationsFiltered;
	}

	public HashMap<String, NodeRN> getFilteredMap() {
		return filteredMap;
	}

	public void setFilteredMap(HashMap<String, NodeRN> filteredMap) {
		this.filteredMap = filteredMap;
	}

	@Override
	public String getParameters() {
		// TODO Auto-generated method stub
		return "Standard Dev: " + this.getStandardDev() + " keepRate: "
				+ this.getKeepRate() + "  threshold: " + threshold
				+ " numberOfNeighbors: " + numberOfNeighbors;
	}

	public boolean isTime() {
		return time;
	}

	public void setTime(boolean time) {
		this.time = time;
	}

	public AverageRoadDistanceMerger getSpeedStorage() {
		
		if(time && speedStorage == null)
		{
			speedStorage = new AverageRoadDistanceMerger();
			speedStorage.load(thresholdMinSpeed, thresholdMaxSpeed);
		}
		
		return speedStorage;
	}

	public void setSpeedStorage(AverageRoadDistanceMerger speedStorage) {
		this.speedStorage = speedStorage;
	}

}
