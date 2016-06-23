package com.erdem.main;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import programs.RoadNetworkNoMarkovAlgorithm;

import com.erdem.datasetparser.DatasetLoader;
import com.erdem.model.EdgeNodeRN;
import com.erdem.model.ObservationRN;

public class AverageRoadDistanceFinder {
	
	public static void main(String args[]) throws Exception
	{

		int startDriver = new Integer(args[0]);
		int endDriver = new Integer(args[1]);
		
		DatasetLoader loader = new DatasetLoader();
		List<String> drivers = null;

		File f = new File("drivers");

		if (!f.exists()) {
			drivers = loader
					.loadTaxiDrivers("/mnt/RoadNetworkWeb/WebContent/WEB-INF/taxi_february.txt");
			loader.serialize(drivers, "drivers");
		} else {
			drivers = (List<String>) loader.deserialize("drivers");
		}


		// set standard deviation as 1
		// number of neighbors as 10
		// keep rate is 1
		// and 0.02 lat lon distance is the area to be considered.
		RoadNetworkNoMarkovAlgorithm algorithm = new RoadNetworkNoMarkovAlgorithm(10, 10, 1, 0.02);
		
		algorithm.init();
		
		HashMap<String, List<Double>> averages = new HashMap<String, List<Double>>(); 
		
		
		System.out.println("Total number of drivers is " + drivers.size());
		for (int driverIndex = startDriver; driverIndex < endDriver; driverIndex++) {

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
			
			System.out.println("Total observation size " + observationsOfTaxiDriver.size());
			
			for (int start = 0; start <= observationsOfTaxiDriver.size() - 10; start += 10) {
								
				try{
					List<ObservationRN> observations = observationsOfTaxiDriver
							.subList(start, start + 10);
					
					List<EdgeNodeRN> edges = algorithm.computeEdges(observations);
					
					for(EdgeNodeRN edge : edges)
					{
						
						
						if(averages.get(edge.getId()) == null)
						{
							averages.put(edge.getId(), new ArrayList<Double>());
						}
						
						List<Double> averageList = averages.get(edge.getId());
						averageList.addAll(edge.getSpeeds());
					}
					
					System.out.printf("driver %s start %d end %d succeeded.\n", driver, start, start+10);
					
				}
				catch(Exception e)
				{
					System.out.printf("driver %s start %d end %d failed.\n", driver, start, start+10);
					System.out.println(e.getMessage());
				}
				
			}

		}
		
		loader.serialize(averages, "averages" + startDriver + "_" + endDriver);
		
	}
}
