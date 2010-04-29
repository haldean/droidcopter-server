package org.haldean.chopper.server;

public class Orientation {
    private double rollRad;
    private double rollDeg;
    
    private double tiltRad;
    private double tiltDeg;

    private double pitchRad;
    private double pitchDeg;

    public static final int RADIANS = 0;
    public static final int DEGREES = 1;

    public Orientation(double _roll, double _tilt, double _pitch) {
	setRoll(_roll);
	setTilt(_tilt);
	setPitch(_pitch);
    }

    public void setRoll(double _roll) {
	rollDeg = _roll;
	rollRad = Math.toRadians(_roll);
    }

    public void setTilt(double _tilt) {
	tiltDeg = _tilt;
	tiltRad = Math.toRadians(_tilt);
    }

    public void setPitch(double _pitch) {
	pitchDeg = _pitch;
	pitchRad = Math.toRadians(_pitch);
    }

    public double getRoll(int unit) {
	if (unit == RADIANS)
	    return rollRad;
	else
	    return rollDeg;
    }

    public double getTilt(int unit) {
	if (unit == RADIANS)
	    return tiltRad;
	else
	    return tiltDeg;
    }

    public double getPitch(int unit) {
	if (unit == RADIANS)
	    return pitchRad;
	else
	    return pitchDeg;
    }

    public String toString() {
	return (new Double(rollDeg)).toString() + "\u00B0, " + 
	    (new Double(tiltDeg)).toString() + "\u00B0, " +
	    (new Double(pitchDeg)).toString() + "\u00B0";
    }
}