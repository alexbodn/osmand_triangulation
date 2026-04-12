package net.osmand.aidl;

import net.osmand.aidl.IOsmAndAidlCallback;
import net.osmand.aidl.contextmenu.ContextMenuButtonsParams;

interface IOsmAndAidlInterface {
    long addContextMenuButtons(in ContextMenuButtonsParams params, IOsmAndAidlCallback callback);
}
