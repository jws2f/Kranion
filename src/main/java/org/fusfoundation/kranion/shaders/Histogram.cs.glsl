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

//uniform float width;
//uniform float height;
//uniform float depth;
//uniform sampler3D image_tex;

layout(binding=0, r16) uniform readonly image3D myImage3D;

layout(std430, binding=1) buffer histogram {
          int h[];
};

// 16 x 8 x 8 work group (1024 total threads per workgroup)
layout (local_size_x = 8, local_size_y = 8, local_size_z = 8) in;

void main() {

    ivec3 size = imageSize(myImage3D);

    // If we are outside the texture volume then bail
    if (gl_GlobalInvocationID.x >= size.x ||
        gl_GlobalInvocationID.y >= size.y ||
        gl_GlobalInvocationID.z >= size.z)
    {
        return;
    }

    // compute texel center coordinate for this thread
//    vec3 imgCoord = vec3((float(gl_GlobalInvocationID.x) + 0.5)/(width),
//                         (float(gl_GlobalInvocationID.y) + 0.5)/(height),
//                         (float(gl_GlobalInvocationID.z) + 0.5)/(depth));
    ivec3 imgCoord = ivec3(gl_GlobalInvocationID.x,
                         gl_GlobalInvocationID.y,
                         gl_GlobalInvocationID.z);

    // Retrieve the voxel value for this thread (one thread per voxel)
    float value = imageLoad(myImage3D, imgCoord).r;
    //float value = texture(image_tex, imgCoord).r;

    // We want the lower 12 bits worth pixel values (0 - 4095)
    // texture data is signed shorts so positive range
    // is 0 - 32767, although texture() returns normalized values in the
    // range of 0.0 - 1.0
    //int v = int(clamp(value * 32767.0, 0.0, 4095.0));
    int v = int(clamp(value * 65535.0, 0.0, 4095.0));

    // Add this threads voxel value vote to histogram
    // atomic add keeps other threads from clobbering each other
    atomicAdd(h[v], 1);

}
