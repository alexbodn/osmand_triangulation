package net.osmand.aidl.gpx;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import java.io.File;

public class ImportGpxParams implements Parcelable {
    public File file;
    public Uri gpxUri;
    public String data;
    public String fileName;
    public String color;
    public boolean show;

    public ImportGpxParams(File file, String fileName, String color, boolean show) {
        this.file = file;
        this.fileName = fileName;
        this.color = color;
        this.show = show;
    }

    public ImportGpxParams(Uri gpxUri, String fileName, String color, boolean show) {
        this.gpxUri = gpxUri;
        this.fileName = fileName;
        this.color = color;
        this.show = show;
    }

    public ImportGpxParams(String data, String fileName, String color, boolean show) {
        this.data = data;
        this.fileName = fileName;
        this.color = color;
        this.show = show;
    }

    protected ImportGpxParams(Parcel in) {
        file = (File) in.readSerializable();
        gpxUri = in.readParcelable(Uri.class.getClassLoader());
        data = in.readString();
        fileName = in.readString();
        color = in.readString();
        show = in.readByte() != 0;
    }

    public static final Creator<ImportGpxParams> CREATOR = new Creator<ImportGpxParams>() {
        @Override
        public ImportGpxParams createFromParcel(Parcel in) {
            return new ImportGpxParams(in);
        }

        @Override
        public ImportGpxParams[] newArray(int size) {
            return new ImportGpxParams[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(file);
        dest.writeParcelable(gpxUri, flags);
        dest.writeString(data);
        dest.writeString(fileName);
        dest.writeString(color);
        dest.writeByte((byte) (show ? 1 : 0));
    }
}
