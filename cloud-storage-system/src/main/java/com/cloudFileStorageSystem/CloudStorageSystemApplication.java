package com.cloudFileStorageSystem;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = {
		"com.cloudFileStorageSystem"
})
public class CloudStorageSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(CloudStorageSystemApplication.class, args);
	}

}
