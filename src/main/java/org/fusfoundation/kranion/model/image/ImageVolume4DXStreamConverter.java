/*
 * The MIT License
 *
 * Copyright 2019 Focused Ultrasound Foundation.
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
package org.fusfoundation.kranion.model.image;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.fusfoundation.kranion.ProgressListener;
import org.fusfoundation.kranion.model.AttributeList;
import static org.fusfoundation.kranion.model.image.ImageVolume.FLOAT_VOXEL;
import static org.fusfoundation.kranion.model.image.ImageVolume.INT_VOXEL;
import static org.fusfoundation.kranion.model.image.ImageVolume.RGBA_VOXEL;
import static org.fusfoundation.kranion.model.image.ImageVolume.UBYTE_VOXEL;
import static org.fusfoundation.kranion.model.image.ImageVolume.USHORT_VOXEL;
import org.lwjgl.BufferUtils;

/**
 *
 * @author jsnell
 */
public class ImageVolume4DXStreamConverter implements Converter {

    private int imageDataChannelRefCount=0;
    private List<Map.Entry<ImageVolume4D, Integer>> dataChannelList = new ArrayList<>();
    
    public int getDataChannelCount() { return dataChannelList.size(); }
    public Map.Entry<ImageVolume4D, Integer> getDataChannel(int index) { return dataChannelList.get(index); }
    
    @Override
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext mc) {
        ImageVolume4D image = (ImageVolume4D)o;
        
        writer.startNode("Dimensions");
        writer.addAttribute("dimensionality", Integer.toString(image.getDimensionality()));
            for (int i=0; i<image.getDimensionality(); i++) {
                writer.startNode("Dimension");
                writer.addAttribute("num", Integer.toString(i));
                mc.convertAnother(image.getDimension(i));
                writer.endNode();
            }
        writer.endNode();
        
        writer.startNode("Attributes");
            mc.convertAnother(image.getAttributeList());
        writer.endNode();
        
        writer.startNode("VoxelData");
        writer.startNode("Channels");
        writer.addAttribute("count", Integer.toString(image.getChannelCount()));
            for (int c=0; c<image.getChannelCount(); c++) {
                writer.startNode("Channel");
                    writer.addAttribute("num", Integer.toString(c));
                    writer.addAttribute("voxelType", Integer.toString(image.getVoxelType()));
                    
                    switch (image.getVoxelType()) {
                        case UBYTE_VOXEL:
                            writer.addAttribute("voxelTypeName", "UBYTE");
                            break;
                        case USHORT_VOXEL:
                            writer.addAttribute("voxelTypeName", "USHORT");
                            break;
                        case INT_VOXEL:
                            writer.addAttribute("voxelTypeName", "INT");
                            break;
                        case RGBA_VOXEL:
                            writer.addAttribute("voxelTypeName", "RGBA");
                            break;
                        case FLOAT_VOXEL:
                            writer.addAttribute("voxelTypeName", "FLOAT");
                            break;
                    };
                    
                    dataChannelList.add(new AbstractMap.SimpleImmutableEntry<ImageVolume4D, Integer>(image, c));
                    writer.setValue("imageData/imageChannel" + Integer.toString(imageDataChannelRefCount++));
                writer.endNode();
            }
        writer.endNode();
        writer.endNode();
    }
    
    public void marshalVoxelData(ZipOutputStream zipOutputStream, ProgressListener listener) throws IOException {
        if (zipOutputStream != null) {
            for (int i = 0; i < getDataChannelCount(); i++) {
                
                if (listener != null) {
                    listener.percentDone("Saving scene", (int)(100.0f * (float)i/getDataChannelCount() * 0.9f) + 10);
                }
                
                zipOutputStream.putNextEntry(new ZipEntry("imageData/imageChannel" + i));

                encodeVoxelStream(getDataChannel(i).getKey(), getDataChannel(i).getValue(), zipOutputStream);
                
                zipOutputStream.closeEntry();
            }
        }
        if (listener != null) {
            listener.percentDone("Saving scene", 100);
        }
    }
    
    private void encodeVoxelStream(ImageVolume4D image, int channel, OutputStream ostream) throws IOException {
        
        System.out.println("Encoding image channel " + channel);
        
        int frameSize = image.getFrameSize();
        int voxelCount = image.getVoxelCount();
        
        if (frameSize == 0) return;
        
        int frameCount = voxelCount / frameSize;
        
        if (frameCount == 0) return;
        
        
        BufferedOutputStream bos = new BufferedOutputStream(ostream);
        
        ByteBuffer buf=null;
        for (int i=0; i<frameCount; i++) {
            buf = image.getFrameAsByteBuffer(buf, channel, false, frameSize, i);
            bos.write(buf.array());
        }
    }
    
    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
        ImageVolume4D image = new ImageVolume4D();
        
        
        while(reader.hasMoreChildren()) {
            reader.moveDown();
            
            String nodeName = reader.getNodeName();
            
            if (nodeName.equalsIgnoreCase("Dimensions")) {
                int i=0;
                while(reader.hasMoreChildren()) {
                    reader.moveDown();
                    String dimNum = reader.getAttribute("num");
                    try {
                        int d = Integer.parseInt(dimNum);
                        ImageDimension dim = (ImageDimension)uc.convertAnother(image, ImageDimension.class);
                        image.setDimension(d, dim);
                    }
                    catch(NumberFormatException e) {
                        throw new ConversionException("Dimension must have num attribute");
                    }
                    reader.moveUp();
                }
            }
            else if (nodeName.equalsIgnoreCase("attributes")) {        
                AttributeList attrList = (AttributeList)uc.convertAnother(image, AttributeList.class);
                Iterator<String> keys = attrList.keySet().iterator();
                while(keys.hasNext()) {
                    String key = keys.next();
                    image.setAttribute(key, AttributeList.translateIn(attrList.get(key)));
                }
            }
            else if (nodeName.equalsIgnoreCase("VoxelData")) {
                reader.moveDown();
                nodeName = reader.getNodeName();
                if (nodeName.equalsIgnoreCase("Channels")) {
                    while(reader.hasMoreChildren()) {
                        reader.moveDown();
                        String channelNum = reader.getAttribute("num");
                        String voxelType = reader.getAttribute("voxelType");
                        try {
                            int ch = Integer.parseInt(channelNum);
                            int voxtype = Integer.parseInt(voxelType);
                            
                            image.addChannel(ch, voxtype);
                            
                            dataChannelList.add(new AbstractMap.SimpleImmutableEntry<ImageVolume4D, Integer>(image, ch));
                                                        
//                            byte payload[] = Base64.getDecoder().decode(reader.getValue());
//                            ByteBuffer buf = ByteBuffer.wrap(payload);
//                            ShortBuffer shortBuf = buf.asShortBuffer();
//                                    
//                            Object imageData = image.getData(ch);
//                        
//                            try {
//                            shortBuf.get((short[])imageData);
//                            }
//                            catch(Exception e) {}
                            
//                            switch (voxtype) {
//                                case UBYTE_VOXEL:
//                                    voxelData.set(channel, new byte[voxelCount]);
//                                    break;
//                                case USHORT_VOXEL:
//                                    voxelData.set(channel, new short[voxelCount]);
//                                    break;
//                                case INT_VOXEL:
//                                case RGBA_VOXEL:
//                                    voxelData.set(channel, new int[voxelCount]);
//                                    break;
//                                case FLOAT_VOXEL:
//                                    voxelData.set(channel, new float[voxelCount]);
//                                    break;
//                            };
//        
//                            try {
//                                voxelType.set(channel, type);
//                            }
//                            catch(ArrayIndexOutOfBoundsException e) {
//                                voxelType.add(channel, type);
//                            }                            
                        }
                        catch(NumberFormatException e) {
                            throw new ConversionException("VoxelData Channel must have channelNum and voxelType attributes");
                        }
                        reader.moveUp();
                    }
                }
                reader.moveUp();                
            }
            
            reader.moveUp();
        }
        
        
        return image;
    }
    
    public void unmarshalVoxelData(ZipFile modelFile, ProgressListener listener) throws IOException, ClassNotFoundException {
        // Now load all the image channel data from associated zip entries
        for (int i = 0; i < getDataChannelCount(); i++) {
            
            if (listener != null) {
                listener.percentDone("Loading scene", (int)(100.0f * (float)i/getDataChannelCount() * 0.9f) + 10);
            }
            
            ZipEntry imageDataEntry = modelFile.getEntry("imageData/imageChannel" + i);
            InputStream imageDataStream = modelFile.getInputStream(imageDataEntry);
            
            ImageVolume4D theImage = getDataChannel(i).getKey();
            Integer theChannel = getDataChannel(i).getValue();
            
            decodeVoxelStream(theImage, theChannel, imageDataStream);
            
            imageDataStream.close();            
        }
        if (listener != null) {
            listener.percentDone("Loading scene", 100);
        }
    }
    
    private Object decodeVoxelStream(ImageVolume4D image, int channel, InputStream istream) throws IOException {
        System.out.println("Decoding image channel " + channel);
        
        Object result = null;
        
        if (image == null) return null;       
        
        int frameSize = image.getFrameSize();
        int voxelCount = image.getVoxelCount();
        
        if (frameSize == 0) return null;
        
        int frameCount = voxelCount / frameSize;
        
        if (frameCount == 0) return null;
        
        int voxelSize = image.getVoxelDataSize(channel);
        
        
        try (BufferedInputStream bis = new BufferedInputStream(istream)) {
            ByteBuffer buf = ByteBuffer.allocate(frameSize*voxelSize) ;
            for (int i=0; i<frameCount; i++) {
                bis.read(buf.array());
                buf = image.putFrameAsByteBuffer(buf, channel, false, frameSize, i);
                buf.rewind();
            }
        }
        
        return image.getData(channel);
    }

    @Override
    public boolean canConvert(Class type) {
        return ImageVolume4D.class == type;
    }
    
}
