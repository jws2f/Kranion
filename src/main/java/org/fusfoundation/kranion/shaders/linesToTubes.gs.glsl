#version 430 
layout (lines) in;
layout (triangle_strip, max_vertices = 20) out;

uniform  mat4 normal;
uniform mat4 model;

uniform vec3 viewPos;
uniform vec3 lightPos;

uniform float clipThickness;

in vec4 gcolor[];
in vec4 gorigin[];
in vec4 fclipVertex[];

out vec4 fcolor;
out vec3 fnormal;

//out gl_PerVertex
//{
//  vec4 gl_Position;
//  float gl_ClipDistance[2];
//} gl_in;

const float sintable[10] = {
0,
0.642787158,
0.984807548,
0.866026288,
0.34202236,
-0.342017373,
-0.866023635,
-0.98480847,
-0.642791223,
-5.30718E-06
};

const float costable[10] = {
1,
0.766044822,
0.173649339,
-0.499998468,
-0.939691814,
-0.939693629,
-0.500003064,
0.173644113,
0.766041411,
1
};

void main() {
    vec3 dir = normalize(gl_in[1].gl_Position - gl_in[0].gl_Position).xyz;
    vec3 xdir = normalize(cross(dir, vec3(1, 0, 0)));
    vec3 ydir = normalize(cross(xdir, dir));

    for (int i=0; i<10; i++) {
        vec3 offset = 0.5 * ((xdir * costable[i]) + (ydir * sintable[i]));

        vec3 n = normalize((-offset));

        fcolor = gcolor[0];
        fnormal = n;

        gl_Position = gl_in[0].gl_Position + vec4(offset, 1);
        gl_ClipDistance[0] = -dot(fclipVertex[0], gorigin[0]) + clipThickness;
        gl_ClipDistance[1] =  dot(fclipVertex[0], gorigin[0]) + clipThickness;
        EmitVertex();

        fcolor = gcolor[1];
        fnormal = n;

        gl_Position = gl_in[1].gl_Position + vec4(offset, 1); 
        gl_ClipDistance[0] = -dot(fclipVertex[1], gorigin[1]) + clipThickness;
        gl_ClipDistance[1] =  dot(fclipVertex[1], gorigin[1]) + clipThickness;
        EmitVertex();
    }
    
    EndPrimitive();
}  