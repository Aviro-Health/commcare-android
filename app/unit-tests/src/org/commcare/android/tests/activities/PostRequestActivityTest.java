package org.commcare.android.tests.activities;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.commcare.CommCareApplication;
import org.commcare.CommCareTestApplication;
import org.commcare.activities.PostRequestActivity;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.commcare.android.mocks.HttpURLConnectionMock;
import org.commcare.android.mocks.ModernHttpRequesterMock;
import org.commcare.android.util.ActivityLaunchUtils;
import org.commcare.android.util.TestAppInstaller;
import org.commcare.dalvik.R;
import org.commcare.models.AndroidSessionWrapper;
import org.commcare.modern.util.Pair;
import org.commcare.network.CommcareRequestEndpointsMock;
import org.commcare.network.LocalReferencePullResponseFactory;
import org.commcare.session.CommCareSession;
import org.commcare.session.RemoteQuerySessionManager;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.services.locale.Localization;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
@Config(application = CommCareTestApplication.class)
@RunWith(AndroidJUnit4.class)
public class PostRequestActivityTest {
    @Before
    public void setup() {
        TestAppInstaller.installAppAndLogin(
                "jr://resource/commcare-apps/case_search_and_claim/profile.ccpr",
                "test", "123");
    }

    @Test
    public void postingToNonHttpsURLTest() {
        String urlString = "http://bad.url.com";
        ModernHttpRequesterMock.setEnforceSecureEndpointValidation(true);
        PostRequestActivity postRequestActivity = buildPostActivity(urlString);
        assertErrorMessage(postRequestActivity, true, Localization.get("auth.request.not.using.https", urlString));
    }

    @Test
    public void postingToNonHttpsURLTest_WithDisabledSecureEndpointValidation() {
        String urlString = "http://bad.url.com";
        ModernHttpRequesterMock.setEnforceSecureEndpointValidation(false);
        PostRequestActivity postRequestActivity = buildPostActivity(urlString);
        assertErrorMessage(postRequestActivity, false, null);
    }

    private static void assertErrorMessage(PostRequestActivity postRequestActivity,
                                           boolean isVisible,
                                           String expectedErrorMessage) {
        TextView errorMessage =
                postRequestActivity.findViewById(R.id.error_message);
        if (isVisible) {
            assertEquals(View.VISIBLE, errorMessage.getVisibility());
        } else {
            assertFalse(View.VISIBLE == errorMessage.getVisibility());
        }
        if (expectedErrorMessage != null) {
            assertEquals(expectedErrorMessage, errorMessage.getText());
        }
    }

    private static PostRequestActivity buildPostActivity(String urlString) {
        URL url = stringToUrl(urlString);
        Intent postLaunchIntent = new Intent();
        if (url != null) {
            postLaunchIntent.putExtra(PostRequestActivity.URL_KEY, url);
            postLaunchIntent.putExtra(PostRequestActivity.PARAMS_KEY,
                    new HashMap<String, String>());
        }
        return Robolectric.buildActivity(PostRequestActivity.class, postLaunchIntent)
                .create().start().resume().get();
    }

    @Test
    public void postingWithoutExtrasTest() {
        PostRequestActivity postRequestActivity = buildPostActivity(null);

        assertErrorMessage(postRequestActivity, true, Localization.get("post.generic.error"));
    }

    @Test
    public void unknownResponseFromServerTest() {
        int unknownResponseCode = 711028100;
        String expectedErrorMessage =
                Localization.get("post.unknown.response", unknownResponseCode + "");
        assertPostFailureMessage(expectedErrorMessage, 711028100);
    }

    private static void assertPostFailureMessage(String expectedErrorMessage,
                                                 int responseCode) {
        ModernHttpRequesterMock.setResponseCodes(new Integer[]{responseCode});
        PostRequestActivity postRequestActivity = buildPostActivity("https://www.fake.com");
        assertErrorMessage(postRequestActivity, true, expectedErrorMessage);
    }

    @Test
    public void clientErrorResponseFromServerTest() {
        int responseCode = 400;
        assertPostFailureMessage(Localization.get("post.client.error", responseCode + ""), responseCode);
    }

    @Test
    public void clientGoneErrorTest() {
        int responseCode = 410;
        assertPostFailureMessage(Localization.get("post.gone.error"), responseCode);
    }

    @Test
    public void clientConflicErrorTest() {
        int responseCode = 409;
        assertPostFailureMessage(Localization.get("post.conflict.error"), responseCode);
    }

    @Test
    public void serverErrorResponseFromServerTest() {
        int responseCode = 550;
        assertPostFailureMessage(Localization.get("post.server.error", responseCode + ""), responseCode);
    }

    @Test
    public void ioErrorInResponseFromServerTest() {
        ModernHttpRequesterMock.setResponseCodes(new Integer[]{200});
        ModernHttpRequesterMock.setRequestPayloads(new String[]{null});
        assertPostFailureMessage(Localization.get("post.io.error", HttpURLConnectionMock.ioErrorMessage), 200);
    }

    @Test
    public void retryClaimTest() {
        ModernHttpRequesterMock.setResponseCodes(new Integer[]{500, 200});
        CommcareRequestEndpointsMock.setCaseFetchResponseCodes(new Integer[]{200});
        LocalReferencePullResponseFactory.setRequestPayloads(new String[]{"jr://resource/commcare-apps/case_search_and_claim/empty_restore.xml"});

        PostRequestActivity postRequestActivity = buildPostActivity("https://www.fake.com");

        Button retryButton = postRequestActivity.findViewById(R.id.request_button);
        assertEquals(View.VISIBLE, retryButton.getVisibility());
        retryButton.performClick();

        assertErrorMessage(postRequestActivity, false, null);

        assertTrue(postRequestActivity.isFinishing());
    }

    /**
     * Launch post request through session dispatch
     */
    @Test
    public void makeSuccessfulPostRequestTest() {
        ModernHttpRequesterMock.setResponseCodes(new Integer[]{200});
        CommcareRequestEndpointsMock.setCaseFetchResponseCodes(new Integer[]{200});
        LocalReferencePullResponseFactory.setRequestPayloads(new String[]{"jr://resource/commcare-apps/case_search_and_claim/empty_restore.xml"});

        AndroidSessionWrapper sessionWrapper =
                CommCareApplication.instance().getCurrentSessionWrapper();
        CommCareSession session = sessionWrapper.getSession();
        session.setCommand("patient-search");
        InputStream is =
                PostRequestActivity.class.getClassLoader().getResourceAsStream("commcare-apps/case_search_and_claim/good-query-result.xml");

        RemoteQuerySessionManager remoteQuerySessionManager =
                RemoteQuerySessionManager.buildQuerySessionManager(sessionWrapper.getSession(),
                        sessionWrapper.getEvaluationContext());
        Pair<ExternalDataInstance, String> instanceOrError =
                remoteQuerySessionManager.buildExternalDataInstance(is);
        session.setQueryDatum(instanceOrError.first);
        session.setDatum("case_id", "321");

        ShadowActivity shadowActivity =
                ActivityLaunchUtils.buildHomeActivity();

        Intent postActivityIntent = shadowActivity.getNextStartedActivity();

        String intentActivityName = postActivityIntent.getComponent().getClassName();
        assertTrue(intentActivityName.equals(PostRequestActivity.class.getName()));

        assertEquals("https://www.fake.com/claim_patient/", postActivityIntent.getSerializableExtra(PostRequestActivity.URL_KEY).toString());
        HashMap<String, String> postUrlParams =
                (HashMap<String, String>)postActivityIntent.getSerializableExtra(PostRequestActivity.PARAMS_KEY);
        assertEquals("321", postUrlParams.get("selected_case_id"));

        PostRequestActivity postRequestActivity =
                Robolectric.buildActivity(PostRequestActivity.class, postActivityIntent)
                        .create().start().resume().get();

        assertTrue(postRequestActivity.isFinishing());
    }

    private static URL stringToUrl(String urlAsString) {
        if (urlAsString == null) {
            return null;
        }
        try {
            return new URL(urlAsString);
        } catch (MalformedURLException e) {
            fail(e.getMessage());
            return null;
        }
    }
}
