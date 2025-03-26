package ofc;

import akka.actor.ActorRef;
import ofc.messages.Crash;
import ofc.messages.Hold;
import ofc.messages.Launch;
import synod.SynodActor;
import synod.messages.Abort;
import synod.messages.Proposal;

public class Process extends SynodActor {
    private int value = 0;
    private ActorRef sender;
    private boolean crashed = false;
    private boolean held = false;

    public Process() {
        super();
        run(this::onLaunch).when(m -> m instanceof Launch);
        run(this::onCrash).when(m -> m instanceof Crash);
        run(this::onHold).when(m -> m instanceof Hold);
    }

    @Override
    public void onReceive(Object message) throws Throwable {
        if (!crashed && !(held && message instanceof Proposal))
            super.onReceive(message);
    }

    private void onHold(Object message, ActorContext context) {
        held = true;
    }

    private void onCrash(Object message, ActorContext context) {
        Crash crash = (Crash) message;
        crashed = Math.random() <= crash.probability();
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
