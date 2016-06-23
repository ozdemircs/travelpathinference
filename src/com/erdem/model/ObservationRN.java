package com.erdem.model;

import java.io.Serializable;

public class ObservationRN implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private double lat;
	
	private double lon;
	
	private long time;
	
	private String userId;
	
	
	public double getLat() {
		return lat;
	}
	public void setLat(double lat) {
		this.lat = lat;
	}
	public double getLon() {
		return lon;
	}
	public void setLon(double lon) {
		this.lon = lon;
	}
	public long getTime() {
		return time;
	}
	public void setTime(long time) {
		this.time = time;
	}
	
	public String toString()
	{
		return this.lat + " " + this.lon;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
}
