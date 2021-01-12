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
#version 120
varying vec4 ecPos;
varying vec4 wPos, color;
varying vec3 normal;

void main()
{
    vec3 n, lightDir;
    vec4 matColor = color;
     
    n = normalize(normal);
     
    // Compute the ligt direction
    lightDir = normalize(vec3(gl_LightSource[0].position.xyz-ecPos.xyz));
     
    vec3 E = normalize(-ecPos.xyz);

    vec3 R = normalize(-reflect(lightDir, n));

    vec4 Iamb = vec4(0.05, 0.05, 0.05, 1); //gl_FrontLightProduct[0].ambient;

    // If any fragments have world positions outside the transducer, color them red
    float radDist = length(wPos);
    float xyDist = length(wPos.xy);
    float interp = min(1.0, max(0.0, min(min((xyDist - 150.0), (radDist - 150.0)), (wPos.z + 35.0)/2.0)*5.0));

    matColor = mix(color, vec4(0.8, 0, 0, 1), interp);

    vec4 Idiff = matColor /*gl_FrontLightProduct[0].diffuse*/ * max(dot(n, lightDir), 0.0);


    vec4 Ispec = vec4(0.3, 0.3, 0.3, 1) /*gl_FrontLightProduct[0].specular*/ * pow(max(dot(R, E), 0.0), 0.3*50.0);
 
    gl_FragColor = Iamb + Idiff + Ispec;

}