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
package org.fusfoundation.kranion;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.fusfoundation.kranion.model.AttributeList;
import org.fusfoundation.kranion.model.image.ImageDimension;
import org.fusfoundation.kranion.model.image.ImageVolume4D;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author jsnell
 */
public class TransducerXStreamConverter implements Converter{
    private int meshDataChannelRefCount=0;
    private List<Map.Entry<PlyFileReader, Integer>> meshChannelList = new ArrayList<>();
    
    public int getDataChannelCount() { return meshChannelList.size(); }
    public Map.Entry<PlyFileReader, Integer> getDataChannel(int index) { return meshChannelList.get(index); }

    @Override
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext mc) {
        Transducer transducer = (Transducer)o;
        
        writer.addAttribute("name", transducer.getName());

        writer.startNode("Channels");
            writer.addAttribute("count", Integer.toString(transducer.getElementCount()));
            for (int i=0; i<transducer.getElementCount(); i++) {
                writer.startNode("Channel");
                    writer.addAttribute("num", Integer.toString(i));
                    writer.addAttribute("area", Float.toString(transducer.getElementArea(i)));
                    Vector3f position = transducer.getElementPos(i);
                    Vector3f normal = transducer.getElementNormal(i);
                    writer.addAttribute("x", Float.toString(position.x));
                    writer.addAttribute("y", Float.toString(position.y));
                    writer.addAttribute("z", Float.toString(position.z));
                    writer.addAttribute("nx", Float.toString(normal.x));
                    writer.addAttribute("ny", Float.toString(normal.y));
                    writer.addAttribute("nz", Float.toString(normal.z));
                writer.endNode();
            }
        writer.endNode();

        writer.startNode("TransducerBody");
        RenderList renderList = transducer.getRenderList();
        if (renderList != null && renderList.getSize() > 0) {
            
            writer.startNode("Transform");
                mc.convertAnother(transducer.getTransducerBodyModelTransform());
            writer.endNode();
            
            writer.startNode("Meshes");
            writer.addAttribute("count", Integer.toString(renderList.getSize()));
            
            Iterator<Renderable> i = renderList.getChildIterator();
            int c = 0;
            while(i.hasNext()) {
                Renderable r = i.next();
                if (r instanceof PlyFileReader) {
                    writer.startNode("Mesh");
                    
                        PlyFileReader plyObj = (PlyFileReader)r;
                        writer.addAttribute("num", Integer.toString(meshDataChannelRefCount));
                        
                        writer.addAttribute("vertexCount", Integer.toString(plyObj.getVertexCount()));
                        writer.addAttribute("faceCount", Integer.toString(plyObj.getFaceCount()));

                        writer.startNode("Color");
                            mc.convertAnother(plyObj.getColor());
                        writer.endNode();

                        writer.startNode("ClipColor");
                            mc.convertAnother(plyObj.getClipColor());
                        writer.endNode();

                        writer.startNode("MeshDataStream");
                            meshChannelList.add(new AbstractMap.SimpleImmutableEntry<PlyFileReader, Integer>(plyObj, c++));
                            writer.setValue("meshData/mesh" + Integer.toString(meshDataChannelRefCount++));
                        writer.endNode();
                    
                    writer.endNode();
//                    
//                    int vertCount = plyObj.getVertexCount();
//                    int faceCount = plyObj.getFaceCount();
//                    
//                    IntBuffer indexBuffer = plyObj.getFaceIndexBuffer();
//                    FloatBuffer vertBuffer = plyObj.getVertexBuffer();
//                    FloatBuffer normBuffer = plyObj.getNormalBuffer();
                }
            }
            writer.endNode();
        }
        writer.endNode();
    }
    
    public void marshalMeshData(ZipOutputStream zipOutputStream, ProgressListener listener) throws IOException {
        if (zipOutputStream != null) {
            for (int i = 0; i < getDataChannelCount(); i++) {
                
//                if (listener != null) {
//                    listener.percentDone("Saving scene", (int)(100.0f * (float)i/getDataChannelCount() * 0.9f) + 10);
//                }
                
                zipOutputStream.putNextEntry(new ZipEntry("meshData/mesh" + getDataChannel(i).getValue()));

                encodeMeshStream(getDataChannel(i).getKey(), getDataChannel(i).getValue(), zipOutputStream);
                
                zipOutputStream.closeEntry();
            }
        }
        if (listener != null) {
            listener.percentDone("Saving scene", 100);
        }
    }
    
    public void unmarshalMeshData(ZipFile modelFile, ProgressListener listener) throws IOException {
        // Now load all the image channel data from associated zip entries
        for (int i = 0; i < getDataChannelCount(); i++) {
            
            if (listener != null) {
//                listener.percentDone("Loading scene", (int)(100.0f * (float)i/getDataChannelCount() * 0.9f) + 10);
            }
            
            ZipEntry imageDataEntry = modelFile.getEntry("meshData/mesh" + getDataChannel(i).getValue());
            InputStream imageDataStream = modelFile.getInputStream(imageDataEntry);
            
            PlyFileReader mesh = getDataChannel(i).getKey();
            Integer theChannel = getDataChannel(i).getValue();
            
            decodeMeshStream(mesh, theChannel, imageDataStream);
            
            imageDataStream.close();            
        }
        if (listener != null) {
//            listener.percentDone("Loading scene", 100);
        }
    }
    
    private void encodeMeshStream(PlyFileReader plyObj, int channel, OutputStream ostream) throws IOException {
        
//        System.out.println("Encoding mesh " + channel);
                
        ByteBuffer vertexCount = BufferUtils.createByteBuffer(4).order(ByteOrder.LITTLE_ENDIAN);
        vertexCount.asIntBuffer().put(plyObj.getVertexCount());
//        vertexCount.flip();
        
        ByteBuffer faceCount = BufferUtils.createByteBuffer(4).order(ByteOrder.LITTLE_ENDIAN);
        faceCount.asIntBuffer().put(plyObj.getFaceCount());
//        faceCount.flip();
        
        WritableByteChannel writechannel = Channels.newChannel(ostream);
        writechannel.write(vertexCount);
        writechannel.write(faceCount);
        writechannel.write(plyObj.getVertexBuffer().order(ByteOrder.LITTLE_ENDIAN));
        writechannel.write(plyObj.getNormalBuffer().order(ByteOrder.LITTLE_ENDIAN));
        writechannel.write(plyObj.getFaceIndexBuffer().order(ByteOrder.LITTLE_ENDIAN));
 
    }
    
    private void decodeMeshStream(PlyFileReader plyObj, int channel, InputStream istream) throws IOException {
        ReadableByteChannel readchannel = Channels.newChannel(istream);
        
        ByteBuffer intBuffer = BufferUtils.createByteBuffer(4);
        intBuffer.order(ByteOrder.LITTLE_ENDIAN);
        
        readchannel.read(intBuffer); // vertex count
        intBuffer.flip();
        int vertCount = intBuffer.asIntBuffer().get();
        intBuffer.rewind();
        
        readchannel.read(intBuffer); // face count
        intBuffer.flip();
        int faceCount = intBuffer.asIntBuffer().get();
        
        int vertsize = vertCount * 4 * 3;
        int normsize = vertCount * 4 * 3;
        int indexsize = faceCount * 4 * 3;
        
        ByteBuffer vertBuffer = ByteBuffer.allocateDirect(vertsize).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer normBuffer = ByteBuffer.allocateDirect(normsize).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer indexBuffer = ByteBuffer.allocateDirect(indexsize).order(ByteOrder.LITTLE_ENDIAN);
                        
        readchannel.read(vertBuffer);
        readchannel.read(normBuffer);
        readchannel.read(indexBuffer);
        
        vertBuffer.flip();
        normBuffer.flip();
        indexBuffer.flip();
        
        plyObj.setVertexCount(vertCount);
        plyObj.setFaceCount(faceCount);
        plyObj.setMeshData(indexBuffer.asIntBuffer(), vertBuffer.asFloatBuffer(), normBuffer.asFloatBuffer());
    }
    
    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
        Transducer  transducer = new Transducer();
        
        String name = reader.getAttribute("name");    
        
        while (reader.hasMoreChildren()) {
            reader.moveDown();

            String nodeName = reader.getNodeName();

            switch (nodeName) {
                case "Channels":
                    int channelCount = Integer.parseInt(reader.getAttribute("count"));
                    transducer.allocateNew(name, channelCount);
                    while (reader.hasMoreChildren()) {
                        reader.moveDown();
                        if (reader.getNodeName().equalsIgnoreCase("Channel")) {
                            try {
                                Vector4f channelLoc = new Vector4f();
                                Vector3f channelNorm = new Vector3f();
                                int num = Integer.parseInt(reader.getAttribute("num"));
                                channelLoc.w = Float.parseFloat(reader.getAttribute("area"));
                                channelLoc.x = Float.parseFloat(reader.getAttribute("x"));
                                channelLoc.y = Float.parseFloat(reader.getAttribute("y"));
                                channelLoc.z = Float.parseFloat(reader.getAttribute("z"));
                                channelNorm.x = Float.parseFloat(reader.getAttribute("nx"));
                                channelNorm.y = Float.parseFloat(reader.getAttribute("ny"));
                                channelNorm.z = Float.parseFloat(reader.getAttribute("nz"));

//                                System.out.println("num=" + num + " " + channelLoc);
                                transducer.setElement(num, channelLoc);
                                transducer.setElementNormal(num, channelNorm.x, channelNorm.y, channelNorm.z);
                                transducer.setElementActive(num, true);
                            } catch (NumberFormatException e) {
                                throw new ConversionException("Required attributes missing or malformed.");
                            }
                        }
                        reader.moveUp();
                    }
                    break;
                case "TransducerBody":
                    while (reader.hasMoreChildren()) {
                        reader.moveDown();
                        String transducerBodyNodeName = reader.getNodeName();
                        switch (transducerBodyNodeName) {
                            case "Transform":
                                Matrix4f transform = (Matrix4f) uc.convertAnother(transducer, Matrix4f.class);
                                transducer.setTransducerBodyModelTransform(transform);
//                                System.out.println(transform);
                                break;
                            case "Meshes":
                                int meshCount = Integer.parseInt(reader.getAttribute("count"));
//                                System.out.println("mesh count = " + meshCount);
                                while (reader.hasMoreChildren()) {
                                    reader.moveDown();
                                    if (reader.getNodeName().equalsIgnoreCase("Mesh")) {
                                        int meshNum = 0;
                                        int vertexCount = 0;
                                        int faceCount = 0;
                                        Vector4f color = null, clipColor = null;
                                        String dataStreamName = "";

                                        try {
                                            meshNum = Integer.parseInt(reader.getAttribute("num"));
                                            vertexCount = Integer.parseInt(reader.getAttribute("vertexCount"));
                                            faceCount = Integer.parseInt(reader.getAttribute("faceCount"));
                                        } catch (NumberFormatException e) {
                                            throw new ConversionException("Required attributes missing or malformed.");
                                        }

                                        while (reader.hasMoreChildren()) {
                                            reader.moveDown();
                                            switch (reader.getNodeName()) {
                                                case "Color":
                                                    color = (Vector4f) uc.convertAnother(transducer, Vector4f.class);
                                                    break;
                                                case "ClipColor":
                                                    clipColor = (Vector4f) uc.convertAnother(transducer, Vector4f.class);
                                                    break;
                                                case "MeshDataStream":
                                                    dataStreamName = reader.getValue();
                                                    break;
                                            }
                                            reader.moveUp();
                                        }
                                        
//                                        System.out.println("mesh " + dataStreamName + " color=" + color);
                                        PlyFileReader mesh = new PlyFileReader(vertexCount, faceCount);
                                        mesh.setColor(color.x, color.y, color.z, color.w);
                                        mesh.setClipColor(clipColor.x, clipColor.y, clipColor.z, clipColor.w);
                                        meshChannelList.add(new AbstractMap.SimpleImmutableEntry<PlyFileReader, Integer>(mesh, meshNum));
                                        transducer.addTransducerBodyModelMesh(mesh);
                                    }
                                    reader.moveUp();
                                }
                                break;
                        }
                        reader.moveUp();
                    }
                    break;
            }
            reader.moveUp();
        }
        
        return transducer;
    }

    @Override
    public boolean canConvert(Class type) {
        return Transducer.class == type;
    }
    
}
