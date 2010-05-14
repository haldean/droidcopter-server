package org.haldean.chopper.server;

import java.io.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.imageio.*;

/** A component to display images */
public class ImageComponent extends JComponent {
    private final Color background = new Color(28, 25, 20);
    private final Color labelColor = Color.WHITE;
    private final Font labelFont = new Font(Font.SANS_SERIF, Font.PLAIN, 10);

    private byte[] imgData;
    private BufferedImage img;
    private AffineTransform transform;

    private long lastCaptureTime;
    private double framerate;

    /** Create an empty ImageComponent */
    public ImageComponent() {
	transform = AffineTransform.getScaleInstance(1, 1);
	img = null;
    }

    /** For TabPanes */
    public String getName() {
	return "Telemetry";
    }	

    /** Set the image displayed by the component 
     *  @param _imgData A JPEG-encoded image in a byte array */
    public void setImage(byte[] _imgData) {
	try {
	    /* Make sure it isn't the same image we already have. */
	    if (_imgData != null && imgData != _imgData) {
		img = ImageIO.read(new ByteArrayInputStream(_imgData));
		imgData = _imgData;

		framerate = 1.0 / ((System.currentTimeMillis() - lastCaptureTime) / 1000.0);
		lastCaptureTime = System.currentTimeMillis();
	    }
	} catch (Exception e) {
	    img = null;
	    e.printStackTrace();
	} finally {
	    repaint();
	}
    }

    /** Paint the image and metadata onto the canvas */
    public void paintComponent(Graphics g) {
	Graphics2D g2 = (Graphics2D) g;

	int width = (int) getSize().getWidth();
	int height = (int) getSize().getHeight();
	g2.setColor(background);
	g2.fillRect(0, 0, width, height);

	g2.setColor(labelColor);
	g2.setFont(labelFont);

	if (img != null) {
	    /* Calculate the appropriate transform to fit the image to
	     * the canvas.  We fit the width, because it is larger
	     * than the height and the canvas is about square. */
	    float scale = (float) width / (float) img.getWidth();
	    /* Create a new square affine transform for that scaling */
	    transform = AffineTransform.getScaleInstance(scale, scale);
	    g2.drawImage(img, transform, null);
	
	    /* Draw the image resolution to the component */
	    g2.drawString("Size: " + (int) img.getWidth() + "x" + (int) img.getHeight(), 1, height - 10);
	    /* Draw the capture time to the component */
	    g2.drawString("Framerate: " + framerate + " fps", 1, height - 22);
	}
	
    }
}