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
    private float crashed = 0;
    private boolean held = false;
    private boolean silent = false;

    public Process() {
        super();
        // The basic idea of this is to run a method when a type of message arrives
        run(this::onLaunch).when(m -> m instanceof Launch);
        run(this::onCrash).when(m -> m instanceof Crash);
        run(this::onHold).when(m -> m instanceof Hold);
    }

    @Override
    public void onReceive(Object message) throws Throwable {
        // If a process receive is crashed it should not answer any message
        // If a process is in hold it should not process any other proposal
        if (!silent)
            silent = Math.random() < crashed;

        if (!silent && !(held && message instanceof Proposal)) {
            super.onReceive(message);
        }

    }

    private void onHold(Object message, ActorContext context) {
        held = true;
    }

    private void onCrash(Object message, ActorContext context) {
        // The process just crashes if the probability reaches a certain level
        Crash crash = (Crash) message;
        crashed = crash.probability();
    }

    private void onLaunch(Object message, ActorContext context) {
        // The proposal should be randomly 0 or 1
        if (Math.random() > 0.5)
            value = 1;

        Proposal proposal = new Proposal(value);
        sender = context.sender();

        self().tell(new Proposal(value), sender);
    }

    @Override
    protected void onAbort(Object message, ActorContext context) {
        Abort abort = (Abort) message;

        // Ity just abort if a proposal is running and
        // if the ballot meets the ballot of the message
        if (currentProposal != null && ballot == abort.ballot()) {
            // adding a new proposal
            // The idea is to propose until it reaches consensus
            currentProposal = null;
            try {
                onReceive(new Proposal(value));
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }
}
