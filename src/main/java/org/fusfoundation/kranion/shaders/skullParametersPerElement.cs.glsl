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

struct sdrRecord {
         float      huSamples[60];
         float      maxVal1, maxVal2;
         float      minVal;
};

struct skullParams {
    float corticalBoneSpeed;
    float averageBoneSpeed;
};

struct elemDistance {
		float distance;
                float sdr;
                float incidentAngle;
                float skullThickness;
                float sdr2;
                float skullNormThickness;
                float skullTransmissionCoeff;
};

layout(std430, binding=0) buffer sdrData{
          sdrRecord sdr[];
};

layout(std430, binding=1) buffer skullParamsPerElement{
		skullParams boneSpeeds[];
};

layout(std430, binding=2) buffer resultDistance{
		elemDistance d[];
};

// compute shader dispatch layout
layout (local_size_x = 256, local_size_y = 1, local_size_z = 1) in;

uniform int         elementCount;
uniform float       skullHUthreshold;
uniform int         minLutIndex;
uniform int         maxLutIndex;
uniform float       minSOSVal;
uniform float       maxSOSVal;
uniform sampler1D   sos_lut_tex;


float remap( float minval, float maxval, float curval )
{
    return clamp (( curval - minval ) / ( maxval - minval ), 0, 1);
}

void main()
{
    uint gid = gl_GlobalInvocationID.x;
    if (gid >= elementCount) return;

    float[] samples = sdr[gid].huSamples;

    int startIndex = int(floor(sdr[gid].maxVal1 + 0.5)) - 2;
    int endIndex = int(floor(sdr[gid].maxVal1 + 0.5)) + 2;

    if (startIndex<0 || endIndex<0) return;

    float sosSum = 0;
    for (int i=startIndex; i<=endIndex; i++) {
        float sos = texture(sos_lut_tex, (samples[i] + 1000)/4096).r;
        sosSum += sos * (maxSOSVal - minSOSVal) + minSOSVal;
    }

    sosSum /= (endIndex-startIndex+1);

    boneSpeeds[gid].corticalBoneSpeed = sosSum;
}