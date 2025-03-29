import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import commom.actors.*;
import ofc.Process;
import ofc.messages.Crash;
import ofc.messages.Hold;
import ofc.messages.Launch;

import java.io.IOException;
import java.util.*;

public class Experiments {
    public static final int[] numberOfProcesses = {3, 10, 100};
    public static final int[] numberOfProcessesThatMayFail = {1, 4, 49};
    public static final float[] probabilityOfFail = {0, 0.1f, 1};
    static final int[] timeUntilElect = {500, 1000, 1500, 2000};

    public static void main(String[] args) throws InterruptedException, IOException {
        List<Result> results = new LinkedList<>();

        for (int i = 0; i < numberOfProcesses.length; i++) {
           for (float fail : probabilityOfFail) {
                for (int time : timeUntilElect) {
                    Result result = run(numberOfProcesses[i], numberOfProcessesThatMayFail[i], fail, time);
                    results.add(result);
                }
            }
        }

        ResultsWriter.write(results);
    }

    private static Result run(int numberOfProcesses, int numberOfProcessesThatMayFail,
                              float probabilityOfFail, int timeUntilElect) throws InterruptedException {

        List<LatencyMeasure> measures = new LinkedList<>();

        for (int w = 0; w < 5; w++) {
            LatencyVerifier.clear();
            Wire.messages.clear();
            IdentityGenerator.clear();

            final ActorSystem system = ActorSystem.create("messagesSystem");
            final ActorRef wire = system.actorOf(Props.create(Wire.class, Wire::new), "wire");

            final List<ActorRef> processes = new ArrayList<>(numberOfProcesses);

            for (int i = 0; i < numberOfProcesses; i++)
                processes.add(system.actorOf(Props.create(Process.class, Process::new), String.format("process%d", i)));

            tellEveryoneAboutEachOther(processes);
            Thread.sleep(1000);

            for (int i = 0; i < numberOfProcesses; i++) {
                processes.get(i).tell(new Launch(), wire);
            }

            Collections.shuffle(processes);

            for (int i = 0; i < numberOfProcessesThatMayFail; i++)
                processes.get(i).tell(new Crash(probabilityOfFail), ActorRef.noSender());

            Thread.sleep(timeUntilElect);
            System.out.printf("Election starts %s\n", processes.get(numberOfProcessesThatMayFail).path().name());

            for (int i = 0; i < numberOfProcessesThatMayFail; i++)
                processes.get(i).tell(new Hold(), ActorRef.noSender());

            for (int i = numberOfProcessesThatMayFail + 1; i < numberOfProcesses; i++)
                processes.get(i).tell(new Hold(), ActorRef.noSender());

            Thread.sleep(1000);
            system.terminate();

            for (String process : LatencyVerifier.start.keySet()) {
                if (LatencyVerifier.end.containsKey(process))
                    System.out.format("%s takes %s milliseconds to decide\n", process,
                            (LatencyVerifier.end.get(process) - LatencyVerifier.start.get(process)) / Math.pow(10, 6));
            }

            List<String> ends = new ArrayList<>(LatencyVerifier.end.keySet());

            ends.sort(Comparator.comparing(LatencyVerifier.end::get));

            float latency = LatencyVerifier.end.get(ends.getFirst()) - LatencyVerifier.start.get(ends.getFirst());
            latency = (float) (latency / Math.pow(10, 6));

            float averageLatency = 0;
            for (String process : LatencyVerifier.end.keySet())
                averageLatency += LatencyVerifier.end.get(process) - LatencyVerifier.start.get(process);

            averageLatency = averageLatency / LatencyVerifier.end.keySet().size();

            measures.add(new LatencyMeasure(latency, averageLatency));
        }

        return new Result(numberOfProcesses, numberOfProcessesThatMayFail, probabilityOfFail, timeUntilElect, measures);
    }

    private static void tellEveryoneAboutEachOther(List<ActorRef> processes) {
        for (ActorRef target : processes) {
            for (ActorRef other : processes) {
                target.tell(other, ActorRef.noSender());
            }
        }
    }
}
