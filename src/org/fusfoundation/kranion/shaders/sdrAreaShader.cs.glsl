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
#version 430
#extension GL_ARB_compute_shader : enable
#extension GL_ARB_shader_storage_buffer_object : enable



struct elemEnd{
         vec4 pos[6]; // calculated by the refraction path shader in previous step
};

struct elemDistance {
		float dist;
                float sdr;
                float incidentAngle;
                float skullThickness;
                float sdr2;
                float normSkullThickness;
                float skullTransmissionCoeff;
};

struct sdrRecord {
         float      huSamples[60];
         float      maxVal1, maxVal2;
         float      minVal;
};

layout(std430, binding=0) buffer inputGeometry{
          elemEnd inputv[];
};

layout(std430, binding=1) buffer resultDistance{
		elemDistance d[];
};

layout(std430, binding=2) buffer resultSDR{
          sdrRecord sdr[];
};

// compute shader dispatch layout
layout (local_size_x = 256, local_size_y = 1, local_size_z = 1) in;

uniform sampler3D   ct_tex;
uniform mat4        ct_tex_matrix;
uniform float       ct_rescale_intercept;
uniform float       ct_rescale_slope;

#define M_PI 3.1415926535897932384626433832795

float ctMin, ctMax;

float remap( float minval, float maxval, float curval )
{
    return clamp (( curval - minval ) / ( maxval - minval ), 0, 1);
}

void main()
{
    uint gid = gl_GlobalInvocationID.x;
    vec3 pos1 = inputv[gid].pos[2].xyz; // point on the outside of the skull
    vec3 pos2 = inputv[gid].pos[3].xyz; // point on the inside of the skull w/ refraction
//    vec3 v = normalize(inputv[gid].pos[0].xyz - inputv[gid].pos[1].xyz); // no refraction
    vec3 v = normalize(pos2 - pos1); // with refraction
    float skullThickness = length(pos2 - pos1);
    float finalRaySegmentDistance = d[gid].dist; // -1 indicates ineffective element

    if (finalRaySegmentDistance == -1.0) {
        d[gid].sdr = -1; // init to failure
        d[gid].sdr2 = -1; // init to failure
        sdr[gid].maxVal1 = -1;
        sdr[gid].maxVal2 = -1;
        sdr[gid].minVal = -1;
        return;
    }

    vec3 xvec = normalize(cross(v, vec3(0, 0, 1)));
    vec3 yvec = normalize(cross(v, xvec));

    float sdrSum = 0.0;
    float sdr2Sum = 0.0;
    float sdrDivisor = 0.0;

    const float offsets[5] = float[5](-2, -1, 1, 2, 0);

    for (int x=0; x<5; x++) {
        for (int y=0; y<5; y++) {

            vec3 pp = pos1 - v*10.0 + 2.25*offsets[x]*xvec + 2.25*offsets[y]*yvec;

            for (int i=0; i<60; i++) {
                vec3 tcoord = (ct_tex_matrix * vec4 (pp, 1)).xyz;
                float ctsample = texture(ct_tex,tcoord).r * 65535.0  * ct_rescale_slope + ct_rescale_intercept;

                sdr[gid].huSamples[i] = ctsample;

                pp += v * 0.5; // step 0.5 mm from pos1 to pos2

            }

            d[gid].sdr = -1; // init to failure
            d[gid].sdr2 = -1; // init to failure

            sdr[gid].maxVal1 = -1;
            sdr[gid].maxVal2 = -1;
            sdr[gid].minVal = -1;

            int leftStartIndex = 0;
            int rightStartIndex = 59;
            int leftPeakIndex = 0;
            int rightPeakIndex = 59;

            // Find the left bone threshold index
            for (int i=0; i<60; i++) {
                leftStartIndex = i;
                if (sdr[gid].huSamples[i] >= 700) {
                    break;
                }
            }

            // Find the right bone threshold index
            for (int i=59; i>=0; i--) {
                rightStartIndex = i;
                if (sdr[gid].huSamples[i] >= 700) {
                    break;
                }
            }

            // Find the left peak
            for (int i=leftStartIndex; i<59; i++) {
                leftPeakIndex = i;
                if (sdr[gid].huSamples[i+1] < sdr[gid].huSamples[i]) {
                    break;
                }
            }

            // Find the right peak
            for (int i=rightStartIndex; i>=1; i--) {
                rightPeakIndex = i;
                if (sdr[gid].huSamples[i-1] < sdr[gid].huSamples[i]) {
                    break;
                }
            }

            // Find the minimum value between peaks
            float minValue = 4096.0;
            int minValueIndex = -1;
            for (int i=leftPeakIndex; i<=rightPeakIndex; i++) {
                if (sdr[gid].huSamples[i] < minValue) {
                    minValue = max(0, sdr[gid].huSamples[i]);
                    minValueIndex = i;
                }
            }

            sdr[gid].maxVal1 = leftPeakIndex;
            sdr[gid].maxVal2 = rightPeakIndex;
            sdr[gid].minVal = minValueIndex;

            if (leftPeakIndex != -1 && rightPeakIndex != -1 && minValueIndex != -1) {
                if (offsets[x]==0 && offsets[y]==0) {
                    sdrSum += minValue/((sdr[gid].huSamples[leftPeakIndex] + sdr[gid].huSamples[rightPeakIndex])/2.0); // old insightec method
                }

                if (sdr[gid].huSamples[leftPeakIndex] != 0.0) {
                    sdrDivisor += 1.0;
                    sdr2Sum += minValue/sdr[gid].huSamples[leftPeakIndex]; // new insightec method, just outer maximum used
                }
            }

        } // y for loop
    } // x for loop


    d[gid].sdr = (sdrSum == 0.0 ? -1 : sdrSum);

    if (sdrDivisor == 0.0) {
        d[gid].sdr2 = -1;
    }
    else {
        d[gid].sdr2 = sdr2Sum / sdrDivisor;
    }
}