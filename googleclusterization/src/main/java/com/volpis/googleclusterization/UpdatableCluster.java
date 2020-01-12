package com.volpis.googleclusterization;

import androidx.annotation.NonNull;

public class UpdatableCluster<T extends ClusterItem> {

    private final Cluster<T> mCluster;
    private boolean mUpdate = false;

    public UpdatableCluster(Cluster<T> cluster) {
        mCluster = cluster;
    }

    public @NonNull
    Cluster<T> getCluster() {
        return mCluster;
    }

    public boolean isUpdate() {
        return mUpdate;
    }

    public void setUpdate(boolean mUpdate) {
        this.mUpdate = mUpdate;
    }
}
