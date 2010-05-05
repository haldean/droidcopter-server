/*
Copyright (C) 2001, 2010 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.ogc.wms;

import gov.nasa.worldwind.util.WWUtil;
import gov.nasa.worldwind.util.xml.*;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.*;
import java.util.Iterator;

/**
 * Parses a WMS layer Extent element. These elements are defined only in WMS 1.1.1.
 *
 * @author tag
 * @version $Id$
 */
public class WMSLayerExtent extends AbstractXMLEventParser
{
    protected String extent;
    protected String name;
    protected String defaultValue;
    protected Boolean nearestValue;

    public WMSLayerExtent(String namespaceURI)
    {
        super(namespaceURI);
    }

    public Object parse(XMLEventParserContext ctx, XMLEvent event, Object... args) throws XMLStreamException
    {
        String s = ctx.getStringParser().parseString(ctx, event);
        if (!WWUtil.isEmpty(s))
            this.setExtent(s);

        return super.parse(ctx, event, args);
    }

    @Override
    protected void doParseEventAttributes(XMLEventParserContext ctx, XMLEvent event, Object... args)
    {
        Iterator iter = event.asStartElement().getAttributes();
        if (iter == null)
            return;

        while (iter.hasNext())
        {
            Attribute attr = (Attribute) iter.next();
            if (attr.getName().getLocalPart().equals("name") && attr.getValue() != null)
                this.setName(attr.getValue());

            else if (attr.getName().getLocalPart().equals("default") && attr.getValue() != null)
                this.setDefaultValue(attr.getValue());

            else if (attr.getName().getLocalPart().equals("nearestValue") && attr.getValue() != null)
            {
                Double d = WWUtil.convertStringToDouble(attr.getValue());
                if (d != null)
                    this.setNearestValue(d.intValue() != 0);
            }
        }
    }

    public String getExtent()
    {
        return extent;
    }

    protected void setExtent(String extent)
    {
        this.extent = extent;
    }

    public String getName()
    {
        return name;
    }

    protected void setName(String name)
    {
        this.name = name;
    }

    public String getDefaultValue()
    {
        return defaultValue;
    }

    protected void setDefaultValue(String defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    public Boolean isNearestValue()
    {
        return nearestValue;
    }

    protected void setNearestValue(Boolean nearestValue)
    {
        this.nearestValue = nearestValue;
    }
}
