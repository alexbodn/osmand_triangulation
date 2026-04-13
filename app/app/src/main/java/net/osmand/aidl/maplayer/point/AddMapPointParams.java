package net.osmand.aidl.maplayer.point;

import android.os.Parcel;
import android.os.Parcelable;

public class AddMapPointParams implements Parcelable {
    public String layerId;
    public String pointId;
    public double lat;
    public double lon;
    public String name;
    public String iconName;
    public boolean needColorizeIcon;
    public int color;
    public String appPackage;

    public AddMapPointParams(String layerId, String pointId, double lat, double lon, String name, String iconName, boolean needColorizeIcon, int color, String appPackage) {
        this.layerId = layerId;
        this.pointId = pointId;
        this.lat = lat;
        this.lon = lon;
        this.name = name;
        this.iconName = iconName;
        this.needColorizeIcon = needColorizeIcon;
        this.color = color;
        this.appPackage = appPackage;
    }

    protected AddMapPointParams(Parcel in) {
        layerId = in.readString();
        pointId = in.readString();
        lat = in.readDouble();
        lon = in.readDouble();
        name = in.readString();
        iconName = in.readString();
        needColorizeIcon = in.readByte() != 0;
        color = in.readInt();
        appPackage = in.readString();
    }

    public static final Creator<AddMapPointParams> CREATOR = new Creator<AddMapPointParams>() {
        @Override
        public AddMapPointParams createFromParcel(Parcel in) {
            return new AddMapPointParams(in);
        }

        @Override
        public AddMapPointParams[] newArray(int size) {
            return new AddMapPointParams[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(layerId);
        dest.writeString(pointId);
        dest.writeDouble(lat);
        dest.writeDouble(lon);
        dest.writeString(name);
        dest.writeString(iconName);
        dest.writeByte((byte) (needColorizeIcon ? 1 : 0));
        dest.writeInt(color);
        dest.writeString(appPackage);
    }
}
