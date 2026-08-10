package EventTicketing.controller;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class EventControllerTest {

    @Test
    void shouldExposeExpectedEventEndpoints() throws Exception {
        Class<?> controllerClass = Class.forName("EventTicketing.controller.EventController");

        assertThat(controllerClass).isNotNull();
        assertThat(controllerClass.getDeclaredMethods())
                .extracting(Method::getName)
                .contains("browse", "detail", "create", "update", "publish", "organizerCancel");
    }
}
