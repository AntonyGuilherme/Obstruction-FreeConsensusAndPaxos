package commom.actors;

public record Result(int numberOfProcess,
                     int numberOfProcessThatMayFail,
                     float probabilityOfFail,
                     int timeUntilElection,
                     float latency) {
}
