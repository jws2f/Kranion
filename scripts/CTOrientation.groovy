import org.fusfoundation.kranion.model.image.ImageVolume;
import org.lwjgl.util.vector.*;
import java.nio.FloatBuffer;

ImageVolume img = (ImageVolume)model.getCtImage();
if (model != null && img != null && view != null) {
    // Rotation Matrix -> Quaternion
    float[] rotFloat = [1.0, 0.0,  0.0,
                        0.0, 0.0, -1.0,
                        0.0, 1.0,  0.0];
    FloatBuffer rotBuf = FloatBuffer.allocate(rotFloat.length).put(rotFloat).position(0);
    Matrix3f rotMatrix = new Matrix3f().load(rotBuf);
    Quaternion quat = new Quaternion().setFromMatrix(rotMatrix);
    // Translation!
    Vector3f translation = new Vector3f(0,0,20);
    // Set rotation and translation!
    img.setAttribute("ImageOrientationQ", quat);
    img.setAttribute("ImageTranslation", translation);
    view.setIsDirty(true);
}