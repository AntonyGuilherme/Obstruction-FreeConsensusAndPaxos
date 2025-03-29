package commom.actors;

import java.util.List;

public record Result(int numberOfProcess,
                     int numberOfProcessThatMayFail,
                     float probabilityOfFail,
                     int timeUntilElection,
                     List<LatencyMeasure> latency) {
}
