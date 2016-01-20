package org.commcare.dalvik.activities;

import android.annotation.TargetApi;
import android.location.Address;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

import org.commcare.android.database.SqlStorage;
import org.commcare.android.database.user.models.GeocodeCacheModel;
import org.commcare.android.models.Entity;
import org.commcare.android.models.NodeEntityFactory;
import org.commcare.android.util.AndroidInstanceInitializer;
import org.commcare.dalvik.R;
import org.commcare.dalvik.application.CommCareApplication;
import org.commcare.session.CommCareSession;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.SessionDatum;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.data.GeoPointData;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.services.storage.StorageFullException;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;

/**
 * @author ctsims
 */
@TargetApi(11)
public class EntityMapActivity extends FragmentActivity implements OnMapReadyCallback {
    private static final String TAG = EntityMapActivity.class.getSimpleName();

    private EvaluationContext entityEvaluationContext;
    private CommCareSession session;
    Vector<Entity<TreeReference>> entities;

    Vector<LatLng> locations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entity_map_view);
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        session = CommCareApplication._().getCurrentSession();
        SessionDatum selectDatum = session.getNeededDatum();
        Detail detail = session.getDetail(selectDatum.getShortDetail());
        NodeEntityFactory factory = new NodeEntityFactory(detail, this.getEvaluationContext());

        Vector<TreeReference> references = getEvaluationContext().expandReference(
                selectDatum.getNodeset());
        entities = new Vector<>();
        for(TreeReference ref : references) {
            entities.add(factory.getEntity(ref));
        }

        int bogusAddresses = 0;
        locations = new Vector<>();
        for(Entity<TreeReference> e : entities) {
            for(int i = 0 ; i < detail.getHeaderForms().length; ++i ){
                if("address".equals(detail.getTemplateForms()[i])) {
                    String address = e.getFieldString(i).trim();
                    if(address != null && !"".equals(address)) {
                        LatLng location = getLatLngFromAddress(address);
                        if (location == null) {
                            bogusAddresses++;
                        } else {
                            locations.add(location);
                        }
                    }
                }
            }
        }
        Log.d(TAG, "Loaded. " + locations.size() +" addresses discovered, " + bogusAddresses + " could not be located");
    }

    private LatLng getLatLngFromAddress(String address) {
        LatLng location = null;
        try {
            GeoPointData data = new GeoPointData().cast(new UncastData(address));
            if(data != null) {
                location = new LatLng(data.getLatitude(), data.getLongitude());
            }
        } catch(Exception ex) {
            //We might not have a geopoint at all. Don't even trip
        }
        return location;
//
//        boolean cached = false;
//        try {
//            GeocodeCacheModel record = geoCache.getRecordForValue(GeocodeCacheModel.META_LOCATION, address);
//            cached = true;
//            if(record.dataExists()){
//                gp = record.getGeoPoint();
//            }
//        } catch(NoSuchElementException nsee) {
//            //no record!
//        }
//
//        //If we don't have a geopoint, let's try to find our address
//        if (!cached && location != null) {
//            try {
//                List<Address> addresses = mGeoCoder.getFromLocationName(address, 3, boundHints[0], boundHints[1], boundHints[2], boundHints[3]);
//                for(Address a : addresses) {
//                    if(a.hasLatitude() && a.hasLongitude()) {
//                        int lat = (int) (a.getLatitude() * 1E6);
//                        int lng = (int) (a.getLongitude() * 1E6);
//                        gp = new GeoPoint(lat, lng);
//
//                        geoCache.write(new GeocodeCacheModel(address, lat, lng));
//                        legit++;
//                        break;
//                    }
//                }
//
//                //We didn't find an address, make a miss record
//                if(gp == null) {
//                    geoCache.write(GeocodeCacheModel.NoHitRecord(address));
//                }
//            } catch (StorageFullException | IOException e1) {
//                e1.printStackTrace();
//            }
//        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        for (LatLng location: locations) {
            map.addMarker(new MarkerOptions()
                    .position(location)
                    .title("Marker"));
        }
    }

    private EvaluationContext getEvaluationContext() {
        if(entityEvaluationContext == null) {
            entityEvaluationContext = session.getEvaluationContext(getInstanceInit());
        }
        return entityEvaluationContext;
    }

    private AndroidInstanceInitializer getInstanceInit() {
        return new AndroidInstanceInitializer(session);
    }
}
