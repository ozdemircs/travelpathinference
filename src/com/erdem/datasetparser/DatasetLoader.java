package com.erdem.datasetparser;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.erdem.model.NodeRN;
import com.erdem.model.ObservationRN;
import com.erdem.model.WayRN;

public class DatasetLoader {

	public List<String> readFile(String fileName)
	{
		
		List<String> content = new ArrayList<String>();
		
		BufferedReader br = null;
		 
		try {
 
			String sCurrentLine;
 
			br = new BufferedReader(new FileReader(fileName));
 
			while ((sCurrentLine = br.readLine()) != null) {
				content.add(sCurrentLine);
			}
 
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		
		return content;
	}

	public XMLStreamReader loadMapDatasetAsXML(String file)
			throws FileNotFoundException, XMLStreamException {
		XMLInputFactory factory = XMLInputFactory.newInstance();

		XMLStreamReader streamReader = factory
				.createXMLStreamReader(new FileReader(file));
		return streamReader;
	}

	public List<WayRN> loadWays(XMLStreamReader streamReader)
			throws XMLStreamException {
		List<WayRN> osmWays = new ArrayList<WayRN>();
		WayRN way = null;
		while (streamReader.hasNext()) {
			streamReader.next();
			if (streamReader.getEventType() == XMLStreamReader.START_ELEMENT) {
				if (streamReader.getLocalName().equals("way")) {

					way = new WayRN();

					way.setId(streamReader.getAttributeValue(0));

					while (!(streamReader.getEventType() == XMLStreamReader.END_ELEMENT && streamReader
							.getLocalName().equals("way"))) {
						streamReader.nextTag();

						if (streamReader.getEventType() == XMLStreamReader.START_ELEMENT) {
							if (streamReader.getLocalName().equals("nd")) {
								way.getNodeIds().add(
										streamReader.getAttributeValue(0));
							}
							if (streamReader.getLocalName().equals("tag")) {
								way.getTags().put(
										streamReader.getAttributeValue(0),
										streamReader.getAttributeValue(1));
							}
						}

					}
					osmWays.add(way);
				}
			}
		}

		return osmWays;

	}

	/**
	 * Loads data from
	 * 
	 * @param streamReader
	 * @return
	 * @throws XMLStreamException
	 */
	public HashMap<String, NodeRN> loadNodes(XMLStreamReader streamReader)
			throws XMLStreamException {

		HashMap<String, NodeRN> osmNodes = new HashMap<String, NodeRN>();

		while (streamReader.hasNext()) {
			streamReader.next();
			if (streamReader.getEventType() == XMLStreamReader.START_ELEMENT) {
				if (streamReader.getLocalName().equals("node")) {
					int attributeCount = streamReader.getAttributeCount();
					String id = null, lat = null, lon = null;
					for (int i = 0; i < attributeCount; i++) {
						if (streamReader.getAttributeLocalName(i).equals("id")) {
							id = streamReader.getAttributeValue(i);
						}
						if (streamReader.getAttributeLocalName(i).equals("lat")) {
							lat = streamReader.getAttributeValue(i);
						}
						if (streamReader.getAttributeLocalName(i).equals("lon")) {
							lon = streamReader.getAttributeValue(i);
						}
					}
					NodeRN node = new NodeRN();

					node.setId(id);
					node.setLatitude(new Double(lat));
					node.setLongitude(new Double(lon));
					osmNodes.put(id, node);
				}

			}
		}

		return osmNodes;

	}

	public HashMap<String, NodeRN> getRoadTruncatedGraph(
			HashMap<String, NodeRN> nodes, List<WayRN> ways) {

		// get truncated map
		HashMap<String, NodeRN> newMap = new HashMap<String, NodeRN>();
		for (WayRN way : ways) {
			if (way.getTags().get("highway") != null
					&& !way.getTags().get("highway").equals("footway")
					&& !way.getTags().get("highway").equals("raceway")
					&& !way.getTags().get("highway").equals("pedestrian")
					&& !way.getTags().get("highway").equals("bus_guideway")
					&& !way.getTags().get("highway").equals("track")
					&& !way.getTags().get("highway").equals("service")
					&& !way.getTags().get("highway").equals("footway")
					&& !way.getTags().get("highway").equals("cycleway")
					&& !way.getTags().get("highway").equals("bridleway")
					&& !way.getTags().get("highway").equals("steps")
					&& !way.getTags().get("highway").equals("path")) {
				
				for (int i = 1; i < way.getNodeIds().size(); i++) {
					// add an edge from node 0 to 1
					String prevI = way.getNodeIds().get(i - 1);
					String nextI = way.getNodeIds().get(i);

					NodeRN prev = nodes.get(prevI);
					NodeRN next = nodes.get(nextI);

					prev.getNeighbors().add(next);

					if (way.getTags().get("oneway") == null
							|| !way.getTags().get("oneway").equals("yes")) {
						next.getNeighbors().add(prev);
					}

					// add prev next to new Map if they are not there.
					if (newMap.get(prevI) == null) {
						newMap.put(prevI, prev);
					}

					// add prev next to new Map if they are not there.
					if (newMap.get(nextI) == null) {
						newMap.put(nextI, next);
					}

				}
				
			}
		}

		return newMap;
	}

	public List<String> loadTaxiDrivers(String datasetFile) throws Exception {

		HashMap<String, String> taxiDrivers = new HashMap<String, String>();

		BufferedReader reader = new BufferedReader(new FileReader(new File(
				datasetFile)));

		String line = reader.readLine();

		while (line != null) {
			String[] tokens = line.split(";");
			if (!taxiDrivers.containsKey(tokens[0])) {
				taxiDrivers.put(tokens[0], tokens[0]);
			}
			line = reader.readLine();
		}

		reader.close();
		return new ArrayList<String>(taxiDrivers.keySet());
	}

	public HashMap<String, List<ObservationRN>> getObservationSequences(
			String datasetFile) throws IOException, ParseException {

		HashMap<String, List<ObservationRN>> observationsMap = new HashMap<String, List<ObservationRN>>();

		BufferedReader reader = new BufferedReader(new FileReader(new File(
				datasetFile)));

		String line = reader.readLine();

		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");

		while (line != null) {
			String[] tokens = line.split(";");
			ObservationRN observation = new ObservationRN();
			Date date = dateFormat.parse(tokens[1]);
			observation.setLat(new Double(tokens[2].substring(6,
					tokens[2].length() - 1).split(" ")[0]));
			observation.setLon(new Double(tokens[2].substring(6,
					tokens[2].length() - 1).split(" ")[1]));
			observation.setTime(date.getTime());
			observation.setUserId(tokens[0]);
			List<ObservationRN> aNewList;
			if (observationsMap.get(tokens[0]) == null) {
				aNewList = new ArrayList<ObservationRN>();
				observationsMap.put(tokens[0], aNewList);
			} else {
				aNewList = observationsMap.get(tokens[0]);
			}
			aNewList.add(observation);
			line = reader.readLine();
		}

		reader.close();

		return observationsMap;
	}

	public List<ObservationRN> getObservationSequences(String userId,
			String datasetFile) throws IOException, ParseException {

		List<ObservationRN> observations = new ArrayList<ObservationRN>();

		BufferedReader reader = new BufferedReader(new FileReader(new File(
				datasetFile)));

		String line = reader.readLine();

		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");

		while (line != null) {
			String[] tokens = line.split(";");

			if (tokens[0].equals(userId)) {
				ObservationRN observation = new ObservationRN();
				Date date = dateFormat.parse(tokens[1]);
				observation.setLat(new Double(tokens[2].substring(6,
						tokens[2].length() - 1).split(" ")[0]));
				observation.setLon(new Double(tokens[2].substring(6,
						tokens[2].length() - 1).split(" ")[1]));
				observation.setTime(date.getTime());
				observation.setUserId(userId);
				observations.add(observation);
			}

			line = reader.readLine();
		}

		reader.close();

		return observations;
	}

	public void serialize(Object object, String fileName) {
		try (OutputStream file = new FileOutputStream(fileName);
				OutputStream buffer = new BufferedOutputStream(file);
				ObjectOutput output = new ObjectOutputStream(buffer);) {
			output.writeObject(object);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public Object deserialize(String fileName) {
		// deserialize the quarks.ser file
		try {

			InputStream file = new FileInputStream(fileName);

			InputStream buffer = new BufferedInputStream(file);
			ObjectInput input = new ObjectInputStream(buffer);

			return input.readObject();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
