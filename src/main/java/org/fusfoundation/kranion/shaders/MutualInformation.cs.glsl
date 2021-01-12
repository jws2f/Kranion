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


layout(std430, binding=0) buffer elements{
          vec4 samplePosition[];
};

layout(std430, binding=1) buffer histograms{
          int histogramValues[];
};

layout (local_size_x = 512, local_size_y = 1, local_size_z = 1) in;

uniform int         sampleCount;
uniform sampler3D   ct_tex;
uniform mat4        ct_tex_matrix;
uniform sampler3D   mr_tex;
uniform mat4        mr_tex_matrix;
uniform vec3 voxelSize; // in mm
uniform vec3 imageVolumeSize; // in voxels

#define binCount 50
#define jointSize 2500
shared int localHistogram[jointSize];

vec3 voxelScale = vec3(1);

const vec3 offset[27] = {
	vec3(-1, -1, -1),   	vec3(-1, -1, 0),   	vec3(-1, -1, 1),
	vec3(-1, 0, -1),   	vec3(-1, 0, 0),   	vec3(-1, 0, 1),
	vec3(-1, 1, -1),   	vec3(-1, 1, 0),   	vec3(-1, 1, 1),


	vec3(0, -1, -1),   	vec3(0, -1, 0),   	vec3(0, -1, 1),
	vec3(0, 0, -1),   	vec3(0, 0, 0),   	vec3(0, 0, 1),
	vec3(0, 1, -1),   	vec3(0, 1, 0),   	vec3(0, 1, 1),


	vec3(1, -1, -1),   	vec3(1, -1, 0),   	vec3(1, -1, 1),
	vec3(1, 0, -1),   	vec3(1, 0, 0),   	vec3(1, 0, 1),
	vec3(1, 1, -1),   	vec3(1, 1, 0),   	vec3(1, 1, 1)


};

const float gaussian1[27] = {
	0.0054548117,   	0.0061811116,   	0.0054548117,
	0.0061811116,   	0.007004117,   	0.0061811116,
	0.0054548117,   	0.0061811116,   	0.0054548117,


	0.0061811116,   	0.007004117,   	0.0061811116,
	0.007004117,   	0.007936705,   	0.007004117,
	0.0061811116,   	0.007004117,   	0.0061811116,


	0.0054548117,   	0.0061811116,   	0.0054548117,
	0.0061811116,   	0.007004117,   	0.0061811116,
	0.0054548117,   	0.0061811116,   	0.0054548117


};

float kernelSum = 0.16777325;

float filtered_sample(sampler3D s, vec3 p) {
     float value = 0.0;

     for (int i=0; i<27; i++) {
        value += gaussian1[i] * texture(s, p + voxelScale*offset[i]).r;
     }

     return value / kernelSum;
}

void main() {

         uint gid = gl_GlobalInvocationID.x;
         if (gid >= sampleCount) return;

        if (gl_LocalInvocationID.x == 0) {
            for (int i=0; i<jointSize; i++) {
                localHistogram[i] = 0;
            }
        }

        memoryBarrierShared();
        barrier();

         voxelScale = vec3(0.5/(imageVolumeSize.x*voxelSize.x/2.0), 0.5/(imageVolumeSize.y*voxelSize.y/2.0), 0.5/(imageVolumeSize.z*voxelSize.z/2.0)) * 2.0; // we can spread samples out becaouse of the gaussian blurring

         vec4 pos = samplePosition[gid];

         vec3 coord = (ct_tex_matrix * pos).xyz;
         //float ctValue = textureLod(ct_tex,coord, 3.0).r * 65535.0;
         float ctValue = filtered_sample(ct_tex,coord).r * 65535.0;
         //float ctValue = texture(ct_tex,coord).r * 65535.0;

         coord = (mr_tex_matrix * pos).xyz;
         //float mrValue = textureLod(mr_tex,coord, 3.0).r * 65535.0;
         float mrValue = filtered_sample(mr_tex,coord).r * 65535.0; // texture() for no filter
         //float mrValue = texture(mr_tex,coord).r * 65535.0;

         int ctIndex = int(min(binCount-1, round(ctValue/81.9))); // 4095/50
         int mrIndex = int(min(binCount-1, round(mrValue/81.9))); // 4095/50

         // Compute the joint histogram
         int updateIndex = ctIndex + binCount * mrIndex;

         atomicAdd(localHistogram[updateIndex], 1);

        memoryBarrierShared();
        barrier();

        if (gl_LocalInvocationID.x == 0) {
            for (int i=0; i<jointSize; i++) {
                atomicAdd(histogramValues[i], localHistogram[i]);
            }
        }

        //memoryBarrier();
        //barrier();

        // Compute the marginal histograms
        //if (gid<binCount) {
        //    for (int i=0; i<binCount; i++) {
        //        histogramValues[jointSize+gid]      += histogramValues[gid * binCount +   i]; // sum rows
        //        histogramValues[jointSize+gid+50]   += histogramValues[  i * binCount + gid]; // sum cols
        //   }
        //}
}