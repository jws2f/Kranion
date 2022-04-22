/* 
 * The MIT License
 *
 * Copyright 2021 Focused Ultrasound Foundation.
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
};

struct sdrRecord {
         float      huSamples[60];
         float      maxVal1, maxVal2;
         float      minVal;
};

struct skullParams {
    float corticalBoneSpeed;
    float averageBoneSpeed;
    float innerCorticalBoneSpeed;
};

layout(std430, binding=0) buffer inputGeometry{
          elemEnd input[];
};

layout(std430, binding=1) buffer resultDistance{
		elemDistance d[];
};

layout(std430, binding=2) buffer resultSDR{
          sdrRecord sdr[];
};

layout(std430, binding=3) buffer skullParamsPerElement{
		skullParams boneSpeeds[];
};

// compute shader dispatch layout
layout (local_size_x = 256, local_size_y = 1, local_size_z = 1) in;

uniform int         elementCount;
uniform float       normDiscRadius;
uniform sampler3D   ct_tex;
uniform mat4        ct_tex_matrix;
uniform float       ct_rescale_intercept;
uniform float       ct_rescale_slope;
uniform float       minSOSVal;
uniform float       maxSOSVal;
uniform sampler1D   sos_lut_tex;

#define M_PI 3.1415926535897932384626433832795

float ctMin, ctMax;

float remap( float minval, float maxval, float curval )
{
    return clamp (( curval - minval ) / ( maxval - minval ), 0, 1);
}

const float offsets[5] = float[5](-2, -1, 1, 2, 0);

// normalized fibonacci offsets
const float xoffsets[25] = float[25](
    0,
    -0.162357024,
    0.027223284,
    0.232040532,
    -0.43363683,
    0.415420375,
    -0.140014752,
    -0.268502681,
    0.584985959,
    -0.610579041,
    0.295117093,
    0.218557606,
    -0.659931776,
    0.775368889,
    -0.473822728,
    -0.10959006,
    0.673454708,
    -0.907067733,
    0.662162015,
    -0.044332747,
    -0.630902431,
    1,
    -0.847746165,
    0.231765004,
    0.53629817
);

const float yoffsets[25] = float[25](
    0,
    -0.144369673,
    0.301096544,
    -0.293778195,
    0.074454338,
    0.25650519,
    -0.505570233,
    0.501821201,
    -0.207369502,
    -0.244645295,
    0.612149729,
    -0.67635746,
    0.371225994,
    0.165462614,
    -0.654195159,
    0.820892637,
    -0.550939672,
    -0.036409841,
    0.639611433,
    -0.930614574,
    0.733855814,
    -0.130601967,
    -0.572538348,
    1,
    -0.908459374
);

#define NSAMPLES 150 // 0.2 mm steps

float avgBoneSpeed(int start, int end, float[NSAMPLES] values) {
    float   sosSum = 0;
    int     sosCount = 0;

    if (end >= start) {

        for (int i=start; i<=end; i++) {
            float sos = texture(sos_lut_tex, (values[i] + 1000)/4096).r;
            sosSum += sos * (maxSOSVal - minSOSVal) + minSOSVal;
            sosCount++;
        }
        return sosSum/sosCount;
    }

    return 0;
}

void main()
{
    uint gid = gl_GlobalInvocationID.x;

    float huSamples[NSAMPLES];

    if (gid>=elementCount) return;

    vec3 pos1 = input[gid].pos[2].xyz; // point on the outside of the skull
    vec3 pos2 = input[gid].pos[3].xyz; // point on the inside of the skull w/ refraction
    float skullThickness = length(pos2 - pos1);
    float finalRaySegmentDistance = d[gid].dist; // -1 indicates ineffective element

    vec3 v;
    if (skullThickness < 0.001) {
        v = normalize(input[gid].pos[0].xyz - input[gid].pos[1].xyz); // no refraction, prob internal reflection
    }
    else {
        v = normalize(pos2 - pos1); // with refraction
    }

    vec3 xvec = normalize(cross(v, vec3(0, 0, 1)));
    vec3 yvec = normalize(cross(v, xvec));

    boneSpeeds[gid].corticalBoneSpeed = 0;
    boneSpeeds[gid].innerCorticalBoneSpeed = 0;
    boneSpeeds[gid].averageBoneSpeed = 0;
    int outerRayCount = 0;
    int innerRayCount = 0;
    int avgRayCount = 0;

    float stepSize = 30.0 / NSAMPLES;

    for (int s=0; s<25; s++) {

        vec3 pp = pos1 - v*10.0 + normDiscRadius*xoffsets[s]*xvec + normDiscRadius*yoffsets[s]*yvec;

        for (int i=0; i<NSAMPLES; i++) {
            vec3 tcoord = (ct_tex_matrix * vec4 (pp, 1)).xyz;
            float ctsample = texture(ct_tex,tcoord).r * 65535.0  * ct_rescale_slope + ct_rescale_intercept;

            huSamples[i] = ctsample;

            pp += v * stepSize; // step 0.5 mm from pos1 to pos2
        }

//        d[gid].sdr = -1; // init to failure
//        d[gid].sdr2 = -1; // init to failure
//        sdr[gid].maxVal1 = -1;
//        sdr[gid].maxVal2 = -1;
//        sdr[gid].minVal = -1;

        int leftStartIndex = 0;
        int rightStartIndex = NSAMPLES-1;
        int leftPeakIndex = 0;
        int rightPeakIndex = NSAMPLES-1;

        // Find the left bone threshold index
        for (int i=0; i<NSAMPLES; i++) {
            leftStartIndex = i;
            if (huSamples[i] >= 700) {
                break;
            }
        }

        // Find the right bone threshold index
        for (int i=NSAMPLES-1; i>=0; i--) {
            rightStartIndex = i;
            if (huSamples[i] >= 700) {
                break;
            }
        }

        // Find the left peak
        for (int i=leftStartIndex; i<NSAMPLES-1; i++) {
            leftPeakIndex = i;
            if (huSamples[i+1] < huSamples[i]) {
                break;
            }
        }

        // Find the right peak
        for (int i=rightStartIndex; i>=1; i--) {
            rightPeakIndex = i;
            if (huSamples[i-1] < huSamples[i]) {
                break;
            }
        }

        // Find the minimum value between peaks
        float minValue = 4096.0;
        int minValueIndex = -1;
        for (int i=leftPeakIndex; i<=rightPeakIndex; i++) {
            if (huSamples[i] < minValue) {
                minValue = max(0, huSamples[i]);
                minValueIndex = i;
            }
        }

//        sdr[gid].maxVal1 = leftPeakIndex;
//        sdr[gid].maxVal2 = rightPeakIndex;
//        sdr[gid].minVal = minValueIndex;

        // setup for outer cortical layer sos estimation
        int halfSpan = (leftPeakIndex - leftStartIndex);
        int startIndex = leftPeakIndex - halfSpan; // just looking at outer cortical for now ****
        int endIndex = leftPeakIndex + halfSpan;

        // if there is more than one cortical peak, don't go past the min diploe point 
        if (rightPeakIndex > leftPeakIndex) {
            endIndex = min(endIndex, minValueIndex);
        }

        float sosVal = avgBoneSpeed(startIndex, endIndex, huSamples);
        if (sosVal > 0) {
            boneSpeeds[gid].corticalBoneSpeed += sosVal;
            outerRayCount++;
        }

        // setup for inner cortical layer sos estimation
        halfSpan = (rightStartIndex - rightPeakIndex);
        startIndex = rightPeakIndex - halfSpan; // inner cortical layer
        endIndex = rightPeakIndex + halfSpan;

        // if there is more than one cortical peak, don't go past the min diploe point 
        if (rightPeakIndex > leftPeakIndex) {
            startIndex = max(startIndex, minValueIndex);
        }

        sosVal = avgBoneSpeed(startIndex, endIndex, huSamples);
        if (sosVal > 0) {
            boneSpeeds[gid].innerCorticalBoneSpeed += sosVal;
            innerRayCount++;
        }

        //Do avg bone speed from outer surface to inner surface
        sosVal = avgBoneSpeed(leftStartIndex, rightStartIndex, huSamples);
        if (sosVal > 0) {
            boneSpeeds[gid].averageBoneSpeed += sosVal;
            avgRayCount++;
        }


    } // end multisample loop

    // calc average speed for all rays here
    boneSpeeds[gid].corticalBoneSpeed /= max(1, outerRayCount); 
    boneSpeeds[gid].innerCorticalBoneSpeed /= max(1, innerRayCount);
    boneSpeeds[gid].averageBoneSpeed /= max(1, avgRayCount); 
}
