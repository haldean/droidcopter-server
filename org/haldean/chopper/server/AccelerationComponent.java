package org.haldean.chopper.server;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.LinkedList;

public class AccelerationComponent extends JPanel {
    private final GraphComponent xAccel;
    private final GraphComponent yAccel;
    private final GraphComponent zAccel;
    private final GraphComponent magAccel;
    private final GraphComponent avgAccel;

    private JPanel statsPanel;
    private final JLabel xLabel;
    private final JLabel yLabel;
    private final JLabel zLabel;
    private final JLabel avgLabel;
    private final JLabel deltaLabel;

    private JPanel scalePanel;
    private JSlider scaleChooser;
    private final JLabel scaleLabel;

    private LinkedList<Double> points;
    private final int averagePointCount = 100;
    private final int defaultScale = 300;

    private final Color foreground = Color.white;
    private final Color background = new Color(28, 25, 20);

    public AccelerationComponent() {
	super(new BorderLayout());
	JPanel graphsPanel = new JPanel(new GridLayout(3,2));

	points = new LinkedList<Double>();

	xAccel = new GraphComponent("X");
	yAccel = new GraphComponent("Y");
	zAccel = new GraphComponent("Z");
	magAccel = new GraphComponent("Magnitude");
	avgAccel = new GraphComponent(averagePointCount + "-Sample Average");

	xLabel = new JLabel();
	yLabel = new JLabel();
	zLabel = new JLabel();
	avgLabel = new JLabel();
	deltaLabel = new JLabel();

	statsPanel = new JPanel(new GridLayout(5,1));
	statsPanel.add(xLabel);
	statsPanel.add(yLabel);
	statsPanel.add(zLabel);
	statsPanel.add(avgLabel);
	statsPanel.add(deltaLabel);

	scaleChooser = new JSlider(25, 500, defaultScale);
	scaleLabel = new JLabel(scaleChooser.getValue() + " samples");
	scaleChooser.addChangeListener(new ChangeListener() {
		public void stateChanged(ChangeEvent e) {
		    setScale(scaleChooser.getValue());
		}
	    });
	setScale(defaultScale);

	scalePanel = new JPanel(new BorderLayout());
	scalePanel.add(scaleLabel, BorderLayout.EAST);
	scalePanel.add(scaleChooser, BorderLayout.CENTER);

	graphsPanel.add(xAccel);
	graphsPanel.add(yAccel);
	graphsPanel.add(zAccel);
	graphsPanel.add(magAccel);
	graphsPanel.add(avgAccel);
	graphsPanel.add(statsPanel);

	add(graphsPanel, BorderLayout.CENTER);
	add(scalePanel, BorderLayout.SOUTH);
    }

    public void setScale(int s) {
	xAccel.setSampleCount(s);
	yAccel.setSampleCount(s);
	zAccel.setSampleCount(s);
	magAccel.setSampleCount(s);
	avgAccel.setSampleCount(s);
	scaleLabel.setText(s + " samples");
    }

    public void updateUI() {
	super.updateUI();
	if (statsPanel != null) {
	    statsPanel.updateUI();
	    xLabel.updateUI();
	    yLabel.updateUI();
	    zLabel.updateUI();
	    avgLabel.updateUI();
	    deltaLabel.updateUI();

	    scalePanel.updateUI();
	    scaleLabel.updateUI();
	    scaleChooser.updateUI();
	}
    }

    private double average() {
	double sum = 0;
	int i;
	synchronized (points) {
	    for (i=0; i<points.size(); i++)
		sum += points.get(i);
	}
	return sum / i;
    }

    public void setAcceleration(double x, double y, double z) {
	double mag = Math.sqrt(Math.pow(x, 2) +
			       Math.pow(y, 2) +
			       Math.pow(z, 2));
	points.add(mag);
	synchronized (points) {
	    while (points.size() > averagePointCount)
		points.removeFirst();
	}
	double avg = average();

	xAccel.addPoint(x);
	yAccel.addPoint(y);
	zAccel.addPoint(z);
	magAccel.addPoint(mag);
	avgAccel.addPoint(avg);

	xLabel.setText("<html><b>Fx</b>: " + x + " N</html>");
	yLabel.setText("<html><b>Fy</b>: " + y + " N</html>");
	zLabel.setText("<html><b>Fz</b>: " + z + " N</html>");
	avgLabel.setText("<html><b>|Favg|</b>: " + avg + " N</html>");
	deltaLabel.setText("<html><b>|Favg - F|</b>: " + (avg - mag) + " N</html>"); 

	repaint();
    }
}