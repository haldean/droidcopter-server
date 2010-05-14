package org.haldean.chopper.server;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;

public class ImagePanel extends JPanel implements Updatable {
    private JComboBox imageSizes;
    private JSlider imageQuality;
    private JLabel imagQualLabel;
    private ImageComponent image;

    private JPanel bottomPanel;

    private final int defaultQuality = 25;

    public ImagePanel() {
	super(new BorderLayout());

	bottomPanel = new JPanel(new GridLayout(1, 3));
	add(bottomPanel, BorderLayout.SOUTH);

	imageSizes = new JComboBox();
	bottomPanel.add(imageSizes);

	imageQuality = new JSlider(0, 100, defaultQuality);
	imageQuality.addChangeListener(new ChangeListener() {
		public void stateChanged(ChangeEvent e) {
		    changeQuality(imageQuality.getValue());
		}
	    });
	bottomPanel.add(imageQuality);

	imagQualLabel = new JLabel();
	bottomPanel.add(imagQualLabel);
	changeQuality(defaultQuality);

	image = new ImageComponent();
	add(image, BorderLayout.CENTER);
    }

    public String getName() {
	return "Telemetry";
    }

    public void updateUI() {
	if (imageQuality != null) {
	    imagQualLabel.updateUI();
	    imageSizes.updateUI();
	    imageQuality.updateUI();
	    bottomPanel.updateUI();
	}
    }

    public void changeQuality(int quality) {
	imagQualLabel.setText("Image Quality: " + quality);
    }

    public void setImage(byte _image[]) {
	image.setImage(_image);
    }

    public void update(String msg) {
	if (msg.startsWith("IMAGE:AVAILABLESIZE")) {
	    String msgParts[] = msg.split(":");
	    imageSizes.addItem(new String(msgParts[2] + "x" + msgParts[3]));
	}
    }
}