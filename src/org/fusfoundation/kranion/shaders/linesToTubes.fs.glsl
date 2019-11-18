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
uniform vec3 viewPos;
uniform vec3 lightPos;


in vec4 fcolor;
in vec4 fecPos;
//in vec4 fclipVertex;
in vec3 fnormal;


out vec4 outputColor;

void main()
{
    vec3 n, lightDir;
     
    n = normalize(fnormal);
     
    // Compute the ligt direction
    lightDir = normalize(vec3(lightPos - fecPos.xyz));
     
    vec3 E = normalize(-fecPos.xyz);

    vec3 R = normalize(-reflect(lightDir, n));

    vec4 Iamb = vec4(fcolor.xyz*0.05, 1); //gl_FrontLightProduct[0].ambient;

    vec4 Idiff = fcolor /*gl_FrontLightProduct[0].diffuse*/ * max(abs(dot(n, lightDir)), 0.0);


    vec4 Ispec = vec4(0.3, 0.3, 0.3, 1) /*gl_FrontLightProduct[0].specular*/ * pow(max(dot(R, E), 0.0), 0.3*50.0);
 
    outputColor = Iamb + Idiff + Ispec;

}