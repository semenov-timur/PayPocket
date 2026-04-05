package com.paypocket;

import com.paypocket.service.UserService;
import com.paypocket.service.WalletService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Точка входа в Spring Boot приложние PayPocket.
 */
@SpringBootApplication
public class PayPocketApplication {
    public static void main(String[] args) {
        SpringApplication.run(PayPocketApplication.class, args);
    }
}
