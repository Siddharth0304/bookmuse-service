package com.bookmuse.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.DockerClientFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIf("databaseAvailable")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "app.jwt-secret=integration-test-only-secret-at-least-32-bytes",
    "app.admin-email=admin@example.test","app.admin-password=AdminPassword123!"})
class BackendIntegrationTest {
    static PostgreSQLContainer db=new PostgreSQLContainer("postgres:16-alpine");
    static boolean databaseAvailable() {
        return System.getenv("TEST_DB_URL")!=null || DockerClientFactory.instance().isDockerAvailable();
    }
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        if(System.getenv("TEST_DB_URL")!=null) {
            r.add("spring.datasource.url",()->System.getenv("TEST_DB_URL"));
            r.add("spring.datasource.username",()->System.getenv("TEST_DB_USERNAME"));
            r.add("spring.datasource.password",()->System.getenv("TEST_DB_PASSWORD"));
        } else {
            db.start();r.add("spring.datasource.url",db::getJdbcUrl);r.add("spring.datasource.username",db::getUsername);r.add("spring.datasource.password",db::getPassword);
        }
    }
    @AfterAll static void stopDatabase() { if(db.isRunning()) db.stop(); }
    @Value("${local.server.port}") int port;
    private final HttpClient client=HttpClient.newHttpClient();
    private final JsonMapper json=JsonMapper.builder().build();
    private record Result(int status,JsonNode body) {}
    private Result call(String method,String path,String token,Object body) throws Exception {
        var b=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api"+path));
        if(token!=null)b.header("Authorization","Bearer "+token);
        b.header("Content-Type","application/json").method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        var r=client.send(b.build(),HttpResponse.BodyHandlers.ofString());
        return new Result(r.statusCode(),r.body().isBlank()?json.nullNode():json.readTree(r.body()));
    }
    private String register(String prefix) throws Exception {
        var r=call("POST","/auth/register",null,Map.of("email",prefix+UUID.randomUUID()+"@example.test","password","ReaderPassword123!","name",prefix));
        assertEquals(201,r.status());return r.body().path("accessToken").asText();
    }
    private String admin() throws Exception { return call("POST","/auth/login",null,Map.of("email","admin@example.test","password","AdminPassword123!")).body().path("accessToken").asText(); }
    private String createBook(String token,String title,String genre) throws Exception {
        var r=call("POST","/books",token,Map.of("title",title,"author","Author","genres",java.util.List.of(genre),"summary","Sample"));
        assertEquals(201,r.status());return r.body().path("id").asText();
    }
    @Test void shouldFilterBooksByAnySelectedGenreAndCombineWithSearch() throws Exception {
        String admin=admin(),reader=register("Genre reader");
        String prefix="Filter"+UUID.randomUUID();
        String fantasy="fantasy "+UUID.randomUUID(),mystery="mystery "+UUID.randomUUID();
        String first=createBook(admin,prefix+" A",fantasy);
        String second=createBook(admin,prefix+" B",mystery);
        createBook(admin,prefix+" C","history");
        createBook(admin,"Outside search",mystery);
        // A similar label must not count as an exact genre match.
        createBook(admin,prefix+" D",fantasy+" extra");
        String f=java.net.URLEncoder.encode(fantasy,java.nio.charset.StandardCharsets.UTF_8);
        String m=java.net.URLEncoder.encode(mystery,java.nio.charset.StandardCharsets.UTF_8);

        var single=call("GET","/books?search="+prefix+"&genre="+f,reader,null);
        assertEquals(200,single.status());
        assertEquals(1,single.body().path("totalElements").asInt());
        assertEquals(first,single.body().path("content").get(0).path("id").asText());

        for(String filter:java.util.List.of("genre="+f+"&genre="+m,"genre="+f+","+m)) {
            var result=call("GET","/books?search="+prefix+"&"+filter+"&size=1&page=1",reader,null);
            assertEquals(200,result.status());
            assertEquals(2,result.body().path("totalElements").asInt());
            assertEquals(2,result.body().path("totalPages").asInt());
            assertEquals(second,result.body().path("content").get(0).path("id").asText());
        }
        String mixed=java.net.URLEncoder.encode("  "+fantasy.toUpperCase(java.util.Locale.ROOT)+", , "+mystery+"  ",java.nio.charset.StandardCharsets.UTF_8);
        var normalized=call("GET","/books?search="+prefix+"&genre="+mixed+"&genre="+f,reader,null);
        assertEquals(200,normalized.status());
        assertEquals(2,normalized.body().path("totalElements").asInt());
        assertEquals(0,call("GET","/books?search="+prefix+"&genre=%25,%5F",reader,null).body().path("totalElements").asInt());
        assertEquals(4,call("GET","/books?search="+prefix+"&genre=,%20",reader,null).body().path("totalElements").asInt());
        assertEquals(4,call("GET","/books?search="+prefix,reader,null).body().path("totalElements").asInt());
    }
    @Test void shouldHideAdministratorsAcrossApisWhileKeepingAdministratorVisibility() throws Exception {
        String admin=admin(),reader=register("Visibility reader"),other=register("Visible follower");
        String adminId=call("GET","/me/account",admin,null).body().path("id").asText();
        String readerId=call("GET","/me",reader,null).body().path("id").asText();
        call("PUT","/users/"+readerId+"/follow",admin,null);
        call("PUT","/users/"+readerId+"/follow",other,null);

        var readers=call("GET","/users?size=100",reader,null);
        var adminReaders=call("GET","/users?size=100",admin,null);
        assertEquals(200,readers.status());
        assertFalse(readers.body().toString().contains(adminId));
        assertEquals(adminReaders.body().path("totalElements").asLong()-1,readers.body().path("totalElements").asLong());
        assertTrue(call("GET","/admin/users?size=100",admin,null).body().toString().contains(adminId));
        assertEquals(403,call("GET","/admin/users",reader,null).status());

        for(String suffix:java.util.List.of("","/followers","/following","/library")) {
            assertEquals(404,call("GET","/users/"+adminId+suffix,reader,null).status());
            assertEquals(200,call("GET","/users/"+adminId+suffix,admin,null).status());
        }
        assertEquals(404,call("PUT","/users/"+adminId+"/follow",reader,null).status());
        assertEquals(1,call("GET","/users/"+readerId,reader,null).body().path("followers").asInt());
        assertEquals(2,call("GET","/users/"+readerId,admin,null).body().path("followers").asInt());
        var followers=call("GET","/users/"+readerId+"/followers?size=1",reader,null).body();
        assertEquals(1,followers.path("totalElements").asInt());
        assertEquals(1,followers.path("totalPages").asInt());
        assertFalse(followers.toString().contains(adminId));

        String book=createBook(admin,"Visibility catalogue book","fiction");
        String readerPost=call("POST","/posts",reader,Map.of("bookId",book,"body","Visible post")).body().path("id").asText();
        String adminPost=call("POST","/posts",admin,Map.of("bookId",book,"body","Administrator post")).body().path("id").asText();
        String adminComment=call("POST","/posts/"+readerPost+"/comments",admin,Map.of("body","Administrator comment")).body().path("id").asText();
        call("PUT","/posts/"+readerPost+"/like",admin,null);
        call("POST","/posts/"+readerPost+"/shares",admin,null);
        var visiblePosts=call("GET","/posts?bookId="+book+"&size=1",reader,null).body();
        assertEquals(1,visiblePosts.path("totalElements").asInt());
        assertFalse(visiblePosts.toString().contains(adminId));
        var visiblePost=call("GET","/posts/"+readerPost,reader,null).body();
        for(String count:java.util.List.of("likes","shares","comments")) {
            assertEquals(0,visiblePost.path(count).asInt());
            assertEquals(1,call("GET","/posts/"+readerPost,admin,null).body().path(count).asInt());
        }
        assertEquals(0,call("GET","/posts/"+readerPost+"/comments",reader,null).body().path("totalElements").asInt());
        assertEquals(1,call("GET","/posts/"+readerPost+"/comments",admin,null).body().path("totalElements").asInt());
        assertEquals(404,call("GET","/posts/"+adminPost,reader,null).status());
        assertEquals(200,call("GET","/posts/"+adminPost,admin,null).status());
        assertEquals(404,call("POST","/posts/"+adminPost+"/comments",reader,Map.of("body","Hidden parent")).status());
        assertEquals(404,call("PUT","/comments/"+adminComment,reader,Map.of("body","Hidden comment")).status());
        assertEquals(200,call("GET","/books/"+book,reader,null).status());

        String readerClub=call("POST","/clubs",reader,Map.of("name","Visible club")).body().path("id").asText();
        String adminClub=call("POST","/clubs",admin,Map.of("name","Administrator club")).body().path("id").asText();
        call("PUT","/clubs/"+readerClub+"/membership",admin,null);
        String readerDiscussion=call("POST","/clubs/"+readerClub+"/discussions",reader,Map.of("title","Visible discussion","body","Reader thread")).body().path("id").asText();
        String adminDiscussion=call("POST","/clubs/"+readerClub+"/discussions",admin,Map.of("title","Hidden discussion","body","Administrator thread")).body().path("id").asText();
        call("POST","/discussions/"+readerDiscussion+"/replies",admin,Map.of("body","Administrator reply"));
        assertFalse(call("GET","/clubs?size=100",reader,null).body().toString().contains(adminId));
        assertTrue(call("GET","/clubs?size=100",admin,null).body().toString().contains(adminId));
        assertEquals(404,call("GET","/clubs/"+adminClub,reader,null).status());
        assertEquals(200,call("GET","/clubs/"+adminClub,admin,null).status());
        var members=call("GET","/clubs/"+readerClub+"/members?size=1",reader,null).body();
        assertEquals(1,members.path("totalElements").asInt());
        assertEquals(1,members.path("totalPages").asInt());
        assertFalse(members.toString().contains(adminId));
        assertEquals(2,call("GET","/clubs/"+readerClub+"/members",admin,null).body().path("totalElements").asInt());
        assertEquals(1,call("GET","/clubs/"+readerClub+"/discussions",reader,null).body().path("totalElements").asInt());
        assertEquals(2,call("GET","/clubs/"+readerClub+"/discussions",admin,null).body().path("totalElements").asInt());
        assertEquals(404,call("GET","/discussions/"+adminDiscussion+"/replies",reader,null).status());
        assertEquals(200,call("GET","/discussions/"+adminDiscussion+"/replies",admin,null).status());
        assertEquals(0,call("GET","/discussions/"+readerDiscussion+"/replies",reader,null).body().path("totalElements").asInt());
        assertEquals(1,call("GET","/discussions/"+readerDiscussion+"/replies",admin,null).body().path("totalElements").asInt());
        var notifications=call("GET","/notifications",reader,null).body();
        assertEquals(1,notifications.path("totalElements").asInt());
        assertFalse(notifications.toString().contains(adminId));
        assertEquals(1,call("GET","/notifications/unread-count",reader,null).body().path("count").asInt());
    }
    @Test void shouldEnforceRolesOwnershipBlockingAndRefreshRotation() throws Exception {
        assertEquals(401,call("GET","/books",null,null).status());
        String admin=admin(),reader=register("Reader"),other=register("Other");
        String book=createBook(admin,"Security book","fiction");
        assertEquals(403,call("POST","/books",reader,Map.of("title","Denied","author","A","genres",java.util.List.of("fiction"))).status());
        String id=call("POST","/posts",reader,Map.of("bookId",book,"body","A review")).body().path("id").asText();
        assertEquals(403,call("PUT","/posts/"+id,other,Map.of("body","Stolen")).status());
        assertEquals(204,call("PUT","/posts/"+id+"/like",other,null).status());
        call("PUT","/posts/"+id+"/like",other,null);
        assertEquals(1,call("GET","/posts/"+id,reader,null).body().path("likes").asInt());
        assertEquals(1,call("GET","/notifications/unread-count",reader,null).body().path("count").asInt());
        var registration=call("POST","/auth/register",null,Map.of("email",UUID.randomUUID()+"@example.test","password","Password123!","name","Refresh reader"));
        String refresh=registration.body().path("refreshToken").asText();
        var rotated=call("POST","/auth/refresh",null,Map.of("refreshToken",refresh));assertEquals(200,rotated.status());
        assertNotEquals(refresh,rotated.body().path("refreshToken").asText());
        assertEquals(401,call("POST","/auth/refresh",null,Map.of("refreshToken",refresh)).status());
        String user=call("GET","/me",reader,null).body().path("id").asText();
        assertEquals(200,call("PUT","/admin/users/"+user+"/status",admin,Map.of("blocked",true)).status());
        assertEquals(401,call("GET","/me",reader,null).status());
        call("PUT","/admin/users/"+user+"/status",admin,Map.of("blocked",false));
        assertEquals(401,call("GET","/me",reader,null).status());
    }
    @Test void shouldRequireClubMembershipAndGenerateReplyAndJoinNotifications() throws Exception {
        String owner=register("Owner"),reader=register("Member");
        String club=call("POST","/clubs",owner,Map.of("name","Reading club","description","Books")).body().path("id").asText();
        assertEquals(403,call("POST","/clubs/"+club+"/discussions",reader,Map.of("title","Denied","body","Body")).status());
        assertEquals(204,call("PUT","/clubs/"+club+"/membership",reader,null).status());
        assertEquals(403,call("PUT","/clubs/"+club,reader,Map.of("name","Stolen")).status());
        String discussion=call("POST","/clubs/"+club+"/discussions",owner,Map.of("title","Discuss","body","Thoughts?")).body().path("id").asText();
        assertEquals(201,call("POST","/discussions/"+discussion+"/replies",reader,Map.of("body","Great read")).status());
        assertEquals(2,call("GET","/notifications/unread-count",owner,null).body().path("count").asInt());
        call("PUT","/notifications/read-all",owner,null);
        assertEquals(0,call("GET","/notifications/unread-count",owner,null).body().path("count").asInt());
        assertEquals(2,call("GET","/clubs/"+club+"/members",reader,null).body().path("totalElements").asInt());
    }
    @Test void shouldRankGenreAffinityExcludeLibraryAndFallBackToPopularity() throws Exception {
        String admin=admin(),reader=register("Recommendation"),popularReader=register("Popularity");
        String unique="genre "+UUID.randomUUID().toString();
        String first=createBook(admin,"Affinity candidate",unique),owned=createBook(admin,"Owned",unique),popular=createBook(admin,"Popular","history");
        call("PUT","/me/library/"+owned,reader,Map.of("status","READ"));
        call("PUT","/me/library/"+popular,popularReader,Map.of("status","READING","progress",20));
        call("PUT","/me/library/"+popular,register("Popularity two"),Map.of("status","WANT_TO_READ"));
        call("PUT","/me/library/"+popular,register("Popularity three"),Map.of("status","WANT_TO_READ"));
        call("PUT","/me",reader,Map.of("name","Recommendation","favouriteGenres",java.util.List.of(unique)));
        var r=call("GET","/me/recommendations?size=100",reader,null);assertEquals(200,r.status());
        assertEquals(first,r.body().path("content").get(0).path("id").asText());
        for(var b:r.body().path("content")) assertNotEquals(owned,b.path("id").asText());
        assertFalse(r.body().path("content").isEmpty());
        var page=call("GET","/books?size=1&page=0",reader,null).body();
        assertEquals(1,page.path("content").size());assertTrue(page.path("totalElements").asLong()>=3);
        String newcomer=register("Newcomer");
        var fallback=call("GET","/me/recommendations?size=100",newcomer,null);assertEquals(200,fallback.status());
        assertEquals(popular,fallback.body().path("content").get(0).path("id").asText());
    }
    @Test void shouldSupportSocialAndLibraryLifecycleWithoutExposingPrivateData() throws Exception {
        String admin=admin(),author=register("Author"),reader=register("Follower");
        String authorId=call("GET","/me",author,null).body().path("id").asText();
        String readerId=call("GET","/me",reader,null).body().path("id").asText();
        assertEquals(204,call("PUT","/users/"+authorId+"/follow",reader,null).status());
        call("PUT","/users/"+authorId+"/follow",reader,null);
        assertEquals(1,call("GET","/users/"+authorId,reader,null).body().path("followers").asInt());
        var publicProfile=call("GET","/users/"+authorId,reader,null).body();
        assertFalse(publicProfile.has("email"));assertFalse(publicProfile.has("passwordHash"));
        assertEquals(1,call("GET","/users/"+readerId+"/following",reader,null).body().path("totalElements").asInt());
        String book=createBook(admin,"Lifecycle book","classics");
        assertEquals(200,call("PUT","/me/library/"+book,author,Map.of("status","READ","progress",30)).status());
        assertEquals(1,call("GET","/me",author,null).body().path("booksRead").asInt());
        assertEquals(100,call("GET","/users/"+authorId+"/library",reader,null).body().path("content").get(0).path("progress").asInt());
        String post=call("POST","/posts",author,Map.of("bookId",book,"body","Thoughts")).body().path("id").asText();
        assertEquals(201,call("POST","/posts/"+post+"/shares",reader,null).status());
        var comment=call("POST","/posts/"+post+"/comments",reader,Map.of("body","My comment"));assertEquals(201,comment.status());
        String commentId=comment.body().path("id").asText();
        assertEquals(403,call("DELETE","/comments/"+commentId,author,null).status());
        assertEquals(200,call("PUT","/comments/"+commentId,reader,Map.of("body","Edited comment")).status());
        var postView=call("GET","/posts/"+post,author,null).body();assertEquals(1,postView.path("shares").asInt());assertEquals(1,postView.path("comments").asInt());
        assertEquals(204,call("DELETE","/comments/"+commentId,reader,null).status());
        assertEquals(204,call("DELETE","/users/"+authorId+"/follow",reader,null).status());
        assertEquals(204,call("DELETE","/me/library/"+book,author,null).status());
        assertEquals(0,call("GET","/me",author,null).body().path("booksRead").asInt());
        assertEquals(200,call("PUT","/books/"+book,admin,Map.of("title","Edited book","author","Author","genres",java.util.List.of("classics"))).status());
        assertEquals(204,call("DELETE","/books/"+book,admin,null).status());
        assertEquals(404,call("GET","/posts/"+post,author,null).status());
    }
}
