package com.dapurkita;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dapurkita.user.Role;
import com.dapurkita.user.User;
import com.dapurkita.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Tes integrasi terhadap PostgreSQL asli (Testcontainers). Memerlukan Docker. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class ApiIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Value("${app.jwt.secret}") String jwtSecret;

    // ---------- helpers ----------

    private String json(Object value) throws Exception {
        return mapper.writeValueAsString(value);
    }

    private JsonNode body(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    private String uniqueEmail() {
        return "u" + UUID.randomUUID().toString().substring(0, 8) + "@mail.com";
    }

    private MvcResult register(String email) throws Exception {
        return mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Siti", "email", email, "password", "rahasia123"))))
                .andExpect(status().isCreated()).andReturn();
    }

    private String userToken() throws Exception {
        return body(register(uniqueEmail())).get("accessToken").asText();
    }

    private String adminToken() throws Exception {
        String email = "admin-" + UUID.randomUUID().toString().substring(0, 8) + "@dapurkita.test";
        users.save(new User("Admin", email, encoder.encode("Admin12345!"), Role.ADMIN));
        MvcResult login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "Admin12345!"))))
                .andExpect(status().isOk()).andReturn();
        return body(login).get("accessToken").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private Map<String, Object> recipe(String title, long categoryId, List<Map<String, Object>> ingredients,
                                       List<String> steps) {
        return Map.of(
                "title", title, "description", "Deskripsi " + title,
                "imageUrl", "https://example.com/x.jpg",
                "cookingTimeMinutes", 60, "servings", 4, "categoryId", categoryId,
                "ingredients", ingredients,
                "instructions", steps.stream().map(s -> Map.of("description", s)).toList());
    }

    private Map<String, Object> ing(String name, String quantity) {
        return Map.of("name", name, "quantity", quantity);
    }

    private long createRecipe(String adminToken, Map<String, Object> payload) throws Exception {
        MvcResult created = mvc.perform(post("/api/v1/recipes").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(json(payload)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/recipes/")))
                .andReturn();
        return body(created).get("id").asLong();
    }

    // ---------- auth ----------

    @Test
    void registerSetsHttpOnlyCookieAndRejectsDuplicateEmail() throws Exception {
        String email = uniqueEmail();
        MvcResult result = register(email);
        Cookie cookie = result.getResponse().getCookie("refreshToken");
        assertNotNull(cookie);
        assertTrue(cookie.isHttpOnly());
        assertEquals("/api/v1/auth", cookie.getPath());
        JsonNode json = body(result);
        assertEquals("Bearer", json.get("tokenType").asText());
        assertEquals(900, json.get("expiresIn").asInt());
        assertEquals("USER", json.get("user").get("role").asText());
        assertTrue(json.get("refreshToken") == null, "refresh token tidak boleh ada di body");

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Lain", "email", email.toUpperCase(), "password", "rahasia123"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("EMAIL_ALREADY_EXISTS")));
    }

    @Test
    void registerValidationReturnsFieldErrors() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "A", "email", "bukan-email", "password", "pendek"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("name")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("email")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("password")))
                .andExpect(jsonPath("$.path", is("/api/v1/auth/register")));
    }

    @Test
    void malformedJsonIsMalformedRequest() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{rusak"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("MALFORMED_REQUEST")));
    }

    @Test
    void loginWithWrongPasswordIsInvalidCredentials() throws Exception {
        String email = uniqueEmail();
        register(email);
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "salah-total"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("INVALID_CREDENTIALS")));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "tidak-ada@mail.com", "password", "apapun123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("INVALID_CREDENTIALS")));
    }

    @Test
    void refreshRotatesAndReuseRevokesAllSessions() throws Exception {
        Cookie first = register(uniqueEmail()).getResponse().getCookie("refreshToken");

        MvcResult refreshed = mvc.perform(post("/api/v1/auth/refresh").cookie(first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        Cookie second = refreshed.getResponse().getCookie("refreshToken");
        assertNotNull(second);
        assertNotEquals(first.getValue(), second.getValue());

        // Token lama dipakai lagi: ditolak dan seluruh sesi dicabut
        mvc.perform(post("/api/v1/auth/refresh").cookie(first))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
        mvc.perform(post("/api/v1/auth/refresh").cookie(second))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithoutCookieIsUnauthorized() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    void logoutRevokesRefreshTokenAndClearsCookie() throws Exception {
        Cookie cookie = register(uniqueEmail()).getResponse().getCookie("refreshToken");
        MvcResult out = mvc.perform(post("/api/v1/auth/logout").cookie(cookie))
                .andExpect(status().isNoContent()).andReturn();
        assertEquals(0, out.getResponse().getCookie("refreshToken").getMaxAge());
        mvc.perform(post("/api/v1/auth/refresh").cookie(cookie)).andExpect(status().isUnauthorized());
    }

    @Test
    void expiredAndInvalidAccessTokens() throws Exception {
        var key = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        Instant past = Instant.now().minusSeconds(3600);
        String expired = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().issuer("dapurkita").subject("1").issuedAt(past.minusSeconds(900))
                        .expiresAt(past).claim("role", "USER").build())).getTokenValue();

        mvc.perform(get("/api/v1/users/me").header("Authorization", bearer(expired)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("TOKEN_EXPIRED")));
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer sampah"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
        mvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
        // Endpoint publik tetap menolak token kedaluwarsa agar frontend memicu refresh
        mvc.perform(get("/api/v1/recipes").header("Authorization", bearer(expired)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("TOKEN_EXPIRED")));
    }

    // ---------- user ----------

    @Test
    void profileCanBeReadAndRenamed() throws Exception {
        String token = userToken();
        mvc.perform(get("/api/v1/users/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name", is("Siti")));
        mvc.perform(patch("/api/v1/users/me").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("name", "  Siti Aminah  "))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name", is("Siti Aminah")));
        mvc.perform(patch("/api/v1/users/me").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("name", "A"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field", is("name")));
    }

    // ---------- categories ----------

    @Test
    void categoriesAreSeededAndPublic() throws Exception {
        mvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].slug", is("makanan-utama")));
    }

    // ---------- recipes: access control ----------

    @Test
    void onlyAdminCanManageRecipes() throws Exception {
        Map<String, Object> payload = recipe("Tes Akses " + UUID.randomUUID(), 2,
                List.of(ing("Garam", "secukupnya")), List.of("Masak."));

        mvc.perform(post("/api/v1/recipes").contentType(MediaType.APPLICATION_JSON).content(json(payload)))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
        mvc.perform(post("/api/v1/recipes").header("Authorization", bearer(userToken()))
                        .contentType(MediaType.APPLICATION_JSON).content(json(payload)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code", is("FORBIDDEN")));

        String admin = adminToken();
        long id = createRecipe(admin, payload);
        String userToken = userToken();
        mvc.perform(put("/api/v1/recipes/" + id).header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON).content(json(payload)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/recipes/" + id).header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void detailHasContractShapeAndNoAuthor() throws Exception {
        long id = createRecipe(adminToken(), recipe("Bentuk " + UUID.randomUUID(), 2,
                List.of(ing("Daging sapi", "1"), ing("Garam", "secukupnya")),
                List.of("Haluskan bumbu.", "Masak.")));
        mvc.perform(get("/api/v1/recipes/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.author").doesNotExist())
                .andExpect(jsonPath("$.isFavorited", is(false)))
                .andExpect(jsonPath("$.category.name", is("Lauk")))
                .andExpect(jsonPath("$.ingredients[0].name", is("Daging sapi")))
                .andExpect(jsonPath("$.ingredients[1].unit").value((Object) null))
                .andExpect(jsonPath("$.instructions[1].stepNumber", is(2)))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    // ---------- recipes: validation ----------

    @Test
    void nestedValidationErrorsUseBracketNotation() throws Exception {
        Map<String, Object> bad = Map.of(
                "title", "Resep Salah", "cookingTimeMinutes", 10, "servings", 2, "categoryId", 2,
                "imageUrl", "ftp://salah",
                "ingredients", List.of(Map.of("name", "", "quantity", "1")),
                "instructions", List.of(Map.of("description", "ok"), Map.of("description", " ")));
        mvc.perform(post("/api/v1/recipes").header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON).content(json(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("ingredients[0].name")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("instructions[1].description")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("imageUrl")));
    }

    @Test
    void unknownCategoryIsValidationErrorOnCategoryId() throws Exception {
        mvc.perform(post("/api/v1/recipes").header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(recipe("Kategori Hilang", 999, List.of(ing("Garam", "1")), List.of("Masak.")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field", is("categoryId")));
    }

    @Test
    void outOfRangeListParamsAreRejected() throws Exception {
        mvc.perform(get("/api/v1/recipes?size=100")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field", is("size")));
        mvc.perform(get("/api/v1/recipes?page=0")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field", is("page")));
        mvc.perform(get("/api/v1/recipes?sort=acak")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field", is("sort")));
        mvc.perform(get("/api/v1/recipes?page=abc")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field", is("page")));
    }

    // ---------- recipes: search ----------

    @Test
    void searchFiltersPaginationAndSort() throws Exception {
        String admin = adminToken();
        String tag = "zz" + UUID.randomUUID().toString().substring(0, 8);
        createRecipe(admin, recipe("Rendang " + tag, 2,
                List.of(ing("Daging sapi", "1"), ing("Santan kental", "500")), List.of("Masak.")));
        createRecipe(admin, recipe("Gulai " + tag, 2,
                List.of(ing("Santan", "300"), ing("Kambing", "1")), List.of("Masak.")));
        createRecipe(admin, recipe("Sambal " + tag, 5,
                List.of(ing("Cabai", "10")), List.of("Ulek.")));

        mvc.perform(get("/api/v1/recipes?q=" + tag.toUpperCase()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements", is(3)));
        mvc.perform(get("/api/v1/recipes?q=" + tag + "&categoryId=5"))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].title", is("Sambal " + tag)));
        // AND: hanya Rendang punya santan DAN daging
        mvc.perform(get("/api/v1/recipes?q=" + tag + "&ingredient=santan&ingredient=daging"))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].title", is("Rendang " + tag)));
        mvc.perform(get("/api/v1/recipes?q=" + tag + "&ingredient=santan"))
                .andExpect(jsonPath("$.totalElements", is(2)));
        // karakter LIKE diperlakukan literal
        mvc.perform(get("/api/v1/recipes?q=%25"))
                .andExpect(jsonPath("$.totalElements", is(0)));

        mvc.perform(get("/api/v1/recipes?q=" + tag + "&size=2&sort=title"))
                .andExpect(jsonPath("$.page", is(1)))
                .andExpect(jsonPath("$.size", is(2)))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.hasNext", is(true)))
                .andExpect(jsonPath("$.content[*].title", contains("Gulai " + tag, "Rendang " + tag)));
        mvc.perform(get("/api/v1/recipes?q=" + tag + "&size=2&sort=title&page=2"))
                .andExpect(jsonPath("$.hasNext", is(false)))
                .andExpect(jsonPath("$.content", hasSize(1)));
        mvc.perform(get("/api/v1/recipes?q=" + tag + "&sort=newest"))
                .andExpect(jsonPath("$.content[0].title", is("Sambal " + tag)));
        mvc.perform(get("/api/v1/recipes?q=tidak-ada-" + tag))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void ingredientMatchesWordStartNotInsideWords() throws Exception {
        String admin = adminToken();
        String tag = "kt" + UUID.randomUUID().toString().substring(0, 8);
        createRecipe(admin, recipe("Soto " + tag, 2,
                List.of(ing("Daging ayam", "1"), ing("Santan", "100")), List.of("Masak.")));
        createRecipe(admin, recipe("Tumis " + tag, 3,
                List.of(ing("Bayam", "1")), List.of("Tumis.")));
        createRecipe(admin, recipe("Urap " + tag, 3,
                List.of(ing("Bayam", "1"), ing("Ayam suwir", "1")), List.of("Aduk.")));

        // "bayam" memuat "ayam" tetapi bukan awal kata: tidak boleh cocok
        mvc.perform(get("/api/v1/recipes?q=" + tag + "&ingredient=ayam"))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content[*].title", contains("Urap " + tag, "Soto " + tag)));
        mvc.perform(get("/api/v1/recipes?q=" + tag + "&ingredient=ayam&ingredient=bayam"))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].title", is("Urap " + tag)));
        mvc.perform(get("/api/v1/recipes?q=" + tag + "&ingredient=bayam"))
                .andExpect(jsonPath("$.totalElements", is(2)));
        // awalan tetap cocok, kata kedua dalam nama bahan juga
        mvc.perform(get("/api/v1/recipes?q=" + tag + "&ingredient=sant"))
                .andExpect(jsonPath("$.totalElements", is(1)));
        mvc.perform(get("/api/v1/recipes?q=" + tag + "&ingredient=suwir"))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    // ---------- recipes: update and delete ----------

    @Test
    void putReplacesWholeRecipeAndDeleteRemovesIt() throws Exception {
        String admin = adminToken();
        long id = createRecipe(admin, recipe("Awal " + UUID.randomUUID(), 2,
                List.of(ing("A", "1"), ing("B", "2")), List.of("satu", "dua")));

        mvc.perform(put("/api/v1/recipes/" + id).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(recipe("Akhir", 3, List.of(ing("C", "3")), List.of("a", "b", "c")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Akhir")))
                .andExpect(jsonPath("$.category.id", is(3)))
                .andExpect(jsonPath("$.ingredients", hasSize(1)))
                .andExpect(jsonPath("$.instructions", hasSize(3)));
        mvc.perform(get("/api/v1/recipes/" + id))
                .andExpect(jsonPath("$.instructions[*].description", contains("a", "b", "c")))
                .andExpect(jsonPath("$.instructions[*].stepNumber", contains(1, 2, 3)));

        mvc.perform(put("/api/v1/recipes/999999").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(recipe("Resep Hantu", 3, List.of(ing("C", "3")), List.of("a")))))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code", is("NOT_FOUND")));

        mvc.perform(delete("/api/v1/recipes/" + id).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/recipes/" + id)).andExpect(status().isNotFound());
    }

    // ---------- favorites ----------

    @Test
    void favoritesAreIdempotentAndReflectedInListsAndDetail() throws Exception {
        String admin = adminToken();
        String tag = "fav" + UUID.randomUUID().toString().substring(0, 8);
        long id = createRecipe(admin, recipe("Favorit " + tag, 2, List.of(ing("A", "1")), List.of("x")));
        String user = userToken();

        mvc.perform(put("/api/v1/recipes/" + id + "/favorite").header("Authorization", bearer(user)))
                .andExpect(status().isNoContent());
        mvc.perform(put("/api/v1/recipes/" + id + "/favorite").header("Authorization", bearer(user)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/recipes?q=" + tag).header("Authorization", bearer(user)))
                .andExpect(jsonPath("$.content[0].isFavorited", is(true)));
        mvc.perform(get("/api/v1/recipes?q=" + tag))
                .andExpect(jsonPath("$.content[0].isFavorited", is(false)));
        mvc.perform(get("/api/v1/recipes/" + id).header("Authorization", bearer(user)))
                .andExpect(jsonPath("$.isFavorited", is(true)));
        mvc.perform(get("/api/v1/users/me/favorites").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].id", is((int) id)))
                .andExpect(jsonPath("$.content[0].isFavorited", is(true)));

        mvc.perform(delete("/api/v1/recipes/" + id + "/favorite").header("Authorization", bearer(user)))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/recipes/" + id + "/favorite").header("Authorization", bearer(user)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/users/me/favorites").header("Authorization", bearer(user)))
                .andExpect(jsonPath("$.content", hasSize(0)));

        mvc.perform(put("/api/v1/recipes/999999/favorite").header("Authorization", bearer(user)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code", is("NOT_FOUND")));
        mvc.perform(put("/api/v1/recipes/" + id + "/favorite"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deletingRecipeRemovesItFromFavorites() throws Exception {
        String admin = adminToken();
        long id = createRecipe(admin, recipe("Hilang " + UUID.randomUUID(), 2, List.of(ing("A", "1")), List.of("x")));
        String user = userToken();
        mvc.perform(put("/api/v1/recipes/" + id + "/favorite").header("Authorization", bearer(user)))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/recipes/" + id).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/users/me/favorites").header("Authorization", bearer(user)))
                .andExpect(jsonPath("$.totalElements", is(0)));
    }
}
