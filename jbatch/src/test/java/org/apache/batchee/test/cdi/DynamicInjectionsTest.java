/*
 * Copyright 2012 International Business Machines Corp.
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.batchee.test.cdi;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.StepExecution;
import org.apache.batchee.test.lifecyle.ContainerLifecycle;
import org.junit.jupiter.api.Assertions;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Properties;

import static org.apache.batchee.util.Batches.waitForEnd;
import static org.testng.Assert.assertEquals;

@Listeners(ContainerLifecycle.class)
public class DynamicInjectionsTest {
    @Test
    public void run() {
        final JobOperator operator = BatchRuntime.getJobOperator();

        String parm1Val = "It's a parm";
        String parm2Val = "Or a prop";
        Properties jobParams = new Properties();
        jobParams.setProperty("refName", "org.apache.batchee.test.cdi.batchlet.NonCDIBeanBatchlet");
        jobParams.setProperty("parm1", parm1Val);
        jobParams.setProperty("parm2", parm2Val);

        long id = operator.start("cdi_inject_beans_2step", jobParams);

        waitForEnd(operator, id);

        JobExecution jobExec = operator.getJobExecution(id);

        Assertions.assertEquals(BatchStatus.COMPLETED, jobExec.getBatchStatus(), "Job didn't complete successfully");
        List<StepExecution> steps = operator.getStepExecutions(jobExec.getExecutionId());
        assertEquals(2, steps.size(), "Wrong number of step executions found");
        /*
         * Expecting exit status of:
         *   <jobExecId>:step1:<parm1Val>:<parm2Val>,<jobExecId>:step2:s2<parm1Val>:s2<parm2Val>
         */
        String expectedExitStatus = jobExec.getExecutionId() + ":step1:" + parm1Val + ":" + parm2Val + ","
                + jobExec.getExecutionId() + ":step2:" + "s2" + parm1Val + ":" + "s2" + parm2Val;
        Assertions.assertEquals(expectedExitStatus, jobExec.getExitStatus(), "Test fails - unexpected exit status");
    }
}
