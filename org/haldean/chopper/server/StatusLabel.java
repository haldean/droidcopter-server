package org.haldean.chopper.server;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class StatusLabel extends JPanel implements Updatable {
    private boolean globeMode;
    private boolean isConnected;
    private boolean isReceiving;
    private double battery;

    private Timer receiptTimer;
    private final int receivingDelay = 2000;

    private JLabel globeModeLabel;
    private JLabel connectedLabel;
    private JLabel receivingLabel;
    private JLabel batteryLabel;

    private final Color accept = Color.GREEN;
    private final Color reject = Color.RED;

    public StatusLabel() {
	super(new GridLayout(1, 4));

	receiptTimer = new Timer(receivingDelay, new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    setReceiving(false);
		}
	    });
	receiptTimer.setRepeats(false);
	receiptTimer.setInitialDelay(receivingDelay);
	receiptTimer.start();

	globeModeLabel = new JLabel();
	globeModeLabel.setOpaque(true);
	globeModeLabel.setHorizontalAlignment(SwingConstants.CENTER);

	connectedLabel = new JLabel();
	connectedLabel.setOpaque(true);
	connectedLabel.setHorizontalAlignment(SwingConstants.CENTER);

	receivingLabel = new JLabel();
	receivingLabel.setOpaque(true);
	receivingLabel.setHorizontalAlignment(SwingConstants.CENTER);

	batteryLabel = new JLabel("Battery Unknown");
	batteryLabel.setOpaque(true);
	batteryLabel.setHorizontalAlignment(SwingConstants.CENTER);

	add(connectedLabel);
	add(receivingLabel);
	add(batteryLabel);
	add(globeModeLabel);
	
	setGlobeMode(false);
	setConnected(false);
	setReceiving(false);

    }

    public void setGlobeMode(boolean _globeMode) {
	globeMode = _globeMode;
	if (globeMode) {
	    globeModeLabel.setText("Globe Mode ON");
	    globeModeLabel.setBackground(reject);
	} else {
	    globeModeLabel.setText("Globe Mode OFF");
	    globeModeLabel.setBackground(accept);
	}
    }

    public void setConnected(boolean _isConnected) {
	isConnected = _isConnected;
	if (isConnected) {
	    connectedLabel.setText("CONNECTED");
	    connectedLabel.setBackground(accept);
	} else {
	    connectedLabel.setText("NOT CONNECTED");
	    connectedLabel.setBackground(reject);
	}
    }

    public void setReceiving(boolean _isReceiving) {
	isReceiving = _isReceiving;
	if (isReceiving) {
	    receivingLabel.setText("RECEIVING");
	    receivingLabel.setBackground(accept);
	} else {
	    receivingLabel.setText("NOT RECEIVING");
	    receivingLabel.setBackground(reject);
	}
    }

    public void setBattery(double _battery) {
	battery = _battery;
	batteryLabel.setText("Battery at " + (int) (battery * 100) + "%");
	if (battery <= 0.3)
	    batteryLabel.setBackground(reject);
	else 
	    batteryLabel.setBackground(accept);
    }

    public void update(String s) {
	setReceiving(true);
	receiptTimer.restart();
    }
}