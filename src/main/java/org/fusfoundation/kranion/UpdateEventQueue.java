/*
 * The MIT License
 *
 * Copyright 2017 john.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.fusfoundation.kranion;

import java.util.Observer;
import java.util.Observable;

/**
 *
 * @author John Snell
 */
public class UpdateEventQueue {
    private final java.util.Queue<updateEvent> updateEventQueue = new java.util.LinkedList<>();

    public UpdateEventQueue() {
        
    }
    
    private class updateEvent {
        public updateEvent(Observable o, Object arg) {
            this.o = o;
            this.arg = arg;
        }
        public Observable o;
        public Object arg;
    }
    
    public synchronized void push(Observable o, Object arg) {
        updateEventQueue.add(new updateEvent(o, arg));
    }
    
    public synchronized void handleEvents(Observer target) {
        updateEvent e;
        while((e = updateEventQueue.poll()) != null) {
            target.update(e.o, e.arg);
        }        
    }
    
    public synchronized boolean isEmpty() {
        return updateEventQueue.isEmpty();
    }    
}
