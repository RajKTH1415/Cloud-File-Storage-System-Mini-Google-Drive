package com.cloudFileStorageSystem;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication(scanBasePackages = {
		"com.cloudFileStorageSystem"
})
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
public class CloudStorageSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(CloudStorageSystemApplication.class, args);
	}

}
