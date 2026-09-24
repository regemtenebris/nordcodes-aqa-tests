# NordCodes AQA — автотесты сервиса `/endpoint`

[![API tests](https://github.com/regemtenebris/nordcodes-aqa-tests/actions/workflows/tests.yml/badge.svg)](https://github.com/regemtenebris/nordcodes-aqa-tests/actions/workflows/tests.yml)
[![Allure Report](https://img.shields.io/badge/Allure-отчёт-orange)](https://regemtenebris.github.io/nordcodes-aqa-tests/)

Автоматизированные API-тесты для Spring Boot-приложения `internal-0.0.1-SNAPSHOT.jar`.

**📊 [Открыть Allure-отчёт последнего прогона](https://regemtenebris.github.io/nordcodes-aqa-tests/)** — ничего устанавливать не нужно.

## Результаты

- **65 автотестов** по [тест-плану](docs/test-plan.md): вход, действие, выход, валидация, безопасность, протокол, конкурентность
- **54 теста** регрессии — проходят
- **11 тестов** воспроизводят **4 найденных дефекта** — падают до исправления

| # | Дефект | Серьёзность |
|---|---|---|
| [#1](https://github.com/regemtenebris/nordcodes-aqa-tests/issues/1) | Токены с буквами G–Z отклоняются, хотя документация их допускает | Major |
| [#2](https://github.com/regemtenebris/nordcodes-aqa-tests/issues/2) | Ответ 3xx от внешнего сервиса считается успехом | Major |
| [#3](https://github.com/regemtenebris/nordcodes-aqa-tests/issues/3) | Сбой внешнего сервиса возвращается как 500, а не 502 | Minor |
| [#4](https://github.com/regemtenebris/nordcodes-aqa-tests/issues/4) | Некорректные HTTP-запросы приводят к 500 вместо 404/405/415 | Minor |

Подробные баг-репорты: [docs/bug-reports.md](docs/bug-reports.md).

## Стек

Java 17 · JUnit 5 · REST Assured · WireMock · Allure · AssertJ · Maven (Wrapper) · GitHub Actions · Jenkins

## Быстрый старт

Нужна только **Java 17+**. Maven ставить не нужно — используется Maven Wrapper.

```bash
git clone https://github.com/regemtenebris/nordcodes-aqa-tests.git
cd nordcodes-aqa-tests
./mvnw test                      # Windows: mvnw.cmd test
```

Тесты **сами** поднимают заглушку внешнего сервиса (WireMock) и запускают приложение из `app/`
на свободных портах, а после прогона всё останавливают. Ничего запускать вручную не нужно.

### Отчёт Allure локально

```bash
./mvnw allure:serve              # или: allure serve target/allure-results
```

### Выборочный запуск

| Команда | Что запускается |
|---|---|
| `./mvnw test -DexcludedGroups=known-bug` | Регрессия без известных дефектов (должна быть зелёной) |
| `./mvnw test -Dgroups=known-bug` | Только тесты на известные дефекты |
| `./mvnw test -Dgroups=smoke` | Smoke-тест основного сценария |
| `./mvnw test -Dtest=LoginTest` | Один класс |

### Настройки

Все параметры — в [`test.properties`](src/test/resources/test.properties).
Любой можно переопределить без правки файла: `-Dkey=value` или переменной окружения `KEY_NAME`.

```bash
./mvnw test -Dapp.jar.path=/path/to/other.jar -Dapp.startup.timeout.seconds=120
```

## Архитектура

```
src/test/java/kz/dias/aqa/
├── tests/            Тестовые сценарии (язык бизнеса) + BaseApiTest
├── client/           EndpointClient — единственная точка работы с HTTP API
├── steps/            Проверки ответов, управление заглушкой, проверка сессии (Allure @Step)
├── model/            Контракт ответа: record ApiResponse, enum Result/Action
├── data/             Генерация тестовых данных (уникальный токен на каждый тест)
├── infrastructure/   Запуск WireMock и приложения, ожидание готовности
├── extensions/       JUnit 5 Extension: окружение одно на весь прогон
├── reporting/        Allure: Environment, Categories, вложения при падении
└── config/           Конфигурация: -D > ENV > test.properties
```

### Ключевые решения

- **Самодостаточный запуск.** JUnit 5 Extension поднимает WireMock и приложение один раз на весь прогон
  (root `ExtensionContext.Store`) и гарантированно останавливает их, включая аварийное завершение (shutdown hook).
- **Случайные порты.** Нет конфликтов с занятыми 8080/8888, тесты запускаются на любой машине и в CI.
- **Изоляция тестов.** Каждый тест использует свой случайный токен, заглушка сбрасывается перед каждым тестом.
  Тесты независимы и выполняются в любом порядке.
- **Проверка побочных эффектов.** Проверяется не только ответ приложения, но и запросы во внешний сервис
  (URL, заголовки, тело) — и их отсутствие там, где их быть не должно.
- **Известные дефекты помечены тегом `known-bug`.** Падение регрессии = новая проблема, а не старая.
- **Отчёт для не-программиста.** Русские названия, группировка по функциональности, серьёзность,
  понятные описания дефектов, ссылки на Issues; при падении прикладываются запросы во внешний сервис и лог приложения.
- **Ожидания без документации.** Где документация молчит (коды ошибок), ожидания основаны на стандарте HTTP
  (RFC 9110), это явно указано в описании тестов.

## CI

- **GitHub Actions** ([workflow](.github/workflows/tests.yml)): регрессия как «шлюз» → тесты известных дефектов →
  Allure-отчёт с историей прогонов → публикация на GitHub Pages.
- **Jenkins** ([Jenkinsfile](Jenkinsfile)): та же логика; регрессия красная при падении,
  известные дефекты делают сборку UNSTABLE (жёлтой). Отчёт через Allure Jenkins Plugin.

## Документация

- [Тест-план и результаты исследовательского тестирования](docs/test-plan.md)
- [Баг-репорты](docs/bug-reports.md)
