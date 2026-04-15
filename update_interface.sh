sed -i '/import net.osmand.aidl.gpx.ImportGpxParams;/a import net.osmand.aidl.gpx.RemoveGpxParams;' app/app/src/main/aidl/net/osmand/aidl/IOsmAndAidlInterface.aidl
sed -i '/boolean importGpx(in ImportGpxParams params);/a \    boolean removeGpx(in RemoveGpxParams params);' app/app/src/main/aidl/net/osmand/aidl/IOsmAndAidlInterface.aidl
