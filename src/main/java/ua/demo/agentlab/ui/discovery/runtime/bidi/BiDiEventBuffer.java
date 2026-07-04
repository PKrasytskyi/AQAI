package ua.demo.agentlab.ui.discovery.runtime.bidi;

import java.util.ArrayList;
import java.util.List;

public class BiDiEventBuffer {

    private final int maxEvents;
    private final List<BiDiRuntimeEvent> events = new ArrayList<>();

    public BiDiEventBuffer(int maxEvents) {
        this.maxEvents = Math.max(100, maxEvents);
    }

    public synchronized void add(BiDiRuntimeEvent event) {
        if (event == null) {
            return;
        }
        events.add(event);
        while (events.size() > maxEvents) {
            events.remove(0);
        }
    }

    public synchronized List<BiDiRuntimeEvent> snapshot() {
        return List.copyOf(events);
    }

    public synchronized void clear() {
        events.clear();
    }
}
