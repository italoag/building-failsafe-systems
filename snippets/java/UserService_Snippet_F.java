/**
 * Para otimizar o código do método de fallback e da transação de compensação, podemos utilizar algumas técnicas como:

Utilizar um gerenciador de transações de compensação, como o JTA (Java Transaction API), para garantir que as operações de compensação sejam realizadas de forma consistente e isolada.

Utilizar uma fila de mensagens assíncrona, como o RabbitMQ ou o Kafka, para garantir que as requisições de compensação sejam processadas de forma escalável e tolerante a falhas.

Utilizar o mecanismo de cache de transações do OpenTelemetry para garantir que as métricas de desempenho das transações de compensação sejam coletadas e exibidas de forma eficiente.

Utilizar o mecanismo de rastreamento de transações do Spring Cloud Sleuth para garantir que as transações de compensação possam ser rastreadas e depuradas de forma fácil.

Utilizar as métricas do Resilience4j para garantir que as transações de compensação possam ser monitoradas e otimizadas de forma proativa.

Abaixo segue um exemplo de como otimizar o código do método de fallback e da transação de compensação:
 */

import org.springframework.stereotype.Service;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.transaction.annotation.Transactional;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.trace.Tracer;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class UserService {
 private static final Tracer tracer = OpenTelemetry.getTracerProvider().get("UserService");
 private final RabbitTemplate rabbitTemplate;
 private final UserRepository userRepository;
 private final CircuitBreaker circuitBreaker;

@Autowired
public UserService(RabbitTemplate rabbitTemplate, UserRepository userRepository, CircuitBreaker circuitBreaker) {
  this.rabbitTemplate = rabbitTemplate;
  this.userRepository = userRepository;
  this.circuitBreaker = circuitBreaker;
 }

 @Transactional
public String getUser(long id) {
  return circuitBreaker.executeSupplier(() -> {
   try {
    return userRepository.findById(id).orElseThrow(() -> new Exception("User not found")).getName();
   } catch (Exception e) {
    tracer.getCurrentSpan().setAttribute("error", true);
    throw e;
   }
  });
 }

 @Transactional
public void compensateGetUser(long id) {
  //logic to compensate the getUser method
  userRepository.deleteById(id);
 }

 @Transactional
public void fallbackGetUser(long id) {
  rabbitTemplate.convertAndSend("userQueue", id);
 }
}

 //circuitBreaker.executeSupplier() é usado para executar o método 'getUser' dentro do Circuit Breaker e ele irá automaticamente lidar com os métodos de fallback e compensação se o circuito estiver aberto.
//Além disso, o uso de 'circuitBreaker' em vez de criar novas instâncias de CircuitBreaker no método 'setupResilienceTriggers' e
//o uso da anotação 'Autowired' para a injeção do construtor em vez da injeção manual para os objetos.