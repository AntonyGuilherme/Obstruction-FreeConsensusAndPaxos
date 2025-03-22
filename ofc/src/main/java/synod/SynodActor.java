package synod;

import akka.actor.ActorRef;
import commom.actors.Actor;
import commom.actors.IdentityGenerator;
import synod.messages.*;

import java.util.*;

public class SynodActor extends Actor {
    private int id = IdentityGenerator.generateIdentity();
    private int ballot = Integer.MIN_VALUE;

    private CurrentProposal currentProposal;
    private int readBallot = Integer.MIN_VALUE;
    private int imposeBallot = Integer.MIN_VALUE;
    private int estimate = -1;

    private final List<ActorRef> processes = new LinkedList<>();

    public SynodActor() {
        run(this::onSynodProcess).when(m -> m instanceof ActorRef);
        run(this::onProposal).when(m -> m instanceof Proposal);
        run(this::onRead).when(m -> m instanceof Read);
        run(this::onAbort).when(m -> m instanceof Abort);
        run(this::onGather).when(m -> m instanceof Gather);
    }

    public void onSynodProcess(Object synodProcessRef, ActorContext context) {
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

    private void onRead(Object message, ActorContext context) {
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

    private void onAbort(Object abort, ActorContext context) {
        currentProposal.sender.tell(abort, getSelf());
    }

    private void onGather(Object message, ActorContext context) {
        Gather gather = (Gather) message;
        currentProposal.gathers.put(context.sender().path().name(), gather);

        if (currentProposal.gathers.size() > processes.size()/2) {
            int proposal = currentProposal.proposal.value();

            List<Gather> gathers = new ArrayList<>(currentProposal.gathers.values().stream().toList());
            if (gathers.stream().anyMatch(g -> g.imposeBallot() > 0)) {
                gathers.sort(Comparator.comparingInt(Gather::imposeBallot));
                proposal = gathers.getLast().estimate();
            }

            currentProposal.gathers.clear();

            for (ActorRef process : processes) {
                process.tell(new Impose(ballot, proposal), getSelf());
            }
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

    class CurrentProposal {
        public Proposal proposal;
        public ActorRef sender;
        public final Map<String, Gather> gathers = new HashMap<>();

        public CurrentProposal(Proposal proposal, ActorRef sender) {
            this.proposal = proposal;
            this.sender = sender;
        }
    }
}
