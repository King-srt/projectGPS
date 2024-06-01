package com.example.lastmodulgps;

public class Data {
    private String latitude;
    private String longitude;

    public String getAlamat() {
        return alamat;
    }

    public void setAlamat(String alamat) {
        this.alamat = alamat;
    }

    private String alamat;

    public Data(String latitude, String longitude, String alamat) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.alamat = alamat;
    }

    public Data() {
   //
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }
}
