/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.awt.*;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.exception.*;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.layers.placename.*;
import gov.nasa.worldwind.util.*;

import javax.swing.*;
import java.awt.*;

/**
 * Provides a base application framework for simple WorldWind examples. Although this class will run stand-alone, it is
 * not meant to be used that way. But it has a main method to show how a derived class would call it.
 *
 * @version $Id: ApplicationTemplate.java 13078 2010-01-31 01:07:17Z tgaskins $
 */
public class ApplicationTemplate
{
    public static class AppPanel extends JPanel
    {
        protected WorldWindowGLCanvas wwd;
        protected StatusBar statusBar;

        public AppPanel(Dimension canvasSize, boolean includeStatusBar)
        {
            super(new BorderLayout());

            this.wwd = this.createWorldWindow();
            this.wwd.setPreferredSize(canvasSize);

            // Create the default model as described in the current worldwind properties.
            Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
            this.wwd.setModel(m);

            // Setup a select listener for the worldmap click-and-go feature
            this.wwd.addSelectListener(new ClickAndGoSelectListener(this.getWwd(), WorldMapLayer.class));

            this.add(this.wwd, BorderLayout.CENTER);
            if (includeStatusBar)
            {
                this.statusBar = new StatusBar();
                this.add(statusBar, BorderLayout.PAGE_END);
                this.statusBar.setEventSource(wwd);
            }
        }

        protected WorldWindowGLCanvas createWorldWindow()
        {
            return new WorldWindowGLCanvas();
        }

        public WorldWindowGLCanvas getWwd()
        {
            return wwd;
        }

        public StatusBar getStatusBar()
        {
            return statusBar;
        }
    }

    protected static class AppFrame extends JFrame
    {
        private Dimension canvasSize = new Dimension(800, 600);

        protected AppPanel wwjPanel;
        protected LayerPanel layerPanel;
        protected StatisticsPanel statsPanel;

        public AppFrame()
        {
            this.initialize(true, true, false);
        }

        public AppFrame(boolean includeStatusBar, boolean includeLayerPanel, boolean includeStatsPanel)
        {
            this.initialize(includeStatusBar, includeLayerPanel, includeStatsPanel);
        }

        protected void initialize(boolean includeStatusBar, boolean includeLayerPanel, boolean includeStatsPanel)
        {
            // Create the WorldWindow.
            this.wwjPanel = this.createAppPanel(this.canvasSize, includeStatusBar);
            this.wwjPanel.setPreferredSize(canvasSize);

            // Put the pieces together.
            this.getContentPane().add(wwjPanel, BorderLayout.CENTER);
            if (includeLayerPanel)
            {
                this.layerPanel = new LayerPanel(this.wwjPanel.getWwd(), null);
                this.getContentPane().add(this.layerPanel, BorderLayout.WEST);
            }

            if (includeStatsPanel || System.getProperty("gov.nasa.worldwind.showStatistics") != null)
            {
                this.statsPanel = new StatisticsPanel(this.wwjPanel.getWwd(), new Dimension(250, canvasSize.height));
                this.getContentPane().add(this.statsPanel, BorderLayout.EAST);
            }

            this.wwjPanel.getWwd().addRenderingExceptionListener(new RenderingExceptionListener()
            {
                public void exceptionThrown(Throwable t)
                {
                    if (t instanceof WWAbsentRequirementException)
                    {
                        String message = "Computer does not meet minimum graphics requirements.\n";
                        message += "Please install up-to-date graphics driver and try again.\n";
                        message += "Reason: " + t.getMessage() + "\n";
                        message += "This program will end when you press OK.";

                        JOptionPane.showMessageDialog(AppFrame.this, message, "Unable to Start Program",
                            JOptionPane.ERROR_MESSAGE);
                        System.exit(-1);
                    }
                }
            });

            this.pack();

            // Center the application on the screen.
            WWUtil.alignComponent(null, this, AVKey.CENTER);
            this.setResizable(true);
        }

        protected AppPanel createAppPanel(Dimension canvasSize, boolean includeStatusBar)
        {
            return new AppPanel(canvasSize, includeStatusBar);
        }

        public Dimension getCanvasSize()
        {
            return canvasSize;
        }

        public AppPanel getWwjPanel()
        {
            return wwjPanel;
        }

        public WorldWindowGLCanvas getWwd()
        {
            return this.wwjPanel.getWwd();
        }

        public StatusBar getStatusBar()
        {
            return this.wwjPanel.getStatusBar();
        }

        public LayerPanel getLayerPanel()
        {
            return layerPanel;
        }

        public StatisticsPanel getStatsPanel()
        {
            return statsPanel;
        }
    }

    public static void insertBeforeCompass(WorldWindow wwd, Layer layer)
    {
        // Insert the layer into the layer list just before the compass.
        int compassPosition = 0;
        LayerList layers = wwd.getModel().getLayers();
        for (Layer l : layers)
        {
            if (l instanceof CompassLayer)
                compassPosition = layers.indexOf(l);
        }
        layers.add(compassPosition, layer);
    }

    public static void insertBeforePlacenames(WorldWindow wwd, Layer layer)
    {
        // Insert the layer into the layer list just before the placenames.
        int compassPosition = 0;
        LayerList layers = wwd.getModel().getLayers();
        for (Layer l : layers)
        {
            if (l instanceof PlaceNameLayer)
                compassPosition = layers.indexOf(l);
        }
        layers.add(compassPosition, layer);
    }

    public static void insertAfterPlacenames(WorldWindow wwd, Layer layer)
    {
        // Insert the layer into the layer list just after the placenames.
        int compassPosition = 0;
        LayerList layers = wwd.getModel().getLayers();
        for (Layer l : layers)
        {
            if (l instanceof PlaceNameLayer)
                compassPosition = layers.indexOf(l);
        }
        layers.add(compassPosition + 1, layer);
    }

    public static void insertBeforeLayerName(WorldWindow wwd, Layer layer, String targetName)
    {
        // Insert the layer into the layer list just before the target layer.
        int targetPosition = 0;
        LayerList layers = wwd.getModel().getLayers();
        for (Layer l : layers)
        {
            if (l.getName().indexOf(targetName) != -1)
            {
                targetPosition = layers.indexOf(l);
                break;
            }
        }
        layers.add(targetPosition, layer);
    }

    static
    {
        if (Configuration.isMacOS())
        {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "World Wind Application");
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
            System.setProperty("apple.awt.brushMetalLook", "true");
        }
        else if (Configuration.isWindowsOS())
        {
            System.setProperty("sun.awt.noerasebackground", "true"); // prevents flashing during window resizing
        }
    }

    public static AppFrame start(String appName, Class appFrameClass)
    {
        if (Configuration.isMacOS() && appName != null)
        {
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", appName);
        }

        try
        {
            final AppFrame frame = (AppFrame) appFrameClass.newInstance();
            frame.setTitle(appName);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            java.awt.EventQueue.invokeLater(new Runnable()
            {
                public void run()
                {
                    frame.setVisible(true);
                }
            });

            return frame;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args)
    {
        // Call the static start method like this from the main method of your derived class.
        // Substitute your application's name for the first argument.
        ApplicationTemplate.start("World Wind Application", AppFrame.class);
    }
}
