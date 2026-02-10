package io.berndruecker.demo;

import static io.camunda.process.test.api.CamundaAssert.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

import io.camunda.client.CamundaClient;
import io.camunda.client.annotation.Deployment;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.process.test.api.CamundaProcessTestContext;
import io.camunda.process.test.api.CamundaSpringProcessTest;

@SpringBootTest(classes = EmailAgentTests.TestProcessApplication.class)
@CamundaSpringProcessTest
@org.testcontainers.junit.jupiter.Testcontainers
class EmailAgentTests {


  // Need an app to deploy
  @SpringBootApplication
  @Deployment(resources = {"classpath:/simple-email-agent.bpmn", "classpath:/load-customer.bpmn"})
  static class TestProcessApplication {}

  private static final String EMAIL_ADDRESS = "demo@camunda.com";
  
    @Autowired private CamundaClient client;
    @Autowired private CamundaProcessTestContext processTestContext;
    
    @Test
    void firstTest() throws Exception {
//      CamundaAssert.setAssertionTimeout(Duration.ofSeconds(75)); // increase for LLM calls
      
        Map<String, Object> variables = new HashMap<String, Object>();        
        variables.put("message", buildMessage("I need my master data documents"));        

        final ProcessInstanceEvent processInstance =
            client
                .newCreateInstanceCommand()
                .bpmnProcessId("simple-email-agent")
                .latestVersion()
                .variables(variables)
                .send()
                .join();
        
        assertThat(processInstance)
          .isCompleted()
          .hasVariableSatisfies("agent", String.class, agent -> {
            System.out.println(agent);            
           })
          .hasCompletedElement("Tool_Ask", 1)
          .hasCompletedElement("Tool_LoadCustomer", 1)
          .hasVariableSatisfies("customer", java.util.Map.class, customer -> {
              Assertions.assertThat(customer).containsEntry("id", "1234");
          });
        
    }
    public static String buildMessage(String text) {
      return buildMessage(UUID.randomUUID().toString(), EMAIL_ADDRESS, text);
      
    }    
    public static String buildMessage(String id, String from, String text) {
        return "{\r\n"
            + "    \"id\": \""+id+"\",\r\n"
            + "    \"from\": \"" + from + "\",\r\n"
            + "    \"text\": \"" + text + "\""
            + "}";
    }
}
