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
public class GradientKernelGenerator {
    
    public GradientKernelGenerator() {}
    
    public static void main(String[] args) {
        
        System.out.println("const vec3 offset[125] = {");
        for (int x=-2; x<=2; x++) {
            for (int z=-2; z<=2; z++) {
                for (int y=-2; y<=2; y++) {
                    if (y!=-2) {
                        System.out.print(",   \t");
                    }
                    else {
                        System.out.print("\t");
                    }
                    System.out.print("vec3(" + x + ", " + y + ", " + z + ")");
                }
                if (!(x==2 && z==2)) {
                    System.out.print(",");
                }
                System.out.println();
            }
            System.out.println("\n");
        }
        System.out.println("};\n");
        
        System.out.println("const float xGradZH[125] = {");
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
                        System.out.print(x/r);
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
