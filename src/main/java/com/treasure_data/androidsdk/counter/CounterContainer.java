package com.treasure_data.androidsdk.counter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;


public class CounterContainer implements Iterable<Entry<String, Counter>>{
    private Map<String, Counter> map = new HashMap<String, Counter>();

    public synchronized void increment(String counterKey, String key, long i) {
        Counter counter = map.get(counterKey);
        if (counter == null) {
            counter = new Counter();
            map.put(counterKey, counter);
        }
        counter.increment(key, i);
    }

    public Counter getCounter(String counterKey) {
        return map.get(counterKey);
    }

    public synchronized void clear() {
        for (Counter counter : map.values()) {
            counter.clear();
        }
    }

    @Override
    public Iterator<Entry<String, Counter>> iterator() {
        return map.entrySet().iterator();
    }
}