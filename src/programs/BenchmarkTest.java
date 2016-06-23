package programs;

import java.util.ArrayList;
import java.util.List;

import com.erdem.datasetparser.DatasetLoader;
import com.erdem.model.Benchmark;
import com.erdem.model.PathGroundTruth;

/**
 * Designed to run benchmark tests. Basically user adds benchmarks to the
 * Benchmark test.
 * 
 * @author erdem
 * 
 */
public class BenchmarkTest {

	private List<Benchmark> listOfBenchmarks = new ArrayList<Benchmark>();

	public void doRun() {

		for (Benchmark benchmark : getListOfBenchmarks()) {
			benchmark.startRun();
		}
	}

	public List<Benchmark> getListOfBenchmarks() {
		return listOfBenchmarks;
	}

	public void setListOfBenchmarks(List<Benchmark> listOfBenchmarks) {
		this.listOfBenchmarks = listOfBenchmarks;
	}

	public static void main(String[] args) {

		List<double[]> parameters = new ArrayList<double[]>();
		
		parameters.add(new double[] { 1, 10, 8, 0.05 });
		parameters.add(new double[] { 1, 10, 4, 0.05 });
		parameters.add(new double[] { 1, 10, 2, 0.05 });
		parameters.add(new double[] { 1, 10, 1, 0.05 });
//		parameters.add(new double[] { 5, 10, 8, 0.05 });
//		parameters.add(new double[] { 5, 10, 4, 0.05 });
//		parameters.add(new double[] { 5, 10, 2, 0.05 });
//		parameters.add(new double[] { 5, 10, 1, 0.05 });		
//		parameters.add(new double[] { 10, 10, 8, 0.05 });
//		parameters.add(new double[] { 10, 10, 4, 0.05 });
//		parameters.add(new double[] { 10, 10, 2, 0.05 });
//		parameters.add(new double[] { 10, 10, 1, 0.05 });

		DatasetLoader loader = new DatasetLoader();

		List<PathGroundTruth> list = (List<PathGroundTruth>) loader
				.deserialize("./groundTruth");

		BenchmarkTest test = new BenchmarkTest();

		for (double[] parameter : parameters) {
			test.getListOfBenchmarks().add(
					new Benchmark(list, new RoadNetworkNoMarkovAlgorithm(
							parameter[0], (int) (parameter[1]),
							(int) (parameter[2]), parameter[3], true)));
		}



		test.doRun();

		for (Benchmark benchmark : test.getListOfBenchmarks()) {
			benchmark.print();
		}
	}

}
