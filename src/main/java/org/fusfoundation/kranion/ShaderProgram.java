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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL21.*;

/**
 *
 * @author john
 */
public class ShaderProgram {
    
    protected int shaderProgramID = 0;    
    protected List<Shader> shaderList = new ArrayList<Shader>();
    
    public ShaderProgram() {}
    
    public int getShaderProgramID() { return shaderProgramID; }
    
    public void start() {
        if (shaderProgramID != 0) {
            glUseProgram(shaderProgramID);
        }
    }
    
    public void stop() {
        glUseProgram(0);
    }
    
    public void addShader(Shader s) {
        shaderList.add(s);
    }
    
    public void addShader(int shaderType, String resourceName) {
        Shader s = new Shader(shaderType, resourceName);
        shaderList.add(s);
    }
    
    public void addShaderSourceString(int shaderType, String shaderSource) {
        Shader s = new Shader();
        s.addShaderSourceString(shaderType, shaderSource);
        shaderList.add(s);
    }
    
    public void removeShader(Shader s) {
        shaderList.remove(s);
    }
    
    public void deleteShaderProgram() {
        if (shaderProgramID != 0) {
            for (int i=0; i<shaderList.size(); i++) {
                shaderList.remove(i).deleteShader();
            }
            glDeleteProgram(shaderProgramID);
            shaderProgramID = 0;
        }
    }
    
    public void compileShaderProgram() {
        if (shaderProgramID != 0) return;
        
        System.out.println("Attaching and linking shader objects");

        shaderProgramID = glCreateProgram();
        
        for (ListIterator<Shader> it = shaderList.listIterator(); it.hasNext();) {
            Shader s = it.next();
            glAttachShader(shaderProgramID, s.getShaderID());
        }

        glLinkProgram(shaderProgramID);
        //glValidateProgram(shaderProgramID);       
        IntBuffer resultValidate = BufferUtils.createIntBuffer(1);
        //glGetProgram(shaderProgramID, GL_VALIDATE_STATUS, resultValidate);
        glGetProgram(shaderProgramID, GL_LINK_STATUS, resultValidate);
        
        if (resultValidate.get() == 0) {
            System.out.println("Shader validaiton error:");
            System.out.println(GetProgramInfoLog());
//            System.exit(-1); // TODO: throw an exception so caller can handle
        }         
    }
    
    public String GetProgramInfoLog() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 10);
        buffer.order(java.nio.ByteOrder.nativeOrder());
        ByteBuffer tmp = ByteBuffer.allocateDirect(4);
        tmp.order(java.nio.ByteOrder.nativeOrder());
        IntBuffer intBuffer = tmp.asIntBuffer();

        glGetProgramInfoLog(shaderProgramID, intBuffer, buffer);
        int numBytes = intBuffer.get(0);
        byte[] bytes = new byte[numBytes];
        buffer.get(bytes);
        return new String(bytes);
    }
    
    public void release() {
        deleteShaderProgram();
    }
    
    @Override
    protected void finalize() throws Throwable
    {
        release();
        super.finalize();
    }
}
