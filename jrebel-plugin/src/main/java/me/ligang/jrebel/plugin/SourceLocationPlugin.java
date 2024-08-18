package me.ligang.jrebel.plugin;

import com.sun.net.httpserver.HttpServer;
import org.zeroturnaround.javarebel.*;

import java.io.IOException;
import java.net.InetSocketAddress;

public class SourceLocationPlugin implements Plugin {

    private static final Logger logger = LoggerFactory.getInstance();

    @Override
    public void preinit() {
        OpenFileServer openFileServer = new OpenFileServer();
        openFileServer.start();

        Integration integration = IntegrationFactory.getInstance();
        ClassLoader classLoader = getClass().getClassLoader();

        try {
            integration.addIntegrationProcessor(
                    classLoader,
                    new SourceLocationClassBytecodeProcessor()
            );
            logger.info("SourceLocationClassBytecodeProcessor registered successfully.");

            System.out.println("==========================================");
            System.out.println("SourceLocationPlugin preinit completed.");
            System.out.println("==========================================");
        } catch (Exception e) {
            logger.error("Failed to register SourceLocationClassBytecodeProcessor", e);
        }
    }

    @Override
    public boolean checkDependencies(ClassLoader classLoader, ClassResourceSource classResourceSource) {
        try {
            classLoader.loadClass("org.springframework.web.bind.annotation.RequestMapping");
            return true;
        } catch (ClassNotFoundException e) {
            logger.warn("Spring framework dependency check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getId() {
        return "source-location-plugin";
    }

    @Override
    public String getName() {
        return "Source Location Plugin";
    }

    @Override
    public String getDescription() {
        return "A plugin that adds source location information to HTTP responses for Spring controllers.";
    }

    @Override
    public String getAuthor() {
        return "LiGang";
    }

    @Override
    public String getWebsite() {
        return "";
    }

    @Override
    public String getSupportedVersions() {
        return "";
    }

    @Override
    public String getTestedVersions() {
        return "";
    }
}
