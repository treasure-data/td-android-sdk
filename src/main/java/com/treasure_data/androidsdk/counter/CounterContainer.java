package com.treasure_data.androidsdk.counter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.treasure_data.androidsdk.apiclient.DbTableDescr;


public class CounterContainer implements Iterable<Entry<DbTableDescr, Counter>>{
    private Map<DbTableDescr, Counter> map = new HashMap<DbTableDescr, Counter>();

    public synchronized void increment(DbTableDescr descr, String key, long amount) {
        Counter counter = map.get(descr);
        if (counter == null) {
            counter = new Counter();
            map.put(descr, counter);
        }
        counter.increment(key, amount);
    }

    public Counter getCounter(DbTableDescr descr) {
        return map.get(descr);
    }

    public synchronized void clear() {
        for (Counter counter : map.values()) {
            counter.clear();
        }
    }

    public synchronized void clear(DbTableDescr descr) {
        map.remove(descr);
    }

    @Override
    public Iterator<Entry<DbTableDescr, Counter>> iterator() {
        return map.entrySet().iterator();
    }
}