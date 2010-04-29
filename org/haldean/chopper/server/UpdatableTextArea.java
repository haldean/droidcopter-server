package org.haldean.chopper.server;

import java.awt.*;
import javax.swing.*;

public class UpdatableTextArea implements Updatable {
    private JTextArea area;
    private JScrollPane scroll;

    public UpdatableTextArea() {
	super();
    }

    private void initialize() {
	if (area == null) {
	    area = new JTextArea();
	    area.setEditable(false);
	    scroll = new JScrollPane(area);
	}
    }

    public void update(String msg) {
	initialize();
	boolean keepUp = ((float) ((float) scroll.getVerticalScrollBar().getValue() / 
			   (float) scroll.getVerticalScrollBar().getMaximum()) > 0.75);
	area.setText(area.getText() + "\n" + msg);
	if (keepUp) {
	    try {
		SwingUtilities.invokeAndWait(new Runnable() {
			public void run() {
			    scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());
			}
		    });
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}
    }

    public JComponent getComponent() {
	initialize();
	return (JComponent) scroll;
    }

    public void updateUI() {
	initialize();
	scroll.updateUI();
	area.updateUI();
    }
}