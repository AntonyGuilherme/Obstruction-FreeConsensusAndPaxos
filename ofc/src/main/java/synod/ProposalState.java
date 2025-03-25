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
    public LinkedList<Acknowledge> acknowledgements =  new LinkedList<>();

    public ProposalState(Proposal proposal, ActorRef sender) {
        this.ballot = 1;
        this.proposal = proposal;
        this.sender = sender;
    }
}
