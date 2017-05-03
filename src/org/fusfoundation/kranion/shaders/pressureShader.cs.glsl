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

uniform float       boneSpeed;
uniform float       waterSpeed;

struct elemStart{
         vec4 pos;
         vec4 dir;
};

struct elemEnd{
         vec4 pos[6]; // calculated by the refraction path shader in previous step
};

struct elemDistance {
		float dist;
                float sdr;
                float incidentAngle;
                float skullThickness;
};

struct pressureRecord {
            float   pressure;
};


layout(std430, binding=0) buffer elements{
            elemStart e[];
};

layout(std430, binding=1) buffer elementRays{
            elemEnd r[];
};

layout(std430, binding=2) buffer elementRayDistance{
            elemDistance d[];
};

layout(std430, binding=3) buffer resultPressure{
            pressureRecord p[];
};

layout(std430, binding=4) buffer outputPhase{
		float outPhase[];
};


// compute shader dispatch layout
layout (local_size_x = 256, local_size_y = 1, local_size_z = 1) in;

#define M_PI 3.1415926535897932384626433832795

#define BONE_SPEED 3400000.0 // mm/s
#define WATER_SPEED 1482000.0 // mm/s
#define CENTER_FREQ 670000.0 // 220000

float remap( float minval, float maxval, float curval )
{
    return clamp (( curval - minval ) / ( maxval - minval ), 0, 1);
}

vec3 GetClosestPoint(vec3 A, vec3 B, vec3 P)
{
    vec3 AP = P - A;
    vec3 AB = B - A;
    float ab2 = AB.x*AB.x + AB.y*AB.y + AB.z*AB.z;
    float ap_ab = AP.x*AB.x + AP.y*AB.y + AP.z*AB.z;
    float t = ap_ab / ab2;

    t = clamp(t, 0, t);

    vec3 Closest = A + AB * t;
    return Closest;
}

uniform vec3        sample_point;

void main()
{
    uint gid = gl_GlobalInvocationID.x; // which transducer element we are

    vec3 rayStart = e[gid].pos.xyz;
    vec3 pos1 = r[gid].pos[2].xyz; // point on the outside of the skull
    vec3 pos2 = r[gid].pos[3].xyz; // point on the inside of the skull w/ refraction
    vec3 rayEnd = r[gid].pos[5].xyz; // closest point to focal spot

    float skullThickness = length(pos2 - pos1);
    float finalRaySegmentDistance = d[gid].dist; // -1 indicates ineffective element

    if (finalRaySegmentDistance >= 0) {

        float rayTimeFirstTwoSegments = distance(pos1, rayStart)/(waterSpeed*1000.0)
                        + skullThickness/(boneSpeed*1000.0);

        float rayTimeFirstTwoSegmentsWater = distance(pos1, rayStart)/(waterSpeed*1000.0)
                        + skullThickness/(waterSpeed*1000.0);

        float rayTime = rayTimeFirstTwoSegments
                        + distance(rayEnd, pos2)/(waterSpeed*1000.0);

        float rayTimeWaterOnly = rayTimeFirstTwoSegmentsWater
                        + distance(rayEnd, pos2)/(waterSpeed*1000.0);

//        float rayPhaseCorrection = cos(2 * M_PI * CENTER_FREQ * rayTime);

        vec3 finalRayIntercept = GetClosestPoint(pos2, rayEnd, sample_point);
        float offAxisDistance = distance(sample_point, finalRayIntercept);

        float sampleRayTime = rayTimeFirstTwoSegments
                        + distance(finalRayIntercept, pos2)/(waterSpeed*1000.0);

        float sampleRayTimeWaterOnly = rayTimeFirstTwoSegmentsWater
                        + distance(finalRayIntercept, pos2)/(waterSpeed*1000.0);


//rayPhaseCorrection = 0;

        //float fwhm = 1;
        //float mag = exp(-(offAxisDistance*offAxisDistance)/(2*fwhm*fwhm)); // gaussian
        float mag = /*abs*/(sin(offAxisDistance*M_PI*0.44)/(offAxisDistance*M_PI*0.44));
        //p[gid].pressure = mag * cos(2 * M_PI * CENTER_FREQ * sampleRayTime - acos(rayPhaseCorrection)); //(rayTime - sampleRayTime));
        p[gid].pressure = mag * cos(2 * M_PI * CENTER_FREQ * (sampleRayTime - rayTime));
        outPhase[gid] = 2.0 * M_PI * CENTER_FREQ * (/* bone componen t*/ (rayTime - rayTimeWaterOnly) + /* steering component */(rayTime - sampleRayTime));
    }
    else {
        p[gid].pressure = 0.0;
        outPhase[gid] = 0.0;
    }

//    outPhase[gid] = skullThickness;

}