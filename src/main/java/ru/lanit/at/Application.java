package ru.lanit.at;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import ru.lanit.at.service.FileReaderService;

@SpringBootApplication
public class Application {
	private static Logger logger = LoggerFactory.getLogger(Application.class);

	public static void main(String[] args) {
		ApplicationContext context = SpringApplication.run(Application.class, args);
		FileReaderService connectionService = context.getBean(FileReaderService.class);
		connectionService.readListConnections();
	}
}
