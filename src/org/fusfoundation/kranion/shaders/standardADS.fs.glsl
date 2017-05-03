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
uniform vec4 ambientColor;
uniform vec4 diffuseColor;
uniform vec4 specularColor;
uniform float specularCoefficient;
varying vec3 normal;
varying vec4 ecPos; // fragment eye space coordinate

void main()
{
    vec3 n, lightDir;
     
    n = normalize(normal);
     
    // Compute the ligt direction
    lightDir = normalize(vec3(gl_LightSource[0].position.xyz-ecPos.xyz));
     
    vec3 E = normalize(-ecPos.xyz);

    vec3 R = normalize(-reflect(lightDir, n));

    vec4 Idiff = diffuseColor * max(dot(n, lightDir), 0.0);

    vec4 Ispec = specularColor * pow(max(dot(R, E), 0.0), specularCoefficient);
 
    gl_FragColor = vec4((ambientColor.rgb + Idiff.rgb + Ispec.rgb), diffuseColor.a);

}