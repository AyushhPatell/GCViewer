package com.tagtraum.perf.gcviewer.imp;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import com.tagtraum.perf.gcviewer.UnittestHelper;
import com.tagtraum.perf.gcviewer.model.AbstractGCEvent;
import com.tagtraum.perf.gcviewer.model.GCModel;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test unified java logging ZGC algorithm
 */
public class TestDataReaderUJLZGC {
    private static final int CONCURRENT_MARK_INDEX = 0;
    private static final int CONCURRENT_PROCESS_REFERENCES_INDEX = 1;
    private static final int CONCURRENT_RESET_RELOCATION_SET_INDEX = 2;
    private static final int CONCURRENT_DESTROY_DETACHED_PAGES_INDEX = 3;
    private static final int CONCURRENT_SELECT_RELOCATION_SET_INDEX = 4;
    private static final int CONCURRENT_PREPARE_RELOCATION_SET_INDEX = 5;
    private static final int CONCURRENT_RELOCATE_INDEX = 6;

    private GCModel gcAllModel;
    private GCModel gcDefaultModel;

    @Before
    public void setUp() throws Exception {
        gcAllModel = getGCModelFromLogFile("sample-ujl-zgc-gc-all.txt");
        gcDefaultModel = getGCModelFromLogFile("sample-ujl-zgc-gc-default.txt");
    }

    @After
    public void tearDown() {
        gcAllModel = null;
        gcDefaultModel = null;
    }

    private GCModel getGCModelFromLogFile(String fileName) throws IOException {
        return UnittestHelper.getGCModelFromLogFile(fileName, UnittestHelper.FOLDER.OPENJDK_UJL, DataReaderUnifiedJvmLogging.class);
    }

    @Test
    public void testGcAll() {
        assertThat("size", gcAllModel.size(), is(8));
        assertThat("amount of gc event types", gcAllModel.getGcEventPauses().size(), is(1));
        assertThat("amount of gc events", gcAllModel.getGCPause().getN(), is(1));
        assertThat("amount of full gc event types", gcAllModel.getFullGcEventPauses().size(), is(0));
        assertThat("amount of gc phases event types", gcAllModel.getGcEventPhases().size(), is(3));
        assertThat("amount of full gc events", gcAllModel.getFullGCPause().getN(), is(0));
        assertThat("amount of concurrent pause types", gcAllModel.getConcurrentEventPauses().size(), is(7));
    }

    @Test
    public void testGcAllGarbageCollection() {
        assertMemoryPauseEvent(gcAllModel.getLastEventAdded(), "Garbage Collection", AbstractGCEvent.Type.UJL_ZGC_GARBAGE_COLLECTION, 0.002653, 1024 * 10620, 1024 * 8800, 1024 * 194560);
    }

    @Test
    public void testGcAllPauseMarkStart() {
        AbstractGCEvent<?> pauseMarkStartEvent = gcAllModel.getGCEvents().next().getPhases().get(0);
        assertMemoryPauseEvent(pauseMarkStartEvent, "Pause Mark Start", AbstractGCEvent.Type.UJL_ZGC_PAUSE_MARK_START, 0.001279, 0, 0, 0);
    }

    @Test
    public void testGcAllConcurrentMark() {
        assertMemoryPauseEvent(gcAllModel.get(CONCURRENT_MARK_INDEX), "Concurrent Mark", AbstractGCEvent.Type.UJL_ZGC_CONCURRENT_MARK, 0.005216, 0, 0, 0);
    }

    @Test
    public void testGcAllPauseMarkEnd() {
        AbstractGCEvent<?> pauseMarkEndEvent = gcAllModel.getGCEvents().next().getPhases().get(1);
        assertMemoryPauseEvent(pauseMarkEndEvent, "Pause Mark End", AbstractGCEvent.Type.UJL_ZGC_PAUSE_MARK_END, 0.000695, 0, 0, 0);
    }

    @Test
    public void testGcAllConcurrentNonref() {
        assertMemoryPauseEvent(gcAllModel.get(CONCURRENT_PROCESS_REFERENCES_INDEX), "Concurrent Process Non-Strong References", AbstractGCEvent.Type.UJL_ZGC_CONCURRENT_NONREF, 0.000258, 0, 0, 0);
    }

    @Test
    public void testGcAllConcurrentResetRelocSet() {
        assertMemoryPauseEvent(gcAllModel.get(CONCURRENT_RESET_RELOCATION_SET_INDEX), "Concurrent Reset Relocation Set", AbstractGCEvent.Type.UJL_ZGC_CONCURRENT_RESET_RELOC_SET, 0.000001, 0, 0, 0);
    }

    @Test
    public void testGcAllConcurrentDetachedPages() {
        assertMemoryPauseEvent(gcAllModel.get(CONCURRENT_DESTROY_DETACHED_PAGES_INDEX), "Concurrent Destroy Detached Pages", AbstractGCEvent.Type.UJL_ZGC_CONCURRENT_DETATCHED_PAGES, 0.000001, 0, 0, 0);
    }

    @Test
    public void testGcAllConcurrentSelectRelocSet() {
        assertMemoryPauseEvent(gcAllModel.get(CONCURRENT_SELECT_RELOCATION_SET_INDEX), "Concurrent Select Relocation Set", AbstractGCEvent.Type.UJL_ZGC_CONCURRENT_SELECT_RELOC_SET, 0.003822, 0, 0, 0);
    }

    @Test
    public void testGcAllConcurrentPrepareRelocSet() {
        assertMemoryPauseEvent(gcAllModel.get(CONCURRENT_PREPARE_RELOCATION_SET_INDEX), "Concurrent Prepare Relocation Set", AbstractGCEvent.Type.UJL_ZGC_CONCURRENT_PREPARE_RELOC_SET, 0.000865, 0, 0, 0);
    }

    @Test
    public void testGcAllPauseRelocateStart() {
        AbstractGCEvent<?> pauseRelocateStartEvent = gcAllModel.getGCEvents().next().getPhases().get(2);
        assertMemoryPauseEvent(pauseRelocateStartEvent, "Pause Relocate Start", AbstractGCEvent.Type.UJL_ZGC_PAUSE_RELOCATE_START, 0.000679, 0, 0, 0);
    }

    @Test
    public void testGcAllConcurrentRelocate() {
        assertMemoryPauseEvent(gcAllModel.get(CONCURRENT_RELOCATE_INDEX), "Concurrent Relocate", AbstractGCEvent.Type.UJL_ZGC_CONCURRENT_RELOCATE, 0.002846, 0, 0, 0);
    }

    @Test
    public void testGcDefault() {
        assertThat("size", gcDefaultModel.size(), is(5));
        assertThat("amount of STW GC pause types", gcDefaultModel.getGcEventPauses().size(), is(5));
        assertThat("amount of STW Full GC pause types", gcDefaultModel.getFullGcEventPauses().size(), is(0));
        assertThat("amount of concurrent pause types", gcDefaultModel.getConcurrentEventPauses().size(), is(0));
    }

    @Test
    public void testGcDefaultMetadataGcThreshold() {
        assertMemoryPauseEvent(gcDefaultModel.get(0), "Metadata GC Threshold heap", AbstractGCEvent.Type.UJL_ZGC_GARBAGE_COLLECTION, 0, 1024 * 106, 1024 * 88, 0);
    }

    @Test
    public void testGcDefaultWarmup() {
        assertMemoryPauseEvent(gcDefaultModel.get(1), "Warmup heap", AbstractGCEvent.Type.UJL_ZGC_GARBAGE_COLLECTION, 0, 1024 * 208, 1024 * 164, 1024 * 164 / 16 * 100);
    }

    @Test
    public void testGcDefaultProactive() {
        assertMemoryPauseEvent(gcDefaultModel.get(2), "Proactive heap", AbstractGCEvent.Type.UJL_ZGC_GARBAGE_COLLECTION, 0, 1024 * 19804, 1024 * 20212, 20212 * 1024 / 10 * 100);
    }

    @Test
    public void testGcDefaultAllocationRate() {
        assertMemoryPauseEvent(gcDefaultModel.get(3), "Allocation Rate heap", AbstractGCEvent.Type.UJL_ZGC_GARBAGE_COLLECTION, 0, 1024 * 502, 1024 * 716, 716 * 1024 / 70 * 100);
    }

    @Test
    public void testDefaultGcSystemGc() {
        assertMemoryPauseEvent(gcDefaultModel.get(4), "System.gc() heap", AbstractGCEvent.Type.UJL_ZGC_GARBAGE_COLLECTION, 0, 1024 * 10124, 1024 * 5020, 5020 * 1024 / 5 * 100);
    }

    @Test
    public void testGcOther() throws Exception {
        GCModel model = getGCModelFromLogFile("sample-ujl-zgc-gc-other.txt");

        assertThat("size", model.size(), is(21));
        assertThat("amount of gc event types", model.getGcEventPauses().size(), is(15));
        assertThat("amount of gc events", model.getGCPause().getN(), is(15));
        assertThat("amount of full gc event types", model.getFullGcEventPauses().size(), is(0));
        assertThat("amount of gc phases event types", model.getGcEventPhases().size(), is(3));
        assertThat("amount of full gc events", model.getFullGCPause().getN(), is(0));
        assertThat("amount of concurrent pause types", model.getConcurrentEventPauses().size(), is(6));
        assertThat("total heap size", model.getHeapAllocatedSizes().getMax(), is(3884 * 1024));

        assertMemoryPauseEvent(model.get(5), "Allocation Stall", AbstractGCEvent.Type.UJL_ZGC_ALLOCATION_STALL, 0.029092, 0, 0, 0);
        assertMemoryPauseEvent(model.get(12), "Relocation Stall", AbstractGCEvent.Type.UJL_ZGC_RELOCATION_STALL, 0.000720, 0, 0, 0);
        assertMemoryPauseEvent(model.get(2), "Concurrent Mark Free", AbstractGCEvent.Type.UJL_ZGC_CONCURRENT_MARK_FREE, 0.000001, 0, 0, 0);
    }

    private void assertMemoryPauseEvent(AbstractGCEvent<?> event, String eventName, AbstractGCEvent.Type expectedType, double expectedPause, long expectedBefore, long expectedAfter, long expectedTotal) {
        UnittestHelper.testMemoryPauseEvent((AbstractGCEvent<?>) event, eventName, expectedType, expectedPause, (int) expectedBefore, (int) expectedAfter, (int) expectedTotal, AbstractGCEvent.Generation.TENURED, false);
    }
}
