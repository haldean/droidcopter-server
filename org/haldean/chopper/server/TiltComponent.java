package org.haldean.chopper.server;

import javax.swing.*;
import java.awt.*;

public class TiltComponent extends JComponent {
    private Orientation tilt;

    private final Color line1 = Color.white;
    private final Color line2 = Color.red;
    private final Color background = new Color(28, 25, 20);

    public TiltComponent() {
	tilt = null;
    }

    /* Argument is in degrees */
    public void setTilt(Orientation _tilt) {
	tilt = _tilt;
	repaint();
    }

    /* Returns the shortest side length of the canvas */
    private int getQuadrantDiameter() {
	Dimension d = getSize();
	return (int) (Math.min(d.getHeight(), d.getWidth()) / 2);
    }

    private int getQuadrantCenterX(int Q) {
	int qWidth = (int) (getSize().getWidth() / 4);
	return qWidth + ((Q == 1 || Q == 4) ? 2 * qWidth : 0);
    }

    private int getQuadrantCenterY(int Q) {
	int qHeight = (int) (getSize().getHeight() / 4);
	return qHeight + ((Q >= 3) ? 2 * qHeight : 0);
    }
    
    public void paint(Graphics g) {
	Graphics2D g2 = (Graphics2D) g;
       
	g2.setColor(background);
	g2.fillRect(0, 0, (int) getSize().getWidth(), (int) getSize().getHeight());

	if (tilt == null)
	    return;

	/* Tilt drawing */
	int lineLength = (int) ((0.4 * getQuadrantDiameter()));
	int tilt_x_center = getQuadrantCenterX(2);
	int tilt_x_component = (int) (lineLength * Math.cos(tilt.getTilt(Orientation.RADIANS)));
	int tilt_y_center = getQuadrantCenterY(2);
	int tilt_y_component = (int) (lineLength * Math.sin(tilt.getTilt(Orientation.RADIANS)));
	int tilt_negate = (Math.abs(tilt.getTilt(Orientation.DEGREES)) > 90) ? -1 : 1;

	g2.setColor(line1);
	g2.drawLine(tilt_x_center - tilt_x_component, tilt_y_center - tilt_y_component,
		    tilt_x_center + tilt_x_component, tilt_y_center + tilt_y_component);
	g2.setColor(line2);
	g2.drawLine(tilt_x_center - tilt_x_component + tilt_negate, tilt_y_center - tilt_y_component + 2 * tilt_negate,
		    tilt_x_center + tilt_x_component + tilt_negate, tilt_y_center + tilt_y_component + 2 * tilt_negate);

	/* Pitch drawing */
	int pitch_x_center = getQuadrantCenterX(1);
	int pitch_x_component = (int) (lineLength * Math.sin(tilt.getPitch(Orientation.RADIANS)));
	int pitch_y_center = getQuadrantCenterY(1);
	int pitch_y_component = (int) (lineLength * Math.cos(tilt.getPitch(Orientation.RADIANS)));

	g2.setColor(line1);
	g2.drawLine(pitch_x_center, pitch_y_center,
		    pitch_x_center - pitch_x_component, pitch_y_center - pitch_y_component);
	g2.fillOval(pitch_x_center - 2, pitch_y_center - 2, 5, 5);

	/* Roll drawing */
	int roll_x_center = getQuadrantCenterX(4);
	int roll_x_component = (int) (lineLength * Math.sin(tilt.getRoll(Orientation.RADIANS)));
	int roll_y_center = getQuadrantCenterY(4);
	int roll_y_component = (int) (lineLength * Math.cos(tilt.getRoll(Orientation.RADIANS)));

	g2.setColor(line2);
	g2.drawLine(roll_x_center, roll_y_center - lineLength - 10, roll_x_center, roll_y_center);
	g2.setColor(line1);
	g2.drawOval(roll_x_center - lineLength, roll_y_center - lineLength, lineLength * 2, lineLength * 2);
	g2.drawLine(roll_x_center, roll_y_center,
		    roll_x_center - roll_x_component, roll_y_center - roll_y_component);
	g2.fillOval(roll_x_center - 2, roll_y_center - 2, 5, 5);
	
	g2.setColor(line1);
	g2.drawString("Tilt: " + Math.round(tilt.getTilt(Orientation.DEGREES)) + "\u00B0", 
		      2, (int) getSize().getHeight() - 30);
	g2.drawString("Pitch: " + Math.round(tilt.getPitch(Orientation.DEGREES)) + "\u00B0",
		      2, (int) getSize().getHeight() - 18);
	g2.drawString("Roll: " + Math.round(tilt.getRoll(Orientation.DEGREES)) + "\u00B0",
		      2, (int) getSize().getHeight() - 6);
	if (Math.abs(tilt.getTilt(Orientation.DEGREES)) > 90) {
	    g2.setColor(Color.red);
	    Font old = g2.getFont();
	    g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
	    g2.drawString("OH FUCK IT'S UPSIDE DOWN.", 2, (int) getSize().getHeight() - 45);
	}
    }
}