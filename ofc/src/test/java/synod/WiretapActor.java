package synod;

import akka.actor.AbstractActor;
import commom.actors.Actor;

import java.util.LinkedList;
import java.util.List;

public class WiretapActor extends Actor {
    public static final List<Object> messages =  new LinkedList<>();

    public WiretapActor() {
        run(WiretapActor::add);
        run(this::log);

    }

    public static synchronized void add(Object message, AbstractActor.ActorContext context) {
        messages.add(message);
    }

    private void log(Object message, AbstractActor.ActorContext context) {
        String from = context.sender().path().name();
        String to = context.self().path().name();

        System.out.printf("from %s to %s : %s%n", from, to, message);
    }
}
