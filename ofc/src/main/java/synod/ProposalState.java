package synod;

import akka.actor.ActorRef;
import synod.messages.Acknowledge;
import synod.messages.Gather;
import synod.messages.Proposal;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class ProposalState {
    public final int ballot;
    public final Proposal proposal;
    public final ActorRef sender;

    public final Map<String, Gather> gathers = new HashMap<>();
    private Gather greaterGather = null;

    public final Map<String, Acknowledge> acknowledgements =  new HashMap<>();

    public ProposalState(Proposal proposal, ActorRef sender) {
        this.ballot = 1;
        this.proposal = proposal;
        this.sender = sender;
    }

    public boolean accumulateGather(ActorRef process, Gather gather, int numberOfProcesses) {
        if (this.gathers.size() > (numberOfProcesses/2))
            return false;

        this.gathers.put(process.path().name(), gather);

        if(greaterGather == null || greaterGather.imposeBallot() < gather.imposeBallot()) {
            greaterGather = gather;
        }

        return this.gathers.size() > (numberOfProcesses/2);
    }

    public boolean accumulateAcknowledgements(ActorRef process, Acknowledge acknowledge, int numberOfProcesses) {
        if (this.acknowledgements.size() > (numberOfProcesses/2))
            return false;

        this.acknowledgements.put(process.path().name(), acknowledge);

        return this.acknowledgements.size() > (numberOfProcesses/2);
    }

    public Gather getGreaterGather() {
        return greaterGather;
    }
}
