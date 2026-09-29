package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import com.badlogic.gdx.Gdx;

public final class WorldSaveManager {

    public static final String SAVES_DIR = "saves";

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yy HH:mm").withZone(ZoneId.systemDefault());

    private WorldSaveManager() {}

    public static final class WorldInfo implements Comparable<WorldInfo> {
        public String name;
        public long   seed;
        public long   created;   // epoch millis
        public String fileName;  // sin extensión

        @Override
        public int compareTo(WorldInfo other) {
            if (other == null) return -1;
            int cmp = Long.compare(other.created, this.created);
            if (cmp != 0) return cmp;
            cmp = Long.compare(this.seed, other.seed);
            if (cmp != 0) return cmp;
            return Objects.compare(this.fileName, other.fileName, String::compareTo);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WorldInfo w = (WorldInfo) o;
            return seed == w.seed && created == w.created && Objects.equals(fileName, w.fileName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fileName, seed, created);
        }
    }

    // Devuelve la lista de mundos guardados, ordenada por fecha (más reciente primero)
    public static List<WorldInfo> listWorlds() {
        List<WorldInfo> worlds = new ArrayList<>();
        File dir = new File(SAVES_DIR);
        if (!dir.exists()) return worlds;

        File[] files = dir.listFiles((d, name) -> name.endsWith(".properties"));
        if (files == null) return worlds;

        for (File f : files) {
            WorldInfo info = loadInfo(f);
            if (info != null) worlds.add(info);
        }
        Collections.sort(worlds);
        return worlds;
    }

    private static WorldInfo loadInfo(File f) {
        try (FileInputStream fis = new FileInputStream(f)) {
            Properties p = new Properties();
            p.load(fis);
            WorldInfo info = new WorldInfo();
            info.name      = p.getProperty("world.name",    "Unknown");
            info.seed      = Long.parseLong(p.getProperty("world.seed",    "12345"));
            info.created   = Long.parseLong(p.getProperty("world.created", "0"));
            String fname   = f.getName();
            info.fileName  = fname.substring(0, fname.lastIndexOf('.'));
            return info;
        } catch (IOException | NumberFormatException e) {
            return null;
        }
    }

    public static WorldInfo saveWorld(String name, long seed) {
        File dir = new File(SAVES_DIR);
        if (!dir.exists() && !dir.mkdirs()) return null;

        long now      = System.currentTimeMillis();
        String safe   = name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        if (safe.isEmpty()) safe = "world";
        String fileName = safe + "_" + now;
        File file = new File(dir, fileName + ".properties");

        Properties p = new Properties();
        p.setProperty("world.name",    name);
        p.setProperty("world.seed",    String.valueOf(seed));
        p.setProperty("world.created", String.valueOf(now));

        try (FileOutputStream fos = new FileOutputStream(file)) {
            p.store(fos, "WhereYouAt - World Save");
            WorldInfo info = new WorldInfo();
            info.name     = name;
            info.seed     = seed;
            info.created  = now;
            info.fileName = fileName;
            return info;
        } catch (IOException e) {
            Gdx.app.error("WorldSaveManager", "No se pudo guardar el mundo: " + name, e);
            return null;
        }
    }

    public static void deleteWorld(String fileName) {
        File f = new File(SAVES_DIR, fileName + ".properties");
        if (f.exists() && !f.delete()) {
            Gdx.app.log("WorldSaveManager", "No se pudo borrar: " + f.getName());
        }
    }

    public static String formatDate(long millis) {
        return DATE_FORMATTER.format(Instant.ofEpochMilli(millis));
    }
}
