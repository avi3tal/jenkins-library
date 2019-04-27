/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2019
 */

import java.util.logging.Logger
import org.junit.*
import org.junit.runners.MethodSorters
import static org.hamcrest.CoreMatchers.*;
import org.hamcrest.collection.IsMapContaining
import org.zowe.jenkins_shared_library.integrationtest.*
import static groovy.test.GroovyAssert.*
import org.zowe.jenkins_shared_library.Utils

/**
 * Test {@link org.zowe.jenkins_shared_library.pipelines.generic.Pipeline}
 *
 * The test case will create a test Jenkins job and attach the current library to it.
 *
 * Then will run several validations on the job:
 *
 * - start with default parameter and the job should success
 * - test a PATCH release
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class GenericPipelineMultibranchPipelineTest extends IntegrationTest {
    // this github owner will be used for testing
    static final String TEST_OWNER = 'zowe'
    // this github repository will be used for testing
    static final String TEST_REPORSITORY = 'jenkins-library-fvt-nodejs'
    // branch to run test
    static final String TEST_BRANCH = 'master'
    // branch to run test
    static final String TEST_JENKINSFILE = 'Jenkinsfile.generic-multibranch'
    // github api instance
    protected static GitHubAPI githubApi

    @BeforeClass
    public static void setup() {
        initMultiBranchPipelineJob([
            'name'             : 'generic-multibranch',
            'git-credential'   : System.getProperty('github.credential'),
            'git-owner'        : TEST_OWNER,
            'git-repository'   : TEST_REPORSITORY,
            'branch'           : TEST_BRANCH,
            'jenkinsfile-path' : TEST_JENKINSFILE
        ])
        this.githubApi = new GitHubAPI("${TEST_OWNER}/${TEST_REPORSITORY}")
    }

    @AfterClass
    public static void teardown() {
        // delete the test job if exists
        if (jenkins && testJobName &&
            buildInformation && buildInformation.containsKey('result') &&
            buildInformation['result'] == 'SUCCESS') {
            jenkins.deleteJob(fullTestJobName)
        }
    }

    @Test
    void testABuildInformation() {
        // buildInformation
        assertThat('Build result', buildInformation, IsMapContaining.hasKey('number'));
        assertThat('Build result', buildInformation, IsMapContaining.hasKey('result'));
        assertThat('Build result', buildInformation['result'], equalTo('SUCCESS'));
    }

    @Test
    void testBConsoleLog() {
        // console log
        assertThat('Build console log', buildLog, not(equalTo('')))
        [
            // build stage
            'Executing stage Build: Source',
            '+ npm install',
            '+ npm run build',
            // custom stage is configured and started
            'This step can be skipped by setting the `Skip Stage: CustomStage` option to true',
            'This is a custom stage, skippable',
            // test stage
            'Executing stage Test: Unit',
            '+ npm run test:unit',
            'Recording test results', // junit
            '[Cobertura] Publishing Cobertura coverage report...', // cobertura
            '[htmlpublisher] Archiving HTML reports...',
            // publish stage
            'Executing stage Publish',
            'Deploying artifact:',
            'Deploying build info to:',
            'Artifact uploading is successful.',
            // Verify Artifact Uploaded stage
            'Executing stage Verify Artifact Uploaded',
            'Successfully found artifact uploaded:',
            // complete stage
            'Pipeline Execution Complete',
            // sending email
            'Sending email notification...',
            "Subject: [${buildInformation['result']}] Job \'${fullTestJobName.join('/')}/${TEST_BRANCH} [${buildInformation['number']}]\'"
        ].each {
            assertThat('Build console log', buildLog, containsString(it))
        }
    }

    @Test
    void testCRelease() {
        // try a release build

        List job = fullTestJobName.collect()
        job.add(TEST_BRANCH)
        // reset result
        buildInformation = [:]
        buildLog = ''

        // retrieve current version
        def currentPkg = githubApi.readPackageJson()
        def currentVersion = Utils.parseSemanticVersion(currentPkg['version'])
        logger.fine("Current package version is: ${currentVersion}")

        // start the job, wait for it's done and get build result
        logger.fine("Starting a release build without pre-release string ...")
        buildInformation = jenkins.startJobAndGetBuildInformation(job, [
            'FETCH_PARAMETER_ONLY' : 'false',
            'LIBRARY_BRANCH'       : System.getProperty('library.branch'),
            'Perform Release'      : true,
            'Pre-Release String'   : ''
        ])
        // load job console log
        if (buildInformation && buildInformation['number']) {
            buildLog = jenkins.getBuildLog(job, buildInformation['number'])
        }

        // retrieve version after release
        def newPkg = githubApi.readPackageJson()
        def newVersion = Utils.parseSemanticVersion(newPkg['version'])
        logger.fine("New package version is: ${newVersion}")

        // we created a tag
        assertThat('Build console log', buildLog, containsString('[new tag]'))

        // list tags
        List<String> tagNames = githubApi.getTags()
        String expectedTag = "v${currentVersion['major']}.${currentVersion['minor']}.${currentVersion['patch']}"
        logger.fine("All tags: ${tagNames}")
        logger.fine("Expected tag: ${expectedTag}")
        // check if we have the tag
        assertThat('Tags', tagNames, hasItem(expectedTag))

        // version is not bumped because default GenericPipeline.bumpVersion() is empty
        assertThat('major version', newVersion['major'], equalTo(currentVersion['major']));
        assertThat('minor version', newVersion['minor'], equalTo(currentVersion['minor']));
        assertThat('patch version', newVersion['patch'], equalTo(currentVersion['patch'] + 1));
    }
}
