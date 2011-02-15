package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;



public class Properties {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Map<String, PropValue> values = new HashMap();

    // ---------------------------------------------------------------------------------------------------- Constructors

    public Properties() {
    }

    public Properties(Properties properties) {
        putAll(properties);
    }

    public Properties(Map<String, Object> map) {
        for (String key : map.keySet()) {
            put(key, new PropValue(map.get(key)));
        }
    }

    public Properties(JSONObject properties) {
        try {
            Iterator<String> i = properties.keys();
            while (i.hasNext()) {
                String key = i.next();
                PropValue value = new PropValue(properties.get(key));   // throws JSONException
                put(key, value);
            }
        } catch (JSONException e) {
            throw new RuntimeException("Constructing Properties from JSONObject failed", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public PropValue get(String key) {
        return values.get(key);
    }

    // ---

    public void put(String key, String value) {
        values.put(key, new PropValue(value));
    }

    public void put(String key, int value) {
        values.put(key, new PropValue(value));
    }

    public void put(String key, long value) {
        values.put(key, new PropValue(value));
    }

    public void put(String key, boolean value) {
        values.put(key, new PropValue(value));
    }

    public void put(String key, PropValue value) {
        values.put(key, value);
    }

    // ---

    public void putAll(Properties properties) {
        for (String key : properties.keySet()) {
            put(key, properties.get(key));
        }
    }

    // ---

    public Set<String> keySet() {
        return values.keySet();
    }

    // ---

    public JSONObject toJSON() {
        try {
            JSONObject o = new JSONObject();
            for (String key : keySet()) {
                o.put(key, get(key).value());
            }
            return o;
        } catch (JSONException e) {
            throw new RuntimeException("Serializing " + this + " failed", e);
        }
    }

    public Map toMap() {
        Map map = new HashMap();
        for (String key : keySet()) {
            map.put(key, get(key).value());
        }
        return map;
    }
}
