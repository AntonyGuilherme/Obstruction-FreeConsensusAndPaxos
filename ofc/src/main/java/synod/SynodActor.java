package synod;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import commom.actors.Actor;
import commom.actors.IdentityGenerator;
import commom.actors.LatencyVerifier;
import synod.messages.*;

import java.util.*;

public class SynodActor extends Actor {
    private final int id = IdentityGenerator.generateIdentity();
    protected int ballot = Integer.MIN_VALUE;
    private boolean hasDecided = false;
    private ActorRef sender;

    protected ProposalState currentProposal;
    private int readBallot = Integer.MIN_VALUE;
    private int imposeBallot = Integer.MIN_VALUE;
    private int estimate = -30;

    private final List<ActorRef> processes = new LinkedList<>();

    public SynodActor() {
        //run(this::log);
        run(this::onSynodProcess).when(m -> m instanceof ActorRef);
        run(this::onProposal).when(m -> m instanceof Proposal);
        run(this::onRead).when(m -> m instanceof Read);
        run(this::onAbort).when(m -> m instanceof Abort);
        run(this::onGather).when(m -> m instanceof Gather);
        run(this::onImpose).when(m -> m instanceof Impose);
        run(this::onAcknowledge).when(m -> m instanceof Acknowledge);
        run(this::onDecide).when(m -> m instanceof Decide);
    }

    private void log(Object message, AbstractActor.ActorContext context) {
        String from = context.sender().path().name();
        String to = context.self().path().name();

        System.out.printf("from %s to %s : %s%n", from, to, message);
    }

    private void onSynodProcess(Object synodProcessRef, ActorContext context) {
        processes.add((ActorRef) synodProcessRef);
    }

    protected void onProposal(Object message, ActorContext context) {
        LatencyVerifier.setStart(self().path().name());
        startBallotIfNeeded();

        Proposal proposal = (Proposal) message;

        ballot += processes.size();

        currentProposal = new ProposalState(proposal);
        sender = sender();
        Read read = new Read(ballot);

        for (ActorRef process : processes) {
            process.tell(read, getSelf());
        }
    }

    private void onRead(Object message, ActorContext context) {
        startImposeBallotIfNeeded();
        Read read = (Read) message;

        if (readBallot > read.ballot() || imposeBallot > read.ballot()) {
            context.sender().tell(new Abort(read.ballot()), getSelf());
        }
        else {
            this.readBallot = read.ballot();
            context.sender().tell(new Gather(read.ballot(), imposeBallot, estimate), getSelf());
        }
    }

    protected void onAbort(Object message, ActorContext context) {
        Abort abort = (Abort) message;

        if (currentProposal != null && ballot == abort.ballot()) {
            sender.tell(abort, getSelf());
            currentProposal = null;
        }
    }

    private void onGather(Object message, ActorContext context) {
        Gather gather = (Gather) message;

        if (currentProposal == null || ballot != gather.ballot())
            return;

        if (currentProposal.GathersReachQuorum(context.sender(), gather, processes.size())) {
            int proposal = currentProposal.proposal.value();
            Gather greatherGather = currentProposal.getGreaterGather();

            if (greatherGather.imposeBallot() > 0)
                proposal = greatherGather.estimate();

            for (ActorRef process : processes) {
                process.tell(new Impose(ballot, proposal), getSelf());
            }
        }
    }

    private void onImpose(Object message, ActorContext context) {
        Impose impose = (Impose) message;

        if (readBallot > impose.ballot() || imposeBallot > impose.ballot())
            context.sender().tell(new Abort(impose.ballot()), getSelf());
        else {
            estimate = impose.value();
            imposeBallot = impose.ballot();
            context.sender().tell(new Acknowledge(impose.ballot()), getSelf());
        }
    }

    private void onAcknowledge(Object message, ActorContext context) {
        Acknowledge ack = (Acknowledge) message;

        if (currentProposal == null || ballot != ack.ballot())
            return;

        if (currentProposal.acknowledgementsReachQuorum(context.sender(), ack, processes.size())) {
            for (ActorRef process : processes) {
                process.tell(new Decide(estimate, ballot), getSelf());
            }

            onDecide(new Decide(estimate, ballot), context);
        }
    }

    private void onDecide(Object message, ActorContext context) {
        if (!hasDecided && sender != null) {
            LatencyVerifier.setEnd(self().path().name());
            Decide decide = (Decide) message;

            for (ActorRef process : processes) {
                process.tell(decide, getSelf());
            }

            sender.tell(message, getSelf());
            currentProposal = null;
            hasDecided = true;
        }
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
}
