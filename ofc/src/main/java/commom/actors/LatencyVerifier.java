package commom.actors;

import java.util.HashMap;
import java.util.Map;

public class LatencyVerifier {
    public final static Map<String, Long> start = new HashMap<>();
    public final static Map<String, Long> end = new HashMap<>();

    private static Long startOfSimulation = System.nanoTime();

    public synchronized static void setStart(String name) {
        start.putIfAbsent(name, startOfSimulation);
    }

    public synchronized static void setEnd(String name) {
        end.putIfAbsent(name, System.nanoTime());
    }

    public static void clear() {
        start.clear();
        end.clear();
        startOfSimulation = System.nanoTime();
    }
}
