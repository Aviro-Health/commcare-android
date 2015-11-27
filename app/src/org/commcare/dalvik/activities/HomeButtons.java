package org.commcare.dalvik.activities;

import android.content.Intent;
import android.view.View;

import org.commcare.dalvik.R;
import org.commcare.dalvik.application.CommCareApplication;
import org.javarosa.core.services.locale.Localization;

import java.util.Vector;

/**
 * @author Phillip Mates (pmates@dimagi.com).
 */
public class HomeButtons {
    private static String[] buttonNames =
            new String[] { "start", "saved", "incomplete", "sync", "report", "logout" };

    public static HomeCardDisplayData[] buildButtonData(CommCareHomeActivity activity, Vector<String> buttonsToHide) {
        HomeCardDisplayData[] allButtons = new HomeCardDisplayData[]{
                new HomeCardDisplayData(Localization.get("home.start"),
                        R.color.white,
                        R.drawable.home_start,
                        R.color.cc_attention_positive_color,
                        getStartButtonListener(activity)),
                new HomeCardDisplayData(Localization.get("home.forms.saved"),
                        R.color.white,
                        R.drawable.home_saved,
                        R.color.cc_light_cool_accent_color,
                        getViewOldFormsListener(activity)),
                new HomeCardDisplayData(Localization.get("home.forms.incomplete"), R.color.white,
                        R.drawable.home_incomplete,
                        R.color.solid_dark_orange,
                        getIncompleteButtonListener(activity)),
                new HomeCardDisplayData(Localization.get("home.sync"), R.color.white,
                        "", R.color.white,
                        R.drawable.home_sync,
                        R.color.cc_brand_color,
                        R.color.cc_brand_text,
                        getSyncButtonListener(activity)),
                new HomeCardDisplayData(Localization.get("home.report"), R.color.white,
                        R.drawable.home_report, R.color.cc_attention_negative_color,
                        getReportButtonListener(activity)),
                new HomeCardDisplayData(Localization.get("home.logout"), R.color.white,
                        "Logged in as: ", R.color.white,
                        R.drawable.home_logout, R.color.cc_neutral_color, R.color.cc_neutral_text,
                        getLogoutButtonListener(activity)),
        };

        int visibleButtonCount = buttonNames.length - buttonsToHide.size();
        HomeCardDisplayData[] buttons = new HomeCardDisplayData[visibleButtonCount];
        int visibleIndex = 0;
        for (int i = 0; i <= visibleButtonCount; i++) {
            if (!buttonsToHide.contains(buttonNames[i])) {
                buttons[visibleIndex] = allButtons[i];
                visibleIndex++;
            }
        }
        return buttons;
    }

    private static View.OnClickListener getViewOldFormsListener(final CommCareHomeActivity activity) {
        return new View.OnClickListener() {
            public void onClick(View v) {
                activity.goToFormArchive(false);
            }
        };
    }

    private static View.OnClickListener getSyncButtonListener(final CommCareHomeActivity activity) {
        return new View.OnClickListener() {
            public void onClick(View v) {
                activity.syncButtonPressed();
            }
        };
    }

    private static View.OnClickListener getStartButtonListener(final CommCareHomeActivity activity) {
        return new View.OnClickListener() {
            public void onClick(View v) {
                activity.enterRootModule();
            }
        };
    }

    private static View.OnClickListener getIncompleteButtonListener(final CommCareHomeActivity activity) {
        return new View.OnClickListener() {
            public void onClick(View v) {
                activity.goToFormArchive(true);
            }
        };
    }

    private static View.OnClickListener getLogoutButtonListener(final CommCareHomeActivity activity) {
        return new View.OnClickListener() {
            public void onClick(View v) {
                CommCareApplication._().closeUserSession();
                activity.userTriggeredLogout();
            }
        };
    }

    private static View.OnClickListener getReportButtonListener(final CommCareHomeActivity activity) {
        return new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(activity, ReportProblemActivity.class);
                activity.startActivityForResult(i, CommCareHomeActivity.REPORT_PROBLEM_ACTIVITY);
            }
        };
    }

    public static class HomeCardDisplayData {
        public final int bgColor;
        public final int textColor;
        public final int imageResource;
        public final String text;
        public final String subText;
        public final int subTextColor;
        public final int subTextBgColor;
        public final View.OnClickListener listener;

        HomeCardDisplayData(String text, int textColor, int imageResource, int bgColor, View.OnClickListener listener) {
            this(text, textColor, "", R.color.white, imageResource, bgColor, R.color.cc_brand_color, listener);
        }

        HomeCardDisplayData(String text, int textColor, String subText, int subTextColor, int imageResource, int bgColor, int subTextBgColor, View.OnClickListener listener) {
            this.bgColor = bgColor;
            this.textColor = textColor;
            this.imageResource = imageResource;
            this.text = text;
            this.subText = subText;
            this.subTextColor = subTextColor;
            this.subTextBgColor = subTextBgColor;
            this.listener = listener;
        }
    }
}
