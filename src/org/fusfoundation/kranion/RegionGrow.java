/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fusfoundation.kranion;

import org.fusfoundation.kranion.model.image.*;
import java.util.*;
/**
 *
 * @author john
 */
public class RegionGrow {
    ImageVolume4D src;
    
    Queue growQ = new LinkedList();
    
    public RegionGrow() {
        
    }
    
    public void setSourceImage(ImageVolume4D image) {
        src = image;
        
        if (src != null) {
            src.addChannel(ImageVolume.UBYTE_VOXEL);
        }
        
//        growQ.add(thing); // push
//        thing = growQ.remove() // pop

    }
    
}
