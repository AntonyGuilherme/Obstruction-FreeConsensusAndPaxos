import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import commom.actors.Wire;
import ofc.Process;
import ofc.messages.Crash;
import ofc.messages.Hold;
import ofc.messages.Launch;

import java.util.*;

public class Experiments {
    public static final int numberOfProcesses = 100;
    public static final int numberOfProcessesThatMayFail = 49;
    public static final float probabilityOfFail = 0.4f;
    static final int timeUntilElect = 400;

    public static void main(String[] args) throws InterruptedException {
        final ActorSystem system = ActorSystem.create("messagesSystem");
        final ActorRef wire = system.actorOf(Props.create(Wire.class, Wire::new), "wire");
        final Map<String, Long> times = new HashMap<>();

        final List<ActorRef> processes = new ArrayList<>(numberOfProcesses);

        for (int i = 0; i < numberOfProcesses; i++)
            processes.add(system.actorOf(Props.create(ofc.Process.class, Process::new), String.format("process%d", i)));

        tellEveryoneAboutEachOther(processes);
        Thread.sleep(100);

        for (int i = 0; i < numberOfProcesses; i++) {
            processes.get(i).tell(new Launch(), wire);
            times.put(processes.get(i).path().name(), System.currentTimeMillis());
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

        for (String process : Wire.times.keySet()) {
            System.out.format("%s takes %d milliseconds to decide\n", process, Wire.times.get(process) - times.get(process));
        }
    }

    private static void tellEveryoneAboutEachOther(List<ActorRef> processes) {
        for (ActorRef target : processes) {
            for (ActorRef other : processes) {
                target.tell(other, ActorRef.noSender());
            }
        }
    }
}
