package org.haldean.chopper.server;

import java.awt.*;
import javax.swing.*;

public class ImageComponent extends JLabel {
    public void setImage(byte[] imgData) {
	setIcon(new ImageIcon(imgData));
	repaint();
    }
}