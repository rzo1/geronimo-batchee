/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.batchee.test.data;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.batch.api.chunk.AbstractItemReader;
import javax.batch.api.chunk.AbstractItemWriter;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.StepExecution;

import org.apache.batchee.container.services.data.DefaultDataRepresentationService;
import org.apache.batchee.spi.DataRepresentationService;
import org.apache.batchee.util.Batches;
import org.junit.Assert;
import org.junit.Test;
import static org.testng.Assert.assertEquals;

public class DefaultDataRepresentationServiceTest {

    private DataRepresentationService dataExtSvc = new DefaultDataRepresentationService();

    @Test
    public void testNullConversions() {
        Assert.assertNull(dataExtSvc.toInternalRepresentation(null));
        Assert.assertNull(dataExtSvc.toJavaRepresentation(null));
    }

    @Test
    public void testProperties() {
        Properties props = new Properties();
        props.put("Key1", "Value1");
        props.put("Key2", "Value2");
        props.put("Key3", "Value3");

        Properties props2 = roundTrip(props, false);
        Assert.assertEquals(props.size(), props2.size());
        for (Object key : props.keySet()) {
            Assert.assertEquals(props.get(key), props2.get(key));
        }
    }


    @Test
    public void testJavaNatives() {
        assertRoundTripEquals(4712, true);
        assertRoundTripEquals(Integer.valueOf(4711), true);
        assertRoundTripEquals("this is a test value", true);
        assertRoundTripEquals(Long.valueOf(78483L), true);
        assertRoundTripEquals(12.34f, true); // be careful about different Locales!
    }

    @Test
    public void testJavaCustomEnum() {
        //X TODO
    }

    @Test
    public void testJavaDate_Timestamp() {
        Date dt = new Date();
        assertRoundTripEquals(dt, true);

        Timestamp tst = new Timestamp(dt.getTime());
        tst.setNanos(12345);
        assertRoundTripEquals(tst, true);
    }

    @Test
    public void testJavaMathTypes() {
        //X TODO BigInteger, BigDecimal
    }

    @Test
    public void testJavaCustomObjects() {
        SomeCustomJavaPojo customPojo = new SomeCustomJavaPojo();
        customPojo.setI(42);

        SomeCustomJavaPojo customPojo2 = roundTrip(customPojo, false);
        Assert.assertNotNull(customPojo2);
        Assert.assertEquals(42, customPojo2.getI());
    }

    @Test
    public void testJava8DateTimeViaReflection() {
        //X TODO
    }

    @Test
    public void testJodaDateTimeViaReflection() {
        //X TODO
    }

    @Test
    public void testJavaParameterisedTypes() {
        //X TODO
    }

    /**
     * java.util.Properties e.g. in JobProperties which got stored in older versions
     */
    @Test
    public void testDeserializeOldProperties() {
        //X TODO
    }

    /**
     * Integer, Long, String, etc which got stored in older versions as serialized values
     */
    @Test
    public void testDeserializeOldJavaNatives() {
        //X TODO
    }

    /**
     * Custom Java Objects got stored in older versions as serialized values
     */
    @Test
    public void testDeserializeOldJavaObjects() {
        //X TODO
    }

    private <T> void assertRoundTripEquals(T data, boolean humanReadable) {
        T data2 = roundTrip(data, humanReadable);
        Assert.assertEquals(data.getClass(), data2.getClass());
        Assert.assertEquals(data, data2);
    }

    @Test
    public void testBatchExecutionWithCheckpoints() {
        final JobOperator op = BatchRuntime.getJobOperator();
        final long id = op.start("checkpoint-storage-test", null);
        Batches.waitForEnd(op, id);

        final List<StepExecution> steps = op.getStepExecutions(id);
        assertEquals(1, steps.size());
        final StepExecution exec = steps.iterator().next();
        Assert.assertEquals(BatchStatus.FAILED, exec.getBatchStatus());
    }


    private <T> T roundTrip(T data, boolean humanReadable) {
        byte[] storedValue = dataExtSvc.toInternalRepresentation(data);
        Assert.assertNotNull(storedValue);

        if (humanReadable) {
            Assert.assertTrue(new String(storedValue).startsWith(DefaultDataRepresentationService.BATCHEE_DATA_PREFIX));
        }

        T deserialisedData = dataExtSvc.toJavaRepresentation(storedValue);
        Assert.assertNotNull(deserialisedData);

        return deserialisedData;
    }


    public static class SomeCustomJavaPojo implements Serializable {
        private int i;

        public int getI() {
            return i;
        }

        public void setI(int i) {
            this.i = i;
        }
    }


    public static class DummyReaderWithCheckpoint extends AbstractItemReader {


        private int counter = 0;


        @Override
        public Object readItem() throws Exception {
            return counter++ <= 3 ? counter : null;
        }

        @Override
        public Serializable checkpointInfo() throws Exception {
            return counter;
        }
    }

    public static class DummyWriterWithCheckpoint extends AbstractItemWriter {

        private Integer lastCount = null;

        @Override
        public void writeItems(List<Object> items) throws Exception {
            lastCount = (Integer) items.get(0);
            if (lastCount.equals(2)) {
                throw new Exception("Intentionally aborting this test batch!");
            }
        }

        @Override
        public Serializable checkpointInfo() throws Exception {
            return lastCount;
        }
    }
}
