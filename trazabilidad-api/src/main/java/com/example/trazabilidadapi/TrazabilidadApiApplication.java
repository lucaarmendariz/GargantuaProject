package com.example.trazabilidadapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Cierre automático de pedidos entregados
public class TrazabilidadApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrazabilidadApiApplication.class, args);
    }

}
