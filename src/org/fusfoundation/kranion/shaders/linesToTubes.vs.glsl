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
#version 330

layout(location = 0) in vec4 position;
layout(location = 1) in vec4 color;

uniform  mat4 projection;
uniform  mat4 view;
uniform  mat4 model;
uniform  mat4 normal;
uniform  mat4 orientation;

uniform vec3 viewPos;
uniform vec3 lightPos;
uniform vec3 currentTarget;

out vec4 gorigin;
out vec4 gcolor;
out vec4 fecPos;
out vec4 fclipVertex;

out gl_PerVertex
{
  vec4 gl_Position;
  float gl_ClipDistance[2];
};

void main()
{   
    
    /* first transform the normal into eye space and normalize the result */
    //fnormal = normalize(gl_NormalMatrix * gl_Normal);
    gcolor = color;
           
    /* compute the vertex position  in eye space. */
    fclipVertex = model * position;
    fecPos = fclipVertex;

    vec4 originPt = model * orientation * vec4(currentTarget, 1);
    gorigin = vec4(0, 0, 1, length(originPt.xyz));

    gl_Position = (projection * model) * position;

} 