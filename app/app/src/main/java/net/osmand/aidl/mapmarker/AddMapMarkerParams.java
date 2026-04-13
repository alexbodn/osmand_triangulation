package net.osmand.aidl.mapmarker;

import android.os.Parcel;
import android.os.Parcelable;

public class AddMapMarkerParams implements Parcelable {
    public double lat;
    public double lon;
    public String name;

    public AddMapMarkerParams(double lat, double lon, String name) {
        this.lat = lat;
        this.lon = lon;
        this.name = name;
    }

    protected AddMapMarkerParams(Parcel in) {
        lat = in.readDouble();
        lon = in.readDouble();
        name = in.readString();
    }

    public static final Creator<AddMapMarkerParams> CREATOR = new Creator<AddMapMarkerParams>() {
        @Override
        public AddMapMarkerParams createFromParcel(Parcel in) {
            return new AddMapMarkerParams(in);
        }

        @Override
        public AddMapMarkerParams[] newArray(int size) {
            return new AddMapMarkerParams[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(lat);
        dest.writeDouble(lon);
        dest.writeString(name);
    }
}
