/*
 * The MIT License
 *
 * Copyright 2016 Focused Ultrasound Foundation.
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

/**
 *
 * @author john
 */

// Since the sigmoid shaped transition is fairly pleasing for
// so many transition animations, I have put this strategy into
// this separate utility class that other Animators can delegate to.
public class AnimatorUtil implements Animator {
    private long startTime, endTime, currentTime;
    private float Start, End, Current;
    
    public AnimatorUtil() {
        endTime = startTime = currentTime = 0L;
    }
    
    public AnimatorUtil(float start, float end, float durationSecs)
    {
        set(start, end, durationSecs);
    }
    
    public final void set(float start, float end, float durationSecs) {
       Start = start;
       End = end;
       startTime = currentTime = System.currentTimeMillis();
       endTime = startTime + Math.round(durationSecs*1000d);
    }
    
    public float getCurrent() { return Current; }
    public float getStart() { return Start; }
    public float getEnd() { return End; }
    
    @Override
    public void cancelAnimation() { endTime = startTime; }
    
    @Override
    public boolean isAnimationDone() {
        return currentTime >= endTime;
    }

    @Override
    public void advanceFrame()
    {
        currentTime = System.currentTimeMillis();
        
        float t;
        if (endTime > startTime) {
            t = Math.max(0f, Math.min(1f, (float)(currentTime - startTime)/(float)(endTime - startTime)));
            
            // rescale sigmoid result between 0 and 1
            float low = (sigmoid(0f));
            float high = (sigmoid(1f));
            
            float sigmoidT = (sigmoid(t) - low) * 1f/(high - low);
            //System.out.println(sigmoidT);

            Current = Start + (End - Start)*sigmoidT;
        }
    }
    
    public static float sigmoid(float t) {
            return sigmoid(t, 10f);
    }
    
    public static float sigmoid(float t, float gain) {
            return (1f / (1f + (float)Math.exp(-((t-0.5f)*gain))));
    }    
}
