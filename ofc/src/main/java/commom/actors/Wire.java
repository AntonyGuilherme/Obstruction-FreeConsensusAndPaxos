package commom.actors;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import synod.messages.Abort;
import synod.messages.Decide;
import synod.messages.Proposal;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Wire extends Actor {
    public static final List<Object> messages =  new LinkedList<>();
    LoggingAdapter logger = Logging.getLogger(getContext().system(), this.getClass());

    public Wire() {
        run(Wire::add);
        run(this::log).when(m -> m instanceof Decide || m instanceof Abort);
    }

    public static synchronized void add(Object message, AbstractActor.ActorContext context) {
        messages.add(message);
    }

    private void log(Object message, AbstractActor.ActorContext context) {
        String from = context.sender().path().name();
        String to = context.self().path().name();

        logger.warning(String.format("from %s to %s : %s", from, to, message));
    }
}
