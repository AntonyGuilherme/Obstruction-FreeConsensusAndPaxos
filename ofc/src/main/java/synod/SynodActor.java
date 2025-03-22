package synod;

import akka.actor.ActorRef;
import commom.actors.Actor;
import commom.actors.IdentityGenerator;
import synod.messages.Proposal;
import synod.messages.Read;

import java.util.LinkedList;
import java.util.List;

public class SynodActor extends Actor {
    private int ballot = Integer.MIN_VALUE;
    private final List<ActorRef> processes = new LinkedList<>();

    public SynodActor() {
        run(this::onReceiveSynodProcess).when(m -> m instanceof ActorRef);
        run(this::onProposal).when(m -> m instanceof Proposal);
    }

    public void onReceiveSynodProcess(Object synodProcessRef, ActorContext context) {
        processes.add((ActorRef) synodProcessRef);
    }

    private void onProposal(Object message, ActorContext context) {
        startBallotIfNeeded();
        Proposal proposal = (Proposal) message;
        ballot += processes.size();
        Read read = new Read(ballot);

        for (ActorRef process : processes) {
            process.tell(read, getSelf());
        }
    }

    private void startBallotIfNeeded() {
        if (ballot == Integer.MIN_VALUE) {
            ballot = IdentityGenerator.generateIdentity() - processes.size();
        }
    }
}
