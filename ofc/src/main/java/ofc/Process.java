package ofc;

import akka.actor.ActorRef;
import ofc.messages.Launch;
import synod.SynodActor;
import synod.messages.Abort;
import synod.messages.Proposal;

public class Process extends SynodActor {
    private int value = 0;
    private ActorRef sender;

    public Process() {
        super();

        run(this::onLaunch).when(m -> m instanceof Launch);
    }

    private void onLaunch(Object message, ActorContext context) {
        if (Math.random() > 0.5)
            value = 1;

        Proposal proposal = new Proposal(value);
        sender = context.sender();

        self().tell(proposal, sender);
    }

    @Override
    protected void onAbort(Object message, ActorContext context) {
        Abort abort = (Abort) message;

        if (currentProposal != null && currentProposal.ballot == abort.ballot()) {
            currentProposal.sender.tell(abort, getSelf());
            self().tell(new Proposal(value), sender);
            currentProposal = null;
        }
    }
}
