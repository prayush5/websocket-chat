package com.example.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChatApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatApplication.class, args);
        System.out.println("\n=========================================");
        System.out.println("\n Chat Application Started Successfully");
        System.out.println("\n=========================================");
    }

}
