package synod;

import akka.actor.ActorRef;
import commom.actors.Actor;
import commom.actors.IdentityGenerator;
import synod.messages.Abort;
import synod.messages.Gather;
import synod.messages.Proposal;
import synod.messages.Read;

import java.util.LinkedList;
import java.util.List;

public class SynodActor extends Actor {
    private int id = IdentityGenerator.generateIdentity();
    private int ballot = Integer.MIN_VALUE;

    private CurrentProposal currentProposal;
    private int readBallot = Integer.MIN_VALUE;
    private int imposeBallot = Integer.MIN_VALUE;
    private int estimate = -1;

    private final List<ActorRef> processes = new LinkedList<>();

    public SynodActor() {
        run(this::onReceiveSynodProcess).when(m -> m instanceof ActorRef);
        run(this::onProposal).when(m -> m instanceof Proposal);
        run(this::onReceiveRead).when(m -> m instanceof Read);
        run(this::onReceiveAbort).when(m -> m instanceof Abort);
    }

    public void onReceiveSynodProcess(Object synodProcessRef, ActorContext context) {
        processes.add((ActorRef) synodProcessRef);
    }

    private void onProposal(Object message, ActorContext context) {
        startBallotIfNeeded();

        Proposal proposal = (Proposal) message;
        currentProposal = new CurrentProposal(proposal, context.getSender());

        ballot += processes.size();
        Read read = new Read(ballot);

        for (ActorRef process : processes) {
            process.tell(read, getSelf());
        }
    }

    private void onReceiveRead(Object message, ActorContext context) {
        startImposeBallotIfNeeded();
        Read read = (Read) message;

        if (readBallot > read.ballot()) {
            context.sender().tell(new Abort(read.ballot()), getSelf());
        }
        else {
            this.readBallot = read.ballot();
            context.sender().tell(new Gather(read.ballot(), imposeBallot, estimate), getSelf());
        }
    }

    private void onReceiveAbort(Object abort, ActorContext context) {
        currentProposal.sender.tell(abort, getSelf());
    }

    private void startBallotIfNeeded() {
        if (ballot == Integer.MIN_VALUE) {
            ballot = this.id - processes.size();
        }
    }

    private void startImposeBallotIfNeeded() {
        if (imposeBallot == Integer.MIN_VALUE) {
            imposeBallot = this.id - processes.size();
        }
    }

    class CurrentProposal {
        public Proposal proposal;
        public ActorRef sender;

        public CurrentProposal(Proposal proposal, ActorRef sender) {
            this.proposal = proposal;
            this.sender = sender;
        }
    }
}
