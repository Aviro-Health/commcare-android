package org.commcare.dalvik.activities;

import android.annotation.SuppressLint;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import org.commcare.android.adapters.HomeScreenAdapter;
import org.commcare.dalvik.R;
import org.commcare.dalvik.application.CommCareApplication;
import org.commcare.dalvik.preferences.CommCarePreferences;
import org.commcare.dalvik.preferences.DeveloperPreferences;
import org.commcare.suite.model.Profile;

import java.util.Vector;

/**
 * @author amstone326
 */
public class HomeActivityUIController {

    private final CommCareHomeActivity activity;

    private HomeScreenAdapter adapter;
    private View mTopBanner;

    public HomeActivityUIController(CommCareHomeActivity activity) {
        this.activity = activity;
        setupUI();
    }

    public View getTopBanner() {
        return mTopBanner;
    }

    private void setupUI() {
        activity.setContentView(R.layout.home_screen);
        adapter = new HomeScreenAdapter(activity, getHiddenButtons(), activity.isDemoUser());
        mTopBanner = View.inflate(activity, R.layout.grid_header_top_banner, null);
        setupGridView();
    }

    private static Vector<String> getHiddenButtons() {
        Vector<String> hiddenButtons = new Vector<>();

        Profile p = CommCareApplication._().getCommCarePlatform().getCurrentProfile();
        if ((p != null && !p.isFeatureActive(Profile.FEATURE_REVIEW)) || !CommCarePreferences.isSavedFormsEnabled()) {
            hiddenButtons.add("saved");
        }

        if (!CommCarePreferences.isIncompleteFormsEnabled()) {
            hiddenButtons.add("incomplete");
        }
        if (!DeveloperPreferences.isHomeReportEnabled()) {
            hiddenButtons.add("report");
        }

        return hiddenButtons;
    }

    private void setupGridView() {
        final RecyclerView grid = (RecyclerView)activity.findViewById(R.id.home_gridview_buttons);
        grid.setHasFixedSize(true);

        StaggeredGridLayoutManager gridView = new StaggeredGridLayoutManager(2, 1);
        grid.setLayoutManager(gridView);
        grid.setItemAnimator(null);
        grid.setAdapter(adapter);

        grid.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @SuppressLint("NewApi")
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    grid.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    grid.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }

                grid.requestLayout();
                adapter.notifyDataSetChanged(); // is going to populate the grid with buttons from the adapter (hardcoded there)
                configUI();
            }
        });
    }

    protected void configUI() {
        if (CommCareApplication._().getCurrentApp() != null) {
            activity.rebuildMenus();
        }
    }

    protected void refreshView() {
        activity.updateCommCareBanner();
        adapter.notifyDataSetChanged();
    }

    // TODO: Use syncNeeded flag to change color of sync message
    protected void displayMessage(String message, boolean syncNeeded, boolean suppressToast) {
        if (!suppressToast) {
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
        }
        // TODO PLM: somehow wire things up to update the sync button text
        //adapter.setNotificationTextForButton(R.layout.home_sync_button, message);
    }
}
