package com.paypocket;

import com.paypocket.model.User;
import com.paypocket.model.Wallet;
import com.paypocket.service.UserService;
import com.paypocket.service.WalletService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;

/**
 * Точка входа в Spring Boot приложние PayPocket.
 */
@SpringBootApplication
public class PayPocketApplication {
    public static void main(String[] args) {
        SpringApplication.run(PayPocketApplication.class, args);
    }

    @Bean
    CommandLineRunner testRunner(UserService userService,  WalletService walletService) {
        return  args -> {
            System.out.println("\n=== PayPocket Spring Boot Test ===\n");

            User alice;
            try {
                alice = userService.register("alice", "alice@mail.com", "1234");
                System.out.println("Registered: " + alice.getUsername());
            } catch (Exception ex) {
                System.out.println("Already exists, logging in...");
                alice = userService.getByUsername("alice");
            }

            Wallet wallet;
            var wallets = walletService.getUserWallets(alice.getId());
            if (wallets.isEmpty()) {
                wallet = walletService.createWallet(alice.getId(), "Основной");
                System.out.println("Created wallet: " + wallet.getName());
            } else {
                wallet = wallets.get(0);
                System.out.println("Wallet found: " + wallet.getName());
            }

            walletService.deposit(wallet.getId(), new BigDecimal("5000.55"));
            System.out.println("Wallet deposited: " + wallet.getBalance());

            System.out.println("\n=== Spring Boot is working! ===");
            System.out.println("Open http://localhost:8080 (web interface coming soon)");
        };
    }
}
