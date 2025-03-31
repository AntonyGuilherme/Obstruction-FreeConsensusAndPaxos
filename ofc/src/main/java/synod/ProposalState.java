package synod;

import akka.actor.ActorRef;
import synod.messages.Acknowledge;
import synod.messages.Gather;
import synod.messages.Proposal;

import java.util.HashMap;
import java.util.Map;

public class ProposalState {
    public final Proposal proposal;

    public final Map<String, Gather> gathers = new HashMap<>();
    private Gather greaterGather = null;

    public final Map<String, Acknowledge> acknowledgements =  new HashMap<>();

    public ProposalState(Proposal proposal) {
        this.proposal = proposal;
    }

    public boolean gathersReachQuorum(ActorRef process, Gather gather, int numberOfProcesses) {
        if (this.gathers.size() > (numberOfProcesses/2))
            return false;

        this.gathers.put(process.path().name(), gather);

        // updating the greater gather
        // this is easier to understand than verifying a list in the end
        if(greaterGather == null || greaterGather.imposeBallot() < gather.imposeBallot()) {
            greaterGather = gather;
        }

        // verifying if the quorum was reached
        return this.gathers.size() > (numberOfProcesses/2);
    }

    public boolean acknowledgementsReachQuorum(ActorRef process, Acknowledge acknowledge, int numberOfProcesses) {
        // consider if the quorum was not reached before
        if (this.acknowledgements.size() > (numberOfProcesses/2))
            return false;

        this.acknowledgements.put(process.path().name(), acknowledge);

        return this.acknowledgements.size() > (numberOfProcesses/2);
    }

    public Gather getGreaterGather() {
        return greaterGather;
    }
}
