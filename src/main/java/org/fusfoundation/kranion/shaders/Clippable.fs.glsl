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
#version 120
varying vec4 diffuse,ambientGlobal, ambient, wPos;
varying vec4 ecPos;
varying vec3 normal,halfVector;
uniform float time;

// Idea of this shader is to create a textured surface for clipped solid
// objects that appears to respond sensibly as the objects are rotated
// with respect to the clip plane and distiguish clipped from unclipped
// surfaces. The texture is generated using a 3D Perlin noise field.

// Perlin noise ideas and sample code taken from:
// https://github.com/ashima/webgl-noise/wiki

vec3 mod289(vec3 x) {
  return x - floor(x * (1.0 / 289.0)) * 289.0;
}

vec4 mod289(vec4 x) {
  return x - floor(x * (1.0 / 289.0)) * 289.0;
}

vec4 permute(vec4 x) {
     return mod289(((x*34.0)+1.0)*x);
}

vec4 taylorInvSqrt(vec4 r)
{
  return vec4(1.79284291400159) - 0.85373472095314 * r;
}

float snoise(vec3 v)
  { 
  const vec2  C = vec2(1.0/6.0, 1.0/3.0) ;
  const vec4  D = vec4(0.0, 0.5, 1.0, 2.0);

// First corner
  vec3 i  = floor(v + dot(v, C.yyy) );
  vec3 x0 =   v - i + dot(i, C.xxx) ;

// Other corners
  vec3 g = step(x0.yzx, x0.xyz);
  vec3 l = 1.0 - g;
  vec3 i1 = min( g.xyz, l.zxy );
  vec3 i2 = max( g.xyz, l.zxy );

  //   x0 = x0 - 0.0 + 0.0 * C.xxx;
  //   x1 = x0 - i1  + 1.0 * C.xxx;
  //   x2 = x0 - i2  + 2.0 * C.xxx;
  //   x3 = x0 - 1.0 + 3.0 * C.xxx;
  vec3 x1 = x0 - i1 + C.xxx;
  vec3 x2 = x0 - i2 + C.yyy; // 2.0*C.x = 1/3 = C.y
  vec3 x3 = x0 - D.yyy;      // -1.0+3.0*C.x = -0.5 = -D.y

// Permutations
  i = mod289(i); 
  vec4 p = permute( permute( permute( 
             i.z + vec4(0.0, i1.z, i2.z, 1.0 ))
           + i.y + vec4(0.0, i1.y, i2.y, 1.0 )) 
           + i.x + vec4(0.0, i1.x, i2.x, 1.0 ));

// Gradients: 7x7 points over a square, mapped onto an octahedron.
// The ring size 17*17 = 289 is close to a multiple of 49 (49*6 = 294)
  float n_ = 0.142857142857; // 1.0/7.0
  vec3  ns = n_ * D.wyz - D.xzx;

  vec4 j = p - 49.0 * floor(p * ns.z * ns.z);  //  mod(p,7*7)

  vec4 x_ = floor(j * ns.z);
  vec4 y_ = floor(j - 7.0 * x_ );    // mod(j,N)

  vec4 x = x_ *ns.x + ns.yyyy;
  vec4 y = y_ *ns.x + ns.yyyy;
  vec4 h = 1.0 - abs(x) - abs(y);

  vec4 b0 = vec4( x.xy, y.xy );
  vec4 b1 = vec4( x.zw, y.zw );

  //vec4 s0 = vec4(lessThan(b0,0.0))*2.0 - 1.0;
  //vec4 s1 = vec4(lessThan(b1,0.0))*2.0 - 1.0;
  vec4 s0 = floor(b0)*2.0 + 1.0;
  vec4 s1 = floor(b1)*2.0 + 1.0;
  vec4 sh = -step(h, vec4(0.0));

  vec4 a0 = b0.xzyw + s0.xzyw*sh.xxyy ;
  vec4 a1 = b1.xzyw + s1.xzyw*sh.zzww ;

  vec3 p0 = vec3(a0.xy,h.x);
  vec3 p1 = vec3(a0.zw,h.y);
  vec3 p2 = vec3(a1.xy,h.z);
  vec3 p3 = vec3(a1.zw,h.w);

//Normalise gradients
  vec4 norm = taylorInvSqrt(vec4(dot(p0,p0), dot(p1,p1), dot(p2, p2), dot(p3,p3)));
  p0 *= norm.x;
  p1 *= norm.y;
  p2 *= norm.z;
  p3 *= norm.w;

// Mix final noise value
  vec4 m = max(0.6 - vec4(dot(x0,x0), dot(x1,x1), dot(x2,x2), dot(x3,x3)), 0.0);
  m = m * m;
  return 42.0 * dot( m*m, vec4( dot(p0,x0), dot(p1,x1), 
                                dot(p2,x2), dot(p3,x3) ) );
}
 
float fbm(vec3 P, int octaves, float lacunarity, float gain)
{
    float sum = 0.0;
    float amp = 1.0;
    vec3 pp = P;
     
    int i;
     
    for(i = 0; i < octaves; i+=1)
    {
        amp *= gain; 
        sum += amp * snoise(pp);
        pp *= lacunarity;
    }
    return sum;
 
}
 
 
float pattern(in vec3 p) {
    float l = 2.5;
    float g = 0.4;
    int oc = 10;
     
    vec3 q = vec3( fbm( p + vec3(0.0,0.0,0.0),oc,l,g),fbm( p + vec3(5.2,1.3, 2.4),oc,l,g),fbm( p + vec3(7.2,5.3, 3.4),oc,l,g));
    vec3 r = vec3( fbm( p + 4.0*q + vec3(1.7,9.2, 5.1),oc,l,g ), fbm( p + 4.0*q + vec3(8.3,2.8,1.3) ,oc,l,g), fbm( p + 4.0*q + vec3(11.3,6.8,3.3) ,oc,l,g));
    return fbm( p + 4.0*r ,oc,l,g);    
}

 
void main()
{
    vec3 n, lightDir;
     
    /* a fragment shader can't write a verying variable, hence we need
    a new variable to store the normalized interpolated normal */

    vec3 ns = vec3(
        -0.15 * snoise(wPos.xyz / 12.5) + 0.35 * snoise(wPos.xyz / 5.0) +  0.2 * snoise(wPos.xyz / 0.5) + 0.2 * snoise(wPos.xyz / 0.15),
        -0.15 * snoise(wPos.yzx / 12.5) + 0.35 * snoise(wPos.yzx / 5.0) +  0.2 * snoise(wPos.yzx / 0.5) + 0.2 * snoise(wPos.yzx / 0.15),
        -0.15 * snoise(wPos.zxy / 12.5) + 0.35 * snoise(wPos.zxy / 5.0) +  0.2 * snoise(wPos.zxy / 0.5) + 0.2 * snoise(wPos.zxy / 0.15)
    );

    //n = normalize(normal + 0.3 * pattern(wPos.xyz / 150.0));

    // We perturb the surface normal by a small amount of perlin noise.
    // Trying to make the the surface looked etched when lit.
    n = normalize(normal + abs(ns*0.15));
    //n = normalize(normal);
    
    // Now we compute a basic lighting equation for ADS lighting with one light source for now

    // Compute the ligt direction
    lightDir = normalize(vec3(gl_LightSource[0].position.xyz-ecPos.xyz));
     
    vec3 E = normalize(-ecPos.xyz);

    vec3 R = normalize(-reflect(lightDir, n));

    vec4 Iamb = gl_FrontLightProduct[0].ambient;

    vec4 Idiff = gl_FrontLightProduct[0].diffuse * max(dot(n, lightDir), 0.0);
    //vec4 Idiff = gl_FrontMaterial.diffuse * max(dot(n, lightDir), 0.0);

    vec4 Ispec = gl_FrontLightProduct[0].specular * pow(max(dot(R, E), 0.0), 0.3*gl_FrontMaterial.shininess);
    //vec4 Ispec = gl_FrontLightProduct[0].specular * pow(max(dot(R, E), 0.0), 0.3*gl_FrontMaterial.shininess);
 
    gl_FragColor = Iamb + Idiff + Ispec;;

}