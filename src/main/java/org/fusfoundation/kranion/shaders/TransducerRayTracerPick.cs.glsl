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


struct vertex{
         vec4 pos;
         vec4 norm;
         vec4 col;
};

struct unlitVertex{
         vec4 pos;
         vec4 col;
};

struct elemColor {
         vec4 color[6];
};

struct skullParams {
    float corticalBoneSpeed;
    float averageBoneSpeed;
};

layout(std430, binding=0) buffer resultColor{
          elemColor c[];
};

layout(std430, binding=1) buffer outputDiscs{
		vertex outDiscTris[];
};

layout(std430, binding=2) buffer outputRays{
		unlitVertex outRays[];
};


uniform int         elementCount;

layout (local_size_x = 256, local_size_y = 1, local_size_z = 1) in;

// We are just recoloring the rays and discs with a coded representation of element number.
// No change in geometry here. Depends on previous pass of raytracing to set geometry.
////////////////
void main()
{
        uint gid = gl_GlobalInvocationID.x;
        if (gid >= elementCount) return;

        vec4 outColor = vec4(0);
       
        uint id = gid + 5; // offset for now to make room for CT=1, MR=2, Frame=3; TODO: add better management of id's

        uint red = id & 0xff;
        uint green = (id >> 8) & 0xff;
        uint blue = (id >> 16) & 0xff;

        ivec3 code = ivec3(red, green, blue);

        outColor = vec4(code.x * 1.0/255.0, code.y * 1.0/255.0, code.z * 1.0/255.0, 1);

        for (int i=0; i<6; i++) {
            c[gid].color[i].xyz = outColor.xyz;
            c[gid].color[i].w = outColor.w;
        }

        outRays[gid*2].col.xyz = outColor.xyz;
        outRays[gid*2].col.w =outColor.w;

        outRays[gid*2+1].col.xyz = outColor.xyz;
        outRays[gid*2+1].col.w = outColor.w;

        uint index1 = gid*20*3;
        for (int i=0; i<20; i++) {
            uint startIndex = i*3 + index1;


            outDiscTris[startIndex].col.xyz = outColor.xyz;
            outDiscTris[startIndex].col.a = outColor.w;


            outDiscTris[startIndex+1].col.xyz = outColor.xyz;
            outDiscTris[startIndex+1].col.a = outColor.w;


            outDiscTris[startIndex+2].col.xyz = outColor.xyz;
            outDiscTris[startIndex+2].col.a = outColor.w;
        }
}