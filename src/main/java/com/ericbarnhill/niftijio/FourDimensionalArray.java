package com.ericbarnhill.niftijio;

/* Four-dimensional array implementation that avoids using java's multi-dimensional arrays.
        * <p/>
        * For very large images, java's multi-dimensional arrays cause too much overhead and eventually
        * result in either heap or garbage collection issues. This implementation uses one single large array
        * while providing a 4D access interface.
        *
        * Method names should be self-explanatory.
        * @author Ghazi Bouabene, University of Basel, Switzerland
        *
        *
 */
public class FourDimensionalArray {

    private double[] data;
    private int nx, ny, nz, dim;

    public FourDimensionalArray(int nx, int ny, int nz, int dim) {
        this.nx = nx;
        this.ny = ny;
        this.nz = nz;
        this.dim = dim;
        this.data = new double[nx * ny * nz * dim];
    }

    public FourDimensionalArray(double[][][][] array) {
        this.nx = array.length;
        this.ny = array[0].length;
        this.nz = array[0][0].length;
        this.dim = array[0][0][0].length;

        this.data = new double[nx * ny * nz * dim];

        for (int d = 0; d < dim; d++)
            for (int k = 0; k < nz; k++)
                for (int j = 0; j < ny; j++)
                    for (int i = 0; i < nx; i++) {
                        set(i,j,k,d, array[i][j][k][d]);
                    }
    }

    public double get(int x, int y, int z, int d) {
        int idx = d * (nx * ny * nz) + z * (nx * ny) + y * nx + x;
        return data[idx];
    }

    public void set(int x, int y, int z, int d, double val) {
        int idx = d * (nx * ny * nz) + z * (nx * ny) + y * nx + x;
        data[idx] = val;
    }

    public double[][][][] toArray() {
        double[][][][] array = new double[nx][ny][nz][dim];
        for (int d = 0; d < dim; d++)
            for (int k = 0; k < nz; k++)
                for (int j = 0; j < ny; j++)
                    for (int i = 0; i < nx; i++) {
                        array[i][j][k][d] = get(i,j,k,d);
                    }
        return array;
    }

    public int sizeX() {return nx;}
    public int sizeY() {return ny;}
    public int sizeZ() {return nz;}
    public int dimension() {return dim;}
}
