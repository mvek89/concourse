package org.cinchapi.concourse;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.cinchapi.concourse.testing.Variables;
import org.cinchapi.concourse.time.Time;
import org.cinchapi.concourse.util.StandardActions;
import org.cinchapi.concourse.util.TestData;
import org.junit.Test;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 * Tests new API named chronologize which returns a mapping from 
 * from each timestamp to each non-empty set of values over time.
 * 
 * @author knd
 *
 */
public class ChronologizeTest extends ConcourseIntegrationTest {

    @Test
    public void testChronologizeIsEmptyForNonExistingKeyInRecord() {
        long record = Variables.register("record", client.create());
        String key = Variables.register("key", TestData.getString());
        String diffKey = Variables.register("diffKey", null);
        Object diffValue = Variables.register("diffValue", TestData.getObject());
        while (diffKey == null || key.equals(diffKey)) {
            diffKey = TestData.getString();
        }
        client.add(diffKey, diffValue, record);
        assertTrue(client.chronologize(key, record).isEmpty());
    }

    @Test
    public void testChronologizeWhenNoRemovalHasHappened() {
        long record = Variables.register("record", client.create());
        String key = Variables.register("key", TestData.getString());
        int testSize = Variables.register("testSize", 5);
        Set<Object> initValues = Variables.register("initValues", Sets.newHashSet());
        for (int i = 0; i < testSize; i++) {
            Object value = null;
            while (value == null || initValues.contains(value)) {
                value = TestData.getObject();
            }
            initValues.add(value);
            client.add(key, value, record);
        }
        Map<Timestamp, Set<Object>> result = client.chronologize(key, record);
        assertEquals(testSize, result.size());
        Iterator<Map.Entry<Timestamp, Set<Object>>> setIter = result.entrySet().iterator();
        for (int i = 0; i < testSize; i++) {
            assertEquals(i + 1, setIter.next().getValue().size());
        }
    }

    @Test
    public void testChronologizeWhenRemovalHasHappened() {
        long record = Variables.register("record", client.create());
        String key = Variables.register("key", TestData.getString());
        int testSize = Variables.register("testSize", 5);
        Set<Object> initValues = Variables.register("initValues", Sets.newHashSet());
        List<Object> listOfValues = new ArrayList<Object>();
        Map<Timestamp, Set<Object>> result = null;
        for (int i = 0; i < testSize; i++) {
            Object value = null;
            while (value == null || initValues.contains(value)) {
                value = TestData.getObject();
            }
            initValues.add(value);
            listOfValues.add(value);
            client.add(key, value, record);
        }
        int expectedMapSize = testSize;
        int expectedLastSetSize = testSize;
        Set<Object> lastValueSet = null;
        // remove 1 value
        expectedMapSize += 1;
        expectedLastSetSize -= 1;
        client.remove(key, listOfValues.get(2), record);
        result = client.chronologize(key, record);
        assertEquals(expectedMapSize, result.size());
        lastValueSet = Iterables.getLast((Iterable<Set<Object>>) result.values());
        assertEquals(expectedLastSetSize, lastValueSet.size());

        // remove 2 values
        expectedMapSize += 2;
        expectedLastSetSize -= 2;
        client.remove(key, listOfValues.get(0), record);
        client.remove(key, listOfValues.get(4), record);
        result = client.chronologize(key, record);
        assertEquals(expectedMapSize, result.size());
        lastValueSet = Iterables.getLast((Iterable<Set<Object>>) result.values());
        assertEquals(expectedLastSetSize, lastValueSet.size());

        // add 1 value
        expectedMapSize += 1;
        expectedLastSetSize += 1;
        client.add(key, listOfValues.get(2), record);
        result = client.chronologize(key, record);
        assertEquals(expectedMapSize, result.size());
        lastValueSet = Iterables.getLast((Iterable<Set<Object>>) result.values());
        assertEquals(expectedLastSetSize, lastValueSet.size());

        // clear all values
        expectedMapSize += expectedLastSetSize - 1; // last empty set filtered out
        expectedLastSetSize = 1;
        client.clear(key, record);
        result = client.chronologize(key, record);
        assertEquals(expectedMapSize, result.size());
        lastValueSet = Iterables.getLast((Iterable<Set<Object>>) result.values());
        assertEquals(expectedLastSetSize, lastValueSet.size());
    }

    @Test
    public void testChronologizeIsNotAffectedByAddingValueAlreadyInKeyInRecord() {
        long record = Variables.register("record", client.create());
        String key = Variables.register("key", TestData.getString());
        int testSize = Variables.register("testSize", 5);
        Set<Object> initValues = Variables.register("initValues", Sets.newHashSet());
        List<Object> listOfValues = new ArrayList<Object>();
        Map<Timestamp, Set<Object>> result = null;
        for (int i = 0; i < testSize; i++) {
            Object value = null;
            while (value == null || initValues.contains(value)) {
                value = TestData.getObject();
            }
            initValues.add(value);
            listOfValues.add(value);
            client.add(key, value, record);
        }
        int expectedMapSize = testSize;
        int expectedLastSetSize = testSize;
        Set<Object> lastValueSet = null;
        // add 1 already existed value
        client.add(key, listOfValues.get(2), record);
        result = client.chronologize(key, record);
        assertEquals(expectedMapSize, result.size());
        lastValueSet = Iterables.getLast((Iterable<Set<Object>>) result.values());
        assertEquals(expectedLastSetSize, lastValueSet.size());
    }

    @Test
    public void testChronologizeIsNotAffectedByRemovingValueNotInKeyInRecord() {
        long record = Variables.register("record", client.create());
        String key = Variables.register("key", TestData.getString());
        int testSize = Variables.register("testSize", 5);
        Set<Object> initValues = Variables.register("initValues", Sets.newHashSet());
        Map<Timestamp, Set<Object>> result = null;
        for (int i = 0; i < testSize; i++) {
            Object value = null;
            while (value == null || initValues.contains(value)) {
                value = TestData.getObject();
            }
            initValues.add(value);
            client.add(key, value, record);
        }
        int expectedMapSize = testSize;
        int expectedLastSetSize = testSize;
        Set<Object> lastValueSet = null;
        // remove 1 non-existing value
        Object nonValue = null;
        while (nonValue == null || initValues.contains(nonValue)) {
            nonValue = TestData.getObject();
        }
        client.remove(key, nonValue, record);
        result = client.chronologize(key, record);
        assertEquals(expectedMapSize, result.size());
        lastValueSet = Iterables.getLast((Iterable<Set<Object>>) result.values());
        assertEquals(expectedLastSetSize, lastValueSet.size());
    }

    @Test
    public void testChronologizeHasFilteredOutEmptyValueSets() {
        long record = Variables.register("record", client.create());
        String key = Variables.register("key", TestData.getString());
        int testSize = Variables.register("testSize", 5);
        Set<Object> initValues = Variables.register("initValues", Sets.newHashSet());
        Map<Timestamp, Set<Object>> result = null;
        for (int i = 0; i < testSize; i++) {
            Object value = null;
            while (value == null || initValues.contains(value)) {
                value = TestData.getObject();
            }
            initValues.add(value);
            client.set(key, value, record);
        }
        result = client.chronologize(key, record);
        for (Set<Object> values : result.values()) {
            assertFalse(values.isEmpty());
        }
    }

    @Test
    public void testChronologizeWithStartTimestampAndEndTimestampBeforeAnyValuesChangeInKeyInRecord() {
        long record = Variables.register("record", client.create());
        String key = Variables.register("key", TestData.getString());
        int testSize = Variables.register("testSize", 5);
        Set<Object> initValues = Variables.register("initValues", Sets.newHashSet());
        Timestamp startTimestamp = Variables.register("startTimestamp", Timestamp.now());
        StandardActions.wait(200, TimeUnit.MILLISECONDS);
        Timestamp endTimestamp = Variables.register("endTimestamp", Timestamp.now());
        StandardActions.wait(200, TimeUnit.MILLISECONDS);
        for (int i = 0; i < testSize; i++) {
            Object value = null;
            while (value == null || initValues.contains(value)) {
                value = TestData.getObject();
            }
            initValues.add(value);
            client.add(key, value, record);
        }
        Map<Timestamp, Set<Object>> result = client.chronologize(key, record, startTimestamp, endTimestamp);
        result = Variables.register("result", result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testChronologizeWithStartTimestampBeforeAndEndTimestampAfterAnyValuesChangeInKeyInRecord() {
        long record = Variables.register("record", client.create());
        String key = Variables.register("key", TestData.getString());
        int testSize = Variables.register("testSize", 5);
        Set<Object> initValues = Variables.register("initValues", Sets.newHashSet());
        Timestamp startTimestamp = Variables.register("startTimestamp", Timestamp.now());
        StandardActions.wait(200, TimeUnit.MILLISECONDS);
        for (int i = 0; i < testSize; i++) {
            Object value = null;
            while (value == null || initValues.contains(value)) {
                value = TestData.getObject();
            }
            initValues.add(value);
            client.add(key, value, record);
        }
        Timestamp endTimestamp = Variables.register("endTimestamp", Timestamp.now());
        StandardActions.wait(200, TimeUnit.MILLISECONDS);
        for (int i = 0; i < testSize; i++) {
            Object value = null;
            while (value == null || initValues.contains(value)) {
                value = TestData.getObject();
            }
            initValues.add(value);
            client.add(key, value, record);
        }
        Map<Timestamp, Set<Object>> result = client.chronologize(key, record, startTimestamp, endTimestamp);
        Set<Object> lastResultSet = Iterables.getLast(result.values());
        result = Variables.register("result", result);
        assertEquals(testSize, result.size());
        assertEquals(testSize, lastResultSet.size());
    }

    @Test
    public void testChronologizeWithStartTimestampAndEndTimestampAfterAnyValuesChangeInKeyInRecord() {
        long record = Variables.register("record", client.create());
        String key = Variables.register("key", TestData.getString());
        int testSize = Variables.register("testSize", 5);
        Set<Object> initValues = Variables.register("initValues", Sets.newHashSet());
        for (int i = 0; i < testSize; i++) {
            Object value = null;
            while (value == null || initValues.contains(value)) {
                value = TestData.getObject();
            }
            initValues.add(value);
            client.add(key, value, record);
        }
        Timestamp startTimestamp = Variables.register("startTimestamp", Timestamp.now());
        StandardActions.wait(200, TimeUnit.MILLISECONDS);
        for (int i = 0; i < testSize; i++) {
            Object value = null;
            while (value == null || initValues.contains(value)) {
                value = TestData.getObject();
            }
            initValues.add(value);
            client.add(key, value, record);
        }
        Timestamp endTimestamp = Variables.register("endTimestamp", Timestamp.now());
        StandardActions.wait(200, TimeUnit.MILLISECONDS);
        for (int i = 0; i < testSize; i++) {
            Object value = null;
            while (value == null || initValues.contains(value)) {
                value = TestData.getObject();
            }
            initValues.add(value);
            client.add(key, value, record);
        }
        Map<Timestamp, Set<Object>> result = client.chronologize(key, record, startTimestamp, endTimestamp);
        Set<Object> lastResultSet = Iterables.getLast(result.values());
        assertEquals(testSize + 1, result.size());
        assertEquals(testSize * 2, lastResultSet.size());
    }

    @Test
    public void testChronolgizeWithStartTimestampAsEpochAndEndTimestampAsNowInKeyInRecord() {
        long record = Variables.register("record", client.create());
        String key = Variables.register("key", TestData.getString());
        int testSize = Variables.register("testSize", 5);
        Set<Object> initValues = Variables.register("initValues", Sets.newHashSet());
        for (int i = 0; i < testSize; i++) {
            Object value = null;
            while (value == null || initValues.contains(value)) {
                value = TestData.getObject();
            }
            initValues.add(value);
            client.add(key, value, record);
        }
        client.set(key, TestData.getObject(), record);
        Timestamp epoch = Variables.register("epochTimestamp", Timestamp.epoch());
        Timestamp now = Variables.register("nowTimestamp", Timestamp.now());
        Map<Timestamp, Set<Object>> result = client.chronologize(key, record, epoch, now);
        result = Variables.register("result", result);
        Set<Object> lastResultSet = Iterables.getLast(result.values());
        assertEquals(testSize * 2, result.size());
        assertEquals(1, lastResultSet.size());
    }

    @Test
    public void testChronologizeWithEndTimestampIsExclusiveAtExactFirstValueChangeInKeyInRecord() {
        long record = Variables.register("record", client.create());
        String key = Variables.register("key", TestData.getString());
        int testSize = Variables.register("testSize", 5);
        Set<Object> initValues = Variables.register("initValues", Sets.newHashSet());
        List<Timestamp> timestamps = new ArrayList<Timestamp>();
        for (int i = 0; i < testSize; i++) {
            Object value = null;
            while (value == null || initValues.contains(value)) {
                value = TestData.getObject();
            }
            initValues.add(value);
            client.add(key, value, record);
            timestamps.add(Timestamp.now());
            StandardActions.wait(200, TimeUnit.MILLISECONDS);
        }
        Map<Timestamp, Set<Object>> chronology = client.chronologize(key, record);
        Timestamp exactStartTimestamp = Variables.register("exactStartTimestamp", Iterables.getFirst((Iterable<Timestamp>) chronology.keySet(), null));
        Map<Timestamp, Set<Object>> result = client.chronologize(key, record, Timestamp.epoch(), exactStartTimestamp);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testChronologizeWithEndTimestampIsExclusiveAfterFirstValueChangeInKeyInRecord() {
        long record = Variables.register("record", client.create());
        String key = Variables.register("key", TestData.getString());
        int testSize = Variables.register("testSize", 5);
        Set<Object> initValues = Variables.register("initValues", Sets.newHashSet());
        List<Timestamp> timestamps = new ArrayList<Timestamp>();
        for (int i = 0; i < testSize; i++) {
            Object value = null;
            while (value == null || initValues.contains(value)) {
                value = TestData.getObject();
            }
            initValues.add(value);
            client.add(key, value, record);
            timestamps.add(Timestamp.now());
            StandardActions.wait(200, TimeUnit.MILLISECONDS);
        }
        Map<Timestamp, Set<Object>> result = client.chronologize(key, record, Timestamp.epoch(), timestamps.get(0));
        Set<Object> lastResultSet = Iterables.getLast(result.values());
        result = Variables.register("result", result);
        assertEquals(1, result.size());
        assertEquals(1, lastResultSet.size());
    }

    @Test
    public void testChronologizeWithStartTimestampIsInclusiveAtExactFirstValueChangeInKeyInRecord() {
        long record = Variables.register("record", client.create());
        String key = Variables.register("key", TestData.getString());
        int testSize = Variables.register("testSize", 5);
        Set<Object> initValues = Variables.register("initValues", Sets.newHashSet());
        List<Timestamp> timestamps = new ArrayList<Timestamp>();
        for (int i = 0; i < testSize; i++) {
            Object value = null;
            while (value == null || initValues.contains(value)) {
                value = TestData.getObject();
            }
            initValues.add(value);
            client.add(key, value, record);
            timestamps.add(Timestamp.now());
            StandardActions.wait(200, TimeUnit.MILLISECONDS);
        }
        Map<Timestamp, Set<Object>> chronology = client.chronologize(key, record);
        Timestamp exactStartTimestamp = Variables.register("exactStartTimestamp", Iterables.getFirst((Iterable<Timestamp>) chronology.keySet(), null));
        Map<Timestamp, Set<Object>> result = client.chronologize(key, record, exactStartTimestamp, timestamps.get(0));
        Set<Object> lastResultSet = Iterables.getLast(result.values());
        assertEquals(1, result.size());
        assertEquals(1, lastResultSet.size());
    }

    @Test
    public void testChronologizeWithStartTimestampIsInclusiveAtExactLastValueChangeInKeyInRecord() {
        long record = Variables.register("record", client.create());
        String key = Variables.register("key", TestData.getString());
        int testSize = Variables.register("testSize", 5);
        Set<Object> initValues = Variables.register("initValues", Sets.newHashSet());
        List<Timestamp> timestamps = new ArrayList<Timestamp>();
        for (int i = 0; i < testSize; i++) {
            Object value = null;
            while (value == null || initValues.contains(value)) {
                value = TestData.getObject();
            }
            initValues.add(value);
            client.add(key, value, record);
            timestamps.add(Timestamp.now());
            StandardActions.wait(200, TimeUnit.MILLISECONDS);
        }
        Map<Timestamp, Set<Object>> chronologie = client.chronologize(key, record);
        Timestamp exactEndTimestamp = Variables.register("exactEndTimestamp", Iterables.getLast((Iterable<Timestamp>) chronologie.keySet()));
        Map<Timestamp, Set<Object>> result = client.chronologize(key, record, exactEndTimestamp, Timestamp.now());
        Set<Object> lastResultSet = Iterables.getLast(result.values());
        assertEquals(1, result.size());
        assertEquals(testSize, lastResultSet.size());
    }

    @Test
    public void testChronologizeWithStartTimestampIsInclusiveAfterLastValueChangeInKeyInRecord() {
        long record = Variables.register("record", client.create());
        String key = Variables.register("key", TestData.getString());
        int testSize = Variables.register("testSize", 5);
        Set<Object> initValues = Variables.register("initValues", Sets.newHashSet());
        List<Timestamp> timestamps = new ArrayList<Timestamp>();
        for (int i = 0; i < testSize; i++) {
            Object value = null;
            while (value == null || initValues.contains(value)) {
                value = TestData.getObject();
            }
            initValues.add(value);
            client.add(key, value, record);
            timestamps.add(Timestamp.now());
            StandardActions.wait(200, TimeUnit.MILLISECONDS);
        }
        Map<Timestamp, Set<Object>> result = client.chronologize(key, record, timestamps.get(testSize - 1), Timestamp.now());
        Set<Object> lastResultSet = Iterables.getLast(result.values());
        assertEquals(1, result.size());
        assertEquals(testSize, lastResultSet.size());
    }

    @Test
    public void testChronologizeWithStartTimestampEqualsEndTimestampBeforeFirstValueChangeInKeyInRecord() {
        long record = Variables.register("record", client.create());
        String key = Variables.register("key", TestData.getString());
        int testSize = Variables.register("testSize", 5);
        Set<Object> initValues = Variables.register("initValues", Sets.newHashSet());
        List<Timestamp> timestamps = new ArrayList<Timestamp>();
        for (int i = 0; i < testSize; i++) {
            Object value = null;
            while (value == null || initValues.contains(value)) {
                value = TestData.getObject();
            }
            initValues.add(value);
            client.add(key, value, record);
            timestamps.add(Timestamp.now());
            StandardActions.wait(200, TimeUnit.MILLISECONDS);
        }
        // check same timestamps before initial add
        Map<Timestamp, Set<Object>> result = client.chronologize(key, record, Timestamp.epoch(), Timestamp.epoch());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testChronologizeWithStartTimestampEqualsEndTimestampAtExactFirstValueChangeInKeyInRecord() {
        long record = Variables.register("record", client.create());
        String key = Variables.register("key", TestData.getString());
        int testSize = Variables.register("testSize", 5);
        Set<Object> initValues = Variables.register("initValues", Sets.newHashSet());
        List<Timestamp> timestamps = new ArrayList<Timestamp>();
        for (int i = 0; i < testSize; i++) {
            Object value = null;
            while (value == null || initValues.contains(value)) {
                value = TestData.getObject();
            }
            initValues.add(value);
            client.add(key, value, record);
            timestamps.add(Timestamp.now());
            StandardActions.wait(200, TimeUnit.MILLISECONDS);
        }
        Map<Timestamp, Set<Object>> chronologie = client.chronologize(key, record);
        Timestamp exactStartTimestamp = Iterables.getFirst(chronologie.keySet(), null);
        Map<Timestamp, Set<Object>> result = client.chronologize(key, record, exactStartTimestamp, exactStartTimestamp);
        Set<Object> lastResultSet = Iterables.getLast(result.values());
        assertEquals(1, result.size());
        assertEquals(1, lastResultSet.size());
    }

    @Test
    public void testChronologizeWithStartTimestampEqualsEndTimestampAfterLastValueChangeInKeyInRecord() {
        long record = Variables.register("record", client.create());
        String key = Variables.register("key", TestData.getString());
        int testSize = Variables.register("testSize", 5);
        Set<Object> initValues = Variables.register("initValues", Sets.newHashSet());
        List<Timestamp> timestamps = new ArrayList<Timestamp>();
        for (int i = 0; i < testSize; i++) {
            Object value = null;
            while (value == null || initValues.contains(value)) {
                value = TestData.getObject();
            }
            initValues.add(value);
            client.add(key, value, record);
            timestamps.add(Timestamp.now());
            StandardActions.wait(200, TimeUnit.MILLISECONDS);
        }
        Map<Timestamp, Set<Object>> result = client.chronologize(key, record, Timestamp.now(), Timestamp.now());
        Set<Object> lastResultSet = Iterables.getLast(result.values());
        assertEquals(1, result.size());
        assertEquals(testSize, lastResultSet.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testChronologizeWithStartTimestampGreaterThanEndTimestampInKeyInRecord() {
        long record = Variables.register("record", client.create());
        String key = Variables.register("key", TestData.getString());
        int testSize = Variables.register("testSize", 5);
        Set<Object> initValues = Variables.register("initValues", Sets.newHashSet());
        List<Timestamp> timestamps = new ArrayList<Timestamp>();
        for (int i = 0; i < testSize; i++) {
            Object value = null;
            while (value == null || initValues.contains(value)) {
                value = TestData.getObject();
            }
            initValues.add(value);
            client.add(key, value, record);
            timestamps.add(Timestamp.now());
            StandardActions.wait(200, TimeUnit.MILLISECONDS);
        }
        client.chronologize(key, record, timestamps.get(3), timestamps.get(2));
    }
    
    @Test
    public void testFindIndexOfNearestSuccessorTimestampWithStartTimestampLessThanFirstTimestampInChronology() {
        Set<Timestamp> timestamps = new LinkedHashSet<Timestamp>();
        timestamps.add(Timestamp.fromMicros(1000L));
        for (int i = 0; i < TestData.getScaleCount(); i++) {
            long increment = 0;
            while (increment == 0) {
                increment = Math.abs(TestData.getScaleCount());
            }
            timestamps.add(Timestamp.fromMicros(
                    Iterables.getLast(timestamps).getMicros() + increment));            
        }
        timestamps = Variables.register("timestamps", timestamps);
        Timestamp startTimestamp = Timestamp.epoch();
        assertEquals(0, client.
                findIndexOfNearestSuccessorTimestamp(timestamps, startTimestamp));
    }
    
    @Test
    public void testFindIndexOfNearestSuccessorTimestampWithStartTimestampGreaterThanLastTimestampInChronology() {
        Set<Timestamp> timestamps = new LinkedHashSet<Timestamp>();
        timestamps.add(Timestamp.fromMicros(1000L));
        for (int i = 0; i < TestData.getScaleCount(); i++) {
            long increment = 0;
            while (increment == 0) {
                increment = Math.abs(TestData.getScaleCount());
            }
            timestamps.add(Timestamp.fromMicros(
                    Iterables.getLast(timestamps).getMicros() + increment));            
        }
        timestamps = Variables.register("timestamps", timestamps);
        Variables.register("timestampsSize", timestamps.size());
        Timestamp startTimestamp = Timestamp.fromMicros(
                Iterables.getLast(timestamps).getMicros() + 1000L);
        assertEquals(timestamps.size(), client.
                findIndexOfNearestSuccessorTimestamp(timestamps, startTimestamp));
    }
    
    @Test
    public void testFindIndexOfNearestSuccessorTimestampWithStartTimestampEqualToATimestampInChronology() {
        Set<Timestamp> timestamps = new LinkedHashSet<Timestamp>();
        timestamps.add(Timestamp.fromMicros(1000L));
        for (int i = 0; i < TestData.getScaleCount(); i++) {
            long increment = 0;
            while (increment == 0) {
                increment = Math.abs(TestData.getScaleCount());
            }
            timestamps.add(Timestamp.fromMicros(
                    Iterables.getLast(timestamps).getMicros() + increment));            
        }
        Timestamp startTimestamp = Timestamp.fromMicros(
                Iterables.getFirst(timestamps, null).getMicros());
        assertEquals(1, client.
                findIndexOfNearestSuccessorTimestamp(timestamps, startTimestamp));
        startTimestamp = Timestamp.fromMicros(
                Iterables.get(timestamps, timestamps.size()/2).getMicros());
        assertEquals(timestamps.size()/2+1, client.
                findIndexOfNearestSuccessorTimestamp(timestamps, startTimestamp));
        startTimestamp = Timestamp.fromMicros(
                Iterables.getLast(timestamps).getMicros());
        assertEquals(timestamps.size(), client.
                findIndexOfNearestSuccessorTimestamp(timestamps, startTimestamp));   
    }
    
    @Test
    public void testFindIndexOfNearestSuccessorTimestampWithStartTimestampGreaterThanFirstTimestampAndLessThanLastTimestampInChronology() {
        Set<Timestamp> timestamps = new LinkedHashSet<Timestamp>();
        timestamps.add(Timestamp.fromMicros(1000L));
        for (int i = 0; i < TestData.getScaleCount(); i++) {
            long increment = 0;
            while (increment == 0) {
                increment = Math.abs(TestData.getScaleCount()) + 100L;
            }
            timestamps.add(Timestamp.fromMicros(
                    Iterables.getLast(timestamps).getMicros() + increment));
        }
        Timestamp abitrary = Iterables.get(timestamps, timestamps.size()/3);
        Timestamp abitrarySuccessor = Iterables.get(timestamps, timestamps.size()/3+1);
        Timestamp startTimestamp = Timestamp.fromMicros(
                (abitrary.getMicros() + abitrarySuccessor.getMicros()) / 2);
        assertEquals(timestamps.size()/3 + 1, client.
                findIndexOfNearestSuccessorTimestamp(timestamps, startTimestamp));
    }
}