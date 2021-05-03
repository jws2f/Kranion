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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL20.*;


/**
 *
 * @author john
 */
public class Shader {
    private int shaderID = 0;
    
    public Shader() {}
    
    public Shader(int shaderType, String resourceName) {
        addShaderSource(shaderType, resourceName);
    }

    private String readResourceFile(String filename) throws IOException {
        InputStream inputStream = this.getClass().getResourceAsStream(filename);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString("UTF-8");
    }

    public int getShaderID() { return shaderID; }
    
    public void deleteShader() {
        if (shaderID != 0) {
            glDeleteShader(shaderID);
            shaderID = 0;
        }
    }
    
    public void addShaderSource(int shaderType, String resourceName)
    {
        // clean up
        deleteShader();
        
        shaderID = glCreateShader(shaderType);

        String shaderSrc = new String();

        try {
            shaderSrc = readResourceFile(resourceName);
//            System.out.println(shaderSrc);
        } catch (IOException e) {
//            System.out.println(e);
            Logger.getGlobal().log(Level.WARNING, "Shader", e);

            System.exit(-1);
        }

//        System.out.println("Compiling shader: " + resourceName);

        glShaderSource(shaderID, shaderSrc);
        glCompileShader(shaderID);                 
        printShaderInfoLog();        
    }
    
    public void addShaderSourceString(int shaderType, String source)
    {
        // clean up
        deleteShader();
        
        shaderID = glCreateShader(shaderType);

//        System.out.println("Compiling shader from string:");

        glShaderSource(shaderID, source);
        glCompileShader(shaderID);                 
        printShaderInfoLog();        
    }
        
    private void printShaderInfoLog() {
        Logger.getGlobal().log(Level.INFO, GetShaderInfoLog());
    }
    
    public String GetShaderInfoLog() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 10);
        buffer.order(java.nio.ByteOrder.nativeOrder());
        ByteBuffer tmp = ByteBuffer.allocateDirect(4);
        tmp.order(java.nio.ByteOrder.nativeOrder());
        IntBuffer intBuffer = tmp.asIntBuffer();

        glGetShaderInfoLog(shaderID, intBuffer, buffer);
        int numBytes = intBuffer.get(0);
        byte[] bytes = new byte[numBytes];
        buffer.get(bytes);
        return new String(bytes);
    }
    
    public void release() {
        deleteShader();        
    }
    
    @Override
    protected void finalize() throws Throwable
    {
        release();
        super.finalize();
    }
}
