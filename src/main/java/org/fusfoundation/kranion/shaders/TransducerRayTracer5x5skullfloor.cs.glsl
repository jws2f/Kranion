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

struct elemStart{
         vec4 pos;
         vec4 dir;
};

struct vertex{
         vec4 pos;
         vec4 norm;
         vec4 col;
};

struct unlitVertex{
         vec4 pos;
         vec4 col;
};

struct elemEnd{
         vec4 pos[6];
//         vec4 pos2;
};

struct elemColor {
         vec4 color[6];
//         vec4 color2;
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

layout(std430, binding=0) buffer elements{
          elemStart e[];
};

layout(std430, binding=1) buffer result{
          elemEnd o[];
};

layout(std430, binding=2) buffer resultColor{
          elemColor c[];
};

layout(std430, binding=3) buffer resultDistance{
		elemDistance d[];
};

layout(std430, binding=4) buffer outputDiscs{
		vertex outDiscTris[];
};

layout(std430, binding=5) buffer outputRays{
		unlitVertex outRays[];
};

layout(std430, binding=6) buffer outputPhase{
		float outPhase[];
};

layout(std430, binding=7) buffer skullFloorRays{
		unlitVertex sfRays[];
};

layout(std430, binding=8) buffer skullFloorDiscs{
		vertex sfDiscTris[];
};

uniform vec3        target; // no steering = vec3(0,0,0);
uniform sampler3D   ct_tex;
uniform mat4        ct_tex_matrix;
uniform float       boneSpeed;
uniform float       waterSpeed;
uniform float       ct_bone_threshold;
uniform float       ct_rescale_intercept;
uniform float       ct_rescale_slope;
uniform int         selectedElement;
uniform int         elementCount;


const float sqr3 = sqrt(3.0)/3;
const float sqr2 = sqrt(2.0)/2;

const vec3 offset[125] = {
	vec3(-2, -2, -2),   	vec3(-2, -1, -2),   	vec3(-2, 0, -2),   	vec3(-2, 1, -2),   	vec3(-2, 2, -2),
	vec3(-2, -2, -1),   	vec3(-2, -1, -1),   	vec3(-2, 0, -1),   	vec3(-2, 1, -1),   	vec3(-2, 2, -1),
	vec3(-2, -2, 0),   	vec3(-2, -1, 0),   	vec3(-2, 0, 0),   	vec3(-2, 1, 0),   	vec3(-2, 2, 0),
	vec3(-2, -2, 1),   	vec3(-2, -1, 1),   	vec3(-2, 0, 1),   	vec3(-2, 1, 1),   	vec3(-2, 2, 1),
	vec3(-2, -2, 2),   	vec3(-2, -1, 2),   	vec3(-2, 0, 2),   	vec3(-2, 1, 2),   	vec3(-2, 2, 2),

	vec3(-1, -2, -2),   	vec3(-1, -1, -2),   	vec3(-1, 0, -2),   	vec3(-1, 1, -2),   	vec3(-1, 2, -2),
	vec3(-1, -2, -1),   	vec3(-1, -1, -1),   	vec3(-1, 0, -1),   	vec3(-1, 1, -1),   	vec3(-1, 2, -1),
	vec3(-1, -2, 0),   	vec3(-1, -1, 0),   	vec3(-1, 0, 0),   	vec3(-1, 1, 0),   	vec3(-1, 2, 0),
	vec3(-1, -2, 1),   	vec3(-1, -1, 1),   	vec3(-1, 0, 1),   	vec3(-1, 1, 1),   	vec3(-1, 2, 1),
	vec3(-1, -2, 2),   	vec3(-1, -1, 2),   	vec3(-1, 0, 2),   	vec3(-1, 1, 2),   	vec3(-1, 2, 2),

	vec3(0, -2, -2),   	vec3(0, -1, -2),   	vec3(0, 0, -2),   	vec3(0, 1, -2),   	vec3(0, 2, -2),
	vec3(0, -2, -1),   	vec3(0, -1, -1),   	vec3(0, 0, -1),   	vec3(0, 1, -1),   	vec3(0, 2, -1),
	vec3(0, -2, 0),   	vec3(0, -1, 0),   	vec3(0, 0, 0),   	vec3(0, 1, 0),   	vec3(0, 2, 0),
	vec3(0, -2, 1),   	vec3(0, -1, 1),   	vec3(0, 0, 1),   	vec3(0, 1, 1),   	vec3(0, 2, 1),
	vec3(0, -2, 2),   	vec3(0, -1, 2),   	vec3(0, 0, 2),   	vec3(0, 1, 2),   	vec3(0, 2, 2),

	vec3(1, -2, -2),   	vec3(1, -1, -2),   	vec3(1, 0, -2),   	vec3(1, 1, -2),   	vec3(1, 2, -2),
	vec3(1, -2, -1),   	vec3(1, -1, -1),   	vec3(1, 0, -1),   	vec3(1, 1, -1),   	vec3(1, 2, -1),
	vec3(1, -2, 0),   	vec3(1, -1, 0),   	vec3(1, 0, 0),   	vec3(1, 1, 0),   	vec3(1, 2, 0),
	vec3(1, -2, 1),   	vec3(1, -1, 1),   	vec3(1, 0, 1),   	vec3(1, 1, 1),   	vec3(1, 2, 1),
	vec3(1, -2, 2),   	vec3(1, -1, 2),   	vec3(1, 0, 2),   	vec3(1, 1, 2),   	vec3(1, 2, 2),

	vec3(2, -2, -2),   	vec3(2, -1, -2),   	vec3(2, 0, -2),   	vec3(2, 1, -2),   	vec3(2, 2, -2),
	vec3(2, -2, -1),   	vec3(2, -1, -1),   	vec3(2, 0, -1),   	vec3(2, 1, -1),   	vec3(2, 2, -1),
	vec3(2, -2, 0),   	vec3(2, -1, 0),   	vec3(2, 0, 0),   	vec3(2, 1, 0),   	vec3(2, 2, 0),
	vec3(2, -2, 1),   	vec3(2, -1, 1),   	vec3(2, 0, 1),   	vec3(2, 1, 1),   	vec3(2, 2, 1),
	vec3(2, -2, 2),   	vec3(2, -1, 2),   	vec3(2, 0, 2),   	vec3(2, 1, 2),   	vec3(2, 2, 2)

};

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

const float xGradZH[125] = {
	-0.57735026,   	-0.6666667,   	-0.70710677,   	-0.6666667,   	-0.57735026,
	-0.6666667,   	-0.81649655,   	-0.8944272,   	-0.81649655,   	-0.6666667,
	-0.70710677,   	-0.8944272,   	-1.0,   	-0.8944272,   	-0.70710677,
	-0.6666667,   	-0.81649655,   	-0.8944272,   	-0.81649655,   	-0.6666667,
	-0.57735026,   	-0.6666667,   	-0.70710677,   	-0.6666667,   	-0.57735026,

	-0.33333334,   	-0.40824828,   	-0.4472136,   	-0.40824828,   	-0.33333334,
	-0.40824828,   	-0.57735026,   	-0.70710677,   	-0.57735026,   	-0.40824828,
	-0.4472136,   	-0.70710677,   	-1.0,   	-0.70710677,   	-0.4472136,
	-0.40824828,   	-0.57735026,   	-0.70710677,   	-0.57735026,   	-0.40824828,
	-0.33333334,   	-0.40824828,   	-0.4472136,   	-0.40824828,   	-0.33333334,

	0.0,   	0.0,   	0.0,   	0.0,   	0.0,
	0.0,   	0.0,   	0.0,   	0.0,   	0.0,
	0.0,   	0.0,   	0.0,   	0.0,   	0.0,
	0.0,   	0.0,   	0.0,   	0.0,   	0.0,
	0.0,   	0.0,   	0.0,   	0.0,   	0.0,

	0.33333334,   	0.40824828,   	0.4472136,   	0.40824828,   	0.33333334,
	0.40824828,   	0.57735026,   	0.70710677,   	0.57735026,   	0.40824828,
	0.4472136,   	0.70710677,   	1.0,   	0.70710677,   	0.4472136,
	0.40824828,   	0.57735026,   	0.70710677,   	0.57735026,   	0.40824828,
	0.33333334,   	0.40824828,   	0.4472136,   	0.40824828,   	0.33333334,

	0.57735026,   	0.6666667,   	0.70710677,   	0.6666667,   	0.57735026,
	0.6666667,   	0.81649655,   	0.8944272,   	0.81649655,   	0.6666667,
	0.70710677,   	0.8944272,   	1.0,   	0.8944272,   	0.70710677,
	0.6666667,   	0.81649655,   	0.8944272,   	0.81649655,   	0.6666667,
	0.57735026,   	0.6666667,   	0.70710677,   	0.6666667,   	0.57735026

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

const float yGradZH[125] = {
	-0.57735026,   	-0.33333334,   	0.0,   	0.33333334,   	0.57735026,
	-0.6666667,   	-0.40824828,   	0.0,   	0.40824828,   	0.6666667,
	-0.70710677,   	-0.4472136,   	0.0,   	0.4472136,   	0.70710677,
	-0.6666667,   	-0.40824828,   	0.0,   	0.40824828,   	0.6666667,
	-0.57735026,   	-0.33333334,   	0.0,   	0.33333334,   	0.57735026,

	-0.6666667,   	-0.40824828,   	0.0,   	0.40824828,   	0.6666667,
	-0.81649655,   	-0.57735026,   	0.0,   	0.57735026,   	0.81649655,
	-0.8944272,   	-0.70710677,   	0.0,   	0.70710677,   	0.8944272,
	-0.81649655,   	-0.57735026,   	0.0,   	0.57735026,   	0.81649655,
	-0.6666667,   	-0.40824828,   	0.0,   	0.40824828,   	0.6666667,

	-0.70710677,   	-0.4472136,   	0.0,   	0.4472136,   	0.70710677,
	-0.8944272,   	-0.70710677,   	0.0,   	0.70710677,   	0.8944272,
	-1.0,   	-1.0,   	0.0,   	1.0,   	1.0,
	-0.8944272,   	-0.70710677,   	0.0,   	0.70710677,   	0.8944272,
	-0.70710677,   	-0.4472136,   	0.0,   	0.4472136,   	0.70710677,

	-0.6666667,   	-0.40824828,   	0.0,   	0.40824828,   	0.6666667,
	-0.81649655,   	-0.57735026,   	0.0,   	0.57735026,   	0.81649655,
	-0.8944272,   	-0.70710677,   	0.0,   	0.70710677,   	0.8944272,
	-0.81649655,   	-0.57735026,   	0.0,   	0.57735026,   	0.81649655,
	-0.6666667,   	-0.40824828,   	0.0,   	0.40824828,   	0.6666667,

	-0.57735026,   	-0.33333334,   	0.0,   	0.33333334,   	0.57735026,
	-0.6666667,   	-0.40824828,   	0.0,   	0.40824828,   	0.6666667,
	-0.70710677,   	-0.4472136,   	0.0,   	0.4472136,   	0.70710677,
	-0.6666667,   	-0.40824828,   	0.0,   	0.40824828,   	0.6666667,
	-0.57735026,   	-0.33333334,   	0.0,   	0.33333334,   	0.57735026

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

const float zGradZH[125] = {
	-0.57735026,   	-0.6666667,   	-0.70710677,   	-0.6666667,   	-0.57735026,
	-0.33333334,   	-0.40824828,   	-0.4472136,   	-0.40824828,   	-0.33333334,
	0.0,   	0.0,   	0.0,   	0.0,   	0.0,
	0.33333334,   	0.40824828,   	0.4472136,   	0.40824828,   	0.33333334,
	0.57735026,   	0.6666667,   	0.70710677,   	0.6666667,   	0.57735026,

	-0.6666667,   	-0.81649655,   	-0.8944272,   	-0.81649655,   	-0.6666667,
	-0.40824828,   	-0.57735026,   	-0.70710677,   	-0.57735026,   	-0.40824828,
	0.0,   	0.0,   	0.0,   	0.0,   	0.0,
	0.40824828,   	0.57735026,   	0.70710677,   	0.57735026,   	0.40824828,
	0.6666667,   	0.81649655,   	0.8944272,   	0.81649655,   	0.6666667,

	-0.70710677,   	-0.8944272,   	-1.0,   	-0.8944272,   	-0.70710677,
	-0.4472136,   	-0.70710677,   	-1.0,   	-0.70710677,   	-0.4472136,
	0.0,   	0.0,   	0.0,   	0.0,   	0.0,
	0.4472136,   	0.70710677,   	1.0,   	0.70710677,   	0.4472136,
	0.70710677,   	0.8944272,   	1.0,   	0.8944272,   	0.70710677,

	-0.6666667,   	-0.81649655,   	-0.8944272,   	-0.81649655,   	-0.6666667,
	-0.40824828,   	-0.57735026,   	-0.70710677,   	-0.57735026,   	-0.40824828,
	0.0,   	0.0,   	0.0,   	0.0,   	0.0,
	0.40824828,   	0.57735026,   	0.70710677,   	0.57735026,   	0.40824828,
	0.6666667,   	0.81649655,   	0.8944272,   	0.81649655,   	0.6666667,

	-0.57735026,   	-0.6666667,   	-0.70710677,   	-0.6666667,   	-0.57735026,
	-0.33333334,   	-0.40824828,   	-0.4472136,   	-0.40824828,   	-0.33333334,
	0.0,   	0.0,   	0.0,   	0.0,   	0.0,
	0.33333334,   	0.40824828,   	0.4472136,   	0.40824828,   	0.33333334,
	0.57735026,   	0.6666667,   	0.70710677,   	0.6666667,   	0.57735026

};

#define M_PI 3.1415926535897932384626433832795
#define BONE_SPEED 3900.0
#define WATER_SPEED 1482.0

vec3 vectorRotate(vec3 vec, vec3 axis, float angle)
{
	float u = axis.x;
	float v = axis.y;
	float w = axis.z;
	float x = vec.x;
	float y = vec.y;
	float z = vec.z;
		
        float vecDotAxis = dot(vec, axis);
        float cosAngle = cos(angle);
        float sinAngle = sin(angle);

	float xPrime = u*vecDotAxis*(1.0-cosAngle)+x*cosAngle+(-w*y + v*z)*sinAngle;
	float yPrime = v*vecDotAxis*(1.0-cosAngle)+y*cosAngle+(w*x-u*z)*sinAngle;
	float zPrime = w*vecDotAxis*(1.0-cosAngle)+z*cosAngle+(-v*x + u*y)*sinAngle;
	return vec3(xPrime,yPrime,zPrime);
}

float ctMin, ctMax;

vec3 findEdge(vec3 start, vec3 direction, bool inWater)
{
//        ctMin = 32768.0;
//        ctMax = 0.0;

        float stepSize = 0.1; // mm

	bool strike = false;
	vec3 pp = start+stepSize*direction;
	for (int i = 0; i<1500; i++) // 150 mm traversal
	{
		vec3 tcoord = (ct_tex_matrix * vec4 (pp, 1)).xyz;
		float ctsample = texture(ct_tex,tcoord).r * 65535.0 * ct_rescale_slope + ct_rescale_intercept;
		if(inWater)
		{
			if (ctsample > ct_bone_threshold)
			{
				strike = true;
				break;
			}
		}
		else
		{
                        // Add some hysteresis here so we make sure we are all the way through the skull, not in diploe
			if (ctsample < ct_bone_threshold*0.3)
			{
                            pp += direction * 30.0;
                            // back up to the ct threshold
                            int maxSteps = 0;
                            while(ctsample < ct_bone_threshold && maxSteps < 500) {
                                pp -= direction*stepSize;
                                tcoord = (ct_tex_matrix * vec4 (pp, 1)).xyz;
                                ctsample = texture(ct_tex,tcoord).r * 65535.0 * ct_rescale_slope + ct_rescale_intercept;
                                maxSteps++;
                            }
                            strike = true;
                            break;
			}
		}

//                if (ctsample > 1024+1200) { //ct_bone_threshold) {
//                    ctMax = max(ctMax, ctsample);
//                    ctMin = min(ctMin, ctsample);
//                }

		pp += direction*stepSize;
	}
	if(!strike)
	{
		///no collision found
		return start;
	}
	return pp;
}

vec3 findNormal(vec3 position)
{
	vec3 grad = vec3(0);
	for (int i = 0; i<125; i++)
	{
		vec3 coord = (ct_tex_matrix * vec4(position+offset[i],1)).xyz;
		float s = texture(ct_tex,coord).r * 65535.0 * ct_rescale_slope + ct_rescale_intercept;
		grad.x += s * xGradZH[i];
		grad.y += s * yGradZH[i];
		grad.z += s * zGradZH[i];
	}
	return normalize(grad);
}

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

float calcNormSkullThickness(vec3 outerSkullPoint, vec3 norm) {
    vec3 innerSkullPoint = findEdge(outerSkullPoint, norm, false);
    if (innerSkullPoint == outerSkullPoint) {
        return -1.0;
    }
    else {
        return distance(innerSkullPoint, outerSkullPoint);
    }

}

layout (local_size_x = 256, local_size_y = 1, local_size_z = 1) in;

const float WATER_DENSITY = 993; // kg/m^3
const float SKULL_OUTER_TABLE_DENSITY = 1879; // kg/m^3
const float SKULL_DIPLOE_DENSITY = 1738; // kg/m^3
const float BRAIN_DENSITY = 1081; //kg/m^3

const float TRABECULAR_BONE_SPEED = 2500; // m/s
const float BRAIN_SPEED = 1540; // m/s

const float OUTER_TABLE_THICKNESS = 1.3; // mm
const float INNER_TABLE_THICKNESS = 1.5; // mm

const float CORTICAL_BONE_ATTENUATION = 152.28; // m^-1
const float TRABECULAR_BONE_ATTENUATION = 148.28; // m^-1

void main()
{
         uint gid = gl_GlobalInvocationID.x;

         if (gid>=elementCount) return;

         vec3 pos = e[gid].pos.xyz;
         vec3 v = normalize(e[gid].dir.xyz);

         ////////////////Find first collision
         vec3 firstCollision = pos.xyz;
         if (e[gid].pos.w > 0) {
            firstCollision = findEdge(pos,v,true);
         }

         vec3 secondCollision = firstCollision;
         vec3 refractedVector = vec3(0);
         vec3 normal = vec3(0);
         vec3 sfnormal = vec3(0);
         vec3 exitVector = vec3(0);
         vec3 exitPoint = vec3(0);
         vec3 secondNormal = vec3(0);
         vec4 outColor = vec4(0);
         float firstIncidenceAngle = -1.0;

         float thetai = 0;
         float thetat = 0;
         float thetai2 = 0;
         float thetat2 = 0;

//         d[gid].sdr = 0.0;
        d[gid].incidentAngle = -1;
        d[gid].distance = -1;

 //        float critAngle = max(asin(waterSpeed/boneSpeed), M_PI/8);//*0.625;
//         float critAngle = min(asin(waterSpeed/boneSpeed), M_PI/4.0);
//         float critAngle = 0.436; // 25 degrees
         float critAngle = asin(waterSpeed/boneSpeed);

         if (firstCollision != pos)
		{
         	//First collision found, get normal
         	normal = findNormal(firstCollision);
         	normal = normalize(normal);

                d[gid].skullNormThickness = calcNormSkullThickness(firstCollision, normal);
                
//                if (dot(normal, v) < 0) {
//                    normal = -normal;
//                }

         	//Get incidence angle
         	firstIncidenceAngle = abs(acos(clamp(dot(normal,v)/(length(normal)*length(v)), -1, 1)));
                d[gid].incidentAngle = firstIncidenceAngle / (2.0*M_PI) * 360.0;

                // we will use later for transmission coeff calc;
                thetai = firstIncidenceAngle;

                if (firstIncidenceAngle < critAngle) {
                
                refractedVector = normalize(refract(v, -normal, boneSpeed/waterSpeed));

                thetat = asin(boneSpeed/waterSpeed * sin(thetai));

     		secondCollision = findEdge(firstCollision,refractedVector,false);
     		if(secondCollision != firstCollision)
     		{
//                        d[gid].sdr = (ctMin-1024.0)/(ctMax-1024.0);
     			//second collision found, get normal (negate normal for correct alignment when finding angle)
     			secondNormal = findNormal(secondCollision);
                        //float flip = sign(dot(refractedVector, secondNormal));
//     			secondNormal = -(secondNormal);
                        if (dot(secondNormal, refractedVector) < 0) {
                            secondNormal = -secondNormal;
                        }
		        
     			//find second incidence angle
                        float secondIncidenceAngle = abs(acos(clamp(dot(secondNormal,refractedVector)/(length(secondNormal)*length(refractedVector)), -1, 1)));
                        
                        thetai2 = secondIncidenceAngle;
                        thetat2 = asin(waterSpeed/boneSpeed * sin(thetai2));

//                        if (secondIncidenceAngle < critAngle) {
                            exitVector = normalize(refract(refractedVector, -secondNormal, waterSpeed/boneSpeed));

                            exitPoint = secondCollision + 15*exitVector;
                            vec3 focalPoint = target;
                                float missDistance = length(cross((exitPoint-secondCollision),(secondCollision-focalPoint)))/length(exitPoint-secondCollision);
                            d[gid].distance = missDistance;
    //     				outColor = mix(vec4(0, 1, 0, 1), vec4(1, 1, 0, 1), remap(0, M_PI/6, firstIncidenceAngle));
//                        }
//                        else {
//                            exitPoint = secondCollision;
//                            d[gid].distance = -1;
//                        }
//	            if (firstIncidenceAngle > critAngle) {
//                	outColor = mix(vec4(1, 0, 0, 0.8), vec4(0.5, 0, 0, 0.3), remap(critAngle, critAngle*2, firstIncidenceAngle));
//	            }
//	            else {
	                //outColor = (gid == selectedElement) ? vec4(0.3, 0.3, 1, 1) : mix(vec4(0, 1, 0, 1), vec4(1, 1, 0, 1), remap(0, critAngle, firstIncidenceAngle));
	                outColor =  mix(vec4(0, 1, 0, 1), vec4(1, 1, 0, 1), remap(0, critAngle, firstIncidenceAngle));
//	            }
         	}
/*         	else
	         {
	         	//SPOT IS NO GOOD
                        secondCollision = firstCollision;
	         	d[gid].distance = -1;
	         } */
             }
/*             else {
                        secondCollision = firstCollision;
	         	d[gid].distance = -1;
             } */
         }
/*         else
         {
         	//SPOT IS NO GOOD
         	firstCollision = pos;
                secondCollision = firstCollision;
         	d[gid].distance = -1;
         } */
        
        float exitLength = 50;
        vec3 skullFloorPoint = pos;//secondCollision.xyz;
        if (d[gid].distance == -1) {
            exitLength = 0;
            outColor = vec4(1,0,0,1);
        }

        o[gid].pos[0].xyz = firstCollision.xyz;
        o[gid].pos[0].w = 1;
        
	o[gid].pos[1].xyz = firstCollision.xyz-v*1.0; // entrance flag height
        o[gid].pos[1].w = 1;
        
        o[gid].pos[2].xyz = firstCollision.xyz;
        o[gid].pos[2].w = 1;
        
	o[gid].pos[3].xyz = secondCollision.xyz;
        o[gid].pos[3].w = 1;

        o[gid].pos[4].xyz = secondCollision.xyz;
        o[gid].pos[4].w = 1;

        
        vec3 finalPoint = secondCollision.xyz;



        if (d[gid].distance != -1) {
            finalPoint = GetClosestPoint(secondCollision, secondCollision.xyz+exitVector*15, target); // target = vec3(0,0,0) + steering
            //finalPoint = secondCollision.xyz+exitVector*15;//exitLength;
            
            // check to see if ray is refracted back out of skull
            if (distance(finalPoint, secondCollision.xyz) == 0) {
                d[gid].distance = -1;
                exitLength = 0;
                outColor = vec4(1,0,0,1);
            }

            d[gid].skullThickness = distance(firstCollision, secondCollision);

            skullFloorPoint = findEdge(finalPoint,exitVector,true);
            if (skullFloorPoint == finalPoint) {
                skullFloorPoint = pos;
            }
        }

        float term1 = SKULL_OUTER_TABLE_DENSITY*boneSpeed*cos(thetai);
        float term2 = WATER_DENSITY*waterSpeed*cos(thetat);
        float outerTransmissionCoeff = 1.0 - (term1-term2)/(term1+term2);

        term1 = WATER_DENSITY*waterSpeed*cos(thetai2);
        term2 = SKULL_OUTER_TABLE_DENSITY*boneSpeed*cos(thetat2);
        float innerTransmissionCoeff = 1.0 - (term1-term2)/(term1+term2);

        float totalThick = d[gid].skullThickness;
        float corticalThickness = min(totalThick, OUTER_TABLE_THICKNESS + INNER_TABLE_THICKNESS);
        float diploeThickness = max(0.0, totalThick - corticalThickness);

        float absorbCoeffCort = exp(-CORTICAL_BONE_ATTENUATION*corticalThickness/1000);
        float absorbCoeffTrab = exp(-TRABECULAR_BONE_ATTENUATION*diploeThickness/1000);

        d[gid].skullTransmissionCoeff = outerTransmissionCoeff * innerTransmissionCoeff * absorbCoeffCort * absorbCoeffCort * absorbCoeffTrab * absorbCoeffTrab;

        int currentChannel = int(gid);
        if (currentChannel == selectedElement) {
            outColor.x = 0.3;
            outColor.y = 0.3;
            outColor.z = 1.0;
        }

	o[gid].pos[5].xyz = finalPoint;
        o[gid].pos[5].w = 1;

        
        for (int i=0; i<6; i++) {
            c[gid].color[i].xyz = outColor.xyz * 0.9;
            c[gid].color[i].w = outColor.w;
        }

        float skullFloorIncidenceAngle = 0.0;
        vec4 sfOutColor = vec4(0);

        if (d[gid].distance == -1 || skullFloorPoint == pos) {
            sfRays[gid*2].pos.xyz = pos;
            sfRays[gid*2].pos.w = 1;

            sfRays[gid*2+1].pos.xyz = pos;
            sfRays[gid*2+1].pos.w = 1;

            sfRays[gid*2].col = vec4(0, 0, 0, 0);
            sfRays[gid*2+1].col = vec4(0, 0, 0, 0);

            sfOutColor = vec4(0);
        }
        else {

            sfRays[gid*2].pos.xyz = finalPoint;
            sfRays[gid*2].pos.w = 1;

            sfRays[gid*2+1].pos.xyz = skullFloorPoint.xyz;
            sfRays[gid*2+1].pos.w = 1;

            sfRays[gid*2].col = vec4(1, 0.3, 0.3, 1);
            sfRays[gid*2+1].col = vec4(1, 0.3, 0.3, 1);

            sfnormal = findNormal(skullFloorPoint);
            sfnormal = normalize(sfnormal);

            skullFloorIncidenceAngle = abs(acos(clamp(dot(sfnormal,exitVector)/(length(sfnormal)*length(exitVector)), -1, 1))) / (2.0*M_PI) * 360.0;
            sfOutColor =  mix(vec4(1, 0, 0, 1), vec4(0.2, 0.2, 1, 1), remap(10, 45, skullFloorIncidenceAngle));
        }

        outRays[gid*2].pos.xyz = pos.xyz;
        outRays[gid*2].pos.w = 1.0;
        outRays[gid*2].col.xyz = outColor.xyz * 0.9;
        outRays[gid*2].col.w = 0.7;

        outRays[gid*2+1].pos.xyz = firstCollision.xyz;
        outRays[gid*2+1].pos.w = 1.0;
        outRays[gid*2+1].col.xyz = outColor.xyz * 0.9;
        outRays[gid*2+1].col.w = 0.7;

        // discs normal to outer skull surface
        vec3 xvec = normalize(cross(normal, vec3(0, 0, 1)));
        vec3 yvec = normalize(cross(normal, xvec));

        // discs normal to skull floor
        vec3 sfxvec = normalize(cross(sfnormal, vec3(0, 0, 1)));
        vec3 sfyvec = normalize(cross(sfnormal, sfxvec));

 //HACK - just mapping sdr to disc color for now
//outColor = mix(vec4(1, 0, 0, 1), vec4(0, 1, 0, 1), clamp(remap(0.3, 1.0, d[gid].sdr), 0, 1));
/*
if (d[gid].distance > -1) {
    //phase
    float wrappedPhase = mod(outPhase[gid], 2 * M_PI);
    if (wrappedPhase > M_PI) {
        wrappedPhase -= 2 * M_PI;
    }
    outColor = mix(vec4(1, 0, 0, 1), vec4(0, 1, 0, 1), clamp((wrappedPhase+M_PI)/(2*M_PI), 0, 1));

    // skull thickness
    //outColor = mix(vec4(0, 1, 0, 1), vec4(1, 0, 0, 1), clamp(distance(firstCollision.xyz, secondCollision.xyz)/20.0, 0, 1));
}
else {
  outColor = vec4(0, 0, 0, 1);
}
*/
///
        uint index1 = gid*20*3;
        for (int i=0; i<20; i++) {
            uint startIndex = i*3 + index1;
            float angle1 = i/20.0*3.14159*2.0;
            float angle2 = (i+1)/20.0*3.14159*2.0;

            outDiscTris[startIndex].pos.xyz = firstCollision.xyz - normal * 1.0; // lifting them 1mm so they don't clip with the skull surface
            outDiscTris[startIndex].pos.w = 1.0;
            outDiscTris[startIndex].norm.xyz = normal;
            outDiscTris[startIndex].norm.w = 0.0;
            outDiscTris[startIndex].col.xyz = outColor.xyz * 0.8;
            outDiscTris[startIndex].col.a = 1.0;

            outDiscTris[startIndex+1].pos.xyz = firstCollision.xyz - normal + 2.0 *(cos(angle1)*xvec + sin(angle1)*yvec);
            outDiscTris[startIndex+1].pos.w = 1.0;
            outDiscTris[startIndex+1].norm.xyz = normal;
            outDiscTris[startIndex+1].norm.w = 0.0;
            outDiscTris[startIndex+1].col.xyz = outColor.xyz * 0.8;
            outDiscTris[startIndex+1].col.a = 1.0;

            outDiscTris[startIndex+2].pos.xyz = firstCollision.xyz - normal + 2.0 *(cos(angle2)*xvec + sin(angle2)*yvec);
            outDiscTris[startIndex+2].pos.w = 1.0;
            outDiscTris[startIndex+2].norm.xyz = normal;
            outDiscTris[startIndex+2].norm.w = 0.0;
            outDiscTris[startIndex+2].col.xyz = outColor.xyz * 0.8;
            outDiscTris[startIndex+2].col.a = 1.0;
            outDiscTris[startIndex].pos.xyz = firstCollision.xyz - normal * 1.0; // lifting them 1mm so they don't clip with the skull surface
            outDiscTris[startIndex].pos.w = 1.0;


// skull floor normal discs
            sfDiscTris[startIndex].pos.xyz = skullFloorPoint.xyz - sfnormal * 1.0; // lifting them 1mm so they don't clip with the skull surface
            sfDiscTris[startIndex].pos.w = 1.0;
            sfDiscTris[startIndex].norm.xyz = sfnormal;
            sfDiscTris[startIndex].norm.w = 0.0;
            sfDiscTris[startIndex].col.xyz = sfOutColor.xyz * 0.8;
            sfDiscTris[startIndex].col.a = 1.0;

            sfDiscTris[startIndex+1].pos.xyz = skullFloorPoint.xyz - sfnormal + 1.0 *(cos(angle1)*sfxvec + sin(angle1)*sfyvec);
            sfDiscTris[startIndex+1].pos.w = 1.0;
            sfDiscTris[startIndex+1].norm.xyz = sfnormal;
            sfDiscTris[startIndex+1].norm.w = 0.0;
            sfDiscTris[startIndex+1].col.xyz = sfOutColor.xyz * 0.8;
            sfDiscTris[startIndex+1].col.a = 1.0;

            sfDiscTris[startIndex+2].pos.xyz = skullFloorPoint.xyz - sfnormal + 1.0 *(cos(angle2)*sfxvec + sin(angle2)*sfyvec);
            sfDiscTris[startIndex+2].pos.w = 1.0;
            sfDiscTris[startIndex+2].norm.xyz = sfnormal;
            sfDiscTris[startIndex+2].norm.w = 0.0;
            sfDiscTris[startIndex+2].col.xyz = sfOutColor.xyz * 0.8;
            sfDiscTris[startIndex+2].col.a = 1.0;
            sfDiscTris[startIndex].pos.xyz = skullFloorPoint.xyz - sfnormal * 1.0; // lifting them 1mm so they don't clip with the skull surface
            sfDiscTris[startIndex].pos.w = 1.0;
        }
}