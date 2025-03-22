package synod;

import commom.actors.Actor;

import java.util.LinkedList;
import java.util.List;

public class WiretapActor extends Actor {

    public static final List<Object> messages =  new LinkedList<>();

    public WiretapActor() {
        run((message, _) -> messages.add(message));
    }
}
