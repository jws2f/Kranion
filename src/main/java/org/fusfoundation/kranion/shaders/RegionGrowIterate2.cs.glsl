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

layout(binding=0, r16) uniform readonly image3D myImage3D;
layout(binding=1, r8) uniform image3D myMask3D;

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

    ivec3 imgCoord = ivec3(gl_GlobalInvocationID.x,
                         gl_GlobalInvocationID.y,
                         gl_GlobalInvocationID.z);

    // Retrieve the voxel value for this thread (one thread per voxel)
    int m = int(imageLoad(myMask3D, imgCoord).r * 255.0);


    if (m!=0) {        
            const float newValue = 1.0 / 255.0;
            imageStore(myMask3D, imgCoord, vec4(newValue));
    }

}