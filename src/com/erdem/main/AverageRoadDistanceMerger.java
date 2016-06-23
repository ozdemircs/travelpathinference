package com.erdem.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.clustering.KMeans;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import net.sf.javaml.tools.InstanceTools;

import com.erdem.datasetparser.DatasetLoader;

public class AverageRoadDistanceMerger {
	
	private HashMap<String, Double> averages = null;
	
	private HashMap<String, Double[]> speedClusters = null;
	
	/**
	 * Load averages file.
	 */
	public void load(double minThreshold, double maxThreshold)
	{
		averages = new HashMap<String, Double>();  
		speedClusters = new HashMap<String, Double[]>();
		
		List<String> fileNames = new ArrayList<String>();
		
		fileNames.add("/mnt/RoadNetworkWeb/averages0_30");
		fileNames.add("/mnt/RoadNetworkWeb/averages30_60");
		fileNames.add("/mnt/RoadNetworkWeb/averages60_90");
		fileNames.add("/mnt/RoadNetworkWeb/averages90_120");
		fileNames.add("/mnt/RoadNetworkWeb/averages120_150");
		fileNames.add("/mnt/RoadNetworkWeb/averages150_180");
		
		DatasetLoader loader = new DatasetLoader();
		
		for(String file : fileNames)
		{
			HashMap<String, List<Double>> average = (HashMap<String, List<Double>>) loader.deserialize(file);
			
			for(String key : average.keySet())
			{
				
				List<Double> values = new ArrayList<Double>();
				
				double averageSpeed = 0;
				int count = 0;
				
				
				
				for(Double d : average.get(key))
				{
					
					
					if(d <= maxThreshold && d >= minThreshold)
					{
						averageSpeed += d;
						count++;
						values.add(d);
					}
				}
				
				if(count > 0)
				{
					averages.put(key, averageSpeed/count);
				}
				
				if(count > 10)
				{
					
					Dataset data = new DefaultDataset();
					for (Double d : values) {
					    Instance tmpInstance = new DenseInstance(new double[]{d});
					    data.add(tmpInstance);
					}
					// create cluster.
					Clusterer km = new KMeans(3);
					Dataset[] clusters = km.cluster(data);
					Double[] averages = new Double[clusters.length];
					for(int i = 0; i < clusters.length; i++)
					{
						double averageInCluster = 0;
						double countInCluster = 0;
						for(Instance instance : clusters[i])
						{
							averageInCluster += instance.value(0);
							countInCluster += 1;
						}
						averageInCluster /= countInCluster;
						averages[i] = averageInCluster;
					}
					
					speedClusters.put(key, averages);
					
				}
			}
		}
	}
	
	/**
	 * Return list of doubles as distances.
	 * @param key
	 * @return
	 */
	public double getSpeed(String key)
	{
		
		if(averages.get(key) == null)
		{
			return -1;
		}
		else
		{
			return averages.get(key);
		}
		
	}
	
	private static double total = 0;
	private static double found = 0;
	
	public double getSpeed(String key, double average)
	{
		
		total++;
		if(speedClusters.get(key) != null)
		{
			found++;
			
			System.out.println(found/total);
			// find closest 
			Double[] clusterCenters = speedClusters.get(key);
			Double closest = clusterCenters[0];
			for(int i = 0; i < clusterCenters.length; i++)
			{
				if(Math.abs(clusterCenters[i] - average) < Math.abs(closest - average))
				{
					closest = clusterCenters[i];
				}
			}
			return closest;
			
		}
		else
		{
			return getSpeed(key);
			
		}
		

	}
	
}
