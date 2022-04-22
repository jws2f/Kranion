package com.ericbarnhill.niftijio;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class NiftiVolume
{
    public NiftiHeader header;
    public FourDimensionalArray data;

    public NiftiVolume(int nx, int ny, int nz, int dim)
    {
        this.header = new NiftiHeader(nx, ny, nz, dim);
        this.data = new FourDimensionalArray(nx,ny,nz,dim);
    }

    public NiftiVolume(NiftiHeader hdr)
    {
        this.header = hdr;

        int nx = 0;
        if (hdr.datatype == NiftiHeader.NIFTI_TYPE_COMPLEX64) {
            nx = hdr.dim[1]*2;
        } else {
            nx = hdr.dim[1];
        }
        int ny = hdr.dim[2];
        int nz = hdr.dim[3];
        int dim = hdr.dim[4];

        if (hdr.dim[0] == 2)
            nz = 1;
        if (dim == 0)
            dim = 1;

        this.data = new FourDimensionalArray(nx,ny,nz,dim);
    }

    public NiftiVolume(double[][][][] data)
    {
        this.data = new FourDimensionalArray(data);
        final int nx = data.length;
        final int ny = data[0].length;
        final int nz = data[0][0].length;
        final int dim = data[0][0][0].length;
        this.header = new NiftiHeader(nx, ny, nz, dim);
    }

    public static NiftiVolume read(String filename) throws IOException {
        NiftiHeader hdr = NiftiHeader.read(filename);

        InputStream is = new FileInputStream(hdr.filename);
        if (hdr.filename.endsWith(".gz"))
            is = new GZIPInputStream(is);
        try {
            return read(new BufferedInputStream(is), hdr);
        } finally {
            is.close();
        }
    }

    /** Read the NIFTI volume from a NIFTI input stream.
     * 
     * @param is an input stream pointing to the beginning of the NIFTI file, uncompressed.
     * @return a NIFTI volume
     * @throws IOException 
     */
    public static NiftiVolume read(InputStream is) throws IOException {
        return read(is, null);
    }

    /** Read the NIFTI volume from a NIFTI input stream.
     * 
     * @param is an input stream pointing to the beginning of the NIFTI file, uncompressed. The operation will close the stream.
     * @param filename the name of the original file, can be null
     * @return a NIFTI volume
     * @throws IOException 
     */
    public static NiftiVolume read(InputStream is, String filename) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(is);
        try {
            bis.mark(2048);
            NiftiHeader hdr = NiftiHeader.read(bis, filename);
            bis.reset();
            return read(bis, hdr);
        } finally {
            bis.close();
        }
    }

    private static NiftiVolume read(BufferedInputStream is, NiftiHeader hdr) throws IOException {
        // skip header
        is.skip((long) hdr.vox_offset);
        int nx = 0;
        if (hdr.datatype == NiftiHeader.NIFTI_TYPE_COMPLEX64) {
            nx = hdr.dim[1]*2; // COMPLEX CASE DOUBLES X
        } else {
            nx = hdr.dim[1];
        }
        int ny = hdr.dim[2];
        int nz = hdr.dim[3];
        int dim = hdr.dim[4];

        if (hdr.dim[0] == 2)
            nz = 1;
        if (dim == 0)
            dim = 1;

        NiftiVolume out = new NiftiVolume(hdr);
        DataInput di = hdr.little_endian ? new LEDataInputStream(is) : new DataInputStream(is);

        double v;

        for (int d = 0; d < dim; d++)
        {
            for (int k = 0; k < nz; k++)
                for (int j = 0; j < ny; j++)
                        for (int i = 0; i < nx; i++)
                        {
                            switch (hdr.datatype)
                            {
                            case NiftiHeader.NIFTI_TYPE_INT8:
                            case NiftiHeader.NIFTI_TYPE_UINT8:
                                v = di.readByte();

                                if ((hdr.datatype == NiftiHeader.NIFTI_TYPE_UINT8) && v < 0)
                                    v = v + 256d;
                                if (hdr.scl_slope != 0)
                                    v = v * hdr.scl_slope + hdr.scl_inter;
                                break;
                            case NiftiHeader.NIFTI_TYPE_INT16:
                            case NiftiHeader.NIFTI_TYPE_UINT16:
                                v = (double) (di.readShort());

                                if ((hdr.datatype == NiftiHeader.NIFTI_TYPE_UINT16) && (v < 0))
                                    v = Math.abs(v) + (double) (1 << 15);
                                if (hdr.scl_slope != 0)
                                    v = v * hdr.scl_slope + hdr.scl_inter;
                                break;
                            case NiftiHeader.NIFTI_TYPE_INT32:
                            case NiftiHeader.NIFTI_TYPE_UINT32:
                                v = (double) (di.readInt());
                                if ((hdr.datatype == NiftiHeader.NIFTI_TYPE_UINT32) && (v < 0))
                                    v = Math.abs(v) + (double) (1 << 31);
                                if (hdr.scl_slope != 0)
                                    v = v * hdr.scl_slope + hdr.scl_inter;
                                break;
                            case NiftiHeader.NIFTI_TYPE_INT64:
                            case NiftiHeader.NIFTI_TYPE_UINT64:
                                v = (double) (di.readLong());
                                if ((hdr.datatype == NiftiHeader.NIFTI_TYPE_UINT64) && (v < 0))
                                    v = Math.abs(v) + (double) (1 << 63);
                                if (hdr.scl_slope != 0)
                                    v = v * hdr.scl_slope + hdr.scl_inter;
                                break;
                            case NiftiHeader.NIFTI_TYPE_FLOAT32:
                                v = (double) (di.readFloat());
                                if (hdr.scl_slope != 0)
                                    v = v * hdr.scl_slope + hdr.scl_inter;
                                break;
                            case NiftiHeader.NIFTI_TYPE_FLOAT64:
                                v = (double) (di.readDouble());
                                if (hdr.scl_slope != 0)
                                    v = v * hdr.scl_slope + hdr.scl_inter;
                                break;
                            case NiftiHeader.NIFTI_TYPE_COMPLEX64:
                                v = (double) (di.readFloat());
                                if (hdr.scl_slope != 0)
                                    v = v * hdr.scl_slope + hdr.scl_inter;
                                break;
                            case NiftiHeader.DT_NONE:
                            case NiftiHeader.DT_BINARY:
                            case NiftiHeader.NIFTI_TYPE_FLOAT128:
                            case NiftiHeader.NIFTI_TYPE_RGB24:
                            case NiftiHeader.NIFTI_TYPE_COMPLEX128:
                            case NiftiHeader.NIFTI_TYPE_COMPLEX256:
                            case NiftiHeader.DT_ALL:
                            default:
                            throw new IOException("Sorry, cannot yet read nifti-1 datatype " + NiftiHeader.decodeDatatype(hdr.datatype));
                        }

                            out.data.set(i,j,k,d,v);
                    }
        }

        return out;
    }

    public void write(String filename) throws IOException
    {
        NiftiHeader hdr = this.header;
        hdr.filename = filename;

        int dim = hdr.dim[4] > 0 ? hdr.dim[4] : 1;
        int nz = hdr.dim[3] > 0 ? hdr.dim[3] : 1;
        int ny = hdr.dim[2];
        int nx = hdr.dim[1];
        if (hdr.datatype == NiftiHeader.NIFTI_TYPE_COMPLEX64) {
            nx = nx*2;
        } 
        OutputStream os = new BufferedOutputStream(new FileOutputStream(hdr.filename));
        if (hdr.filename.endsWith(".gz"))
            os = new BufferedOutputStream(new GZIPOutputStream(os));

        DataOutput dout = (hdr.little_endian) ? new LEDataOutputStream(os) : new DataOutputStream(os);

        byte[] hbytes = hdr.encodeHeader();
        dout.write(hbytes);

        int nextra = (int) hdr.vox_offset - hbytes.length;
        byte[] extra = new byte[nextra];
        dout.write(extra);
        for (int d = 0; d < dim; d++)
            for (int k = 0; k < nz; k++)
                for (int j = 0; j < ny; j++)
                    for (int i = 0; i < nx; i++)
                    {
                        double v = this.data.get(i,j,k,d);

                        switch (hdr.datatype)
                        {
                        case NiftiHeader.NIFTI_TYPE_INT8:
                        case NiftiHeader.NIFTI_TYPE_UINT8:
                            if (hdr.scl_slope == 0)
                                dout.writeByte((int) v);
                            else
                                dout.writeByte((int) ((v - hdr.scl_inter) / hdr.scl_slope));
                            break;
                        case NiftiHeader.NIFTI_TYPE_INT16:
                        case NiftiHeader.NIFTI_TYPE_UINT16:
                            if (hdr.scl_slope == 0)
                                dout.writeShort((short) (v));
                            else
                                dout.writeShort((short) ((v - hdr.scl_inter) / hdr.scl_slope));
                            break;
                        case NiftiHeader.NIFTI_TYPE_INT32:
                        case NiftiHeader.NIFTI_TYPE_UINT32:
                            if (hdr.scl_slope == 0)
                                dout.writeInt((int) (v));
                            else
                                dout.writeInt((int) ((v - hdr.scl_inter) / hdr.scl_slope));
                            break;
                        case NiftiHeader.NIFTI_TYPE_INT64:
                        case NiftiHeader.NIFTI_TYPE_UINT64:
                            if (hdr.scl_slope == 0)
                                dout.writeLong((long) Math.rint(v));
                            else
                                dout.writeLong((long) Math.rint((v - hdr.scl_inter) / hdr.scl_slope));
                            break;
                        case NiftiHeader.NIFTI_TYPE_FLOAT32:
                            if (hdr.scl_slope == 0)
                                dout.writeFloat((float) (v));
                            else
                                dout.writeFloat((float) ((v - hdr.scl_inter) / hdr.scl_slope));
                            break;
                        case NiftiHeader.NIFTI_TYPE_FLOAT64:
                            if (hdr.scl_slope == 0)
                                dout.writeDouble(v);
                            else
                                dout.writeDouble((v - hdr.scl_inter) / hdr.scl_slope);
                            break;
                        case NiftiHeader.DT_NONE:
                        case NiftiHeader.DT_BINARY:
                        case NiftiHeader.NIFTI_TYPE_COMPLEX64:
                            if (hdr.scl_slope == 0)
                                dout.writeFloat((float)v);
                            else
                                dout.writeFloat((float) ((v - hdr.scl_inter) / hdr.scl_slope));
                            break;
                        case NiftiHeader.NIFTI_TYPE_FLOAT128:
                        case NiftiHeader.NIFTI_TYPE_RGB24:
                        case NiftiHeader.NIFTI_TYPE_COMPLEX128:
                        case NiftiHeader.NIFTI_TYPE_COMPLEX256:
                        case NiftiHeader.DT_ALL:
                        default:
                            throw new IOException("Sorry, cannot yet write nifti-1 datatype " + NiftiHeader.decodeDatatype(hdr.datatype));

                        }
                    }

        if (hdr.little_endian)
            ((LEDataOutputStream) dout).close();
        else
            ((DataOutputStream) dout).close();

        return;
    }
}
