package fr.fonkisoft.bcmanager;

import java.sql.SQLException;

import fr.fonkisoft.bcmanager.rest.Controller;
import io.javalin.Javalin;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Main class of Bandcamp Collection manager, it starts the web server.
 */
@Slf4j
public class JavalinServer {
	
	@Getter
	private static int serverPort = 7070;
	@Getter
	private static String databasePath = "~/bc-manager/bcmanager";
	@Getter
	private static String databaseUser = "SA";
	@Getter
	private static String databasePassword = "";

	public static void main(String[] args) throws SQLException {
		log.info("\n\n"
				+ "  █████▄  ▄▄▄  ▄▄  ▄▄ ▄▄▄▄   ▄▄▄▄  ▄▄▄  ▄▄   ▄▄ ▄▄▄▄     ▄▄▄▄  ▄▄▄  ▄▄    ▄▄    ▄▄▄▄▄  ▄▄▄▄ ▄▄▄▄▄▄ ▄▄  ▄▄▄  ▄▄  ▄▄   ██▄  ▄██  ▄▄▄  ▄▄  ▄▄  ▄▄▄   ▄▄▄▄ ▄▄▄▄▄ ▄▄▄▄  \n"
				+ "  ██▄▄██ ██▀██ ███▄██ ██▀██ ██▀▀▀ ██▀██ ██▀▄▀██ ██▄█▀   ██▀▀▀ ██▀██ ██    ██    ██▄▄  ██▀▀▀   ██   ██ ██▀██ ███▄██   ██ ▀▀ ██ ██▀██ ███▄██ ██▀██ ██ ▄▄ ██▄▄  ██▄█▄ \n"
				+ "  ██▄▄█▀ ██▀██ ██ ▀██ ████▀ ▀████ ██▀██ ██   ██ ██      ▀████ ▀███▀ ██▄▄▄ ██▄▄▄ ██▄▄▄ ▀████   ██   ██ ▀███▀ ██ ▀██   ██    ██ ██▀██ ██ ▀██ ██▀██ ▀███▀ ██▄▄▄ ██ ██ \n");

		parseArgs(args);
		
		Controller controller = new Controller();
		Javalin app = Javalin.create(config -> {
				config.showJavalinBanner = false; 
				config.staticFiles.add("/app");
				config.spaRoot.addFile("/", "/app/index.html");
			})
			.get("/api/tags", controller::getTags)
			.get("/api/albums", controller::getAlbums)
			.post("/api/album", controller::importAlbum)
			.post("/api/fanpage", controller::importFanPage)
			.get("/api/fanpage", controller::importFanPageStatus)
			.get("/api/*", ctx -> ctx.status(400));
		
		app.start(serverPort);
		
		log.info("\n\nApplication started on: http://localhost" + (serverPort == 80 ? "": ":" + serverPort) + "/\n");
	}
	
	private static void parseArgs(String[] args) {
		log.info("\n\nParameters customization:\n  java -jar bc-manager.jar [-port <server port>] [-dbpath <database path>] [-dbUser <database user>] [-dbPass <database password>]\n"
				+ "    -port: server port (default: 7070)\n"
				+ "    -dbpath: database file path (default: ~/bc-manager/bcmanager)\n"
				+ "    -dbUser: database username (default: SA)\n"
				+ "    -dbPass: database password (default: none)\n");
		for (int i = 0 ; i < args.length ; i++) {
			if ("-port".equals(args[i])) {
				serverPort = Integer.parseInt(args[i + 1]);
				log.info("Use server port: {}", serverPort);
				i++;
			}
			else if ("-dbpath".equals(args[i])) {
				databasePath = args[i + 1];
				log.info("Use database path: {}", databasePath);
				i++;
			}
			else if ("-dbUser".equals(args[i])) {
				databaseUser = args[i + 1];
				log.info("Use database user: {}", databaseUser);
				i++;
			}
			else if ("-dbPass".equals(args[i])) {
				databasePassword = args[i + 1];
				log.info("Use database password: {}", databasePassword);
				i++;
			}
		}
	}
}
