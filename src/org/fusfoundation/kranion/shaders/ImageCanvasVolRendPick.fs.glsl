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
#version 130

uniform int slice;
uniform int last_slice;
uniform int showMR;
uniform float center;
uniform float window;
uniform float mr_center;
uniform float mr_window;
uniform float mr_rescale_intercept;
uniform float mr_rescale_slope;
uniform float ct_rescale_intercept;
uniform float ct_rescale_slope;
uniform float ct_threshold;
uniform float mr_threshold;
uniform sampler3D ct_tex;
uniform sampler3D mr_tex;
uniform sampler3D ct_grad_tex;
uniform sampler1D lut_tex;
uniform sampler3D ovly_tex;
//layout(location=0) out vec4 colorout;
varying vec3 frag_position;
uniform vec3 light_position;
uniform vec3 eye_position;

#define minOpacity 0.95
#define opacityWindow 250.0
#define LIGHTING_ALPHA_THRESHOLD 0.001


void main(void)
{
       vec4 color = vec4(1);
        float ctTexVal = texture3D(ct_tex, gl_TexCoord[0].stp).r;
        color = texture1D(lut_tex, ctTexVal);

        float ctsample = ctTexVal * 65535.0  * ct_rescale_slope + ct_rescale_intercept;

        // only pick on bone
        color.a = ctsample > 650.0 ? color.a : 0.0;

        float mrsample = 0;
        float ctval = (ctsample - center)/(window) + 0.5;
        bool noLighting = false;
        bool useMRval = false;


        if (slice==last_slice) {
            if (showMR==1) {
                mrsample = texture3D(mr_tex, gl_TexCoord[1].stp).r * 65535.0  * mr_rescale_slope + mr_rescale_intercept;
                if (mrsample < mr_threshold && color.a < 0.05) {
                    discard;
                }
                if (color.a < 0.4) {
                    noLighting = true;
                    useMRval = true;
                }
            }
            else {
                if (/*ctOpacity*/ color.a < 0.01)
                    discard;
            }
        }   
        else {
            if (/*ctOpacity*/ color.a  < 0.01)
                discard;
        }

        vec3 color_tmp = vec3(0.0, 0.0, 0.0);
        // compute the gradient and lighting only if the pixel is visible "enough"
        if (slice != last_slice && color.a > LIGHTING_ALPHA_THRESHOLD)
        {
                 color_tmp = vec3(1.0/255.0, 0, 0);
        }
        else {
            if (useMRval) {
                color_tmp = vec3(2.0/255.0, 0, 0);
            }
            else {
                color_tmp = vec3(1.0/255.0, 0, 0);
            }
        }

        gl_FragColor = vec4(color_tmp, 1);
}