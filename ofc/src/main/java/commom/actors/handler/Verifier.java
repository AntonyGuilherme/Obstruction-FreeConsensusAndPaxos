package commom.actors.handler;

@FunctionalInterface
public interface Verifier {
    boolean when(Object message);
}
