/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.updatebot.git;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class RemoteUsernamePasswordTest {
    private static final transient Logger LOG = LoggerFactory.getLogger(RemoteUsernamePasswordTest.class);

    public static void assertRemoveGitUserNamePassword(String input, String expected) {
        String actual = GitHelper.removeUsernamePassword(input);
        LOG.debug("Transformed " + input + " => " + actual);
        assertThat(actual).describedAs("Removed username password from " + input).isEqualTo(expected);
    }

    @Test
    public void testRemoteUserPassword() throws Exception {
        assertRemoveGitUserNamePassword("cheese", "cheese");
        assertRemoveGitUserNamePassword("https://github.com/foo/bar.git", "https://github.com/foo/bar.git");
        assertRemoveGitUserNamePassword("https://someuser:somepassword@github.com/fabric8-updatebot/updatebot.git", "https://github.com/fabric8-updatebot/updatebot.git");
    }

}
