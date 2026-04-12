package net.osmand.aidl.contextmenu;

import android.os.Parcel;
import android.os.Parcelable;

public class AContextMenuButton implements Parcelable {
    public int buttonId;
    public String leftTextCaption;
    public String rightTextCaption;
    public String leftIconName;
    public String rightIconName;
    public boolean needColorizeIcon;
    public boolean enabled;

    public AContextMenuButton(int buttonId, String leftTextCaption, String rightTextCaption, String leftIconName, String rightIconName, boolean needColorizeIcon, boolean enabled) {
        this.buttonId = buttonId;
        this.leftTextCaption = leftTextCaption;
        this.rightTextCaption = rightTextCaption;
        this.leftIconName = leftIconName;
        this.rightIconName = rightIconName;
        this.needColorizeIcon = needColorizeIcon;
        this.enabled = enabled;
    }

    protected AContextMenuButton(Parcel in) {
        buttonId = in.readInt();
        leftTextCaption = in.readString();
        rightTextCaption = in.readString();
        leftIconName = in.readString();
        rightIconName = in.readString();
        needColorizeIcon = in.readByte() != 0;
        enabled = in.readByte() != 0;
    }

    public static final Creator<AContextMenuButton> CREATOR = new Creator<AContextMenuButton>() {
        @Override
        public AContextMenuButton createFromParcel(Parcel in) {
            return new AContextMenuButton(in);
        }

        @Override
        public AContextMenuButton[] newArray(int size) {
            return new AContextMenuButton[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(buttonId);
        dest.writeString(leftTextCaption);
        dest.writeString(rightTextCaption);
        dest.writeString(leftIconName);
        dest.writeString(rightIconName);
        dest.writeByte((byte) (needColorizeIcon ? 1 : 0));
        dest.writeByte((byte) (enabled ? 1 : 0));
    }
}
