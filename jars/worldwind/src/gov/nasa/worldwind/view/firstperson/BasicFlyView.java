/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.view.firstperson;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.view.*;

import javax.media.opengl.GL;

/**
 * @author jym
 * @version $Id: BasicFlyView.java 12632 2009-09-22 02:33:48Z jterhorst $
 */
public class BasicFlyView extends BasicView
{
    protected final static double DEFAULT_MIN_ELEVATION = 500;
    protected final static double DEFAULT_MAX_ELEVATION = 4000000;
    protected final static Angle DEFAULT_MIN_PITCH = Angle.ZERO;
    protected final static Angle DEFAULT_MAX_PITCH = Angle.fromDegrees(120);
    


    public BasicFlyView()
    {
        this.viewInputHandler = new FlyViewInputHandler();
        
        this.viewLimits = new FlyViewLimits();
        this.viewLimits.setPitchLimits(DEFAULT_MIN_PITCH, DEFAULT_MAX_PITCH);
        this.viewLimits.setEyeElevationLimits(DEFAULT_MIN_ELEVATION, DEFAULT_MAX_ELEVATION);
        loadConfigurationValues();
    }

    private void loadConfigurationValues()
    {
        Double initLat = Configuration.getDoubleValue(AVKey.INITIAL_LATITUDE);
        Double initLon = Configuration.getDoubleValue(AVKey.INITIAL_LONGITUDE);
        double initElev = 50000.0; //this.eyePosition.getElevation();
        // Set center latitude and longitude. Do not change center elevation.
        Double initAltitude = Configuration.getDoubleValue(AVKey.INITIAL_ALTITUDE);
        if (initAltitude != null)
            initElev = initAltitude;
        if (initLat != null && initLon != null)
        {
            initElev = ((FlyViewLimits) viewLimits).limitEyeElevation(initElev);
            
            setEyePosition(Position.fromDegrees(initLat, initLon, initElev));
        }
        // Set only center latitude. Do not change center longitude or center elevation.
        else if (initLat != null)
            setEyePosition(Position.fromDegrees(initLat, this.eyePosition.getLongitude().degrees, initElev));
        // Set only center longitude. Do not center latitude or center elevation.
        else if (initLon != null)
            setEyePosition(Position.fromDegrees(this.eyePosition.getLatitude().degrees, initLon, initElev));

        Double initHeading = Configuration.getDoubleValue(AVKey.INITIAL_HEADING);
        if (initHeading != null)
            setHeading(Angle.fromDegrees(initHeading));

        Double initPitch = Configuration.getDoubleValue(AVKey.INITIAL_PITCH);
        if (initPitch != null)
            setPitch(Angle.fromDegrees(initPitch));

        Double initFov = Configuration.getDoubleValue(AVKey.FOV);
        if (initFov != null)
            setFieldOfView(Angle.fromDegrees(initFov));
    }

 
    public void setEyePosition(Position eyePosition)
    {
        if (eyePosition == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.getGlobe() != null)
        {
            double elevation = ((FlyViewLimits) this.viewLimits).limitEyeElevation(
                eyePosition, this.getGlobe());
            LatLon location = BasicViewPropertyLimits.limitEyePositionLocation(
                eyePosition.getLatitude(), eyePosition.getLongitude(),  this.viewLimits);
            this.eyePosition = new Position(location, elevation);
        }
        else
        {
            LatLon location = BasicViewPropertyLimits.limitEyePositionLocation(
                eyePosition.getLatitude(), eyePosition.getLongitude(), this.viewLimits);
            this.eyePosition = new Position(location, eyePosition.getElevation());
            this.eyePosition = eyePosition;
        }
    }

    public Matrix getModelViewMatrix(Position eyePosition, Position centerPosition)
    {
        if (eyePosition == null || centerPosition == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (this.globe == null)
        {
            String message = Logging.getMessage("nullValue.DrawingContextGlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        Vec4 newEyePoint = this.globe.computePointFromPosition(eyePosition);
        Vec4 newCenterPoint = this.globe.computePointFromPosition(centerPosition);
        if (newEyePoint == null || newCenterPoint == null)
        {
            String message = Logging.getMessage("View.ErrorSettingOrientation", eyePosition, centerPosition);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // If eye lat/lon != center lat/lon, then the surface normal at the center point will be a good value
        // for the up direction.
        Vec4 up = this.globe.computeSurfaceNormalAtPoint(newCenterPoint);
        // Otherwise, estimate the up direction by using the *current* heading with the new center position.
        Vec4 forward = newCenterPoint.subtract3(newEyePoint).normalize3();
        if (forward.cross3(up).getLength3() < 0.001)
        {
            Matrix modelview = ViewUtil.computeTransformMatrix(
                this.globe, eyePosition, this.heading, Angle.ZERO, Angle.ZERO);
            if (modelview != null)
            {
                Matrix modelviewInv = modelview.getInverse();
                if (modelviewInv != null)
                {
                    up = Vec4.UNIT_Y.transformBy4(modelviewInv);
                }
            }
        }

        if (up == null)
        {
            String message = Logging.getMessage("View.ErrorSettingOrientation", eyePosition, centerPosition);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Matrix modelViewMatrix = ViewUtil.computeModelViewMatrix(
            this.globe, newEyePoint, newCenterPoint, up);
        return(modelViewMatrix);
    }

    public ViewUtil.ViewState getViewState(Position eyePosition, Position centerPosition)
    {
        if (eyePosition == null || centerPosition == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (this.globe == null)
        {
            String message = Logging.getMessage("nullValue.DrawingContextGlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        Vec4 newEyePoint = this.globe.computePointFromPosition(eyePosition);
        Vec4 newCenterPoint = this.globe.computePointFromPosition(centerPosition);
        if (newEyePoint == null || newCenterPoint == null)
        {
            String message = Logging.getMessage("View.ErrorSettingOrientation", eyePosition, centerPosition);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // If eye lat/lon != center lat/lon, then the surface normal at the center point will be a good value
        // for the up direction.
        Vec4 up = this.globe.computeSurfaceNormalAtPoint(newCenterPoint);
        // Otherwise, estimate the up direction by using the *current* heading with the new center position.
        Vec4 forward = newCenterPoint.subtract3(newEyePoint).normalize3();
        if (forward.cross3(up).getLength3() < 0.001)
        {
            Matrix modelview = ViewUtil.computeTransformMatrix(
                this.globe, eyePosition, this.heading, Angle.ZERO, Angle.ZERO);
            if (modelview != null)
            {
                Matrix modelviewInv = modelview.getInverse();
                if (modelviewInv != null)
                {
                    up = Vec4.UNIT_Y.transformBy4(modelviewInv);
                }
            }
        }

        if (up == null)
        {
            String message = Logging.getMessage("View.ErrorSettingOrientation", eyePosition, centerPosition);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        ViewUtil.ViewState viewState = ViewUtil.computeViewState(
            this.globe, newEyePoint, newCenterPoint, up);
        
        return(viewState);

    }

    protected void doApply(DrawContext dc)
    {

        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (dc.getGL() == null)
        {
            String message = Logging.getMessage("nullValue.DrawingContextGLIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (dc.getGlobe() == null)
        {
            String message = Logging.getMessage("nullValue.DrawingContextGlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        // Update DrawContext and Globe references.
        this.dc = dc;
        this.globe = this.dc.getGlobe();

        //========== modelview matrix state ==========//
        // Compute the current modelview matrix.
        this.modelview = ViewUtil.computeTransformMatrix(this.globe, this.eyePosition,
                this.heading, this.pitch, this.roll);
        if (this.modelview == null)
            this.modelview = Matrix.IDENTITY;
        // Compute the current inverse-modelview matrix.
        this.modelviewInv = this.modelview.getInverse();
        if (this.modelviewInv == null)
            this.modelviewInv = Matrix.IDENTITY;

        //========== projection matrix state ==========//
        // Get the current OpenGL viewport state.
        int[] viewportArray = new int[4];
        this.dc.getGL().glGetIntegerv(GL.GL_VIEWPORT, viewportArray, 0);
        this.viewport = new java.awt.Rectangle(viewportArray[0], viewportArray[1], viewportArray[2], viewportArray[3]);
        // Compute the current clip plane distances.
        this.nearClipDistance = this.computeNearClipDistance();
        this.farClipDistance = this.computeFarClipDistance();
        // Compute the current viewport dimensions.
        double viewportWidth = this.viewport.getWidth() <= 0.0 ? 1.0 : this.viewport.getWidth();
        double viewportHeight = this.viewport.getHeight() <= 0.0 ? 1.0 : this.viewport.getHeight();
        // Compute the current projection matrix.
        this.projection = Matrix.fromPerspective(this.fieldOfView,
            viewportWidth, viewportHeight,
            this.nearClipDistance, this.farClipDistance);
        // Compute the current frustum.
        this.frustum = Frustum.fromPerspective(this.fieldOfView,
            (int) viewportWidth, (int) viewportHeight,
            this.nearClipDistance, this.farClipDistance);

        //========== load GL matrix state ==========//
        loadGLViewState(dc, this.modelview, this.projection);

        //========== after apply (GL matrix state) ==========//
        afterDoApply();
    }

    protected void afterDoApply()
    {
        // Clear cached computations.
        this.lastEyePosition = null;
        this.lastEyePoint = null;
        this.lastUpVector = null;
        this.lastForwardVector = null;
        this.lastFrustumInModelCoords = null;
    }

    protected void setViewState(ViewUtil.ViewState viewState)
    {
        if (viewState != null)
        {
            if (viewState.getPosition() != null)
            {
                Position eyePos = ViewUtil.normalizedEyePosition(viewState.getPosition());
                LatLon limitedLocation = BasicViewPropertyLimits.limitEyePositionLocation(this.eyePosition.getLatitude(),
                    this.eyePosition.getLongitude(), this.getViewPropertyLimits());
                this.eyePosition = new Position(limitedLocation, eyePos.getElevation());
            }
            if (viewState.getHeading() != null)
            {
                this.heading = ViewUtil.normalizedHeading(viewState.getHeading());
                this.heading = BasicViewPropertyLimits.limitHeading(this.heading, this.getViewPropertyLimits());
            }
            if (viewState.getPitch() != null)
            {
                this.pitch = ViewUtil.normalizedPitch(viewState.getPitch());
                this.pitch = BasicViewPropertyLimits.limitPitch(this.pitch, this.getViewPropertyLimits());
            }
            if (viewState.getRoll() != null)
            {
                this.roll = ViewUtil.normalizedRoll(viewState.getRoll());
                this.pitch = BasicViewPropertyLimits.limitPitch(this.pitch, this.getViewPropertyLimits());
            }
        }
        else
        {
            String message = Logging.getMessage("nullValue.ViewStateIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
    }

    public void setHeading(Angle heading)
    {
        if (heading == null)
        {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.heading = ViewUtil.normalizedHeading(heading);
        this.heading = BasicViewPropertyLimits.limitHeading(this.heading, this.getViewPropertyLimits());

    }

    public void setPitch(Angle pitch)
    {
        if (pitch == null)
        {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.pitch = ViewUtil.normalizedPitch(pitch);
        this.pitch = BasicViewPropertyLimits.limitPitch(this.pitch, this.getViewPropertyLimits());
    }

    public void setViewPropertyLimits(ViewPropertyLimits limits)
    {
        this.viewLimits = limits;
        this.setViewState(new ViewUtil.ViewState(this.getEyePosition(), 
            this.getHeading(), this.getPitch(), Angle.ZERO));
    }
}
