/* 
 * The MIT License
 *
 * Copyright 2020 Focused Ultrasound Foundation.
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
        //callQ.add(c);
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
