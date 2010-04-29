package org.haldean.chopper.server;

public class Waypoint {
    public double lat;
    public double lon;
    public double alt;
    public double accuracy;

    public Waypoint(double _lat, double _lon) {
	this(_lat, _lon, 0);
    }

    public Waypoint(double _lat, double _lon, double _alt) {
	lat = _lat;
	lon = _lon;
	alt = _alt;
    }

    public Waypoint(double _lat, double _lon, double _alt, double _accuracy) {
	lat = _lat;
	lon = _lon;
	alt = _alt;
	accuracy = _accuracy;
    }

    public String toString() {
	return toString(",");
    }

    public String toString(String divider) {
	return new String(lat + divider + lon);
    }

    public double distance(Waypoint w) {
	return Math.sqrt(Math.pow(w.getLatitude() - lat, 2) +
			 Math.pow(w.getLongitude() - lon, 2) +
			 Math.pow(w.getAltitude() - alt, 2));
    }

    public double getLatitude() {
	return lat;
    }

    public double getLongitude() {
	return lon;
    }

    public double getAltitude() {
	return alt; 
    }
}