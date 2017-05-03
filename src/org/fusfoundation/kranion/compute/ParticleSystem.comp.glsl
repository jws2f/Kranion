#version 430
#extension GL_ARB_compute_shader : enable
#extension GL_ARB_shader_storage_buffer_object : enable

struct particle{
         vec4 currentPos;
         vec4 dir;
};

struct particleColor{
         vec4 color;
};

layout(std430, binding=0) buffer particles{
          particle p[];
};

layout(std430, binding=1) buffer colors{
          particleColor c[];
};

layout (local_size_x = 256, local_size_y = 1, local_size_z = 1) in;
void main(){
         const float DT = 1;
         const vec3 g1 = vec3(-100.0, -100.0, -100.0);
         const vec3 g2 = vec3(100.0, 100.0, 100.0);
         uint gid = gl_GlobalInvocationID.x;
         vec3 pos = p[gid].currentPos.xyz;
         vec3 v = p[gid].dir.xyz;

         vec3 G = normalize(pos-g1)*-.98;
         float gd = length(pos-g1);
         vec3 G2 = normalize(pos-g2)*-.98;
         float gd2 = length(pos-g2);

         vec3 pp = pos + v*DT + 0.5*DT*DT*G/(gd*gd) + 0.5*DT*DT*G2/(gd2*gd2);
         vec3 vp = v + G*DT/(gd*gd) + G2*DT/(gd2*gd2);
         p[gid].currentPos.xyz = pp;
         p[gid].dir.xyz = vp;
         vec3 pcolor = normalize(vp);
         c[gid].color.x = abs(pcolor.x);
         c[gid].color.y = abs(pcolor.y);
         c[gid].color.z = abs(pcolor.z);
}

