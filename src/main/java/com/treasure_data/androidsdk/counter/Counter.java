package com.treasure_data.androidsdk.counter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class Counter implements Iterable<Entry<String, Long>> {
    private Map<String, Long> map = new HashMap<String, Long>();

    public void increment(String key) {
        increment(key, 1);
    }

    public synchronized void increment(String key, long i) {
        Long v = map.get(key);
        if (v == null) {
            map.put(key, i);
            return;
        }
        map.put(key, v + i);
    }

    public synchronized void clear() {
        map.clear();
    }

    @Override
    public Iterator<Entry<String, Long>> iterator() {
        return map.entrySet().iterator();
    }
}