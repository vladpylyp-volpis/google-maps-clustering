package com.volpis.googleclusterization;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.volpis.googleclusterization.Preconditions.checkNotNull;

class ClusterRenderer<T extends ClusterItem> implements GoogleMap.OnMarkerClickListener {

    private static final String TAG = ClusterRenderer.class.getName();

    private static final int BACKGROUND_MARKER_Z_INDEX = 0;

    private static final int FOREGROUND_MARKER_Z_INDEX = 1;

    private final GoogleMap mGoogleMap;

    private final List<Cluster<T>> mClusters = new ArrayList<>();

    private final Map<Cluster<T>, Marker> mMarkers = new HashMap<>();

    private IconGenerator<T> mIconGenerator;

    private ClusterManager.Callbacks<T> mCallbacks;

    ClusterRenderer(@NonNull Context context, @NonNull GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.setOnMarkerClickListener(this);
        mIconGenerator = new DefaultIconGenerator<>(context);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Object markerTag = marker.getTag();
        if (markerTag instanceof Cluster) {
            //noinspection unchecked
            Cluster<T> cluster = (Cluster<T>) marker.getTag();
            List<T> clusterItems = cluster.getItems();

            if (mCallbacks != null) {
                if (clusterItems.size() > 1) {
                    return mCallbacks.onClusterClick(cluster);
                } else {
                    return mCallbacks.onClusterItemClick(clusterItems.get(0));
                }
            }
        }

        return false;
    }

    void setCallbacks(@Nullable ClusterManager.Callbacks<T> listener) {
        mCallbacks = listener;
    }

    void setIconGenerator(@NonNull IconGenerator<T> iconGenerator) {
        mIconGenerator = iconGenerator;
    }

    void render(@NonNull List<Cluster<T>> clusters) {
        List<Cluster<T>> clustersToAdd = new ArrayList<>();
        List<Cluster<T>> clustersToRemove = new ArrayList<>();

        for (Cluster<T> cluster : clusters) {
            if (mMarkers.containsKey(cluster)) {
                clustersToRemove.add(cluster);
            }
            clustersToAdd.add(cluster);
        }

        for (Cluster<T> cluster : mMarkers.keySet()) {
            if (!clusters.contains(cluster)) {
                clustersToRemove.add(cluster);
            }
        }

        mClusters.addAll(clustersToAdd);
        mClusters.removeAll(clustersToRemove);

        // Remove the old clusters.
        for (Cluster<T> clusterToRemove : clustersToRemove) {
            removeCluster(clusterToRemove);
        }

        // Add the new clusters.
        for (Cluster<T> clusterToAdd : clustersToAdd) {
            addCluster(clusterToAdd, clustersToRemove);
        }
    }

    private void addCluster(Cluster<T> clusterToAdd, List<Cluster<T>> clustersToRemove) {
        Marker markerToAdd;

        MarkerOptions markerOptionsToAdd = getMarkerOptions(clusterToAdd);

        Cluster parentCluster = findParentCluster(clustersToRemove, clusterToAdd.getLatitude(),
                clusterToAdd.getLongitude());
        if (parentCluster != null) {
            markerToAdd = mGoogleMap.addMarker(markerOptionsToAdd);
            animateMarkerToLocation(markerToAdd,
                    new LatLng(clusterToAdd.getLatitude(), clusterToAdd.getLongitude()), false);
        } else {
            markerOptionsToAdd.alpha(0.0F);
            markerToAdd = mGoogleMap.addMarker(markerOptionsToAdd);
            animateMarkerAppearance(markerToAdd);
        }
        markerToAdd.setTag(clusterToAdd);

        mMarkers.put(clusterToAdd, markerToAdd);
    }

    private void removeCluster(Cluster<T> clusterToRemove) {
        Marker markerToRemove = mMarkers.get(clusterToRemove);
        if (markerToRemove != null) {
            markerToRemove.setZIndex(BACKGROUND_MARKER_Z_INDEX);

            Cluster<T> parentCluster = findParentCluster(mClusters, clusterToRemove.getLatitude(),
                    clusterToRemove.getLongitude());
            if (parentCluster != null) {
                animateMarkerToLocation(markerToRemove, new LatLng(parentCluster.getLatitude(),
                        parentCluster.getLongitude()), true);
            } else {
                markerToRemove.remove();
            }

            mMarkers.remove(clusterToRemove);
        }
    }

    private MarkerOptions getMarkerOptions(Cluster<T> cluster) {
        BitmapDescriptor markerIcon = getMarkerIcon(cluster);
        String markerTitle = getMarkerTitle(cluster);
        String markerSnippet = getMarkerSnippet(cluster);

        return new MarkerOptions()
                .position(new LatLng(cluster.getLatitude(), cluster.getLongitude()))
                .icon(markerIcon)
                .title(markerTitle)
                .snippet(markerSnippet)
                .zIndex(FOREGROUND_MARKER_Z_INDEX);
    }

    @NonNull
    private BitmapDescriptor getMarkerIcon(@NonNull Cluster<T> cluster) {
        BitmapDescriptor clusterIcon;

        List<T> clusterItems = cluster.getItems();
        if (clusterItems.size() > 1) {
            clusterIcon = mIconGenerator.getClusterIcon(cluster);
        } else {
            clusterIcon = mIconGenerator.getClusterItemIcon(clusterItems.get(0));
        }

        return checkNotNull(clusterIcon);
    }

    @Nullable
    private String getMarkerTitle(@NonNull Cluster<T> cluster) {
        List<T> clusterItems = cluster.getItems();
        if (clusterItems.size() > 1) {
            return null;
        } else {
            return clusterItems.get(0).getTitle();
        }
    }

    @Nullable
    private String getMarkerSnippet(@NonNull Cluster<T> cluster) {
        List<T> clusterItems = cluster.getItems();
        if (clusterItems.size() > 1) {
            return null;
        } else {
            return clusterItems.get(0).getSnippet();
        }
    }

    @Nullable
    private Cluster<T> findParentCluster(@NonNull List<Cluster<T>> clusters,
                                         double latitude, double longitude) {
        for (Cluster<T> cluster : clusters) {
            if (cluster.contains(latitude, longitude)) {
                return cluster;
            }
        }

        return null;
    }

    private void animateMarkerToLocation(@NonNull final Marker marker, @NonNull LatLng targetLocation,
                                         final boolean removeAfter) {
        ObjectAnimator objectAnimator = ObjectAnimator.ofObject(marker, "position",
                new LatLngTypeEvaluator(), targetLocation);
        objectAnimator.setInterpolator(new FastOutSlowInInterpolator());
        objectAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (removeAfter) {
                    marker.remove();
                }
            }
        });
        objectAnimator.start();
    }

    private void animateMarkerAppearance(@NonNull Marker marker) {
        ObjectAnimator.ofFloat(marker, "alpha", 1.0F).start();
    }

    private static class LatLngTypeEvaluator implements TypeEvaluator<LatLng> {

        @Override
        public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
            double latitude = (endValue.latitude - startValue.latitude) * fraction + startValue.latitude;
            double longitude = (endValue.longitude - startValue.longitude) * fraction + startValue.longitude;
            return new LatLng(latitude, longitude);
        }
    }
}
