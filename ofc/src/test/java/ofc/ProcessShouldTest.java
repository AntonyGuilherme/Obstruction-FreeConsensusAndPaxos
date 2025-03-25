package ofc;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import commom.actors.IdentityGenerator;
import ofc.messages.Launch;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import synod.WiretapActor;
import synod.messages.*;

public class ProcessShouldTest {
    private ActorSystem system;

    @Before
    public void setUp() {
        this.system = ActorSystem.create("messagesSystem");
    }

    @Test
    public void whenAProcessReceivesALaunchItShouldProposeUntilDecide() throws InterruptedException {
        ActorRef process = system.actorOf(Props.create(Process.class, Process::new), "process0");
        ActorRef process1 = system.actorOf(Props.create(Process.class, Process::new), "process1");
        ActorRef process2 = system.actorOf(Props.create(Process.class, Process::new), "process2");

        ActorRef wire = system.actorOf(Props.create(WiretapActor.class, WiretapActor::new), "wire");

        tellEveryoneAboutEachOther(process, process1, process2);
        Thread.sleep(100);

        process.tell(new Launch(), wire);
        process1.tell(new Launch(), wire);
        process2.tell(new Launch(), wire);
        Thread.sleep(100);

        var decides  = WiretapActor.messages.stream().filter(m -> m instanceof Decide).toList();
        Decide decide = (Decide) decides.getFirst();

        Assert.assertEquals(3, decides.size());
        Assert.assertTrue(decides.stream().allMatch(m -> ((Decide)m).value() == decide.value()));
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
        IdentityGenerator.clear();
    }
}
