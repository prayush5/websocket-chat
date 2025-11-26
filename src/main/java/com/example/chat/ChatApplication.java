package com.example.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChatApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatApplication.class, args);
        System.out.println("\n=========================================");
        System.out.println("Chat Application Started Successfully");
        System.out.println("Default users: ");
        System.out.println(" -admin/ admin123");
        System.out.println(" -user/ user123");
        System.out.println("\n=========================================");
    }

}
