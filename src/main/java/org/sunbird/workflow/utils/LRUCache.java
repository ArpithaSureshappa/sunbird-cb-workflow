package org.sunbird.workflow.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.sunbird.workflow.config.Configuration;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class LRUCache<K, V> extends LinkedHashMap<K, V> {

    @Autowired
    Configuration conf;
    private final int maxSize;
    private final long maxAgeInMillis;
    private final Map<K, Long> entryTimeMap;

    public LRUCache() {
        super(16, 0.75f, true);
        this.maxSize = conf.getEnrolStatusCountLocalCacheSize();
        this.maxAgeInMillis = conf.getEnrolStatusCountLocalTimeToLive()*60;
        this.entryTimeMap = new HashMap<>();
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        boolean isMaxSizeExceeded = size() > maxSize;
        if (isMaxSizeExceeded || isEntryExpired(eldest.getKey())) {
            entryTimeMap.remove(eldest.getKey());
            return true;
        }
        return false;
    }

    @Override
    public V get(Object key) {
        V value = super.get(key);
        if (value != null) {
            entryTimeMap.put((K) key, System.currentTimeMillis());
        }
        return value;
    }

    @Override
    public V put(K key, V value) {
        entryTimeMap.put(key, System.currentTimeMillis());
        return super.put(key, value);
    }

    private boolean isEntryExpired(K key) {
        if (!entryTimeMap.containsKey(key)) {
            return true;  // Entry does not exist, considered expired
        }
        long entryTime = entryTimeMap.get(key);
        long currentTime = System.currentTimeMillis();
        return (currentTime - entryTime) > maxAgeInMillis;
    }

 }
