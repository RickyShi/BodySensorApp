package edu.missouri.bas.service;

import java.lang.ref.WeakReference;

import android.os.Binder;

//Binder has a memory leak, they will be leaked
//every time they are used to connect to a service
public class ServiceBinder<S> extends Binder {

	private  WeakReference<S> mService;

    public ServiceBinder(S service){
        mService = new WeakReference<S>(service);
    }
    
    public S getService() {
        return mService.get();
    }
}
