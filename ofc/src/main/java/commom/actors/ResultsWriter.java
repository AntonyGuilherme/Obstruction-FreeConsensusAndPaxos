package commom.actors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ResultsWriter {
    public static void write(List<Result> results) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode json = objectMapper.createArrayNode();

        for (Result result : results) {
            ObjectNode experiment = json.addObject();
            experiment.put("numberOfProcess", result.numberOfProcess());
            experiment.put("numberOfFaultyProcesses", result.numberOfProcessThatMayFail());
            experiment.put("failureProb", result.probabilityOfFail());
            experiment.put("leaderElectionTime", result.timeUntilElection());

            ArrayNode measures = experiment.putArray("results");

            for (LatencyMeasure measure : result.latency()) {
                ObjectNode measureObj = measures.addObject();
                measureObj.put("consensusLatency", measure.latency());
                measureObj.put("averageLatency", measure.averageLatency());
            }
        }

        objectMapper.writeValue(new File("results.json"), json);
    }
}
