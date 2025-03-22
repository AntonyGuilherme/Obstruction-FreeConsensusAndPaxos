package commom.actors;

public class IdentityGenerator {
    private static int currentIdentity = 0;

    public static int generateIdentity() {
        return currentIdentity++;
    }

    public static void clear() {
        currentIdentity = 0;
    }
}
