package net.osmand.aidl;

interface IOsmAndAidlCallback {
    void onUpdate();
    void onKeyboardAppeared();
    void onKeyboardDisappeared();
    void onContextMenuButtonClicked(int buttonId, String pointId, String layerId);
}
