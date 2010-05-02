package org.haldean.chopper.server;

import java.util.LinkedList;
import java.util.Iterator;
import javax.swing.*;
import java.awt.*;

/**
 *  A class which draws a graph based on a series of given result sets
 */
public class GraphComponent extends JComponent {
    private LinkedList<Double> series;
    private double max = 1;
    private double min = -1;
    private double margin = 1;

    private int sampleCount = 100;
    private int firstIndex = 0;

    private Color background = new Color(28, 25, 20);
    private Color border = Color.darkGray;
    private Color line = Color.white;
    private Color axes = Color.lightGray;
    private Font labelFont = new Font(Font.SANS_SERIF, Font.PLAIN, 10);

    private String name;

    public GraphComponent() {
	this(new String());
    }

    public GraphComponent(String _name) {
	super();
	name = _name;
	series = new LinkedList<Double>();
    }

    public void setMax(double _max) {
	max = _max;
	repaint();
    }

    public void setMin(double _min) {
	min = _min;
	repaint();
    }

    public void setName(String _name) {
	name = _name;
	repaint();
    }

    public void setSampleCount(int _sampleCount) {
	sampleCount = _sampleCount;
	repaint();
    }

    /**
     *  Adds a result set to the graph as its own series. Note that results added
     *  MUST BE SORTED in ascending order.
     *  @param r The result set to add 
     */
    public void addPoint(double p) {
	synchronized (series) {
	    series.add(new Double(p));
	    firstIndex = Math.max(0, series.size() - sampleCount);
	    if (p > (max - margin))
		max = p + margin;
	    if (p < (min + margin))
		min = p - margin;
	}
	repaint();
    }

    public int pointToY(double p) {
	return (int) (((max - p) / (max - min)) * getSize().getHeight());
    }

    public int pointToX(int x) {
	return (int) (((float) x / (float) sampleCount) * getSize().getWidth());
    }

    /**
     *  Paints the graph onto the provided graphics object
     *  @param g The graphics object to paint onto
     */
    public void paintComponent(Graphics g) {
	Graphics2D g2 = (Graphics2D)g;

	int width = (int) getSize().getWidth();
	int height = (int) getSize().getHeight();
	int y0 = pointToY(0);

	/* Background */
	g2.setColor(background);
	g2.fillRect(0, 0, width, height);

	/* Border */
	g2.setColor(border);
	g2.drawRect(0, 0, width, height);

	/* Horizontal axis */
	g2.setColor(axes);
	g2.drawLine(0, y0, width, y0);

	/* Graph label */
	g2.setFont(labelFont);
	g2.drawString(name, 1, y0 - 2);

	g2.setColor(line);
	synchronized (series) {
	    if (series.size() > 0) {
		int lastY = pointToY(series.get(firstIndex));
		int lastX = pointToX(0);
		for (int i=firstIndex; i<series.size() && i < (firstIndex + sampleCount); i++) {
		    int y = pointToY(series.get(i));
		    int x = pointToX(i - firstIndex);

		    g2.drawLine(lastX, lastY, x, y);
		    lastY = y;
		    lastX = x;
		}
	    }
	}
    }

    public static void main(String args[]) {
	JFrame f = new JFrame();
	final GraphComponent gc = new GraphComponent("Amplifying Sine");
	gc.setPreferredSize(new Dimension(700, 400));
	gc.setSampleCount(2000);
	f.add(gc);
	f.pack();
	f.setVisible(true);
	
	try {
	    double j=0;
	    for (int i=0; i<=360; i++) {
		j += 0.1;
		gc.addPoint(j * Math.sin(Math.toRadians(i)));
		Thread.sleep(20);
		if (i==360)
		    i = 0;
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}