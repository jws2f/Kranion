/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fusfoundation.kranion;

import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 *
 * @author jsnell
 */
public class CrossThreadCallableManager {
    private final Queue<CrossThreadCallable> callQ = new ArrayDeque(16);

    public CrossThreadCallableManager() {}
    
    public Object call(CrossThreadCallable c) {
        callQ.add(c);
        synchronized (callQ) {
            callQ.add(c);
        }

        return c.getResult();
    }
    
    public void processWaitingCalls() {
        synchronized (callQ) {
            while (!callQ.isEmpty()) {
                CrossThreadCallable callable = callQ.remove();
                callable.callAndNotify();
            }
        }
    }
}
