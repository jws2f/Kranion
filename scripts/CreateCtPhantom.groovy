import org.fusfoundation.kranion.model.image.ImageVolume;
import org.fusfoundation.kranion.model.image.ImageVolume4D;

import org.lwjgl.util.vector.*;
import java.nio.FloatBuffer;

ImageVolume img = new ImageVolume4D(ImageVolume.USHORT_VOXEL, 512, 512, 300, 1);

img.getDimension(0).setSampleWidth(0.5f);
img.getDimension(1).setSampleWidth(0.5f);
img.getDimension(2).setSampleWidth(0.5f);

img.getDimension(0).setSampleSpacing(0.5f);
img.getDimension(1).setSampleSpacing(0.5f);
img.getDimension(2).setSampleSpacing(0.5f);

img.setAttribute("RescaleSlope", 1f);
img.setAttribute("RescaleIntercept", -1000f);
img.setAttribute("ImageOrientationQ", new Quaternion());
img.setAttribute("ImageTranslation", new Vector3f());

short[] voxels = img.getData();

for (int z=0; z<200; z++) {
    print("slice = " + z + "\n");
    
   for (int y=0; y<512; y++) {
      for (int x =0; x<512; x++) {
          float d = Math.sqrt((z-150)*(z-150) + 0.7f *(y-256)*(y-256) + (x-256)*(x-256));
          if (d<140 && d>125) {
              voxels[(z-150+150)*512*512 + (y-256+256)*512 + x - 256 + 256] = 2000 * (0.85f + 0.6f * Math.abs((d-125)/15 - 0.5));
          }
          else if (d>=140) {
              voxels[(z-150+150)*512*512 + (y-256+256)*512 + x - 256 + 256] = 2000 * Math.max(0f, 1f - (d-140f)/0.5f);
          }
           else if (d<=125) {
              voxels[(z-150+150)*512*512 + (y-256+256)*512 + x - 256 + 256] = 2000 * Math.max(0f, 1f - (125f-d)/0.5f);
          }
      }
   }
}

model.setCtImage(img);