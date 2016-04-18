package com.bradsdeals.clojuresupport.loader;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

    public void start(BundleContext context) throws Exception {
        DynamicClojure.start(context);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }

}
