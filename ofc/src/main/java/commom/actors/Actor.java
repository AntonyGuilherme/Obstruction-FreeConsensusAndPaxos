package commom.actors;

import akka.actor.UntypedAbstractActor;
import commom.actors.handler.Action;
import commom.actors.handler.Handler;
import commom.actors.handler.MessageHandler;
import commom.actors.handler.VerifierBuilder;

import java.util.ArrayList;
import java.util.List;

public class Actor extends UntypedAbstractActor {
    private final List<Handler> handlers;

    public Actor() {
        this.handlers = new ArrayList<>();
    }

    @Override
    public void onReceive(Object message) throws Throwable {
        for (Handler handler : handlers) {
            if (handler.when(message)) {
                try {
                    handler.run(message, getContext());
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            }
        }
    }

    public VerifierBuilder run(Action action) {
        MessageHandler builder = new MessageHandler();
        handlers.add(builder);
        return builder.setAction(action);
    }
}
