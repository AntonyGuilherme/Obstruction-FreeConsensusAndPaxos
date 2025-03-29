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

    // adding the other knows processes
    private void onSynodProcess(Object synodProcessRef, ActorContext context) {
        processes.add((ActorRef) synodProcessRef);
    }

    protected void onProposal(Object message, ActorContext context) {
        // adding the start time of the first proposal
        LatencyVerifier.setStart(self().path().name());

        // starting the ballot in case it is not have a valid start value
        // this is necessary because the number of process is not known until the first
        // real message arrives
        startBallotIfNeeded();

        Proposal proposal = (Proposal) message;

        // incrementing the ballot number with thr number of knowing processes
        // in this way a ballot number is never equal to the another one
        ballot += processes.size();

        // this object is responsible to accumulate all messages of a proposal
        // so it always have a value while a proposal is being processed
        currentProposal = new ProposalState(proposal);
        // who actually ask for the proposal
        sender = sender();
        Read read = new Read(ballot);

        // asking the for the values of the other known processes
        for (ActorRef process : processes) {
            process.tell(read, getSelf());
        }
    }

    private void onRead(Object message, ActorContext context) {
        startImposeBallotIfNeeded();
        Read read = (Read) message;

        // if the ballot of the process is greater or the imposed before
        // the process should abort, because a "better" is known
        if (readBallot > read.ballot() || imposeBallot > read.ballot()) {
            context.sender().tell(new Abort(read.ballot()), getSelf());
        }
        else {
            // informing the process about the known value
            this.readBallot = read.ballot();
            context.sender().tell(new Gather(read.ballot(), imposeBallot, estimate), getSelf());
        }
    }

    protected void onAbort(Object message, ActorContext context) {
        Abort abort = (Abort) message;
        // it will abort only if a proposal is running and if the ballot
        // meets the current ballot
        if (currentProposal != null && ballot == abort.ballot()) {
            sender.tell(abort, getSelf());
            currentProposal = null;
        }
    }

    private void onGather(Object message, ActorContext context) {
        Gather gather = (Gather) message;

        // just consider the gather if the ballot is from the current proposal
        if (currentProposal == null || ballot != gather.ballot())
            return;

        // adding the gathers until it reach a quorum
        if (currentProposal.gathersReachQuorum(context.sender(), gather, processes.size())) {
            int proposal = currentProposal.proposal.value();
            // getting the greater gather accumulated, i.e. the one with the greater imposed value
            Gather greatherGather = currentProposal.getGreaterGather();

            // if the greater gather have an imposed ballot positive
            // this means that the process have a potential decided value
            if (greatherGather.imposeBallot() > 0)
                proposal = greatherGather.estimate();

            // imposing the proposal to every other process
            for (ActorRef process : processes) {
                process.tell(new Impose(ballot, proposal), getSelf());
            }
        }
    }

    private void onImpose(Object message, ActorContext context) {
        Impose impose = (Impose) message;

        // if the ballot of the process is greater or the imposed before
        // the process should abort, because a "better" is known
        if (readBallot > impose.ballot() || imposeBallot > impose.ballot())
            context.sender().tell(new Abort(impose.ballot()), getSelf());
        else {
            // accepting the proposed value and informing the process
            estimate = impose.value();
            imposeBallot = impose.ballot();
            context.sender().tell(new Acknowledge(impose.ballot()), getSelf());
        }
    }

    private void onAcknowledge(Object message, ActorContext context) {
        Acknowledge ack = (Acknowledge) message;

        // accept the acknowledgement only if a proposal is running
        // and if the ballot numbers meets the actual number
        if (currentProposal == null || ballot != ack.ballot())
            return;

        // accumulating the acknowledgements until it reaches quorum
        if (currentProposal.acknowledgementsReachQuorum(context.sender(), ack, processes.size())) {
            // informing every other process that they should decide
            for (ActorRef process : processes)
                process.tell(new Decide(estimate, ballot), getSelf());

            // this decides is placed here
            // to ensure that the first process to decide is the process that reach
            // this step first
            onDecide(new Decide(estimate, ballot), context);
        }
    }

    private void onDecide(Object message, ActorContext context) {

        // the process should decide if it did not happen before
        // and if the sender require for a proposal from this process
        if (!hasDecided && sender != null) {
            LatencyVerifier.setEnd(self().path().name());
            Decide decide = (Decide) message;

            // informing every other process to also decide
            for (ActorRef process : processes) {
                process.tell(decide, getSelf());
            }

            // informing the requester that the consensus was reached
            sender.tell(message, getSelf());
            // ending the proposal state and saving the decision status
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
