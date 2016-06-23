package programs;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.erdem.datasetparser.DatasetLoader;
import com.erdem.model.EdgeNodeRN;
import com.erdem.model.ObservationRN;
import com.erdem.model.PathGroundTruth;

/**
 * This class will extract ground truths from user's data.
 * @author erdem
 *
 */
public class GroundTruthExtraction {
	
	
	public static void main(String[] args) throws Exception
	{
		// initialization code.
		DatasetLoader loader = new DatasetLoader();
		
		// these are default parameters.
		RoadNetworkNoMarkovAlgorithm algorithm = new RoadNetworkNoMarkovAlgorithm(10, 10, 1, 0.05, true);
		algorithm.init();
		

		List<PathGroundTruth> groundTruth = new ArrayList<PathGroundTruth>();
		
		// parse benchmarkTests.
		
		List<String> benchmarks = loader.readFile("./benchmarkTests");
		
		
		for(String benchmark : benchmarks)
		{
			
			System.out.println(benchmark);
			
			String[] benchmarkSplit = benchmark.split("-");
			
			String driver = benchmarkSplit[0];
			
			int start = new Integer(benchmarkSplit[1]);
			int end = new Integer(benchmarkSplit[2]);
			int keepRate = new Integer(benchmarkSplit[3]);
			double standardDev = new Double(benchmarkSplit[4]);
			
			
			List<ObservationRN> observationsOfTaxiDriver;

			File f = new File("driver" + driver);

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
			
			PathGroundTruth path = new PathGroundTruth();
			
			path.setUserId(driver);
			
			path.getObservations().addAll(observationsOfTaxiDriver.subList(start, end));
			
			path.setStartObservation(start);
			
			path.setEndObservation(end);
			
			List<EdgeNodeRN> edges = algorithm.computePathEdges(path.getObservations(), 
					keepRate, 0.05, standardDev, 10);
			
			List<String> edgeIds = new ArrayList<String>();
			
			for(EdgeNodeRN edgeNodeRN : edges)
			{
				edgeIds.add(edgeNodeRN.getId());
			}
			
			path.setEdges(edgeIds);
			
			groundTruth.add(path);
			
		}
	
		
		loader.serialize(groundTruth, "groundTruth");
	}
	
	
	
}
