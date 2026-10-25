package me.rageoffire.rofplayervault.utils;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.logging.Logger;

/**
 * Auto-downloads mysql-connector-j into /plugins/ROF_PlayerVault/libs/
 * and loads it at runtime if the driver is not already on the classpath.
 *
 * Flow:
 *  1. Check if com.mysql.cj.jdbc.Driver is already loadable → done, no download needed
 *  2. Check if the jar already exists in /libs/ → load it directly
 *  3. Download from Maven Central into /libs/ → load it
 *
 * This keeps the plugin jar lean (no shaded driver) while still working
 * out-of-the-box on every server version including 1.8.
 */
public final class MySQLDriverLoader {

    private static final String DRIVER_CLASS  = "com.mysql.cj.jdbc.Driver";
    private static final String GROUP         = "com/mysql/mysql-connector-j";
    private static final String VERSION       = "9.4.0";
    private static final String JAR_NAME      = "mysql-connector-j-" + VERSION + ".jar";
    private static final String MAVEN_CENTRAL =
            "https://repo1.maven.org/maven2/" + GROUP + "/" + VERSION + "/" + JAR_NAME;

    private MySQLDriverLoader() {}

    /**
     * Ensures the MySQL JDBC driver is available on the classpath.
     *
     * @param libsFolder  plugin's libs folder (created if absent)
     * @param logger      plugin logger for status messages
     * @return true if the driver is ready to use, false if it could not be loaded
     */
    public static boolean ensureDriver(File libsFolder, Logger logger) {

        // 1. Already on classpath? (Paper 1.16+, or server has it in /libs)
        if (isDriverPresent()) {
            logger.info("[MySQL] Driver already present on classpath.");
            return true;
        }

        // 2. Already downloaded into our libs folder?
        if (!libsFolder.exists()) libsFolder.mkdirs();
        File jarFile = new File(libsFolder, JAR_NAME);

        if (jarFile.exists()) {
            logger.info("[MySQL] Loading cached driver from " + jarFile.getPath());
            return loadJar(jarFile, logger);
        }

        // 3. Download from Maven Central
        logger.info("[MySQL] Driver not found — downloading " + JAR_NAME + " from Maven Central...");
        if (!download(MAVEN_CENTRAL, jarFile, logger)) return false;

        return loadJar(jarFile, logger);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static boolean isDriverPresent() {
        try {
            Class.forName(DRIVER_CLASS);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean download(String urlStr, File dest, Logger logger) {
        try {
            URL url = new URL(urlStr);
            File tmp = new File(dest.getParent(), dest.getName() + ".tmp");

            try (InputStream in  = url.openStream();
                 OutputStream out = new FileOutputStream(tmp)) {
                byte[] buf = new byte[8192];
                long total = 0;
                int  n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                    total += n;
                }
                logger.info("[MySQL] Downloaded " + (total / 1024) + " KB.");
            }

            // Rename tmp → final only after successful download
            Files.move(tmp.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;

        } catch (Exception e) {
            logger.severe("[MySQL] Download failed: " + e.getMessage());
            logger.severe("[MySQL] Place " + JAR_NAME + " manually in " + dest.getParent());
            return false;
        }
    }

    /**
     * Injects the jar into the current classloader at runtime.
     * Works on Java 8 (URLClassLoader trick) and Java 9+ (reflection fallback).
     */
    private static boolean loadJar(File jarFile, Logger logger) {
        try {
            ClassLoader cl = MySQLDriverLoader.class.getClassLoader();

            // Java 8 path — URLClassLoader has an addURL method
            if (cl instanceof URLClassLoader) {
                Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                addUrl.setAccessible(true);
                addUrl.invoke(cl, jarFile.toURI().toURL());
                logger.info("[MySQL] Driver loaded (URLClassLoader).");
                return true;
            }

            // Java 9+ path — use Bukkit's PluginClassLoader which exposes addJar via reflection
            try {
                Method addJar = cl.getClass().getDeclaredMethod("addJar", File.class);
                addJar.setAccessible(true);
                addJar.invoke(cl, jarFile);
                logger.info("[MySQL] Driver loaded (PluginClassLoader.addJar).");
                return true;
            } catch (NoSuchMethodException ignored) {}

            // Last resort — create a child URLClassLoader (driver registers itself via ServiceLoader)
            URLClassLoader child = new URLClassLoader(
                    new URL[]{ jarFile.toURI().toURL() },
                    Thread.currentThread().getContextClassLoader()
            );
            Thread.currentThread().setContextClassLoader(child);
            Class.forName(DRIVER_CLASS, true, child);
            logger.info("[MySQL] Driver loaded (child URLClassLoader).");
            return true;

        } catch (Exception e) {
            logger.severe("[MySQL] Failed to load driver jar: " + e.getMessage());
            return false;
        }
    }
}
