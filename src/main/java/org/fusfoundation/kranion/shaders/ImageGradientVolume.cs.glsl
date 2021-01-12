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

uniform float width;
uniform float height;
uniform float depth;
uniform vec3 voxelSize; // in mm

uniform sampler3D image_tex;

layout(binding=1, rgba16f) uniform writeonly image3D myImage3D;


// 8 x 8 x 8 work group (512 total threads per workgroup)
layout (local_size_x = 8, local_size_y = 8, local_size_z = 8) in;

// 3x3x3 Sobel mask offsets and weights
const vec3 offset[27] = {
    vec3(-1, -1, -1),   vec3(-1, 0, -1),    vec3(-1, 1, -1),
    vec3(-1, -1, 0),    vec3(-1, 0, 0),     vec3(-1, 1, 0),
    vec3(-1, -1, 1),    vec3(-1, 0, 1),     vec3(-1, 1, 1),

    vec3(0, -1, -1),   vec3(0, 0, -1),    vec3(0, 1, -1),
    vec3(0, -1, 0),    vec3(0, 0, 0),     vec3(0, 1, 0),
    vec3(0, -1, 1),    vec3(0, 0, 1),     vec3(0, 1, 1),

    vec3(1, -1, -1),   vec3(1, 0, -1),    vec3(1, 1, -1),
    vec3(1, -1, 0),    vec3(1, 0, 0),     vec3(1, 1, 0),
    vec3(1, -1, 1),    vec3(1, 0, 1),     vec3(1, 1, 1),
};

const float sqr3 = sqrt(3.0)/3;
const float sqr2 = sqrt(2.0)/2;

const float xGrad[27] = {
    -1, -3, -1,
    -3, -6, -3,
    -1, -3, -1,

    0, 0, 0,
    0, 0, 0,
    0, 0, 0,

    1, 3, 1,
    3, 6, 3,
    1, 3, 1
};

const float xGradZH[27] = {
    -sqr3, -sqr2, -sqr3,
    -sqr2, -1, -sqr2,
    -sqr3, -sqr2, -sqr3,

    0, 0, 0,
    0, 0, 0,
    0, 0, 0,

    sqr3, sqr2, sqr3,
    sqr2, 1, sqr2,
    sqr3, sqr2, sqr3
};

const float yGrad[27] = {
    -1, 0, 1,
    -3, 0, 3,
    -1, 0, 1,

    -3, 0, 3,
    -6, 0, 6,
    -3, 0, 3,

    -1, 0, 1,
    -3, 0, 3,
    -1, 0, 1
};

// 3x3x3 Zucker-Hummel weights
const float yGradZH[27] = {
    -sqr3, 0, sqr3,
    -sqr2, 0, sqr2,
    -sqr3, 0, sqr3,

    -sqr2, 0, sqr2,
    -1, 0, 1,
    -sqr2, 0, sqr2,

    -sqr3, 0, sqr3,
    -sqr2, 0, sqr2,
    -sqr3, 0, sqr3
};

const float zGrad[27] = {
    -1, -3, -1,
    0, 0, 0,
    1, 3, 1,

    -3, -6, -3,
    0, 0, 0,
    3, 6, 3,

    -1, -3, -1,
    0, 0, 0,
    1, 3, 1
};

const float zGradZH[27] = {
    -sqr3, -sqr2, -sqr3,
    0, 0, 0,
    sqr3, sqr2, sqr3,

    -sqr2, -1, -sqr2,
    0, 0, 0,
    sqr2, 1, sqr2,

    -sqr3, -sqr2, -sqr3,
    0, 0, 0,
    sqr3, sqr2, sqr3
};

//vec3 gradient_delta = vec3(0.003, 0.003, 0.00375);
//vec3 gradient_delta = vec3(0.0045, 0.0045, 0.00689);
vec3 gradient_delta = vec3(1);

vec3 findNormal(vec3 position)
{
	vec3 grad = vec3(0);
	for (int i = 0; i<27; i++)
	{
		vec3 coord = position + (offset[i] * gradient_delta);
		float s = texture(image_tex, coord).r * 65535.0;
		grad.x += s * xGradZH[i];
		grad.y += s * yGradZH[i];
		grad.z += s * zGradZH[i];
	}
	return grad;
}

void main() {

    ivec3 size = imageSize(myImage3D);

    // If we are outside the texture volume then bail
    if (gl_GlobalInvocationID.x >= size.x ||
        gl_GlobalInvocationID.y >= size.y ||
        gl_GlobalInvocationID.z >= size.z)
    {
        return;
    }

    gradient_delta = vec3(0.5/(width*voxelSize.x), 0.5/(height*voxelSize.y), 0.5/(depth*voxelSize.z));

    // compute texel center coordinate for this thread
    vec3 srcCoord = vec3((float(gl_GlobalInvocationID.x) + 0.5)/(width),
                         (float(gl_GlobalInvocationID.y) + 0.5)/(height),
                         (float(gl_GlobalInvocationID.z) + 0.5)/(depth));

    ivec3 dstCoord = ivec3(gl_GlobalInvocationID.x,
                         gl_GlobalInvocationID.y,
                         gl_GlobalInvocationID.z);

    vec4 value;
    vec3 diff;

    diff = -findNormal(srcCoord);

    value.xyz = normalize(diff);
    value.w = length(diff);

    // Store the voxel gradient value for this thread (one thread per voxel)
    imageStore(myImage3D, dstCoord, value);

}
