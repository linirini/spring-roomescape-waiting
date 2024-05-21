package roomescape.acceptance;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import roomescape.service.theme.dto.ThemeRequest;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql("/truncate-with-guests.sql")
class ThemeAcceptanceTest extends AcceptanceTest {
    private long themeId;

    @DisplayName("테마 추가 성공 테스트")
    @Test
    void createTheme() {
        //given
        ThemeRequest themeRequest = new ThemeRequest("레벨2 탈출", "우테코 레벨2를 탈출하는 내용입니다.",
                "https://i.pinimg.com/236x/6e/bc/46/6ebc461a94a49f9ea3b8bbe2204145d4.jpg");

        //when&then
        RestAssured.given().contentType(ContentType.JSON).body(themeRequest)
                .when().post("/themes")
                .then().log().all().statusCode(201).body("id", is(greaterThan(0)));
    }

    @DisplayName("테마 추가 실패 테스트 - 썸네일 형식 오류")
    @Test
    void cannotCreateThemeByThumbnail() {
        //given
        ThemeRequest themeRequest = new ThemeRequest("레벨2 탈출", "우테코 레벨2를 탈출하는 내용입니다.", "//i.pinimg.com/236x/6e/bc/4");

        //when&then
        RestAssured.given().contentType(ContentType.JSON).body(themeRequest)
                .when().post("/themes")
                .then().log().all().statusCode(400).body("message", is("올바르지 않은 썸네일 형식입니다."));
    }

    @DisplayName("테마 조회 성공 테스트")
    @TestFactory
    Stream<DynamicTest> findAll() {
        ThemeRequest themeRequest = new ThemeRequest("레벨2 탈출", "우테코 레벨2를 탈출하는 내용입니다.",
                "https://i.pinimg.com/236x/6e/bc/46/6ebc461a94a49f9ea3b8bbe2204145d4.jpg");
        return Stream.of(
                DynamicTest.dynamicTest("테마를 생성한다.", () -> {
                    RestAssured.given().contentType(ContentType.JSON).body(themeRequest)
                            .when().post("/themes")
                            .then().extract().response().jsonPath().get("id");
                }),
                DynamicTest.dynamicTest("모든 테마를 조회한다.", () -> {
                    RestAssured.given().log().all()
                            .when().get("/themes")
                            .then().log().all()
                            .assertThat().statusCode(200).body("size()", is(1));
                })
        );

        //when&then
    }

    @DisplayName("테마 삭제 성공 테스트")
    @TestFactory
    Stream<DynamicTest> deleteTheme() {
        ThemeRequest themeRequest = new ThemeRequest("레벨2 탈출", "우테코 레벨2를 탈출하는 내용입니다.",
                "https://i.pinimg.com/236x/6e/bc/46/6ebc461a94a49f9ea3b8bbe2204145d4.jpg");
        return Stream.of(
                DynamicTest.dynamicTest("테마를 생성한다.", () -> {
                    themeId = (int) RestAssured.given().contentType(ContentType.JSON).body(themeRequest)
                            .when().post("/themes")
                            .then().extract().response().jsonPath().get("id");
                }),
                DynamicTest.dynamicTest("테마를 삭제한다.", () -> {
                    RestAssured.given().log().all()
                            .when().delete("/themes/" + themeId)
                            .then().log().all().statusCode(204);

                }),
                DynamicTest.dynamicTest("모든 테마를 조회하면 0개이다.", () -> {
                    RestAssured.given().log().all()
                            .when().get("/themes")
                            .then().log().all()
                            .assertThat().statusCode(200).body("size()", is(0));
                })
        );
    }

    @DisplayName("인기 테마 조회 성공 테스트")
    @Test
    @Sql("/insert-past-reservation.sql")
    void findPopularThemes() {
        //when&then
        RestAssured.given().log().all()
                .when().get("/themes/popular")
                .then().log().all()
                .assertThat().statusCode(200).body("size()", is(2));
    }
}