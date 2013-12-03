/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.scxml;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.commons.scxml2.SCXMLExecutor;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.scxml.test.ErrorRecord;
import org.onehippo.repository.scxml.test.ErrorRecordTestUtils;
import org.onehippo.repository.scxml.test.ErrorRecordingErrorReporterWrapper;

/**
 * RepositorySCXMLExecutorFactoryTest
 */
public class RepositorySCXMLExecutorFactoryTest {

    private static final String SCXML_HELLO_WITH_ERROR_JEXL_SCRIPTS =
            "<scxml xmlns=\"http://www.w3.org/2005/07/scxml\" initial=\"hello\">\n" +
            "  <state id=\"hello\">\n" +
            "    <initial>\n" +
            "      <transition target=\"world\" />\n" +
            "    </initial>\n" +
            "    <state id=\"world\">\n" +
            "      <onentry>\n" +
            "        <script>\n" +
            "          unknownObject.invoke();\n" +
            "        </script>\n" +
            "      </onentry>\n" +
            "    </state>\n" +
            "  </state>\n" +
            "</scxml>";

    private MockRepositorySCXMLRegistry registry;
    private RepositorySCXMLExecutorFactory execFactory;

    @Before
    public void before() throws Exception {
        registry = new MockRepositorySCXMLRegistry();

        execFactory = new RepositorySCXMLExecutorFactory();
        execFactory.initialize();
    }

    @Test
    public void testLoadWithErrorJexlScripts() throws Exception {
        MockNode scxmlConfigNode = registry.createConfigNode();
        registry.addScxmlNode(scxmlConfigNode, "hello-with-error-jexl-scripts", SCXML_HELLO_WITH_ERROR_JEXL_SCRIPTS);
        registry.setUp(scxmlConfigNode);

        SCXMLDefinition helloScxml = registry.getSCXMLDefinition("hello-with-error-jexl-scripts");
        SCXMLExecutor helloExec = execFactory.createSCXMLExecutor(helloScxml);
        // replace errorReporter to capture the error infos
        ErrorRecordingErrorReporterWrapper errorReporter = new ErrorRecordingErrorReporterWrapper(helloScxml);
        helloExec.setErrorReporter(errorReporter);

        helloExec.go();

        List<ErrorRecord> errorRecords = errorReporter.getErrorRecords();

        // Note: Also, SimpleErrorReporter in commons-SCXML will log this as well: "... Expression error inside /hello/world ..."
        //       But let's just assert for hippo specific path information here.
        assertTrue(ErrorRecordTestUtils.containsErrorDetail(errorRecords, "in /hippo:moduleconfig/hipposcxml:definitions/hello-with-error-jexl-scripts"));
    }

}
