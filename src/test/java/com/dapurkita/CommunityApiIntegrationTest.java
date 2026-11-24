package com.dapurkita;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Kontrak v2.4: ulasan, komentar, artikel, dan upload foto. Memerlukan Docker. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class CommunityApiIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13, 'I', 'H', 'D', 'R'};

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    // ---------- helpers ----------

    private String json(Object value) throws Exception {
        return mapper.writeValueAsString(value);
    }

    private JsonNode body(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String userToken(String name) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", name, "email", "u" + suffix() + "@mail.com",
                                "password", "rahasia123"))))
                .andExpect(status().isCreated()).andReturn();
        return body(result).get("accessToken").asText();
    }

    private String adminToken() throws Exception {
        String email = "admin-" + suffix() + "@dapurkita.test";
        users.save(new User("Admin Dapur", email, encoder.encode("Admin12345!"), Role.ADMIN));
        MvcResult login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "Admin12345!"))))
                .andExpect(status().isOk()).andReturn();
        return body(login).get("accessToken").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private Map<String, Object> recipe(String title, String imageUrl) {
        Map<String, Object> payload = new HashMap<>(Map.of(
                "title", title, "cookingTimeMinutes", 30, "servings", 2, "categoryId", 1,
                "ingredients", List.of(Map.of("name", "Garam", "quantity", "1")),
                "instructions", List.of(Map.of("description", "Aduk rata."))));
        payload.put("imageUrl", imageUrl);
        return payload;
    }

    private long createRecipe(String admin) throws Exception {
        MvcResult created = mvc.perform(post("/api/v1/recipes").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(json(recipe("Resep " + suffix(), null))))
                .andExpect(status().isCreated()).andReturn();
        return body(created).get("id").asLong();
    }

    private void review(String token, long recipeId, int rating, String content) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("rating", rating);
        payload.put("content", content);
        mvc.perform(put("/api/v1/recipes/" + recipeId + "/reviews/me").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json(payload)))
                .andExpect(status().isOk());
    }

    private long comment(String token, long recipeId, String content, Long parentId) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("content", content);
        payload.put("parentId", parentId);
        MvcResult result = mvc.perform(post("/api/v1/recipes/" + recipeId + "/comments")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json(payload)))
                .andExpect(status().isCreated()).andReturn();
        return body(result).get("id").asLong();
    }

    private Map<String, Object> article(String title, String category, List<String> tags) {
        return Map.of("title", title, "excerpt", "  Ringkasan  ", "content", "Paragraf satu.\n\nParagraf dua.",
                "category", category, "tags", tags);
    }

    private long createArticle(String admin, Map<String, Object> payload) throws Exception {
        MvcResult created = mvc.perform(post("/api/v1/articles").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(json(payload)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/articles/")))
                .andReturn();
        return body(created).get("id").asLong();
    }

    // ---------- ulasan ----------

    @Test
    void reviewsAreOnePerUserAndFeedRecipeRating() throws Exception {
        String admin = adminToken();
        String siti = userToken("Siti");
        String budi = userToken("Budi");
        long id = createRecipe(admin);

        mvc.perform(get("/api/v1/recipes/" + id))
                .andExpect(jsonPath("$.ratingAverage", nullValue()))
                .andExpect(jsonPath("$.ratingCount", is(0)));
        mvc.perform(get("/api/v1/recipes/" + id + "/reviews/me").header("Authorization", bearer(siti)))
                .andExpect(status().isNotFound());

        review(siti, id, 3, "Lumayan");
        review(siti, id, 5, "  Enak sekali  ");   // mengubah, bukan menambah
        review(budi, id, 4, "   ");               // teks kosong disimpan null

        mvc.perform(get("/api/v1/recipes/" + id + "/reviews/me").header("Authorization", bearer(siti)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating", is(5)))
                .andExpect(jsonPath("$.content", is("Enak sekali")))
                .andExpect(jsonPath("$.author.name", is("Siti")))
                .andExpect(jsonPath("$.author.email").doesNotExist());
        mvc.perform(get("/api/v1/recipes/" + id + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content[0].author.name", is("Budi")))
                .andExpect(jsonPath("$.content[0].content", nullValue()));
        mvc.perform(get("/api/v1/recipes/" + id))
                .andExpect(jsonPath("$.ratingAverage", is(4.5)))
                .andExpect(jsonPath("$.ratingCount", is(2)));
        mvc.perform(get("/api/v1/recipes").param("size", "50"))
                .andExpect(jsonPath("$.content[?(@.id == " + id + ")].ratingCount", contains(2)));

        // hapus sendiri idempoten
        mvc.perform(delete("/api/v1/recipes/" + id + "/reviews/me").header("Authorization", bearer(budi)))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/recipes/" + id + "/reviews/me").header("Authorization", bearer(budi)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/recipes/" + id)).andExpect(jsonPath("$.ratingAverage", is(5.0)));
    }

    @Test
    void reviewValidationAuthAndModeration() throws Exception {
        String admin = adminToken();
        String siti = userToken("Siti");
        long id = createRecipe(admin);

        mvc.perform(put("/api/v1/recipes/" + id + "/reviews/me").header("Authorization", bearer(siti))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("rating", 6))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field", is("rating")));
        mvc.perform(put("/api/v1/recipes/" + id + "/reviews/me")
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("rating", 5))))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/recipes/999999/reviews/me").header("Authorization", bearer(siti))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("rating", 5))))
                .andExpect(status().isNotFound());

        review(siti, id, 5, "Mantap");
        long reviewId = body(mvc.perform(get("/api/v1/recipes/" + id + "/reviews/me")
                .header("Authorization", bearer(siti))).andReturn()).get("id").asLong();
        mvc.perform(delete("/api/v1/reviews/" + reviewId).header("Authorization", bearer(siti)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/reviews/" + reviewId).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/reviews/" + reviewId).header("Authorization", bearer(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void featuredReviewsNeedHighRatingAndText() throws Exception {
        String admin = adminToken();
        long id = createRecipe(admin);
        String text = "Unggulan " + suffix();
        review(userToken("Rina"), id, 5, text);
        review(userToken("Dodi"), id, 3, "Biasa " + suffix());

        mvc.perform(get("/api/v1/reviews/featured").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content", is(text)))
                .andExpect(jsonPath("$[0].recipe.id", is((int) id)))
                .andExpect(jsonPath("$[*].rating", not(hasItem(3))));
        mvc.perform(get("/api/v1/reviews/featured").param("size", "11"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field", is("size")));
        mvc.perform(get("/api/v1/recipes/" + id + "/reviews"))
                .andExpect(jsonPath("$.content[0].recipe").doesNotExist());
    }

    // ---------- komentar ----------

    @Test
    void commentsHaveOneLevelOfReplies() throws Exception {
        String admin = adminToken();
        String siti = userToken("Siti");
        String budi = userToken("Budi");
        long id = createRecipe(admin);
        long other = createRecipe(admin);

        long first = comment(siti, id, "Komentar pertama", null);
        long second = comment(budi, id, "  Komentar kedua  ", null);
        comment(budi, id, "Balasan satu", first);
        long reply = comment(siti, id, "Balasan dua", first);

        // balasan tidak bisa dibalas; induk harus dari resep yang sama
        for (Map.Entry<Long, Long> bad : Map.of(id, reply, other, first).entrySet()) {
            mvc.perform(post("/api/v1/recipes/" + bad.getKey() + "/comments").header("Authorization", bearer(siti))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("content", "Tidak boleh", "parentId", bad.getValue()))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field", is("parentId")));
        }
        mvc.perform(post("/api/v1/recipes/" + id + "/comments").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "Anonim"))))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/recipes/" + id + "/comments").header("Authorization", bearer(siti))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("content", "   "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field", is("content")));

        mvc.perform(get("/api/v1/recipes/" + id + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.totalComments", is(4)))
                .andExpect(jsonPath("$.content[0].id", is((int) second)))
                .andExpect(jsonPath("$.content[0].content", is("Komentar kedua")))
                .andExpect(jsonPath("$.content[0].replies", hasSize(0)))
                .andExpect(jsonPath("$.content[1].replies[*].content", contains("Balasan satu", "Balasan dua")))
                .andExpect(jsonPath("$.content[1].replies[0].parentId", is((int) first)))
                .andExpect(jsonPath("$.content[1].replies[0].recipeId", is((int) id)));
    }

    @Test
    void commentDeletionRulesAndCascade() throws Exception {
        String admin = adminToken();
        String siti = userToken("Siti");
        String budi = userToken("Budi");
        long id = createRecipe(admin);
        long parent = comment(siti, id, "Induk", null);
        comment(budi, id, "Balasan", parent);
        long budiComment = comment(budi, id, "Milik Budi", null);

        mvc.perform(delete("/api/v1/comments/" + parent).header("Authorization", bearer(budi)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/comments/" + parent).header("Authorization", bearer(siti)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/recipes/" + id + "/comments"))
                .andExpect(jsonPath("$.totalComments", is(1)));
        mvc.perform(delete("/api/v1/comments/" + budiComment).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/comments/" + budiComment).header("Authorization", bearer(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingRecipeRemovesItsCommentsAndReviews() throws Exception {
        String admin = adminToken();
        String siti = userToken("Siti");
        long id = createRecipe(admin);
        comment(siti, id, "Akan hilang", null);
        review(siti, id, 5, "Akan hilang juga");

        mvc.perform(delete("/api/v1/recipes/" + id).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/recipes/" + id + "/comments")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/recipes/" + id + "/reviews")).andExpect(status().isNotFound());
    }

    // ---------- artikel ----------

    @Test
    void onlyAdminWritesArticlesAndTagsAreNormalized() throws Exception {
        String admin = adminToken();
        String tag = "tag" + suffix();
        Map<String, Object> payload = article("Rahasia Santan", "TIPS", List.of(" " + tag.toUpperCase() + " ", tag, "santan"));

        mvc.perform(post("/api/v1/articles").header("Authorization", bearer(userToken("Siti")))
                        .contentType(MediaType.APPLICATION_JSON).content(json(payload)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/articles").contentType(MediaType.APPLICATION_JSON).content(json(payload)))
                .andExpect(status().isUnauthorized());

        long id = createArticle(admin, payload);
        mvc.perform(get("/api/v1/articles/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags", contains(tag, "santan")))
                .andExpect(jsonPath("$.excerpt", is("Ringkasan")))
                .andExpect(jsonPath("$.readingTimeMinutes", is(1)))
                .andExpect(jsonPath("$.author.name", is("Admin Dapur")))
                .andExpect(jsonPath("$.content", is("Paragraf satu.\n\nParagraf dua.")))
                .andExpect(jsonPath("$.coverImageUrl", nullValue()));

        Map<String, Object> invalid = new HashMap<>(article("ab", "TIPS",
                List.of("1", "2", "3", "4", "5", "6", "7", "8", "9")));
        invalid.put("coverImageUrl", "/api/v1/images/../../etc/passwd");
        MvcResult rejected = mvc.perform(post("/api/v1/articles").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(json(invalid)))
                .andExpect(status().isBadRequest()).andReturn();
        List<String> fields = body(rejected).get("errors").findValuesAsText("field");
        assertEquals(List.of("coverImageUrl", "tags", "title"), fields.stream().sorted().toList());
    }

    @Test
    void articleListFiltersFacetsAndNeighbours() throws Exception {
        String admin = adminToken();
        String tag = "uji" + suffix();
        String word = "Kunyit" + suffix();
        long older = createArticle(admin, article(word + " lama", "BAHAN", List.of(tag)));
        long middle = createArticle(admin, article("Teknik " + suffix(), "TEKNIK", List.of(tag, "dasar")));
        long newer = createArticle(admin, article(word + " baru", "BAHAN", List.of()));

        mvc.perform(get("/api/v1/articles").param("q", word.toLowerCase()))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content[0].id", is((int) newer)))
                .andExpect(jsonPath("$.content[0].content").doesNotExist());
        mvc.perform(get("/api/v1/articles").param("tag", tag.toUpperCase()))
                .andExpect(jsonPath("$.content[*].id", contains((int) middle, (int) older)));
        mvc.perform(get("/api/v1/articles").param("category", "TEKNIK").param("q", "teknik"))
                .andExpect(jsonPath("$.content[*].id", hasItem((int) middle)))
                .andExpect(jsonPath("$.content[*].category", not(hasItem("BAHAN"))));
        mvc.perform(get("/api/v1/articles").param("category", "RESEP"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field", is("category")));

        mvc.perform(get("/api/v1/articles/facets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[*].category", contains("TIPS", "CERITA", "BAHAN", "TEKNIK")))
                .andExpect(jsonPath("$.tags[?(@.name == '" + tag + "')].count", contains(2)));

        mvc.perform(get("/api/v1/articles/" + middle))
                .andExpect(jsonPath("$.previous.id", is((int) older)))
                .andExpect(jsonPath("$.next.id", is((int) newer)));
    }

    @Test
    void putReplacesArticleAndKeepsPublishedAt() throws Exception {
        String admin = adminToken();
        long id = createArticle(admin, article("Judul awal", "CERITA", List.of("a", "b")));
        String publishedAt = body(mvc.perform(get("/api/v1/articles/" + id)).andReturn()).get("publishedAt").asText();

        String longText = String.join(" ", java.util.Collections.nCopies(450, "kata"));
        Map<String, Object> replacement = Map.of("title", "Judul baru", "content", longText, "category", "TIPS",
                "tags", List.of("b", "a"));
        mvc.perform(put("/api/v1/articles/" + id).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(json(replacement)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Judul baru")))
                .andExpect(jsonPath("$.excerpt", nullValue()))
                .andExpect(jsonPath("$.tags", contains("b", "a")))
                .andExpect(jsonPath("$.readingTimeMinutes", is(3)))
                .andExpect(jsonPath("$.publishedAt", is(publishedAt)));

        mvc.perform(delete("/api/v1/articles/" + id).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/articles/" + id)).andExpect(status().isNotFound());
    }

    // ---------- foto ----------

    @Test
    void adminUploadsImageThatIsServedAndUsableAsRecipeImage() throws Exception {
        String admin = adminToken();
        MvcResult uploaded = mvc.perform(multipart("/api/v1/images")
                        .file(new MockMultipartFile("file", "foto.txt", "text/plain", PNG))
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url", matchesPattern("^/api/v1/images/[a-f0-9]{32}\\.png$")))
                .andReturn();
        String url = body(uploaded).get("url").asText();

        MvcResult served = mvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("Cache-Control", containsString("immutable")))
                .andReturn();
        assertArrayEquals(PNG, served.getResponse().getContentAsByteArray());

        mvc.perform(post("/api/v1/recipes").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(json(recipe("Bergambar " + suffix(), url))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrl", is(url)));
        mvc.perform(post("/api/v1/recipes").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(recipe("Jalur aneh " + suffix(), "/api/v1/images/x.jpg"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field", is("imageUrl")));
    }

    @Test
    void uploadRejectsNonImagesAndNonAdmins() throws Exception {
        String admin = adminToken();
        MockMultipartFile fake = new MockMultipartFile("file", "foto.jpg", "image/jpeg",
                "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/v1/images").file(fake).header("Authorization", bearer(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field", is("file")));
        mvc.perform(multipart("/api/v1/images").header("Authorization", bearer(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field", is("file")));

        MockMultipartFile png = new MockMultipartFile("file", "a.png", "image/png", PNG);
        mvc.perform(multipart("/api/v1/images").file(png).header("Authorization", bearer(userToken("Siti"))))
                .andExpect(status().isForbidden());
        mvc.perform(multipart("/api/v1/images").file(png)).andExpect(status().isUnauthorized());

        mvc.perform(get("/api/v1/images/" + "0".repeat(32) + ".jpg")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/images/bukan-nama-valid.jpg")).andExpect(status().isNotFound());
    }
}
