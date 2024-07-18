package edu.gatech.ce.allgather;

import static edu.gatech.ce.allgather.utils.CalibrationUtilsKt.getZTransformationMatrix;
import static edu.gatech.ce.allgather.utils.CalibrationUtilsKt.getZVectFromCalib;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.ejml.simple.SimpleMatrix;
import org.junit.Test;

public class CalibrationUtilsTest {
    private SimpleMatrix getRotationMatrix(double angle, double[] axis) {
        Vector3D tempAxis = new Vector3D(axis[0], axis[1], axis[2]);
        tempAxis = tempAxis.normalize();
        Rotation rotation = new Rotation(tempAxis, angle, RotationConvention.VECTOR_OPERATOR);

        return new SimpleMatrix(rotation.getMatrix());
    }
    
    @Test
    public void testGetZVectFromCalib() {
        SimpleMatrix target;
        double[][] input;
        SimpleMatrix localZ;

        // Test Case 1
        target = new SimpleMatrix(new double[][]{{0, 0, 1}});
        input = new double[][]{{0, 1, 1}, {0, -1, 1}};
        localZ = getZVectFromCalib(input)[0];
        assert localZ.isIdentical(target, 1e-8);

        // Test Case 2
        target = new SimpleMatrix(new double[][]{{0, 0, 1}});
        input = new double[][]{{1, 0, 1}, {-1, 0, 1}};
        localZ = getZVectFromCalib(input)[0];
        assert localZ.isIdentical(target, 1e-8);

        // Test Case 3
        target = new SimpleMatrix(new double[][]{{0, -Math.sin(Math.PI / 8), Math.cos(Math.PI / 8)}});
        input = new double[][]{{0, 0, 1}, {0, -1, 1}};
        localZ = getZVectFromCalib(input)[0];
        assert localZ.isIdentical(target, 1e-8);

        // Test Case 4
        target = new SimpleMatrix(new double[][]{{0, Math.sin(Math.PI / 8), Math.cos(Math.PI / 8)}});
        input = new double[][]{{0, 1, 1}, {0, 0, 1}};
        localZ = getZVectFromCalib(input)[0];
        assert localZ.isIdentical(target, 1e-8);
    }

    @Test
    public void testGetZTransformation() {
        SimpleMatrix expectedMatrix;
        SimpleMatrix actualMatrix;
        SimpleMatrix localZ;

        // Test case 1: Transformation for no rotation
        expectedMatrix = SimpleMatrix.identity(3);
        localZ = new SimpleMatrix(new double[][]{{0}, {0}, {1}});
        actualMatrix = getZTransformationMatrix(localZ);
        assert actualMatrix.isIdentical(expectedMatrix, 1e-8);

        // Test case 2: Transformation for -π/8 rotation around the x-axis
        expectedMatrix = getRotationMatrix(-Math.PI / 8, new double[]{1, 0, 0});
        localZ = new SimpleMatrix(new double[][]{{0}, {-Math.sin(Math.PI / 8)}, {Math.cos(Math.PI / 8)}});
        actualMatrix = getZTransformationMatrix(localZ);
        assert actualMatrix.isIdentical(expectedMatrix, 1e-8);

        // Test case 3: Transformation for π/8 rotation around the x-axis
        expectedMatrix = getRotationMatrix(Math.PI / 8, new double[]{1, 0, 0});
        localZ = new SimpleMatrix(new double[][]{{0}, {Math.sin(Math.PI / 8)}, {Math.cos(Math.PI / 8)}});
        actualMatrix = getZTransformationMatrix(localZ);
        assert actualMatrix.isIdentical(expectedMatrix, 1e-8);
    }
}
