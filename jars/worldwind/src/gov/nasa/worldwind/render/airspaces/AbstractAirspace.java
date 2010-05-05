/*
Copyright (C) 2001, 2010 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render.airspaces;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.cache.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;

import java.util.*;

/**
 * @author dcollins
 * @version $Id: AbstractAirspace.java 13347 2010-04-27 23:50:20Z dcollins $
 */
@SuppressWarnings({"deprecation"})
public abstract class AbstractAirspace extends AVListImpl implements Airspace, Movable
{
    protected static final String ARC_SLICES = "ArcSlices";
    protected static final String DISABLE_TERRAIN_CONFORMANCE = "DisableTerrainConformance";
    protected static final String EXPIRY_TIME = "ExpiryTime";
    protected static final String GEOMETRY_CACHE_NAME = "Airspace Geometry";
    protected static final String GEOMETRY_CACHE_KEY = Geometry.class.getName();
    protected static final String GLOBE_KEY = "GlobeKey";
    protected static final String LENGTH_SLICES = "LengthSlices";
    protected static final String LOOPS = "Loops";
    protected static final String PILLARS = "Pillars";
    protected static final String SLICES = "Slices";
    protected static final String SPLIT_THRESHOLD = "SplitThreshold";
    protected static final String STACKS = "Stacks";
    protected static final String SUBDIVISIONS = "Subdivisions";
    protected static final String VERTICAL_EXAGGERATION = "VerticalExaggeration";

    private static final long DEFAULT_GEOMETRY_CACHE_SIZE = 16777216L; // 16 megabytes

    private boolean visible = true;
    private AirspaceAttributes attributes;
    private double lowerAltitude = 0.0;
    private double upperAltitude = 1.0;
    private boolean lowerTerrainConforming = false;
    private boolean upperTerrainConforming = false;
    private String lowerAltitudeDatum = AVKey.ABOVE_MEAN_SEA_LEVEL;
    private String upperAltitudeDatum = AVKey.ABOVE_MEAN_SEA_LEVEL;
    private LatLon groundReference;
    private boolean enableLevelOfDetail = true;
    private Collection<DetailLevel> detailLevels = new TreeSet<DetailLevel>();
    // Geometry computation and rendering support.
    private AirspaceRenderer renderer = new AirspaceRenderer();
    private GeometryBuilder geometryBuilder = new GeometryBuilder();
    // Extent support.
    // Geometry update support.
    private long expiryTime = -1L;
    private long minExpiryTime = 2000L;
    private long maxExpiryTime = 6000L;
    private static Random rand = new Random();
    // Elevation lookup map.
    private Map<LatLon, Double> elevationMap = new HashMap<LatLon, Double>();

    // Airspaces perform about 5% better if their extent is cached, so do that here.
    protected static class ExtentInfo
    {
        // The extent depends on the state of the globe used to compute it, and the vertical exaggeration.
        protected Extent extent;
        protected double verticalExaggeration;
        protected Object globeStateKey;

        public ExtentInfo(Extent extent, DrawContext dc)
        {
            this.extent = extent;
            this.verticalExaggeration = dc.getVerticalExaggeration();
            this.globeStateKey = dc.getGlobe().getStateKey(dc);
        }

        protected boolean isValid(DrawContext dc)
        {
            return this.verticalExaggeration == dc.getVerticalExaggeration()
                && globeStateKey.equals(dc.getGlobe().getStateKey(dc));
        }
    }

    protected HashMap<Globe, ExtentInfo> extents = new HashMap<Globe, ExtentInfo>(2); // usually only 1, but few at most

    public AbstractAirspace(AirspaceAttributes attributes)
    {
        if (attributes == null)
        {
            String message = "nullValue.AirspaceAttributesIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.attributes = attributes;

        if (!WorldWind.getMemoryCacheSet().containsCache(GEOMETRY_CACHE_KEY))
        {
            long size = Configuration.getLongValue(AVKey.AIRSPACE_GEOMETRY_CACHE_SIZE, DEFAULT_GEOMETRY_CACHE_SIZE);
            MemoryCache cache = new BasicMemoryCache((long) (0.85 * size), size);
            cache.setName(GEOMETRY_CACHE_NAME);
            WorldWind.getMemoryCacheSet().addCache(GEOMETRY_CACHE_KEY, cache);
        }
    }

    public AbstractAirspace()
    {
        this(new BasicAirspaceAttributes());
    }

    public boolean isVisible()
    {
        return this.visible;
    }

    public void setVisible(boolean visible)
    {
        this.visible = visible;
    }

    public AirspaceAttributes getAttributes()
    {
        return this.attributes;
    }

    public void setAttributes(AirspaceAttributes attributes)
    {
        if (attributes == null)
        {
            String message = "nullValue.AirspaceAttributesIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.attributes = attributes;
    }

    public double[] getAltitudes()
    {
        double[] array = new double[2];
        array[0] = this.lowerAltitude;
        array[1] = this.upperAltitude;
        return array;
    }

    protected double[] getAltitudes(double verticalExaggeration)
    {
        double[] array = this.getAltitudes();
        array[0] = array[0] * verticalExaggeration;
        array[1] = array[1] * verticalExaggeration;
        return array;
    }

    public void setAltitudes(double lowerAltitude, double upperAltitude)
    {
        this.lowerAltitude = lowerAltitude;
        this.upperAltitude = upperAltitude;
        this.setExtentOutOfDate();
    }

    public void setAltitude(double altitude)
    {
        this.setAltitudes(altitude, altitude);
    }

    public boolean[] isTerrainConforming()
    {
        // This method is here for backwards compatibility. The new scheme uses enumerations (in the form of Strings).

        boolean[] array = new boolean[2];
        array[0] = this.lowerTerrainConforming;
        array[1] = this.upperTerrainConforming;
        return array;
    }

    public void setTerrainConforming(boolean lowerTerrainConformant, boolean upperTerrainConformant)
    {
        // This method is here for backwards compatibility. The new scheme uses enumerations (in the form of Strings).

        this.lowerTerrainConforming = lowerTerrainConformant;
        this.upperTerrainConforming = upperTerrainConformant;

        this.lowerAltitudeDatum = this.lowerTerrainConforming ? AVKey.ABOVE_GROUND_LEVEL : AVKey.ABOVE_MEAN_SEA_LEVEL;
        this.upperAltitudeDatum = this.upperTerrainConforming ? AVKey.ABOVE_GROUND_LEVEL : AVKey.ABOVE_MEAN_SEA_LEVEL;

        this.setExtentOutOfDate();
    }

    public String[] getAltitudeDatum()
    {
        return new String[] {this.lowerAltitudeDatum, this.upperAltitudeDatum};
    }

    // TODO: The altitude datum logic is currently implemented only for Polygon. Implement it for the rest of them.

    public void setAltitudeDatum(String lowerAltitudeDatum, String upperAltitudeDatum)
    {
        if (lowerAltitudeDatum == null || upperAltitudeDatum == null)
        {
            String message = Logging.getMessage("nullValue.AltitudeDatumIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.lowerAltitudeDatum = lowerAltitudeDatum;
        this.upperAltitudeDatum = upperAltitudeDatum;

        if (lowerAltitudeDatum.equals(AVKey.ABOVE_GROUND_LEVEL) || lowerAltitudeDatum.equals(
            AVKey.ABOVE_GROUND_REFERENCE))
            this.lowerTerrainConforming = true;

        if (upperAltitudeDatum.equals(AVKey.ABOVE_GROUND_LEVEL) || upperAltitudeDatum.equals(
            AVKey.ABOVE_GROUND_REFERENCE))
            this.upperTerrainConforming = true;

        this.setExtentOutOfDate();
    }

    public LatLon getGroundReference()
    {
        return this.groundReference;
    }

    public void setGroundReference(LatLon groundReference)
    {
        this.groundReference = groundReference;
    }

    protected void adjustForGroundReference(DrawContext dc, boolean[] terrainConformant, double[] altitudes,
        LatLon groundRef)
    {
        if (groundRef == null)
            return; // Can't apply the datum without a reference point.

        for (int i = 0; i < 2; i++)
        {
            if (this.getAltitudeDatum()[i].equals(AVKey.ABOVE_GROUND_REFERENCE))
            {
                altitudes[i] += this.computeElevationAt(dc, groundRef.getLatitude(), groundRef.getLongitude());
                terrainConformant[i] = false;
            }
        }
    }

    public boolean isAirspaceCollapsed()
    {
        return this.lowerAltitude == this.upperAltitude && this.lowerTerrainConforming == this.upperTerrainConforming;
    }

    public void setTerrainConforming(boolean terrainConformant)
    {
        this.setTerrainConforming(terrainConformant, terrainConformant);
    }

    public boolean isEnableLevelOfDetail()
    {
        return this.enableLevelOfDetail;
    }

    public void setEnableLevelOfDetail(boolean enableLevelOfDetail)
    {
        this.enableLevelOfDetail = enableLevelOfDetail;
    }

    public Iterable<DetailLevel> getDetailLevels()
    {
        return this.detailLevels;
    }

    public void setDetailLevels(Collection<DetailLevel> detailLevels)
    {
        this.detailLevels.clear();
        this.addDetailLevels(detailLevels);
    }

    protected void addDetailLevels(Collection<DetailLevel> newDetailLevels)
    {
        if (newDetailLevels != null)
            for (DetailLevel level : newDetailLevels)
            {
                if (level != null)
                    this.detailLevels.add(level);
            }
    }

    public boolean isAirspaceVisible(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (dc.getView() == null)
        {
            String message = "nullValue.DrawingContextViewIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Extent extent = this.getExtent(dc);
        return extent != null && extent.intersects(dc.getView().getFrustumInModelCoordinates());
    }

    public Extent getExtent(Globe globe, double verticalExaggeration)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.computeExtent(globe, verticalExaggeration);
    }

    protected abstract Extent computeExtent(Globe globe, double verticalExaggeration);

    public Extent getExtent(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (dc.getGlobe() == null)
        {
            String message = Logging.getMessage("nullValue.DrawingContextGlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        ExtentInfo extentInfo = this.extents.get(dc.getGlobe());
        if (extentInfo != null && extentInfo.isValid(dc))
        {
            return extentInfo.extent;
        }
        else
        {
            extentInfo = new ExtentInfo(this.computeExtent(dc), dc);
            this.extents.put(dc.getGlobe(), extentInfo);
            return extentInfo.extent;
        }
    }

    protected Extent computeExtent(DrawContext dc)
    {
        return this.getExtent(dc.getGlobe(), dc.getVerticalExaggeration());
    }

    protected void setExtentOutOfDate()
    {
        this.extents.clear(); // Doesn't hurt to remove all cached extents because re-creation is cheap
    }

    public AirspaceRenderer getRenderer()
    {
        return this.renderer;
    }

    protected void setRenderer(AirspaceRenderer renderer)
    {
        if (renderer == null)
        {
            String message = "nullValue.AirspaceRendererIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.renderer = renderer;
    }

    public void render(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (!this.isVisible())
            return;

        if (!this.isAirspaceVisible(dc))
            return;

        this.doRender(dc);
    }

    public void renderGeometry(DrawContext dc, String drawStyle)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (drawStyle == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.doRenderGeometry(dc, drawStyle);
    }

    public void renderExtent(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        this.doRenderExtent(dc);
    }

    public void move(Position position)
    {
        if (position == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.moveTo(this.getReferencePosition().add(position));
    }

    public void moveTo(Position position)
    {
        if (position == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Position oldRef = this.getReferencePosition();
        //noinspection UnnecessaryLocalVariable
        Position newRef = position;
        this.doMoveTo(oldRef, newRef);
    }

    protected void doMoveTo(Position oldRef, Position newRef)
    {
        if (oldRef == null)
        {
            String message = "nullValue.OldRefIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (newRef == null)
        {
            String message = "nullValue.NewRefIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double[] altitudes = this.getAltitudes();
        double elevDelta = newRef.getElevation() - oldRef.getElevation();
        this.setAltitudes(altitudes[0] + elevDelta, altitudes[1] + elevDelta);
    }

    protected Position computeReferencePosition(List<? extends LatLon> locations, double[] altitudes)
    {
        if (locations == null)
        {
            String message = "nullValue.LocationsIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (altitudes == null)
        {
            String message = "nullValue.AltitudesIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        int count = locations.size();
        if (count == 0)
            return null;

        LatLon ll;
        if (count < 3)
            ll = locations.get(0);
        else
            ll = locations.get(count / 2);

        return new Position(ll, altitudes[0]);
    }

    //**************************************************************//
    //********************  Geometry Rendering  ********************//
    //**************************************************************//

    // TODO: utility method for transforming list of LatLons into equivalent list comparable of crossing the dateline
    // (a) for computing a bounding sector, then bounding cylinder
    // (b) for computing tessellations of the list as 2D points
    // These lists of LatLons (Polygon) need to be capable of passing over
    // (a) the dateline
    // (b) either pole

    protected abstract void doRenderGeometry(DrawContext dc, String drawStyle);

    protected GeometryBuilder getGeometryBuilder()
    {
        return this.geometryBuilder;
    }

    protected void setGeometryBuilder(GeometryBuilder gb)
    {
        if (gb == null)
        {
            String message = "nullValue.GeometryBuilderIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.geometryBuilder = gb;
    }

    protected void doRender(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        AirspaceRenderer renderer = this.getRenderer();
        renderer.renderNow(dc, Arrays.asList(this));
    }

    protected void doRenderExtent(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Extent extent = this.getExtent(dc);
        if (extent != null && extent instanceof Renderable)
            ((Renderable) extent).render(dc);
    }

    protected DetailLevel computeDetailLevel(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Iterable<DetailLevel> detailLevels = this.getDetailLevels();
        if (detailLevels == null)
            return null;

        Iterator<DetailLevel> iter = detailLevels.iterator();
        if (!iter.hasNext())
            return null;

        // Find the first detail level that meets rendering criteria.
        DetailLevel level = iter.next();
        while (iter.hasNext() && !level.meetsCriteria(dc, this))
        {
            level = iter.next();
        }

        return level;
    }

    protected Extent computeBoundingVolume(Globe globe, double verticalExaggeration,
        Iterable<? extends LatLon> locations)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (locations == null)
        {
            String message = "nullValue.LocationsIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double[] altitudes = this.getAltitudes();
        boolean[] terrainConformant = this.isTerrainConforming();

        // Get the points corresponding to the given locations at the lower and upper altitudes.
        ArrayList<Vec4> points = new ArrayList<Vec4>();
        for (int a = 0; a < 2; a++)
        {
            for (LatLon ll : locations)
            {
                points.add(globe.computePointFromPosition(ll.getLatitude(), ll.getLongitude(),
                    verticalExaggeration * altitudes[a]));
            }
        }
        if (points.size() == 0)
            return null;

        // Compute the average point.
        Vec4 centerPoint = Vec4.computeAveragePoint(points);
        if (centerPoint == null)
            return null;

        // Get the center point and the lower and upper altitudes.
        LatLon centerLocation = globe.computePositionFromPoint(centerPoint);
        points.add(globe.computePointFromPosition(centerLocation.getLatitude(), centerLocation.getLongitude(),
            verticalExaggeration * altitudes[0]));
        points.add(globe.computePointFromPosition(centerLocation.getLatitude(), centerLocation.getLongitude(),
            verticalExaggeration * altitudes[1]));

        // Compute the surface normal at the center point. This will be the axis or up direction of the extent.
        Vec4 axis = globe.computeSurfaceNormalAtPoint(centerPoint);

        // Compute the extrema parallel and perpendicular projections of each point on the axis.
        double min_parallel = 0.0;
        double max_parallel = 0.0;
        double max_perp = 0.0;
        for (Vec4 vec : points)
        {
            Vec4 p = vec.subtract3(centerPoint);
            double parallel_proj = p.dot3(axis);
            Vec4 parallel = axis.multiply3(parallel_proj);
            Vec4 perpendicular = p.subtract3(parallel);
            double perpendicular_proj = perpendicular.getLength3();

            if (parallel_proj < min_parallel)
                min_parallel = parallel_proj;
            else if (parallel_proj > max_parallel)
                max_parallel = parallel_proj;

            if (perpendicular_proj > max_perp)
                max_perp = perpendicular_proj;
        }

        // If terrain conformance is enabled, add the minimum or maximum elevations around the locations to the
        // extrema parallel projections.
        if (terrainConformant[0] || terrainConformant[1])
        {
            double min_elev, max_elev;
            if (LatLon.locationsCrossDateLine(locations))
            {
                Sector[] splitSector = Sector.splitBoundingSectors(locations);
                double[] a = globe.getMinAndMaxElevations(splitSector[0]);
                double[] b = globe.getMinAndMaxElevations(splitSector[1]);
                min_elev = Math.min(a[0], b[0]); // Take the smallest min elevation.
                max_elev = Math.max(a[1], b[1]); // Take the largest max elevation.
            }
            else
            {
                Sector sector = Sector.boundingSector(locations);
                double[] a = globe.getMinAndMaxElevations(sector);
                min_elev = a[0];
                max_elev = a[1];
            }

            if (terrainConformant[0] && min_elev < 0.0)
                min_parallel += verticalExaggeration * min_elev;
            if (terrainConformant[1] && max_elev > 0.0)
                max_parallel += verticalExaggeration * max_elev;
        }

        // The bottom and top of the cylinder are the extrema parallel projections from the center point along the axis.
        Vec4 bottomPoint = axis.multiply3(min_parallel).add3(centerPoint);
        Vec4 topPoint = axis.multiply3(max_parallel).add3(centerPoint);
        // The radius of the cylinder is the extrama perpendicular projection.
        double radius = max_perp;

        return new Cylinder(bottomPoint, topPoint, radius);
    }

    protected MemoryCache getGeometryCache()
    {
        return WorldWind.getMemoryCache(GEOMETRY_CACHE_KEY);
    }

    protected boolean isExpired(DrawContext dc, Geometry geom)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (dc.getGlobe() == null)
        {
            String message = Logging.getMessage("nullValue.DrawingContextGlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (geom == null)
        {
            String message = "nullValue.AirspaceGeometryIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Object o = geom.getValue(EXPIRY_TIME);
        if (o != null && o instanceof Long)
            if (dc.getFrameTimeStamp() > (Long) o)
                return true;

        o = geom.getValue(GLOBE_KEY);
        if (o != null)
            if (!dc.getGlobe().getStateKey(dc).equals(o))
                return true;

        return false;
    }

    protected void updateExpiryCriteria(DrawContext dc, Geometry geom)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (dc.getGlobe() == null)
        {
            String message = Logging.getMessage("nullValue.DrawingContextGlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        long expiryTime = this.getExpiryTime();
        geom.setValue(EXPIRY_TIME, (expiryTime >= 0L) ? expiryTime : null);
        geom.setValue(GLOBE_KEY, dc.getGlobe().getStateKey(dc));
    }

    protected long getExpiryTime()
    {
        return this.expiryTime;
    }

    protected void setExpiryTime(long timeMillis)
    {
        this.expiryTime = timeMillis;
    }

    protected long[] getExpiryRange()
    {
        long[] array = new long[2];
        array[0] = this.minExpiryTime;
        array[1] = this.maxExpiryTime;
        return array;
    }

    protected void setExpiryRange(long minTimeMillis, long maxTimeMillis)
    {
        this.minExpiryTime = minTimeMillis;
        this.maxExpiryTime = maxTimeMillis;
    }

    protected long nextExpiryTime(DrawContext dc, boolean[] terrainConformance)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        long expiryTime;
        if (terrainConformance[0] || terrainConformance[1])
        {
            long time = nextLong(this.minExpiryTime, this.maxExpiryTime);
            expiryTime = dc.getFrameTimeStamp() + time;
        }
        else
        {
            expiryTime = -1L;
        }
        return expiryTime;
    }

    private static long nextLong(long lo, long hi)
    {
        long n = hi - lo + 1;
        long i = rand.nextLong() % n;
        return lo + ((i < 0) ? -i : i);
    }

    protected void clearElevationMap()
    {
        this.elevationMap.clear();
    }

    public Vec4 computePointFromPosition(DrawContext dc, Angle latitude, Angle longitude, double elevation,
        boolean terrainConformant)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (dc.getGlobe() == null)
        {
            String message = Logging.getMessage("nullValue.DrawingContextGlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }
        if (latitude == null || longitude == null)
        {
            String message = Logging.getMessage("nullValue.LatitudeOrLongitudeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double newElevation = elevation;

        if (terrainConformant)
        {
            newElevation += this.computeElevationAt(dc, latitude, longitude);
        }

        return dc.getGlobe().computePointFromPosition(latitude, longitude, newElevation);
    }

    protected double computeElevationAt(DrawContext dc, Angle latitude, Angle longitude)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (dc.getGlobe() == null)
        {
            String message = Logging.getMessage("nullValue.DrawingContextGlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }
        if (latitude == null || longitude == null)
        {
            String message = Logging.getMessage("nullValue.LatitudeOrLongitudeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Globe globe;
        LatLon latlon;
        Vec4 surfacePoint;
        Position surfacePos;
        Double elevation;

        latlon = new LatLon(latitude, longitude);
        elevation = this.elevationMap.get(latlon);

        if (elevation == null)
        {
            globe = dc.getGlobe();
            elevation = 0.0;

            surfacePoint = dc.getPointOnGlobe(latitude, longitude);
            if (surfacePoint != null)
            {
                surfacePos = globe.computePositionFromPoint(surfacePoint);
                elevation += surfacePos.getElevation();
            }
            else
            {
                elevation += dc.getVerticalExaggeration() * globe.getElevation(latitude, longitude);
            }

            this.elevationMap.put(latlon, elevation);
        }

        return elevation;
    }

    //**************************************************************//
    //******************** END Geometry Rendering  *****************//
    //**************************************************************//

    public String getRestorableState()
    {
        RestorableSupport rs = RestorableSupport.newRestorableSupport();
        this.doGetRestorableState(rs, null);

        return rs.getStateAsXml();
    }

    protected void doGetRestorableState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        // Method is invoked by subclasses to have superclass add its state and only its state
        this.doMyGetRestorableState(rs, context);
    }

    private void doMyGetRestorableState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        rs.addStateValueAsBoolean(context, "visible", this.isVisible());
        rs.addStateValueAsDouble(context, "lowerAltitude", this.getAltitudes()[0]);
        rs.addStateValueAsDouble(context, "upperAltitude", this.getAltitudes()[1]);
        rs.addStateValueAsBoolean(context, "lowerTerrainConforming", this.isTerrainConforming()[0]);
        rs.addStateValueAsBoolean(context, "upperTerrainConforming", this.isTerrainConforming()[1]);
        rs.addStateValueAsString(context, "lowerAltitudeDatum", this.getAltitudeDatum()[0]);
        rs.addStateValueAsString(context, "upperAltitudeDatum", this.getAltitudeDatum()[1]);
        if (this.getGroundReference() != null)
            rs.addStateValueAsLatLon(context, "groundReference", this.getGroundReference());

        this.attributes.getRestorableState(rs, rs.addStateObject(context, "attributes"));
    }

    public void restoreState(String stateInXml)
    {
        if (stateInXml == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        RestorableSupport rs;
        try
        {
            rs = RestorableSupport.parse(stateInXml);
        }
        catch (Exception e)
        {
            // Parsing the document specified by stateInXml failed.
            String message = Logging.getMessage("generic.ExceptionAttemptingToParseStateXml", stateInXml);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message, e);
        }

        this.doRestoreState(rs, null);
    }

    protected void doRestoreState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        // Method is invoked by subclasses to have superclass add its state and only its state
        this.doMyRestoreState(rs, context);
    }

    private void doMyRestoreState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        Boolean booleanState = rs.getStateValueAsBoolean(context, "visible");
        if (booleanState != null)
            this.setVisible(booleanState);

        Double lo = rs.getStateValueAsDouble(context, "lowerAltitude");
        if (lo == null)
            lo = this.getAltitudes()[0];

        Double hi = rs.getStateValueAsDouble(context, "upperAltitude");
        if (hi == null)
            hi = this.getAltitudes()[1];

        this.setAltitudes(lo, hi);

        Boolean loConform = rs.getStateValueAsBoolean(context, "lowerTerrainConforming");
        if (loConform == null)
            loConform = this.isTerrainConforming()[0];

        Boolean hiConform = rs.getStateValueAsBoolean(context, "upperTerrainConforming");
        if (hiConform == null)
            hiConform = this.isTerrainConforming()[1];

        this.setTerrainConforming(loConform, hiConform);

        String lowerDatum = rs.getStateValueAsString(context, "lowerAltitudeDatum");
        if (lowerDatum == null)
            lowerDatum = this.getAltitudeDatum()[0];

        String upperDatum = rs.getStateValueAsString(context, "upperAltitudeDatum");
        if (upperDatum == null)
            upperDatum = this.getAltitudeDatum()[1];

        this.setAltitudeDatum(lowerDatum, upperDatum);

        LatLon groundRef = rs.getStateValueAsLatLon(context, "groundReference");
        if (groundRef != null)
            this.setGroundReference(groundRef);

        RestorableSupport.StateObject so = rs.getStateObject(context, "attributes");
        if (so != null)
            this.getAttributes().restoreState(rs, so);
    }
}
