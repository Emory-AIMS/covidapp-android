package com.example.coronavirusherdimmunity.utils;
import android.location.Location;

public class GeoInd {
    private final double EARTH_RADIUS = 6378137; //in meters
    //  privacy level to epsilon
    private final double low = 20.0;
    private final double medium = 15.0;
    private final double high = 5.0;
    private final double radius = 50.0; // default radius

    public void perturbLocaion(Location location, String level) {
        double eps = levelToEps(level);
        addPolarNoise(eps, radius, location);
    }

    private void addPolarNoise(double eps, double radius, Location location) {
        double theta = Math.random() * Math.PI * 2; // random number in [0, 2 * PI)
        double z = Math.random(); //random variable in [0, 1)
        double r = inverseCumulativeGamma(eps, z) * radius;

        double ang_distance = r / EARTH_RADIUS;
        double lat = rad_of_deg(location.getLatitude());
        double lon = rad_of_deg(location.getLongitude());

        double noisyLat = Math.asin(Math.sin(lat) * Math.cos(ang_distance)
                + Math.cos(lat) * Math.sin(ang_distance) * Math.cos(theta));
        double noisyLon = lon + Math.atan2(Math.sin(theta) * Math.sin(ang_distance) * Math.cos(lat),
                Math.cos(ang_distance) - Math.sin(lat) * Math.sin(noisyLat));

        noisyLon = (noisyLon + 3 * Math.PI) % (2 * Math.PI) - Math.PI;  // normalize to -180 to 180

        location.setLatitude(deg_of_rad(noisyLat));
        location.setLongitude(deg_of_rad(noisyLon));
    }

    private double deg_of_rad(double ang){
        return ang * 180 / Math.PI;
    }

    private double rad_of_deg(double ang){
        return ang * Math.PI / 180;
    }

    private double LambertW(double x){
        double minDiff = 1e-10;
        if(x == -1/Math.E){
            return -1;
        }else if(x > 0 && x < -1){
            double q = Math.log(-x);
            double p = 1;
            while(Math.abs(p - q) > minDiff){
                p = (q * q + x / Math.exp(q)) / (q + 1);
                q = (p * p + x / Math.exp(p)) / (p + 1);
            }
            return (Math.round(1000000*q) / 1000000.0);
        }else
            return 0;
    }

    private double inverseCumulativeGamma(double eps, double z){
        double x = (z -1) / Math.E;
        return -(LambertW(x) + 1) / eps;
    }

    //  privacy level to epsilon
    public double levelToEps(String level){
        if(level.equals("L"))
            return low;
        else if(level.equals("M"))
            return medium;
        else return high;
    }
}

