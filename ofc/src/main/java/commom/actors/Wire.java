package commom.actors;

import akka.actor.AbstractActor;
import synod.messages.Decide;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Wire extends Actor {
    public static final List<Object> messages =  new LinkedList<>();
    public static final Map<String, Long> times =  new HashMap<>();

    public Wire() {
        run(Wire::add);
        run(Wire::onDecide).when(m -> m instanceof Decide);
        run(this::log);
    }

    private static void onDecide(Object message, ActorContext context) {
        times.put(context.sender().path().name(), System.currentTimeMillis());
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
