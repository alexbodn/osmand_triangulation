package net.osmand.aidl;

import net.osmand.aidl.IOsmAndAidlCallback;
import net.osmand.aidl.contextmenu.ContextMenuButtonsParams;
import net.osmand.aidl.maplayer.AddMapLayerParams;
import net.osmand.aidl.maplayer.point.AddMapPointParams;
import net.osmand.aidl.gpx.ImportGpxParams;
import net.osmand.aidl.gpx.RemoveGpxParams;

interface IOsmAndAidlInterface {
    long addContextMenuButtons(in ContextMenuButtonsParams params, IOsmAndAidlCallback callback);
    boolean addMapLayer(in AddMapLayerParams params);
    boolean addMapPoint(in AddMapPointParams params);
    boolean importGpx(in ImportGpxParams params);
    boolean removeGpx(in RemoveGpxParams params);
}
