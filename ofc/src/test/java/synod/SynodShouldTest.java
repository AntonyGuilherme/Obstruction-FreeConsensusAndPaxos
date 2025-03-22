package synod;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import commom.actors.IdentityGenerator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import synod.messages.Proposal;
import synod.messages.Read;

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
        Thread.sleep(50);

        synod.tell(new Proposal(1), wiretap);
        Thread.sleep(50);

        synod.tell(new Proposal(1), wiretap);
        Thread.sleep(50);

        Assert.assertEquals(4, WiretapActor.messages.size());
        Assert.assertEquals(2, WiretapActor.messages.stream().filter(m -> ((Read)m).ballot() == 3).count());
    }

    @After
    public void tearDown() {
        this.system.terminate();
        WiretapActor.messages.clear();
    }
}
