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

package com.cdancy.bitbucket.rest.features;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.cdancy.bitbucket.rest.BaseBitbucketApiLiveTest;
import com.cdancy.bitbucket.rest.domain.branch.Branch;
import com.cdancy.bitbucket.rest.domain.branch.BranchModel;
import com.cdancy.bitbucket.rest.domain.branch.BranchPage;
import com.cdancy.bitbucket.rest.domain.branch.BranchPermission;
import com.cdancy.bitbucket.rest.domain.branch.BranchPermissionEnumType;
import com.cdancy.bitbucket.rest.domain.branch.BranchPermissionPage;
import com.cdancy.bitbucket.rest.domain.branch.Matcher;
import com.cdancy.bitbucket.rest.domain.pullrequest.User;
import com.cdancy.bitbucket.rest.options.CreateBranch;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

@Test(groups = "live", testName = "BranchApiLiveTest", singleThreaded = true)
public class BranchApiLiveTest extends BaseBitbucketApiLiveTest {

    /*
    String projectKey = randomStringLettersOnly();
    String repoKey = randomStringLettersOnly();
    String tagName = randomStringLettersOnly();
    String commitHash = "";
    */

    String projectKey = "BUILD";
    String repoKey = "dancc-test";
    String branchName = randomStringLettersOnly();
    String commitHash = "5284b6cec569346855710b535dafb915423110c2";
    String existingGroup = "dev-group";
    Long branchPermissionId = null;

    String defaultBranchId = "refs/heads/master";

    @BeforeClass
    public void init() {
        Branch branch = api().getDefault(projectKey, repoKey);
        assertThat(branch).isNotNull();
        assertThat(branch.errors().isEmpty()).isTrue();
        defaultBranchId = branch.id();
    }

    @Test
    public void testCreateBranch() {
        CreateBranch createBranch = CreateBranch.create(branchName, commitHash, null);
        Branch branch = api().create(projectKey, repoKey, createBranch);
        assertThat(branch).isNotNull();
        assertThat(branch.errors().isEmpty()).isTrue();
        assertThat(branch.id().endsWith(branchName)).isTrue();
        assertThat(commitHash.equalsIgnoreCase(branch.latestChangeset())).isTrue();
    }

    @Test (dependsOnMethods = "testCreateBranch")
    public void testListBranches() {
        BranchPage branch = api().list(projectKey, repoKey, null, null, null, null, null, 1);
        assertThat(branch).isNotNull();
        assertThat(branch.errors().isEmpty()).isTrue();
        assertThat(branch.values().size() > 0).isTrue();
    }

    @Test (dependsOnMethods = "testListBranches")
    public void testGetBranchModel() {
        BranchModel branchModel = api().model(projectKey, repoKey);
        assertThat(branchModel).isNotNull();
        assertThat(branchModel.errors().isEmpty()).isTrue();
    }

    @Test (dependsOnMethods = "testGetBranchModel")
    public void testUpdateDefaultBranch() {
        boolean success = api().updateDefault(projectKey, repoKey, "refs/heads/" + branchName);
        assertThat(success).isTrue();
    }

    @Test (dependsOnMethods = "testUpdateDefaultBranch")
    public void testGetNewDefaultBranch() {
        Branch branch = api().getDefault(projectKey, repoKey);
        assertThat(branch).isNotNull();
        assertThat(branch.errors().isEmpty()).isTrue();
        assertThat(branch.id()).isNotNull();
    }

    @Test (dependsOnMethods = "testGetNewDefaultBranch")
    public void testCreateBranchPermission() {
        List<String> groupPermission = new ArrayList<>();
        groupPermission.add(existingGroup);
        List<Long> listAccessKey = new ArrayList<>();
        listAccessKey.add(123L);
        List<BranchPermission> listBranchPermission = new ArrayList<>();
        listBranchPermission.add(BranchPermission.createWithId(null, BranchPermissionEnumType.FAST_FORWARD_ONLY,
                Matcher.create(Matcher.MatcherId.RELEASE, true), new ArrayList<User>(), groupPermission, listAccessKey));

        boolean success = api().updateBranchPermission(projectKey, repoKey, listBranchPermission);
        assertThat(success).isTrue();
    }

    @Test (dependsOnMethods = "testCreateBranchPermission")
    public void testListBranchPermission() {
        BranchPermissionPage branchPermissionPage = api().listBranchPermission(projectKey, repoKey, null, 1);
        assertThat(branchPermissionPage).isNotNull();
        assertThat(branchPermissionPage.errors().isEmpty()).isTrue();
        assertThat(branchPermissionPage.values().size() > 0).isTrue();
        branchPermissionId = branchPermissionPage.values().get(0).id();
    }

    @Test (dependsOnMethods = "testListBranchPermission")
    public void testDeleteBranchPermission() {
        if (branchPermissionId != null) {
            boolean success = api().deleteBranchPermission(projectKey, repoKey, branchPermissionId);
            assertThat(success).isTrue();
        } else {
            fail("branchPermissionId is null");
        }
    }

    @AfterClass
    public void fin() {
        boolean success = api().updateDefault(projectKey, repoKey, defaultBranchId);
        assertThat(success).isTrue();
        success = api().delete(projectKey, repoKey, "refs/heads/" + branchName);
        assertThat(success).isTrue();
    }

    private BranchApi api() {
        return api.branchApi();
    }
}
