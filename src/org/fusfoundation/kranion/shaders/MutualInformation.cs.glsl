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

/*
 * Sample dummy shader to check the highlighter plugin.
 */

#version 430 core
#extension GL_ARB_shading_language_include : require

#include "my-shader.glsl"

uniform vec3 position;
uniform uint elementCount;
uniform usampler3D textureSampler;

layout(binding = 0, r32ui) readonly uniform uimage3D myImage3D;

layout (local_size_x = 256) in;
void main() {
    if (gl_GlobalInvocationID.x >= elementCount) { return; }

    int number = 5 + 3;
    float alpha = 0.5f;    // line comment

    ivec3 texCoord = ivec3(0,0,0);
    uint value = imageLoad(myImage3D, texCoord);

    @$@$ // Invalid characters! They're only allowed inside comments: @$@$
}
