#version 120

varying vec3 frag_position; // in object space
varying vec3 light_position;

void main(void)
{
   gl_TexCoord[0]  = gl_TextureMatrix[0] * gl_MultiTexCoord0;
   gl_TexCoord[1]  = gl_TextureMatrix[1] * gl_MultiTexCoord0;
   gl_TexCoord[3]  = gl_TextureMatrix[3] * gl_MultiTexCoord0;
   gl_TexCoord[4]  = gl_TextureMatrix[4] * gl_MultiTexCoord0;
   gl_TexCoord[5]  = gl_TextureMatrix[5] * gl_MultiTexCoord0;
   gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
   light_position = (gl_TextureMatrix[0] * vec4(150, -150, -400, 1)).xyz;
   frag_position = gl_Vertex.xyz;
}