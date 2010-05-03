package org.haldean.chopper.server;

import java.awt.*;
import javax.swing.*;

public class UpdatableTextArea extends JPanel implements Updatable {
    private JTextArea area;
    private JScrollPane scroll;
    private JCheckBox scrollLock;

    public UpdatableTextArea() {
	super(new BorderLayout());

	area = new JTextArea();
	area.setEditable(false);
	scroll = new JScrollPane(area);
	scrollLock = new JCheckBox("Scroll Lock", false);
	scrollLock.setSelected(true);

	add(scroll, BorderLayout.CENTER);
	add(scrollLock, BorderLayout.SOUTH);
    }

    public void update(String msg) {
	area.setText(area.getText() + "\n" + msg);
	if (scrollLock.isSelected()) {
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

    public void updateUI() {
	if (scrollLock != null) {
	    area.updateUI();
	    scroll.updateUI();
	    scroll.getVerticalScrollBar().updateUI();
	    scrollLock.updateUI();
	}
	super.updateUI();
    }
}