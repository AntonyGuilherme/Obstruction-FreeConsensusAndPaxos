package synod;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import commom.actors.IdentityGenerator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import synod.messages.*;

import java.util.List;
import java.util.stream.Stream;

public class SynodShouldTest {
    private ActorSystem system;

    @Before
    public void setUp() {
        this.system = ActorSystem.create("messagesSystem");
        IdentityGenerator.clear();
    }

    @Test
    public void whenProposalStartAProcessShouldReadAllValuesOfKnownProcesses() throws InterruptedException {
        ActorRef synod = system.actorOf(Props.create(SynodActor.class, SynodActor::new), "synod");
        ActorRef wiretap = system.actorOf(Props.create(WiretapActor.class, WiretapActor::new), "wiretap0");
        ActorRef wiretap1 = system.actorOf(Props.create(WiretapActor.class, WiretapActor::new), "wiretap1");

        synod.tell(wiretap, ActorRef.noSender());
        synod.tell(wiretap1, ActorRef.noSender());
        synod.tell(synod, ActorRef.noSender());
        Thread.sleep(50);

        synod.tell(new Proposal(1), wiretap);
        Thread.sleep(50);

        Assert.assertEquals(2, WiretapActor.messages.size());
        Assert.assertTrue(WiretapActor.messages.stream().allMatch(m -> ((Read)m).ballot() == 0));
    }

    @Test
    public void whenProposalStartAProcessShouldIncrementItsBallot() throws InterruptedException {
        ActorRef synod = system.actorOf(Props.create(SynodActor.class, SynodActor::new), "synod");
        ActorRef wiretap = system.actorOf(Props.create(WiretapActor.class, WiretapActor::new), "wiretap0");
        ActorRef wiretap1 = system.actorOf(Props.create(WiretapActor.class, WiretapActor::new), "wiretap1");

        synod.tell(wiretap, ActorRef.noSender());
        synod.tell(wiretap1, ActorRef.noSender());
        synod.tell(synod, ActorRef.noSender());
        Thread.sleep(100);

        synod.tell(new Proposal(1), wiretap);
        Thread.sleep(100);

        synod.tell(new Proposal(1), wiretap);
        Thread.sleep(100);

        Assert.assertEquals(4, WiretapActor.messages.size());
        Assert.assertEquals(2, WiretapActor.messages.stream().filter(m -> ((Read)m).ballot() == 3).count());
    }

    @Test
    public void whenReceiveReadRequestAProcessShouldAnswerGather() throws InterruptedException {
        ActorRef synod = system.actorOf(Props.create(SynodActor.class, SynodActor::new), "synod");
        ActorRef wiretap = system.actorOf(Props.create(WiretapActor.class, WiretapActor::new), "wiretap");

        synod.tell(wiretap, ActorRef.noSender());
        synod.tell(synod, ActorRef.noSender());
        Thread.sleep(50);

        synod.tell(new Proposal(1), wiretap);
        Thread.sleep(50);

        synod.tell(new Read(42), wiretap);
        Thread.sleep(50);

        Stream<Object> messages = WiretapActor.messages.stream().filter(m -> m instanceof Gather);
        Gather gather = (Gather) messages.toList().getFirst();

        Assert.assertEquals(42, gather.ballot());
        Assert.assertEquals(-2, gather.imposeBallot());
        Assert.assertEquals(-1, gather.estimate());
    }

    @Test
    public void whenReceiveReadRequestWithABallotWorseThanItsReadBallotProcessShouldAnswerAbort() throws InterruptedException {
        ActorRef synod = system.actorOf(Props.create(SynodActor.class, SynodActor::new), "synod");
        ActorRef wiretap = system.actorOf(Props.create(WiretapActor.class, WiretapActor::new), "wiretap");

        synod.tell(wiretap, ActorRef.noSender());
        synod.tell(synod, ActorRef.noSender());
        Thread.sleep(50);

        synod.tell(new Proposal(1), wiretap);
        Thread.sleep(50);

        synod.tell(new Read(42), wiretap);
        Thread.sleep(50);

        synod.tell(new Read(41), wiretap);
        Thread.sleep(50);

        Stream<Object> messages = WiretapActor.messages.stream().filter(m -> m instanceof Abort);
        Abort abort = (Abort) messages.toList().getFirst();

        Assert.assertEquals(41, abort.ballot());
    }

    @Test
    public void whenReceiveReadRequestWithABallotWorseThanItsImposedBallotProcessShouldAnswerAbort() throws InterruptedException {
        ActorRef synod = system.actorOf(Props.create(SynodActor.class, SynodActor::new), "synod");
        ActorRef wiretap = system.actorOf(Props.create(WiretapActor.class, WiretapActor::new), "wiretap");

        synod.tell(wiretap, ActorRef.noSender());
        synod.tell(synod, ActorRef.noSender());
        Thread.sleep(50);

        synod.tell(new Impose(10, 1), wiretap);
        Thread.sleep(50);

        synod.tell(new Read(9), wiretap);
        Thread.sleep(50);

        Stream<Object> messages = WiretapActor.messages.stream().filter(m -> m instanceof Abort);
        Abort abort = (Abort) messages.toList().getFirst();

        Assert.assertEquals(9, abort.ballot());
    }

    @Test
    public void whenReceiveAbortProcessShouldAnswerAbortToTheProposal() throws InterruptedException {
        ActorRef synod = system.actorOf(Props.create(SynodActor.class, SynodActor::new), "synod");
        ActorRef wiretap = system.actorOf(Props.create(WiretapActor.class, WiretapActor::new), "wiretap");

        synod.tell(wiretap, ActorRef.noSender());
        synod.tell(synod, ActorRef.noSender());
        Thread.sleep(50);

        synod.tell(new Proposal(1), wiretap);
        Thread.sleep(50);

        synod.tell(new Read(42), wiretap);
        Thread.sleep(50);

        synod.tell(new Abort(1), wiretap);
        Thread.sleep(50);

        Stream<Object> messages = WiretapActor.messages.stream().filter(m -> m instanceof Abort);
        Abort abort = (Abort) messages.toList().getFirst();

        Assert.assertEquals(1, abort.ballot());
    }

    @Test
    public void whenAccumulateGathersAProcessShouldImpose() throws InterruptedException {
        ActorRef synod = system.actorOf(Props.create(SynodActor.class, SynodActor::new), "synod");
        ActorRef wiretap = system.actorOf(Props.create(WiretapActor.class, WiretapActor::new), "wiretap0");
        ActorRef wiretap1 = system.actorOf(Props.create(WiretapActor.class, WiretapActor::new), "wiretap1");

        synod.tell(wiretap, ActorRef.noSender());
        synod.tell(wiretap1, ActorRef.noSender());
        synod.tell(synod, ActorRef.noSender());
        Thread.sleep(100);

        synod.tell(new Proposal(1), wiretap);
        Thread.sleep(100);

        synod.tell(new Gather(0, -3, -1), wiretap);
        Thread.sleep(100);

        List<Object> messages = WiretapActor.messages.stream().filter(m -> m instanceof Impose).toList();
        Impose impose = (Impose) messages.getFirst();

        Assert.assertEquals(2, messages.size());
        Assert.assertEquals(0, impose.ballot());
        Assert.assertEquals(1, impose.value());
    }

    @Test
    public void whenAccumulateGathersAProcessShouldImposeTheGreaterKnownProposal() throws InterruptedException {
        ActorRef synod = system.actorOf(Props.create(SynodActor.class, SynodActor::new), "synod");
        ActorRef wiretap = system.actorOf(Props.create(WiretapActor.class, WiretapActor::new), "wiretap0");
        ActorRef wiretap1 = system.actorOf(Props.create(WiretapActor.class, WiretapActor::new), "wiretap1");

        synod.tell(wiretap, ActorRef.noSender());
        synod.tell(wiretap1, ActorRef.noSender());
        synod.tell(synod, ActorRef.noSender());
        Thread.sleep(100);

        synod.tell(new Proposal(1), wiretap);
        Thread.sleep(100);

        synod.tell(new Gather(0, 42, 0), wiretap);
        Thread.sleep(100);

        List<Object> messages = WiretapActor.messages.stream().filter(m -> m instanceof Impose).toList();
        Impose impose = (Impose) messages.getFirst();

        Assert.assertEquals(2, messages.size());
        Assert.assertEquals(0, impose.ballot());
        Assert.assertEquals(0, impose.value());
    }

    @Test
    public void whenReceiveImposeAProcessShouldAcknowledge() throws InterruptedException {
        ActorRef synod = system.actorOf(Props.create(SynodActor.class, SynodActor::new), "synod");
        ActorRef wiretap = system.actorOf(Props.create(WiretapActor.class, WiretapActor::new), "wiretap");

        synod.tell(wiretap, ActorRef.noSender());
        synod.tell(synod, ActorRef.noSender());
        Thread.sleep(50);

        synod.tell(new Impose(0, 1), wiretap);
        Thread.sleep(100);

        List<Object> messages = WiretapActor.messages.stream().filter(m -> m instanceof Acknowledge).toList();
        Acknowledge acknowledge = (Acknowledge) messages.getFirst();

        Assert.assertEquals(1, messages.size());
        Assert.assertEquals(0, acknowledge.ballot());
    }

    @Test
    public void whenReceiveImposeAProcessShouldAbortIfTheImposeIsWorseThanAPreviousImpose() throws InterruptedException {
        ActorRef synod = system.actorOf(Props.create(SynodActor.class, SynodActor::new), "synod");
        ActorRef wiretap = system.actorOf(Props.create(WiretapActor.class, WiretapActor::new), "wiretap");

        synod.tell(wiretap, ActorRef.noSender());
        synod.tell(synod, ActorRef.noSender());
        Thread.sleep(50);

        synod.tell(new Impose(10, 1), wiretap);
        Thread.sleep(100);

        synod.tell(new Impose(0, 0), wiretap);
        Thread.sleep(100);

        List<Object> messages = WiretapActor.messages.stream().filter(m -> m instanceof Abort).toList();
        Abort abort = (Abort) messages.getFirst();

        Assert.assertEquals(1, messages.size());
        Assert.assertEquals(0, abort.ballot());
    }

    @Test
    public void whenReceiveImposeAProcessShouldAbortIfTheImposeIsWorseThanAPreviousRead() throws InterruptedException {
        ActorRef synod = system.actorOf(Props.create(SynodActor.class, SynodActor::new), "synod");
        ActorRef wiretap = system.actorOf(Props.create(WiretapActor.class, WiretapActor::new), "wiretap");

        synod.tell(wiretap, ActorRef.noSender());
        synod.tell(synod, ActorRef.noSender());
        Thread.sleep(50);

        synod.tell(new Read(10), wiretap);
        Thread.sleep(100);

        synod.tell(new Impose(0, 0), wiretap);
        Thread.sleep(100);

        List<Object> messages = WiretapActor.messages.stream().filter(m -> m instanceof Abort).toList();
        Abort abort = (Abort) messages.getFirst();

        Assert.assertEquals(1, messages.size());
        Assert.assertEquals(0, abort.ballot());
    }

    @Test
    public void whenReceiveAQuorumOfAcknowledgementsAProcessShouldDecide() throws InterruptedException {
        ActorRef synod = system.actorOf(Props.create(SynodActor.class, SynodActor::new), "synod");
        ActorRef synod2 = system.actorOf(Props.create(SynodActor.class, SynodActor::new), "synod2");
        ActorRef wire = system.actorOf(Props.create(WiretapActor.class, WiretapActor::new), "wire");

        synod.tell(synod2, ActorRef.noSender());
        synod.tell(synod, ActorRef.noSender());
        synod.tell(wire, ActorRef.noSender());
        synod2.tell(synod2, ActorRef.noSender());
        synod2.tell(synod, ActorRef.noSender());
        synod2.tell(wire, ActorRef.noSender());
        Thread.sleep(50);

        synod.tell(new Proposal(0), wire);
        Thread.sleep(100);

        List<Object> messages = WiretapActor.messages.stream().filter(m -> m instanceof Decide).toList();
        Decide decide = (Decide) messages.getFirst();

        Assert.assertEquals(1, messages.size());
        Assert.assertEquals(0, decide.value());
    }

    @Test
    public void whenAProcessDoNotReceiveAQuorumOfAcknowledgementsItShouldNotDecide() throws InterruptedException {
        ActorRef synod = system.actorOf(Props.create(SynodActor.class, SynodActor::new), "synod");
        ActorRef synod2 = system.actorOf(Props.create(SynodActor.class, SynodActor::new), "synod1");
        ActorRef wire = system.actorOf(Props.create(WiretapActor.class, WiretapActor::new), "wire");
        ActorRef wire1 = system.actorOf(Props.create(WiretapActor.class, WiretapActor::new), "wire1");

        tellEveryoneAboutEachOther(synod, synod2, wire, wire1);
        Thread.sleep(50);

        synod.tell(new Proposal(0), wire);
        Thread.sleep(50);
        synod.tell(new Gather(0, -3, -1), wire);
        Thread.sleep(100);

        Assert.assertFalse(WiretapActor.messages.stream().anyMatch(m -> m instanceof Decide));
    }

    @Test
    public void whenAProcessDoNotReceiveAQuorumOfGathersItShouldNotImpose() throws InterruptedException {
        ActorRef synod = system.actorOf(Props.create(SynodActor.class, SynodActor::new), "synod");
        ActorRef synod2 = system.actorOf(Props.create(SynodActor.class, SynodActor::new), "synod1");
        ActorRef wire = system.actorOf(Props.create(WiretapActor.class, WiretapActor::new), "wire");
        ActorRef wire1 = system.actorOf(Props.create(WiretapActor.class, WiretapActor::new), "wire1");

        tellEveryoneAboutEachOther(synod, synod2, wire, wire1);
        Thread.sleep(50);

        synod.tell(new Proposal(0), wire);
        Thread.sleep(100);

        Assert.assertFalse(WiretapActor.messages.stream().anyMatch(m -> m instanceof Impose));
    }

    @Test
    public void whenAProcessReceiveAGathersOfADifferentBallotItShouldNotBeConsidered() throws InterruptedException {
        ActorRef synod = system.actorOf(Props.create(SynodActor.class, SynodActor::new), "synod");
        ActorRef synod2 = system.actorOf(Props.create(SynodActor.class, SynodActor::new), "synod1");
        ActorRef wire = system.actorOf(Props.create(WiretapActor.class, WiretapActor::new), "wire");
        ActorRef wire1 = system.actorOf(Props.create(WiretapActor.class, WiretapActor::new), "wire1");

        tellEveryoneAboutEachOther(synod, synod2, wire, wire1);
        Thread.sleep(50);

        synod.tell(new Proposal(0), wire);
        synod.tell(new Gather(42, -3, -1), wire);
        Thread.sleep(100);

        Assert.assertFalse(WiretapActor.messages.stream().anyMatch(m -> m instanceof Impose));
    }

    private void tellEveryoneAboutEachOther(ActorRef... processes) {
        for (ActorRef target : processes) {
            for (ActorRef other : processes) {
                target.tell(other, ActorRef.noSender());
            }
        }
    }

    @After
    public void tearDown() {
        this.system.terminate();
        WiretapActor.messages.clear();
    }
}
