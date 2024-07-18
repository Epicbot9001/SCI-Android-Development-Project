package com.example.myapplication;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;

public class Transform {
    private String src;
    private String target;
    public Transform(String src, String target) {
        this.src = src;
        this.target = target;
    }

    /**
     * Convert proj4 coordinates to JTS coordinates.
     *
     * @param projCoords
     * @return
     */
    private Coordinate[] convert(ProjCoordinate[] projCoords) {
        Coordinate[] jtsCoords = new Coordinate[projCoords.length];
        for (int i = 0; i < projCoords.length; ++i) {
            jtsCoords[i] = new Coordinate(projCoords[i].x, projCoords[i].y);
        }
        return jtsCoords;
    }

    /**
     * Convert JTS coordinates to proj4j coordinates.
     *
     * @param jtsCoords
     * @return
     */
    private ProjCoordinate[] convert(Coordinate[] jtsCoords) {
        ProjCoordinate[] projCoords = new ProjCoordinate[jtsCoords.length];
        for (int i = 0; i < jtsCoords.length; ++i) {
            projCoords[i] = new ProjCoordinate(jtsCoords[i].x, jtsCoords[i].y);
        }
        return projCoords;
    }

    private ProjCoordinate[] transformCoordinates(CoordinateTransform ct, ProjCoordinate[] in) {
        ProjCoordinate[] out = new ProjCoordinate[in.length];
        for (int i = 0; i < in.length; ++i) {
            out[i] = ct.transform(in[i], new ProjCoordinate());
        }
        return out;
    }

    private Coordinate[] transformCoordinates(CoordinateTransform ct,
                                                    Coordinate[] in) {
        return convert(transformCoordinates(ct, convert(in)));
    }

    private Geometry transform(CoordinateTransform ct,
                                             Geometry point) {
        return point.getFactory().createPoint(transformCoordinates(ct, point.getCoordinates())[0]);
    }

    public Geometry transformPoint(Geometry point) {
        CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
        CRSFactory crsFactory = new CRSFactory();

        CoordinateReferenceSystem srcCrs = crsFactory.createFromParameters(null, this.src);
        CoordinateReferenceSystem tgtCrs = crsFactory.createFromParameters(null, this.target);

        CoordinateTransform coordTransform = ctFactory.createTransform(srcCrs, tgtCrs);

        return transform(coordTransform, point);
    }
}
