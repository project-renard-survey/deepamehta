package de.deepamehta.plugins.webclient;

import de.deepamehta.core.osgi.Activator;
import de.deepamehta.plugins.webservice.provider.TopicProvider;

import java.util.HashSet;
import java.util.Set;



public class Application extends javax.ws.rs.core.Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(TopicProvider.class);
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> singletons = new HashSet<Object>();
        singletons.add(Activator.getService().getPlugin("de.deepamehta.webclient"));
        return singletons;
    }
}
