package com.erdem.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
/**
 * Models a single run with given algorithm,
 * Paths as ground truth
 * 
 * Also correctEstimates and totalEdges.
 * 
 * @author erdem
 */
public class Benchmark {
	
	private List<PathGroundTruth> paths; 
	private Algorithm algorithm;
	private double correctEstimates = 0;
	private double totalEdges = 0;
	
	public Benchmark(List<PathGroundTruth> paths, Algorithm algorithm)
	{
		this.setPaths(paths);
		this.setAlgorithm(algorithm);
	}
	
	/**
	 * start run calls init of the algorithm.
	 * and for each path, it computes edges and compares those with ground truth edges.
	 * 
	 */
	public void startRun(){
		
		algorithm.init();
		
		for(int i = 0; i < paths.size(); i++)
		{

			PathGroundTruth path = paths.get(i);
			try{
				List<EdgeNodeRN> edges = algorithm.computeEdges(path.getObservations());
				
				// found edges, ground truth edges.
				List<String> foundEdges = new ArrayList<String>();
				for(EdgeNodeRN edge : edges)
				{
					foundEdges.add(edge.getId());
				}
				
				List<String> groundTruthEdges = path.getEdges();
				
				// compare these two with each other.
				
				// create hashmaps.
				
				// how can we compare these two?
				
				// you can create ground truth hash map and edges 
				// hash map then compare those.
				
				int[] frequencies = doAnalysis(foundEdges, groundTruthEdges);
				
				setCorrectEstimates(getCorrectEstimates() + frequencies[0]);
				setTotalEdges(getTotalEdges() + frequencies[1]);
				System.out.println("Driver id " + path.getUserId() + " " + path.getStartObservation() + " " + path.getEndObservation());
				System.out.println("Overall accuracy " + getCorrectEstimates() + " " + getTotalEdges() + " path accuracy  " + 1.0 * frequencies[0] / frequencies[1]);
			
			
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}

		
		}
		
		System.out.println("Run result : " + getCorrectEstimates()/getTotalEdges());

	}

	/**
	 * Do analysis computes scores such as found edges and ground edges.
	 * @param foundEdges
	 * @param groundTruthEdges
	 * @return
	 */
	public static int[] doAnalysis(List<String> foundEdges,
			List<String> groundTruthEdges) {
		
		int[] counts = new int[2];
		
		counts[0] = 0;
		counts[1] = 0;
		
		HashMap<String, Integer> foundMap = new HashMap<String, Integer>();
		HashMap<String, Integer> groundTruthMap = new HashMap<String, Integer>();
		
		for(String edge : foundEdges)
		{
			Integer result = foundMap.get(edge);
			if(result != null)
			{
				result += 1;
			}
			else
			{
				result = 1;
			}
			foundMap.put(edge, result);
		}
		
		for(String edge : groundTruthEdges)
		{
			Integer result = groundTruthMap.get(edge);
			if(result != null)
			{
				result += 1;
			}
			else
			{
				result = 1;
			}
			groundTruthMap.put(edge, result);
		}		
		
		// compute counts.
//		
//		for(String key : groundTruthMap.keySet())
//		{
//			Integer countGround = groundTruthMap.get(key);
//			
//			Integer countFound = foundMap.get(key) == null ? 0 : foundMap.get(key);
//		
//			counts[0] += countFound < countGround ? countFound : countGround;
//			counts[1] += countGround;
//		
//		}
//		
		counts[0] = 0;
		counts[1] = groundTruthMap.keySet().size() + foundMap.keySet().size();
		for(String key : groundTruthMap.keySet()){
			
			
			Integer countFound = foundMap.get(key) == null ? 0 : foundMap.get(key);
			
			if(countFound > 0)
			{
				counts[0] = counts[0] + 1;
			}
			
			
		}
		
		counts[1] = counts[1] - counts[0];
		
		return counts;
	}

	public List<PathGroundTruth> getPaths() {
		return paths;
	}

	public void setPaths(List<PathGroundTruth> paths) {
		this.paths = paths;
	}

	public Algorithm getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(Algorithm algorithm) {
		this.algorithm = algorithm;
	}

	public double getCorrectEstimates() {
		return correctEstimates;
	}

	public void setCorrectEstimates(double correctEstimates) {
		this.correctEstimates = correctEstimates;
	}

	public double getTotalEdges() {
		return totalEdges;
	}

	public void setTotalEdges(double totalEdges) {
		this.totalEdges = totalEdges;
	}
	
	public void print()
	{
		System.out.println("*****");
		System.out.println(algorithm.getParameters());
		System.out.println(correctEstimates / totalEdges);
		System.out.println("*****");
	}

	
}
