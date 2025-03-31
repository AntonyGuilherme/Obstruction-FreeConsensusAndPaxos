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
    public static final float[] probabilityOfFail = {0, 0.1f, 1f};
    static final int[] timeUntilElect = {500, 1000, 1500};

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
            // cleaning all states to every experiment to be executed in the same initial state
            Wire.messages.clear();
            IdentityGenerator.clear();

            final ActorSystem system = ActorSystem.create("messagesSystem");
            final ActorRef wire = system.actorOf(Props.create(Wire.class, Wire::new), "wire");

            final List<ActorRef> processes = new ArrayList<>(numberOfProcesses);

            // adding the processes to the system
            for (int i = 0; i < numberOfProcesses; i++)
                processes.add(system.actorOf(Props.create(Process.class, Process::new), String.format("process%d", i)));

            // informing all processes of each other
            tellEveryoneAboutEachOther(processes);
            // awaiting to ensure that every process received the address to the other ones before
            // the proposals starts
            Thread.sleep(1000);

            LatencyVerifier.clear();
            // starting the proposals
            for (int i = 0; i < numberOfProcesses; i++) {
                processes.get(i).tell(new Launch(), wire);
            }

            // sorting randomly the list of processes
            // to fail randomly some of them
            Collections.shuffle(processes);

            // crashing some of the processes
            for (int i = 0; i < numberOfProcessesThatMayFail; i++)
                processes.get(i).tell(new Crash(probabilityOfFail), ActorRef.noSender());

            // awaiting the election
            Thread.sleep(timeUntilElect);
            System.out.printf("Election starts %s\n", processes.get(numberOfProcessesThatMayFail).path().name());

            // putting almost every process in hold
            // only one of them will keep proposing
            for (int i = 0; i < numberOfProcessesThatMayFail; i++)
                processes.get(i).tell(new Hold(), ActorRef.noSender());

            for (int i = numberOfProcessesThatMayFail + 1; i < numberOfProcesses; i++)
                processes.get(i).tell(new Hold(), ActorRef.noSender());

            // waiting until all the chit-chat ends
            Thread.sleep(1000);
            system.terminate();

            List<String> ends = new ArrayList<>(LatencyVerifier.end.keySet());

            // ordering by ending date (the ones that ends first is placed first)
            ends.sort(Comparator.comparing(LatencyVerifier.end::get));

            // latency of the first process that decides
            float latency = LatencyVerifier.end.get(ends.getFirst()) - LatencyVerifier.start.get(ends.getFirst());
            latency = (float) (latency / Math.pow(10, 6));

            // calculating the average latency
            float averageLatency = 0;
            for (String process : LatencyVerifier.end.keySet())
                averageLatency += LatencyVerifier.end.get(process) - LatencyVerifier.start.get(process);

            averageLatency = averageLatency / LatencyVerifier.end.keySet().size();
            averageLatency = (float) (averageLatency / Math.pow(10, 6));

            System.out.printf("average consensus : %s, first consensus %s\n", averageLatency, latency);

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
