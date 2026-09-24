package kz.dias.aqa.tests;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.Allure;
import kz.dias.aqa.data.TestData;
import kz.dias.aqa.model.Action;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static kz.dias.aqa.steps.ResponseAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Race conditions: many identical requests for the same token at the same moment.
 */
@Epic("Сервис авторизации пользователей (/endpoint)")
@Feature("Надёжность")
@Story("Одновременные запросы")
@Severity(SeverityLevel.NORMAL)
@DisplayName("Одновременные запросы")
class ConcurrencyTest extends BaseApiTest {

    private static final int PARALLEL_REQUESTS = 20;

    @Test
    @DisplayName("Из 20 одновременных входов с одним токеном успешен ровно один, внешний сервис вызывается один раз")
    void concurrentLoginsCreateSingleSession() {
        String token = TestData.validToken();

        List<Integer> statuses = api.sendConcurrently(token, Action.LOGIN, PARALLEL_REQUESTS);
        Allure.addAttachment("HTTP-статусы ответов", statuses.toString());

        assertThat(statuses).filteredOn(s -> s == 200).as("Успешных ответов (200)").hasSize(1);
        assertThat(statuses).filteredOn(s -> s == 409).as("Отказов «уже существует» (409)")
                .hasSize(PARALLEL_REQUESTS - 1);
        externalService.verifyAuthCalledOnceWith(token);
    }

    @Test
    @DisplayName("Из 20 одновременных выходов с одним токеном успешен ровно один")
    void concurrentLogoutsSucceedOnce() {
        String token = TestData.validToken();
        assertSuccess(api.login(token));

        List<Integer> statuses = api.sendConcurrently(token, Action.LOGOUT, PARALLEL_REQUESTS);
        Allure.addAttachment("HTTP-статусы ответов", statuses.toString());

        assertThat(statuses).filteredOn(s -> s == 200).as("Успешных ответов (200)").hasSize(1);
        assertThat(statuses).filteredOn(s -> s == 403).as("Отказов «не найден» (403)")
                .hasSize(PARALLEL_REQUESTS - 1);
    }
}
