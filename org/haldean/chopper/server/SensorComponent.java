package org.haldean.chopper.server;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;

public class SensorComponent extends JPanel {
    private final GraphComponent light;
    private final GraphComponent prox;
    private final GraphComponent flux;
    private final GraphComponent temp;
    private final GraphComponent press;

    private JPanel statsPanel;
    private final JLabel lightLabel;
    private final JLabel proxLabel;
    private final JLabel fluxLabel;
    private final JLabel tempLabel;
    private final JLabel pressLabel;

    private JPanel scalePanel;
    private JSlider scaleChooser;
    private final JLabel scaleLabel;

    private final int defaultScale = 300;

    private final Color foreground = Color.white;
    private final Color background = new Color(28, 25, 20);

    public SensorComponent() {
	super(new BorderLayout());
	JPanel graphsPanel = new JPanel(new GridLayout(3,2));

	light = new GraphComponent("Light");
	prox = new GraphComponent("Proximity");
	flux = new GraphComponent("Flux");
	temp = new GraphComponent("Temperature");
	press = new GraphComponent("Pressure");

	lightLabel = new JLabel();
	proxLabel = new JLabel();
	fluxLabel = new JLabel();
	tempLabel = new JLabel();
	pressLabel = new JLabel();

	statsPanel = new JPanel(new GridLayout(5,1));
	statsPanel.add(lightLabel);
	statsPanel.add(proxLabel);
	statsPanel.add(fluxLabel);
	statsPanel.add(tempLabel);
	statsPanel.add(pressLabel);

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

	graphsPanel.add(light);
	graphsPanel.add(prox);
	graphsPanel.add(flux);
	graphsPanel.add(temp);
	graphsPanel.add(press);
	graphsPanel.add(statsPanel);

	add(graphsPanel, BorderLayout.CENTER);
	add(scalePanel, BorderLayout.SOUTH);
    }

    public String getName() {
	return "Sensors";
    }

    public void setScale(int s) {
	light.setSampleCount(s);
	prox.setSampleCount(s);
	flux.setSampleCount(s);
	temp.setSampleCount(s);
	press.setSampleCount(s);
	scaleLabel.setText(s + " samples");
    }

    public void updateUI() {
	super.updateUI();
	if (statsPanel != null) {
	    statsPanel.updateUI();
	    lightLabel.updateUI();
	    proxLabel.updateUI();
	    fluxLabel.updateUI();
	    tempLabel.updateUI();
	    pressLabel.updateUI();
	    scaleLabel.updateUI();

	    scalePanel.updateUI();
	    scaleLabel.updateUI();
	    scaleChooser.updateUI();
	}
    }

    public void setLight(double _l) {
	light.addPoint(_l);
	lightLabel.setText("<html><b>Light</b>: " + _l + " lux</html>");
	repaint();
    }

    public void setProximity(double _p) {
	prox.addPoint(_p);
	proxLabel.setText("<html><b>Proximity</b>: " + _p + " cm</html>");
	repaint();
    }

    public void setFlux(double _f) {
	flux.addPoint(_f);
	fluxLabel.setText("<html><b>Flux</b>: " + _f + " \u00B5T</html>");
	repaint();
    }

    public void setTemperature(double _t) {
	temp.addPoint(_t);
	tempLabel.setText("<html><b>Temperature</b>: " + _t + "\u00B0 C</html>");
	repaint();
    }

    public void setPressure(double _p) {
	press.addPoint(_p);
	pressLabel.setText("<html><b>Pressure</b>: " + _p + " Pa</html>");
	repaint();
    }
}