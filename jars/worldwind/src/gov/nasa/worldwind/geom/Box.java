/*
Copyright (C) 2001, 2010 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.geom;

import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.GL;

/**
 * An arbitralily oriented box, typically used as a oriented bounding volume for a collection of points or shapes. A
 * <code>Box</code> is defined by three orthogonal axes and two positions along each of those axes. Each of the
 * positions specifies the location of a box side along the respective axis. The three axes are named by convention "R",
 * "S" and "T", and are ordered by decreasing length -- R is the longest axis, followed by S and then T.
 *
 * @author tag
 * @version $Id: Box.java 13353 2010-04-30 06:00:58Z tgaskins $
 */
public class Box implements Extent, Renderable
{
    public Vec4 bottomCenter; // point at center of box's longest axis
    public Vec4 topCenter; // point at center of box's longest axis
    protected final Vec4 center; // center of box
    protected final Vec4 r; // longest axis
    protected final Vec4 s; // next longest axis
    protected final Vec4 t; // shortest axis
    protected final Vec4 ru; // r axis unit normal
    protected final Vec4 su; // s axis unit normal
    protected final Vec4 tu; // t axis unit normal
    protected final double rLength; // length of r axis
    protected final double sLength; // length of s axis
    protected final double tLength; // length of t axis
    protected final Plane[] planes; // the six planes, with positive normals facing outwards

    /**
     * Construct a box from three specified unit axes and the locations of the box faces relative to those axes. The box
     * faces are specified by two scalar locations along each axis, each location indicating a face. The non-unit length
     * of an axis is the distance between its respective two locations. The longest side is specified first, followed by
     * the second longest side and then the shortest side.
     * <p/>
     * The axes are normally principal axes computed from a collection of points in order to form an oriented bounding
     * volume. See {@link WWMath#computePrincipalAxes(Iterable)}.
     * <p/>
     * Note: No check is made to ensure the order of the face locations.
     *
     * @param axes the unit-length axes.
     * @param rMin the location along the first axis corresponding to the left-most box side relative to the axis.
     * @param rMax the location along the first axis corresponding to the right-most box side relative to the axis.
     * @param sMin the location along the second axis corresponding to the left-most box side relative to the axis.
     * @param sMax the location along the second axis corresponding to the right-most box side relative to the axis.
     * @param tMin the location along the third axis corresponding to the left-most box side relative to the axis.
     * @param tMax the location along the third axis corresponding to the right-most box side relative to the axis.
     *
     * @throws IllegalArgumentException if the axes array or one of its entries is null.
     */
    public Box(Vec4 axes[], double rMin, double rMax, double sMin, double sMax, double tMin, double tMax)
    {
        if (axes == null || axes[0] == null || axes[1] == null || axes[2] == null)
        {
            String msg = Logging.getMessage("nullValue.AxesIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.ru = axes[0];
        this.su = axes[1];
        this.tu = axes[2];

        this.r = this.ru.multiply3(rMax - rMin);
        this.s = this.su.multiply3(sMax - sMin);
        this.t = this.tu.multiply3(tMax - tMin);

        this.rLength = this.r.getLength3();
        this.sLength = this.s.getLength3();
        this.tLength = this.t.getLength3();

        // Plane normals point outwards from the box.
        this.planes = new Plane[6];
        this.planes[0] = new Plane(-this.ru.x, -this.ru.y, -this.ru.z, +rMin);
        this.planes[1] = new Plane(+this.ru.x, +this.ru.y, +this.ru.z, -rMax);
        this.planes[2] = new Plane(-this.su.x, -this.su.y, -this.su.z, +sMin);
        this.planes[3] = new Plane(+this.su.x, +this.su.y, +this.su.z, -sMax);
        this.planes[4] = new Plane(-this.tu.x, -this.tu.y, -this.tu.z, +tMin);
        this.planes[5] = new Plane(+this.tu.x, +this.tu.y, +this.tu.z, -tMax);

        double a = 0.5 * (rMin + rMax);
        double b = 0.5 * (sMin + sMax);
        double c = 0.5 * (tMin + tMax);
        this.center = ru.multiply3(a).add3(su.multiply3(b)).add3(tu.multiply3(c));

        Vec4 rHalf = r.multiply3(0.5);
        this.topCenter = this.center.add3(rHalf);
        this.bottomCenter = this.center.subtract3(rHalf);
    }

    /**
     * Construct a unit-length cube centered at a specified point.
     *
     * @param point the center of the cube.
     *
     * @throws IllegalArgumentException if the point is null.
     */
    public Box(Vec4 point)
    {
        if (point == null)
        {
            String msg = Logging.getMessage("nullValue.PointIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.ru = new Vec4(1, 0, 0, 1);
        this.su = new Vec4(0, 1, 0, 1);
        this.tu = new Vec4(0, 0, 1, 1);

        this.r = this.ru;
        this.s = this.su;
        this.t = this.tu;

        this.rLength = 1;
        this.sLength = 1;
        this.tLength = 1;

        // Plane normals point outwards from the box.
        this.planes = new Plane[6];
        double d = 0.5 * point.getLength3();
        this.planes[0] = new Plane(-this.ru.x, -this.ru.y, -this.ru.z, -(d + 0.5));
        this.planes[1] = new Plane(+this.ru.x, +this.ru.y, +this.ru.z, -(d + 0.5));
        this.planes[2] = new Plane(-this.su.x, -this.su.y, -this.su.z, -(d + 0.5));
        this.planes[3] = new Plane(+this.su.x, +this.su.y, +this.su.z, -(d + 0.5));
        this.planes[4] = new Plane(-this.tu.x, -this.tu.y, -this.tu.z, -(d + 0.5));
        this.planes[5] = new Plane(+this.tu.x, +this.tu.y, +this.tu.z, -(d + 0.5));

        this.center = ru.add3(su).add3(tu).multiply3(0.5);

        Vec4 rHalf = r.multiply3(0.5);
        this.topCenter = this.center.add3(rHalf);
        this.bottomCenter = this.center.subtract3(rHalf);
    }

    /**
     * Returns the box's center point.
     *
     * @return the box's center point.
     */
    public Vec4 getCenter()
    {
        return this.center;
    }

    /**
     * Returns the point corresponding to the center of the box side left-most along the R (first) axis.
     *
     * @return the bottom-center point.
     */
    public Vec4 getBottomCenter()
    {
        return this.bottomCenter;
    }

    /**
     * Returns the point corresponding to the center of the box side right-most along the R (first) axis.
     *
     * @return the top-center point.
     */
    public Vec4 getTopCenter()
    {
        return this.topCenter;
    }

    /**
     * Returns the R (first) axis. The axis length is the distance between the box sides perpendicular to the axis.
     *
     * @return the R axis.
     */
    public Vec4 getRAxis()
    {
        return this.r;
    }

    /**
     * Returns the S (second) axis. The axis length is the distance between the box sides perpendicular to the axis.
     *
     * @return the S axis.
     */
    public Vec4 getSAxis()
    {
        return this.s;
    }

    /**
     * Returns the T (third) axis. The axis length is the distance between the box sides perpendicular to the axis.
     *
     * @return the T axis.
     */
    public Vec4 getTAxis()
    {
        return this.t;
    }

    /**
     * Returns the R (first) axis in unit length.
     *
     * @return the unit R axis.
     */
    public Vec4 getUnitRAxis()
    {
        return this.r;
    }

    /**
     * Returns the S (second) axis in unit length.
     *
     * @return the unit S axis.
     */
    public Vec4 getUnitSAxis()
    {
        return this.s;
    }

    /**
     * Returns the T (third) axis in unit length.
     *
     * @return the unit T axis.
     */
    public Vec4 getUnitTAxis()
    {
        return this.t;
    }

    /**
     * Returns the six planes of the box. The plane normals are directed outwards from the box.
     *
     * @return the six box planes in the order R-min, R-max, S-min, S-max, T-min, T-max.
     */
    public Plane[] getPlanes()
    {
        return this.planes;
    }

    /**
     * Returns the length of the R axis.
     *
     * @return the length of the R axis.
     */
    public double getRLength()
    {
        return rLength;
    }

    /**
     * Returns the length of the S axis.
     *
     * @return the length of the S axis.
     */
    public double getSLength()
    {
        return sLength;
    }

    /**
     * Returns the length of the T axis.
     *
     * @return the length of the T axis.
     */
    public double getTLength()
    {
        return tLength;
    }

    /**
     * Returns the effective diameter of the box as if it were a sphere. The length returned is the square root of the
     * sum of the squares of axis lengths.
     *
     * @return the effetive diameter of the box.
     */
    public double getDiameter()
    {
        return Math.sqrt(this.rLength * this.rLength + this.sLength * this.sLength + this.tLength * this.tLength);
    }

    /**
     * Returns the effective radius of the box as if it were a sphere. The length returned is half the square root of
     * the sum of the squares of axis lengths.
     *
     * @return the effetive radius of the box.
     */
    public double getRadius()
    {
        return 0.5 * this.getDiameter();
    }

    /**
     * Compute a <code>Box</code> that bounds a specified list of points. Principal axes are computed for the points and
     * used to form a <code>Box</code>.
     *
     * @param points the points for which to compute a bounding volume.
     *
     * @return the bounding volume, with axes lengths consistent with the conventions described in the overview.
     *
     * @throws IllegalArgumentException if the point list is null or empty.
     */
    public static Box computeBoundingBox(Iterable<? extends Vec4> points)
    {
        if (points == null)
        {
            String msg = Logging.getMessage("nullValue.PointListIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        Vec4[] axes = WWMath.computePrincipalAxes(points);
        if (axes == null)
        {
            String msg = Logging.getMessage("generic.ListIsEmpty");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        Vec4 r = axes[0];
        Vec4 s = axes[1];
        Vec4 t = axes[2];

        // Find the extremes along each axis.
        double minDotR = Double.MAX_VALUE;
        double maxDotR = -minDotR;
        double minDotS = Double.MAX_VALUE;
        double maxDotS = -minDotS;
        double minDotT = Double.MAX_VALUE;
        double maxDotT = -minDotT;

        for (Vec4 p : points)
        {
            if (p == null)
                continue;

            double pdr = p.dot3(r);
            if (pdr < minDotR)
                minDotR = pdr;
            if (pdr > maxDotR)
                maxDotR = pdr;

            double pds = p.dot3(s);
            if (pds < minDotS)
                minDotS = pds;
            if (pds > maxDotS)
                maxDotS = pds;

            double pdt = p.dot3(t);
            if (pdt < minDotT)
                minDotT = pdt;
            if (pdt > maxDotT)
                maxDotT = pdt;
        }

        if (maxDotR == minDotR)
            maxDotR = minDotR + 1;
        if (maxDotS == minDotS)
            maxDotS = minDotS + 1;
        if (maxDotT == minDotT)
            maxDotT = minDotT + 1;

        return new Box(axes, minDotR, maxDotR, minDotS, maxDotS, minDotT, maxDotT);
    }

    /** {@inheritDoc} */
    public boolean intersects(Frustum frustum)
    {
        // FYI: this code is identical to that in Cylinder.intersects.

        if (frustum == null)
        {
            String message = Logging.getMessage("nullValue.FrustumIsNull");

            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double intersectionPoint;
        Vec4[] endPoints = new Vec4[] {this.bottomCenter, this.topCenter};

        double effectiveRadius = this.getEffectiveRadius(frustum.getNear());
        intersectionPoint = this.intersectsAt(frustum.getNear(), effectiveRadius, endPoints);
        if (intersectionPoint < 0)
            return false;

        // Near and far have the same effective radius.
        effectiveRadius = this.getEffectiveRadius(frustum.getFar());
        intersectionPoint = this.intersectsAt(frustum.getFar(), effectiveRadius, endPoints);
        if (intersectionPoint < 0)
            return false;

        effectiveRadius = this.getEffectiveRadius(frustum.getLeft());
        intersectionPoint = this.intersectsAt(frustum.getLeft(), effectiveRadius, endPoints);
        if (intersectionPoint < 0)
            return false;

        effectiveRadius = this.getEffectiveRadius(frustum.getRight());
        intersectionPoint = this.intersectsAt(frustum.getRight(), effectiveRadius, endPoints);
        if (intersectionPoint < 0)
            return false;

        effectiveRadius = this.getEffectiveRadius(frustum.getTop());
        intersectionPoint = this.intersectsAt(frustum.getTop(), effectiveRadius, endPoints);
        if (intersectionPoint < 0)
            return false;

        effectiveRadius = this.getEffectiveRadius(frustum.getBottom());
        intersectionPoint = this.intersectsAt(frustum.getBottom(), effectiveRadius, endPoints);
        return intersectionPoint >= 0;
    }

    protected double getEffectiveRadius(Plane plane)
    {
        // Determine the effective radius of the box axis relative to the plane.
        Vec4 n = plane.getNormal();
        return 0.5 * (Math.abs(this.s.dot3(n)) + Math.abs(this.t.dot3(n)));
    }

    protected double intersectsAt(Plane plane, double effectiveRadius, Vec4[] endpoints)
    {
        // Test the distance from the first end-point.
        double dq1 = plane.dot(endpoints[0]);
        boolean bq1 = dq1 <= -effectiveRadius;

        // Test the distance from the possibly reduced second end-point.
        double dq2 = plane.dot(endpoints[1]);
        boolean bq2 = dq2 <= -effectiveRadius;

        if (bq1 && bq2) // endpoints more distant from plane than effective radius; box is on neg. side of plane
            return -1;

        if (bq1 == bq2) // endpoints less distant from plane than effective radius; can't draw any conclusions
            return 0;

        // Compute and return the endpoints of the cylinder on the positive side of the plane.
        double t = (effectiveRadius + dq1) / plane.getNormal().dot3(endpoints[0].subtract3(endpoints[1]));

        Vec4 newEndPoint = endpoints[0].add3(endpoints[1].subtract3(endpoints[0]).multiply3(t));
        // truncate the line to only that in the positive halfspace (e.g., inside the frustum)
        if (bq1)
            endpoints[0] = newEndPoint;
        else
            endpoints[1] = newEndPoint;

        return t;
    }

    /** {@inheritDoc} */
    public boolean intersects(Plane plane)
    {
        if (plane == null)
        {
            String message = Logging.getMessage("nullValue.PlaneIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double effectiveRadius = this.getEffectiveRadius(plane);
        return this.intersects(plane, effectiveRadius) >= 0;
    }

    protected double intersects(Plane plane, double effectiveRadius)
    {
        // Test the distance from the first end-point.
        double dq1 = plane.dot(this.bottomCenter);
        boolean bq1 = dq1 <= -effectiveRadius;

        // Test the distance from the top of the box.
        double dq2 = plane.dot(this.topCenter);
        boolean bq2 = dq2 <= -effectiveRadius;

        if (bq1 && bq2) // both beyond effective radius; box is on negative side of plane
            return -1;

        if (bq1 == bq2) // both within effective radius; can't draw any conclusions
            return 0;

        return 1; // box almost certainly intersects
    }

    /** {@inheritDoc} */
    public Intersection[] intersect(Line line)
    {
        return WWMath.polytopeIntersect(line, this.planes);
    }

    /** {@inheritDoc} */
    public boolean intersects(Line line)
    {
        if (line == null)
        {
            String message = Logging.getMessage("nullValue.LineIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return WWMath.polytopeIntersect(line, this.planes) != null;
    }

//    public static void main(String[] args)
//    {
//        Box box = new Box(new Vec4[] {new Vec4(1, 0, 0), new Vec4(0, 1, 0), new Vec4(0, 0, 1)},
//            -.5, .5, -.5, .5, -.5, .5);
//        Line line = new Line(new Vec4(-1, 0.5, 0.5), new Vec4(1, 0, 0));
//        Intersection[] intersections = box.intersect(line);
//        if (intersections != null && intersections.length > 0 && intersections[0] != null)
//            System.out.println(intersections[0]);
//        if (intersections != null && intersections.length > 1 && intersections[1] != null)
//            System.out.println(intersections[1]);
//    }

//    /** {@inheritDoc} */
//    public Intersection[] intersect(Line line)
//    {
//        return WWMath.polytopeIntersect(line, this.planes);
//        // Algorithm from "3-D Computer Graphics" by Samuel R. Buss, 2005, Section X.1.4.
//
//        // Determine intersection with each plane and categorize the intersections as "front" if the line intersects
//        // the front side of the plane (dot product of line direction with plane normal is negative) and "back" if the
//        // line intersects the back side of the plane (dot product of line direction with plane normal is positive).
//
//        double fMax = -Double.MAX_VALUE;
//        double bMin = Double.MAX_VALUE;
//        boolean isTangent = false;
//
//        Vec4 u = line.getDirection();
//        Vec4 p = line.getOrigin();
//
//        for (Plane plane : this.planes)
//        {
//            Vec4 n = plane.getNormal();
//            double d = -plane.getDistance();
//
//            double s = u.dot3(n);
//            if (s == 0) // line is parallel to plane
//            {
//                double pdn = p.dot3(n);
//                if (pdn > d) // is line in positive halfspace (in front of) of the plane?
//                    return null; // no intersection
//                else
//                {
//                    if (pdn == d)
//                        isTangent = true; // line coincident with plane
//                    continue; // line is in negative halfspace; possible intersection; check other planes
//                }
//            }
//
//            // Determine whether front or back intersection.
//            double a = (d - p.dot3(n)) / s;
//            if (u.dot3(n) < 0) // line intersects front face and therefore entering box
//            {
//                if (a > fMax)
//                {
//                    if (a > bMin)
//                        return null;
//                    fMax = a;
//                }
//            }
//            else // line intersects back face and therefore leaving box
//            {
//                if (a < bMin)
//                {
//                    if (a < 0 || a < fMax)
//                        return null;
//                    bMin = a;
//                }
//            }
//        }
//
//        // Compute the Cartesian intersection points. There will be no more than two.
//        if (fMax >= 0) // intersects frontface and backface; point origin is outside the box
//            return new Intersection[]
//                {
//                    new Intersection(p.add3(u.multiply3(fMax)), isTangent),
//                    new Intersection(p.add3(u.multiply3(bMin)), isTangent)
//                };
//        else // intersects backface only; point origin is within the box
//            return new Intersection[] {new Intersection(p.add3(u.multiply3(bMin)), isTangent)};
//    }

    /**
     * Draws a representation of the <code>Box</code>.
     *
     * @param dc the <code>DrawContext</code> to be used.
     */
    public void render(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DocumentSourceIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (dc.isPickingMode())
            return;

        Vec4 a = this.s.add3(this.t).multiply3(-0.5);
        Vec4 b = this.s.subtract3(this.t).multiply3(0.5);
        Vec4 c = this.s.add3(this.t).multiply3(0.5);
        Vec4 d = this.t.subtract3(this.s).multiply3(0.5);

        GL gl = dc.getGL();
        OGLStackHandler ogsh = new OGLStackHandler();
        ogsh.pushAttrib(gl, GL.GL_CURRENT_BIT | GL.GL_ENABLE_BIT | GL.GL_TRANSFORM_BIT | GL.GL_DEPTH_BUFFER_BIT);
        try
        {
            gl.glLineWidth(1f);
            gl.glEnable(GL.GL_BLEND);
            OGLUtil.applyBlending(gl, false);
            gl.glEnable(GL.GL_DEPTH_TEST);

            gl.glDepthFunc(GL.GL_LEQUAL);
            gl.glColor4f(1f, 1f, 1f, 0.5f);
            this.drawBox(dc, a, b, c, d);

            gl.glDepthFunc(GL.GL_GREATER);
            gl.glColor4f(1f, 0f, 1f, 0.4f);
            this.drawBox(dc, a, b, c, d);
        }
        finally
        {
            ogsh.pop(gl);
        }
    }

    protected void drawBox(DrawContext dc, Vec4 a, Vec4 b, Vec4 c, Vec4 d)
    {
        Vec4 e = a.add3(this.r);
        Vec4 f = d.add3(this.r);
        GL gl = dc.getGL();

        dc.getView().pushReferenceCenter(dc, this.bottomCenter);
        OGLStackHandler ogsh = new OGLStackHandler();
        ogsh.pushModelview(gl);
        try
        {
            // Draw parallel lines in R direction
            int n = 20;
            Vec4 dr = this.r.multiply3(1d / (double) n);

            this.drawOutline(dc, a, b, c, d);
            for (int i = 1; i < n; i++)
            {
                gl.glTranslated(dr.x, dr.y, dr.z);
                this.drawOutline(dc, a, b, c, d);
            }

            // Draw parallel lines in S direction
            n = 20;
            Vec4 ds = this.s.multiply3(1d / (double) n);

            gl.glPopMatrix();
            gl.glPushMatrix();
            this.drawOutline(dc, a, e, f, d);
            for (int i = 1; i < n; i++)
            {
                gl.glTranslated(ds.x, ds.y, ds.z);
                this.drawOutline(dc, a, e, f, d);
            }
        }
        finally
        {
            ogsh.pop(gl);
            dc.getView().popReferenceCenter(dc);
        }
    }

    protected void drawOutline(DrawContext dc, Vec4 a, Vec4 b, Vec4 c, Vec4 d)
    {
        GL gl = dc.getGL();
        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(a.x, a.y, a.z);
        gl.glVertex3d(b.x, b.y, b.z);
        gl.glVertex3d(c.x, c.y, c.z);
        gl.glVertex3d(d.x, d.y, d.z);
        gl.glEnd();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (!(o instanceof Box))
            return false;

        Box box = (Box) o;

        if (center != null ? !center.equals(box.center) : box.center != null)
            return false;
        if (r != null ? !r.equals(box.r) : box.r != null)
            return false;
        if (s != null ? !s.equals(box.s) : box.s != null)
            return false;
        //noinspection RedundantIfStatement
        if (t != null ? !t.equals(box.t) : box.t != null)
            return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = center != null ? center.hashCode() : 0;
        result = 31 * result + (r != null ? r.hashCode() : 0);
        result = 31 * result + (s != null ? s.hashCode() : 0);
        result = 31 * result + (t != null ? t.hashCode() : 0);
        return result;
    }
}
