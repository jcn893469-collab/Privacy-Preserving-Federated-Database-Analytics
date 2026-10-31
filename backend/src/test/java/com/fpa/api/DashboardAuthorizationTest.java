package com.fpa.api;

import com.fpa.api.controller.AdminController;
import com.fpa.api.controller.AnalyticsController;
import com.fpa.api.controller.DatasetController;
import com.fpa.api.controller.FederatedDatabaseController;
import com.fpa.api.controller.PublicDashboardController;
import com.fpa.api.controller.UserController;
import com.fpa.api.dto.PublicDashboardDatabaseResponse;
import com.fpa.api.dto.PublicDashboardResponse;
import com.fpa.config.SecurityConfig;
import com.fpa.domain.entity.User;
import com.fpa.domain.entity.FederatedResult;
import com.fpa.security.CustomUserDetailsService;
import com.fpa.security.CurrentUserService;
import com.fpa.security.JwtAuthenticationFilter;
import com.fpa.security.JwtService;
import com.fpa.service.AdminDashboardService;
import com.fpa.service.AnalyticsContributionService;
import com.fpa.service.AuditLogService;
import com.fpa.service.FederatedAggregationService;
import com.fpa.service.FederatedDatabaseService;
import com.fpa.service.DatasetService;
import com.fpa.service.PublicDashboardService;
import com.fpa.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(controllers = {
    AdminController.class,
    AnalyticsController.class,
    DatasetController.class,
    FederatedDatabaseController.class,
    UserController.class,
    PublicDashboardController.class
})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class DashboardAuthorizationTest {

    private static final List<String> TAB_ENDPOINTS = List.of(
        "/api/admin/status",
        "/api/admin/views/organization-summary",
        "/api/users?page=0&size=1",
        "/api/databases?page=0&size=1",
        "/api/analytics/contributions?page=0&size=1",
        "/api/analytics/results/runs?page=0&size=1",
        "/api/admin/audit-logs?page=0&size=1"
    );

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private AdminDashboardService adminDashboardService;

    @MockBean
    private AuditLogService auditLogService;

    @MockBean
    private AnalyticsContributionService analyticsContributionService;

    @MockBean
    private FederatedAggregationService federatedAggregationService;

    @MockBean
    private FederatedDatabaseService federatedDatabaseService;

    @MockBean
    private DatasetService datasetService;

    @MockBean
    private UserService userService;

    @MockBean
    private CurrentUserService currentUserService;

    @MockBean
    private PublicDashboardService publicDashboardService;

    @BeforeEach
    void returnAuthenticatedDatabaseUserForAllowedControllerRequests() {
        when(auditLogService.searchAndFilter(any(), any(), any(), any(), any(), any()))
            .thenReturn(Page.empty());
        when(federatedAggregationService.getHistoricalRuns(any(), any()))
            .thenReturn(Page.empty());
        when(federatedAggregationService.executeAndPersistFederatedRun("overall"))
            .thenReturn(FederatedResult.builder().build());
        when(currentUserService.requireAnyRole(any(User.Role[].class))).thenAnswer(invocation -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String roleName = authentication.getAuthorities().iterator().next().getAuthority().substring("ROLE_".length());
            User user = User.builder()
                .fullName("Test User")
                .email("test@example.org")
                .passwordHash("unused")
                .role(User.Role.valueOf(roleName))
                .active(true)
                .build();
            user.setId(1L);
            return user;
        });
    }

    @Test
    void datasetAndColumnEndpointsAreLimitedToAdminsAndDataOwners() throws Exception {
        for (String endpoint : List.of(
                "/api/datasets/database/1",
                "/api/datasets/1/columns")) {
            mockMvc.perform(get(endpoint).with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
            mockMvc.perform(get(endpoint).with(SecurityMockMvcRequestPostProcessors.user("owner").roles("DATA_OWNER")))
                .andExpect(status().isOk());
            mockMvc.perform(get(endpoint).with(SecurityMockMvcRequestPostProcessors.user("researcher").roles("RESEARCHER")))
                .andExpect(status().isForbidden());
        }

        for (var request : List.of(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/datasets"),
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/datasets/1"),
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/datasets/1"),
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/datasets/1/columns"),
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/datasets/1/columns/1"),
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/datasets/1/columns/1"))) {
            mockMvc.perform(request.with(SecurityMockMvcRequestPostProcessors.user("researcher").roles("RESEARCHER")))
                .andExpect(status().isForbidden());
        }
    }

    @Test
    void datasetSearchUsesRequestedPageAndRejectsOutOfRangePagination() throws Exception {
        when(datasetService.searchDatasets(eq(1L), eq("vitals"), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/datasets/database/1?query=vitals&page=1&size=20")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());

        var pageable = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(datasetService).searchDatasets(eq(1L), eq("vitals"), pageable.capture());
        assertEquals(1, pageable.getValue().getPageNumber());
        assertEquals(20, pageable.getValue().getPageSize());

        mockMvc.perform(get("/api/datasets/database/1?page=-1")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/datasets/database/1?size=101")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
            .andExpect(status().isBadRequest());
    }

    @Test
    void publicDashboardReturnsReadOnlyMetricsWithoutAuthentication() throws Exception {
        when(publicDashboardService.getDashboard()).thenReturn(new PublicDashboardResponse(
            1,
            1,
            1,
            10,
            new BigDecimal("25.0000"),
            new BigDecimal("2.500000"),
            List.of(new PublicDashboardDatabaseResponse(
                "Synthetic Organization",
                "synthetic-db",
                "owner@example.org",
                new BigDecimal("10.0000"),
                new BigDecimal("0.0000"),
                true
            ))
        ));

        mockMvc.perform(get("/api/public/dashboard"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalOrganizations").value(1))
            .andExpect(jsonPath("$.totalContributions").value(1))
            .andExpect(jsonPath("$.globalProtectedSum").value(25))
            .andExpect(jsonPath("$.globalProtectedMean").value(2.5))
            .andExpect(jsonPath("$.databases[0].ownerEmail").value("owner@example.org"))
            .andExpect(jsonPath("$.databases[0].ownerUserId").doesNotExist())
            .andExpect(jsonPath("$.databases[0].description").doesNotExist());
    }

    @Test
    void publicLandingPageUsesThePublicApiAndRefreshesAfterSuccessfulWrites() throws IOException {
        String landingPage = readStaticResource("/static/index.html");
        String portalScript = readStaticResource("/static/js/org-portal.js");
        String adminDashboard = readStaticResource("/static/admin-dashboard.html");

        assertTrue(landingPage.contains("fetch('/api/public/dashboard'"));
        assertTrue(landingPage.contains("window.addEventListener('storage'"));
        assertTrue(landingPage.contains("Unable to load live network data."));
        assertTrue(landingPage.contains("No organization databases are registered yet."));
        assertFalse(landingPage.contains("fetch('/api/analytics/results/global'"));
        assertTrue(portalScript.contains("notifyPublicDashboardUpdated()"));
        assertTrue(adminDashboard.contains("OrgAuth.notifyPublicDashboardUpdated();"));
    }

    @Test
    void adminCanLoadAllSevenDashboardTabs() throws Exception {
        for (String endpoint : TAB_ENDPOINTS) {
            mockMvc.perform(get(endpoint).with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        }
    }

    @Test
    void unauthenticatedAdminTabRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/admin/status"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void dataOwnerCanLoadOnlyOwnDatabaseAndContributionTabs() throws Exception {
        for (int i = 0; i < TAB_ENDPOINTS.size(); i++) {
            var request = get(TAB_ENDPOINTS.get(i))
                .with(SecurityMockMvcRequestPostProcessors.user("owner").roles("DATA_OWNER"));
            if (i == 3 || i == 4) {
                mockMvc.perform(request).andExpect(status().isOk());
            } else {
                mockMvc.perform(request).andExpect(status().isForbidden());
            }
        }
    }

    @Test
    void researcherCanLoadOnlyFederatedRunsAndForbiddenAccessKeepsAuthenticationUsable() throws Exception {
        for (int i = 0; i < TAB_ENDPOINTS.size(); i++) {
            var request = get(TAB_ENDPOINTS.get(i))
                .with(SecurityMockMvcRequestPostProcessors.user("researcher").roles("RESEARCHER"));
            if (i == 5) {
                mockMvc.perform(request).andExpect(status().isOk());
            } else {
                mockMvc.perform(request).andExpect(status().isForbidden());
            }
        }
        mockMvc.perform(get("/api/analytics/results/runs?page=0&size=1")
                .with(SecurityMockMvcRequestPostProcessors.user("researcher").roles("RESEARCHER")))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/analytics/results/global")
                .with(SecurityMockMvcRequestPostProcessors.user("researcher").roles("RESEARCHER")))
            .andExpect(status().isOk());
    }

    @Test
    void onlyAdminsAndResearchersCanStartFederatedRuns() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/analytics/results/runs?metric=overall")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
            .andExpect(status().isCreated());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/analytics/results/runs?metric=overall")
                .with(SecurityMockMvcRequestPostProcessors.user("researcher").roles("RESEARCHER")))
            .andExpect(status().isCreated());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/analytics/results/runs?metric=overall")
                .with(SecurityMockMvcRequestPostProcessors.user("owner").roles("DATA_OWNER")))
            .andExpect(status().isForbidden());
    }

    @Test
    void dashboardNavigationPassesClickedTabAndTreatsForbiddenAsAccessDenied() throws IOException {
        String html = readStaticResource("/static/admin-dashboard.html");
        assertTrue(html.contains("onclick=\"switchTab('tabContributions', this)\""));
        assertTrue(html.contains("function switchTab(tabId, tabElement)"));
        assertFalse(html.contains("event.currentTarget"));
        assertTrue(html.contains("if (res.status === 403)"));
        assertTrue(html.contains("setDashboardNotice(message, true)"));
        assertFalse(html.contains("if (res.status === 403) { OrgAuth.clearSession();"));
    }

    @Test
    void researcherHasSeparateDashboardAndResponsiveLayoutsAreDefined() throws IOException {
        String researcherDashboard = readStaticResource("/static/researcher-dashboard.html");
        String adminDashboard = readStaticResource("/static/admin-dashboard.html");
        String loginPage = readStaticResource("/static/org-login.html");
        String portalScript = readStaticResource("/static/js/org-portal.js");
        String portalStyles = readStaticResource("/static/css/org-portal.css");

        assertTrue(researcherDashboard.contains("Federated computation"));
        assertFalse(researcherDashboard.contains("User Management"));
        assertTrue(adminDashboard.contains("researcher-dashboard.html"));
        assertTrue(loginPage.contains("researcher-dashboard.html"));
        assertTrue(portalScript.contains("s.role !== 'DATA_OWNER'"));
        assertTrue(portalStyles.contains("@media (max-width: 760px)"));
        assertTrue(portalStyles.contains("@media (max-width: 460px)"));
    }

    private String readStaticResource(String path) throws IOException {
        try (var stream = getClass().getResourceAsStream(path)) {
            assertTrue(stream != null, path + " should be on the test classpath");
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
