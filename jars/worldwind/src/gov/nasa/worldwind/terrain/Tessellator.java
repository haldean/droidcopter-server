/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.terrain;

import gov.nasa.worldwind.WWObject;
import gov.nasa.worldwind.render.DrawContext;

/**
 * @author tag
 * @version $Id: Tessellator.java 5380 2008-05-29 17:02:06Z tgaskins $
 */
public interface Tessellator extends WWObject
{
    SectorGeometryList tessellate(DrawContext dc);

    boolean isMakeTileSkirts();

    void setMakeTileSkirts(boolean makeTileSkirts);
}
