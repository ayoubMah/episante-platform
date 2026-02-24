package episante.aai.appointmentservice;

import episante.aai.appointmentservice.config.KafkaTopicsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@EnableFeignClients
@SpringBootApplication
@EnableConfigurationProperties(KafkaTopicsProperties.class) // reference the standalone class
@ComponentScan(basePackages = {
        "episante.aai.appointmentservice",
        "com.upec.episantecommon"
})
public class AppointmentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppointmentServiceApplication.class, args);
    }

}
