package org.haldean.chopper.server;

import java.awt.*;
import javax.swing.*;

public class AccelerationComponent extends JPanel {
    private GraphComponent xAccel;
    private GraphComponent yAccel;
    private GraphComponent zAccel;

    private JPanel statsPanel;
    private final JLabel xLabel;
    private final JLabel yLabel;
    private final JLabel zLabel;

    private final Color background = new Color(28, 25, 20);

    public AccelerationComponent() {
	super(new GridLayout(2,2));
	xAccel = new GraphComponent();
	yAccel = new GraphComponent();
	zAccel = new GraphComponent();

	xLabel = new JLabel();
	yLabel = new JLabel();
	zLabel = new JLabel();

	statsPanel = new JPanel(new GridLayout(3,1));
	statsPanel.add(xLabel);
	statsPanel.add(yLabel);
	statsPanel.add(zLabel);

	add(xAccel);
	add(yAccel);
	add(zAccel);
	add(statsPanel);
    }

    public void setAcceleration(double x, double y, double z) {
	xAccel.addPoint(x);
	yAccel.addPoint(y);
	zAccel.addPoint(z);

	xLabel.setText("X: " + x);
	yLabel.setText("Y: " + y);
	zLabel.setText("Z: " + z);
	repaint();
    }
}