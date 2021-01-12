/* 
 * The MIT License
 *
 * Copyright 2019 Focused Ultrasound Foundation.
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

layout(location=1)  uniform int         seedCount;

layout(location=2)  uniform vec3        voxelSize; // in mm
layout(location=3)  uniform vec3        voxelDim; // in voxels

layout(location=4)  uniform sampler1D   odfVerts; // direction lut

layout(location=5)  uniform sampler3D   fdIndex[8]; // fiber direction index volumes, lookup through odfVerts
layout(location=13) uniform sampler3D   faValue[8]; // fa value volumes

layout(location=21) uniform int         odfVertsSize;

layout(std430, binding=0) buffer seeds{
    vec4 seed_location[];
};

layout(std430, binding=1) buffer fiberPoints{
    vec4 fiber_points[];
};

layout(std430, binding=3) buffer fiberColors{
    vec4 fiber_points_color[];
};

layout(std430, binding=2) buffer fiberPointCount{
    int fiber_points_count[];
};

// 256 total threads per workgroup, 1D layout
layout (local_size_x = 64) in;

ivec3   size;
float    texelSizeX;
float    texelSizeY;
float    texelSizeZ;

float dirAngle(vec3 dir1, vec3 dir2) {
    float angle =  (acos(dot(dir1, dir2))); // abs() to pick smallest angle when fibers running in opposite directions
    if (angle > 3.14159/2.0) {
        angle = 3.14159 - angle;
    }
    return angle;
}

// no fiber turning angle filtering, choose highest FA fiber
vec3 getFiberDir(vec3 texCoord, float faThresh) {
    float maxFaVal = faThresh;
    vec3 returnDir = vec3(0);

    for (int i=0; i<8; i++) {
        float faVal = texture(faValue[i], texCoord).r;
        if (faVal > maxFaVal) {
            maxFaVal = faVal;
            returnDir =  texture(odfVerts, (2*(texture(fdIndex[i], texCoord).r) + 1)/(odfVertsSize * 2)).xyz;
        }
    }
    return returnDir;
}

// with fiber turning angle filtering
vec3 getFiberDir2(vec3 texCoord, float faThresh, vec3 currentDir) {
    float minAngle = 1.05; // turning angle < 60 deg
    vec3 returnDir = vec3(0);

    for (int i=0; i<8; i++) {
        float faVal = texture(faValue[i], texCoord).r;
        vec3 dir = texture(odfVerts, (2*(texture(fdIndex[i], texCoord).r) + 1)/(odfVertsSize * 2)).xyz;
        float angle = dirAngle(currentDir, dir);
        if (faVal > faThresh && angle < minAngle) { 
            minAngle = angle;
            returnDir = dir;
        }
    }

    return dot(returnDir, currentDir) > 0.0 ? returnDir : -returnDir;
}

// triliniear filtered fiber direction WITHOUT turning angle filter
vec3 getFilteredFiberDir(vec3 texCoord, float faThresh) {

    vec3 r = vec3( fract(texCoord.x * texelSizeX),
                    fract(texCoord.y * texelSizeY),
                    fract(texCoord.z * texelSizeZ));
    vec3 m = vec3(1-r.x, 1-r.y, 1-r.z);

    vec3 retval = getFiberDir(texCoord,  faThresh) * m.x * m.y * m.z;
    retval += getFiberDir(texCoord + vec3(texelSizeX, 0,            0),             faThresh) * r.x * m.y * m.z;
    retval += getFiberDir(texCoord + vec3(0,            texelSizeY, 0),             faThresh) * m.x * r.y * m.z;
    retval += getFiberDir(texCoord + vec3(texelSizeX,   texelSizeY, 0),             faThresh) * r.x * r.y * m.z;
    retval += getFiberDir(texCoord + vec3(0,            0,          texelSizeZ),    faThresh) * m.x * m.y * r.z;
    retval += getFiberDir(texCoord + vec3(texelSizeX,   0,          texelSizeZ),    faThresh) * r.x * m.y * m.z;
    retval += getFiberDir(texCoord + vec3(0,            texelSizeY, texelSizeZ),    faThresh) * m.x * r.y * r.z;
    retval += getFiberDir(texCoord + vec3(texelSizeX,   texelSizeY, texelSizeZ),    faThresh) * r.x * r.y * r.z;

    return normalize(retval);
}

// triliniear filtered fiber direction WITH turning angle filter
vec3 getFilteredFiberDir2(vec3 texCoord, float faThresh, vec3 currentDir) {

    vec3 r = vec3( fract(texCoord.x * texelSizeX),
                    fract(texCoord.y * texelSizeY),
                    fract(texCoord.z * texelSizeZ));
    vec3 m = vec3(1-r.x, 1-r.y, 1-r.z);

    float w = 0;
    vec3 retval = vec3(0);
    vec3 val;

    val = getFiberDir2(texCoord,  faThresh, currentDir) * m.x * m.y * m.z;
    if (dot(val,val)>0) {
        w += m.x * m.y * m.z;
        retval += val;
    }
    val = getFiberDir2(texCoord + vec3(texelSizeX, 0,            0),             faThresh, currentDir) * r.x * m.y * m.z;
    if (dot(val,val)>0) {
        w += r.x * m.y * m.z;
        retval += val;
    }
    val = getFiberDir2(texCoord + vec3(0,            texelSizeY, 0),             faThresh, currentDir) * m.x * r.y * m.z;
    if (dot(val,val)>0) {
        w += m.x * r.y * m.z;
        retval += val;
    }
    val = getFiberDir2(texCoord + vec3(texelSizeX,   texelSizeY, 0),             faThresh, currentDir) * r.x * r.y * m.z;
    if (dot(val,val)>0) {
        w += r.x * r.y * m.z;
        retval += val;
    }
    val = getFiberDir2(texCoord + vec3(0,            0,          texelSizeZ),    faThresh, currentDir) * m.x * m.y * r.z;
    if (dot(val,val)>0) {
        w += m.x * m.y * r.z;
        retval += val;
    }
    val = getFiberDir2(texCoord + vec3(texelSizeX,   0,          texelSizeZ),    faThresh, currentDir) * r.x * m.y * m.z;
    if (dot(val,val)>0) {
        w += r.x * m.y * m.z;
        retval += val;
    }
    val = getFiberDir2(texCoord + vec3(0,            texelSizeY, texelSizeZ),    faThresh, currentDir) * m.x * r.y * r.z;
    if (dot(val,val)>0) {
        w += m.x * r.y * r.z;
        retval += val;
    }
    val = getFiberDir2(texCoord + vec3(texelSizeX,   texelSizeY, texelSizeZ),    faThresh, currentDir) * r.x * r.y * r.z;
    if (dot(val,val)>0) {
        w += r.x * r.y * r.z;
        retval += val;
    }

    retval = w>0.0 ? normalize(retval) : vec3(0);

    return retval;
}

float getFAvalue(vec3 texLoc) {
    float maxFAvalue = 0;
    for (int i=0; i<8; i++) {
        float faVal = texture(faValue[0], texLoc).r;
        if (faVal > maxFAvalue) {
            maxFAvalue = faVal;
        }
    }
    return maxFAvalue;
}


void main() {

    uint id = gl_GlobalInvocationID.x;

    int fiberPointsMax = 512;

    uint fiberForwardBase = id*2*512;
    uint fiberBackwardBase = (id*2+1)*512;

    float step_size = 0.5;
    float fa_threshold = 0.0625;

    size = textureSize(fdIndex[0], 0);
    texelSizeX = 1.0 / size.x; //size of one texel 
    texelSizeY = 1.0 / size.y; //size of one texel 
    texelSizeZ = 1.0 / size.z; //size of one texel 

    // If we are outside the seedCount max then bail
    if (gl_GlobalInvocationID.x >= seedCount)
    {
        return;
    }

    vec4 seed = seed_location[id];
    vec3 currentLoc = seed.xyz;
    vec3 texLoc = vec3( seed.x/voxelSize.x/voxelDim.x,
                        seed.y/voxelSize.y/voxelDim.y,
                        seed.z/voxelSize.z/voxelDim.z);

    vec4 volumeCenter = vec4(voxelSize.x * voxelDim.x/2, voxelSize.y * voxelDim.y/2, voxelSize.z * voxelDim.z/2, 0);

    float faVal = getFAvalue(texLoc);
    vec3 fiberDir = getFilteredFiberDir(texLoc, fa_threshold);
    fiber_points_count[fiberForwardBase] = 0;
    fiber_points_count[fiberBackwardBase] = 0;

    // do some work
    int counter=0;
    for (int i=0; i<fiberPointsMax/2; i++) {
        if (faVal > fa_threshold && !(fiberDir.x == 0 && fiberDir.y == 0 && fiberDir.z == 0)) {
            fiber_points[fiberForwardBase + counter] = vec4(currentLoc, 1) - volumeCenter;
            fiber_points_color[fiberForwardBase + counter] = vec4(abs(fiberDir.x), abs(fiberDir.y), abs(fiberDir.z), 0.9);
            counter++;

            currentLoc += fiberDir * step_size;
            texLoc = vec3( currentLoc.x/voxelSize.x/voxelDim.x,
                        currentLoc.y/voxelSize.y/voxelDim.y,
                        currentLoc.z/voxelSize.z/voxelDim.z);
            faVal = getFAvalue(texLoc);
            fiberDir = getFilteredFiberDir2(texLoc, fa_threshold, fiberDir);

        }
        else {
            fiber_points_count[id*2] = counter;
            break;
        }
    }

    currentLoc = seed.xyz;
    texLoc = vec3( seed.x/voxelSize.x/voxelDim.x,
                        seed.y/voxelSize.y/voxelDim.y,
                        seed.z/voxelSize.z/voxelDim.z);

    faVal = getFAvalue(texLoc);
    fiberDir = getFilteredFiberDir(texLoc, fa_threshold);

    int counter2=0;
    for (int i=0; i<fiberPointsMax/2; i++) {
        if (faVal > fa_threshold && !(fiberDir.x == 0 && fiberDir.y == 0 && fiberDir.z == 0)) {
            fiber_points[fiberBackwardBase + counter2] = vec4(currentLoc, 1) - volumeCenter;
            fiber_points_color[fiberBackwardBase + counter2] = vec4(abs(fiberDir.x), abs(fiberDir.y), abs(fiberDir.z), 0.9);
            counter2++;

            currentLoc += fiberDir * -step_size;
            texLoc = vec3( currentLoc.x/voxelSize.x/voxelDim.x,
                        currentLoc.y/voxelSize.y/voxelDim.y,
                        currentLoc.z/voxelSize.z/voxelDim.z);
            faVal = getFAvalue(texLoc);
            fiberDir = getFilteredFiberDir2(texLoc, fa_threshold, fiberDir);

        }
        else {
            fiber_points_count[id*2+1] = counter2;
            break;
        }
    }

    if (counter + counter2 < 180) {
            fiber_points_count[id*2] = 0;
            fiber_points_count[id*2+1] = 0;        
    }

}