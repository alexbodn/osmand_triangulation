package net.osmand.aidl.contextmenu;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.List;

public class ContextMenuButtonsParams implements Parcelable {
    public AContextMenuButton leftButton;
    public AContextMenuButton rightButton;
    public String id;
    public String appPackage;
    public String layerId;
    public long callbackId;
    public List<String> pointsIds;

    public ContextMenuButtonsParams(AContextMenuButton leftButton, AContextMenuButton rightButton, String id, String appPackage, String layerId, long callbackId, List<String> pointsIds) {
        this.leftButton = leftButton;
        this.rightButton = rightButton;
        this.id = id;
        this.appPackage = appPackage;
        this.layerId = layerId;
        this.callbackId = callbackId;
        this.pointsIds = pointsIds;
    }

    protected ContextMenuButtonsParams(Parcel in) {
        leftButton = in.readParcelable(AContextMenuButton.class.getClassLoader());
        rightButton = in.readParcelable(AContextMenuButton.class.getClassLoader());
        id = in.readString();
        appPackage = in.readString();
        layerId = in.readString();
        callbackId = in.readLong();
        pointsIds = in.createStringArrayList();
    }

    public static final Creator<ContextMenuButtonsParams> CREATOR = new Creator<ContextMenuButtonsParams>() {
        @Override
        public ContextMenuButtonsParams createFromParcel(Parcel in) {
            return new ContextMenuButtonsParams(in);
        }

        @Override
        public ContextMenuButtonsParams[] newArray(int size) {
            return new ContextMenuButtonsParams[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(leftButton, flags);
        dest.writeParcelable(rightButton, flags);
        dest.writeString(id);
        dest.writeString(appPackage);
        dest.writeString(layerId);
        dest.writeLong(callbackId);
        dest.writeStringList(pointsIds);
    }
}
