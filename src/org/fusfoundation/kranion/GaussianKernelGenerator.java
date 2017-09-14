/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fusfoundation.kranion;

import java.io.*;

/**
 *
 * @author john
 */
public class GaussianKernelGenerator {
    
    public GaussianKernelGenerator() {}
    
    public static void main(String[] args) {
        
        System.out.println("const vec3 offset[27] = {");
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (z != -1) {
                        System.out.print(",   \t");
                    } else {
                        System.out.print("\t");
                    }
                    System.out.print("vec3(" + x + ", " + y + ", " + z + ")");
                }
                if (!(x == 1 && y == 1)) {
                    System.out.print(",");
                }
                System.out.println();
            }
            System.out.println("\n");
        }
        System.out.println("};\n");
        
        System.out.println("const float gaussian1[27] = {");
        double kernelSum = 0.0;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (z != -1) {
                        System.out.print(",   \t");
                    } else {
                        System.out.print("\t");
                    }
//                    float r = (float)Math.sqrt(x*x + y*y + z*z);
//                    if (r != 0f) {
//                        System.out.print(x/r);
//                    }
//                    else {
//                        System.out.print(0f);
//                    }
                    double sigma = 2;
                    double n = 1.0 / Math.pow(Math.sqrt(2*Math.PI) * sigma, 3.0);
                    double g = n * Math.exp(-(x * x + y * y + z * z) / (2.0 * sigma * sigma));
                    System.out.print((float) g);
                    kernelSum += g;
                }
                if (!(x == 1 && y == 1)) {
                    System.out.print(",");
                }
                System.out.println();
            }
            System.out.println("\n");
        }
        System.out.println("};\n");
        
        System.out.println("float kernelSum = " + (float)kernelSum + ";\n");
        
        System.out.println("const float yGradZH[125] = {");
        for (int x=-2; x<=2; x++) {
            for (int z=-2; z<=2; z++) {
                for (int y=-2; y<=2; y++) {
                    if (y!=-2) {
                        System.out.print(",   \t");
                    }
                    else {
                        System.out.print("\t");
                    }
                    float r = (float)Math.sqrt(x*x + y*y + z*z);
                    if (r != 0f) {
                        System.out.print(y/r);
                    }
                    else {
                        System.out.print(0f);
                    }
                }
                if (!(x==2 && z==2)) {
                    System.out.print(",");
                }
                System.out.println();
            }
            System.out.println("\n");
        }    
        System.out.println("};\n");
        
        System.out.println("const float zGradZH[125] = {");
        for (int x=-2; x<=2; x++) {
            for (int z=-2; z<=2; z++) {
                for (int y=-2; y<=2; y++) {
                    if (y!=-2) {
                        System.out.print(",   \t");
                    }
                    else {
                        System.out.print("\t");
                    }
                    float r = (float)Math.sqrt(x*x + y*y + z*z);
                    if (r != 0f) {
                        System.out.print(z/r);
                    }
                    else {
                        System.out.print(0f);
                    }
                }
                if (!(x==2 && z==2)) {
                    System.out.print(",");
                }
                System.out.println();
            }
            System.out.println("\n");
        }
        System.out.println("};");
    }
}
