/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.cache.MemoryCache;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.LayerConfiguration;
import gov.nasa.worldwind.util.*;
import org.w3c.dom.Document;

import java.io.IOException;

/**
 * @author dcollins
 * @version $Id: TiledImageProducer.java 13257 2010-04-08 16:55:59Z dcollins $
 */
public class TiledImageProducer extends TiledRasterProducer
{
    private static final String DEFAULT_IMAGE_FORMAT = "image/png";
    // Statically reference the readers used to for unknown data sources. This drastically improves the performance of
    // reading large quantities of sources. Since the readers are invoked from a single thread, they can be
    // safely re-used.
    private static DataRasterReader[] readers = new DataRasterReader[]
        {
            new ImageIORasterReader(),
            new GeotiffRasterReader(),
            new RPFRasterReader()
        };

    public TiledImageProducer(MemoryCache cache, int writeThreadPoolSize)
    {
        super(cache, writeThreadPoolSize);
    }

    public TiledImageProducer()
    {
    }

    public String getDataSourceDescription()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(Logging.getMessage("TiledImageProducer.Description"));
        sb.append(" (").append(super.getDataSourceDescription()).append(")");
        return sb.toString();
    }

    protected DataRaster createDataRaster(int width, int height, Sector sector, AVList params)
    {
        int transparency = java.awt.image.BufferedImage.TRANSLUCENT; // TODO: make configurable
        //noinspection UnnecessaryLocalVariable
        BufferedImageRaster raster = new BufferedImageRaster(width, height, transparency, sector);
        return raster;
    }

    protected DataRasterReader[] getDataRasterReaders()
    {
        return readers;
    }

    protected DataRasterWriter[] getDataRasterWriters()
    {
        return new DataRasterWriter[]
            {
                // Configure the ImageIO writer to disable writing of georeference files. Georeferencing files are
                // redundant for tiled images. The image format is defined in the data configuration file, and each
                // tile's georeferencing information is implicit in the tile structure.
                new ImageIORasterWriter(false),
                new DDSRasterWriter()
            };
    }

    protected String validateDataSource(Object source, AVList params)
    {
        // TiledImageProducer does not accept null data sources.
        if (source == null)
            return Logging.getMessage("nullValue.SourceIsNull");

        // TiledRasterProducer accepts BufferedImageRaster as a data source. If the data source is a DataRaster, then
        // check that it's a BufferedImageRaster.
        if (source instanceof DataRaster)
        {
            DataRaster raster = (DataRaster) source;

            if (!(raster instanceof BufferedImageRaster))
                return Logging.getMessage("TiledRasterProducer.UnrecognizedDataSource", raster);

            String s = this.validateDataSourceParams(raster, String.valueOf(raster));
            if (s != null)
                return s;
        }
        // For any other data source, attempt to find a reader for the data source. If the reader know's the data
        // source's raster type, then check that it's a color image or a monochromatic image.
        else
        {
            DataRasterReader reader = ReadableDataRaster.findReaderFor(source, params, this.getDataRasterReaders());
            if (reader == null)
                return Logging.getMessage("TiledRasterProducer.UnrecognizedDataSource", source);

            // Copy the parameter list to insulate changes from the caller.
            params = (params != null) ? params.copy() : new AVListImpl();

            try
            {
                reader.readMetadata(source, params);
            }
            catch (IOException e)
            {
                return Logging.getMessage("TiledRasterProducer.ExceptionWhileReading", source, e.getMessage());
            }

            String s = this.validateDataSourceParams(params, String.valueOf(source));
            if (s != null)
                return s;
        }

        return null;
    }

    protected String validateDataSourceParams(AVList params, String name)
    {
        if (params.hasKey(AVKey.RASTER_TYPE) &&
            params.getValue(AVKey.RASTER_TYPE) != AVKey.RASTER_TYPE_COLOR_IMAGE &&
            params.getValue(AVKey.RASTER_TYPE) != AVKey.RASTER_TYPE_MONOCHROME_IMAGE)
        {
            return Logging.getMessage("TiledRasterProducer.UnrecognizedRasterType",
                params.getValue(AVKey.RASTER_TYPE), name);
        }

        if (params.hasKey(AVKey.COORDINATE_SYSTEM) &&
            params.getValue(AVKey.COORDINATE_SYSTEM) != AVKey.COORDINATE_SYSTEM_GEOGRAPHIC)
        {
            return Logging.getMessage("TiledRasterProducer.UnrecognizedCoordinateSystem",
                params.getValue(AVKey.COORDINATE_SYSTEM), name);
        }

        if (params.getValue(AVKey.SECTOR) == null)
            return Logging.getMessage("TiledRasterProducer.NoSector", name);

        return null;
    }

    protected void initProductionParameters(AVList params)
    {
        // Preserve backward compatibility with previous verisons of TiledImageProducer. If the caller specified a
        // format suffix parameter, use it to compute the image format properties. This gives priority to the format
        // suffix property to ensure applications which use format suffix continue to work.
        if (params.getValue(AVKey.FORMAT_SUFFIX) != null)
        {
            String s = WWIO.makeMimeTypeForSuffix(params.getValue(AVKey.FORMAT_SUFFIX).toString());
            if (s != null)
            {
                params.setValue(AVKey.IMAGE_FORMAT, s);
                params.setValue(AVKey.AVAILABLE_IMAGE_FORMATS, new String[] {s});
            }
        }

        // Use the default image format if none exists.
        if (params.getValue(AVKey.IMAGE_FORMAT) == null)
            params.setValue(AVKey.IMAGE_FORMAT, DEFAULT_IMAGE_FORMAT);

        // Compute the available image formats if none exists.
        if (params.getValue(AVKey.AVAILABLE_IMAGE_FORMATS) == null)
        {
            params.setValue(AVKey.AVAILABLE_IMAGE_FORMATS,
                new String[] {params.getValue(AVKey.IMAGE_FORMAT).toString()});
        }

        // Compute the format suffix if none exists.
        if (params.getValue(AVKey.FORMAT_SUFFIX) == null)
        {
            params.setValue(AVKey.FORMAT_SUFFIX,
                WWIO.makeSuffixForMimeType(params.getValue(AVKey.IMAGE_FORMAT).toString()));
        }
    }

    /**
     * Returns a Layer configuration document which describes the tiled imagery produced by this TiledImageProducer. The
     * document's contents are based on the configuration document for a TiledImageLayer, except this document describes
     * an offline dataset. This returns null if the parameter list is null, or if the configuration document cannot be
     * created for any reason.
     *
     * @param params the parameters which describe a Layer configuration document's contents.
     *
     * @return the configuration document, or null if the parameter list is null or does not contain the required
     *         parameters.
     */
    protected Document createConfigDoc(AVList params)
    {
        AVList configParams = params.copy();

        // Determine a default display name if none exists.
        if (configParams.getValue(AVKey.DISPLAY_NAME) == null)
            configParams.setValue(AVKey.DISPLAY_NAME, params.getValue(AVKey.DATASET_NAME));

        // Set the SERVICE_NAME and NETWORK_RETRIEVAL_ENABLED parameters to indicate this dataset is offline.
        configParams.setValue(AVKey.SERVICE_NAME, "Offline");
        configParams.setValue(AVKey.NETWORK_RETRIEVAL_ENABLED, Boolean.FALSE);

        // Set the USE_MIP_MAPS, and USE_TRANSPARENT_TEXTURES parameters to true. The imagery produced by
        // TiledImageProducer is best viewed with mipMapping enabled, and texture transparency enabled. These parameters
        // tell the consumer of this imagery to enable these features during rendering.
        configParams.setValue(AVKey.USE_MIP_MAPS, Boolean.TRUE);
        configParams.setValue(AVKey.USE_TRANSPARENT_TEXTURES, Boolean.TRUE);

        // Set the COMPRESS_TEXTURES parameter to true for any non-DDS tile format.
        if (configParams.getValue(AVKey.FORMAT_SUFFIX) == null ||
            !configParams.getValue(AVKey.FORMAT_SUFFIX).toString().endsWith("dds"))
        {
            configParams.setValue(AVKey.COMPRESS_TEXTURES, Boolean.TRUE);
        }

        // Return a configuration file for a TiledImageLayer. TiledImageLayer is the standard WWJ component which
        // consumes and renders tiled imagery.
        return LayerConfiguration.createTiledImageLayerDocument(configParams);
    }
}
