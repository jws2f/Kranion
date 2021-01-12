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

uniform sampler3D   ovly_tex;
uniform sampler1D   ovly_lut_tex;
uniform float       ovly_lut_tex_min;
uniform float       ovly_lut_tex_max;

//layout(location=0) out vec4 colorout;
varying vec3 frag_position;
uniform vec3 light_position;
uniform vec3 eye_position;

#define minOpacity 0.95
#define opacityWindow 250.0
#define LIGHTING_ALPHA_THRESHOLD 0.001

// computes a simplified lighting equation
vec3 blinn_phong(vec3 N, vec3 V, vec3 L, int light)
{
    // material properties
    // you might want to put this into a bunch or uniforms
    vec3 Ka = vec3(0.2, 0.2, 0.2);
    vec3 Kd = vec3(1.0, 1.0, 1.0);
    vec3 Ks = vec3(0.25, 0.25, 0.25);
    float shininess = 30.0;

    // diffuse coefficient
    float diff_coeff = max(dot(L,N),0.0);

    // specular coefficient
    vec3 H = normalize(L+V);
    float spec_coeff = pow(max(dot(H,N), 0.0), shininess);
    if (diff_coeff <= 0.0) 
        spec_coeff = 0.0;

    // final lighting model
    return  Ka * gl_LightSource[light].ambient.rgb + 
            Kd * gl_LightSource[light].diffuse.rgb  * diff_coeff + 
            Ks * gl_LightSource[light].specular.rgb * spec_coeff ;
}

void main(void)
{
       vec4 color = vec4(1);
        float ctTexVal = texture3D(ct_tex, gl_TexCoord[0].stp).r;
        float pval = gl_TexCoord[5].p;
        if (pval >=0 && pval <= 1) pval = 0.5;
        float ovlyTexVal = texture3D(ovly_tex, vec3(gl_TexCoord[5].st, pval)).r; //floating point texture now, so no * 32767.0;
        //float ctOpacity = texture1D(lut_tex, ctTexVal).r;
        color = texture1D(lut_tex, ctTexVal);

        float ctsample = ctTexVal * 65535.0 * ct_rescale_slope + ct_rescale_intercept;
        float mrsample = 0;
        float ctval = (ctsample - center)/window + 0.5;
        bool noLighting = false;
        bool useMRval = false;

        vec4 ovlyColor = vec4(0.0);


/*
        if (ovlyTexVal > 699.0) {
            ovlyColor = vec4(0.3, 1.0, 0.3, 1.0);
        }
        else if (ovlyTexVal > 499.0) {
            ovlyColor = vec4(1, 1, 0.3, 1.0);
        }
        else if (ovlyTexVal > 399.0) {
            ovlyColor = vec4(1.0, 0.7, 0.7, 1.0);
        }
*/
/*
        if (ovlyTexVal <= 43.0) {
            //ovlyColor = mix(vec4(0.0, 0.0, 0.0, 0.0), vec4(0.1, 0.1, 0.7, 0.6), clamp((ovlyTexVal-37.0)/6.0f, 0, 1));
            ovlyColor = mix(vec4(0.0, 0.0, 0.0, 0.0), vec4(0.1, 0.1, 0.7, 0.0), clamp((ovlyTexVal-37.0)/6.0f, 0, 1));
        }
        else if (ovlyTexVal <= 55.0) {
            //ovlyColor = mix(vec4(0.1, 0.1, 0.7, 0.65), vec4(1, 0.3, 0.3, 1), clamp((ovlyTexVal-43)/12.0, 0, 1));
            ovlyColor = mix(vec4(0.1, 0.1, 0.7, 0.05), vec4(1, 0.3, 0.3, 1), clamp((ovlyTexVal-43)/12.0, 0, 1));
        }
        else {
            ovlyColor = mix(vec4(1, 0.3, 0.3, 1), vec4(1, 0.8, 0.8, 1), clamp((ovlyTexVal-55)/5.0, 0, 1));
        }
*/
        if (slice==last_slice) {
            ovlyColor = texture1D(ovly_lut_tex, clamp((ovlyTexVal - ovly_lut_tex_min)/(ovly_lut_tex_max - ovly_lut_tex_min), 0, 1));

            if (showMR==1) {
                mrsample = texture3D(mr_tex, gl_TexCoord[1].stp).r * 65535.0 * mr_rescale_slope + mr_rescale_intercept;
                if (mrsample < mr_threshold && color.a < 0.05 && ovlyTexVal == 0) {
                    discard;
                }
                if (color.a < 0.4) {
//                    float mrval = (mrsample - mr_center)/mr_window;
//                    color.rgb = vec3(mrval);
//                    color.a = 1;
                    noLighting = true;
                    useMRval = true;
                }
//                else {
//                    if (ctsample < max(ct_threshold-150, 20) )
//                        discard;
//                    color.rgb = vec3(ctval, ctval*0.9, ctval*0.5);
//                    color.a = min(minOpacity, (ctsample-(ct_threshold-150))/opacityWindow);
//                }
            }
            else {
                if (/*ctOpacity*/ color.a < 0.01 && ovlyTexVal == 0)
                    discard;
                    color.rgb = color.rgb * ctval;
                //color.rgb = vec3(ctval, ctval*0.9, ctval*0.5);
//                color.rgb = vec3(0.9, 0.9*0.9, 0.9*0.5);
//                color.a = ctOpacity;
            }
        }   
        else {
            if (/*ctOpacity*/ color.a  < 0.01)
                discard;
            //color.rgb = vec3(ctval, ctval*0.9, ctval*0.5);
//            color.rgb = vec3(0.9, 0.9*0.9, 0.9*0.5);
//            color.a = ctOpacity;        
        }

        vec3 color_tmp = vec3(0.0, 0.0, 0.0);
        vec3 gradient_delta = vec3(0.003, 0.003, 0.00375);
//        vec3 gradient_delta = vec3(0.006, 0.006, 0.0075);
//        vec3 eye_position = vec3(0, 0, -600);
//        vec3 light_position = vec3(150, 150, 400);

        // compute the gradient and lighting only if the pixel is visible "enough"
        if (slice != last_slice && !noLighting && color.a > LIGHTING_ALPHA_THRESHOLD)
        {
            vec3 N;

            // on-the-fly gradient computation: slower but requires less memory (no gradient texture required).
            vec4 diff;//, sample1, sample2;
//            sample1.x = texture3D(ct_tex, gl_TexCoord[0].xyz-vec3(gradient_delta.x,0.0,0.0) ).r * 4095;
//            sample2.x = texture3D(ct_tex, gl_TexCoord[0].xyz+vec3(gradient_delta.x,0.0,0.0) ).r * 4095;
//            sample1.y = texture3D(ct_tex, gl_TexCoord[0].xyz-vec3(0.0,gradient_delta.y,0.0) ).r * 4095;
//            sample2.y = texture3D(ct_tex, gl_TexCoord[0].xyz+vec3(0.0,gradient_delta.y,0.0) ).r * 4095;
//            sample1.z = texture3D(ct_tex, gl_TexCoord[0].xyz-vec3(0.0,0.0,gradient_delta.z) ).r * 4095;
//            sample2.z = texture3D(ct_tex, gl_TexCoord[0].xyz+vec3(0.0,0.0,gradient_delta.z) ).r * 4095;
//            diff = sample1 - sample2;

            // Lookup precalculated gradient
            diff = texture3D(ct_grad_tex, gl_TexCoord[0].xyz);
            
            if (diff.a > 1.0) {
                N  = normalize(diff.xyz);//normalize( diff);

                vec3 V  = normalize(eye_position - frag_position);
                vec3 L = normalize(light_position - frag_position);
                color_tmp.rgb += color.rgb * blinn_phong(N,V,L,0);
            }
            else {
                color_tmp = color.rgb;
            }
        }
        else {
            if (useMRval) {
                float mrval = (mrsample - mr_center)/mr_window + 0.5;
                color.rgb = vec3(mrval);
                if (mrsample>mr_threshold) {
                    color.a = 1;
                }
                else {
                    color.a = 0;
                }
                color_tmp = color.rgb;
            }
            else {
                color_tmp = color.rgb * ctval;
            }
        }

        //float alpha = ovlyTexVal.a;
         //gl_FragColor = vec4((1 - alpha)color_tmp.rgb + alpha * ovlyTexVal.rgb, alpha);
        gl_FragColor = mix(vec4(color_tmp,color.a), ovlyColor, ovlyColor.a);
        gl_FragColor.a = color.a;
       //gl_FragColor = vec4(color.a,color.a,color.a,color.a) * ovlyColor;
}