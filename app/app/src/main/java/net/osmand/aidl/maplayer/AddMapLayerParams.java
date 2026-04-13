package net.osmand.aidl.maplayer;

import android.os.Parcel;
import android.os.Parcelable;

public class AddMapLayerParams implements Parcelable {
    public String layerId;
    public String layerName;
    public float zOrder;

    public AddMapLayerParams(String layerId, String layerName, float zOrder) {
        this.layerId = layerId;
        this.layerName = layerName;
        this.zOrder = zOrder;
    }

    protected AddMapLayerParams(Parcel in) {
        layerId = in.readString();
        layerName = in.readString();
        zOrder = in.readFloat();
    }

    public static final Creator<AddMapLayerParams> CREATOR = new Creator<AddMapLayerParams>() {
        @Override
        public AddMapLayerParams createFromParcel(Parcel in) {
            return new AddMapLayerParams(in);
        }

        @Override
        public AddMapLayerParams[] newArray(int size) {
            return new AddMapLayerParams[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(layerId);
        dest.writeString(layerName);
        dest.writeFloat(zOrder);
    }
}
