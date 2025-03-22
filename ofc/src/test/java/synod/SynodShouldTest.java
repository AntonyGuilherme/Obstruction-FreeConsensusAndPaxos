package synod;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import org.junit.After;
import org.junit.Before;

public class SynodShouldTest {
    private ActorSystem system;
    private ActorRef wiretap;

    @Before
    public void setUp() {
        this.system = ActorSystem.create("messagesSystem");
        this.wiretap = system.actorOf(Props.create(WiretapActor.class, WiretapActor::new), "wiretap");
    }

    @After
    public void tearDown() {
        this.system.terminate();
        WiretapActor.messages.clear();
    }
}
