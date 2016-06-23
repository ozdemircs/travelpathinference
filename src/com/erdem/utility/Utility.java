package com.erdem.utility;

import com.erdem.model.PointRN;

public class Utility {
	/**
	 * Computes distance in meters.
	 * 
	 * @param lat1
	 * @param lng1
	 * @param lat2
	 * @param lng2
	 * @return
	 */
	public static double distFrom(double lat1, double lon1, double lat2, double lon2) {
		double earthRadius = 6371000; // meters
		double dLat = Math.toRadians(lat2 - lat1);
		double dLng = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2)
				* Math.sin(dLng / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		float dist = (float) (earthRadius * c);

		return dist;
		

	}

	public static double closestDistance(double latEdge1, double lonEdge1,
			double latEdge2, double lonEdge2, double latP, double lonP) {

		double xDelta = lonEdge2 - lonEdge1;
		double yDelta = latEdge2 - latEdge1;


		double t = ((lonP - lonEdge1) * xDelta + (latP - latEdge1) * yDelta) / (xDelta * xDelta + yDelta * yDelta);

		if (t < 0) {
			t = 0;
		} else if (t > 1) {
			t = 1;
		}

		double lon = lonEdge1 + xDelta * t;
		double lat = latEdge1 + yDelta * t;
		

		
		return distFrom(latP, lonP, lat, lon);
		
	}
	
	public static PointRN findClosestOnLine(double latEdge1, double lonEdge1, 
			double latEdge2, double lonEdge2, 
			double latP, double lonP)
	{
		

		double xDelta = lonEdge2 - lonEdge1;
		double yDelta = latEdge2 - latEdge1;


		double t = ((lonP - lonEdge1) * xDelta + (latP - latEdge1) * yDelta) / (xDelta * xDelta + yDelta * yDelta);

		if (t < 0) {
			t = 0;
		} else if (t > 1) {
			t = 1;
		}

		double lon = lonEdge1 + xDelta * t;
		double lat = latEdge1 + yDelta * t;
		
		PointRN pointClosest = new PointRN();
		
		pointClosest.setLatitude(lat);
		pointClosest.setLongitude(lon);
		
		return pointClosest;
		
	}
	
	public static void main(String[] args)
	{
		System.out.println(closestDistance(41.9743102 ,12.5202096,  41.9766765 , 12.5172618,41.9753022924641, 12.5188125538987));
	}
}
