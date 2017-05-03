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

import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUniform4f;
import static org.lwjgl.opengl.GL20.glUseProgram;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author john
 */
public class StandardShader extends ShaderProgram {
    private static int staticShaderID = 0;
    private static int refCount = 0;
    
    private Vector4f ambientColor = new Vector4f();
    private Vector4f diffuseColor = new Vector4f();
    private Vector4f specularColor = new Vector4f();
    private float specularCoefficient;
    private int flipNormals = 0;
    
    public StandardShader() {
        if (staticShaderID == 0) {
            this.addShader(GL_VERTEX_SHADER, "shaders/standardADS.vs.glsl");
            this.addShader(GL_FRAGMENT_SHADER, "shaders/standardADS.fs.glsl");
            this.compileShaderProgram();
            staticShaderID = super.getShaderProgramID();
        }
        
        super.shaderProgramID = staticShaderID;
        
        refCount++;
    }
    
    public void setAmbientColor(float red, float green, float blue, float alpha) {
        ambientColor.set(red, green, blue, alpha);
    }
    
    public void setDiffusetColor(float red, float green, float blue, float alpha) {
        diffuseColor.set(red, green, blue, alpha);
    }
    
    public void setSpecularColor(float red, float green, float blue, float alpha) {
        specularColor.set(red, green, blue, alpha);
    }
    
    public void setSpecularCoefficient(float specularity) {
        specularCoefficient = specularity;
    }
    
    public void setFlipNormals(boolean flip) {
        flipNormals = flip ? 1 : 0;
    }
        
    @Override
    public void start() {
        super.start();
            
        int uniformLoc = glGetUniformLocation(getShaderProgramID(), "ambientColor");
        glUniform4f(uniformLoc, ambientColor.x, ambientColor.y, ambientColor.z, ambientColor.w);

        uniformLoc = glGetUniformLocation(getShaderProgramID(), "diffuseColor");
        glUniform4f(uniformLoc, diffuseColor.x, diffuseColor.y, diffuseColor.z, diffuseColor.w);

        uniformLoc = glGetUniformLocation(getShaderProgramID(), "specularColor");
        glUniform4f(uniformLoc, specularColor.x, specularColor.y, specularColor.z, specularColor.w);

        uniformLoc = glGetUniformLocation(getShaderProgramID(), "specularCoefficient");
        glUniform1f(uniformLoc, specularCoefficient);

        uniformLoc = glGetUniformLocation(getShaderProgramID(), "flipNormals");
        glUniform1i(uniformLoc, flipNormals);
    }
    
    @Override
    public void release() {
        deleteShaderProgram();
    }
    
    @Override
    public void deleteShaderProgram() {
        if (refCount > 0) {
            refCount--;

            if (refCount == 0 && staticShaderID != 0) {
                glDeleteProgram(staticShaderID);
                staticShaderID = 0;
            }
        }
        // otherwise if we have any shaders delete them
        for (int i = 0; i < shaderList.size(); i++) {
            shaderList.remove(i).deleteShader();
        }
    }
}
