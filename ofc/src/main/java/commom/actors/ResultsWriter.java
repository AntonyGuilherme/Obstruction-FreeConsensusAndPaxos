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
            experiment.put("numberOfProcesses", result.numberOfProcess());
            experiment.put("numberOfProcessesThatMayFail", result.numberOfProcessThatMayFail());
            experiment.put("probabilityOfFail", result.probabilityOfFail());
            experiment.put("timeUntilElection", result.timeUntilElection());
            experiment.put("latency", result.latency());
        }

        objectMapper.writeValue(new File("results.json"), json);
    }
}
