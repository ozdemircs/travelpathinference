package com.erdem.model;

/**
 * Holds distance of a node to something.
 * @author erdem
 *
 * @param <T>
 */
public class DistanceHolder<T> implements Comparable<DistanceHolder<T>>{

	@Override
	public int compareTo(DistanceHolder<T> o) {		
		return o.distance < this.distance ? 1 : -1;
	}
	
	public T node;
	public double distance;
	public DistanceHolder<T> parent;
}
