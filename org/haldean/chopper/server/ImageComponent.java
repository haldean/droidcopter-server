package org.haldean.chopper.server;

import java.io.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.imageio.*;

public class ImageComponent extends JComponent {
    private final Color background = new Color(28, 25, 20);
    private final Color labelColor = Color.WHITE;
    private final Font labelFont = new Font(Font.SANS_SERIF, Font.PLAIN, 10);

    private byte[] imgData;
    private BufferedImage img;
    private AffineTransform transform;

    private long captureTime;

    public ImageComponent() {
	transform = AffineTransform.getScaleInstance(1, 1);
	img = null;
	captureTime = System.currentTimeMillis();
    }

    public void setCaptureTime(long time) {
	captureTime = time;
    }	

    public void setImage(byte[] _imgData) {
	try {
	    if (_imgData != null && imgData != _imgData) {
		img = ImageIO.read(new ByteArrayInputStream(_imgData));
		imgData = _imgData;
	    }
	} catch (Exception e) {
	    img = null;
	    e.printStackTrace();
	} finally {
	    repaint();
	}
    }

    public void paintComponent(Graphics g) {
	Graphics2D g2 = (Graphics2D) g;

	int width = (int) getSize().getWidth();
	int height = (int) getSize().getHeight();
	g2.setColor(background);
	g2.fillRect(0, 0, width, height);

	if (img != null) {
	    float scale = (float) width / (float) img.getWidth();
	    transform = AffineTransform.getScaleInstance(scale, scale);
	    g2.drawImage(img, transform, null);
	}
	
	g2.setColor(labelColor);
	g2.setFont(labelFont);
	g2.drawString("Captured " + ((System.currentTimeMillis() - captureTime) / 1000.0) + " sec ago", 1, 11);
    }
}