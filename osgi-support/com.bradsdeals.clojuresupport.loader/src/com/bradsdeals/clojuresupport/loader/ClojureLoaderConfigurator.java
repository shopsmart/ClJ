package com.bradsdeals.clojuresupport.loader;

import org.eclipse.osgi.internal.hookregistry.HookConfigurator;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;

public class ClojureLoaderConfigurator implements HookConfigurator {

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.osgi.baseadaptor.HookConfigurator#addHooks(org.eclipse.osgi.baseadaptor.HookRegistry)
     */
    @Override
    public void addHooks(HookRegistry hookRegistry) {
         hookRegistry.addClassLoaderHook(new WorkspaceClassLoadingHook(hookRegistry.getConfiguration()));
//        hookRegistry.addBundleFileWrapperFactoryHook(new JarLoaderBundleFileWrapperFactory());
    }

}