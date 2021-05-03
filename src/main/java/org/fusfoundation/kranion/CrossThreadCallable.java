/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fusfoundation.kranion;

/**
 *
 * @author jsnell
 */
public abstract class CrossThreadCallable {
    protected Object result = null;
    
    // called on the interpreter thread
    public Object getResult() {
        synchronized(this) {
            try {
                wait();
            }
            catch(InterruptedException e) {                
            }
        }
        return result;
    };
    
    // called on the main thread
    public void callAndNotify() {
        synchronized(this) {
            result = call();
            notifyAll();
        }
    }
    
    public abstract Object call();
}
