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

import com.cdancy.bitbucket.rest.domain.pullrequest.User;
import org.testng.annotations.Test;

import com.cdancy.bitbucket.rest.BitbucketApi;
import com.cdancy.bitbucket.rest.BitbucketApiMetadata;
import com.cdancy.bitbucket.rest.domain.branch.Branch;
import com.cdancy.bitbucket.rest.domain.branch.BranchModel;
import com.cdancy.bitbucket.rest.domain.branch.BranchPage;
import com.cdancy.bitbucket.rest.domain.branch.BranchPermission;
import com.cdancy.bitbucket.rest.domain.branch.BranchPermissionPage;
import com.cdancy.bitbucket.rest.domain.branch.BranchPermissionEnumType;
import com.cdancy.bitbucket.rest.domain.branch.Matcher;
import com.cdancy.bitbucket.rest.internal.BaseBitbucketMockTest;
import com.cdancy.bitbucket.rest.options.CreateBranch;
import com.google.common.collect.ImmutableMap;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Mock tests for the {@link BranchApi} class.
 */
@Test(groups = "unit", testName = "BranchApiMockTest")
public class BranchApiMockTest extends BaseBitbucketMockTest {

    public void testCreateBranch() throws Exception {
        MockWebServer server = mockEtcdJavaWebServer();

        server.enqueue(new MockResponse().setBody(payloadFromResource("/branch.json")).setResponseCode(200));
        BitbucketApi baseApi = api(server.getUrl("/"));
        BranchApi api = baseApi.branchApi();
        try {
            String projectKey = "PRJ";
            String repoKey = "myrepo";
            String branchName = "dev-branch";
            String commitHash = "8d351a10fb428c0c1239530256e21cf24f136e73";

            CreateBranch createBranch = CreateBranch.create(branchName, commitHash, null);
            Branch branch = api.create(projectKey, repoKey, createBranch);
            assertThat(branch).isNotNull();
            assertThat(branch.errors().isEmpty()).isTrue();
            assertThat(branch.id().endsWith(branchName)).isTrue();
            assertThat(commitHash.equalsIgnoreCase(branch.latestChangeset())).isTrue();
            assertSent(server, "POST", "/rest/branch-utils/" + BitbucketApiMetadata.API_VERSION
                    + "/projects/" + projectKey + "/repos/" + repoKey + "/branches");
        } finally {
            baseApi.close();
            server.shutdown();
        }
    }

    public void testListBranches() throws Exception {
        MockWebServer server = mockEtcdJavaWebServer();

        server.enqueue(new MockResponse().setBody(payloadFromResource("/branch-list.json")).setResponseCode(200));
        BitbucketApi baseApi = api(server.getUrl("/"));
        BranchApi api = baseApi.branchApi();
        try {
            String projectKey = "PRJ";
            String repoKey = "myrepo";

            BranchPage branch = api.list(projectKey, repoKey, null, null, null, null, null, 1);
            assertThat(branch).isNotNull();
            assertThat(branch.errors().isEmpty()).isTrue();
            assertThat(branch.values().size() > 0).isTrue();
            assertThat("hello-world".equals(branch.values().get(0).displayId())).isTrue();
            Map<String, ?> queryParams = ImmutableMap.of("limit", 1);
            assertSent(server, "GET", "/rest/api/" + BitbucketApiMetadata.API_VERSION
                    + "/projects/" + projectKey + "/repos/" + repoKey + "/branches", queryParams);
        } finally {
            baseApi.close();
            server.shutdown();
        }
    }

    public void testListBranchesNonExistent() throws Exception {
        MockWebServer server = mockEtcdJavaWebServer();

        server.enqueue(new MockResponse().setBody(payloadFromResource("/branch-list-error.json")).setResponseCode(404));
        BitbucketApi baseApi = api(server.getUrl("/"));
        BranchApi api = baseApi.branchApi();
        try {
            String projectKey = "hello";
            String repoKey = "world";

            BranchPage branch = api.list(projectKey, repoKey, null, null, null, null, null, 1);
            assertThat(branch).isNotNull();
            assertThat(branch.errors().size() > 0).isTrue();
            Map<String, ?> queryParams = ImmutableMap.of("limit", 1);
            assertSent(server, "GET", "/rest/api/" + BitbucketApiMetadata.API_VERSION
                    + "/projects/" + projectKey + "/repos/" + repoKey + "/branches", queryParams);
        } finally {
            baseApi.close();
            server.shutdown();
        }
    }

    public void testGetBranchModel() throws Exception {
        MockWebServer server = mockEtcdJavaWebServer();

        server.enqueue(new MockResponse().setBody(payloadFromResource("/branch-model.json")).setResponseCode(200));
        BitbucketApi baseApi = api(server.getUrl("/"));
        BranchApi api = baseApi.branchApi();
        try {
            String projectKey = "PRJ";
            String repoKey = "myrepo";
            BranchModel branchModel = api.model(projectKey, repoKey);
            assertThat(branchModel).isNotNull();
            assertThat(branchModel.errors().isEmpty()).isTrue();
            assertThat(branchModel.types().size() > 0).isTrue();
            assertSent(server, "GET", "/rest/branch-utils/" + BitbucketApiMetadata.API_VERSION
                    + "/projects/" + projectKey + "/repos/" + repoKey + "/branchmodel");
        } finally {
            baseApi.close();
            server.shutdown();
        }
    }

    public void testGetBranchModelOnError() throws Exception {
        MockWebServer server = mockEtcdJavaWebServer();

        server.enqueue(new MockResponse().setBody(payloadFromResource("/branch-list-error.json")).setResponseCode(404));
        BitbucketApi baseApi = api(server.getUrl("/"));
        BranchApi api = baseApi.branchApi();
        try {
            String projectKey = "PRJ";
            String repoKey = "myrepo";
            BranchModel branchModel = api.model(projectKey, repoKey);
            assertThat(branchModel).isNotNull();
            assertThat(branchModel.errors()).isNotEmpty();
            assertSent(server, "GET", "/rest/branch-utils/" + BitbucketApiMetadata.API_VERSION
                    + "/projects/" + projectKey + "/repos/" + repoKey + "/branchmodel");
        } finally {
            baseApi.close();
            server.shutdown();
        }
    }

    public void testDeleteBranch() throws Exception {
        MockWebServer server = mockEtcdJavaWebServer();

        server.enqueue(new MockResponse().setResponseCode(204));
        BitbucketApi baseApi = api(server.getUrl("/"));
        BranchApi api = baseApi.branchApi();
        try {
            String projectKey = "PRJ";
            String repoKey = "myrepo";
            boolean success = api.delete(projectKey, repoKey, "refs/heads/some-branch-name");
            assertThat(success).isTrue();
            assertSent(server, "DELETE", "/rest/branch-utils/" + BitbucketApiMetadata.API_VERSION
                    + "/projects/" + projectKey + "/repos/" + repoKey + "/branches");
        } finally {
            baseApi.close();
            server.shutdown();
        }
    }

    public void testGetDefaultBranch() throws Exception {
        MockWebServer server = mockEtcdJavaWebServer();

        server.enqueue(new MockResponse().setBody(payloadFromResource("/branch-default.json")).setResponseCode(200));
        BitbucketApi baseApi = api(server.getUrl("/"));
        BranchApi api = baseApi.branchApi();
        try {
            String projectKey = "PRJ";
            String repoKey = "myrepo";

            Branch branch = api.getDefault(projectKey, repoKey);
            assertThat(branch).isNotNull();
            assertThat(branch.errors().isEmpty()).isTrue();
            assertThat(branch.id()).isNotNull();
            assertSent(server, "GET", "/rest/api/" + BitbucketApiMetadata.API_VERSION
                    + "/projects/" + projectKey + "/repos/" + repoKey + "/branches/default");
        } finally {
            baseApi.close();
            server.shutdown();
        }
    }

    public void testUpdateDefaultBranch() throws Exception {
        MockWebServer server = mockEtcdJavaWebServer();

        server.enqueue(new MockResponse().setResponseCode(204));
        BitbucketApi baseApi = api(server.getUrl("/"));
        BranchApi api = baseApi.branchApi();
        try {
            String projectKey = "PRJ";
            String repoKey = "myrepo";

            boolean success = api.updateDefault(projectKey, repoKey, "refs/heads/my-new-default-branch");
            assertThat(success).isTrue();
            assertSent(server, "PUT", "/rest/api/" + BitbucketApiMetadata.API_VERSION
                    + "/projects/" + projectKey + "/repos/" + repoKey + "/branches/default");
        } finally {
            baseApi.close();
            server.shutdown();
        }
    }

    public void testListBranchePermissions() throws Exception {
        MockWebServer server = mockEtcdJavaWebServer();

        server.enqueue(new MockResponse().setBody(payloadFromResource("/branch-permission-list.json")).setResponseCode(200));
        BitbucketApi baseApi = api(server.getUrl("/"));
        BranchApi api = baseApi.branchApi();
        try {
            String projectKey = "PRJ";
            String repoKey = "myrepo";

            BranchPermissionPage branch = api.listBranchPermission(projectKey, repoKey, null, 1);
            assertThat(branch).isNotNull();
            assertThat(branch.errors().isEmpty()).isTrue();
            assertThat(branch.values().size() > 0).isTrue();
            assertThat(839L == branch.values().get(0).id()).isTrue();
            assertThat(2 == branch.values().get(0).accessKeys().size()).isTrue();

            Map<String, ?> queryParams = ImmutableMap.of("limit", 1);
            assertSent(server, "GET", "/rest/branch-permissions/2.0"
                    + "/projects/" + projectKey + "/repos/" + repoKey + "/restrictions", queryParams);
        } finally {
            baseApi.close();
            server.shutdown();
        }
    }

    public void testListBranchesPermissionsNonExistent() throws Exception {
        MockWebServer server = mockEtcdJavaWebServer();

        server.enqueue(new MockResponse().setBody(payloadFromResource("/branch-permission-list-error.json")).setResponseCode(404));
        BitbucketApi baseApi = api(server.getUrl("/"));
        BranchApi api = baseApi.branchApi();
        try {
            String projectKey = "hello";
            String repoKey = "world";

            BranchPermissionPage branch = api.listBranchPermission(projectKey, repoKey, null, 1);
            assertThat(branch).isNotNull();
            assertThat(branch.errors().size() > 0).isTrue();

            Map<String, ?> queryParams = ImmutableMap.of("limit", 1);
            assertSent(server, "GET", "/rest/branch-permissions/2.0"
                    + "/projects/" + projectKey + "/repos/" + repoKey + "/restrictions", queryParams);
        } finally {
            baseApi.close();
            server.shutdown();
        }
    }

    public void testUpdateBranchesPermissions() throws Exception {
        MockWebServer server = mockEtcdJavaWebServer();

        server.enqueue(new MockResponse().setResponseCode(204));
        BitbucketApi baseApi = api(server.getUrl("/"));
        BranchApi api = baseApi.branchApi();
        try {

            List<String> groupPermission = new ArrayList<>();
            groupPermission.add("Test12354");
            List<Long> listAccessKey = new ArrayList<>();
            listAccessKey.add(123L);
            List<BranchPermission> listBranchPermission = new ArrayList<>();
            listBranchPermission.add(BranchPermission.createWithId(839L, BranchPermissionEnumType.FAST_FORWARD_ONLY,
                    Matcher.create(Matcher.MatcherId.RELEASE, true), new ArrayList<User>(), groupPermission,
                    listAccessKey));

            String projectKey = "PRJ";
            String repoKey = "myrepo";
            boolean success = api.updateBranchPermission(projectKey, repoKey, listBranchPermission);
            assertThat(success).isTrue();
            assertSent(server, "POST", "/rest/branch-permissions/2.0"
                    + "/projects/" + projectKey + "/repos/" + repoKey + "/restrictions");
        } finally {
            baseApi.close();
            server.shutdown();
        }
    }

    public void testDeleteBranchesPermissions() throws Exception {
        MockWebServer server = mockEtcdJavaWebServer();

        server.enqueue(new MockResponse().setResponseCode(204));
        BitbucketApi baseApi = api(server.getUrl("/"));
        BranchApi api = baseApi.branchApi();
        try {
            String projectKey = "PRJ";
            String repoKey = "myrepo";
            Long idToDelete = 839L;
            boolean success = api.deleteBranchPermission(projectKey, repoKey, idToDelete);
            assertThat(success).isTrue();
            assertSent(server, "DELETE", "/rest/branch-permissions/2.0"
                    + "/projects/" + projectKey + "/repos/" + repoKey + "/restrictions/" + idToDelete);
        } finally {
            baseApi.close();
            server.shutdown();
        }
    }
}
