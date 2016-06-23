package programs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import be.ac.ulg.montefiore.run.jahmm.Hmm;
import be.ac.ulg.montefiore.run.jahmm.ObservationVector;
import be.ac.ulg.montefiore.run.jahmm.OpdfMultiGaussian;

import com.erdem.model.Algorithm;
import com.erdem.model.EdgeNodeRN;
import com.erdem.model.NodeRN;
import com.erdem.model.ObservationRN;
import com.erdem.utility.GraphUtility;

public class RoadNetworkVTrackAlgorithm extends Algorithm {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int keepRate;
	private double threshold;
	private double covariance;
	private int sampleRate;
	
	// but for these ones it is not a good idea to get them through servlet.
	private List<ObservationRN> observationsFiltered;
	
	private HashMap<String, NodeRN> filteredMap;
	
	public RoadNetworkVTrackAlgorithm(int keepRate, double threshold, double covariance, int sampleRate)
	{
		this.setKeepRate(keepRate);
		this.setThreshold(threshold);
		this.setCovariance(covariance);
		this.setSampleRate(sampleRate);
	}
	
	
	@Override
	public List<EdgeNodeRN> computeEdges(List<ObservationRN> observations) {
		return computePathEdges(observations, keepRate, threshold, covariance, sampleRate);
	}
	
	public List<EdgeNodeRN> computePathEdges(List<ObservationRN> observations,
			int keepRate, 
			double threshold, 
			double covariance,
			int sampleRate) {
		

		// filter observations.
		observationsFiltered = new ArrayList<ObservationRN>();
		for (int i = 0; i < observations.size(); i += keepRate) {
			observationsFiltered.add(observations.get(i));
		}
		
		// if last item is not included into observations list, add it as well.
		if((observations.size()-1) % keepRate != 0)
		{
			observationsFiltered.add(observations.get(observations.size()-1));
		}
		
		
		

		GraphUtility utility = new GraphUtility();

		setFilteredMap(utility.filterNodesAroundEachObservation(getNewRoadMap(),
				observationsFiltered,
				threshold));
		
		
		HashMap<String, EdgeNodeRN> edges = utility
				.convertToAnEdgeGraph(filteredMap);
		
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

				if (source == target
						|| source.getNodeTarget() == target.getNodeSource()) {
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
			mean[0] = (edgesList.get(i).getNodeSource().getLatitude() + edgesList
					.get(i).getNodeTarget().getLatitude()) / 2;
			mean[1] = (edgesList.get(i).getNodeSource().getLongitude() + edgesList
					.get(i).getNodeTarget().getLongitude()) / 2;

			double[][] variance = new double[2][2];
			variance[0][0] = covariance;
			variance[0][1] = 0;
			variance[1][0] = 0;
			variance[1][1] = covariance;

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
					/ (sampleRateInt * keepRate);

			double lonDelta = (next.getLon() - prev.getLon())
					/ (sampleRateInt * keepRate);

			for (int j = 0; j < sampleRateInt * keepRate; j++) {
				oSeq.add(new ObservationVector(new double[] {
						prev.getLat() + latDelta * j,
						prev.getLon() + lonDelta * j }));
			}

			oSeq.add(new ObservationVector(new double[] { next.getLat(),
					next.getLon() }));
		}

		int[] sequence = hmm.mostLikelyStateSequence(oSeq);

		
		List<EdgeNodeRN> edgesResult = new ArrayList<EdgeNodeRN>();
		// preparation of sequence list
		for (int i = 0; i < sequence.length; i++) {
			edgesResult.add(edgesList.get(sequence[i]));
		}
		return edgesResult;
	}


	public HashMap<String, NodeRN> getFilteredMap() {
		return filteredMap;
	}


	public void setFilteredMap(HashMap<String, NodeRN> filteredMap) {
		this.filteredMap = filteredMap;
	}


	public List<ObservationRN> getObservationsFiltered() {
		return observationsFiltered;
	}


	public void setObservationsFiltered(List<ObservationRN> observationsFiltered) {
		this.observationsFiltered = observationsFiltered;
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


	public double getCovariance() {
		return covariance;
	}


	public void setCovariance(double covariance) {
		this.covariance = covariance;
	}


	public int getSampleRate() {
		return sampleRate;
	}


	public void setSampleRate(int sampleRate) {
		this.sampleRate = sampleRate;
	}
	
	
}
