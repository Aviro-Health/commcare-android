package org.commcare.android.tests.processing;

import org.commcare.CommCareApplication;
import org.commcare.CommCareTestApplication;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.commcare.android.util.TestUtils;
import org.commcare.cases.model.Case;
import org.commcare.android.database.user.models.ACase;
import org.commcare.cases.util.CaseDBUtils;
import org.commcare.engine.cases.CaseUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;

/**
 * @author ctsims
 */
@Config(application = CommCareTestApplication.class)
@RunWith(AndroidJUnit4.class)
public class ProcessingTest {

    @Before
    public void setupTests() {
        TestUtils.initializeStaticTestStorage();
    }

    @Test
    public void testIndexRemoval() {
        TestUtils.processResourceTransaction("/inputs/case_create.xml");
        Case c = TestUtils.getCaseStorage().getRecordForValue(ACase.INDEX_CASE_ID, "test_case_id");
        assertEquals("Case Name", "Test Case", c.getName());
        assertEquals("Case Property", "initial", c.getPropertyString("test_value"));

        TestUtils.processResourceTransaction("/inputs/case_update.xml");
        Case c2 = TestUtils.getCaseStorage().getRecordForValue(ACase.INDEX_CASE_ID, "test_case_id");
        assertEquals("Updated", "changed", c2.getPropertyString("test_value"));

        TestUtils.processResourceTransaction("/inputs/case_create_and_index.xml");
        Case c3 = TestUtils.getCaseStorage().getRecordForValue(ACase.INDEX_CASE_ID, "test_case_id_child");
        assertEquals("Indexed", "test_case_id", c3.getIndices().elementAt(0).getTarget());

        TestUtils.processResourceTransaction("/inputs/case_break_index.xml");
        Case c4 = TestUtils.getCaseStorage().getRecordForValue(ACase.INDEX_CASE_ID, "test_case_id_child");
        assertEquals("Removed Index Count", 0, c4.getIndices().size());
    }

    @Test
    public void testTypeChange() {
        TestUtils.processResourceTransaction("/inputs/case_create.xml");
        Case c = TestUtils.getCaseStorage().getRecordForValue(ACase.INDEX_CASE_ID, "test_case_id");
        assertEquals("Initial Type", "unit_test", c.getTypeId());

        TestUtils.processResourceTransaction("/inputs/case_change_type.xml");
        Case c2 = TestUtils.getCaseStorage().getRecordForValue(ACase.INDEX_CASE_ID, "test_case_id");
        assertEquals("Changed Type", "changed_unit_test", c2.getTypeId());
    }

    @Test
    public void testHashingParity() {
        assertHashesEqual();
        TestUtils.processResourceTransaction("/inputs/case_create.xml");
        assertHashesEqual();
        TestUtils.processResourceTransaction("/inputs/case_update.xml");
        assertHashesEqual();
        TestUtils.processResourceTransaction("/inputs/case_create_and_index.xml");
        assertHashesEqual();
    }

    private void assertHashesEqual() {
        String rawCalcHash = CaseDBUtils.computeCaseDbHash(TestUtils.getCaseStorage());

        String fastCalcHash = CaseUtils.computeCaseDbHash(TestUtils.getCaseStorage());

        assertEquals("fasthash incorrect", rawCalcHash, fastCalcHash);
    }
}
